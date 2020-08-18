package io.mainframe.hacs.trash_notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.mainframe.hacs.R;

public class TrashCalendar {

    private static final String SUMMARY_TAG = "SUMMARY:";
    private static final String DSTART_TAG = "DTSTART:";
    private static final String DEND_TAG = "DTEND:";

    private final Context context;
    private final ArrayList<TrashEvent> eventList = new ArrayList<>();

    public TrashCalendar(Context context) {
        this.context = context;

        try {
            parseIcalFile("Abfallkalender.ics");
        } catch (IOException | ParseException e) {
            Logger.error(e, "Can't parse trash calendar: " + e.getMessage());
        }

        // TESTING
//        final GregorianCalendar cal = new GregorianCalendar();
//        cal.set(2019, 9, 1, 8, 4, 23);
//        for (int i = 0; i < 30; i++) {
//            final Date start = cal.getTime();
//            final Date end = new Date(start.getTime() + 1000 * 60 * 60);
//            eventList.add(new TrashEvent("Event Test " + i, start, end));
//
//            cal.add(Calendar.DAY_OF_MONTH, 1);
//        }
//        Collections.sort(this.eventList, new Comparator<TrashEvent>() {
//            @Override
//            public int compare(TrashEvent o1, TrashEvent o2) {
//                return o1.startDate.compareTo(o2.startDate);
//            }
//        });
    }

    protected InputStream openAssetAsStream(String assetFilename) throws IOException {
        return context.getResources().getAssets().open(assetFilename);
    }

    protected Date now() {
        return new Date();
    }

    private void parseIcalFile(String assetFilename) throws IOException, ParseException {
        try (InputStream is = openAssetAsStream(assetFilename)) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            // e.g. 20181029T060000Z
            SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault());
            parser.setTimeZone(TimeZone.getTimeZone("utc"));

            boolean inEvent = false;
            String summary = null;
            Date startDate = null, endDate = null;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("BEGIN:VEVENT")) {
                    inEvent = true;
                    // reset values
                    summary = null;
                    startDate = endDate = null;
                } else if (line.equals("END:VEVENT")) {
                    inEvent = false;
                    if (summary == null || startDate == null || endDate == null) {
                        Logger.warn(String.format("Could not parse event. %s, %s, %s", summary, startDate, endDate));
                        continue;
                    }
                    eventList.add(new TrashEvent(summary, startDate, endDate));
                }

                if (inEvent) {
                    if (line.startsWith(SUMMARY_TAG)) {
                        summary = line.substring(SUMMARY_TAG.length());
                    } else if (line.startsWith(DSTART_TAG)) {
                        startDate = parser.parse(line.substring(DSTART_TAG.length()));
                    } else if (line.startsWith(DEND_TAG)) {
                        endDate = parser.parse(line.substring(DEND_TAG.length()));
                    }
                }
            }
        }

        // sort the events by start date
        Collections.sort(eventList, new Comparator<TrashEvent>() {
            @Override
            public int compare(TrashEvent o1, TrashEvent o2) {
                return o1.startDate.compareTo(o2.startDate);
            }
        });
    }

    public ArrayList<TrashEvent> getEventList() {
        return eventList;
    }

    public List<TrashEvent> getEvents(Date startDate, Date endDate) {
        ArrayList<TrashEvent> events = new ArrayList<>();
        for (TrashEvent trashEvent : getEventList()) {
            if (trashEvent.startDate.after(startDate)) {
                if (trashEvent.startDate.after(endDate)) {
                    // not in time range anymore
                    break;
                }
                events.add(trashEvent);
            }
        }

        return events;
    }


    // null for no immediate trash action, the summary text else
    public String getTrashSummaryForTomorrow() {
        Date now = now();

        // warn if the next event is 16 hours in the future
        long futureThreshold = now.getTime() + (1000 * 60 * 60 * 16);

        List<TrashEvent> events = getEvents(now, new Date(futureThreshold));
        return makeSummary(events);
    }

    private String makeSummary(List<TrashEvent> events) {
        String summary = null;
        for (TrashEvent event : events) {
            summary = summary == null ? event.summary : summary + ", " + event.summary;
        }

        return summary;
    }

    public void setNextAlarm() {
        final boolean trashNotifications = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.PREFS_TRASH_NOTIFICATIONS), false);
        if (!trashNotifications) {
            return;
        }

        // the next evening at 20.03
        GregorianCalendar nextNotificationDate = new GregorianCalendar();
        nextNotificationDate.set(Calendar.HOUR_OF_DAY, 20);
        nextNotificationDate.set(Calendar.MINUTE, 3);
        if (nextNotificationDate.getTimeInMillis() < System.currentTimeMillis()) {
            // we are after 20.03 in this evening -> add one day
            nextNotificationDate.add(Calendar.DAY_OF_YEAR, 1);
        }

        Date now = now();
        // warn if the next event is 16 hours in the future of the planned notification
        long futureThreshold = nextNotificationDate.getTimeInMillis() + (1000 * 60 * 60 * 16);
        List<TrashEvent> events = getEvents(nextNotificationDate.getTime(), new Date(now.getTime() + futureThreshold));
        if (events.isEmpty()) {
            Logger.info("No next alarm.");
            return;
        }

        Intent notificationIntent = new Intent(context, NotificationPublisher.class);

        String msg = String.format("%s wird morgen abgeholt. Bitte an die Straße stellen.", makeSummary(events));
        notificationIntent.putExtra(NotificationPublisher.EXTRA_MSG, msg);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, nextNotificationDate.getTimeInMillis(), pendingIntent);

        Logger.info("Setting next alarm {} at {}", events, nextNotificationDate.getTime());
    }

    public static class TrashEvent {
        public final String summary;
        public final Date startDate, endDate;

        public TrashEvent(String summary, Date startDate, Date endDate) {
            this.summary = summary;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @Override
        public String toString() {
            return "TrashEvent{" +
                    "summary='" + summary + '\'' +
                    ", startDate=" + startDate +
                    ", endDate=" + endDate +
                    '}';
        }
    }

}
