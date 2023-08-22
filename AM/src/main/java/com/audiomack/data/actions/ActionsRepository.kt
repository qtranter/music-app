package com.audiomack.data.actions

import android.database.SQLException
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.api.ArtistsDataSource
import com.audiomack.data.api.ArtistsRepository
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.api.NotificationSettingsDataSource
import com.audiomack.data.api.NotificationSettingsRepository
import com.audiomack.data.api.NotificationsEnabledResult
import com.audiomack.data.inapprating.InAppRating
import com.audiomack.data.inapprating.InAppRatingManager
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadRepository
import com.audiomack.data.reachability.Reachability
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.appsflyer.AppsFlyerDataSource
import com.audiomack.data.tracking.appsflyer.AppsFlyerRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.data.widget.WidgetDataSource
import com.audiomack.data.widget.WidgetRepository
import com.audiomack.download.AMMusicDownloader
import com.audiomack.download.AmDownloadAnalytics
import com.audiomack.download.DownloadJobData
import com.audiomack.download.DownloadOrigin
import com.audiomack.download.MusicDownloader
import com.audiomack.download.TrackParentCollection
import com.audiomack.model.AMArtist
import com.audiomack.model.AMPlaylistTracks
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventDownload
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.model.EventFavoriteDelete
import com.audiomack.model.EventFavoriteStatusChanged
import com.audiomack.model.EventFollowChange
import com.audiomack.model.EventUploadDeleted
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.PremiumDownloadMusicModel
import com.audiomack.model.PremiumDownloadStatsModel
import com.audiomack.model.PremiumLimitedDownloadAlertViewType
import com.audiomack.model.PremiumOnlyDownloadAlertViewType
import com.audiomack.network.API
import com.audiomack.network.APIInterface
import io.reactivex.Observable
import java.util.Date
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

class ActionsRepository(
    private val reachabilityDataSource: ReachabilityDataSource = Reachability.getInstance(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val artistsDataSource: ArtistsDataSource = ArtistsRepository(),
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val widgetDataSource: WidgetDataSource = WidgetRepository(),
    private val inAppRating: InAppRating = InAppRatingManager.getInstance(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val appsFlyerDataSource: AppsFlyerDataSource = AppsFlyerRepository(),
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    private val musicDownloader: MusicDownloader = AMMusicDownloader.getInstance(),
    private val apiDownloads: APIInterface.DownloadsInterface = API.getInstance(),
    private val premiumDownloadDataSource: PremiumDownloadDataSource = PremiumDownloadRepository.getInstance(),
    private val notificationSettings: NotificationSettingsDataSource = NotificationSettingsRepository(),
    private val adsManager: AdsDataSource = AdProvidersHelper,
    private val eventBus: EventBus = EventBus.getDefault()
) : ActionsDataSource {

    override fun toggleFavorite(
        music: AMResultItem,
        mixpanelButton: String,
        mixpanelSource: MixpanelSource
    ): Observable<ToggleFavoriteResult> {
        return Observable.create { emitter ->
            if (!reachabilityDataSource.networkAvailable) {
                emitter.tryOnError(ToggleFavoriteException.Offline)
                return@create
            }
            if (!userDataSource.isLoggedIn()) {
                emitter.tryOnError(ToggleFavoriteException.LoggedOut)
                return@create
            }
            widgetDataSource.updateFavoriteStatus(!music.isFavorited)
            if (userDataSource.isMusicFavorited(music)) {
                userDataSource.removeMusicFromFavorites(music)
                music.favoriteStatus = AMResultItem.ItemAPIStatus.Off
                val unfavorited = musicDataSource.unfavorite(music).blockingFirst()
                if (unfavorited) {
                    music.favoriteStatus = AMResultItem.ItemAPIStatus.Off
                    eventBus.post(EventFavoriteDelete(music))
                    emitter.onNext(
                        ToggleFavoriteResult.Notify(
                            isSuccessful = true,
                            wantedToFavorite = false,
                            music.isPlaylist,
                            music.isAlbum,
                            !music.isPlaylist && !music.isAlbum,
                            music.title ?: "",
                            music.artist ?: ""
                        )
                    )
                } else {
                    userDataSource.addMusicToFavorites(music)
                    music.favoriteStatus = AMResultItem.ItemAPIStatus.On
                    eventBus.post(EventFavoriteStatusChanged(music.itemId))
                    emitter.onNext(
                        ToggleFavoriteResult.Notify(
                            isSuccessful = false,
                            wantedToFavorite = false,
                            music.isPlaylist,
                            music.isAlbum,
                            !music.isPlaylist && !music.isAlbum,
                            music.title ?: "",
                            music.artist ?: ""
                        )
                    )
                    widgetDataSource.updateFavoriteStatus(music.isFavorited)
                }
            } else {
                userDataSource.addMusicToFavorites(music)
                music.favoriteStatus = AMResultItem.ItemAPIStatus.On
                val favorited = musicDataSource.favorite(music, mixpanelSource).blockingFirst()
                if (favorited) {
                    music.favoriteStatus = AMResultItem.ItemAPIStatus.On
                    if (music.isPlaylist) {
                        trackingDataSource.trackGA(
                            "playlist",
                            "favorite",
                            music.artist + "/" + music.title
                        )
                    }
                    mixpanelDataSource.trackAddToFavorites(music, mixpanelSource, mixpanelButton)
                    appsFlyerDataSource.trackAddToFavorites()
                    emitter.onNext(
                        ToggleFavoriteResult.Notify(
                            isSuccessful = true,
                            wantedToFavorite = true,
                            music.isPlaylist,
                            music.isAlbum,
                            !music.isPlaylist && !music.isAlbum,
                            music.title ?: "",
                            music.artist ?: ""
                        )
                    )
                } else {
                    userDataSource.removeMusicFromFavorites(music)
                    music.favoriteStatus = AMResultItem.ItemAPIStatus.Off
                    eventBus.post(EventFavoriteStatusChanged(music.itemId))
                    widgetDataSource.updateFavoriteStatus(music.isFavorited)
                    emitter.onNext(
                        ToggleFavoriteResult.Notify(
                            isSuccessful = false,
                            wantedToFavorite = true,
                            music.isPlaylist,
                            music.isAlbum,
                            !music.isPlaylist && !music.isAlbum,
                            music.title ?: "",
                            music.artist ?: ""
                        )
                    )
                }
                inAppRating.incrementFavoriteCount()
                inAppRating.request()
            }
            eventBus.post(EventFavoriteStatusChanged(music.itemId))
            emitter.onComplete()
        }
    }

    override fun toggleRepost(
        music: AMResultItem,
        mixpanelButton: String,
        mixpanelSource: MixpanelSource
    ): Observable<ToggleRepostResult> {
        return Observable.create { emitter ->
            if (!reachabilityDataSource.networkAvailable) {
                emitter.tryOnError(ToggleRepostException.Offline)
                return@create
            }
            if (!userDataSource.isLoggedIn()) {
                emitter.tryOnError(ToggleRepostException.LoggedOut)
                return@create
            }
            widgetDataSource.updateRepostStatus(!music.isReposted)
            if (!music.isReposted) {
                inAppRating.request()
                val reposted = musicDataSource.repost(music, mixpanelSource).blockingFirst()
                mixpanelDataSource.trackReUp(music, mixpanelSource, mixpanelButton)
                if (reposted) {
                    emitter.onNext(
                        ToggleRepostResult.Notify(
                            true,
                            music.isAlbum,
                            music.title ?: "",
                            music.artist ?: ""
                        )
                    )
                } else {
                    emitter.onNext(
                        ToggleRepostResult.Notify(
                            false,
                            music.isAlbum,
                            music.title ?: "",
                            music.artist ?: ""
                        )
                    )
                    widgetDataSource.updateRepostStatus(music.isReposted)
                }
            } else {
                val unreposted = musicDataSource.unrepost(music).blockingFirst()
                if (!unreposted) {
                    widgetDataSource.updateRepostStatus(music.isReposted)
                    eventBus.post(EventUploadDeleted(music))
                }
            }
            emitter.onComplete()
        }
    }

    override fun toggleFollow(
        music: AMResultItem?,
        artist: AMArtist?,
        mixpanelButton: String,
        mixpanelSource: MixpanelSource
    ): Observable<ToggleFollowResult> {
        val uploaderId = artist?.artistId ?: music?.uploaderId ?: ""
        val uploaderSlug = artist?.urlSlug ?: music?.uploaderSlug ?: ""
        val uploaderName = artist?.name ?: music?.uploaderName ?: music?.artist ?: "-"
        val uploaderImage = artist?.smallImage ?: music?.uploaderTinyImage ?: ""
        val goingToFollow = !userDataSource.isArtistFollowed(uploaderId)

        return Observable.create { emitter ->
            require(music != null || artist != null) { "music and artist are both null" }
            if (!reachabilityDataSource.networkAvailable) {
                emitter.tryOnError(ToggleFollowException.Offline)
                return@create
            }
            if (!userDataSource.isLoggedIn()) {
                emitter.tryOnError(ToggleFollowException.LoggedOut)
                return@create
            }
            if (goingToFollow) {
                emitter.onNext(ToggleFollowResult.Finished(true))
                emitter.onNext(ToggleFollowResult.Notify(true, uploaderName, uploaderSlug, uploaderImage))
                val followed = artistsDataSource.follow(uploaderSlug).blockingFirst()
                if (followed) {
                    userDataSource.addArtistToFollowing(uploaderId)
                    mixpanelDataSource.trackFollowAccount(
                        uploaderName,
                        uploaderId,
                        mixpanelSource,
                        mixpanelButton
                    )
                }
                eventBus.post(EventFollowChange(uploaderId))
                emitter.onNext(ToggleFollowResult.Finished(followed))
                try {
                    when (notificationSettings.areNotificationsEnabledForNewMusic().blockingGet()) {
                        is NotificationsEnabledResult.DisabledAtOSLevel -> emitter.onNext(ToggleFollowResult.AskForPermission(PermissionRedirect.Settings))
                        is NotificationsEnabledResult.DisabledAtAppLevel -> emitter.onNext(ToggleFollowResult.AskForPermission(PermissionRedirect.NotificationsManager))
                        else -> { /* no-op */ }
                    }
                } catch (e: Exception) {
                    Timber.w(e)
                }
            } else {
                emitter.onNext(ToggleFollowResult.Finished(false))
                emitter.onNext(ToggleFollowResult.Notify(false, uploaderName, uploaderSlug, uploaderImage))
                val followed = artistsDataSource.unfollow(uploaderSlug).blockingFirst()
                if (!followed) {
                    userDataSource.removeArtistFromFollowing(uploaderId)
                    mixpanelDataSource.trackUnfollowAccount(
                        uploaderName,
                        uploaderId,
                        mixpanelSource,
                        mixpanelButton
                    )
                }
                eventBus.post(EventFollowChange(uploaderId))
                emitter.onNext(ToggleFollowResult.Finished(followed))
            }
            emitter.onComplete()
        }
    }

    override fun addToPlaylist(
        music: AMResultItem
    ): Observable<Boolean> {
        return Observable.create { emitter ->
            if (!userDataSource.isLoggedIn()) {
                emitter.tryOnError(AddToPlaylistException.LoggedOut)
                return@create
            }
            emitter.onNext(true)
            emitter.onComplete()
        }
    }

    override fun toggleHighlight(
        music: AMResultItem,
        mixpanelButton: String,
        mixpanelSource: MixpanelSource
    ): Observable<ToggleHighlightResult> {
        return Observable.create { emitter ->
            if (!reachabilityDataSource.networkAvailable) {
                emitter.tryOnError(ToggleHighlightException.Offline)
                return@create
            }
            if (!userDataSource.isLoggedIn()) {
                emitter.tryOnError(ToggleHighlightException.LoggedOut)
                return@create
            }
            val highlighted = userDataSource.isMusicHighlighted(music)
            if (highlighted) {
                val removed = musicDataSource.removeFromHighlights(music).blockingFirst()
                if (removed) {
                    userDataSource.removeFromHighlights(music)
                    emitter.onNext(ToggleHighlightResult.Removed)
                    emitter.onComplete()
                } else {
                    emitter.onError(ToggleHighlightException.Failure(false))
                }
            } else {
                if (userDataSource.highlightsCount == 4) {
                    emitter.onError(ToggleHighlightException.ReachedLimit)
                } else {
                    val added =
                        musicDataSource.addToHighlights(music, mixpanelSource).blockingFirst()
                    if (added) {
                        userDataSource.addToHighlights(music)
                        mixpanelDataSource.trackHighlight(music, mixpanelSource, mixpanelButton)
                        emitter.onNext(ToggleHighlightResult.Added(music.title ?: ""))
                        emitter.onComplete()
                    } else {
                        emitter.onError(ToggleHighlightException.Failure(true))
                    }
                }
            }
        }
    }

    override fun toggleDownload(
        music: AMResultItem,
        mixpanelButton: String,
        mixpanelSource: MixpanelSource,
        retry: Boolean,
        skipFrozenCheck: Boolean,
        parentAlbum: AMResultItem?
    ): Observable<ToggleDownloadResult> {
        return Observable.create { emitter ->
            if (!userDataSource.isLoggedIn()) {
                emitter.tryOnError(ToggleDownloadException.LoggedOut(if (music.isPlaylist) LoginSignupSource.OfflinePlaylist else LoginSignupSource.Download))
                return@create
            }
            when {
                music.isSong || music.isAlbumTrack || music.isPlaylistTrack -> {
                    if (!retry && music.isDownloadCompletedIndependentlyFromType) {
                        if (!skipFrozenCheck && music.downloadType == AMResultItem.MusicDownloadType.Premium && !premiumDataSource.isPremium) {
                            emitter.tryOnError(
                                ToggleDownloadException.ShowPremiumDownload(
                                    PremiumDownloadModel(
                                        music = PremiumDownloadMusicModel(music),
                                        alertTypePremium = PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline
                                    )
                                )
                            )
                            emitter.onComplete()
                            return@create
                        }
                        if (!skipFrozenCheck && music.downloadType == AMResultItem.MusicDownloadType.Limited && music.isDownloadFrozen) {
                            if (premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount + 1 <= premiumDownloadDataSource.premiumDownloadLimit) {
                                val exception =
                                    musicDataSource.markFrozenDownloads(false, listOf(music.itemId))
                                        .blockingGet()
                                if (exception == null) {
                                    eventBus.post(EventDownload(music.itemId, true))
                                    eventBus.post(EventDownloadsEdited())
                                    emitter.onNext(ToggleDownloadResult.ShowUnlockedToast(music.title ?: ""))
                                    emitter.onComplete()
                                    return@create
                                }
                            }
                            emitter.tryOnError(
                                ToggleDownloadException.ShowPremiumDownload(
                                    PremiumDownloadModel(
                                        music = PremiumDownloadMusicModel(music, premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(music)),
                                        stats = PremiumDownloadStatsModel(mixpanelButton, mixpanelSource, premiumDownloadDataSource.premiumDownloadLimit, premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount),
                                        alertTypeLimited = PremiumLimitedDownloadAlertViewType.DownloadFrozen
                                    )
                                )
                            )
                            emitter.onComplete()
                            return@create
                        }
                        emitter.onNext(ToggleDownloadResult.ConfirmMusicDeletion)
                        emitter.onComplete()
                        return@create
                    }
                    if (music.downloadType == AMResultItem.MusicDownloadType.Premium && !premiumDataSource.isPremium) {
                        emitter.tryOnError(
                            ToggleDownloadException.ShowPremiumDownload(
                                PremiumDownloadModel(
                                    alertTypePremium = PremiumOnlyDownloadAlertViewType.Download
                                )
                            )
                        )
                        emitter.onComplete()
                        return@create
                    }
                    if (!retry && !premiumDownloadDataSource.canDownloadMusicBasedOnPremiumLimitedCount(music)) {
                        emitter.tryOnError(
                            ToggleDownloadException.ShowPremiumDownload(
                                PremiumDownloadModel(
                                    music = PremiumDownloadMusicModel(music, premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(music)),
                                    stats = PremiumDownloadStatsModel(mixpanelButton, mixpanelSource, premiumDownloadDataSource.premiumDownloadLimit, premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount),
                                    alertTypeLimited = PremiumLimitedDownloadAlertViewType.ReachedLimit
                                )
                            )
                        )
                        emitter.onComplete()
                        return@create
                    }
                    mixpanelDataSource.trackDownloadToOffline(music, mixpanelSource, mixpanelButton)
                    music.mixpanelSource = mixpanelSource
                    musicDownloader.enqueueDownload(
                        DownloadJobData(
                            currentTrack = music,
                            downloadAnalytics = AmDownloadAnalytics(
                                DownloadOrigin.BASE_SINGLE,
                                trackingDataSource
                            ),
                            parentCollection = parentAlbum?.let { TrackParentCollection.Album(it.itemId, 1, item = it) }
                        )
                    )
                    emitter.onNext(ToggleDownloadResult.DownloadStarted)
                }
                music.isAlbum -> {
                    if (music.tracks == null) {
                        music.loadTracks()
                    }
                    if (music.tracks == null) {
                        emitter.onComplete()
                        return@create
                    }
                    if (music.isDownloadCompleted) {
                        if (!skipFrozenCheck && music.downloadType == AMResultItem.MusicDownloadType.Premium && !premiumDataSource.isPremium) {
                            emitter.tryOnError(
                                ToggleDownloadException.ShowPremiumDownload(
                                    PremiumDownloadModel(
                                        music = PremiumDownloadMusicModel(music, premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(music)),
                                        alertTypePremium = PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline
                                    )
                                )
                            )
                            emitter.onComplete()
                            return@create
                        }
                        if (!skipFrozenCheck && music.downloadType == AMResultItem.MusicDownloadType.Limited && premiumDownloadDataSource.getFrozenCount(music) > 0) {
                            if ((music.tracks?.size ?: 0) > premiumDownloadDataSource.premiumDownloadLimit && !premiumDataSource.isPremium) {
                                emitter.tryOnError(
                                    ToggleDownloadException.ShowPremiumDownload(
                                        PremiumDownloadModel(
                                            music = PremiumDownloadMusicModel(music),
                                            alertTypeLimited = PremiumLimitedDownloadAlertViewType.DownloadAlbumLargerThanLimitAlreadyDownloaded
                                        )
                                    )
                                )
                                emitter.onComplete()
                                return@create
                            }
                            if (premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount + premiumDownloadDataSource.getFrozenCount(music) <= premiumDownloadDataSource.premiumDownloadLimit) {
                                val exception = musicDataSource.markFrozenDownloads(
                                    false,
                                    music.tracks!!.mapNotNull { it.itemId }).blockingGet()
                                if (exception == null) {
                                    eventBus.post(EventDownload(music.itemId, true))
                                    emitter.onNext(ToggleDownloadResult.ShowUnlockedToast(music.title ?: ""))
                                    emitter.onComplete()
                                    return@create
                                }
                            }
                            emitter.tryOnError(
                                ToggleDownloadException.ShowPremiumDownload(
                                    PremiumDownloadModel(
                                        music = PremiumDownloadMusicModel(music, premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(music)),
                                        stats = PremiumDownloadStatsModel(mixpanelButton, mixpanelSource, premiumDownloadDataSource.premiumDownloadLimit, premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount),
                                        alertTypeLimited = PremiumLimitedDownloadAlertViewType.DownloadFrozen
                                    )
                                )
                            )
                            emitter.onComplete()
                            return@create
                        }
                        emitter.onNext(ToggleDownloadResult.ConfirmMusicDeletion)
                        emitter.onComplete()
                        return@create
                    }
                    if (music.downloadType == AMResultItem.MusicDownloadType.Premium && !premiumDataSource.isPremium) {
                        emitter.tryOnError(
                            ToggleDownloadException.ShowPremiumDownload(
                                PremiumDownloadModel(
                                    alertTypePremium = PremiumOnlyDownloadAlertViewType.Download
                                )
                            )
                        )
                        emitter.onComplete()
                        return@create
                    }
                    if (music.downloadType == AMResultItem.MusicDownloadType.Limited && (music.tracks?.size
                            ?: 0) > premiumDownloadDataSource.premiumDownloadLimit && !premiumDataSource.isPremium
                    ) {
                        emitter.tryOnError(
                            ToggleDownloadException.ShowPremiumDownload(
                                PremiumDownloadModel(
                                    music = PremiumDownloadMusicModel(music),
                                    alertTypeLimited = if (music.isDownloaded) PremiumLimitedDownloadAlertViewType.DownloadAlbumLargerThanLimitAlreadyDownloaded else PremiumLimitedDownloadAlertViewType.DownloadAlbumLargerThanLimit
                                )
                            )
                        )
                        emitter.onComplete()
                        return@create
                    }
                    if (!premiumDownloadDataSource.canDownloadMusicBasedOnPremiumLimitedCount(music)) {
                        emitter.tryOnError(
                            ToggleDownloadException.ShowPremiumDownload(
                                PremiumDownloadModel(
                                    music = PremiumDownloadMusicModel(music, premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(music)),
                                    stats = PremiumDownloadStatsModel(mixpanelButton, mixpanelSource, premiumDownloadDataSource.premiumDownloadLimit, premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount),
                                    alertTypeLimited = PremiumLimitedDownloadAlertViewType.ReachedLimit
                                )
                            )
                        )
                        return@create
                    }
                    musicDownloader.cacheImages(music)
                    music.isDownloadCompleted = true
                    music.setDownloadDate(Date())
                    try {
                        music.save()
                    } catch (e: SQLException) {
                        Timber.w(e)
                    }
                    apiDownloads.addDownload(music.itemId, mixpanelSource.page)
                    mixpanelDataSource.trackDownloadToOffline(music, mixpanelSource, mixpanelButton)
                    val tracksCount = music.tracks!!.filter { !it.isDownloadCompleted }.size
                    music.tracks!!.forEachIndexed { index, track ->
                        if (!track.isGeoRestricted) {
                            track.mixpanelSource = mixpanelSource
                            musicDownloader.enqueueDownload(
                                DownloadJobData(
                                    currentTrack = track,
                                    album = music,
                                    downloadAnalytics = AmDownloadAnalytics(
                                        DownloadOrigin.ALBUM,
                                        trackingDataSource
                                    ),
                                    parentCollection = TrackParentCollection.Album(
                                        music.itemId,
                                        tracksCount,
                                        index == 0,
                                        music
                                    )
                                )
                            )
                        }
                    }
                    trackingDataSource.trackGA(
                        "Download Album",
                        "Album",
                        "${music.artist} - ${music.title}"
                    )
                    emitter.onNext(ToggleDownloadResult.DownloadStarted)
                    if (tracksCount == 0) {
                        // All tracks were already downloaded: bundle tracks and notify its completion
                        val result = musicDataSource.bundleAlbumTracks(music.itemId).blockingGet()
                        eventBus.post(EventDownload(music.itemId, true))
                    }
                }
                music.isPlaylist -> {
                    if (!premiumDataSource.isPremium) {
                        emitter.tryOnError(ToggleDownloadException.Unsubscribed(InAppPurchaseMode.PlaylistDownload))
                        return@create
                    }
                    if (music.tracks == null) {
                        if (music.isDownloaded) {
                            emitter.onNext(ToggleDownloadResult.ConfirmPlaylistDeletion)
                            emitter.onComplete()
                            return@create
                        } else {
                            emitter.onNext(ToggleDownloadResult.StartedBlockingAPICall)
                            lateinit var remotePlaylist: AMResultItem
                            try {
                                remotePlaylist =
                                    musicDataSource.getPlaylistInfo(music.itemId).blockingFirst()
                                emitter.onNext(ToggleDownloadResult.EndedBlockingAPICall)
                            } catch (e: Exception) {
                                emitter.onNext(ToggleDownloadResult.EndedBlockingAPICall)
                                emitter.tryOnError(ToggleDownloadException.FailedDownloadingPlaylist)
                            }
                            music.setTracksAndRemoveRestricted(remotePlaylist.tracks)
                            emitter.onNext(
                                ToggleDownloadResult.ConfirmPlaylistDownload(
                                    music.tracks?.size ?: 0
                                )
                            )
                            emitter.onComplete()
                        }
                    } else {
                        if (!premiumDownloadDataSource.canDownloadMusicBasedOnPremiumLimitedCount(
                                music
                            )
                        ) {
                            emitter.tryOnError(
                                ToggleDownloadException.ShowPremiumDownload(
                                    PremiumDownloadModel(
                                        music = PremiumDownloadMusicModel(music, premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(music)),
                                        stats = PremiumDownloadStatsModel(mixpanelButton, mixpanelSource, premiumDownloadDataSource.premiumDownloadLimit, premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount),
                                        alertTypeLimited = PremiumLimitedDownloadAlertViewType.ReachedLimit
                                    )
                                )
                            )
                            emitter.onComplete()
                            return@create
                        }
                        mixpanelDataSource.trackDownloadToOffline(
                            music,
                            mixpanelSource,
                            mixpanelButton
                        )
                        musicDownloader.cacheImages(music)
                        music.isDownloadCompleted = true
                        music.setDownloadDate(Date())
                        music.save()
                        try {
                            val tracksCount = music.tracks!!.filter { !it.isDownloadCompleted }.size
                            AMPlaylistTracks.deletePlaylist(music.itemId)
                            music.tracks!!.forEachIndexed { index, track ->
                                if (!track.isDownloadCompleted) {
                                    track.mixpanelSource = mixpanelSource
                                    musicDownloader.enqueueDownload(
                                        DownloadJobData(
                                            currentTrack = track,
                                            album = music,
                                            downloadAnalytics = AmDownloadAnalytics(
                                                DownloadOrigin.BASE_PLAYLIST,
                                                trackingDataSource
                                            ),
                                            parentCollection = TrackParentCollection.Playlist(
                                                music.itemId,
                                                tracksCount,
                                                index == 0,
                                                music
                                            )
                                        )
                                    )
                                    AMPlaylistTracks(music.itemId, track.itemId, index).save()
                                } else {
                                    AMPlaylistTracks(music.itemId, track.itemId, index).save()
                                }
                            }
                        } catch (e: Exception) {
                            trackingDataSource.trackException(e)
                        }
                        trackingDataSource.trackGA(
                            "Download Playlist",
                            "Playlist",
                            "${music.artist} - ${music.title}"
                        )
                        emitter.onNext(ToggleDownloadResult.DownloadStarted)
                    }
                }
            }
            inAppRating.incrementDownloadCount()
            inAppRating.request()
            adsManager.showInterstitial()
            emitter.onComplete()
        }
    }
}
