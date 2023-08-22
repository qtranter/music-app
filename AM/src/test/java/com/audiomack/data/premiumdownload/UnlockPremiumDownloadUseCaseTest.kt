package com.audiomack.data.premiumdownload

import com.audiomack.data.api.MusicDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventDownload
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class UnlockPremiumDownloadUseCaseTest {

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    @Mock
    private lateinit var eventBus: EventBus

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var sut: UnlockPremiumDownloadUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        schedulersProvider = TestSchedulersProvider()

        sut = UnlockPremiumDownloadUseCase(
            musicDataSource,
            eventBus,
            schedulersProvider
        )
    }

    @After
    fun tearDown() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `unlock song`() {
        val id = "123"
        val song = mock<AMResultItem> {
            on { itemId } doReturn id
            on { isAlbum } doReturn false
            on { tracks } doReturn null
        }
        whenever(musicDataSource.getOfflineItem(any())).thenReturn(Single.just(song))
        whenever(musicDataSource.markFrozenDownloads(any(), any())).doReturn(Completable.complete())

        sut.unlockFrozenDownload(id)

        verify(musicDataSource).getOfflineItem(id)
        verify(musicDataSource).markFrozenDownloads(false, listOf(id))
        verify(eventBus).post(argWhere { it is EventDownload })
    }

    @Test
    fun `unlock album with all tracks, fails`() {
        val id = "1"
        val trackId = "123"
        val track = mock<AMResultItem> {
            on { itemId } doReturn trackId
            on { tracks } doReturn emptyList()
        }
        val album = mock<AMResultItem> {
            on { itemId } doReturn id
            on { isAlbum } doReturn true
            on { tracks } doReturn listOf(track)
        }
        whenever(musicDataSource.getOfflineItem(any())).thenReturn(Single.just(album))
        whenever(musicDataSource.markFrozenDownloads(any(), any())).doReturn(Completable.error(Exception("")))

        sut.unlockFrozenDownload(id)

        verify(musicDataSource).getOfflineItem(id)
        verify(musicDataSource).markFrozenDownloads(false, listOf(trackId))
        verify(eventBus, never()).post(argWhere { it is EventDownload })
    }

    @Test
    fun `unlock fails because of non existing music`() {
        val id = "123"
        whenever(musicDataSource.getOfflineItem(any())).thenReturn(Single.error(Exception("")))

        sut.unlockFrozenDownload(id)

        verify(musicDataSource).getOfflineItem(id)
        verify(musicDataSource, never()).markFrozenDownloads(false, listOf(id))
        verify(eventBus, never()).post(argWhere { it is EventDownload })
    }
}
