package com.audiomack.ui.queue

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.playback.Playback
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class QueueViewModelTest {

    @Mock
    private lateinit var preferencesDataSource: PreferencesDataSource

    @Mock
    private lateinit var queueDataSource: QueueDataSource

    @Mock
    private lateinit var playback: Playback

    private lateinit var viewModel: QueueViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        viewModel = QueueViewModel(preferencesDataSource, queueDataSource, playback)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun back() {
        val observer: Observer<Void> = mock()
        viewModel.backEvent.observeForever(observer)
        viewModel.onBackTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun options() {
        val item = mock<AMResultItem>()
        val index = 0
        val observer: Observer<Pair<AMResultItem, Int>> = mock()
        viewModel.showOptionsEvent.observeForever(observer)
        viewModel.didTapKebab(item, index)
        verify(observer).onChanged(any())
    }

    @Test
    fun queueChanged() {
        val observer: Observer<List<AMResultItem>> = mock()
        viewModel.queue.observeForever(observer)
        viewModel.queueDataObserver.onNext(listOf())
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun songChanged() {
        val observer: Observer<Void> = mock()
        viewModel.refreshData.observeForever(observer)
        viewModel.queueIndexObserver.onNext(0)
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun songTapped() {
        viewModel.onSongTapped(7)
        verify(playback).skip(eq(7))
    }

    @Test
    fun songRemoved() {
        viewModel.onSongDeleted(7)
        verify(queueDataSource).removeAt(eq(7))
    }

    @Test
    fun songMoved() {
        viewModel.onSongMoved(7, 1)
        verify(queueDataSource).move(eq(7), eq(1))
    }

    @Test
    fun tooltipClosed() {
        viewModel.onTooltipClosed()
        verify(preferencesDataSource).queueAddToPlaylistTooltipShown = true
    }

    @Test
    fun scrollToCurrentlyPlayingSong() {
        `when`(queueDataSource.index).thenReturn(5)
        val observer: Observer<Int> = mock()
        viewModel.setCurrentSongEvent.observeForever(observer)
        viewModel.scrollToCurrentlyPlayingSong()
        verify(observer).onChanged(5)
    }

    @Test
    fun didDeleteCurrentlyPlayingSong() {
        viewModel.didDeleteCurrentlyPlayingSong()
        verify(playback).next()
    }

    @Test
    fun onCreate() {
        viewModel.onCreate(mock())
        verify(preferencesDataSource).queueAddToPlaylistTooltipShown
    }
}
