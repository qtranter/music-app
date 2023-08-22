package com.audiomack.ui.queue

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelPageQueue
import com.audiomack.model.AMResultItem
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.home.AlertTriggers
import com.audiomack.ui.home.NavigationActions
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class SlideUpMenuQueueViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var queueRepo: QueueDataSource

    @Mock private lateinit var alertTriggers: AlertTriggers

    @Mock private lateinit var navigationActions: NavigationActions

    private val schedulers = TestSchedulersProvider()

    private lateinit var viewModel: SlideUpMenuQueueViewModel

    private val orderedItemsObservable = PublishSubject.create<List<AMResultItem>>()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        whenever(queueRepo.orderedItems).thenReturn(orderedItemsObservable)

        viewModel =
            SlideUpMenuQueueViewModel(queueRepo, alertTriggers, navigationActions, schedulers)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun onSaveToPlaylistSuccess() {
        val item = mock<AMResultItem> {
            on { isLocal } doReturn false
        }
        orderedItemsObservable.onNext(listOf(item))

        viewModel.onSaveToPlaylistClick()

        verify(navigationActions, times(1))
            .launchAddToPlaylist(argWhere { it.mixpanelSource.page == MixpanelPageQueue })
        verify(alertTriggers, never()).onGenericError()
    }

    @Test
    fun onSaveToPlaylistError() {
        val item = mock<AMResultItem> {
            on { isLocal } doThrow RuntimeException()
        }
        orderedItemsObservable.onNext(listOf(item))

        viewModel.onSaveToPlaylistClick()

        verify(navigationActions, never()).launchAddToPlaylist(any())
        verify(alertTriggers, times(1)).onGenericError()
    }

    @Test
    fun onClearAllClick() {
        viewModel.onClearAllClick()
        verify(queueRepo, times(1)).clear()
        verify(navigationActions, times(1)).navigateBack()
    }

    @Test
    fun onClearUpcomingClick() {
        whenever(queueRepo.index).thenReturn(1)
        viewModel.onClearUpcomingClick()
        verify(queueRepo, times(1)).clear(2)
    }
}
