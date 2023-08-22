package com.audiomack.data.tracking.mixpanel

import android.content.Intent
import com.audiomack.TestApplication
import com.audiomack.data.premium.InAppPurchaseDataSource
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.telco.TelcoDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.AMComment
import com.audiomack.model.AMResultItem
import com.audiomack.model.AdRevenueInfo
import com.audiomack.model.AuthenticationType
import com.audiomack.model.CommentMethod
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.Music
import com.audiomack.model.MusicType
import com.audiomack.model.PermissionType
import com.audiomack.model.QueueType
import com.audiomack.model.SearchReturnType
import com.audiomack.model.SearchType
import com.audiomack.model.ShareMethod
import com.audiomack.model.SongEndType
import com.audiomack.model.WorldArticle
import com.audiomack.onesignal.TransactionalNotificationInfo
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    application = TestApplication::class
)
class MixpanelRepositoryTest {

    private lateinit var tracker: MixpanelTracker
    private lateinit var sut: MixpanelRepository
    private lateinit var mixpanelSource: MixpanelSource

    @Before
    fun setup() {
        tracker = mock()
        sut = MixpanelRepository(tracker)
        mixpanelSource = MixpanelSource("Tab", "Page")
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun trackViewSignupPage() {
        val source = LoginSignupSource.AccountFollow
        sut.trackViewSignupPage(source)
        verify(tracker).trackEvent(eq("View Sign Up Page"), argWhere { it["Button"] == source.stringValue })
    }

    @Test
    fun trackCreateAccount() {
        val userId = "123"
        val source = LoginSignupSource.AccountFollow

        val authenticationType = AuthenticationType.Facebook

        val userDataSource: UserDataSource = mock()
        `when`(userDataSource.getUserId()).thenReturn(userId)

        val premiumDataSource: PremiumDataSource = mock()
        `when`(premiumDataSource.isPremium).thenReturn(true)

        sut.trackCreateAccount(source, authenticationType, userDataSource, premiumDataSource)
        verify(tracker).trackEvent(eq("Create Account"), argWhere { it["Authentication Type"] == authenticationType.stringValue && it["Button"] == source.stringValue })
        verify(tracker).trackUserProperties(argWhere { it["Subscription Type"] == "Premium" && it["User ID"] == userId })
        verify(tracker).trackSuperProperties(argWhere { it["Subscription Type"] == "Premium" && it["User ID"] == userId })
    }

    @Test
    fun trackPromptPermissions() {
        sut.trackPromptPermissions(PermissionType.Camera)
        verify(tracker).trackEvent(eq("Prompt Permissions"), argWhere { it["Permission Type"] == PermissionType.Camera.stringValue() })
    }

    @Test
    fun trackEnablePermissions() {
        sut.trackEnablePermissions(mock(), arrayOf("android.permission.WRITE_EXTERNAL_STORAGE"), intArrayOf())
        verify(tracker).trackEvent(eq("Enable Permissions"), argWhere { it["Permission Type"] == PermissionType.Storage.stringValue() })
        verify(tracker).trackUserProperties(argWhere { it.keys.contains("Location Enabled") && it.keys.contains("Notification Enabled") && it.keys.contains("Storage Enabled") && it.keys.contains("Camera Enabled") })
    }

    @Test
    fun trackLogin() {
        val userId = "123"
        val simOperatorName = "Virgin"
        val source = LoginSignupSource.AccountFollow

        val authenticationType = AuthenticationType.Facebook

        val userDataSource: UserDataSource = mock()
        `when`(userDataSource.getUserId()).thenReturn(userId)

        val premiumDataSource: PremiumDataSource = mock()
        `when`(premiumDataSource.isPremium).thenReturn(true)

        val telcoDataSource: TelcoDataSource = mock()
        `when`(telcoDataSource.getSimOperatorName()).thenReturn(simOperatorName)

        sut.trackLogin(source, authenticationType, userDataSource, premiumDataSource, telcoDataSource)
        verify(tracker).trackEvent(eq("Log In"), argWhere { it["Button"] == source.stringValue && it["Authentication Type"] == authenticationType.stringValue })
        verify(tracker).trackUserProperties(argWhere { it["Authentication Type"] == authenticationType.stringValue })
        verify(tracker).setUserPropertyOnce(eq("First Log In Date"), any())
        verify(tracker).trackSuperProperties(argWhere { it["User ID"] == userId && it["Subscription Type"] == "Premium" && it["ta_sim_operator_name"] == simOperatorName })
    }

    @Test
    fun trackLogout() {
        sut.trackLogout()
        verify(tracker).trackEvent(eq("Log Out"), any())
        verify(tracker).reset()
    }

    @Test
    fun trackViewPremiumSubscription() {
        sut.trackViewPremiumSubscription(InAppPurchaseMode.BannerAdDismissal)
        verify(tracker).trackEvent(eq("View Premium Subscription"), argWhere { it["Button"] == InAppPurchaseMode.BannerAdDismissal.stringValue() })
    }

    @Test
    fun trackPremiumCheckoutStarted() {
        sut.trackPremiumCheckoutStarted(InAppPurchaseMode.MyLibraryBar)
        verify(tracker).trackEvent(eq("Premium Checkout Started"), argWhere { it["Button"] == InAppPurchaseMode.MyLibraryBar.stringValue() })
    }

    @Test
    fun trackPurchasePremiumTrial() {
        val source = InAppPurchaseMode.MyLibraryBar
        val currency = "EUR"
        val price = 1.23
        val inAppPurchaseDataSource: InAppPurchaseDataSource = mock()
        `when`(inAppPurchaseDataSource.getCurrency()).thenReturn(currency)
        `when`(inAppPurchaseDataSource.getSubscriptionPrice()).thenReturn(price)

        sut.trackPurchasePremiumTrial(source, inAppPurchaseDataSource)
        verify(tracker).trackEvent(eq("Purchase Premium Trial"), argWhere { it["Monthly Subscription Currency"] == currency && it["Button"] == source.stringValue() })
        verify(tracker).trackUserProperties(argWhere { it["Monthly Subscription Amount"] == price })
        verify(tracker).trackSuperProperties(argWhere { it["Subscription Type"] == "Premium" })
    }

    @Test
    fun trackCancelSubscription() {
        val currency = "EUR"
        val price = 1.23
        val inAppPurchaseDataSource: InAppPurchaseDataSource = mock()
        `when`(inAppPurchaseDataSource.getCurrency()).thenReturn(currency)
        `when`(inAppPurchaseDataSource.getSubscriptionPrice()).thenReturn(price)

        sut.trackCancelSubscription(inAppPurchaseDataSource)
        verify(tracker).trackEvent(eq("Cancel Subscription"), argWhere { it["Monthly Subscription Currency"] == currency })
        verify(tracker).trackUserProperties(argWhere { it.keys.contains("Cancellation Date") && it["Monthly Subscription Amount"] == price })
    }

    @Test
    fun trackPlaySong() {
        val id = "123"
        val genre = "electronic"
        val title = "Innuendo"
        val artist = "Queen"
        val song = AMResultItem().apply {
            this.itemId = id
            this.genre = genre
            this.title = title
            this.artist = artist
        }
        val durationPlayed = 30
        val endType = SongEndType.ChangedSong
        val button = MixpanelButtonAlbumDetails

        sut.trackPlaySong(song, durationPlayed, endType, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Play Song"), argWhere { it["Song ID"] == id && it["Genre"] == genre &&
            it["Song Name"] == title.toLowerCase() && it["Artist Name"] == artist.toLowerCase() &&
            it["Song End Type"] == endType.stringValue() &&
            it["Duration Played (seconds)"] == durationPlayed && it.containsKey("Music Tags") &&
            it.validSourceParameters(mixpanelSource) && it["Button"] == button })
        verify(tracker).trackUserProperties(any())
    }

    @Test
    fun trackDownloadToOffline() {
        val id = "123"
        val type = "song"
        val music = AMResultItem().apply {
            this.itemId = id
            this.type = type
        }
        val button = MixpanelButtonList

        sut.trackDownloadToOffline(music, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Download to Offline"), argWhere { it["Song ID"] == id && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
    }

    @Test
    fun trackCreatePlaylist() {
        val id = "123"
        val title = "My Playlist"
        val playlist = AMResultItem().apply {
            this.itemId = id
            this.title = title
        }
        sut.trackCreatePlaylist(playlist)
        verify(tracker).trackEvent(eq("Create Playlist"), argWhere { it["Playlist ID"] == id && it["Playlist Name"] == title })
    }

    @Test
    fun trackAddToPlaylist() {
        val musicId = "123"
        val playlistId = "123456"
        val playlistTitle = "My playlist"
        val music = Music(id = musicId)
        val playlist = AMResultItem().apply {
            this.itemId = playlistId
            this.title = playlistTitle
        }
        val button = MixpanelButtonList

        sut.trackAddToPlaylist(music, playlist, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Add to Playlist"), argWhere { it["Song ID"] == musicId && it["Playlist ID"] == playlistId && it["Playlist Name"] == playlistTitle && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
    }

    @Test
    fun trackHighlight() {
        val id = "123"
        val type = "song"
        val music = AMResultItem().apply {
            this.itemId = id
            this.type = type
        }
        val button = MixpanelButtonKebabMenu

        sut.trackHighlight(music, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Highlight"), argWhere { it["Song ID"] == id && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
        verify(tracker).trackUserProperties(argWhere { it.keys.contains("Last Highlighted Date") })
    }

    @Test
    fun trackAddToFavorites() {
        val id = "123"
        val type = "song"
        val music = AMResultItem().apply {
            this.itemId = id
            this.type = type
        }
        val button = MixpanelButtonKebabMenu

        sut.trackAddToFavorites(music, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Add to Favorites"), argWhere { it["Song ID"] == id && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
        verify(tracker).trackUserProperties(argWhere { it.keys.contains("Last Favorited Date") })
    }

    @Test
    fun trackQueue() {
        val id = "123"
        val type = "album"
        val music = AMResultItem().apply {
            this.itemId = id
            this.type = type
        }
        val queueType = QueueType.PlayNext
        val button = MixpanelButtonKebabMenu

        sut.trackQueue(music, queueType, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Queue"), argWhere { it["Album ID"] == id && it["Queue Type"] == queueType.stringValue() && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
    }

    @Test
    fun trackSearch() {
        val query = "Queen"
        val type = SearchType.Recent
        val returnType = SearchReturnType.Replacement
        sut.trackSearch(query, type, returnType)
        verify(tracker).trackEvent(eq("Search"), argWhere { it["Search Term"] == query.toLowerCase() && it["Search Type"] == type.stringValue() && it["Search Return"] == returnType.stringValue() })
        verify(tracker).trackUserProperties(argWhere { it.keys.contains("Last Search Date") })
    }

    @Test
    fun trackReUp() {
        val id = "123"
        val type = "album"
        val music = AMResultItem().apply {
            this.itemId = id
            this.type = type
        }
        val button = MixpanelButtonAlbumDetails

        sut.trackReUp(music, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Re-Up"), argWhere { it["Album ID"] == id && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
        verify(tracker).trackUserProperties(argWhere { it.keys.contains("Last Re-upped Date") })
    }

    @Test
    fun trackFollowAccount() {
        val name = "Queen"
        val id = "123"
        val button = MixpanelButtonArtistInfo

        sut.trackFollowAccount(name, id, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Follow Account"), argWhere { it["Account Name"] == name && it["Account ID"] == id && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
    }

    @Test
    fun trackUnfollowAccount() {
        val name = "Queen"
        val id = "123"
        val button = MixpanelButtonArtistInfo

        sut.trackUnfollowAccount(name, id, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Unfollow Account"), argWhere { it["Account Name"] == name && it["Account ID"] == id && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
    }

    @Test
    fun `share music`() {
        val id = "123"
        val type = "album"
        val music = AMResultItem().apply {
            this.itemId = id
            this.type = type
        }
        val shareMethod = ShareMethod.Twitter
        val button = MixpanelButtonSettings

        sut.trackShareContent(shareMethod, null, music, null, null, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Share Content"), argWhere { it["Album ID"] == id && it["Content Type"] == "Album" && it["Share Method"] == shareMethod.stringValue() && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
        verify(tracker).incrementUserProperty(eq("# of Shares"), eq(1.toDouble()))
        verify(tracker).trackUserProperties(argWhere { it.keys.contains("Last Shared Date") })
    }

    @Test
    fun `share artist`() {
        val artistID = "123"
        val artistName = "Artist"
        val artist = mock<AMArtist> {
            on { artistId } doReturn artistID
            on { name } doReturn artistName
        }
        val shareMethod = ShareMethod.Twitter
        val button = MixpanelButtonSettings

        sut.trackShareContent(shareMethod, artist, null, null, null, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Share Content"), argWhere { it["Account ID"] == artistID && it["Account Name"] == artistName && it["Content Type"] == "Account" && it["Share Method"] == shareMethod.stringValue() && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
        verify(tracker).incrementUserProperty(eq("# of Shares"), eq(1.toDouble()))
        verify(tracker).trackUserProperties(argWhere { it.keys.contains("Last Shared Date") })
    }

    @Test
    fun `share comment`() {
        val commentId = "123"
        val comment = mock<AMComment> {
            on { entityId } doReturn commentId
        }
        val shareMethod = ShareMethod.Twitter
        val button = MixpanelButtonSettings

        sut.trackShareContent(shareMethod, null, null, comment, null, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Share Content"), argWhere { it["Comment ID"] == commentId && it["Content Type"] == "Comment" && it["Share Method"] == shareMethod.stringValue() && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
        verify(tracker).incrementUserProperty(eq("# of Shares"), eq(1.toDouble()))
        verify(tracker).trackUserProperties(argWhere { it.keys.contains("Last Shared Date") })
    }

    @Test
    fun `share post`() {
        val postSlug = "slug"
        val postTitle = "title"
        val article = mock<WorldArticle> {
            on { slug } doReturn postSlug
            on { title } doReturn postTitle
        }
        val shareMethod = ShareMethod.Standard
        val button = MixpanelButtonWorld

        sut.trackShareContent(shareMethod, null, null, null, article, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Share Content"), argWhere { it["Article Slug"] == postSlug && it["Article Name"] == postTitle && it["Content Type"] == "World" && it["Share Method"] == shareMethod.stringValue() && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
        verify(tracker).incrementUserProperty(eq("# of Shares"), eq(1.toDouble()))
        verify(tracker).trackUserProperties(argWhere { it.keys.contains("Last Shared Date") })
    }

    @Test
    fun trackAddComment() {
        val commentIdTest = "123"
        val entityIdTest = "456"
        val comment = AMComment().apply {
            uuid = commentIdTest
        }
        val entity = AMResultItem().apply {
            itemId = entityIdTest
        }
        sut.trackAddComment(comment, entity)
        verify(tracker).trackEvent(eq(MixpanelEventAddComment), argWhere { it[MixpanelPropertyCommentID] == commentIdTest })
        verify(tracker).incrementUserProperty(eq(MixpanelPropertyNumberOfCommentsAdded), eq(1.toDouble()))
        verify(tracker).trackUserProperties(argWhere { it.keys.contains(MixpanelPropertyLastCommentAddedDate) })
    }

    @Test
    fun trackUpVoteComment() {
        val commentIdTest = "123"
        val entityIdTest = "456"
        val comment = AMComment().apply {
            uuid = commentIdTest
        }
        val entity = AMResultItem().apply {
            itemId = entityIdTest
        }
        val commentMethod = CommentMethod.UpVote
        sut.trackCommentDetail(commentMethod, comment, entity)
        verify(tracker).trackEvent(eq(MixpanelEventUpvoteComment), argWhere { it[MixpanelPropertyCommentID] == commentIdTest })
        verify(tracker).incrementUserProperty(eq(MixpanelPropertyNumberOfCommentsUpvoted), eq(1.toDouble()))
    }

    @Test
    fun trackDownVoteComment() {
        val commentIdTest = "123"
        val entityIdTest = "456"
        val comment = AMComment().apply {
            uuid = commentIdTest
        }
        val entity = AMResultItem().apply {
            itemId = entityIdTest
        }
        val commentMethod = CommentMethod.DownVote
        sut.trackCommentDetail(commentMethod, comment, entity)
        verify(tracker).trackEvent(eq(MixpanelEventDownvoteComment), argWhere { it[MixpanelPropertyCommentID] == commentIdTest })
        verify(tracker).incrementUserProperty(eq(MixpanelPropertyNumberOfCommentsDownvoted), eq(1.toDouble()))
    }

    @Test
    fun trackReportComment() {
        val commentIdTest = "123"
        val entityIdTest = "456"
        val comment = AMComment().apply {
            uuid = commentIdTest
        }
        val entity = AMResultItem().apply {
            itemId = entityIdTest
        }
        val commentMethod = CommentMethod.Report
        sut.trackCommentDetail(commentMethod, comment, entity)
        verify(tracker).trackEvent(eq(MixpanelEventReportComment), argWhere { it[MixpanelPropertyCommentID] == commentIdTest })
        verify(tracker).incrementUserProperty(eq(MixpanelPropertyNumberOfCommentsReported), eq(1.toDouble()))
    }

    @Test
    fun trackError() {
        val type = "Download"
        val description = "Internal Server Error"
        sut.trackError(type, description)
        verify(tracker).trackEvent(eq("Error"), argWhere { it["Error Type"] == type && it["Error Description"] == description })
    }

    @Test
    fun trackIdentity() {
        val userId = "123"
        val userDataSource: UserDataSource = mock()
        `when`(userDataSource.getUserId()).thenReturn(userId)
        `when`(userDataSource.getUser()).thenReturn(AMArtist())
        val premiumDataSource: PremiumDataSource = mock()
        `when`(premiumDataSource.isPremium).thenReturn(false)
        sut.trackIdentity(userDataSource, premiumDataSource)
        verify(tracker).identifyUser(userId)
        verify(tracker).trackUserProperties(argWhere { it.filter { it.key.startsWith("#") }.all { it.value is Int } && it.containsKey("Badge") })
        verify(tracker).trackSuperProperties(argWhere { it["Subscription Type"] == "Free" })
    }

    @Test
    fun trackGeneralProperties() {
        val onesignalId = "123"
        sut.trackGeneralProperties(onesignalId)
        verify(tracker).trackUserProperties(argWhere {
            it.containsKey("Language") && it["\$onesignal_user_id"] == onesignalId
        })
    }

    @Test
    fun trackAppsFlyerConversion() {
        val source = "abc"
        val conversionData = mutableMapOf<String, String>().apply {
            put("media_source", source)
        }
        val firstOpen = true
        sut.trackAppsFlyerConversion(conversionData, firstOpen)
        verify(tracker).setUserPropertyOnce(any(), any())
        verify(tracker).trackSuperProperties(any())
        verify(tracker).trackEvent(eq("Install"), any())
        verify(tracker).flush()
    }

    @Test
    fun trackPushReceived() {
        sut.trackPushReceived(Intent())
        verifyZeroInteractions(tracker)
    }

    @Test
    fun trackPushOpened() {
        sut.trackPushOpened(Intent())
        verifyZeroInteractions(tracker)
    }

    @Test
    fun flushEvents() {
        sut.flushEvents()
        verify(tracker).flush()
    }

    @Test
    fun `onboarding with selection`() {
        val artist = "JOEVANGO"
        val playlist = "Verified: Hip Hop"
        val genre = "rap"
        sut.trackOnboarding(artist, playlist, genre)
        verify(tracker).trackEvent(eq("Onboarding"), argWhere { it["Artist Name"] == artist.toLowerCase() && it["Playlist Name"] == playlist })
        verify(tracker).trackUserProperties(argWhere { it["Onboarding Genre"] == genre && it.size == 1 })
    }

    @Test
    fun `onboarding without selection`() {
        sut.trackOnboarding()
        verify(tracker).trackEvent(eq("Onboarding"), argWhere { it["Artist Name"] == "No Selection" && it["Playlist Name"] == "No Selection" })
        verify(tracker).trackUserProperties(argWhere { it["Onboarding Genre"] == "No Selection" })
    }

    @Test
    fun transactionalNotificationOpened() {
        val info = TransactionalNotificationInfo("a", "b", "c", "d", "e", "f", "g", "h", "i")
        sut.trackTransactionalNotificationOpened(info)
        verify(tracker).trackEvent(eq("Transactional Push Opened"), argWhere { it.size == 9 })
    }

    @Test
    fun trackAdServed() {
        val info = mock<AdRevenueInfo>()
        sut.trackAdServed(info)
        verify(tracker).trackEvent(eq(MixpanelPropertyAdServed), argWhere { it.size == 12 })
    }

    @Test
    fun trackBillingIssue() {
        sut.trackBillingIssue()
        verify(tracker).trackEvent(eq("Billing Issue"), argWhere { it.isEmpty() })
    }

    @Test
    fun trackBellNotification() {
        val type = "Follow"
        sut.trackBellNotification(type)
        verify(tracker).trackEvent(eq("Bell Notification"), argWhere { it["Bell Type"] == type })
    }

    @Test
    fun trackScreenshot() {
        val id = "123"
        val music = Music(
            id = id,
            type = MusicType.Album
        )
        val screenshotType = "Verified Artist"
        val screenshotUser = "Creator"
        val button = MixpanelButtonSettings

        sut.trackScreenshot(screenshotType, screenshotUser, null, music, mixpanelSource, button)
        verify(tracker).trackEvent(eq("Screenshot"), argWhere { it["Screenshot Type"] == screenshotType && it["Screenshot User"] == screenshotUser && it["Album ID"] == id && it["Content Type"] == "Album" && it.validSourceParameters(mixpanelSource) && it["Button"] == button })
    }

    @Test
    fun trackSleepTimer() {
        sut.trackSleepTimer(SleepTimerSource.Settings)
        verify(tracker).trackEvent(eq("Set Sleep Timer"), argWhere { it["Source"] == "Settings" })
        sut.trackSleepTimer(SleepTimerSource.Prompt)
        verify(tracker).trackEvent(eq("Set Sleep Timer"), argWhere { it["Source"] == "Prompt" })
    }

    @Test
    fun trackTrendingBannerClick() {
        val url = "https://domain"
        sut.trackTrendingBannerClick(url)
        verify(tracker).trackEvent(eq("Trending Message Bar"), argWhere { it["URL"] == url })
    }

    @Test
    fun trackResetPassword() {
        val email = "test@email.com"
        sut.trackResetPassword(email)
        verify(tracker).trackEvent(eq("Reset Password"), argWhere { it["\$email"] == email })
    }

    @Test
    fun trackChangePassword() {
        sut.trackChangePassword()
        verify(tracker).trackEvent(eq("Change Password"), argWhere { it.isEmpty() })
    }

    @Test
    fun trackRestoreDownloads() {
        val count = 100
        sut.trackRestoreDownloads(RestoreDownloadsMode.All, count)
        sut.trackRestoreDownloads(RestoreDownloadsMode.Manually, count)
        verify(tracker, times(1)).trackEvent(eq("Restore Downloads"), argWhere { it["# of Downloads Restored"] == count && it["Button"] == "Restore All" })
        verify(tracker, times(1)).trackEvent(eq("Restore Downloads"), argWhere { it["# of Downloads Restored"] == count && it["Button"] == "Restore Manually" })
    }

    @Test
    fun trackFollowPushPermissionPrompt() {
        sut.trackFollowPushPermissionPrompt(true)
        verify(tracker, times(1)).trackEvent(eq("Follow Permission Prompt"), argWhere { it.size == 1 && it["Answer"] == "Grant" })
        sut.trackFollowPushPermissionPrompt(false)
        verify(tracker, times(1)).trackEvent(eq("Follow Permission Prompt"), argWhere { it.size == 1 && it["Answer"] == "Decline" })
    }

    @Test
    fun trackPremiumDownloadNotification() {
        sut.trackPremiumDownloadNotification(PremiumDownloadType.Limited)
        verify(tracker, times(1)).trackEvent(eq("Premium Download Notification"), argWhere { it.size == 1 && it["Message Type"] == "Limited" })
        sut.trackPremiumDownloadNotification(PremiumDownloadType.PremiumOnly)
        verify(tracker, times(1)).trackEvent(eq("Premium Download Notification"), argWhere { it.size == 1 && it["Message Type"] == "Premium-Only" })
    }

    @Test
    fun trackLocalFileOpened() {
        val songName = "song"
        val artistName = "artist"
        sut.trackLocalFileOpened(songName, artistName)
        verify(tracker, times(1)).trackEvent(
            eq("Local File Opened"),
            argWhere { it["Song Name"] == songName && it["Artist Name"] == artistName }
        )
    }

    private fun Map<String, Any>.validSourceParameters(mixpanelSource: MixpanelSource): Boolean {
        return this["Source Tab"] == mixpanelSource.tab &&
            this["Source Page"] == mixpanelSource.page &&
            (mixpanelSource.extraParams ?: emptyList()).all { this[it.first] == it.second }
    }
}
