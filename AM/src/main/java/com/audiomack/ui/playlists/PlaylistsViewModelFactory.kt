package com.audiomack.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PlaylistsViewModelFactory(
    private val deeplinkTag: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlaylistsViewModel(deeplinkTag) as T
    }
}
