package io.mainframe.hacs.trash_notifications

import android.content.Context
import org.pmw.tinylog.Logger
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TrashCalendar(
    val dateProvider: () -> Date = { Date() },
    val loadAsset: (assetFilename: String) -> InputStream
) {
    private val eventList = ArrayList<TrashEvent>()

    init {
        try {
            parseIcalFile("Abfallkalender.ics")
        } catch (e: Exception) {
            Logger.error(e, "Can't parse trash calendar: ${e.message}")
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

    private fun parseIcalFile(assetFilename: String) {
        loadAsset(assetFilename).use { `is` ->
            val reader = BufferedReader(InputStreamReader(`is`))
            // e.g. 20181029T060000Z
            val parser = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("utc")

            var inEvent = false
            var summary: String? = null
            var startDate: Date? = null
            var endDate: Date? = null

            var line: String
            while ((reader.readLine().also { line = it }) != null) {
                if (line == "BEGIN:VEVENT") {
                    inEvent = true
                    // reset values
                    summary = null
                    endDate = null
                    startDate = endDate
                } else if (line == "END:VEVENT") {
                    inEvent = false
                    if (summary == null || startDate == null || endDate == null) {
                        Logger.warn(
                            "Could not parse event. {}, {}, {}",
                            summary, startDate, endDate
                        )
                        continue
                    }
                    eventList.add(TrashEvent(summary, startDate, endDate))
                }

                if (inEvent) {
                    when {
                        line.startsWith(SUMMARY_TAG) -> {
                            summary = line.substring(SUMMARY_TAG.length)
                        }

                        line.startsWith(DSTART_TAG) -> {
                            startDate = parser.parse(line.substring(DSTART_TAG.length))
                        }

                        line.startsWith(DEND_TAG) -> {
                            endDate = parser.parse(line.substring(DEND_TAG.length))
                        }
                    }
                }
            }
        }
        // sort the events by start date
        eventList.sortWith { o1, o2 -> o1.startDate.compareTo(o2.startDate) }
    }

    private fun getEvents(startDate: Date, endDate: Date): List<TrashEvent> {
        val events = ArrayList<TrashEvent>()
        for (trashEvent in eventList) {
            if (trashEvent.startDate.after(startDate)) {
                if (trashEvent.startDate.after(endDate)) {
                    // not in time range anymore
                    break
                }
                events.add(trashEvent)
            }
        }

        return events
    }


    fun getTrashSummaryForTomorrow(): String? {
        val now = dateProvider()

        // warn if the next event is 16 hours in the future
        val futureThreshold = now.time + (1000 * 60 * 60 * 16)

        val events = getEvents(now, Date(futureThreshold))
        return makeSummary(events)
    }

    private fun makeSummary(events: List<TrashEvent>): String? {
        var summary: String? = null
        for (event in events) {
            summary = if (summary == null) event.summary else summary + ", " + event.summary
        }

        return summary
    }

    data class TrashEvent(val summary: String, val startDate: Date, val endDate: Date)

    companion object {

        fun assetByContext(context: Context, assetFilename: String): InputStream {
            return context.resources.assets.open(assetFilename)
        }

        private const val SUMMARY_TAG = "SUMMARY:"
        private const val DSTART_TAG = "DTSTART:"
        private const val DEND_TAG = "DTEND:"
    }
}