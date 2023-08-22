package com.audiomack.ui.playlist.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource

class PlaylistViewModelFactory(
    private val playlist: AMResultItem,
    private val online: Boolean,
    private val deleted: Boolean,
    private val mixpanelSource: MixpanelSource,
    private val openShare: Boolean
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlaylistViewModel(
            playlist,
            online,
            deleted,
            mixpanelSource,
            openShare
        ) as T
    }
}
