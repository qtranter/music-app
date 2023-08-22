package com.audiomack.ui.sleeptimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.sleeptimer.SleepTimer
import com.audiomack.data.sleeptimer.SleepTimerManager
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.tracking.mixpanel.SleepTimerSource
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.Second
import com.audiomack.utils.SingleLiveEvent

class SleepTimerViewModel(
    private val source: SleepTimerSource,
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val sleepTimer: SleepTimer = SleepTimerManager.getInstance(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository()
) : BaseViewModel() {

    val closeEvent = SingleLiveEvent<Void>()
    val upgradeEvent = SingleLiveEvent<Void>()

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onSetSleepTimerTapped(seconds: Second) {
        if (premiumDataSource.isPremium) {
            sleepTimer.set(seconds)
            mixpanelDataSource.trackSleepTimer(source)
        } else upgradeEvent.call()
        closeEvent.call()
    }
}

class SleepTimerViewModelFactory(
    private val source: SleepTimerSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SleepTimerViewModel(source) as T
    }
}
