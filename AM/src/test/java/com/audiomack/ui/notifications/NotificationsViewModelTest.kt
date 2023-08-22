package com.audiomack.ui.notifications

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.model.AMResultItem
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class NotificationsViewModelTest {

    private lateinit var viewModel: NotificationsViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        viewModel = NotificationsViewModel()
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun showFragment() {
        val observer: Observer<Void> = mock()
        viewModel.showNotificationsFragmentEvent.observeForever(observer)
        viewModel.onCreate()
        verify(observer).onChanged(null)
    }

    @Test
    fun onRequestedPlaylistsGrid() {
        val playlists: List<AMResultItem> = emptyList()
        val observer: Observer<List<AMResultItem>> = mock()
        viewModel.showPlaylistsGridEvent.observeForever(observer)
        viewModel.onRequestedPlaylistsGrid(playlists)
        verify(observer).onChanged(playlists)
    }
}
