package com.audiomack.data.tracking.appsflyer

import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserDataSource

class AppsFlyerRepository(
    private val tracker: AppsFlyerTracker = AppsFlyerTrackerImpl.getInstance(MixpanelRepository())
) : AppsFlyerDataSource {

    override fun trackUserSignup() {
        tracker.trackEvent(AppsFlyerEvents.Signup.stringValue())
    }

    override fun trackAddToFavorites() {
        tracker.trackEvent(AppsFlyerEvents.AddToFavorites.stringValue())
    }

    override fun trackCreatePlaylist() {
        tracker.trackEvent(AppsFlyerEvents.CreatePlaylist.stringValue())
    }

    override fun trackShareContent() {
        tracker.trackEvent(AppsFlyerEvents.ShareContent.stringValue())
    }

    override fun trackSongPlay() {
        tracker.trackEvent(AppsFlyerEvents.PlaySong.stringValue())
    }

    override fun trackPremiumView() {
        tracker.trackEvent(AppsFlyerEvents.PremiumView.stringValue())
    }

    override fun trackPremiumStart() {
        tracker.trackEvent(AppsFlyerEvents.PremiumStart.stringValue())
    }

    override fun trackPremiumTrial() {
        tracker.trackEvent(AppsFlyerEvents.PremiumTrial.stringValue())
    }

    override fun trackAdWatched() {
        tracker.trackEvent(AppsFlyerEvents.AdWatched.stringValue())
    }

    override fun trackIdentity(userDataSource: UserDataSource) {
        userDataSource.getUserId()?.let {
            tracker.trackUserId(it)
        }
    }
}
