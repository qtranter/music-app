package com.audiomack.model

import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertEquals
import org.junit.Test

class AMGenreTests {

    @Test
    fun testConversionFromApiValue() {
        AMGenre.values().forEach {
            assertEquals(it, AMGenre.fromApiValue(it.apiValue()))
        }
    }

    @Test
    fun testConversionFromHumanValue() {
        AMGenre.values().filter { it != AMGenre.All }.forEach {
            assertEquals(it, AMGenre.fromHumanValue(it.humanValue(mock()), mock()))
        }
    }

    @Test
    fun testHumanValues() {
        AMGenre.values().filter { it != AMGenre.All }.forEach {
            assert(it.humanValue(mock()).isNotBlank())
        }
    }

    @Test
    fun testGenreRapConversion() {
        assertEquals(AMGenre.Rap, AMGenre.fromApiValue("rap"))
    }

    @Test
    fun testGenreElectronicConversion() {
        assertEquals(AMGenre.Electronic, AMGenre.fromApiValue("electronic"))
    }

    @Test
    fun testGenreRockConversion() {
        assertEquals(AMGenre.Rock, AMGenre.fromApiValue("rock"))
    }

    @Test
    fun testGenrePopConversion() {
        assertEquals(AMGenre.Pop, AMGenre.fromApiValue("pop"))
    }

    @Test
    fun testGenreAfrobeatsConversion() {
        assertEquals(AMGenre.Afrobeats, AMGenre.fromApiValue("afrobeats"))
    }

    @Test
    fun testGenreAfropopFallback() {
        assertEquals(AMGenre.Afrobeats, AMGenre.fromApiValue("afropop"))
    }

    @Test
    fun testGenreReggaeConversion() {
        assertEquals(AMGenre.Dancehall, AMGenre.fromApiValue("dancehall"))
    }

    @Test
    fun testGenrePodcastConversion() {
        assertEquals(AMGenre.Podcast, AMGenre.fromApiValue("podcast"))
    }

    @Test
    fun testGenreJazzConversion() {
        assertEquals(AMGenre.Jazz, AMGenre.fromApiValue("jazz"))
    }

    @Test
    fun testGenreCountryConversion() {
        assertEquals(AMGenre.Country, AMGenre.fromApiValue("country"))
    }

    @Test
    fun testGenreWorldConversion() {
        assertEquals(AMGenre.World, AMGenre.fromApiValue("world"))
    }

    @Test
    fun testGenreClassicalConversion() {
        assertEquals(AMGenre.Classical, AMGenre.fromApiValue("classical"))
    }

    @Test
    fun testGenreGospelConversion() {
        assertEquals(AMGenre.Gospel, AMGenre.fromApiValue("gospel"))
    }

    @Test
    fun testGenreAcapellaConversion() {
        assertEquals(AMGenre.Acapella, AMGenre.fromApiValue("acapella"))
    }

    @Test
    fun testGenreRnbConversion() {
        assertEquals(AMGenre.Rnb, AMGenre.fromApiValue("rnb"))
    }

    @Test
    fun testGenreLatinConversion() {
        assertEquals(AMGenre.Latin, AMGenre.fromApiValue("latin"))
    }

    @Test
    fun testGenreInstrumentalConversion() {
        assertEquals(AMGenre.Instrumental, AMGenre.fromApiValue("instrumental"))
    }

    @Test
    fun testGenreDjMixConversion() {
        assertEquals(AMGenre.Djmix, AMGenre.fromApiValue("dj-mix"))
    }

    @Test
    fun testGenreFolkConversion() {
        assertEquals(AMGenre.Folk, AMGenre.fromApiValue("folk"))
    }

    @Test
    fun testGenreOtherConversion() {
        assertEquals(AMGenre.Other, AMGenre.fromApiValue("other"))
    }

    @Test
    fun testInvalidGenre() {
        assertEquals(AMGenre.Other, AMGenre.fromApiValue("invalid genre"))
    }

    @Test
    fun testGenreNullConversion() {
        assertEquals(AMGenre.Other, AMGenre.fromApiValue(null))
    }

    @Test
    fun testGenreEmptyConversion() {
        assertEquals(AMGenre.Other, AMGenre.fromApiValue(""))
    }
}
