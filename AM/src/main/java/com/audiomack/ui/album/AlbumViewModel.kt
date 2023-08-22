package com.audiomack.ui.album

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
import com.audiomack.data.actions.ToggleRepostException
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.music.MusicManager
import com.audiomack.data.music.MusicManagerImpl
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadRepository
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonAlbumDetails
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.ActionToBeResumed
import com.audiomack.model.EventDeletedDownload
import com.audiomack.model.EventDownload
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.model.EventLoginState
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.PremiumDownloadMusicModel
import com.audiomack.model.PremiumDownloadStatsModel
import com.audiomack.model.PremiumLimitedDownloadAlertViewType
import com.audiomack.model.PremiumOnlyDownloadAlertViewType
import com.audiomack.model.ProgressHUDMode
import com.audiomack.playback.ActionState
import com.audiomack.playback.ActionState.ACTIVE
import com.audiomack.playback.ActionState.DEFAULT
import com.audiomack.playback.ActionState.DISABLED
import com.audiomack.playback.ActionState.LOADING
import com.audiomack.playback.Playback
import com.audiomack.playback.PlaybackItem
import com.audiomack.playback.PlaybackState
import com.audiomack.playback.PlayerPlayback
import com.audiomack.playback.SongAction.Download
import com.audiomack.playback.SongAction.Favorite
import com.audiomack.playback.SongAction.RePost
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.mylibrary.offline.local.LocalMediaExclusionsDataSource
import com.audiomack.ui.mylibrary.offline.local.LocalMediaExclusionsRepository
import com.audiomack.usecases.RefreshCommentCountUseCase
import com.audiomack.usecases.RefreshCommentCountUseCaseImpl
import com.audiomack.utils.GeneralPreferences
import com.audiomack.utils.GeneralPreferencesImpl
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.views.AMRecyclerView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import kotlin.math.max
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class AlbumViewModel(
    private val album: AMResultItem,
    val mixpanelSource: MixpanelSource,
    private val openShare: Boolean,
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    val imageLoader: ImageLoader = PicassoImageLoader,
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    private val queueDataSource: QueueDataSource = QueueRepository.getInstance(),
    private val playerPlayback: Playback = PlayerPlayback.getInstance(),
    private val generalPreferences: GeneralPreferences = GeneralPreferencesImpl(),
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val musicManager: MusicManager = MusicManagerImpl(),
    private val eventBus: EventBus = EventBus.getDefault(),
    refreshCommentCountUseCase: RefreshCommentCountUseCase = RefreshCommentCountUseCaseImpl(),
    private val premiumDownloadDataSource: PremiumDownloadDataSource = PremiumDownloadRepository.getInstance(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val musicDownloadActionStateHelper: MusicDownloadActionStateHelper = MusicDownloadActionStateHelperImpl(
        premiumDownloadDataSource,
        premiumDataSource
    ),
    private val exclusionsRepo: LocalMediaExclusionsDataSource = LocalMediaExclusionsRepository.getInstance()
) : BaseViewModel(), AlbumTracksAdapter.Listener, AMRecyclerView.ScrollListener {

    private val _album = MutableLiveData(album)

    /** Emits a String with the album title */
    val title: LiveData<String> = Transformations.map(_album) { it.title ?: "" }

    /** Emits a String with the album artist */
    val artist: LiveData<String> = Transformations.map(_album) { it.artist ?: "" }

    /** Emits a String with the album featuring data */
    val feat: LiveData<String> = Transformations.map(_album) { it.featured ?: "" }

    /** Emits a Boolean indicating if the featuring text is visible or not */
    val featVisible: LiveData<Boolean> = Transformations.map(_album) { !it.featured.isNullOrEmpty() }

    val showUploader: LiveData<Boolean> = Transformations.map(_album) { !it.isLocal }

    val enableCommentsButton: LiveData<Boolean> = Transformations.map(_album) { !it.isLocal }

    val enableShareButton: LiveData<Boolean> = Transformations.map(_album) { !it.isLocal }

    val showInfoButton: LiveData<Boolean> = Transformations.map(_album) { !it.isLocal }

    /** Emits a Boolean indicating if the album uploader is currently followed or not */
    private val _followStatus = MutableLiveData<Boolean>()
    val followStatus: LiveData<Boolean> get() = _followStatus

    /** Emits a Boolean indicating if the follow button is visible or not */
    val followVisible: LiveData<Boolean> = Transformations.map(_album) { userDataSource.getUserSlug() != it.uploaderSlug }

    /** Emits a Boolean indicating if the repost button is visible or not */
    val repostVisible: LiveData<Boolean> = Transformations.map(_album) { userDataSource.getUserSlug() != it.uploaderSlug }

    /** Emits the high-res version of the album artwork */
    val highResImage: LiveData<String> = Transformations.map(_album) { it.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetOriginal) }

    /** Emits the low-res version of the album artwork */
    val lowResImage: LiveData<String> = Transformations.map(_album) { it.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall) }

    /** Emits a Boolean indicating if the play button is active or not */
    private val _playButtonActive = MutableLiveData<Boolean>()
    val playButtonActive: LiveData<Boolean> get() = _playButtonActive

    /** Emits the whole album, used to show the tracks */
    val setupTracksEvent = SingleLiveEvent<AMResultItem>()
    val closeEvent = SingleLiveEvent<Void>()
    val notifyFollowToastEvent = SingleLiveEvent<ToggleFollowResult.Notify>()
    val notifyOfflineEvent = SingleLiveEvent<Void>()
    val notifyRepostEvent = SingleLiveEvent<ToggleRepostResult.Notify>()
    val notifyFavoriteEvent = SingleLiveEvent<ToggleFavoriteResult.Notify>()
    val loginRequiredEvent = SingleLiveEvent<LoginSignupSource>()
    val showErrorEvent = SingleLiveEvent<String>()
    val openUploaderEvent = SingleLiveEvent<String>()
    val downloadTooltipEvent = SingleLiveEvent<Void>()
    val openMusicInfoEvent = SingleLiveEvent<AMResultItem>()
    val scrollEvent = SingleLiveEvent<Void>()
    val shuffleEvent = SingleLiveEvent<Pair<AMResultItem, AMResultItem>>()
    val openTrackOptionsEvent = SingleLiveEvent<AMResultItem>()
    val shareEvent = SingleLiveEvent<AMResultItem>()
    val georestrictedMusicClickedEvent = SingleLiveEvent<Void>()
    val openTrackEvent = SingleLiveEvent<Triple<AMResultItem, AMResultItem?, Int>>()
    val openTrackOptionsFailedDownloadEvent = SingleLiveEvent<AMResultItem>()
    val openCommentsEvent = SingleLiveEvent<AMResultItem>()
    val reloadAdapterTracksEvent = SingleLiveEvent<Void>()
    val reloadAdapterTracksRangeEvent = SingleLiveEvent<List<Int>>()
    val reloadAdapterTrackEvent = SingleLiveEvent<Int>()
    val adapterTracksChangedEvent = SingleLiveEvent<List<AMResultItem>>()
    val removeTrackFromAdapterEvent = SingleLiveEvent<AMResultItem>()
    val premiumRequiredEvent = SingleLiveEvent<InAppPurchaseMode>()
    val showHUDEvent = SingleLiveEvent<ProgressHUDMode>()
    val showConfirmDownloadDeletionEvent = SingleLiveEvent<AMResultItem>()
    val showPremiumDownloadEvent = SingleLiveEvent<PremiumDownloadModel>()
    val showUnlockedToastEvent = SingleLiveEvent<String>()
    val promptNotificationPermissionEvent = SingleLiveEvent<PermissionRedirect>()
    val genreEvent = SingleLiveEvent<String>()
    val tagEvent = SingleLiveEvent<String>()

    val adsVisible: Boolean
    get() = adsDataSource.adsVisible

    val isAlbumFavorited: Boolean
        get() = userDataSource.isMusicFavorited(album)

    private val isAlbumReposted: Boolean
        get() = userDataSource.isMusicReposted(album)

    private val _favoriteAction = MutableLiveData<Favorite>()
    val favoriteAction: LiveData<Favorite> get() = _favoriteAction

    private val _rePostAction = MutableLiveData<RePost>()
    val rePostAction: LiveData<RePost> get() = _rePostAction

    private val _downloadAction = MutableLiveData<Download>()
    val downloadAction: LiveData<Download> get() = _downloadAction

    private val _commentsCount = MutableLiveData<Int>()
    val commentsCount: LiveData<Int> get() = _commentsCount

    var recyclerviewConfigured = false

    private var pendingActionAfterLogin: PendingActionAfterLogin? = null

    private val reloadAdapterTracksBuffer = mutableListOf<Int>()
    private val reloadAdapterTracksBufferSubject = BehaviorSubject.create<Boolean>()

    @VisibleForTesting
    val premiumObserver = object : AlbumObserver<Boolean>() {
        override fun onNext(premium: Boolean) {
            if (!album.isLocal) {
                _downloadAction.postValue(
                    Download(musicDownloadActionStateHelper.downloadState(album))
                )
            }
        }
    }

    @VisibleForTesting
    val playbackStateObserver = object : AlbumObserver<PlaybackState>() {
        override fun onNext(state: PlaybackState) {
            _playButtonActive.postValue((state == PlaybackState.PLAYING || state == PlaybackState.LOADING) && queueDataSource.isCurrentItemOrParent(album))
        }
    }

    @VisibleForTesting
    val playbackItemObserver = object : AlbumObserver<PlaybackItem>() {
        override fun onNext(t: PlaybackItem) {
            reloadAdapterTracksEvent.call()
        }
    }

    @VisibleForTesting
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventDownload: EventDownload) {
        if (eventDownload.itemId == album.itemId) {
            if (album.itemId == eventDownload.itemId) {
                refreshDownloadButton()
            }
        } else {
            album.tracks?.indexOfFirst { it.itemId == eventDownload.itemId }?.takeIf { it != -1 }?.let {
                reloadAdapterTracksBuffer.add(it)
                reloadAdapterTracksBufferSubject.onNext(true)
                refreshDownloadButton()
            }
        }
    }

    @VisibleForTesting
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventDeletedDownload: EventDeletedDownload) {
        if (album.isDownloaded) {
            if (album.tracks?.size == 1 && album.tracks?.firstOrNull()?.itemId == eventDeletedDownload.item.itemId) {
                // If we have removed the last album track then delete the album, close this screen and notify its deletion
                album.deepDelete()
                closeEvent.call()
                EventBus.getDefault().post(EventDeletedDownload(album))
            }
            removeTrackFromAdapterEvent.postValue(eventDeletedDownload.item)
        } else {
            reloadAdapterTracksEvent.call()
        }
    }

    @VisibleForTesting
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventDownloadsEdited) {
        reloadAdapterTracksEvent.call()
        refreshDownloadButton()
    }

    init {
        _followStatus.postValue(userDataSource.isArtistFollowed(album.uploaderId))
        _favoriteAction.postValue(
            if (album.isLocal) Favorite(ActionState.DISABLED)
            else Favorite(if (isAlbumFavorited) ACTIVE else DEFAULT)
        )
        _rePostAction.postValue(
            if (album.isLocal) RePost(ActionState.DISABLED)
            else RePost(if (isAlbumReposted) ACTIVE else DEFAULT)
        )
        refreshDownloadButton()

        setupTracksEvent.postValue(album)

        // Subscriptions
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        premiumDataSource.premiumObservable.subscribe(premiumObserver)
        subscribeToPlayback()
        setFavoriteListener()
        setRePostListener()
        setCommentCountListener()
        eventBus.register(this)
        compositeDisposable.add(refreshCommentCountUseCase.refresh(album).subscribe())
        subscribeToAdapterUpdates()
        subscribeToExclusionsUpdates()

        if (openShare) {
            shareEvent.postValue(album)
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
                    is PendingActionAfterLogin.Follow -> onFollowTapped(it.mixpanelSource)
                    is PendingActionAfterLogin.Favorite -> {
                        it.track
                            ?.let { onTrackFavoriteTapped(it) }
                            ?: onFavoriteTapped()
                    }
                    is PendingActionAfterLogin.Repost -> onRepostTapped()
                    is PendingActionAfterLogin.Download -> download(it.music, it.mixpanelButton)
                }
                pendingActionAfterLogin = null
            }
            else -> pendingActionAfterLogin = null
        }
    }

    private fun refreshDownloadButton() {
        if (album.isLocal) {
            _downloadAction.value = Download(DISABLED)
            return
        }

        musicDataSource.getOfflineResource(album.itemId)
            .flatMap { resource ->
                resource.data?.let { Observable.just(it) }
                    ?: Observable.error(RuntimeException())
            }
            .map { musicDownloadActionStateHelper.downloadState(album) }
            .observeOn(schedulersProvider.main)
            .subscribe({
                _downloadAction.value = Download(it)
            }, {
                _downloadAction.value = Download(DEFAULT)
            })
            .also { compositeDisposable.add(it) }
    }

    fun onDownloadTapped() {
        download(album, MixpanelButtonAlbumDetails)
    }

    fun onTrackDownloadTapped(track: AMResultItem, mixpanelButton: String) {
        download(track, mixpanelButton)
    }

    private fun download(music: AMResultItem, mixpanelButton: String) {
        actionsDataSource.toggleDownload(music, mixpanelButton, mixpanelSource, skipFrozenCheck = !mixpanelSource.isInMyDownloads, parentAlbum = album)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                when (result) {
                    is ToggleDownloadResult.ConfirmMusicDeletion -> showConfirmDownloadDeletionEvent.postValue(music)
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
                    is ToggleDownloadException.ShowPremiumDownload -> showPremiumDownloadEvent.postValue(throwable.model)
                    else -> Timber.w(throwable)
                }
            })
            .also { compositeDisposable.add(it) }
    }

    override fun onUploaderTapped() {
        album.uploaderSlug?.let {
            openUploaderEvent.postValue(it)
        }
    }

    override fun onTagTapped(tag: String) {
        if (tag == _album.value?.genre) {
            genreEvent.postValue(tag.trim())
        } else {
            tagEvent.postValue("tag:${tag.trim()}")
        }
    }

    fun onBackTapped() {
        closeEvent.call()
    }

    fun onInfoTapped() {
        openMusicInfoEvent.postValue(album)
    }

    fun onPlayAllTapped() {
        if (queueDataSource.isCurrentItemOrParent(album)) {
            playerPlayback.apply { if (isPlaying) pause() else play() }
        } else {
            if (mixpanelSource.isInMyDownloads && album.downloadType == AMResultItem.MusicDownloadType.Limited && premiumDownloadDataSource.getFrozenCount(album) > 0) {
                val availableMusicToUnfreeze = premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount + premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(album) <= premiumDownloadDataSource.premiumDownloadLimit
                showPremiumDownloadEvent.postValue(PremiumDownloadModel(
                    music = PremiumDownloadMusicModel(album, premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(album)),
                    stats = PremiumDownloadStatsModel(MixpanelButtonList, mixpanelSource, premiumDownloadDataSource.premiumDownloadLimit, premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount),
                    alertTypeLimited = if (availableMusicToUnfreeze) PremiumLimitedDownloadAlertViewType.PlayFrozenOfflineWithAvailableUnfreezes else PremiumLimitedDownloadAlertViewType.PlayFrozenOffline
                ))
            } else if (mixpanelSource.isInMyDownloads && album.downloadType == AMResultItem.MusicDownloadType.Premium && !premiumDataSource.isPremium) {
                showPremiumDownloadEvent.postValue(PremiumDownloadModel(
                    music = PremiumDownloadMusicModel(album),
                    alertTypePremium = PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline
                ))
            } else {
                album.tracks?.firstOrNull()?.let { onTrackTapped(it) }
            }
        }
    }

    fun onShuffleTapped() {
        if (mixpanelSource.isInMyDownloads && album.downloadType == AMResultItem.MusicDownloadType.Limited && premiumDownloadDataSource.getFrozenCount(album) > 0) {
            val availableMusicToUnfreeze = premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount + premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(album) <= premiumDownloadDataSource.premiumDownloadLimit
            showPremiumDownloadEvent.postValue(PremiumDownloadModel(
                music = PremiumDownloadMusicModel(album, premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(album)),
                stats = PremiumDownloadStatsModel(MixpanelButtonList, mixpanelSource, premiumDownloadDataSource.premiumDownloadLimit, premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount),
                alertTypeLimited = if (availableMusicToUnfreeze) PremiumLimitedDownloadAlertViewType.PlayFrozenOfflineWithAvailableUnfreezes else PremiumLimitedDownloadAlertViewType.PlayFrozenOffline,
                actionToBeResumed = ActionToBeResumed.Shuffle
            ))
        } else if (mixpanelSource.isInMyDownloads && album.downloadType == AMResultItem.MusicDownloadType.Premium && !premiumDataSource.isPremium) {
            showPremiumDownloadEvent.postValue(PremiumDownloadModel(
                music = PremiumDownloadMusicModel(album),
                alertTypePremium = PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline,
                actionToBeResumed = ActionToBeResumed.Shuffle
            ))
        } else {
            val tracks = album.tracks ?: return
            val firstTrack = tracks.firstOrNull() ?: return
            shuffleEvent.postValue(Pair(firstTrack, album))
        }
    }

    fun onShareTapped() {
        shareEvent.postValue(album)
    }

    fun onLayoutReady() {
        // Do not show tooltip if we need to open the menu
        if (!openShare) {
            if (generalPreferences.needToShowAlbumDownloadTooltip()) {
                generalPreferences.setAlbumDownloadTooltipShown()
                downloadTooltipEvent.call()
            }
        }
    }

    // Follow

    fun onFollowTapped(mixpanelSource: MixpanelSource) {
        compositeDisposable.add(
            actionsDataSource.toggleFollow(album, null, MixpanelButtonAlbumDetails, mixpanelSource)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    when (it) {
                        is ToggleFollowResult.Finished -> _followStatus.postValue(it.followed)
                        is ToggleFollowResult.Notify -> notifyFollowToastEvent.postValue(it)
                        is ToggleFollowResult.AskForPermission -> promptNotificationPermissionEvent.postValue(it.redirect)
                    }
                }, {
                    when (it) {
                        is ToggleFollowException.LoggedOut -> {
                            pendingActionAfterLogin = PendingActionAfterLogin.Follow(mixpanelSource)
                            loginRequiredEvent.postValue(LoginSignupSource.AccountFollow)
                        }
                        is ToggleFollowException.Offline -> notifyOfflineEvent.call()
                    }
                })
        )
    }

    // Favorite

    fun onFavoriteTapped() {
        actionsDataSource.toggleFavorite(album, MixpanelButtonAlbumDetails, mixpanelSource)
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
                        val errorMsg = if (isAlbumFavorited) {
                            MainApplication.context?.getString(R.string.toast_unfavorited_album_error)
                        } else {
                            MainApplication.context?.getString(R.string.toast_favorited_album_error)
                        }
                        showErrorEvent.postValue(errorMsg ?: "")
                    }
                }
            })
            .also { compositeDisposable.add(it) }
    }

    private fun setFavoriteListener() {
        if (album.isLocal) return

        album.favoriteSubject.subscribe(object : AlbumObserver<AMResultItem.ItemAPIStatus>() {
            override fun onNext(status: AMResultItem.ItemAPIStatus) {
                when (status) {
                    AMResultItem.ItemAPIStatus.Loading -> {
                        _favoriteAction.postValue(Favorite(LOADING))
                    }
                    AMResultItem.ItemAPIStatus.Off -> {
                        _favoriteAction.postValue(Favorite(DEFAULT))
                    }
                    AMResultItem.ItemAPIStatus.On -> {
                        _favoriteAction.postValue(Favorite(ACTIVE))
                    }
                    AMResultItem.ItemAPIStatus.Queued -> {}
                }
            }
        })
    }

    // Repost

    fun onRepostTapped() {
        actionsDataSource.toggleRepost(album, MixpanelButtonAlbumDetails, mixpanelSource)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                when (result) {
                    is ToggleRepostResult.Notify -> notifyRepostEvent.postValue(result)
                }
            }, { throwable ->
                when (throwable) {
                    is ToggleRepostException.LoggedOut -> {
                        pendingActionAfterLogin = PendingActionAfterLogin.Repost
                        loginRequiredEvent.postValue(LoginSignupSource.Repost)
                    }
                    is ToggleRepostException.Offline -> {
                        notifyOfflineEvent.call()
                    }
                    else -> {
                        val errorMsg =
                            MainApplication.context?.getString(R.string.toast_reposted_album_error)
                        showErrorEvent.postValue(errorMsg ?: "")
                    }
                }
            })
            .also { compositeDisposable.add(it) }
    }

    private fun setRePostListener() {
        if (album.isLocal) return

        album.repostSubject.subscribe(object : AlbumObserver<AMResultItem.ItemAPIStatus>() {
            override fun onNext(status: AMResultItem.ItemAPIStatus) {
                when (status) {
                    AMResultItem.ItemAPIStatus.Loading -> {
                        _rePostAction.postValue(RePost(LOADING))
                    }
                    AMResultItem.ItemAPIStatus.Off -> {
                        _rePostAction.postValue(RePost(DEFAULT))
                    }
                    AMResultItem.ItemAPIStatus.On -> {
                        _rePostAction.postValue(RePost(ACTIVE))
                    }
                    AMResultItem.ItemAPIStatus.Queued -> {}
                }
            }
        })
    }

    // Comments count

    private fun setCommentCountListener() {
        album.commentsCountSubject.subscribe(object : AlbumObserver<Int>() {
            override fun onNext(count: Int) {
                _commentsCount.postValue(count)
            }
        })
    }

    // AlbumTracksAdapter.Listener

    override fun onTrackTapped(track: AMResultItem) {
        val tracks = album.tracks ?: return
        if (track.isGeoRestricted) {
            georestrictedMusicClickedEvent.call()
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
                        openTrackEvent.postValue(Triple(track, album, index))
                    }
                }, {})
        )
    }

    override fun onTrackActionsTapped(track: AMResultItem) {
        if (track.isGeoRestricted) {
            georestrictedMusicClickedEvent.call()
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
                        album.tracks?.indexOfFirst { it.itemId == track.itemId }?.let { position ->
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
                        showErrorEvent.postValue(errorMsg ?: "")
                    }
                }
            })
            .also { compositeDisposable.add(it) }
    }

    fun onRemoveTrackFromAdapter(track: AMResultItem) {
        removeTrackFromAdapterEvent.postValue(track)
    }

    override fun onCommentsTapped() {
        openCommentsEvent.postValue(album)
    }

    override fun onFollowTapped() {
        onFollowTapped(mixpanelSource)
    }

    private fun subscribeToExclusionsUpdates() {
        if (!album.isLocal) return

        exclusionsRepo.exclusionsObservable
            .subscribeOn(schedulersProvider.io)
            .map { exclusions -> exclusions.map { it.mediaId.toString() } }
            .map { exclusions -> album.tracks?.filterNot { exclusions.contains(it.itemId) } }
            .observeOn(schedulersProvider.main)
            .subscribe { tracks -> adapterTracksChangedEvent.postValue(tracks) }
            .composite()
    }

    // AMRecyclerView.ScrollListener

    override fun onScroll() {
        scrollEvent.call()
    }

    // Entities

    sealed class PendingActionAfterLogin {
        data class Follow(val mixpanelSource: MixpanelSource) : PendingActionAfterLogin()
        object Repost : PendingActionAfterLogin()
        data class Favorite(val track: AMResultItem? = null) : PendingActionAfterLogin()
        data class Download(val music: AMResultItem, val mixpanelButton: String) : PendingActionAfterLogin()
    }

    // Utils

    abstract inner class AlbumObserver<T> : Observer<T> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            Timber.tag(TAG).e(e)
        }
    }

    // Static

    companion object {
        private const val TAG = "AlbumViewModel"
    }
}
