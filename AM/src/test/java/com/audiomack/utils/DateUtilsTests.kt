package com.audiomack.utils

import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DateUtilsTests {

    @Test
    fun testItemDateParsing() {
        assertEquals("May 22, 2018", DateUtils.getItemDateAsString(1527025093000L))
        assertEquals("October 2, 2017", DateUtils.getItemDateAsString(1506960215000L))
    }

    @Test
    fun testNotificationDateParsing() {
        assertNotNull(DateUtils.getInstance().getNotificationDate("2018-06-22T05:15:16.000000"))
        assertNotNull(DateUtils.getInstance().getNotificationDate("2018-05-02T10:11:22.000000"))
    }

    @Test
    fun testArtistCreatedDateFormatting() {
        assertEquals(
            "Nov '17",
            DateUtils.getInstance().getArtistCreatedAsString(Date(1509747701000L))
        )
        assertEquals(
            "Jul '16",
            DateUtils.getInstance().getArtistCreatedAsString(Date(1469801562000L))
        )
    }

    @Test
    fun testYOB() {
        assertEquals(2018, DateUtils.getYOB(Date(1527025093000L)))
        assertEquals(2017, DateUtils.getYOB(Date(1506960215000L)))
    }
}
