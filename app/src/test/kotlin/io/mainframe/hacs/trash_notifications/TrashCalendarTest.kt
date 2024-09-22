package io.mainframe.hacs.trash_notifications

import org.junit.Assert
import org.junit.Test
import java.text.SimpleDateFormat

class TrashCalendarTest {

    @Test
    fun `more than one event per day`() {
        val eventData = """
BEGIN:VCALENDAR
PRODID:-//eluceo/ical//2.0/EN
VERSION:2.0
CALSCALE:GREGORIAN
BEGIN:VEVENT
UID:3c0038f8e3bc48bbb9f2f63f581b17ef
DTSTAMP:20230101T025220Z
SUMMARY:Gelber Sack/Tonne
DTSTART:20230102T060000
DTEND:20230102T070000
END:VEVENT
BEGIN:VEVENT
UID:2ced945dd70f87c6bd70e3d453f4a588
DTSTAMP:20230101T025220Z
SUMMARY:Altpapier
DTSTART:20230102T060000
DTEND:20230102T070000
END:VEVENT
END:VCALENDAR  
        """.trimIndent()

        val tc = TrashCalendar(
            dateProvider = { SimpleDateFormat("dd-MM-yyyy hh:mm").parse("01-01-2023 18:23") },
            loadAsset = { eventData.byteInputStream() }
        )

        Assert.assertEquals("Gelber Sack/Tonne, Altpapier", tc.getTrashSummaryForTomorrow())
    }
}
