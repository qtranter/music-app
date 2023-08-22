package com.audiomack.ui.mylibrary.offline.edit

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.api.MusicDataSource
import com.audiomack.download.MusicDownloader
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventDeletedDownload
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.mylibrary.offline.local.AddLocalMediaExclusionUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.subjects.PublishSubject
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class EditDownloadsViewModelTest {

    @Mock
    private lateinit var musicRepository: MusicDataSource

    @Mock
    private lateinit var musicDownloader: MusicDownloader

    @Mock
    private lateinit var eventBus: EventBus

    @Mock
    private lateinit var addLocalMediaExclusionUseCase: AddLocalMediaExclusionUseCase

    private lateinit var viewModel: EditDownloadsViewModel

    private lateinit var schedulersProvider: SchedulersProvider

    @Mock
    private lateinit var closeObserver: Observer<Void>

    @Mock
    private lateinit var showMusicListObserver: Observer<List<AMResultItem>>

    @Mock
    private lateinit var removeSelectedMusicObserver: Observer<Void>

    @Mock
    private lateinit var removeButtonEnabledObserver: Observer<Boolean>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val offlineItemsSubject = PublishSubject.create<List<AMResultItem>>()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()

        whenever(musicRepository.getOfflineItems(any(), any())).thenReturn(offlineItemsSubject)

        viewModel = EditDownloadsViewModel(musicRepository, musicDownloader, addLocalMediaExclusionUseCase, schedulersProvider, eventBus).apply {
            closeEvent.observeForever(closeObserver)
            showMusicListEvent.observeForever(showMusicListObserver)
            removeSelectedMusicEvent.observeForever(removeSelectedMusicObserver)
            removeButtonEnabled.observeForever(removeButtonEnabledObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `music list event on emission from music repo`() {
        offlineItemsSubject.onNext(listOf())
        verify(musicRepository).getOfflineItems(any(), any())
        verify(showMusicListObserver).onChanged(any())
    }

    @Test
    fun `close click observed`() {
        viewModel.onCloseButtonClick()
        verify(closeObserver).onChanged(null)
    }

    @Test
    fun `remove button click observed`() {
        val music = mock<AMResultItem>()
        val musicList = listOf(music, music, music)
        whenever(musicRepository.deleteMusicFromDB(any())).thenReturn(Completable.complete())

        viewModel.onSelectionChanged(musicList)
        viewModel.onRemoveButtonClick()

        verify(musicRepository, times(musicList.size)).deleteMusicFromDB(any())
        verify(removeSelectedMusicObserver).onChanged(null)
        verify(eventBus).post(argWhere { it is EventDownloadsEdited })
    }

    @Test
    fun `music removed`() {
        val music = mock<AMResultItem>()
        whenever(musicRepository.deleteMusicFromDB(music)).thenReturn(Completable.complete())
        viewModel.onMusicRemoved(music)
        verify(musicRepository).deleteMusicFromDB(music)
        verify(eventBus).post(argWhere { (it as EventDeletedDownload).item == music })
    }

    @Test
    fun `selection changed`() {
        val musicList = emptyList<AMResultItem>()
        viewModel.onSelectionChanged(musicList)
        verify(removeButtonEnabledObserver).onChanged(false)
    }

    @Test
    fun `downloads completely removed`() {
        viewModel.onDownloadsCompletelyRemoved()
        verify(closeObserver).onChanged(null)
    }
}
