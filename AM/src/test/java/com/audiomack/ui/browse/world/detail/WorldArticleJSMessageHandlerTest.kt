package com.audiomack.ui.browse.world.detail

import com.audiomack.TestApplication
import com.audiomack.data.deeplink.Deeplink
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import org.junit.After
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
class WorldArticleJSMessageHandlerTest {

    private lateinit var sut: WorldArticleJSMessageHandlerImpl
    private val mixpanelSource = MixpanelSource.empty

    @Before
    fun setup() {
        sut = WorldArticleJSMessageHandlerImpl()
    }

    @After
    fun tearDown() {}

    @Test
    fun `play song deeplink`() {
        assertEquals(Deeplink.Song("mdoto/popfuture"), sut.parseMessage(
            """
            {
                "action": "play",
                "data": {
                    "artistSlug": "mdoto",
                    "musicSlug": "popfuture",
                    "musicType": "song"
                }
            }
            """.trimIndent(), mixpanelSource))
    }

    @Test
    fun `play album deeplink`() {
        assertEquals(Deeplink.AlbumPlay("mdoto/popfuture", mixpanelSource), sut.parseMessage(
            """
            {
                "action": "play",
                "data": {
                    "artistSlug": "mdoto",
                    "musicSlug": "popfuture",
                    "musicType": "album"
                }
            }
            """.trimIndent(), mixpanelSource))
    }

    @Test
    fun `play playlist deeplink`() {
        assertEquals(Deeplink.PlaylistPlay("mdoto/popfuture", mixpanelSource), sut.parseMessage(
            """
            {
                "action": "play",
                "data": {
                    "artistSlug": "mdoto",
                    "musicSlug": "popfuture",
                    "musicType": "playlist"
                }
            }
            """.trimIndent(), mixpanelSource))
    }

    @Test
    fun `add to queue song deeplink`() {
        assertEquals(Deeplink.AddToQueue("mdoto/popfuture", MusicType.Song, mixpanelSource), sut.parseMessage(
            """
            {
                "action": "add-to-queue",
                "data": {
                    "artistSlug": "mdoto",
                    "musicSlug": "popfuture",
                    "musicType": "song"
                }
            }
            """.trimIndent(), mixpanelSource))
    }

    @Test
    fun `add to queue album deeplink`() {
        assertEquals(Deeplink.AddToQueue("mdoto/popfuture", MusicType.Album, mixpanelSource), sut.parseMessage(
            """
            {
                "action": "add-to-queue",
                "data": {
                    "artistSlug": "mdoto",
                    "musicSlug": "popfuture",
                    "musicType": "album"
                }
            }
            """.trimIndent(), mixpanelSource))
    }

    @Test
    fun `add to queue playlist deeplink`() {
        assertEquals(Deeplink.AddToQueue("mdoto/popfuture", MusicType.Playlist, mixpanelSource), sut.parseMessage(
            """
            {
                "action": "add-to-queue",
                "data": {
                    "artistSlug": "mdoto",
                    "musicSlug": "popfuture",
                    "musicType": "playlist"
                }
            }
            """.trimIndent(), mixpanelSource))
    }

    @Test
    fun `invalid deeplink`() {

        // Invalid musicType
        assertEquals(null, sut.parseMessage(
            """
            {
                "action": "play",
                "data": {
                    "artistSlug": "mdoto",
                    "musicSlug": "popfuture",
                    "musicType": "lyric"
                }
            }
            """.trimIndent(), mixpanelSource))

        // Missing musicType
        assertEquals(null, sut.parseMessage(
            """
            {
                "action": "add-to-queue",
                "data": {
                    "artistSlug": "mdoto",
                    "musicSlug": "popfuture"
                }
            }
            """.trimIndent(), mixpanelSource))

        // Invalid action
        assertEquals(null, sut.parseMessage(
            """
            {
                "action": "repost",
                "data": {
                    "artistSlug": "mdoto",
                    "musicSlug": "popfuture",
                    "musicType": "playlist"
                }
            }
            """.trimIndent(), mixpanelSource))

        // Missing action
        assertEquals(null, sut.parseMessage(
            """
            {
                "data": {
                    "artistSlug": "mdoto",
                    "musicSlug": "popfuture",
                    "musicType": "playlist"
                }
            }
            """.trimIndent(), mixpanelSource))

        // Missing data
        assertEquals(null, sut.parseMessage(
            """
            {
                "action": "add-to-queue"
            }
            """.trimIndent(), mixpanelSource))

        // Empty json
        assertEquals(null, sut.parseMessage("{}", mixpanelSource))

        // Malformed json
        assertEquals(null, sut.parseMessage("{", mixpanelSource))

        // Empty string
        assertEquals(null, sut.parseMessage("", mixpanelSource))
    }
}
