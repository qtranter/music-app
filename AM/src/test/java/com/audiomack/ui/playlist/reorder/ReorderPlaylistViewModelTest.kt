package com.audiomack.ui.playlist.reorder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.api.MusicDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ReorderPlaylistViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val track = mock<AMResultItem> {
        on { itemId } doReturn "1"
    }

    private val playlist = mock<AMResultItem> {
        on { itemId } doReturn "123"
        on { title } doReturn "Test"
        on { genre } doReturn "rap"
        on { tracks } doReturn (0 until 5).map { track }
    }

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: ReorderPlaylistViewModel

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        viewModel = ReorderPlaylistViewModel(playlist, schedulersProvider, musicDataSource)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `close tapped`() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `on create`() {
        val observer: Observer<List<AMResultItem>> = mock()
        viewModel.showTracksEvent.observeForever(observer)
        viewModel.onCreate()
        verify(observer).onChanged(argWhere { it.size == playlist.tracks!!.size })
    }

    @Test
    fun `save successful`() {
        `when`(
            musicDataSource.reorderPlaylist(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyString()
            )
        ).thenReturn(Observable.just(playlist))
        val observerLoading: Observer<ReorderPlaylistViewModel.ReorderPlaylistLoadingStatus> = mock()
        val observerClose: Observer<Void> = mock()
        viewModel.loadingEvent.observeForever(observerLoading)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onSaveTapped(playlist.tracks!!.reversed())
        verify(observerLoading, times(1)).onChanged(eq(ReorderPlaylistViewModel.ReorderPlaylistLoadingStatus.Loading))
        verify(observerLoading, times(1)).onChanged(argWhere { it is ReorderPlaylistViewModel.ReorderPlaylistLoadingStatus.Success })
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `save error`() {
        `when`(
            musicDataSource.reorderPlaylist(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyString()
            )
        ).thenReturn(Observable.error(Exception("Unknown failure")))
        val observerLoading: Observer<ReorderPlaylistViewModel.ReorderPlaylistLoadingStatus> = mock()
        val observerClose: Observer<Void> = mock()
        viewModel.loadingEvent.observeForever(observerLoading)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onSaveTapped(playlist.tracks!!.reversed())
        verify(observerLoading, times(1)).onChanged(eq(ReorderPlaylistViewModel.ReorderPlaylistLoadingStatus.Loading))
        verify(observerLoading, times(1)).onChanged(argWhere { it is ReorderPlaylistViewModel.ReorderPlaylistLoadingStatus.Error })
        verifyZeroInteractions(observerClose)
    }
}
