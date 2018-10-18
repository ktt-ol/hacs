package io.mainframe.hacs.trash_notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;

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
import java.util.Locale;
import java.util.TimeZone;

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
            Logger.error("Can't parse trash calendar: " + e.getMessage(), e);
        }

        // TESTING
//        final GregorianCalendar cal = new GregorianCalendar();
//        cal.set(2018, 8, 20, 20, 4, 23);
//        for (int i = 0; i < 30; i++) {
//            final Date start = cal.getTime();
//            final Date end = new Date(start.getTime() + 1000 * 60 * 60);
//            eventList.add(new TrashEvent("Event Test " + i, start, end));
//
//            cal.add(Calendar.DAY_OF_MONTH, 1);
//        }
    }

    private void parseIcalFile(String assetFilename) throws IOException, ParseException {
        try (InputStream is = context.getResources().getAssets().open(assetFilename)) {
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

    public TrashEvent getNextEvent(Date targetTime) {
        for (TrashEvent trashEvent : getEventList()) {
            if (trashEvent.startDate.after(targetTime)) {
                return trashEvent;
            }
        }

        return null;
    }

    // null for no immediate trash action, the summary text else
    public String getTrashSummaryForTomorrow() {
        final TrashEvent nextEvent = getNextEvent(new Date());
        if (nextEvent == null) {
            return null;
        }

        // warn if the next event is 16 hours in the future
        long futureThreshold = System.currentTimeMillis() + (1000 * 60 * 16);

        if (nextEvent.startDate.getTime() < futureThreshold) {
            return nextEvent.summary;
        }

        return null;
    }

    public void setNextAlarm() {
        // the next evening at 20.03
        GregorianCalendar nextNotificationDate = new GregorianCalendar();
        nextNotificationDate.set(Calendar.HOUR_OF_DAY, 20);
        nextNotificationDate.set(Calendar.MINUTE, 3);
        if (nextNotificationDate.getTimeInMillis() < System.currentTimeMillis()) {
            // we are after 20.03 in this evening -> add one day
            nextNotificationDate.add(Calendar.DAY_OF_YEAR, 1);
        }

        final TrashEvent nextEvent = getNextEvent(nextNotificationDate.getTime());
        if (nextEvent == null) {
            Logger.info("No next alarm.");
            return;
        }

        // warn if the next event is 16 hours in the future of the planned notification
        long futureThreshold = nextNotificationDate.getTimeInMillis() + (1000 * 60 * 16);

        if (nextEvent.startDate.getTime() > futureThreshold) {
            Logger.info("Next alarm ({}) not in the threshold ({}).",
                    nextEvent, nextNotificationDate.getTime());
            return;
        }


        Intent notificationIntent = new Intent(context, NotificationPublisher.class);

        String msg = String.format("%s wird morgen abgeholt. Bitte an die Straße stellen.", nextEvent.summary);
        notificationIntent.putExtra(NotificationPublisher.EXTRA_MSG, msg);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, nextNotificationDate.getTimeInMillis(), pendingIntent);

        Logger.info("Setting next alarm {} at {}", nextEvent, nextNotificationDate.getTime());
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
