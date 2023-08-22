package com.audiomack.ui.mylibrary.offline.local.menu

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.data.music.local.LocalMediaDataSource
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.playback.Playback
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.home.AlertTriggers
import com.audiomack.ui.home.NavigationActions
import com.audiomack.ui.mylibrary.offline.local.AddLocalMediaExclusionUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class SlideUpMenuLocalMediaViewModelTest {

    @Mock
    private lateinit var localMedia: LocalMediaDataSource

    @Mock
    private lateinit var addLocalMediaExclusion: AddLocalMediaExclusionUseCase

    @Mock
    private lateinit var navigation: NavigationActions

    @Mock
    private lateinit var alertTriggers: AlertTriggers

    @Mock
    private lateinit var playback: Playback

    @Mock
    private lateinit var queue: QueueDataSource

    private lateinit var schedulers: SchedulersProvider

    private val mediaId = 123L

    private lateinit var mediaItem: AMResultItem

    private lateinit var viewModel: SlideUpMenuLocalMediaViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulers = TestSchedulersProvider()
        mediaItem = mock {
            on { isSong } doReturn true
        }
        whenever(localMedia.getTrack(mediaId)).thenReturn(Single.just(mediaItem))
        viewModel = SlideUpMenuLocalMediaViewModel(
            localMedia,
            addLocalMediaExclusion,
            navigation,
            alertTriggers,
            playback,
            queue,
            schedulers
        ).apply {
            id = mediaId
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `remove from queue`() {
        val index = 0
        whenever(queue.index).thenReturn(1)
        viewModel.onRemoveFromQueueClick(index)
        verify(queue).removeAt(eq(index))
        verify(queue, never()).skip(any())
    }

    @Test
    fun `remove the currently playing song, also skips song`() {
        val index = 0
        whenever(queue.index).thenReturn(0)
        viewModel.onRemoveFromQueueClick(index)
        verify(queue).removeAt(eq(index))
        verify(queue).skip(index)
    }

    @Test
    fun `hide media, successful`() {
        whenever(addLocalMediaExclusion.addExclusionFrom(mediaItem)).thenReturn(Single.just(emptyList()))
        viewModel.onHideClick()
        verify(navigation).navigateBack()
    }

    @Test
    fun `hide media, failure`() {
        whenever(addLocalMediaExclusion.addExclusionFrom(mediaItem)).thenReturn(Single.error(Exception()))
        viewModel.onHideClick()
        verify(alertTriggers).onGenericError()
        verify(navigation).navigateBack()
    }

    @Test
    fun `play next`() {
        viewModel.onPlayNextClick()
        verify(navigation).navigateBack()
        verify(playback).addQueue(any(), eq(QueueDataSource.CURRENT_INDEX))
    }

    @Test
    fun `add to queue`() {
        viewModel.onAddToQueueClick()
        verify(navigation).navigateBack()
        verify(playback).addQueue(any(), eq(null))
    }
}
