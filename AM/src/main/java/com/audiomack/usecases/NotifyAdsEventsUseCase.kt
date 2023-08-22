package com.audiomack.usecases

import com.audiomack.BuildConfig
import com.audiomack.data.logviewer.LogType
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.ui.home.AlertManager
import com.audiomack.ui.home.AlertTriggers
import timber.log.Timber

interface NotifyAdsEventsUseCase {
    fun notify(logMessage: String, prominentMessage: String = logMessage)
}

class NotifyAdsEventsUseCaseImpl(
    private val preferencesRepository: PreferencesDataSource = PreferencesRepository(),
    private val alertTriggers: AlertTriggers = AlertManager
) : NotifyAdsEventsUseCase {
    override fun notify(logMessage: String, prominentMessage: String) {
        Timber.tag(LogType.ADS.tag).d(logMessage)
        if (BuildConfig.AUDIOMACK_DEBUG || preferencesRepository.trackingAds) {
            alertTriggers.onAdEvent(prominentMessage)
        }
    }
}
