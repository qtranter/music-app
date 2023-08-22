package com.audiomack.data.inapprating

import android.app.Activity
import androidx.annotation.VisibleForTesting
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingProvider
import com.audiomack.data.tracking.TrackingRepository
import io.reactivex.subjects.BehaviorSubject

class InAppRatingManager private constructor(
    private val remoteVariables: RemoteVariablesProvider,
    private val trackingDataSource: TrackingDataSource,
    private val preferences: InAppRatingPreferences,
    private val engine: InAppRatingEngine
) : InAppRating {

    override val inAppRating = BehaviorSubject.create<InAppRatingResult>()

    override fun incrementDownloadCount() {
        preferences.incrementDownloadCount()
    }

    override fun incrementFavoriteCount() {
        preferences.incrementFavoriteCount()
    }

    override fun request() {
        if (!remoteVariables.inAppRatingEnabled) {
            return
        }

        if (preferences.answer == ANSWER_YES) {
            return
        }

        if ((preferences.timestamp + remoteVariables.inAppRatingInterval) > System.currentTimeMillis()) {
            return
        }

        if (preferences.downloadsCount <= remoteVariables.inAppRatingMinDownloads && preferences.favoritesCount <= remoteVariables.inAppRatingMinFavorites) {
            return
        }

        preferences.timestamp = System.currentTimeMillis()
        trackingDataSource.trackEvent("RatingPrompt", providers = listOf(TrackingProvider.Firebase))
        inAppRating.onNext(InAppRatingResult.ShowRatingPrompt)
    }

    override fun show(activity: Activity) {
        engine.show(activity)
    }

    override fun onRatingPromptAccepted() {
        preferences.answer = ANSWER_YES
        trackingDataSource.trackEvent("RatingEnjoyingAudiomack", providers = listOf(TrackingProvider.Firebase))
        inAppRating.onNext(InAppRatingResult.OpenRating)
    }

    override fun onRatingPromptDeclined() {
        preferences.answer = ANSWER_NO
        trackingDataSource.trackEvent("RatingNotEnjoyingAudiomack", providers = listOf(TrackingProvider.Firebase))
        inAppRating.onNext(InAppRatingResult.ShowDeclinedRatingPrompt)
    }

    override fun onDeclinedRatingPromptAccepted() {
        trackingDataSource.trackEvent("RatingEnjoyingRedirect", providers = listOf(TrackingProvider.Firebase))
        inAppRating.onNext(InAppRatingResult.OpenSupport)
    }

    override fun onDeclinedRatingPromptDeclined() {
        trackingDataSource.trackEvent("RatingNotEnjoyingRedirect", providers = listOf(TrackingProvider.Firebase))
    }

    companion object {
        private const val ANSWER_YES = "yes"
        private const val ANSWER_NO = "no"

        @Volatile
        private var INSTANCE: InAppRatingManager? = null

        @JvmOverloads
        @JvmStatic
        fun getInstance(
            remoteVariablesProvider: RemoteVariablesProvider = RemoteVariablesProviderImpl(),
            trackingDataSource: TrackingDataSource = TrackingRepository(),
            preferences: InAppRatingPreferences = InAppRatingPreferencesImpl,
            engine: InAppRatingEngine = PlayStoreInAppRatingEngine()
        ): InAppRatingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InAppRatingManager(
                    remoteVariablesProvider,
                    trackingDataSource,
                    preferences,
                    engine
                ).also { INSTANCE = it }
            }
        }

        @VisibleForTesting
        fun destroy() {
            INSTANCE = null
        }
    }
}
