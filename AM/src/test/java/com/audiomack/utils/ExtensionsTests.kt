package com.audiomack.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsTests {

    @Test
    fun `testSplitFeatArtists "and"`() {
        val result = "and".featArtists
        val expected = listOf("and")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "y"`() {
        val result = "y".featArtists
        val expected = listOf("y")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "&"`() {
        val result = "&".featArtists
        val expected = listOf("&")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "Sand"`() {
        val result = "Sand".featArtists
        val expected = listOf("Sand")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "Sand and water"`() {
        val result = "Sand and water".featArtists
        val expected = listOf("Sand", "water")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "BlocBoy JB, Polo G"`() {
        val result = "BlocBoy JB, Polo G".featArtists
        val expected = listOf("BlocBoy JB", "Polo G")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "yy and &address"`() {
        val result = "yy and &address".featArtists
        val expected = listOf("yy", "&address")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "Cardi B"`() {
        val result = "Cardi B".featArtists
        val expected = listOf("Cardi B")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "Romeo and Juliet"`() {
        val result = "Romeo and Juliet".featArtists
        val expected = listOf("Romeo", "Juliet")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "Romeo And Juliet"`() {
        val result = "Romeo And Juliet".featArtists
        val expected = listOf("Romeo And Juliet")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "Romeo & Juliet"`() {
        val result = "Romeo & Juliet".featArtists
        val expected = listOf("Romeo", "Juliet")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "Romeo y Juliet"`() {
        val result = "Romeo y Juliet".featArtists
        val expected = listOf("Romeo", "Juliet")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "Romeo Y Juliet"`() {
        val result = "Romeo Y Juliet".featArtists
        val expected = listOf("Romeo Y Juliet")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "Moneybagg Yo, Yo Gotti, Lil Durk, Polo G, Meek Mill & Young Thug"`() {
        val result = "Moneybagg Yo, Yo Gotti, Lil Durk, Polo G, Meek Mill & Young Thug".featArtists
        val expected = listOf("Moneybagg Yo", "Yo Gotti", "Lil Durk", "Polo G", "Meek Mill", "Young Thug")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `testSplitFeatArtists "Moneybagg Yo,Yo Gotti,Lil Durk,Polo G,Meek Mill & Young Thug"`() {
        val result = "Moneybagg Yo,Yo Gotti,Lil Durk,Polo G,Meek Mill & Young Thug".featArtists
        val expected = listOf("Moneybagg Yo", "Yo Gotti", "Lil Durk", "Polo G", "Meek Mill", "Young Thug")
        result.zip(expected).forEach { assertEquals(it.first, it.second) }
    }

    @Test
    fun `app version comparisons`() {
        assertTrue("5.10.0".isVersionLowerThan("6"))
        assertTrue("5.10.0".isVersionLowerThan("6.0"))
        assertTrue("5.10.0".isVersionLowerThan("6.20.30"))

        assertFalse("5.10.0".isVersionLowerThan(""))
        assertFalse("5.10.0".isVersionLowerThan("0.0.0"))
        assertFalse("5.10.0".isVersionLowerThan("5.0"))
        assertFalse("5.10.0".isVersionLowerThan("5.10"))
        assertFalse("5.10.0".isVersionLowerThan("5.10.0"))
        assertFalse("5.10.0".isVersionLowerThan("5.10.0-alpha"))
        assertFalse("5.10.0".isVersionLowerThan("5.6.0"))
        assertFalse("5.10.0".isVersionLowerThan("5.6.1.2"))
        assertFalse("5.10.0".isVersionLowerThan("5.6.1.2.3"))
    }
}
