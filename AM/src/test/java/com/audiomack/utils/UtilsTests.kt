package com.audiomack.utils

import com.nhaarman.mockitokotlin2.mock
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilsTests {

    @Test
    fun testTimeFromMilliseconds0() {
        assertEquals("0:00", Utils.timeFromMilliseconds(0))
    }

    @Test
    fun testTimeFromMillisecondsNegative() {
        assertEquals("-:--", Utils.timeFromMilliseconds(-30000))
    }

    @Test
    fun testTimeFromMilliseconds127400() {
        assertEquals("2:07", Utils.timeFromMilliseconds(127400))
    }

    @Test
    fun testTimeFromMilliseconds349900() {
        assertEquals("5:49", Utils.timeFromMilliseconds(349500))
    }

    @Test
    fun testDeslashValidUrl() {
        assertEquals("http://google.com", Utils.deslash("http://google.com"))
    }

    @Test
    fun testDeslashInvalidUrl() {
        assertEquals("http://google.com", Utils.deslash("http:\\/\\/google.com"))
    }

    @Test
    fun testUserAgent() {
        val userAgent = Utils.getUserAgent(mock())
        assertTrue(userAgent.contains("audiomack-android") && userAgent.contains(";"))
    }

    @Test
    fun testRemoveNonAsciiCharacter() {
        assertEquals(
            "H e l l o W o r l d!!",
            Utils.removeNonASCIICharacters("H±e¤l¼lÅo£Wßoær÷l∇d!!")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testPurgeDirectory() {
        val dir = File("directory")
        dir.mkdir()
        File(dir, "a.txt").createNewFile()
        File(dir, "b.txt").createNewFile()
        assertEquals(dir.listFiles().size.toLong(), 2)
        Utils.purgeDirectory(dir)
        assertEquals(dir.listFiles().size.toLong(), 0)
    }

    @Test
    @Throws(Exception::class)
    fun testMoveFile() {
        val src = File("a.txt")
        val dest = File("b.txt")
        src.createNewFile()
        dest.delete()
        assertFalse(dest.exists())
        Utils.moveFile(src.absolutePath, dest.absolutePath)
        assertTrue(dest.exists())
        dest.delete()
    }

    @Test
    fun testFormatFullStatNumber_zero() {
        assertEquals("0", Utils.formatFullStatNumber(0L))
    }

    @Test
    fun testFormatFullStatNumber_tens() {
        assertEquals("65", Utils.formatFullStatNumber(65L))
    }

    @Test
    fun testFormatFullStatNumber_thousands() {
        assertEquals("1,309", Utils.formatFullStatNumber(1309L))
    }

    @Test
    fun testFormatFullStatNumber_hundredsthousands() {
        assertEquals("125,101", Utils.formatFullStatNumber(125101L))
    }

    @Test
    fun testFormatFullStatNumber_millions() {
        assertEquals("7,900,123", Utils.formatFullStatNumber(7900123L))
    }

    @Test
    fun testFormatFullStatNumber_hundredsmillions() {
        assertEquals("987,654,321", Utils.formatFullStatNumber(987654321L))
    }

    @Test
    fun testFormatFullStatNumber_billions() {
        assertEquals("25,000,000,000", Utils.formatFullStatNumber(25000000000L))
    }

    @Test
    fun testFormatShortStatNumber_zero() {
        assertEquals("0", Utils.formatShortStatNumber(0L))
    }

    @Test
    fun testFormatShortStatNumber_tens() {
        assertEquals("65", Utils.formatShortStatNumber(65L))
    }

    @Test
    fun testFormatShortStatNumber_thousands() {
        assertEquals("1.30K", Utils.formatShortStatNumber(1309L))
    }

    @Test
    fun testFormatShortStatNumber_hundredsthousands() {
        assertEquals("125K", Utils.formatShortStatNumber(125101L))
    }

    @Test
    fun testFormatShortStatNumber_millions() {
        assertEquals("7.90M", Utils.formatShortStatNumber(7900123L))
    }

    @Test
    fun testFormatShortStatNumber_hundredsmillions() {
        assertEquals("987M", Utils.formatShortStatNumber(987654321L))
    }

    @Test
    fun testFormatShortStatNumber_billions() {
        assertEquals("25.0B", Utils.formatShortStatNumber(25000000000L))
    }

    @Test
    fun testFormatShortStatNumberWithoutDecimals_zero() {
        assertEquals("0", Utils.formatShortStatNumberWithoutDecimals(0L))
    }

    @Test
    fun testFormatShortStatNumberWithoutDecimals_tens() {
        assertEquals("65", Utils.formatShortStatNumberWithoutDecimals(65L))
    }

    @Test
    fun testFormatShortStatNumberWithoutDecimals_thousands() {
        assertEquals("1K", Utils.formatShortStatNumberWithoutDecimals(1309L))
    }

    @Test
    fun testFormatShortStatNumberWithoutDecimals_hundredsthousands() {
        assertEquals("125K", Utils.formatShortStatNumberWithoutDecimals(125101L))
    }

    @Test
    fun testFormatShortStatNumberWithoutDecimals_millions() {
        assertEquals("7M", Utils.formatShortStatNumberWithoutDecimals(7900123L))
    }

    @Test
    fun testFormatShortStatNumberWithoutDecimals_hundredsmillions() {
        assertEquals("987M", Utils.formatShortStatNumberWithoutDecimals(987654321L))
    }

    @Test
    fun testFormatShortStatNumberWithoutDecimals_billions() {
        assertEquals("25B", Utils.formatShortStatNumberWithoutDecimals(25000000000L))
    }

    @Test
    fun testFormatShortStatNumberWithoutDecimals_manyBillions() {
        assertEquals("2500B", Utils.formatShortStatNumberWithoutDecimals(2500000000000L))
    }

    @Test
    fun testRoundNumber_invalid() {
        assertEquals(450, Utils.roundNumber(450, 0).toLong())
        assertEquals(450, Utils.roundNumber(450, -1).toLong())
        assertEquals(0, Utils.roundNumber(0, -1).toLong())
    }

    @Test
    fun testRoundNumber_exact() {
        assertEquals(0, Utils.roundNumber(0, 1).toLong())
        assertEquals(450, Utils.roundNumber(450, 1).toLong())
    }

    @Test
    fun testRoundNumber_rounding() {
        assertEquals(420, Utils.roundNumber(450, 70).toLong())
        assertEquals(450, Utils.roundNumber(450, 50).toLong())
        assertEquals(0, Utils.roundNumber(450, 1000).toLong())
        assertEquals(0, Utils.roundNumber(0, 30).toLong())
    }
}
