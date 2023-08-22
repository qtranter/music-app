package com.audiomack.ui.onboarding.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource

class PlaylistOnboardingViewModelFactory(
    private val artistImage: String,
    private val playlist: AMResultItem,
    private val mixpanelSource: MixpanelSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlaylistOnboardingViewModel(
            artistImage,
            playlist,
            mixpanelSource
        ) as T
    }
}
