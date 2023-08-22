package com.audiomack.ui.playlist.details

import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.common.MusicDownloadActionStateHelper
import com.audiomack.common.MusicDownloadActionStateHelperImpl
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.music.MusicManager
import com.audiomack.data.music.MusicManagerImpl
import com.audiomack.data.playlist.PlayListDataSource
import com.audiomack.data.playlist.PlaylistRepository
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadRepository
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.reachability.Reachability
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.data.tracking.mixpanel.MixpanelButtonPlaylistDetails
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMPlaylistTracks
import com.audiomack.model.AMResultItem
import com.audiomack.model.ArtistWithBadge
import com.audiomack.model.EventDownload
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.model.EventLoginState
import com.audiomack.model.EventPlaylistDeleted
import com.audiomack.model.EventPlaylistEdited
import com.audiomack.model.EventTrackRemoved
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.playback.ActionState
import com.audiomack.playback.Playback
import com.audiomack.playback.PlaybackItem
import com.audiomack.playback.PlaybackState
import com.audiomack.playback.PlayerPlayback
import com.audiomack.playback.SongAction.Download
import com.audiomack.playback.SongAction.Edit
import com.audiomack.playback.SongAction.Favorite
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.usecases.RefreshCommentCountUseCase
import com.audiomack.usecases.RefreshCommentCountUseCaseImpl
import com.audiomack.utils.AMClickableSpan
import com.audiomack.utils.GeneralPreferences
import com.audiomack.utils.GeneralPreferencesImpl
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.spannableString
import com.audiomack.views.AMRecyclerView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import kotlin.math.max
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class PlaylistViewModel(
    private var playlist: AMResultItem,
    private val online: Boolean,
    private val deleted: Boolean,
    private val mixpanelSource: MixpanelSource,
    private val openShare: Boolean,
    val imageLoader: ImageLoader = PicassoImageLoader,
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val generalPreferences: GeneralPreferences = GeneralPreferencesImpl(),
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val musicManager: MusicManager = MusicManagerImpl(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val queueDataSource: QueueDataSource = QueueRepository.getInstance(),
    private val playerPlayback: Playback = PlayerPlayback.getInstance(),
    private val eventBus: EventBus = EventBus.getDefault(),
    refreshCommentCountUseCase: RefreshCommentCountUseCase = RefreshCommentCountUseCaseImpl(),
    private val playListDataSource: PlayListDataSource = PlaylistRepository(),
    private val premiumDownloadDataSource: PremiumDownloadDataSource = PremiumDownloadRepository.getInstance(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val reachabilityDataSource: ReachabilityDataSource = Reachability.getInstance(),
    private val musicDownloadActionStateHelper: MusicDownloadActionStateHelper = MusicDownloadActionStateHelperImpl(premiumDownloadDataSource, premiumDataSource)
) : BaseViewModel(), PlaylistTracksAdapter.Listener, AMRecyclerView.ScrollListener {

    private val _playlist = MutableLiveData(playlist)

    /** Emits the playlist name as a String */
    val title: LiveData<String> = Transformations.map(_playlist) { it.title ?: "" }

    /** Emits infos about the playlist uploader, including the name and verifications status */
    val uploader: LiveData<ArtistWithBadge> = Transformations.map(_playlist) {
        ArtistWithBadge(
            it.uploaderName ?: "-",
            it.isUploaderVerified,
            it.isUploaderTastemaker,
            it.isUploaderAuthenticated
        )
    }

    /** Emits a boolean indicating if the follow button is ON or OFF */
    private val _followStatus = MutableLiveData<Boolean>()
    val followStatus: LiveData<Boolean> get() = _followStatus

    /** Emits a Boolean indicating if the follow button is active or not */
    val followVisible: LiveData<Boolean> = Transformations.map(_playlist) { userDataSource.getUserSlug() != it.uploaderSlug }

    /** Emits the playlist description as a String or SpannableString in case it's longer than a certain threshold */
    val description: LiveData<CharSequence> = Transformations.map(_playlist) {
        val trimmedDesc = it.desc?.replace("\n", " ")?.replace("\r", " ")?.trim()
        when {
            trimmedDesc.isNullOrEmpty() -> {
                null
            }
            trimmedDesc.length <= 75 -> {
                trimmedDesc
            }
            else -> {
                val context = MainApplication.context ?: return@map null
                val moreString = context.getString(R.string.playlist_desc_more) ?: ""
                val moreSpannableString = context.spannableString(
                    fullString = moreString,
                    highlightedStrings = listOf(moreString),
                    highlightedColor = R.color.orange,
                    highlightedFont = R.font.opensans_bold,
                    highlightedSize = 10,
                    clickableSpans = listOf(AMClickableSpan(context) { onInfoTapped() }))
                TextUtils.concat(trimmedDesc.substring(0, 75).trim() + "... ", moreSpannableString)
            }
        }
    }

    /** Emits a Boolean indicating if the description is visible or not */
    val descriptionVisible: LiveData<Boolean> = Transformations.map(_playlist) {
        !playlist.desc?.trim().isNullOrEmpty()
    }

    /** Emits the high-res version of the playlist artwork */
    val highResImage: LiveData<String> = Transformations.map(_playlist) { it.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetOriginal) }

    /** Emits the low-res version of the playlist artwork */
    val lowResImage: LiveData<String> = Transformations.map(_playlist) { it.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall) }

    /** Emits the banner image */
    val banner: LiveData<String> = Transformations.map(_playlist) { it.banner }

    /** Emits a Boolean indicating if the play button is active or not */
    private val _playButtonActive = MutableLiveData<Boolean>()
    val playButtonActive: LiveData<Boolean> get() = _playButtonActive

    /** Emits a Boolean indicating if the favorite button is active or not */
    val favoriteVisible: LiveData<Boolean> = Transformations.map(_playlist) { userDataSource.getUserSlug() != it.uploaderSlug }

    /** Emits a Boolean indicating if the edit button is active or not */
    val editVisible: LiveData<Boolean> = Transformations.map(_playlist) { userDataSource.getUserSlug() == it.uploaderSlug }

    /** Emits a Boolean indicating if the sync button is active or not */
    private var _syncVisible = MutableLiveData<Boolean>()
    val syncVisible: LiveData<Boolean> get() = _syncVisible

    /** Emits the whole playlist, used to show the tracks */
    val setupTracksEvent = SingleLiveEvent<AMResultItem>()
    val closeEvent = SingleLiveEvent<Void>()
    val openMusicInfoEvent = SingleLiveEvent<AMResultItem>()
    val shareEvent = SingleLiveEvent<AMResultItem>()
    val showEditMenuEvent = SingleLiveEvent<Void>()
    val closeOptionsEvent = SingleLiveEvent<Void>()
    val openEditEvent = SingleLiveEvent<AMResultItem>()
    val openReorderEvent = SingleLiveEvent<AMResultItem>()
    val showDeleteConfirmationEvent = SingleLiveEvent<AMResultItem>()
    val deletePlaylistStatusEvent = SingleLiveEvent<DeletePlaylistStatus>()
    val shuffleEvent = SingleLiveEvent<Pair<AMResultItem, AMResultItem>>()
    val openTrackEvent = SingleLiveEvent<Triple<AMResultItem, AMResultItem?, Int>>()
    val openTrackOptionsEvent = SingleLiveEvent<AMResultItem>()
    val openTrackOptionsFailedDownloadEvent = SingleLiveEvent<AMResultItem>()
    val openUploaderEvent = SingleLiveEvent<String>()
    val showPlaylistTakenDownAlertEvent = SingleLiveEvent<Void>()
    val openPlaylistEvent = SingleLiveEvent<AMResultItem>()
    val createPlaylistStatusEvent = SingleLiveEvent<CreatePlaylistStatus>()
    val performSyncEvent = SingleLiveEvent<Void>()
    val scrollEvent = SingleLiveEvent<Void>()
    val showFavoriteTooltipEvent = SingleLiveEvent<Void>()
    val showDownloadTooltipEvent = SingleLiveEvent<Void>()
    val removeTrackEvent = SingleLiveEvent<Int>()
    val notifyFollowToast = SingleLiveEvent<ToggleFollowResult.Notify>()
    val notifyOfflineEvent = SingleLiveEvent<Void>()
    val loginRequiredEvent = SingleLiveEvent<LoginSignupSource>()
    val georestrictedMusicClickedEvent = SingleLiveEvent<AMResultItem>()
    val openCommentsEvent = SingleLiveEvent<AMResultItem>()
    val showHUDEvent = SingleLiveEvent<ProgressHUDMode>()
    val notifyFavoriteEvent = SingleLiveEvent<ToggleFavoriteResult.Notify>()
    val reloadAdapterTracksEvent = SingleLiveEvent<Void>()
    val reloadAdapterTracksRangeEvent = SingleLiveEvent<List<Int>>()
    val reloadAdapterTrackEvent = SingleLiveEvent<Int>()
    /** Emits the playlist that needs to be downloaded or deleted */
    val addDeleteDownloadEvent = SingleLiveEvent<AMResultItem>()
    val premiumRequiredEvent = SingleLiveEvent<InAppPurchaseMode>()
    val showConfirmDownloadDeletionEvent = SingleLiveEvent<AMResultItem>()
    val showConfirmPlaylistDownloadDeletionEvent = SingleLiveEvent<AMResultItem>()
    val showFailedPlaylistDownloadEvent = SingleLiveEvent<Void>()
    val showConfirmPlaylistSyncEvent = SingleLiveEvent<Int>()
    val showPremiumDownloadEvent = SingleLiveEvent<PremiumDownloadModel>()
    val showUnlockedToastEvent = SingleLiveEvent<String>()
    val promptNotificationPermissionEvent = SingleLiveEvent<PermissionRedirect>()

    var recyclerviewConfigured = false

    val tracks: List<AMResultItem>
        get() = playlist.tracks ?: emptyList()

    val adsVisible: Boolean
        get() = adsDataSource.adsVisible

    val isPlaylistFavorited: Boolean
        get() = userDataSource.isMusicFavorited(playlist)

    private val _favoriteAction = MutableLiveData<Favorite>()
    val favoriteAction: LiveData<Favorite> get() = _favoriteAction

    private val _downloadAction = MutableLiveData<Download>()
    val downloadAction: LiveData<Download> get() = _downloadAction

    val editAction = SingleLiveEvent<Edit>()

    private val _commentsCount = MutableLiveData<Int>()
    val commentsCount: LiveData<Int> get() = _commentsCount

    private var pendingActionAfterLogin: PendingActionAfterLogin? = null

    private val reloadAdapterTracksBuffer = mutableListOf<Int>()
    private val reloadAdapterTracksBufferSubject = BehaviorSubject.create<Boolean>()

    @VisibleForTesting
    val premiumObserver = object : PlaylistObserver<Boolean>() {
        override fun onNext(premium: Boolean) {
            _downloadAction.postValue(Download(musicDownloadActionStateHelper.downloadState(playlist)))
        }
    }

    @VisibleForTesting
    val playbackStateObserver = object : PlaylistObserver<PlaybackState>() {
        override fun onNext(state: PlaybackState) {
            _playButtonActive.postValue((state == PlaybackState.PLAYING || state == PlaybackState.LOADING) && queueDataSource.isCurrentItemOrParent(playlist))
        }
    }

    @VisibleForTesting
    val playbackItemObserver = object : PlaylistObserver<PlaybackItem>() {
        override fun onNext(t: PlaybackItem) {
            reloadAdapterTracksEvent.call()
        }
    }

    @VisibleForTesting
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventDownload: EventDownload) {
        val itemId = eventDownload.itemId ?: return
        if (itemId == playlist.itemId) {
            musicDataSource.getOfflineResource(playlist.itemId)
                .flatMap { resource ->
                    resource.data?.let { Observable.just(it) }
                        ?: Observable.error(RuntimeException())
                }
                .map { musicDownloadActionStateHelper.downloadState(it) }
                .observeOn(schedulersProvider.main)
                .subscribe({
                    _downloadAction.value = Download(it)
                }, {
                    _downloadAction.value = Download(ActionState.DEFAULT)
                })
                .also { compositeDisposable.add(it) }
        } else {
            playlist.tracks?.indexOfFirst { it.itemId == eventDownload.itemId }?.takeIf { it != -1 }?.let {
                reloadAdapterTracksBuffer.add(it)
                reloadAdapterTracksBufferSubject.onNext(true)
            }
        }
    }

    @VisibleForTesting
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventPlaylistDeleted: EventPlaylistDeleted) {
        if (playlist.itemId == eventPlaylistDeleted.item.itemId) {
            closeEvent.call()
        }
    }

    @VisibleForTesting
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventPlaylistEdited: EventPlaylistEdited) {
        if (playlist.itemId == eventPlaylistEdited.item.itemId) {
            playlist = eventPlaylistEdited.item
            _playlist.postValue(playlist)
            loadFollowStatus()
            setupTracksEvent.postValue(playlist)
        }
    }

    @VisibleForTesting
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventTrackRemoved: EventTrackRemoved) {
        eventTrackRemoved.trackIds.forEach { removedTrackId ->
            playlist.tracks?.let { tracks ->
                tracks.indexOfLast { removedTrackId == it.itemId }
                    .takeIf { it >= 0 }
                    ?.let { index ->
                        playlist.tracks?.removeAt(index)
                        playlist.playlistTracksCount = playlist.tracks?.size ?: 0
                        removeTrackEvent.postValue(index)
                    }
            }
        }
    }

    @VisibleForTesting
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventDownloadsEdited) {
        reloadAdapterTracksEvent.call()
        _downloadAction.postValue(Download(musicDownloadActionStateHelper.downloadState(playlist)))
    }

    init {
        loadFollowStatus()

        setupTracksEvent.postValue(playlist)

        compositeDisposable.add(
            checkSyncAvailability()
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe { syncEnabled ->
                    _syncVisible.postValue(syncEnabled)
                }
        )

        // Actions
        _favoriteAction.postValue(
            Favorite(if (isPlaylistFavorited) ActionState.ACTIVE else ActionState.DEFAULT)
        )
        _downloadAction.postValue(Download(musicDownloadActionStateHelper.downloadState(playlist)))

        editAction.postValue(
            Edit(state =
                if (reachabilityDataSource.networkAvailable)
                    ActionState.DEFAULT
                else
                    ActionState.DISABLED
            )
        )

        // Subscriptions
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        premiumDataSource.premiumObservable.subscribe(premiumObserver)
        subscribeToPlayback()
        setFavoriteListener()
        setCommentCountListener()
        eventBus.register(this)
        compositeDisposable.add(refreshCommentCountUseCase.refresh(playlist).subscribe())
        subscribeToAdapterUpdates()

        if (openShare) {
            shareEvent.postValue(playlist)
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventBus.unregister(this)
    }

    private fun subscribeToPlayback() {
        playerPlayback.apply {
            state.observable
                .distinctUntilChanged()
                .observeOn(schedulersProvider.main)
                .subscribe(playbackStateObserver)
            item
                .distinctUntilChanged()
                .observeOn(schedulersProvider.main)
                .subscribe(playbackItemObserver)
        }
    }

    private fun subscribeToAdapterUpdates() {
        compositeDisposable.add(
            reloadAdapterTracksBufferSubject
                .debounce(250, TimeUnit.MILLISECONDS)
                .subscribe {
                    reloadAdapterTracksRangeEvent.postValue(reloadAdapterTracksBuffer.toList())
                    reloadAdapterTracksBuffer.clear()
                }
        )
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        when (state) {
            EventLoginState.LOGGED_IN -> pendingActionAfterLogin?.let {
                when (it) {
                    is PendingActionAfterLogin.Follow -> onFollowTapped()
                    is PendingActionAfterLogin.Favorite -> {
                        it.track
                            ?.let { onTrackFavoriteTapped(it) }
                            ?: onFavoriteTapped()
                    }
                    is PendingActionAfterLogin.Download -> download(it.music, it.mixpanelButton)
                }
                pendingActionAfterLogin = null
            }
            else -> pendingActionAfterLogin = null
        }
    }

    private fun loadFollowStatus() {
        _followStatus.postValue(userDataSource.isArtistFollowed(playlist.uploaderId))
    }

    fun onBackTapped() {
        closeEvent.call()
    }

    fun onInfoTapped() {
        openMusicInfoEvent.postValue(playlist)
    }

    fun onShareTapped() {
        shareEvent.postValue(playlist)
    }

    fun onEditTapped() {
        showEditMenuEvent.call()
    }

    fun onDownloadTapped() {
        download(playlist, MixpanelButtonPlaylistDetails)
    }

    fun onPlayAllTapped() {
        if (queueDataSource.isCurrentItemOrParent(playlist)) {
            playerPlayback.apply { if (isPlaying) pause() else play() }
        } else {
            tracks.firstOrNull()?.let { onTrackTapped(it) }
        }
    }

    fun onShuffleTapped() {
        val tracks = playlist.tracks ?: return
        val firstTrack = tracks.firstOrNull() ?: return
        shuffleEvent.postValue(Pair(firstTrack, playlist))
    }

    fun onUploaderTapped() {
        playlist.uploaderSlug?.let {
            openUploaderEvent.postValue(it)
        }
    }

    fun onOptionReorderRemoveTracksTapped() {
        closeOptionsEvent.call()
        openReorderEvent.postValue(playlist)
    }

    fun onOptionEditPlaylistTapped() {
        closeOptionsEvent.call()
        openEditEvent.postValue(playlist)
    }

    fun onOptionSharePlaylistTapped() {
        closeOptionsEvent.call()
        shareEvent.postValue(playlist)
    }

    fun onOptionDeletePlaylistTapped() {
        closeOptionsEvent.call()
        showDeleteConfirmationEvent.postValue(playlist)
    }

    fun onConfirmDeletePlaylistTapped() {
        deletePlaylistStatusEvent.postValue(DeletePlaylistStatus.Loading)
        compositeDisposable.add(
            musicDataSource.deletePlaylist(playlist.itemId)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    deletePlaylistStatusEvent.postValue(DeletePlaylistStatus.Success(MainApplication.context?.getString(R.string.playlist_delete_succeeded_template, playlist.title) ?: ""))
                    EventBus.getDefault().post(EventPlaylistDeleted(playlist))
                    playlist.deepDelete()
                }, {
                    deletePlaylistStatusEvent.postValue(DeletePlaylistStatus.Error(MainApplication.context?.getString(R.string.playlist_delete_failed) ?: ""))
                })
        )
    }

    fun onSyncTapped() {
        if (deleted) {
            showPlaylistTakenDownAlertEvent.call()
        } else {
            download(playlist, MixpanelButtonPlaylistDetails)
            performSyncEvent.call()
        }
    }

    fun onCreatePlaylistTapped() {
        val ids = playlist.tracks?.map { it.itemId }?.filter { !it.isNullOrBlank() }?.joinToString(",") ?: ""
        createPlaylistStatusEvent.postValue(CreatePlaylistStatus.Loading)
        compositeDisposable.add(
            musicDataSource.createPlaylist(
                playlist.title ?: "",
                playlist.genre ?: "",
                playlist.desc ?: "",
                false,
                ids,
                null,
                null,
                mixpanelSource.page
            )
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ newPlaylist ->
                    createPlaylistStatusEvent.postValue(CreatePlaylistStatus.Success(MainApplication.context?.getString(R.string.add_to_playlist_success_generic) ?: ""))
                    try {
                        playlist.deepDelete()
                        newPlaylist.save()
                        AMPlaylistTracks.savePlaylist(newPlaylist)
                        closeEvent.call()
                        openPlaylistEvent.postValue(newPlaylist)
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e)
                        closeEvent.call()
                    }
                }, {
                    createPlaylistStatusEvent.postValue(CreatePlaylistStatus.Error(MainApplication.context?.getString(R.string.add_to_playlist_error) ?: ""))
                })
        )
    }

    fun onDeleteTakendownPlaylistTapped() {
        playlist.deepDelete()
        EventBus.getDefault().post(EventDownload(playlist.itemId, false))
        closeEvent.call()
    }

    fun onLayoutReady() {
        // Do not show tooltip if we need to open the menu
        if (!openShare) {
            if ((userDataSource.getUserSlug() != playlist.uploaderSlug) && generalPreferences.needToShowPlaylistFavoriteTooltip()) {
                showFavoriteTooltipEvent.call()
                generalPreferences.setPlaylistFavoriteTooltipShown()
            } else if (generalPreferences.needToShowPlaylistDownloadTooltip()) {
                showDownloadTooltipEvent.call()
                generalPreferences.setPlaylistDownloadTooltipShown()
            }
        }
    }

    private fun checkSyncAvailability(): Single<Boolean> {
        return Single.create { emitter ->
            var syncEnabled = false
            try {
                if (deleted) {
                    syncEnabled = true
                } else if (online) {
                    val dbItem = AMResultItem.findById(playlist.itemId)
                    if (dbItem != null) {
                        dbItem.loadTracks()
                        val dbTracks = dbItem.tracks ?: emptyList()
                        val onlineTracks =
                            playlist.tracks?.filter { !it.isGeoRestricted } ?: emptyList()
                        var songsToBeSynced = 0
                        onlineTracks.forEach { onlineTrack ->
                            if (dbTracks.indexOfFirst { it.itemId != null && it.itemId == onlineTrack.itemId } == -1) {
                                songsToBeSynced++
                            }
                        }
                        syncEnabled = songsToBeSynced > 0
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e)
                syncEnabled = false
            }
            emitter.onSuccess(syncEnabled)
        }
    }

    // Download

    fun onTrackDownloadTapped(track: AMResultItem, mixpanelButton: String) {
        download(track, mixpanelButton)
    }

    fun onPlaylistSyncConfirmed() {
        download(playlist, MixpanelButtonPlaylistDetails)
        if (userDataSource.isLoggedIn() && !playlist.isUploadedByMyself(MainApplication.context) && !userDataSource.isMusicFavorited(playlist)) {
            onFavoriteTapped()
        }
    }

    private fun download(music: AMResultItem, mixpanelButton: String) {
        actionsDataSource.toggleDownload(music, mixpanelButton, mixpanelSource)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                when (result) {
                    ToggleDownloadResult.ConfirmPlaylistDeletion -> showConfirmPlaylistDownloadDeletionEvent.postValue(music)
                    is ToggleDownloadResult.ConfirmMusicDeletion -> showConfirmDownloadDeletionEvent.postValue(music)
                    is ToggleDownloadResult.ConfirmPlaylistDownload -> showConfirmPlaylistSyncEvent.postValue(result.tracksCount)
                    ToggleDownloadResult.StartedBlockingAPICall -> showHUDEvent.postValue(ProgressHUDMode.Loading)
                    ToggleDownloadResult.EndedBlockingAPICall -> showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    is ToggleDownloadResult.ShowUnlockedToast -> showUnlockedToastEvent.postValue(result.musicName)
                }
            }, { throwable ->
                when (throwable) {
                    is ToggleDownloadException.LoggedOut -> {
                        pendingActionAfterLogin = PendingActionAfterLogin.Download(music, mixpanelButton)
                        loginRequiredEvent.postValue(throwable.source)
                    }
                    is ToggleDownloadException.Unsubscribed -> premiumRequiredEvent.postValue(throwable.mode)
                    ToggleDownloadException.FailedDownloadingPlaylist -> showFailedPlaylistDownloadEvent.call()
                    is ToggleDownloadException.ShowPremiumDownload -> showPremiumDownloadEvent.postValue(throwable.model)
                    else -> Timber.w(throwable)
                }
            })
            .also { compositeDisposable.add(it) }
    }

    // Follow

    fun onFollowTapped() {
        compositeDisposable.add(
            actionsDataSource.toggleFollow(playlist, null, MixpanelButtonPlaylistDetails, mixpanelSource)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    when (it) {
                        is ToggleFollowResult.Finished -> _followStatus.postValue(it.followed)
                        is ToggleFollowResult.Notify -> notifyFollowToast.postValue(it)
                        is ToggleFollowResult.AskForPermission -> promptNotificationPermissionEvent.postValue(it.redirect)
                    }
                }, {
                    when (it) {
                        is ToggleFollowException.LoggedOut -> {
                            pendingActionAfterLogin = PendingActionAfterLogin.Follow
                            loginRequiredEvent.postValue(LoginSignupSource.AccountFollow)
                        }
                        is ToggleFollowException.Offline -> notifyOfflineEvent.call()
                    }
                })
        )
    }

    // Favorite

    fun onFavoriteTapped() {
        actionsDataSource.toggleFavorite(playlist, MixpanelButtonPlaylistDetails, mixpanelSource)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                when (result) {
                    is ToggleFavoriteResult.Notify -> notifyFavoriteEvent.postValue(result)
                }
            }, { throwable ->
                when (throwable) {
                    is ToggleFavoriteException.LoggedOut -> {
                        pendingActionAfterLogin = PendingActionAfterLogin.Favorite()
                        loginRequiredEvent.postValue(LoginSignupSource.Favorite)
                    }
                    is ToggleFavoriteException.Offline -> {
                        notifyOfflineEvent.call()
                    }
                    else -> {
                        val errorMsg = if (isPlaylistFavorited) {
                            MainApplication.context?.getString(R.string.toast_unfavorited_playlist_error)
                        } else {
                            MainApplication.context?.getString(R.string.toast_favorited_playlist_error)
                        }
                        showHUDEvent.postValue(ProgressHUDMode.Failure(errorMsg ?: ""))
                    }
                }
            })
            .also { compositeDisposable.add(it) }
    }

    private fun setFavoriteListener() {
        playlist.favoriteSubject.subscribe(object : PlaylistObserver<AMResultItem.ItemAPIStatus>() {
            override fun onNext(status: AMResultItem.ItemAPIStatus) {
                when (status) {
                    AMResultItem.ItemAPIStatus.Loading -> {
                        _favoriteAction.postValue(Favorite(ActionState.LOADING))
                    }
                    AMResultItem.ItemAPIStatus.Off -> {
                        _favoriteAction.postValue(Favorite(ActionState.DEFAULT))
                    }
                    AMResultItem.ItemAPIStatus.On -> {
                        _favoriteAction.postValue(Favorite(ActionState.ACTIVE))
                    }
                    AMResultItem.ItemAPIStatus.Queued -> {}
                }
            }
        })
    }

    // Comments count

    private fun setCommentCountListener() {
        playlist.commentsCountSubject.subscribe(object : PlaylistObserver<Int>() {
            override fun onNext(count: Int) {
                _commentsCount.postValue(count)
            }
        })
    }

    // PlaylistTracksAdapter.Listener

    override fun onTrackTapped(track: AMResultItem) {
        if (track.isGeoRestricted) {
            georestrictedMusicClickedEvent.postValue(track)
            return
        }
        compositeDisposable.add(
            musicManager.isDownloadFailed(track)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ failed ->
                    if (failed) {
                        openTrackOptionsFailedDownloadEvent.postValue(track)
                    } else {
                        val index = max(0, tracks.filter { !it.isGeoRestricted }.indexOfFirst { it.itemId == track.itemId })
                        openTrackEvent.postValue(Triple(track, playlist, index))
                    }
                }, {})
        )
    }

    override fun onTrackActionsTapped(track: AMResultItem) {
        if (track.isGeoRestricted) {
            georestrictedMusicClickedEvent.postValue(track)
            return
        }
        compositeDisposable.add(
            musicManager.isDownloadFailed(track)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ failed ->
                    if (failed) {
                        openTrackOptionsFailedDownloadEvent.postValue(track)
                    } else {
                        openTrackOptionsEvent.postValue(track)
                    }
                }, {})
        )
    }

    override fun onTrackDownloadTapped(track: AMResultItem) {
        download(track, MixpanelButtonList)
    }

    override fun onTrackFavoriteTapped(track: AMResultItem) {
        actionsDataSource.toggleFavorite(track, MixpanelButtonList, mixpanelSource)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                when (result) {
                    is ToggleFavoriteResult.Notify -> {
                        playlist.tracks?.indexOfFirst { it.itemId == track.itemId }?.takeIf { it != -1 }?.let { position ->
                            reloadAdapterTrackEvent.postValue(position)
                        }
                        notifyFavoriteEvent.postValue(result)
                    }
                }
            }, { throwable ->
                when (throwable) {
                    is ToggleFavoriteException.LoggedOut -> {
                        pendingActionAfterLogin = PendingActionAfterLogin.Favorite(track)
                        loginRequiredEvent.postValue(LoginSignupSource.Favorite)
                    }
                    is ToggleFavoriteException.Offline -> {
                        notifyOfflineEvent.call()
                    }
                    else -> {
                        val errorMsg = if (userDataSource.isMusicFavorited(track)) {
                            MainApplication.context?.getString(R.string.toast_unfavorited_song_error)
                        } else {
                            MainApplication.context?.getString(R.string.toast_favorited_song_error)
                        }
                        showHUDEvent.postValue(ProgressHUDMode.Failure(errorMsg ?: ""))
                    }
                }
            })
            .also { compositeDisposable.add(it) }
    }

    override fun onCommentsTapped() {
        openCommentsEvent.postValue(playlist)
    }

    // AMRecyclerView.ScrollListener

    override fun onScroll() {
        scrollEvent.call()
    }

    // Georestricted actions

    fun removeGeorestrictedTrack(track: AMResultItem) {

        val updatedTracks = tracks.filter { it.itemId != track.itemId }

        compositeDisposable.add(
            playListDataSource.editPlaylist(
                playlist.itemId,
                playlist.title ?: "",
                playlist.genre,
                playlist.desc,
                playlist.isPrivatePlaylist,
                updatedTracks.joinToString(",") { it.itemId },
                null,
                null
            )
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .doOnSubscribe { showHUDEvent.postValue(ProgressHUDMode.Loading) }
                .doFinally { showHUDEvent.postValue(ProgressHUDMode.Dismiss) }
                .subscribe(
                    { freshPlaylist -> onMessageEvent(EventPlaylistEdited(freshPlaylist)) },
                    { showHUDEvent.postValue(ProgressHUDMode.Failure("")) }
                )
        )
    }

    // Entities

    sealed class PendingActionAfterLogin {
        object Follow : PendingActionAfterLogin()
        data class Favorite(val track: AMResultItem? = null) : PendingActionAfterLogin()
        data class Download(val music: AMResultItem, val mixpanelButton: String) : PendingActionAfterLogin()
    }

    sealed class DeletePlaylistStatus {
        object Loading : DeletePlaylistStatus()
        class Success(val message: String) : DeletePlaylistStatus()
        class Error(val message: String) : DeletePlaylistStatus()
    }

    sealed class CreatePlaylistStatus {
        object Loading : CreatePlaylistStatus()
        class Success(val message: String) : CreatePlaylistStatus()
        class Error(val message: String) : CreatePlaylistStatus()
    }

    // Utils

    abstract inner class PlaylistObserver<T> : Observer<T> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            Timber.tag(TAG).e(e)
        }
    }

    companion object {
        private const val TAG = "PlaylistViewModel"
    }
}
