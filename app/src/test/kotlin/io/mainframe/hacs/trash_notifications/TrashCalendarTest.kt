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
VERSION:2.0
PRODID:www.citkomm.de
X-PUBLISHED-TTL:P1W
BEGIN:VEVENT
UID:5e0c0da85ead6
DTSTART:20200814T050000Z
SEQUENCE:0
TRANSP:OPAQUE
DTEND:20200814T060000Z
SUMMARY:Restabfall
CLASS:PUBLIC
DTSTAMP:20200101T041032Z
END:VEVENT
BEGIN:VEVENT
UID:5e0c0da85eb7f
DTSTART:20200817T050000Z
SEQUENCE:0
TRANSP:OPAQUE
DTEND:20200817T060000Z
SUMMARY:Gelber Sack/Tonne
CLASS:PUBLIC
DTSTAMP:20200101T041032Z
END:VEVENT
BEGIN:VEVENT
UID:5e0c0da85ec25
DTSTART:20200817T050000Z
SEQUENCE:0
TRANSP:OPAQUE
DTEND:20200817T060000Z
SUMMARY:Altpapier
CLASS:PUBLIC
DTSTAMP:20200101T041032Z
END:VEVENT
BEGIN:VEVENT
UID:5e0c0da85ecca
DTSTART:20200821T050000Z
SEQUENCE:0
TRANSP:OPAQUE
DTEND:20200821T060000Z
SUMMARY:Bioabfall
CLASS:PUBLIC
DTSTAMP:20200101T041032Z
END:VEVENT
END:VCALENDAR            
        """.trimIndent()

        val contextMock = mock<Context> {}

        val tc: TrashCalendar = object : TrashCalendar(contextMock) {
            override fun openAssetAsStream(assetFilename: String?): InputStream {
                return eventData.byteInputStream()
            }

            override fun now(): Date = mkDate("16-08-2020 18:23")
        }

        Assert.assertEquals("Gelber Sack/Tonne, Altpapier", tc.trashSummaryForTomorrow)
    }


    private fun mkDate(dateValue: String): Date = SimpleDateFormat("dd-MM-yyyy hh:mm").parse(dateValue)
}