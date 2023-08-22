package com.audiomack.data.remotevariables

import com.audiomack.data.remotevariables.datasource.FirebaseRemoteVariablesDataSource
import com.audiomack.data.remotevariables.datasource.RemoteVariablesDataSource
import io.reactivex.Observable

class RemoteVariablesProviderImpl(
    val firebase: RemoteVariablesDataSource = FirebaseRemoteVariablesDataSource
) : RemoteVariablesProvider {

    // Firebase
    private val varInterstitialTiming = LongRemoteVariable("ad_interstitial_timing", 275L)
    private val varPlayerAdEnabled = BooleanRemoteVariable("ad_player_enabled", true)
    private val varBannerAdEnabled = BooleanRemoteVariable("ad_banner_enabled", true)
    private val varInterstitialAdEnabled = BooleanRemoteVariable("ad_interstitial_enabled", true)
    private val varInterstitialSoundOnAdEnabled = BooleanRemoteVariable("ad_interstitial_sound_on_ad_enabled", false)
    private val varPlayerNativeAdsPercentage = LongRemoteVariable("now_playing_native_ad_percentage", 50L)
    private val varInterstitialSoundOnAdPeriod = LongRemoteVariable("ad_interstitial_sound_on_ad_period", 86400L)
    private val varApsEnabled = BooleanRemoteVariable("ad_aps_enabled", true)
    private val varApsTimeout = LongRemoteVariable("ad_aps_timeout", 750L)
    private val varFirstOpeningDeeplink = StringRemoteVariable("deeplink_first_session", "audiomack://onboarding-artistselect")
    private val varTester = BooleanRemoteVariable("tester", false)
    private val varDownloadCheckEnabled = BooleanRemoteVariable("check_download_enabled", true)
    private val varSyncCheckEnabled = BooleanRemoteVariable("check_sync_enabled", true)
    private val varBookmarksEnabled = BooleanRemoteVariable("bookmarks_enabled", true)
    private val varBookmarksExpirationHours = LongRemoteVariable("bookmarks_expiration_hours", 120L)
    private val varInAppRatingEnabled = BooleanRemoteVariable("in_app_rating_enabled", true)
    private val varInAppUpdatesMinImmediateVersion = StringRemoteVariable("in_app_updates_immediate_min_version", "5.6.0")
    private val varInAppUpdatesMinFlexibleVersion = StringRemoteVariable("in_app_updates_flexible_min_version", "5.6.0")
    private val varAudioAdsEnabled = BooleanRemoteVariable("ad_audio_enabled", true)
    private val varAudioAdsTiming = LongRemoteVariable("ad_audio_timing", 1200L)
    private val varAdFirstPlayDelay = LongRemoteVariable("ad_first_play_delay", 1000L)
    private val varDeeplinksPathsBlacklist = StringRemoteVariable("deeplinks_paths_blacklist",
        "oauth,edit,upload,forgot-password,world,dashboard,creators,amp,amp-code,monetization,about,stats,premium-partner-agreement,contact-us,yourvoiceyourchoice,premium,responsible-disclosure,email")
    private val varTrendingBannerEnabled = BooleanRemoteVariable("home_banner_enabled", false)
    private val varTrendingBannerMessage = StringRemoteVariable("home_banner_message", "")
    private val varTrendingBannerLink = StringRemoteVariable("home_banner_link", "")
    private val varInAppRatingMinFavorites = LongRemoteVariable("in_app_rating_min_favorites", 5L)
    private val varInAppRatingMinDownloads = LongRemoteVariable("in_app_rating_min_downloads", 5L)
    private val varInAppRatingInterval = LongRemoteVariable("in_app_rating_interval", 2592000000L)
    private val varHideFollowOnSearchForLoggedOutUsers =
        BooleanRemoteVariable("hide_follow_on_search_for_logged_out_users", false)
    private val varSlideUpMenuShareMode = StringRemoteVariable("slide_up_menu_share_mode", RemoteVariablesProvider.FIREBASE_SLIDE_UP_MENU_SHARE_MODE_GRID)
    private val varLoginAlertmessage = StringRemoteVariable("pre_login_message", "")
    private val varAdWithholdPlays = LongRemoteVariable("ad_withhold_plays", 0L)

    override fun initialise(): Observable<Boolean> {

        val firebaseList = listOf(
            varInterstitialTiming,
            varPlayerAdEnabled,
            varBannerAdEnabled,
            varInterstitialAdEnabled,
            varInterstitialSoundOnAdEnabled,
            varPlayerNativeAdsPercentage,
            varInterstitialSoundOnAdPeriod,
            varApsEnabled,
            varApsTimeout,
            varFirstOpeningDeeplink,
            varTester,
            varDownloadCheckEnabled,
            varSyncCheckEnabled,
            varBookmarksEnabled,
            varBookmarksExpirationHours,
            varInAppRatingEnabled,
            varInAppUpdatesMinImmediateVersion,
            varInAppUpdatesMinFlexibleVersion,
            varAudioAdsEnabled,
            varAudioAdsTiming,
            varAdFirstPlayDelay,
            varDeeplinksPathsBlacklist,
            varTrendingBannerEnabled,
            varTrendingBannerMessage,
            varTrendingBannerLink,
            varInAppRatingMinFavorites,
            varInAppRatingMinDownloads,
            varInAppRatingInterval,
            varHideFollowOnSearchForLoggedOutUsers,
            varSlideUpMenuShareMode,
            varLoginAlertmessage,
            varAdWithholdPlays
        )

        return firebase.init(firebaseList).flatMap { Observable.just(true) }
    }

    override val interstitialTiming: Long
        get() = firebase.getLong(varInterstitialTiming)

    override val playerAdEnabled: Boolean
        get() = firebase.getBoolean(varPlayerAdEnabled)

    override val bannerAdEnabled: Boolean
        get() = firebase.getBoolean(varBannerAdEnabled)

    override val interstitialAdEnabled: Boolean
        get() = firebase.getBoolean(varInterstitialAdEnabled)

    override val interstitialSoundOnAdEnabled: Boolean
        get() = firebase.getBoolean(varInterstitialSoundOnAdEnabled)

    override val playerNativeAdsPercentage: Long
        get() = firebase.getLong(varPlayerNativeAdsPercentage)

    override val interstitialSoundOnAdPeriod: Long
        get() = firebase.getLong(varInterstitialSoundOnAdPeriod)

    override val apsEnabled: Boolean
        get() = firebase.getBoolean(varApsEnabled)

    override val apsTimeout: Long
        get() = firebase.getLong(varApsTimeout)

    override val firstOpeningDeeplink: String
        get() = firebase.getString(varFirstOpeningDeeplink)

    override val tester: Boolean
        get() = firebase.getBoolean(varTester)

    override val downloadCheckEnabled: Boolean
        get() = firebase.getBoolean(varDownloadCheckEnabled)

    override val syncCheckEnabled: Boolean
        get() = firebase.getBoolean(varSyncCheckEnabled)

    override val bookmarksEnabled: Boolean
        get() = firebase.getBoolean(varBookmarksEnabled)

    override val bookmarksExpirationHours: Long
        get() = firebase.getLong(varBookmarksExpirationHours)

    override val inAppRatingEnabled: Boolean
        get() = firebase.getBoolean(varInAppRatingEnabled)

    override val inAppUpdatesMinImmediateVersion: String
        get() = firebase.getString(varInAppUpdatesMinImmediateVersion)

    override val inAppUpdatesMinFlexibleVersion: String
        get() = firebase.getString(varInAppUpdatesMinFlexibleVersion)

    override val audioAdsEnabled: Boolean
        get() = firebase.getBoolean(varAudioAdsEnabled)

    override val audioAdsTiming: Long
        get() = firebase.getLong(varAudioAdsTiming)

    override val adFirstPlayDelay: Long
        get() = firebase.getLong(varAdFirstPlayDelay)

    override val deeplinksPathsBlacklist: List<String>
        get() = firebase.getString(varDeeplinksPathsBlacklist)
            .split(",")
            .filter { it.isNotBlank() }
            .map { it.trim() }

    override val trendingBannerEnabled: Boolean
        get() = firebase.getBoolean(varTrendingBannerEnabled)

    override val trendingBannerMessage: String
        get() = firebase.getString(varTrendingBannerMessage)

    override val trendingBannerLink: String
        get() = firebase.getString(varTrendingBannerLink)

    override val inAppRatingMinFavorites: Long
        get() = firebase.getLong(varInAppRatingMinFavorites)

    override val inAppRatingMinDownloads: Long
        get() = firebase.getLong(varInAppRatingMinDownloads)

    override val inAppRatingInterval: Long
        get() = firebase.getLong(varInAppRatingInterval)

    override val hideFollowOnSearchForLoggedOutUsers: Boolean
        get() = firebase.getBoolean(varHideFollowOnSearchForLoggedOutUsers)

    override val slideUpMenuShareMode: String
        get() = firebase.getString(varSlideUpMenuShareMode)

    override val loginAlertMessage: String
        get() = firebase.getString(varLoginAlertmessage)

    override val adWithholdPlays: Long
        get() = firebase.getLong(varAdWithholdPlays)
}
