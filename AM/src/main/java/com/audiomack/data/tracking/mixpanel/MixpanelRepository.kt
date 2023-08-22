package com.audiomack.data.tracking.mixpanel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BadParcelableException
import androidx.core.content.ContextCompat
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
import com.audiomack.model.MusicType
import com.audiomack.model.PermissionType
import com.audiomack.model.QueueType
import com.audiomack.model.SearchReturnType
import com.audiomack.model.SearchType
import com.audiomack.model.ShareMethod
import com.audiomack.model.SongEndType
import com.audiomack.model.WorldArticle
import com.audiomack.onesignal.TransactionalNotificationInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class MixpanelRepository(
    private val mixpanelTracker: MixpanelTracker = MixpanelTrackerImpl
) : MixpanelDataSource {

    private val TAG = MixpanelRepository::class.java.simpleName
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun trackViewSignupPage(source: LoginSignupSource) {
        mixpanelTracker.trackEvent(MixpanelEventViewSignupPage, mapOf(
            Pair(MixpanelPropertyButton, source.stringValue)
        ))
    }

    override fun trackCreateAccount(
        source: LoginSignupSource,
        authenticationType: AuthenticationType,
        userDataSource: UserDataSource,
        premiumDataSource: PremiumDataSource
    ) {
        mixpanelTracker.trackSuperProperties(mapOf(
            Pair(MixpanelPropertyUserID, userDataSource.getUserId() ?: ""),
            Pair(
                MixpanelPropertySubscriptionType,
                if (!premiumDataSource.isPremium) MixpanelSubscriptionFree else MixpanelSubscriptionPremium
            )
        ))
        mixpanelTracker.trackEvent(MixpanelEventCreateAccount, mapOf(
            Pair(MixpanelPropertyAuthenticationType, authenticationType.stringValue),
            Pair(MixpanelPropertyButton, source.stringValue)
        ))
        val userPropertiesMap = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertyUserID, userDataSource.getUserId() ?: "")
            put(MixpanelPropertyAuthenticationType, authenticationType.stringValue)
            put(MixpanelPropertySignupDate, dateFormatter.format(Date()))
            put(
                MixpanelPropertySubscriptionType,
                if (!premiumDataSource.isPremium) MixpanelSubscriptionFree else MixpanelSubscriptionPremium
            )
        }
        mixpanelTracker.trackUserProperties(userPropertiesMap)
    }

    override fun trackPromptPermissions(permissionType: PermissionType) {
        mixpanelTracker.trackEvent(MixpanelEventPromptPermissions, mapOf(
            Pair(MixpanelPropertyPermissionType, permissionType.stringValue())
        ))
    }

    override fun trackEnablePermissions(context: Context, permissions: Array<String>, grantResults: IntArray) {
        val locationEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val storageEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val cameraEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        val permissionTypes: List<PermissionType> = permissions.indices.mapNotNull {
            when (permissions[it]) {
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> PermissionType.Storage
                Manifest.permission.ACCESS_COARSE_LOCATION -> PermissionType.Location
                Manifest.permission.CAMERA -> PermissionType.Camera
                else -> {
                    Timber.tag(TAG).w("Permission type not handled yet: ${permissions[it]}")
                    null
                }
            }
        }

        permissionTypes.forEach {
            mixpanelTracker.trackEvent(MixpanelEventEnablePermissions, mapOf(
                Pair(MixpanelPropertyPermissionType, it.stringValue())
            ))
        }
        mixpanelTracker.trackUserProperties(mapOf(
            Pair(MixpanelPropertyLocationEnabled, locationEnabled),
            Pair(MixpanelPropertyNotificationEnabled, true),
            Pair(MixpanelPropertyStorageEnabled, storageEnabled),
            Pair(MixpanelPropertyCameraEnabled, cameraEnabled)
        ))
    }

    override fun trackLogin(
        source: LoginSignupSource,
        authenticationType: AuthenticationType,
        userDataSource: UserDataSource,
        premiumDataSource: PremiumDataSource,
        telcoDataSource: TelcoDataSource
    ) {
        val superProperties = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertyUserID, userDataSource.getUserId() ?: "")
            put(
                MixpanelPropertySubscriptionType,
                if (!premiumDataSource.isPremium) MixpanelSubscriptionFree else MixpanelSubscriptionPremium
            )
            put(MixpanelPropertyTAPhoneCount, telcoDataSource.getPhoneCount()?.toString() ?: "N/A")
            put(MixpanelPropertyTAPhoneType, telcoDataSource.getPhoneType() ?: "N/A")
            put(MixpanelPropertyTASimOperator, telcoDataSource.getSimOperator() ?: "N/A")
            put(MixpanelPropertyTASimOperatorName, telcoDataSource.getSimOperatorName() ?: "N/A")
            put(MixpanelPropertyTASimCarrierId, telcoDataSource.getSimCarrierId()?.toString() ?: "N/A")
            put(MixpanelPropertyTASimCarrierIdName, telcoDataSource.getSimCarrierIdName() ?: "N/A")
            put(MixpanelPropertyCarrierName, telcoDataSource.getSimOperatorName() ?: "N/A")
            put(MixpanelPropertyMCC, telcoDataSource.getMobileCountryCode() ?: "N/A")
            put(MixpanelPropertyMNC, telcoDataSource.getMobileNetworkCode() ?: "N/A")
            put(MixpanelPropertyIsWifi, telcoDataSource.isWifi())
        }
        mixpanelTracker.trackSuperProperties(superProperties)

        mixpanelTracker.trackEvent(MixpanelEventLogin, mapOf(
            Pair(MixpanelPropertyButton, source.stringValue),
            Pair(MixpanelPropertyAuthenticationType, authenticationType.stringValue)
        ))

        val userProperties = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertyAuthenticationType, authenticationType.stringValue)
            put(MixpanelPropertyLastLoginDate, dateFormatter.format(Date()))
            put(
                MixpanelPropertySubscriptionType,
                if (!premiumDataSource.isPremium) MixpanelSubscriptionFree else MixpanelSubscriptionPremium
            )
            put(MixpanelPropertyUserID, userDataSource.getUserId() ?: "")
        }
        mixpanelTracker.trackUserProperties(userProperties)

        mixpanelTracker.setUserPropertyOnce(MixpanelPropertyFirstLoginDate, dateFormatter.format(Date()))
    }

    override fun trackLogout() {
        mixpanelTracker.trackEvent(MixpanelEventLogout, mapOf())
        mixpanelTracker.reset()
    }

    override fun trackViewPremiumSubscription(source: InAppPurchaseMode) {
        mixpanelTracker.trackEvent(MixpanelEventViewPremiumSubscription, mapOf(
            Pair(MixpanelPropertyButton, source.stringValue())
        ))
    }

    override fun trackPremiumCheckoutStarted(source: InAppPurchaseMode) {
        mixpanelTracker.trackEvent(MixpanelEventPremiumCheckoutStarted, mapOf(
            Pair(MixpanelPropertyButton, source.stringValue())
        ))
    }

    override fun trackPurchasePremiumTrial(
        source: InAppPurchaseMode,
        inAppPurchaseDataSource: InAppPurchaseDataSource
    ) {
        mixpanelTracker.trackSuperProperties(mapOf(
            Pair(MixpanelPropertySubscriptionType, MixpanelSubscriptionPremium)
        ))
        mixpanelTracker.trackEvent(MixpanelEventPurchasePremiumTrial, mapOf(
            Pair(MixpanelPropertyButton, source.stringValue()),
            Pair(MixpanelPropertyMonthlySubAmount, inAppPurchaseDataSource.getSubscriptionPrice()),
            Pair(MixpanelPropertyMonthlySubCurrency, inAppPurchaseDataSource.getCurrency())
        ))
        mixpanelTracker.trackUserProperties(mapOf(
            Pair(MixpanelPropertySubscriptionType, MixpanelSubscriptionPremium),
            Pair(MixpanelPropertyMonthlySubAmount, inAppPurchaseDataSource.getSubscriptionPrice()),
            Pair(MixpanelPropertyMonthlySubCurrency, inAppPurchaseDataSource.getCurrency())
        ))
    }

    override fun trackCancelSubscription(inAppPurchaseDataSource: InAppPurchaseDataSource) {
        mixpanelTracker.trackEvent(MixpanelEventCancelSubscription, mapOf(
            Pair(MixpanelPropertyMonthlySubCurrency, inAppPurchaseDataSource.getCurrency())
        ))
        mixpanelTracker.trackUserProperties(mapOf(
            Pair(MixpanelPropertySubscriptionType, MixpanelSubscriptionFree),
            Pair(MixpanelPropertyMonthlySubAmount, inAppPurchaseDataSource.getSubscriptionPrice()),
            Pair(MixpanelPropertyMonthlySubCurrency, inAppPurchaseDataSource.getCurrency()),
            Pair(MixpanelPropertyCancellationDate, dateFormatter.format(Date()))
        ))
    }

    override fun trackPlaySong(song: AMResultItem, durationPlayed: Int, endType: SongEndType, source: MixpanelSource, button: String) {
        if (source == MixpanelSource.empty) {
            Timber.tag(TAG).e("Invalid MixpanelSource for `trackPlaySong`: $song")
            return
        }
        val eventProperties = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertySongName, song.title?.toLowerCase(Locale.US) ?: "")
            put(MixpanelPropertySongId, song.itemId ?: "")
            put(MixpanelPropertyArtistName, song.artist?.toLowerCase(Locale.US) ?: "")
            song.songReleaseDate.takeIf { it > 0L }?.let {
                put(MixpanelPropertySongReleaseDate, dateFormatter.format(Date(it)))
            }
            if (song.isAlbumTrack || song.isAlbumTrackDownloadedAsSingle) {
                put(MixpanelPropertyAlbumName, song.album?.toLowerCase(Locale.US) ?: "")
                put(MixpanelPropertyAlbumId, song.parentId ?: "")
                song.albumReleaseDate.takeIf { it > 0L }?.let {
                    put(MixpanelPropertyAlbumReleaseDate, dateFormatter.format(Date(it)))
                }
            } else if (song.isPlaylistTrack) {
                put(MixpanelPropertyPlaylistId, song.parentId ?: "")
                put(MixpanelPropertyPlaylistName, song.playlist ?: "")
            }
            put(MixpanelPropertyGenre, song.genre ?: "")
            put(MixpanelPropertySourcePage, source.page)
            put(MixpanelPropertyButton, button)
            put(MixpanelPropertySourceTab, source.tab)
            put(MixpanelPropertyDurationPlayed, durationPlayed)
            put(MixpanelPropertySongEndType, endType.stringValue())
            put(MixpanelPropertyOffline, song.isDownloadCompleted)
            put(MixpanelPropertyShuffle, source.shuffled)
            put(MixpanelPropertyMusicTags, song.tags.toList())
            source.extraParams?.let {
                putAll(it)
            }
        }
        mixpanelTracker.trackEvent(MixpanelEventPlaySong, eventProperties)
        mixpanelTracker.trackUserProperties(mapOf(
            Pair(MixpanelPropertyLastSongPlayedDate, dateFormatter.format(Date()))
        ))
    }

    override fun trackDownloadToOffline(music: AMResultItem, source: MixpanelSource, button: String) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            when {
                music.isAlbum -> {
                    put(MixpanelPropertyAlbumName, music.title?.toLowerCase(Locale.US) ?: "")
                    put(MixpanelPropertyAlbumId, music.itemId ?: "")
                }
                music.isPlaylist -> {
                    put(MixpanelPropertyPlaylistId, music.itemId ?: "")
                    put(MixpanelPropertyPlaylistName, music.title ?: "")
                }
                else -> {
                    put(MixpanelPropertySongName, music.title?.toLowerCase(Locale.US) ?: "")
                    put(MixpanelPropertySongId, music.itemId ?: "")
                }
            }
            put(MixpanelPropertyArtistName, music.artist?.toLowerCase(Locale.US) ?: "")
            put(MixpanelPropertyGenre, music.genre ?: "")
            put(MixpanelPropertyPremiumDownload, music.premiumDownloadRawString)
            put(MixpanelPropertySourcePage, source.page)
            put(MixpanelPropertyButton, button)
            put(MixpanelPropertySourceTab, source.tab)
            source.extraParams?.let {
                putAll(it)
            }
        }
        mixpanelTracker.trackEvent(MixpanelEventDownloadToOffline, eventProperties)
    }

    override fun trackCreatePlaylist(playlist: AMResultItem) {
        mixpanelTracker.trackEvent(MixpanelEventCreatePlaylist, mapOf(
            Pair(MixpanelPropertyPlaylistId, playlist.itemId ?: ""),
            Pair(MixpanelPropertyPlaylistName, playlist.title ?: "")
        ))
    }

    override fun trackAddToPlaylist(music: Music, playlist: AMResultItem, source: MixpanelSource, button: String) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertySongName, music.title.toLowerCase(Locale.US))
            put(MixpanelPropertySongId, music.id)
            put(MixpanelPropertyArtistName, music.artist.toLowerCase(Locale.US))
            put(MixpanelPropertyGenre, music.genre)
            put(MixpanelPropertySourcePage, source.page)
            put(MixpanelPropertyButton, button)
            put(MixpanelPropertySourceTab, source.tab)
            put(MixpanelPropertyPlaylistId, playlist.itemId ?: "")
            put(MixpanelPropertyPlaylistName, playlist.title ?: "")
            source.extraParams?.let {
                putAll(it)
            }
        }
        mixpanelTracker.trackEvent(MixpanelEventAddToPlaylist, eventProperties)
    }

    override fun trackHighlight(music: AMResultItem, source: MixpanelSource, button: String) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            when {
                music.isAlbum -> {
                    put(MixpanelPropertyAlbumName, music.title?.toLowerCase(Locale.US) ?: "")
                    put(MixpanelPropertyAlbumId, music.itemId ?: "")
                    put(MixpanelPropertyContentType, MixpanelContentTypeAlbum)
                }
                else -> {
                    put(MixpanelPropertySongName, music.title?.toLowerCase(Locale.US) ?: "")
                    put(MixpanelPropertySongId, music.itemId ?: "")
                    put(MixpanelPropertyContentType, MixpanelContentTypeSong)
                }
            }
            put(MixpanelPropertyArtistName, music.artist?.toLowerCase(Locale.US) ?: "")
            put(MixpanelPropertyGenre, music.genre ?: "")
            put(MixpanelPropertySourcePage, source.page)
            put(MixpanelPropertyButton, button)
            put(MixpanelPropertySourceTab, source.tab)
            source.extraParams?.let {
                putAll(it)
            }
        }
        mixpanelTracker.trackEvent(MixpanelEventHighlight, eventProperties)
        mixpanelTracker.trackUserProperties(mapOf(
            Pair(MixpanelPropertyLastHighlightedDate, dateFormatter.format(Date()))
        ))
    }

    override fun trackAddToFavorites(music: AMResultItem, source: MixpanelSource, button: String) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            when {
                music.isAlbum -> {
                    put(MixpanelPropertyAlbumName, music.title?.toLowerCase(Locale.US) ?: "")
                    put(MixpanelPropertyAlbumId, music.itemId ?: "")
                    put(MixpanelPropertyContentType, MixpanelContentTypeAlbum)
                }
                music.isPlaylist -> {
                    put(MixpanelPropertyPlaylistId, music.itemId ?: "")
                    put(MixpanelPropertyPlaylistName, music.title ?: "")
                    put(MixpanelPropertyContentType, MixpanelContentTypePlaylist)
                }
                else -> {
                    put(MixpanelPropertySongName, music.title?.toLowerCase(Locale.US) ?: "")
                    put(MixpanelPropertySongId, music.itemId ?: "")
                    put(MixpanelPropertyContentType, MixpanelContentTypeSong)
                }
            }
            put(MixpanelPropertyArtistName, music.artist?.toLowerCase(Locale.US) ?: "")
            put(MixpanelPropertyGenre, music.genre ?: "")
            put(MixpanelPropertySourcePage, source.page)
            put(MixpanelPropertyButton, button)
            put(MixpanelPropertySourceTab, source.tab)
            source.extraParams?.let {
                putAll(it)
            }
            put(MixpanelPropertyCreatorUserId, music.uploaderId ?: "")
        }
        mixpanelTracker.trackEvent(MixpanelEventAddToFavorites, eventProperties)
        mixpanelTracker.trackUserProperties(mapOf(
            Pair(MixpanelPropertyLastFavoritedDate, dateFormatter.format(Date()))
        ))
    }

    override fun trackQueue(music: AMResultItem, queueType: QueueType, source: MixpanelSource, button: String) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            when {
                music.isAlbum -> {
                    put(MixpanelPropertyAlbumName, music.title?.toLowerCase(Locale.US) ?: "")
                    put(MixpanelPropertyAlbumId, music.itemId ?: "")
                    put(MixpanelPropertyContentType, MixpanelContentTypeAlbum)
                }
                music.isPlaylist -> {
                    put(MixpanelPropertyPlaylistId, music.itemId ?: "")
                    put(MixpanelPropertyPlaylistName, music.title ?: "")
                    put(MixpanelPropertyContentType, MixpanelContentTypePlaylist)
                }
                else -> {
                    put(MixpanelPropertySongName, music.title?.toLowerCase(Locale.US) ?: "")
                    put(MixpanelPropertySongId, music.itemId ?: "")
                    put(MixpanelPropertyContentType, MixpanelContentTypeSong)
                }
            }
            put(MixpanelPropertyArtistName, music.artist?.toLowerCase(Locale.US) ?: "")
            put(MixpanelPropertyGenre, music.genre ?: "")
            put(MixpanelPropertySourcePage, source.page)
            put(MixpanelPropertyButton, button)
            put(MixpanelPropertySourceTab, source.tab)
            source.extraParams?.let {
                putAll(it)
            }
            put(MixpanelPropertyQueueType, queueType.stringValue())
        }
        mixpanelTracker.trackEvent(MixpanelEventQueue, eventProperties)
    }

    override fun trackSearch(query: String, type: SearchType, returnType: SearchReturnType) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertySearchTerm, query.toLowerCase(Locale.US))
            put(MixpanelPropertySearchType, type.stringValue())
            put(MixpanelPropertySearchReturnType, returnType.stringValue())
        }
        mixpanelTracker.trackEvent(MixpanelEventSearch, eventProperties)
        mixpanelTracker.trackUserProperties(mapOf(
            Pair(MixpanelPropertyLastSearchDate, dateFormatter.format(Date()))
        ))
    }

    override fun trackReUp(music: AMResultItem, source: MixpanelSource, button: String) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            when {
                music.isAlbum -> {
                    put(MixpanelPropertyAlbumName, music.title?.toLowerCase(Locale.US) ?: "")
                    put(MixpanelPropertyAlbumId, music.itemId ?: "")
                }
                else -> {
                    put(MixpanelPropertySongName, music.title?.toLowerCase(Locale.US) ?: "")
                    put(MixpanelPropertySongId, music.itemId ?: "")
                }
            }
            put(MixpanelPropertyArtistName, music.artist?.toLowerCase(Locale.US) ?: "")
            put(MixpanelPropertyGenre, music.genre ?: "")
            put(MixpanelPropertySourcePage, source.page)
            put(MixpanelPropertyButton, button)
            put(MixpanelPropertySourceTab, source.tab)
            source.extraParams?.let {
                putAll(it)
            }
        }
        mixpanelTracker.trackEvent(MixpanelEventReup, eventProperties)
        mixpanelTracker.trackUserProperties(mapOf(
            Pair(MixpanelPropertyLastReuppedDate, dateFormatter.format(Date()))
        ))
    }

    override fun trackFollowAccount(accountName: String, accountId: String, source: MixpanelSource, button: String) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertyAccountName, accountName)
            put(MixpanelPropertyAccountId, accountId)
            put(MixpanelPropertySourcePage, source.page)
            put(MixpanelPropertyButton, button)
            put(MixpanelPropertySourceTab, source.tab)
            source.extraParams?.let {
                putAll(it)
            }
        }
        mixpanelTracker.trackEvent(MixpanelEventFollowAccount, eventProperties)
    }

    override fun trackUnfollowAccount(accountName: String, accountId: String, source: MixpanelSource, button: String) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertyAccountName, accountName)
            put(MixpanelPropertyAccountId, accountId)
            put(MixpanelPropertySourcePage, source.page)
            put(MixpanelPropertyButton, button)
            put(MixpanelPropertySourceTab, source.tab)
            source.extraParams?.let {
                putAll(it)
            }
        }
        mixpanelTracker.trackEvent(MixpanelEventUnfollowAccount, eventProperties)
    }

    override fun trackShareContent(method: ShareMethod, artist: AMArtist?, music: AMResultItem?, comment: AMComment?, article: WorldArticle?, source: MixpanelSource, button: String) {

        val map = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertyShareMethod, method.stringValue())
            artist?.let {
                put(MixpanelPropertyContentType, MixpanelContentTypeAccount)
                put(MixpanelPropertyAccountName, it.name ?: "")
                put(MixpanelPropertyAccountId, it.artistId ?: "")
            }
            music?.let {
                when {
                    it.isPlaylist -> {
                        put(MixpanelPropertyContentType, MixpanelContentTypePlaylist)
                        put(MixpanelPropertyPlaylistId, music.itemId ?: "")
                        put(MixpanelPropertyPlaylistName, music.title ?: "")
                    }
                    it.isAlbum -> {
                        put(MixpanelPropertyContentType, MixpanelContentTypeAlbum)
                        put(MixpanelPropertyAlbumName, music.title?.toLowerCase(Locale.US) ?: "")
                        put(MixpanelPropertyAlbumId, music.itemId ?: "")
                    }
                    else -> {
                        put(MixpanelPropertyContentType, MixpanelContentTypeSong)
                        put(MixpanelPropertySongName, music.title?.toLowerCase(Locale.US) ?: "")
                        put(MixpanelPropertySongId, music.itemId ?: "")
                    }
                }
                put(MixpanelPropertyArtistName, music.artist?.toLowerCase(Locale.US) ?: "")
                put(MixpanelPropertyGenre, music.genre ?: "")
            }
            comment?.let {
                put(MixpanelPropertyContentType, MixpanelContentTypeComment)
                put(MixpanelPropertyCommentID, comment.entityId ?: "")
            }
            article?.let {
                put(MixpanelPropertyContentType, MixpanelContentTypeWorld)
                put(MixpanelPropertyArticleName, article.title ?: "")
                put(MixpanelPropertyArticleSlug, article.slug ?: "")
                article.publishedDate()?.let { put(MixpanelPropertyArticleDate, it) }
            }
            put(MixpanelPropertySourcePage, source.page)
            put(MixpanelPropertyButton, button)
            put(MixpanelPropertySourceTab, source.tab)
            source.extraParams?.let {
                putAll(it)
            }
        }
        mixpanelTracker.trackEvent(MixpanelEventShareComment, map)
        mixpanelTracker.incrementUserProperty(MixpanelPropertyNumberOfShares, 1.toDouble())
        mixpanelTracker.trackUserProperties(mapOf(
            Pair(MixpanelPropertyLastSharedDate, dateFormatter.format(Date()))
        ))
    }

    override fun trackError(type: String, description: String) {
        val map = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertyErrorType, type)
            put(MixpanelPropertyErrorDescription, description)
        }
        mixpanelTracker.trackEvent(MixpanelEventError, map)
    }

    override fun trackIdentity(
        userDataSource: UserDataSource,
        premiumDataSource: PremiumDataSource
    ) {
        val userId = userDataSource.getUserId() ?: return
        val userEmail = userDataSource.getEmail() ?: ""
        val userSlug = userDataSource.getUserSlug() ?: ""
        val subscriptionType =
            if (!premiumDataSource.isPremium) MixpanelSubscriptionFree else MixpanelSubscriptionPremium

        mixpanelTracker.identifyUser(userId)

        val userProperties = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertyEmail, userEmail)
            put(MixpanelPropertyUserSlug, userSlug)
            put(MixpanelPropertyLanguage, Locale.getDefault().language)
            val artist = userDataSource.getUser() ?: return@apply
            put(MixpanelPropertyNumberOfReups, artist.reupsCount)
            put(MixpanelPropertyNumberOfFavorites, artist.favoritesCount)
            put(MixpanelPropertyNumberOfFollowers, artist.followersCount)
            put(MixpanelPropertyNumberOfFollowing, artist.followingCount)
            put(MixpanelPropertyNumberOfOfflineDownloads, userDataSource.getOfflineDownloadsCount())
            put(MixpanelPropertyNumberOfPremiumLimitedDownloads, userDataSource.getPremiumLimitedDownloadsCount())
            put(MixpanelPropertyNumberOfPremiumOnlyDownloads, userDataSource.getPremiumOnlyDownloadsCount())
            put(MixpanelPropertyNumberOfPlaylistsCreated, artist.playlistsCount)
            put(MixpanelPropertyNumberOfHighlighted, artist.pinnedCount)
            put(MixpanelPropertyNumberOfUploads, artist.uploadsCount)
            put(MixpanelPropertyDisplayName, artist.name ?: "")
            put(MixpanelPropertySubscriptionType, subscriptionType)
            put(MixpanelPropertyUserBadge, when {
                artist.isVerified -> MixpanelUserBadgeVerified
                artist.isTastemaker -> MixpanelUserBadgeTastemaker
                artist.isAuthenticated -> MixpanelUserBadgeAuthenticated
                else -> MixpanelUserBadgeUnauthenticated
            })
            put(MixpanelPropertyGender, artist.gender?.toString() ?: "")
            put(MixpanelPropertyBirthday, artist.birthday ?: "")
        }

        mixpanelTracker.trackUserProperties(userProperties)

        mixpanelTracker.trackSuperProperties(mapOf(
            MixpanelPropertySubscriptionType to subscriptionType
        ))
    }

    override fun trackGeneralProperties(oneSignalUserId: String?) {
        val map = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertyLanguage, Locale.getDefault().language)
            oneSignalUserId?.let {
                put(MixpanelPropertyOnesignalUserId, it)
            }
        }
        mixpanelTracker.trackUserProperties(map)
    }

    override fun trackAppsFlyerConversion(conversionData: Map<String, Any>, installation: Boolean) {
        val mediaSource = conversionData["media_source"]
        val campaign = conversionData["campaign"]

        val superProperties = mutableMapOf<String, Any>().apply {
            if (mediaSource != null) {
                put(MixpanelPropertyAttributionMediaSource, mediaSource)
            } else if (campaign != null) {
                put(MixpanelPropertyAttributionCampaign, campaign)
            }
        }
        if (superProperties.isNotEmpty()) {
            mixpanelTracker.trackSuperProperties(superProperties)
            superProperties.forEach {
                mixpanelTracker.setUserPropertyOnce(it.key, it.value)
            }
        }
        if (installation) {
            mixpanelTracker.trackEvent(MixpanelEventInstall, emptyMap())
        } else {
            mixpanelTracker.trackEvent(MixpanelEventAppOpened, emptyMap())
        }
        mixpanelTracker.flush()
    }

    override fun trackPushReceived(intent: Intent) {
        val campaignId = if (intent.hasExtra("mp_campaign_id")) intent.getStringExtra("mp_campaign_id") else null
        val messageId = if (intent.hasExtra("mp_message_id")) intent.getStringExtra("mp_message_id") else null
        val extraLogData = if (intent.hasExtra("mp")) intent.getStringExtra("mp") else null
        if (campaignId != null && messageId != null) {
            if (mixpanelTracker.isAppInForeground()) {
                var pushProps = JSONObject()
                try {
                    if (extraLogData != null) {
                        pushProps = JSONObject(extraLogData)
                    }
                } catch (e: JSONException) {
                    Timber.w(e)
                }
                try {
                    pushProps.put(MixpanelPropertyCampaignId, Integer.valueOf(campaignId))
                    pushProps.put(MixpanelPropertyMessageId, Integer.valueOf(messageId))
                    pushProps.put(MixpanelPropertyMessageType, "push")
                    val map = pushProps.keys().asSequence().map { Pair(it, pushProps.get(it)) }.toMap()
                    mixpanelTracker.trackEvent(MixpanelEventCampaignReceived, map)
                } catch (e: JSONException) {
                    Timber.w(e)
                }
            }
        }
    }

    override fun trackPushOpened(intent: Intent) {
        try {
            if (intent.hasExtra("mp_campaign_id") && intent.hasExtra("mp_message_id")) {
                val campaignId = intent.getStringExtra("mp_campaign_id")
                val messageId = intent.getStringExtra("mp_message_id")
                val extraLogData = intent.getStringExtra("mp")

                try {
                    val pushProps = if (extraLogData != null) {
                        JSONObject(extraLogData)
                    } else {
                        JSONObject()
                    }
                    pushProps.put(MixpanelPropertyCampaignId, campaignId.toIntOrNull())
                    pushProps.put(MixpanelPropertyMessageId, messageId.toIntOrNull())
                    pushProps.put(MixpanelPropertyMessageType, "push")
                    val map = pushProps.keys().asSequence().map { Pair(it, pushProps.get(it)) }.toMap()
                    mixpanelTracker.trackEvent(MixpanelEventAppOpen, map)
                } catch (e: JSONException) {
                    Timber.w(e)
                }
                intent.removeExtra("mp_campaign_id")
                intent.removeExtra("mp_message_id")
                intent.removeExtra("mp")
            }
        } catch (e: BadParcelableException) {
            // https://github.com/mixpanel/mixpanel-android/issues/251
        }
    }

    override fun flushEvents() {
        mixpanelTracker.flush()
    }

    override fun trackAddComment(comment: AMComment?, entity: AMResultItem?) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            comment?.let {
                put(MixpanelPropertyCommentRoot, comment.threadUuid.isNullOrEmpty())
                put(MixpanelPropertyNumberOfUpvotes, comment.upVotes ?: 0)
                put(MixpanelPropertyNumberOfDownvotes, comment.downVotes ?: 0)
                put(MixpanelPropertyCreatorID, comment.userId ?: "")
                put(MixpanelPropertyCommentID, comment.uuid ?: "")
            }
            entity?.let {
                when {
                    it.isPlaylist -> {
                        put(MixpanelPropertyPlaylistId, entity.itemId ?: "")
                        put(MixpanelPropertyPlaylistName, entity.title ?: "")
                    }
                    it.isAlbum -> {
                        put(MixpanelPropertyAlbumName, entity.title?.toLowerCase(Locale.US) ?: "")
                        put(MixpanelPropertyAlbumId, entity.itemId ?: "")
                    }
                    else -> {
                        put(MixpanelPropertySongName, entity.title?.toLowerCase(Locale.US) ?: "")
                        put(MixpanelPropertySongId, entity.itemId ?: "")
                    }
                }
                put(MixpanelPropertyArtistName, entity.artist?.toLowerCase(Locale.US) ?: "")
                put(MixpanelPropertyGenre, entity.genre ?: "")
            }
        }
        mixpanelTracker.trackEvent(MixpanelEventAddComment, eventProperties)
        mixpanelTracker.incrementUserProperty(MixpanelPropertyNumberOfCommentsAdded, 1.toDouble())
        mixpanelTracker.setUserPropertyOnce(MixpanelPropertyFirstCommentAddedDate, dateFormatter.format(Date()))
        mixpanelTracker.trackUserProperties(mapOf(Pair(MixpanelPropertyLastCommentAddedDate, dateFormatter.format(Date()))))
    }

    override fun trackCommentDetail(method: CommentMethod, comment: AMComment?, entity: AMResultItem?) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            comment?.let {
                put(MixpanelPropertyCommentRoot, comment.threadUuid.isNullOrEmpty())
                put(MixpanelPropertyNumberOfUpvotes, comment.upVotes ?: 0)
                put(MixpanelPropertyNumberOfDownvotes, comment.downVotes ?: 0)
                put(MixpanelPropertyCreatorID, comment.userId ?: "")
                put(MixpanelPropertyCommentID, comment.uuid ?: "")
            }
            entity?.let {
                when {
                    it.isPlaylist -> {
                        put(MixpanelPropertyPlaylistId, entity.itemId ?: "")
                        put(MixpanelPropertyPlaylistName, entity.title ?: "")
                    }
                    it.isAlbum -> {
                        put(MixpanelPropertyAlbumName, entity.title?.toLowerCase(Locale.US) ?: "")
                        put(MixpanelPropertyAlbumId, entity.itemId ?: "")
                    }
                    else -> {
                        put(MixpanelPropertySongName, entity.title?.toLowerCase(Locale.US) ?: "")
                        put(MixpanelPropertySongId, entity.itemId ?: "")
                    }
                }
                put(MixpanelPropertyArtistName, entity.artist?.toLowerCase(Locale.US) ?: "")
                put(MixpanelPropertyGenre, entity.genre ?: "")
            }
        }
        when (method) {
            CommentMethod.UpVote -> {
                mixpanelTracker.trackEvent(MixpanelEventUpvoteComment, eventProperties)
                mixpanelTracker.incrementUserProperty(MixpanelPropertyNumberOfCommentsUpvoted, 1.toDouble())
            }
            CommentMethod.DownVote -> {
                mixpanelTracker.trackEvent(MixpanelEventDownvoteComment, eventProperties)
                mixpanelTracker.incrementUserProperty(MixpanelPropertyNumberOfCommentsDownvoted, 1.toDouble())
            }
            CommentMethod.Report -> {
                mixpanelTracker.trackEvent(MixpanelEventReportComment, eventProperties)
                mixpanelTracker.incrementUserProperty(MixpanelPropertyNumberOfCommentsReported, 1.toDouble())
            }
        }
    }

    override fun trackOnboarding(artistName: String?, playlistName: String?, genre: String?) {
        val noSelection = artistName == null && playlistName == null && genre == null
        if (noSelection) {
            val eventProperties = mapOf(
                Pair(MixpanelPropertyArtistName, MixpanelPropertyValueNoSelection),
                Pair(MixpanelPropertyPlaylistName, MixpanelPropertyValueNoSelection)
            )
            mixpanelTracker.trackEvent(MixpanelEventOnboarding, eventProperties)
            val userProperties = mapOf(
                Pair(MixpanelPropertyOnboardingGenre, MixpanelPropertyValueNoSelection)
            )
            mixpanelTracker.trackUserProperties(userProperties)
        } else {
            val artistNameNonNull = artistName ?: return
            val playlistNameNonNull = playlistName ?: return
            val genreNonNull = genre ?: return
            val eventProperties = mapOf(
                Pair(MixpanelPropertyArtistName, artistNameNonNull.toLowerCase(Locale.US)),
                Pair(MixpanelPropertyPlaylistName, playlistNameNonNull)
            )
            mixpanelTracker.trackEvent(MixpanelEventOnboarding, eventProperties)
            val userProperties = mapOf(
                Pair(MixpanelPropertyOnboardingGenre, genreNonNull)
            )
            mixpanelTracker.trackUserProperties(userProperties)
        }
    }

    override fun trackTransactionalNotificationOpened(info: TransactionalNotificationInfo) {
        mixpanelTracker.trackEvent(MixpanelEventTransactionalPushOpened, mutableMapOf<String, Any>().apply {
            info.songName?.let { put(MixpanelPropertySongName, it.toLowerCase(Locale.US)) }
            info.songId?.let { put(MixpanelPropertySongId, it) }
            info.albumName?.let { put(MixpanelPropertyAlbumName, it.toLowerCase(Locale.US)) }
            info.albumId?.let { put(MixpanelPropertyAlbumId, it) }
            info.playlistName?.let { put(MixpanelPropertyPlaylistName, it) }
            info.playlistId?.let { put(MixpanelPropertyPlaylistId, it) }
            info.artistName?.let { put(MixpanelPropertyArtistName, it.toLowerCase(Locale.US)) }
            info.genre?.let { put(MixpanelPropertyGenre, it) }
            info.campaign?.let { put(MixpanelPropertyCampaign, it) }
        })
    }

    override fun trackAdServed(info: AdRevenueInfo) {
        mixpanelTracker.trackEvent(MixpanelPropertyAdServed, mutableMapOf<String, Any>().apply {
            put(MixpanelPropertyAdGroupPriority, info.adGroupPriority)
            put(MixpanelPropertyAdUnitFormat, info.adUnitFormat)
            put(MixpanelPropertyCountry, info.country)
            put(MixpanelPropertyPublisherRevenue, info.publisherRevenue)
            put(MixpanelPropertyPrecision, info.precision)
            put(MixpanelPropertyId, info.impressionId)
            put(MixpanelPropertyAdGroupId, info.adGroupId)
            put(MixpanelPropertyAdUnitId, info.adUnitId)
            put(MixpanelPropertyAdGroupType, info.adGroupType)
            put(MixpanelPropertyCurrency, info.currency)
            put(MixpanelPropertyAdUnitName, info.adUnitName)
            put(MixpanelPropertyAdGroupName, info.adGroupName)
        })
    }

    override fun trackBillingIssue() {
        mixpanelTracker.trackEvent(MixpanelEventBillingIssue, mapOf())
    }

    override fun trackBellNotification(bellType: String) {
        mixpanelTracker.trackEvent(MixpanelEventBellNotification, mapOf(
            MixpanelPropertyBellType to bellType
        ))
    }

    override fun trackScreenshot(
        screenshotType: String,
        screenshotUser: String,
        artist: Artist?,
        music: Music?,
        source: MixpanelSource,
        button: String
    ) {
        val map = mutableMapOf<String, Any>().apply {
            artist?.let {
                put(MixpanelPropertyContentType, MixpanelContentTypeAccount)
                put(MixpanelPropertyAccountName, it.name)
                put(MixpanelPropertyAccountId, it.id)
            }
            music?.let {
                when (it.type) {
                    MusicType.Playlist -> {
                        put(MixpanelPropertyContentType, MixpanelContentTypePlaylist)
                        put(MixpanelPropertyPlaylistId, music.id)
                        put(MixpanelPropertyPlaylistName, music.title)
                    }
                    MusicType.Album -> {
                        put(MixpanelPropertyContentType, MixpanelContentTypeAlbum)
                        put(MixpanelPropertyAlbumName, music.title.toLowerCase(Locale.US))
                        put(MixpanelPropertyAlbumId, music.id)
                    }
                    else -> {
                        put(MixpanelPropertyContentType, MixpanelContentTypeSong)
                        put(MixpanelPropertySongName, music.title.toLowerCase(Locale.US))
                        put(MixpanelPropertySongId, music.id)
                    }
                }
                put(MixpanelPropertyArtistName, music.artist.toLowerCase(Locale.US))
                put(MixpanelPropertyGenre, music.genre)
            }
            put(MixpanelPropertyScreenshotType, screenshotType)
            put(MixpanelPropertyScreenshotUser, screenshotUser)
            put(MixpanelPropertySourcePage, source.page)
            put(MixpanelPropertyButton, button)
            put(MixpanelPropertySourceTab, source.tab)
            source.extraParams?.let {
                putAll(it)
            }
        }
        mixpanelTracker.trackEvent(MixpanelEventScreenshot, map)
    }

    override fun trackSleepTimer(source: SleepTimerSource) {
        mixpanelTracker.trackEvent(MixpanelEventSleepTimer, mapOf(
            MixpanelPropertySource to when (source) {
                SleepTimerSource.Prompt -> MixpanelSourcePrompt
                SleepTimerSource.Settings -> MixpanelSourceSettings
            }
        ))
    }

    override fun trackTrendingBannerClick(url: String) {
        mixpanelTracker.trackEvent(MixpanelEventTrendingMessageBar, mapOf(MixpanelPropertyURL to url))
    }

    override fun trackViewArticle(article: WorldArticle) {
        val eventProperties = mutableMapOf<String, Any>().apply {
            put(MixpanelPropertyArticleName, article.title ?: "")
            put(MixpanelPropertyArticleSlug, article.slug ?: "")
            article.publishedDate()?.let { put(MixpanelPropertyArticleDate, it) }
        }
        mixpanelTracker.trackEvent(MixpanelEventViewArticle, eventProperties)
    }

    override fun trackResetPassword(email: String) {
        mixpanelTracker.trackEvent(MixpanelEventResetPassword, mapOf(MixpanelPropertyEmail to email))
    }

    override fun trackChangePassword() {
        mixpanelTracker.trackEvent(MixpanelEventChangePassword, mapOf())
    }

    override fun trackRestoreDownloads(kind: RestoreDownloadsMode, count: Int) {
        mixpanelTracker.trackEvent(MixpanelEventRestoreDownloads, mapOf(
            MixpanelPropertyRestoreDownloadsCount to count,
            MixpanelPropertyButton to if (kind == RestoreDownloadsMode.All) MixpanelRestoreDownloadsButtonAll else MixpanelRestoreDownloadsButtonManually
        ))
    }

    override fun trackFollowPushPermissionPrompt(granted: Boolean) {
        mixpanelTracker.trackEvent(MixpanelEventFollowPermissionPrompt, mapOf(
            MixpanelPropertyAnswer to (if (granted) MixpanelAnswerGrant else MixpanelAnswerDecline)))
    }

    override fun trackPremiumDownloadNotification(type: PremiumDownloadType) {
        mixpanelTracker.trackEvent(MixpanelEventPremiumDownloadNotification, mapOf(
            MixpanelPropertyDownloadMessageType to when (type) {
                PremiumDownloadType.Limited -> MixpanelDownloadMessageTypeLimited
                PremiumDownloadType.PremiumOnly -> MixpanelDownloadMessageTypePremiumOnly
            }
        ))
    }

    override fun trackLocalFileOpened(songName: String, artistName: String) {
        mixpanelTracker.trackEvent(
            MixpanelEventLocalFileOpened,
            mapOf(
                MixpanelPropertySongName to songName,
                MixpanelPropertyArtistName to artistName
            )
        )
    }
}
