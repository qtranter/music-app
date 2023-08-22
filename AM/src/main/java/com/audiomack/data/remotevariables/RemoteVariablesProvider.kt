package com.audiomack.data.remotevariables

import io.reactivex.Observable

interface RemoteVariablesProvider {

    fun initialise(): Observable<Boolean>

    // Ads
    val interstitialTiming: Long
    val playerAdEnabled: Boolean
    val bannerAdEnabled: Boolean
    val interstitialAdEnabled: Boolean
    val interstitialSoundOnAdEnabled: Boolean
    val playerNativeAdsPercentage: Long
    val interstitialSoundOnAdPeriod: Long
    val apsEnabled: Boolean
    val apsTimeout: Long
    val audioAdsEnabled: Boolean
    val audioAdsTiming: Long
    val adFirstPlayDelay: Long
    val adWithholdPlays: Long

    // First session deeplink
    val firstOpeningDeeplink: String // e.g. "audiomack://onboarding-artistselect", "audiomack://music_trending"
    val deeplinksPathsBlacklist: List<String>

    // Tester
    val tester: Boolean

    // Housekeeping
    val downloadCheckEnabled: Boolean
    val syncCheckEnabled: Boolean

    // Bookmarks
    val bookmarksEnabled: Boolean
    val bookmarksExpirationHours: Long

    // In app rating
    val inAppRatingEnabled: Boolean
    val inAppRatingMinFavorites: Long
    val inAppRatingMinDownloads: Long
    val inAppRatingInterval: Long

    // In app updates
    val inAppUpdatesMinImmediateVersion: String
    val inAppUpdatesMinFlexibleVersion: String

    // Trending banner
    val trendingBannerEnabled: Boolean
    val trendingBannerMessage: String
    val trendingBannerLink: String

    // Follow button
    val hideFollowOnSearchForLoggedOutUsers: Boolean

    // Slide share menu
    val slideUpMenuShareMode: String

    // Login alert
    val loginAlertMessage: String

    companion object {
        const val FIREBASE_SLIDE_UP_MENU_SHARE_MODE_LIST = "list"
        const val FIREBASE_SLIDE_UP_MENU_SHARE_MODE_GRID = "grid"
    }
}
