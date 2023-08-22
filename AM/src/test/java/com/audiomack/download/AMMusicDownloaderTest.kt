package com.audiomack.download

import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.storage.Storage
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.network.APIInterface
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class AMMusicDownloaderTest {

    @Mock
    private lateinit var httpDownloader: MusicHttpDownloader

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var trackingDataSource: TrackingDataSource

    @Mock
    private lateinit var storage: Storage

    @Mock
    private lateinit var apiDownloads: APIInterface.DownloadsInterface

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    private val musicDownloader: MusicDownloader by lazy {
        AMMusicDownloader.getInstance(
            httpDownloader,
            mixpanelDataSource,
            trackingDataSource,
            storage,
            apiDownloads,
            musicDataSource
        )
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `empty state`() {
        assertFalse(musicDownloader.isMusicBeingDownloaded(mock { on { itemId } doReturn "123" }))
        assertFalse(musicDownloader.isMusicWaitingForDownload(mock { on { itemId } doReturn "123" }))
    }

    @Test
    fun `cache images`() {
        GlobalScope.launch {
            musicDownloader.cacheImages(
                mock {
                    on { itemId } doReturn "123"
                    on { getImageURLWithPreset(any()) } doReturn "https://bla"
                    on { banner } doReturn "https://blabla"
                }
            )
            verify(httpDownloader.download(any(), any()), times(3))
        }
    }

    @Test
    fun `cache images, invalid id`() {
        GlobalScope.launch {
            musicDownloader.cacheImages(
                mock {
                    on { itemId } doReturn "123"
                    on { getImageURLWithPreset(any()) } doReturn "https://bla"
                    on { banner } doReturn "https://blabla"
                }
            )
            verify(httpDownloader.download(any(), any()), times(0))
        }
    }
}
