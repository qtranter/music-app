package com.audiomack.ui.replacedownload

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.database.MusicDAOException
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import com.audiomack.model.PremiumDownloadInfoModel
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.PremiumDownloadMusicModel
import com.audiomack.model.PremiumDownloadStatsModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class ReplaceDownloadViewModelTest {

    @Mock
    lateinit var musicDataSource: MusicDataSource

    @Mock
    lateinit var actionsDataSource: ActionsDataSource

    lateinit var schedulersProvider: SchedulersProvider

    @Mock
    lateinit var eventBus: EventBus

    lateinit var viewModel: ReplaceDownloadViewModel

    lateinit var data: PremiumDownloadModel

    lateinit var tracks: List<AMResultItem>

    @Mock
    lateinit var openDownloadsObserver: Observer<Void>

    @Mock
    lateinit var closeObserver: Observer<Void>

    @Mock
    lateinit var showHUDObserver: Observer<ProgressHUDMode>

    @Mock
    lateinit var subtitleTextObserver: Observer<Int>

    @Mock
    lateinit var replaceTextDataObserver: Observer<PremiumDownloadInfoModel>

    @Mock
    lateinit var itemsObserver: Observer<List<AMResultItem>>

    @Mock
    lateinit var itemsSelectedObserver: Observer<List<AMResultItem>>

    @Mock
    private lateinit var showUnlockedToastEventObserver: Observer<String>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        schedulersProvider = TestSchedulersProvider()

        data = PremiumDownloadModel(
            music = PremiumDownloadMusicModel("123", MusicType.Song, 1, emptyList()),
            stats = PremiumDownloadStatsModel("", MixpanelSource.empty, 20, 5)
        )

        val track1 = mock<AMResultItem> { on { itemId } doReturn "1" }
        val track2 = mock<AMResultItem> { on { itemId } doReturn "2" }
        tracks = listOf(track1, track2)
        whenever(musicDataSource.savedPremiumLimitedUnfrozenTracks(any(), any())).thenReturn(Single.just(tracks))

        viewModel = ReplaceDownloadViewModel(
            musicDataSource,
            actionsDataSource,
            schedulersProvider,
            eventBus
        ).apply {
            openDownloadsEvent.observeForever(openDownloadsObserver)
            closeEvent.observeForever(closeObserver)
            showHUDEvent.observeForever(showHUDObserver)
            subtitleText.observeForever(subtitleTextObserver)
            replaceTextData.observeForever(replaceTextDataObserver)
            items.observeForever(itemsObserver)
            itemsSelected.observeForever(itemsSelectedObserver)
            showUnlockedToastEvent.observeForever(showUnlockedToastEventObserver)

            init(data)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `observe items`() {
        verify(itemsObserver).onChanged(argWhere { it.size == 2 })
    }

    @Test
    fun `close click observed`() {
        viewModel.onCloseClick()
        verify(closeObserver).onChanged(null)
    }

    @Test
    fun `replace click observed, music was already saved in DB`() {
        val musicId = "123"
        val musicTitle = "title"
        val localMusic = mock<AMResultItem> {
            on { itemId } doReturn musicId
            on { title } doReturn musicTitle
        }
        whenever(musicDataSource.getOfflineItem(musicId)).thenReturn(Single.just(localMusic))
        whenever(musicDataSource.deleteMusicFromDB(any())).thenReturn(Completable.complete())
        whenever(musicDataSource.markFrozenDownloads(any(), any())).thenReturn(Completable.complete())

        viewModel.onReplaceClick()

        verify(showHUDObserver, times(2)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDObserver, times(2)).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDObserver, never()).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(closeObserver, times(1)).onChanged(null)
        verify(eventBus, times(1)).post(argWhere { it is EventDownloadsEdited })
        verify(showUnlockedToastEventObserver).onChanged(musicTitle)
    }

    @Test
    fun `replace click observed, music wasn't already saved in DB`() {
        val musicId = "123"
        val remoteMusic = mock<AMResultItem> {
            on { itemId } doReturn musicId
        }
        whenever(musicDataSource.getOfflineItem(musicId)).thenReturn(Single.error(MusicDAOException("")))
        whenever(musicDataSource.getMusicInfo(any(), any())).thenReturn(Observable.just(remoteMusic))
        whenever(musicDataSource.deleteMusicFromDB(any())).thenReturn(Completable.complete())
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.just(ToggleDownloadResult.DownloadStarted))

        viewModel.onReplaceClick()

        verify(showHUDObserver, times(2)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDObserver, times(2)).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDObserver, never()).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(closeObserver, times(1)).onChanged(null)
        verify(eventBus, times(1)).post(argWhere { it is EventDownloadsEdited })
    }

    @Test
    fun `toggle selection on`() {
        viewModel.onSongClick(tracks.first(), false)
        verify(itemsSelectedObserver).onChanged(argWhere { it.size == 1 })
        verify(replaceTextDataObserver).onChanged(argWhere { it.selectedCount == 1 })
    }

    @Test
    fun `toggle selection off`() {
        viewModel.onSongClick(tracks.first(), true)
        verify(itemsSelectedObserver).onChanged(argWhere { it.isEmpty() })
        verify(replaceTextDataObserver, times(2)).onChanged(argWhere { it.selectedCount == 0 }) // It also happens once on init
    }
}
