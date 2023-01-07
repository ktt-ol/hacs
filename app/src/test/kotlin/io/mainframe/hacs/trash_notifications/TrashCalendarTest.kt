package io.mainframe.hacs.trash_notifications

import android.content.Context
import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

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

        val contextMock = mock<Context> {}

        val tc: TrashCalendar = object : TrashCalendar(contextMock) {
            override fun openAssetAsStream(assetFilename: String?): InputStream {
                return eventData.byteInputStream()
            }

            override fun now(): Date = mkDate("01-01-2023 18:23")
        }

        Assert.assertEquals("Gelber Sack/Tonne, Altpapier", tc.trashSummaryForTomorrow)
    }


    private fun mkDate(dateValue: String): Date = SimpleDateFormat("dd-MM-yyyy hh:mm").parse(dateValue)
}