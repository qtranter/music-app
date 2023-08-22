package com.audiomack.ui.slideupmenu.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource

class SlideUpMenuMusicViewModelFactory(
    private val music: AMResultItem,
    private val mixpanelSource: MixpanelSource,
    private val removeFromDownloadsEnabled: Boolean,
    private val removeFromQueueEnabled: Boolean,
    private val removeFromQueueIndex: Int?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SlideUpMenuMusicViewModel(
            music,
            mixpanelSource,
            removeFromDownloadsEnabled,
            removeFromQueueEnabled,
            removeFromQueueIndex
        ) as T
    }
}
