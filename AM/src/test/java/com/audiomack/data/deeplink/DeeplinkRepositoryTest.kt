package com.audiomack.data.deeplink

import android.content.Intent
import android.net.Uri
import com.audiomack.TestApplication
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.BenchmarkType
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.WorldPage
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    application = TestApplication::class
)
class DeeplinkRepositoryTest {

    @Mock private lateinit var remoteVariablesProvider: RemoteVariablesProvider

    private lateinit var sut: DeeplinkRepository

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        whenever(remoteVariablesProvider.deeplinksPathsBlacklist).thenReturn(listOf("world", "about", "dashboard", "upload", "contact-us", "creators", "yourvoiceyourchoice", "recent", "premium", "support")) // Also added paths already handled here
        sut = DeeplinkRepository.getInstance(remoteVariablesProvider)
        sut.duplicateDeeplinkCheckEnabled = false
    }

    @After
    fun tearDown() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `null data`() {
        assertEquals(sut.obtainDeeplink(null), null)
    }

    @Test
    fun `invalid data`() {
        assertEquals(sut.obtainDeeplink(Intent()), null)
    }

    @Test
    fun `shortcut offline`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { putExtra("shortcut", "offline") }), Deeplink.MyDownloads)
    }

    @Test
    fun `shortcut top songs`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { putExtra("shortcut", "topSongs") }), Deeplink.TopSongs())
    }

    @Test
    fun `shortcut favorites`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { putExtra("shortcut", "favorites") }), Deeplink.MyFavorites)
    }

    @Test
    fun `shortcut playlists`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { putExtra("shortcut", "playlists") }), Deeplink.MyPlaylists)
    }

    @Test
    fun `shortcut invalid`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { putExtra("shortcut", "invalid") }), null)
    }

    @Test
    fun `artist favorites`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artist_favorites/matteinn") }), Deeplink.ArtistFavorites("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://matteinn/favorites") }), Deeplink.ArtistFavorites("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/matteinn/favorites") }), Deeplink.ArtistFavorites("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/matteinn/favorites") }), Deeplink.ArtistFavorites("matteinn"))
    }

    @Test
    fun `artist playlists`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artist_playlists/matteinn") }), Deeplink.ArtistPlaylists("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://matteinn/playlists") }), Deeplink.ArtistPlaylists("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/matteinn/playlists") }), Deeplink.ArtistPlaylists("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/matteinn/playlists") }), Deeplink.ArtistPlaylists("matteinn"))
    }

    @Test
    fun `artist uploads`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artist_uploads/matteinn") }), Deeplink.ArtistUploads("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://matteinn/uploads") }), Deeplink.ArtistUploads("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/matteinn/uploads") }), Deeplink.ArtistUploads("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/matteinn/uploads") }), Deeplink.ArtistUploads("matteinn"))
    }

    @Test
    fun `artist followers`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artist_followers/matteinn") }), Deeplink.ArtistFollowers("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://matteinn/followers") }), Deeplink.ArtistFollowers("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/matteinn/followers") }), Deeplink.ArtistFollowers("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/matteinn/followers") }), Deeplink.ArtistFollowers("matteinn"))
    }

    @Test
    fun `my favorites`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artist_favorites") }), Deeplink.MyFavorites)
    }

    @Test
    fun `my playlists`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artist_playlists") }), Deeplink.MyPlaylists)
    }

    @Test
    fun `my uploads`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artist_uploads") }), Deeplink.MyUploads)
    }

    @Test
    fun `my followers`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artist_followers") }), Deeplink.MyFollowers)
    }

    @Test
    fun `my following`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artist_following") }), Deeplink.MyFollowing)
    }

    @Test
    fun `my downloads`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artist_downloads") }), Deeplink.MyDownloads)
    }

    @Test
    fun `artist details`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artist/matteinn") }), Deeplink.Artist("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://matteinn") }), Deeplink.Artist("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/artist/matteinn") }), Deeplink.Artist("matteinn"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/matteinn") }), Deeplink.Artist("matteinn"))
    }

    @Test
    fun `home screen`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://home") }), Deeplink.Home)
    }

    @Test
    fun `browse music_trending`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://music_trending") }), Deeplink.Trending())
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/trending-now") }), Deeplink.Trending())
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/trending-now") }), Deeplink.Trending())
    }

    @Test
    fun `browse world`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://world") }), Deeplink.World)
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/world") }), Deeplink.World)
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/world") }), Deeplink.World)
    }

    @Test
    fun `world pages`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://world/features") }), Deeplink.WorldPage(WorldPage("features", "hash-features")))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/world/features") }), Deeplink.WorldPage(WorldPage("features", "hash-features")))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/world/features") }), Deeplink.WorldPage(WorldPage("features", "hash-features")))
    }

    @Test
    fun `world tags`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://world/tag/kemba") }), Deeplink.WorldPage(WorldPage("Kemba", "kemba")))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/world/tag/kemba") }), Deeplink.WorldPage(WorldPage("Kemba", "kemba")))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/world/tag/kemba") }), Deeplink.WorldPage(WorldPage("Kemba", "kemba")))
    }

    @Test
    fun `world post`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://world/post/kemba-interview") }), Deeplink.WorldPost("kemba-interview"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/world/post/kemba-interview") }), Deeplink.WorldPost("kemba-interview"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/world/post/kemba-interview") }), Deeplink.WorldPost("kemba-interview"))
    }

    @Test
    fun `browse music_{genre}_trending`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://music_dancehall_trending") }), Deeplink.Trending("dancehall"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://music_undefined_trending") }), null)
    }

    @Test
    fun `browse music_songs`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://music_songs") }), Deeplink.TopSongs())
    }

    @Test
    fun `browse music_{genre}_songs`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://music_electronic_songs") }), Deeplink.TopSongs("electronic"))
    }

    @Test
    fun `browse music_albums`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://music_albums") }), Deeplink.TopAlbums())
    }

    @Test
    fun `browse music_{genre}_albums`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://music_rap_albums") }), Deeplink.TopAlbums("rap"))
    }

    @Test
    fun `recently added`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://recent") }), Deeplink.RecentlyAdded())
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://music_recent") }), Deeplink.RecentlyAdded())
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/recent") }), Deeplink.RecentlyAdded())
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/recent") }), Deeplink.RecentlyAdded())
    }

    @Test
    fun `playlists screen`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://playlists") }), Deeplink.Playlists())
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://playlists_browse") }), Deeplink.Playlists())
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://playlists/browse") }), Deeplink.Playlists())
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/playlists/browse") }), Deeplink.Playlists())
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/playlists/browse") }), Deeplink.Playlists())
    }

    @Test
    fun `playlists screen with category deeplink`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://playlists/browse/verified") }), Deeplink.Playlists("verified"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://playlists/browse/lo-fi") }), Deeplink.Playlists("lo-fi"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/playlists/browse/verified") }), Deeplink.Playlists("verified"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/playlists/browse/lo-fi") }), Deeplink.Playlists("lo-fi"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/playlists/browse/verified") }), Deeplink.Playlists("verified"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/playlists/browse/lo-fi") }), Deeplink.Playlists("lo-fi"))
    }

    @Test
    fun `song details`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://song/matteinn/sample") }), Deeplink.Song("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://matteinn/song/sample") }), Deeplink.Song("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/song/matteinn/sample") }), Deeplink.Song("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/matteinn/song/sample") }), Deeplink.Song("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/song/matteinn/sample") }), Deeplink.Song("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/matteinn/song/sample") }), Deeplink.Song("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://www.audiomack.com/song/matteinn/sample") }), Deeplink.Song("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://www.audiomack.com/matteinn/song/sample") }), Deeplink.Song("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://dcf.audiomack.com/song/matteinn/sample") }), Deeplink.Song("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://dcf.audiomack.com/matteinn/song/sample") }), Deeplink.Song("matteinn/sample"))
    }

    @Test
    fun `album details`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://album/matteinn/sample") }), Deeplink.Album("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://matteinn/album/sample") }), Deeplink.Album("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/album/matteinn/sample") }), Deeplink.Album("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/matteinn/album/sample") }), Deeplink.Album("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/album/matteinn/sample") }), Deeplink.Album("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/matteinn/album/sample") }), Deeplink.Album("matteinn/sample"))
    }

    @Test
    fun `playlist details`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://playlist/matteinn/sample") }), Deeplink.Playlist("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://matteinn/playlist/sample") }), Deeplink.Playlist("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/playlist/matteinn/sample") }), Deeplink.Playlist("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/matteinn/playlist/sample") }), Deeplink.Playlist("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/playlist/matteinn/sample") }), Deeplink.Playlist("matteinn/sample"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/matteinn/playlist/sample") }), Deeplink.Playlist("matteinn/sample"))
    }

    @Test
    fun `premium screen`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://premium") }), Deeplink.Premium(InAppPurchaseMode.Deeplink))
    }

    @Test
    fun `suggested follows`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://suggested_follows") }), Deeplink.SuggestedFollows)
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://artists/popular") }), Deeplink.SuggestedFollows)
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/artists/popular") }), Deeplink.SuggestedFollows)
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/artists/popular") }), Deeplink.SuggestedFollows)
    }

    @Test
    fun `support screen`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://support") }), Deeplink.Support)
    }

    @Test
    fun `support ticket`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://support/asd") }), Deeplink.SupportTicket("asd"))
    }

    @Test
    fun `now playing`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://nowplaying") }), Deeplink.NowPlaying)
    }

    @Test
    fun benchmark() {
        val entityType = "song"
        val entityId = "artist/title"
        val benchmarkType = "plays"
        val benchmarkMilestone = 1000L
        val deeplink = "audiomack://benchmark/$entityType/$entityId/$benchmarkType/$benchmarkMilestone"
        assertEquals(sut.obtainDeeplink(Intent().apply {
            putExtra("am_deeplink", deeplink)
        }), Deeplink.Benchmark(entityId, entityType, BenchmarkModel(BenchmarkType.fromString(benchmarkType), null, benchmarkMilestone)))
        assertEquals(sut.obtainDeeplink(Intent().apply {
            data = Uri.parse(deeplink)
        }), Deeplink.Benchmark(entityId, entityType, BenchmarkModel(BenchmarkType.fromString(benchmarkType), null, benchmarkMilestone)))
    }

    @Test
    fun `external link`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { putExtra("am_deeplink", "https://google.com") }), Deeplink.Link(Uri.parse("https://google.com")))
        assertEquals(sut.obtainDeeplink(Intent().apply { putExtra("am_deeplink", "https://google.com/store") }), Deeplink.Link(Uri.parse("https://google.com/store")))
        assertEquals(sut.obtainDeeplink(Intent().apply { putExtra("am_deeplink", "https://google.com/store/pixel") }), Deeplink.Link(Uri.parse("https://google.com/store/pixel")))
        assertEquals(sut.obtainDeeplink(Intent().apply { putExtra("am_deeplink", "https://www.twitch.tv/audiomack/") }), Deeplink.Link(Uri.parse("https://www.twitch.tv/audiomack/")))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://google.com") }), Deeplink.Link(Uri.parse("https://google.com")))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://google.com/store") }), Deeplink.Link(Uri.parse("https://google.com/store")))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://google.com/store/pixel") }), Deeplink.Link(Uri.parse("https://google.com/store/pixel")))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/about/privacy-policy") }), Deeplink.Link(Uri.parse("https://audiomack.com/about/privacy-policy")))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://www.twitch.tv/audiomack/") }), Deeplink.Link(Uri.parse("https://www.twitch.tv/audiomack/")))
    }

    @Test
    fun `audiomack links to be ignored`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/yourvoiceyourchoice") }), Deeplink.Link(Uri.parse("https://audiomack.com/yourvoiceyourchoice")))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/contact-us") }), Deeplink.Link(Uri.parse("https://audiomack.com/contact-us")))
    }

    @Test
    fun `comments and replies`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/polog/album/the-goat?comment=a5ff5670-9661-11ea-8a69-31ed55e94a05") }), Deeplink.Comments("polog/the-goat", "album", "a5ff5670-9661-11ea-8a69-31ed55e94a05"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/joevango/playlist/verified-rnb?comment=cab8f130-7e8c-11e9-a54d-221e2c0d0e7d") }), Deeplink.Comments("joevango/verified-rnb", "playlist", "cab8f130-7e8c-11e9-a54d-221e2c0d0e7d"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/6ix9ine/song/gooba?comment=068a65d0-9161-11ea-9923-5d19cd124627&thread=64945ee0-915e-11ea-9f2b-383ee17cff37") }), Deeplink.Comments("6ix9ine/gooba", "song", "068a65d0-9161-11ea-9923-5d19cd124627", "64945ee0-915e-11ea-9f2b-383ee17cff37"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/polog/album/the-goat?comment=a5ff5670-9661-11ea-8a69-31ed55e94a05") }), Deeplink.Comments("polog/the-goat", "album", "a5ff5670-9661-11ea-8a69-31ed55e94a05"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/joevango/playlist/verified-rnb?comment=cab8f130-7e8c-11e9-a54d-221e2c0d0e7d") }), Deeplink.Comments("joevango/verified-rnb", "playlist", "cab8f130-7e8c-11e9-a54d-221e2c0d0e7d"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/6ix9ine/song/gooba?comment=068a65d0-9161-11ea-9923-5d19cd124627&thread=64945ee0-915e-11ea-9f2b-383ee17cff37") }), Deeplink.Comments("6ix9ine/gooba", "song", "068a65d0-9161-11ea-9923-5d19cd124627", "64945ee0-915e-11ea-9f2b-383ee17cff37"))
    }

    @Test
    fun `playlist share`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://share/playlist/joevango/verified-rnb") }), Deeplink.PlaylistShare("joevango/verified-rnb"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://share/playlist/invalid") }), null)
    }

    @Test
    fun `album share`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://share/album/polog/the-goat") }), Deeplink.AlbumShare("polog/the-goat"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://share/album/invalid") }), null)
    }

    @Test
    fun `song share`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://share/song/6ix9ine/gooba") }), Deeplink.SongShare("6ix9ine/gooba"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://share/song/invalid") }), null)
    }

    @Test
    fun `artist share`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://share/artist/eminem") }), Deeplink.ArtistShare("eminem"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://share/artist") }), null)
    }

    @Test
    fun `notification center`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://notifications") }), Deeplink.Notifications)
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/notifications") }), Deeplink.Notifications)
    }

    @Test
    fun `email verification`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/?verifyHash=aaa&uId=bbb") }), Deeplink.EmailVerification("aaa"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com?verifyHash=aaa") }), Deeplink.EmailVerification("aaa"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com?verifyHash") }), Deeplink.Link(Uri.parse("https://audiomack.com?verifyHash")))
    }

    @Test
    fun `change password`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/edit/profile/password") }), Deeplink.ChangePassword)
    }

    @Test
    fun `reset password`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/?verifyPasswordToken=123") }), Deeplink.ResetPassword("123"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com?verifyPasswordToken=123") }), Deeplink.ResetPassword("123"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com?verifyPasswordToken") }), Deeplink.Link(Uri.parse("https://audiomack.com?verifyPasswordToken")))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/reset-password?verifyPasswordToken=123") }), Deeplink.ResetPassword("123"))
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/reset-password") }), Deeplink.Link(Uri.parse("https://audiomack.com/reset-password")))
    }

    @Test
    fun login() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://login") }), Deeplink.Login)
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("https://audiomack.com/login") }), Deeplink.Login)
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("http://audiomack.com/login") }), Deeplink.Login)
    }

    @Test
    fun `artists selection onboarding`() {
        assertEquals(sut.obtainDeeplink(Intent().apply { data = Uri.parse("audiomack://onboarding-artistselect") }), Deeplink.ArtistSelectOnboarding)
    }
}
