package com.audiomack.ui.slideupmenu.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource

class SlideUpMenuShareViewModelFactory(
    private val music: AMResultItem?,
    private val artist: AMArtist?,
    private val mixpanelSource: MixpanelSource,
    private val mixpanelButton: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SlideUpMenuShareViewModel(
                music,
                artist,
                mixpanelSource,
                mixpanelButton
        ) as T
    }
}
