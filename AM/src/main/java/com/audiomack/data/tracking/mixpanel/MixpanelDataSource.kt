package com.audiomack.data.tracking.mixpanel

import android.content.Context
import android.content.Intent
import com.audiomack.data.premium.InAppPurchaseDataSource
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.telco.TelcoDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.AMComment
import com.audiomack.model.AMResultItem
import com.audiomack.model.AdRevenueInfo
import com.audiomack.model.Artist
import com.audiomack.model.AuthenticationType
import com.audiomack.model.CommentMethod
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.Music
import com.audiomack.model.PermissionType
import com.audiomack.model.QueueType
import com.audiomack.model.SearchReturnType
import com.audiomack.model.SearchType
import com.audiomack.model.ShareMethod
import com.audiomack.model.SongEndType
import com.audiomack.model.WorldArticle
import com.audiomack.onesignal.TransactionalNotificationInfo

interface MixpanelDataSource {

    fun trackViewSignupPage(source: LoginSignupSource)

    fun trackCreateAccount(
        source: LoginSignupSource,
        authenticationType: AuthenticationType,
        userDataSource: UserDataSource,
        premiumDataSource: PremiumDataSource
    )

    fun trackPromptPermissions(permissionType: PermissionType)

    fun trackEnablePermissions(context: Context, permissions: Array<String>, grantResults: IntArray)

    fun trackLogin(
        source: LoginSignupSource,
        authenticationType: AuthenticationType,
        userDataSource: UserDataSource,
        premiumDataSource: PremiumDataSource,
        telcoDataSource: TelcoDataSource
    )

    fun trackLogout()

    fun trackViewPremiumSubscription(source: InAppPurchaseMode)

    fun trackPremiumCheckoutStarted(source: InAppPurchaseMode)

    fun trackPurchasePremiumTrial(
        source: InAppPurchaseMode,
        inAppPurchaseDataSource: InAppPurchaseDataSource
    )

    fun trackCancelSubscription(inAppPurchaseDataSource: InAppPurchaseDataSource)

    fun trackPlaySong(song: AMResultItem, durationPlayed: Int, endType: SongEndType, source: MixpanelSource, button: String)

    fun trackDownloadToOffline(music: AMResultItem, source: MixpanelSource, button: String)

    fun trackCreatePlaylist(playlist: AMResultItem)

    fun trackAddToPlaylist(music: Music, playlist: AMResultItem, source: MixpanelSource, button: String)

    fun trackHighlight(music: AMResultItem, source: MixpanelSource, button: String)

    fun trackAddToFavorites(music: AMResultItem, source: MixpanelSource, button: String)

    fun trackQueue(music: AMResultItem, queueType: QueueType, source: MixpanelSource, button: String)

    fun trackSearch(query: String, type: SearchType, returnType: SearchReturnType)

    fun trackReUp(music: AMResultItem, source: MixpanelSource, button: String)

    fun trackFollowAccount(accountName: String, accountId: String, source: MixpanelSource, button: String)

    fun trackUnfollowAccount(accountName: String, accountId: String, source: MixpanelSource, button: String)

    fun trackShareContent(method: ShareMethod, artist: AMArtist?, music: AMResultItem?, comment: AMComment?, article: WorldArticle?, source: MixpanelSource, button: String)

    fun trackError(type: String, description: String)

    fun trackIdentity(userDataSource: UserDataSource, premiumDataSource: PremiumDataSource)

    fun trackGeneralProperties(oneSignalUserId: String?)

    fun trackAppsFlyerConversion(conversionData: Map<String, Any>, installation: Boolean)

    fun trackPushReceived(intent: Intent)

    fun trackPushOpened(intent: Intent)

    fun flushEvents()

    fun trackAddComment(comment: AMComment?, entity: AMResultItem?)

    fun trackCommentDetail(method: CommentMethod, comment: AMComment?, entity: AMResultItem?)

    fun trackOnboarding(artistName: String? = null, playlistName: String? = null, genre: String? = null)

    fun trackTransactionalNotificationOpened(info: TransactionalNotificationInfo)

    fun trackAdServed(info: AdRevenueInfo)

    fun trackBillingIssue()

    fun trackBellNotification(bellType: String)

    fun trackScreenshot(screenshotType: String, screenshotUser: String, artist: Artist?, music: Music?, source: MixpanelSource, button: String)

    fun trackSleepTimer(source: SleepTimerSource)

    fun trackTrendingBannerClick(url: String)

    fun trackViewArticle(post: WorldArticle)

    fun trackResetPassword(email: String)

    fun trackChangePassword()

    fun trackRestoreDownloads(kind: RestoreDownloadsMode, count: Int)

    fun trackFollowPushPermissionPrompt(granted: Boolean)

    fun trackPremiumDownloadNotification(type: PremiumDownloadType)

    fun trackLocalFileOpened(songName: String, artistName: String)
}

enum class RestoreDownloadsMode { All, Manually }
enum class SleepTimerSource { Prompt, Settings }
enum class PremiumDownloadType { Limited, PremiumOnly }
