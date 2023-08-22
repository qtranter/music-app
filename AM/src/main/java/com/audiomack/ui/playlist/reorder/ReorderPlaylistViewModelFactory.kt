package com.audiomack.ui.playlist.reorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.data.api.MusicDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.rx.SchedulersProvider

class ReorderPlaylistViewModelFactory(
    private val playlist: AMResultItem,
    private val schedulersProvider: SchedulersProvider,
    private val musicDataSource: MusicDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ReorderPlaylistViewModel(playlist, schedulersProvider, musicDataSource) as T
    }
}
