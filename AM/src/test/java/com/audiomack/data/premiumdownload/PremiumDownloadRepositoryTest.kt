package com.audiomack.data.premiumdownload

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.data.database.MusicDAO
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.download.MusicDownloader
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class PremiumDownloadRepositoryTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Mock
    private lateinit var musicDAO: MusicDAO

    private lateinit var schedulersProvider: SchedulersProvider

    @Mock
    private lateinit var musicDownloader: MusicDownloader

    @Mock
    private lateinit var eventBus: EventBus

    private lateinit var sut: PremiumDownloadRepository

    private lateinit var premiumSubject: BehaviorSubject<Boolean>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        schedulersProvider = TestSchedulersProvider()
        premiumSubject = BehaviorSubject.create()

        whenever(premiumDataSource.premiumObservable).thenReturn(premiumSubject)

        sut = PremiumDownloadRepository.getInstance(
            premiumDataSource,
            musicDAO,
            schedulersProvider,
            musicDownloader,
            eventBus
        )
    }

    @After
    fun tearDown() {
        PremiumDownloadRepository.destroy()
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `observed unsubscribed status, freeze some tracks`() {
        val ids = (1 until 26).map { it.toString() }
        whenever(musicDAO.premiumLimitedUnfrozenDownloadCountAsync()).thenReturn(Single.just(ids.size))
        whenever(musicDAO.getPremiumLimitedSongs()).thenReturn(Single.just(ids))
        whenever(musicDAO.markFrozen(any(), any())).thenReturn(Completable.complete())
        premiumSubject.onNext(false)
        verify(musicDAO).premiumLimitedUnfrozenDownloadCountAsync()
        verify(musicDAO).getPremiumLimitedSongs()
        verify(musicDAO).markFrozen(eq(true), argWhere { it.size == 5 && it.last() == "5" })
        verify(eventBus).post(argWhere { it is EventDownloadsEdited })
    }

    @Test
    fun `observed unsubscribed status, do not freeze anything because there are less than 20 unfrozen tracks`() {
        val ids = listOf("1", "2", "3")
        whenever(musicDAO.premiumLimitedUnfrozenDownloadCountAsync()).thenReturn(Single.just(10))
        whenever(musicDAO.getPremiumLimitedSongs()).thenReturn(Single.just(ids))
        whenever(musicDAO.markFrozen(any(), any())).thenReturn(Completable.complete())
        premiumSubject.onNext(false)
        verify(musicDAO).premiumLimitedUnfrozenDownloadCountAsync()
        verify(musicDAO, never()).getPremiumLimitedSongs()
        verify(musicDAO, never()).markFrozen(false, ids)
        verify(eventBus, never()).post(argWhere { it is EventDownloadsEdited })
    }

    @Test
    fun `observed subscribed status, freeze some tracks`() {
        val ids = listOf("1", "2", "3")
        whenever(musicDAO.getPremiumLimitedSongs()).thenReturn(Single.just(ids))
        whenever(musicDAO.markFrozen(any(), any())).thenReturn(Completable.complete())
        premiumSubject.onNext(true)
        verify(musicDAO, never()).premiumLimitedUnfrozenDownloadCountAsync()
        verify(musicDAO).getPremiumLimitedSongs()
        verify(musicDAO).markFrozen(false, ids)
        verify(eventBus).post(argWhere { it is EventDownloadsEdited })
    }

    @Test
    fun `download limit`() {
        assert(sut.premiumDownloadLimit == 20)
    }

    @Test
    fun `remaining premium limited count`() {
        whenever(musicDAO.premiumLimitedUnfrozenDownloadCount()).thenReturn(15)
        assert(sut.remainingPremiumLimitedDownloadCount == 5)
    }

    @Test
    fun `frozen count for frozen song`() {
        val music = mock<AMResultItem> {
            on { isSong } doReturn true
            on { isDownloadFrozen } doReturn true
        }
        assert(sut.getFrozenCount(music) == 1)
    }

    @Test
    fun `frozen count for frozen album track`() {
        val music = mock<AMResultItem> {
            on { isAlbumTrack } doReturn true
            on { isDownloadFrozen } doReturn true
        }
        assert(sut.getFrozenCount(music) == 1)
    }

    @Test
    fun `frozen count for not frozen playlist track`() {
        val music = mock<AMResultItem> {
            on { isPlaylistTrack } doReturn true
            on { isDownloadFrozen } doReturn false
        }
        assert(sut.getFrozenCount(music) == 0)
    }

    @Test
    fun `frozen count for album`() {
        val track = mock<AMResultItem> {
            on { isDownloadFrozen } doReturn true
        }
        val music = mock<AMResultItem> {
            on { isPlaylist } doReturn true
            on { tracks } doReturn listOf(track, track)
        }
        assert(sut.getFrozenCount(music) == 2)
    }

    @Test
    fun `frozen count for empty playlist`() {
        val music = mock<AMResultItem> {
            on { isPlaylist } doReturn true
            on { tracks } doReturn emptyList()
        }
        assert(sut.getFrozenCount(music) == 0)
    }

    @Test
    fun `count of songs to be downloaded for limited song`() {
        val music = mock<AMResultItem> {
            on { isSong } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
        }
        assert(sut.getToBeDownloadedPremiumLimitedCount(music) == 1)
    }

    @Test
    fun `count of songs to be downloaded for premium album track`() {
        val music = mock<AMResultItem> {
            on { isAlbumTrack } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Premium
        }
        assert(sut.getToBeDownloadedPremiumLimitedCount(music) == 0)
    }

    @Test
    fun `count of songs to be downloaded for free playlist track`() {
        val music = mock<AMResultItem> {
            on { isPlaylistTrack } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }
        assert(sut.getToBeDownloadedPremiumLimitedCount(music) == 0)
    }

    @Test
    fun `count of songs to be downloaded for album`() {
        val track = mock<AMResultItem> {
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
            on { isDownloadCompleted } doReturn false
        }
        val music = mock<AMResultItem> {
            on { isPlaylist } doReturn true
            on { tracks } doReturn listOf(track, track)
        }
        assert(sut.getToBeDownloadedPremiumLimitedCount(music) == 2)
    }

    @Test
    fun `count of songs to be downloaded for empty playlist`() {
        val music = mock<AMResultItem> {
            on { isPlaylist } doReturn true
            on { tracks } doReturn emptyList()
        }
        assert(sut.getToBeDownloadedPremiumLimitedCount(music) == 0)
    }

    @Test
    fun `can download free music`() {
        val music = mock<AMResultItem> {
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }
        assert(sut.canDownloadMusicBasedOnPremiumLimitedCount(music))
    }

    @Test
    fun `can download premium limited music, user is premium`() {
        val music = mock<AMResultItem> {
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
        }
        whenever(premiumDataSource.isPremium).thenReturn(true)
        assert(sut.canDownloadMusicBasedOnPremiumLimitedCount(music))
    }

    @Test
    fun `cannot download premium-only music`() {
        val music = mock<AMResultItem> {
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Premium
        }
        assert(!sut.canDownloadMusicBasedOnPremiumLimitedCount(music))
    }

    @Test
    fun `cannot download premium limited frozen music`() {
        val music = mock<AMResultItem> {
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
            on { isDownloadFrozen } doReturn true
        }
        assert(!sut.canDownloadMusicBasedOnPremiumLimitedCount(music))
    }

    @Test
    fun `cannot download premium limited music, no more downloads available`() {
        val music = mock<AMResultItem> {
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
            on { isSong } doReturn true
        }
        whenever(musicDAO.premiumLimitedUnfrozenDownloadCount()).thenReturn(25)
        assert(!sut.canDownloadMusicBasedOnPremiumLimitedCount(music))
    }

    @Test
    fun `can download premium limited music, downloads available`() {
        val music = mock<AMResultItem> {
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
            on { isSong } doReturn true
            whenever(musicDAO.premiumLimitedUnfrozenDownloadCount()).thenReturn(19)
        }
        assert(sut.canDownloadMusicBasedOnPremiumLimitedCount(music))
    }
}
