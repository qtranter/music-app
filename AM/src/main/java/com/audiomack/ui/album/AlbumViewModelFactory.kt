package com.audiomack.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource

class AlbumViewModelFactory(
    private val album: AMResultItem,
    private val mixpanelSource: MixpanelSource,
    private val openShare: Boolean
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AlbumViewModel(album, mixpanelSource, openShare) as T
    }
}
