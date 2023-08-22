package com.audiomack.model

import com.audiomack.TestApplication
import com.nhaarman.mockitokotlin2.mock
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    application = TestApplication::class
)
class AMFeaturedSpotTest {

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `featured artist parsing`() {
        val actorName = "Matteo"
        val actorSlug = "matteinn"
        val actorImage = "https://"

        val json = JSONObject().apply {
            put("type", "featured")
            put("ref", JSONObject().apply {
                put("type", "artist")
                put("name", actorName)
                put("url_slug", actorSlug)
                put("image_base", actorImage)
            })
        }
        val featuredSpot = AMFeaturedSpot.fromJSON(json) ?: run {
            fail("null featured spot")
            return
        }
        assertNotNull(featuredSpot.artist)
        assertNull(featuredSpot.item)
        assertEquals(actorName, featuredSpot.artist!!.name)
        assertEquals(actorSlug, featuredSpot.artist!!.urlSlug)
        assert(featuredSpot.artist!!.tinyImage!!.startsWith(actorImage))
        assert(featuredSpot.artist!!.smallImage!!.startsWith(actorImage))
        assert(featuredSpot.artist!!.mediumImage!!.startsWith(actorImage))
        assert(featuredSpot.artist!!.largeImage!!.startsWith(actorImage))
        assertTrue(featuredSpot.getPrettyType(mock())!!.toLowerCase().contains("featured"))
    }

    @Test
    fun `sponsored playlist parsing`() {
        val playlistId = "100"
        val playlistTitle = "Rock Music"

        val json = JSONObject().apply {
            put("type", "sponsored")
            put("ref", JSONObject().apply {
                put("type", "playlist")
                put("id", playlistId)
                put("title", playlistTitle)
                put("artist", JSONObject())
            })
        }
        val featuredSpot = AMFeaturedSpot.fromJSON(json) ?: run {
            fail("null featured spot")
            return
        }
        assertNull(featuredSpot.artist)
        assertNotNull(featuredSpot.item)
        assertTrue(featuredSpot.item!!.isPlaylist)
        assertEquals(playlistId, featuredSpot.item!!.itemId)
        assertEquals(playlistTitle, featuredSpot.item!!.title)
        assertTrue(featuredSpot.getPrettyType(mock())!!.toLowerCase().contains("sponsored"))
    }

    @Test
    fun `invalid parsing`() {
        val json = JSONObject()
        val featuredSpot = AMFeaturedSpot.fromJSON(json)
        assertNull(featuredSpot)
    }
}
