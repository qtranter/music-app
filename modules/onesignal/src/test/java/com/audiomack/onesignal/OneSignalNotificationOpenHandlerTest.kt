package com.audiomack.onesignal

import android.net.Uri
import junit.framework.Assert.assertNull
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OneSignalNotificationOpenHandlerTest {

    private lateinit var sut: OneSignalNotificationOpenHandler

    @Before
    fun setup() {
        sut = OneSignalNotificationOpenHandler()
    }

    @Test
    fun `valid payload`() {
        val artistName = "b"
        val albumName = "Carpe Diem"
        val albumId = "2"
        val genre = "d"
        val campaign = "z"
        val deeplink = "audiomack://olamide/album/carpe-diem"

        val json = JSONObject().apply {
            put("artistname", artistName)
            put("albumname", albumName)
            put("albumid", albumId)
            put("genre", genre)
            put("campaign", campaign)
            put("am_deeplink", deeplink)
        }

        val result = sut.processNotification(json)
        assert(result != null)
        assert(requireNotNull(result).deeplinkUri == Uri.parse(deeplink))
        assert(result.info == TransactionalNotificationInfo(
            artistName = artistName,
            albumName = albumName,
            albumId = albumId,
            genre = genre,
            campaign = campaign
        ))
    }

    @Test
    fun `valid payload with ticket deeplink`() {
        val songName = "a"
        val songId = "1"
        val artistName = "b"
        val albumName = "c"
        val albumId = "2"
        val genre = "d"
        val playlistName = "e"
        val playlistId = "3"
        val campaign = "z"
        val ticketId = "123"

        val json = JSONObject().apply {
            put("songname", songName)
            put("songid", songId)
            put("artistname", artistName)
            put("albumname", albumName)
            put("albumid", albumId)
            put("genre", genre)
            put("playlistname", playlistName)
            put("playlistid", playlistId)
            put("campaign", campaign)
            put("ticket_id", ticketId)
        }

        val result = sut.processNotification(json)
        assert(result != null)
        assert(requireNotNull(result).deeplinkUri == Uri.parse("audiomack://support/$ticketId"))
        assert(result.info == TransactionalNotificationInfo(
            songName = songName,
            songId = songId,
            artistName = artistName,
            albumName = albumName,
            albumId = albumId,
            genre = genre,
            playlistName = playlistName,
            playlistId = playlistId,
            campaign = campaign
        ))
    }

    @Test
    fun `without data`() {
        assertNull(sut.processNotification(JSONObject()))
    }
}
