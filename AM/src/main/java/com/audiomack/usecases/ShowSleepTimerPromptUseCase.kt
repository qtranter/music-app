package com.audiomack.usecases

import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.preferences.SleepTimerPromptStatus
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import io.reactivex.Maybe

enum class SleepTimerPromptMode { Unlocked, Locked }

interface ShowSleepTimerPromptUseCase {
    /**
     * Emits the [SleepTimerPromptMode] type of prompt that should be shown, or nothing if not needed
     */
    fun getPromptMode(): Maybe<SleepTimerPromptMode>
}

class ShowSleepTimerPromptUseCaseImpl(
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val preferencesDataSource: PreferencesDataSource = PreferencesRepository()
) : ShowSleepTimerPromptUseCase {

    override fun getPromptMode() = Maybe.create<SleepTimerPromptMode> { emitter ->
        var status = preferencesDataSource.sleepTimerPromptStatus
        if (status == SleepTimerPromptStatus.Unknown) {
            status = if (adsDataSource.isFreshInstall()) SleepTimerPromptStatus.Skipped else SleepTimerPromptStatus.NotShown
            preferencesDataSource.sleepTimerPromptStatus = status
        }
        if (status == SleepTimerPromptStatus.NotShown) {
            if (premiumDataSource.isPremium) {
                emitter.onSuccess(SleepTimerPromptMode.Unlocked)
            } else {
                emitter.onSuccess(SleepTimerPromptMode.Locked)
            }
        }
        preferencesDataSource.sleepTimerPromptStatus = SleepTimerPromptStatus.Shown
        emitter.onComplete()
    }
}
