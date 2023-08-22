package com.audiomack.model

import com.audiomack.TestApplication
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    application = TestApplication::class
)
class AMArtistTest {

    private lateinit var json: JSONObject

    private val id = "123"
    private val name = "Matteo"
    private val image = "https://avatar"
    private val genre = "rap"
    private val hometown = "Florence"
    private val bio = "Hello World"
    private val url = "https://website"
    private val urlSlug = "slug"
    private val label = "label"
    private val twitter = "@username"
    private val facebook = "https://facebook"
    private val instagram = "@username"
    private val verified = ""
    private val uploadCountExcludingReups = 1
    private val favorites = listOf("1", "2", "3", "4", "5")
    private val playlists = listOf("6")
    private val followingCount = 1100
    private val followersCount = 224523
    private val feedCount = 33
    private val created = 1570204748
    private val admin = false
    private val banner = "https://banner"
    private val youtube = "https://youtube"
    private val reups = listOf("7", "8", "9", "10")
    private val pinned = listOf("11", "12")
    private val canComment = true
    private val playsCount = 66666666L

    @Before
    fun setup() {
        json = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("image_base", image)
            put("genre", genre)
            put("hometown", hometown)
            put("bio", bio)
            put("url", url)
            put("url_slug", urlSlug)
            put("label", label)
            put("twitter", twitter)
            put("facebook", facebook)
            put("instagram", instagram)
            put("verified", verified)
            put("upload_count_excluding_reups", uploadCountExcludingReups)
            put("favorite_music", JSONArray().apply {
                favorites.forEach { put(it) }
            })
            put("playlists", JSONArray().apply {
                playlists.forEach { put(it) }
            })
            put("following_count", followingCount)
            put("followers_count", followersCount)
            put("new_feed_items", feedCount)
            put("created", created)
            put("is_admin", admin)
            put("image_banner", banner)
            put("youtube", youtube)
            put("reups", JSONArray().apply {
                reups.forEach { put(it) }
            })
            put("pinned", JSONArray().apply {
                pinned.forEach { put(it) }
            })
            put("can_comment", canComment)
            put("stats", JSONObject().apply {
                put("plays-raw", playsCount)
            })
        }
    }

    @Test
    fun `artist parsing`() {
        val artist = AMArtist.fromJSON(false, json)
        assertEquals(id, artist.artistId)
        assertEquals(name, artist.name)
        assert(artist.tinyImage!!.startsWith(image))
        assert(artist.smallImage!!.startsWith(image))
        assert(artist.mediumImage!!.startsWith(image))
        assert(artist.largeImage!!.startsWith(image))
        assertEquals(genre, artist.genre)
        assertEquals(hometown, artist.hometown)
        assertEquals(bio, artist.bio)
        assertEquals(url, artist.url)
        assertEquals(urlSlug, artist.urlSlug)
        assertEquals(label, artist.label)
        assertEquals(twitter, artist.twitter)
        assertEquals(facebook, artist.facebook)
        assertEquals(instagram, artist.instagram)
        assertEquals(false, artist.isVerified)
        assertEquals(uploadCountExcludingReups, artist.uploadsCount)
        assertEquals(favorites.size, artist.favoritesCount)
        assertEquals(playlists.size, artist.playlistsCount)
        assertEquals(followingCount, artist.followingCount)
        assertEquals(followersCount, artist.followersCount)
        assertEquals(feedCount, artist.feedCount)
        assertEquals("Oct '19", artist.getCreated())
        assertEquals(false, artist.isAdmin)
        assertEquals(banner, artist.banner)
        assertEquals(youtube, artist.youtube)
        assertEquals(reups.size, artist.reupsCount)
        assertEquals(pinned.size, artist.pinnedCount)
        assertEquals(canComment, artist.canComment)
        assertEquals(playsCount, artist.playsCount)
    }

    @Test
    fun `artist parsing, not admin, verified field`() {
        val verifiedArtist = AMArtist.fromJSON(false, json.apply {
            put("verified", "yes")
        })

        val tastemakerArtist = AMArtist.fromJSON(false, json.apply {
            put("verified", "tastemaker")
        })

        assertEquals(true, verifiedArtist.isVerified)
        assertEquals(false, verifiedArtist.isTastemaker)

        assertEquals(false, tastemakerArtist.isVerified)
        assertEquals(true, tastemakerArtist.isTastemaker)
    }
}
