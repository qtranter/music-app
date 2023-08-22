package com.audiomack.ui.slideupmenu.music

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.common.MusicDownloadActionStateHelper
import com.audiomack.common.MusicDownloadActionStateHelperImpl
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.AddToPlaylistException
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleHighlightException
import com.audiomack.data.actions.ToggleHighlightResult
import com.audiomack.data.actions.ToggleRepostException
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadRepository
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.share.ShareManager
import com.audiomack.data.share.ShareManagerImpl
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonKebabMenu
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItem.ItemAPIStatus
import com.audiomack.model.ActionToBeResumed
import com.audiomack.model.EventDownload
import com.audiomack.model.EventHighlightsUpdated
import com.audiomack.model.EventLoginState
import com.audiomack.model.EventRemovedDownloadFromList
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.PremiumDownloadMusicModel
import com.audiomack.model.PremiumDownloadStatsModel
import com.audiomack.model.PremiumLimitedDownloadAlertViewType
import com.audiomack.model.PremiumOnlyDownloadAlertViewType
import com.audiomack.model.ProgressHUDMode
import com.audiomack.model.ShareMethod
import com.audiomack.playback.ActionState.ACTIVE
import com.audiomack.playback.ActionState.DEFAULT
import com.audiomack.playback.ActionState.LOADING
import com.audiomack.playback.SongAction.AddToPlaylist
import com.audiomack.playback.SongAction.Download
import com.audiomack.playback.SongAction.Favorite
import com.audiomack.playback.SongAction.RePost
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.usecases.RefreshCommentCountUseCase
import com.audiomack.usecases.RefreshCommentCountUseCaseImpl
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.Collections
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class SlideUpMenuMusicViewModel(
    private val music: AMResultItem,
    private val mixpanelSource: MixpanelSource,
    private val removeFromDownloadsEnabled: Boolean,
    private val removeFromQueueEnabled: Boolean,
    private val removeFromQueueIndex: Int?,
    private val shareManager: ShareManager = ShareManagerImpl(),
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val queueDataSource: QueueDataSource = QueueRepository.getInstance(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    refreshCommentCountUseCase: RefreshCommentCountUseCase = RefreshCommentCountUseCaseImpl(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val premiumDownloadDataSource: PremiumDownloadDataSource = PremiumDownloadRepository.getInstance(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val eventBus: EventBus = EventBus.getDefault(),
    private val musicDownloadActionStateHelper: MusicDownloadActionStateHelper = MusicDownloadActionStateHelperImpl(premiumDownloadDataSource, premiumDataSource)
) : BaseViewModel() {

    data class ViewState(
        val imageUrl: String? = null,
        val title: String? = null,
        val artist: String? = null,
        val addedBy: String? = null,
        val uploaderVerified: Boolean = false,
        val uploaderTastemaker: Boolean = false,
        val uploaderAuthenticated: Boolean = false,
        val addToPlaylistVisible: Boolean = false,
        val repostVisible: Boolean = false,
        val downloadVisible: Boolean = false,
        val deleteDownloadVisible: Boolean = false,
        val musicFavorited: Boolean = false,
        val musicHighlighted: Boolean = false,
        val removeFromDownloadsEnabled: Boolean = false,
        val removeFromQueueEnabled: Boolean = false
    )

    val closeEvent = SingleLiveEvent<Void>()
    val dismissEvent = SingleLiveEvent<Void>()
    val musicInfoEvent = SingleLiveEvent<Void>()
    val artistInfoEvent = SingleLiveEvent<String?>()
    val startAnimationEvent = SingleLiveEvent<Void>()
    val addToPlaylistEvent = SingleLiveEvent<Triple<List<AMResultItem>, MixpanelSource, String>>()
    val showHUDEvent = SingleLiveEvent<ProgressHUDMode>()
    val notifyOfflineEvent = SingleLiveEvent<Void>()
    val notifyRepostEvent = SingleLiveEvent<ToggleRepostResult.Notify>()
    val notifyFavoriteEvent = SingleLiveEvent<ToggleFavoriteResult.Notify>()
    val loginRequiredEvent = SingleLiveEvent<LoginSignupSource>()
    val premiumRequiredEvent = SingleLiveEvent<InAppPurchaseMode>()
    val openCommentsEvent = SingleLiveEvent<AMResultItem>()
    val reachedHighlightsLimitEvent = SingleLiveEvent<Void>()
    val highlightErrorEvent = SingleLiveEvent<Void>()
    val highlightSuccessEvent = SingleLiveEvent<String>()
    val showConfirmDownloadDeletionEvent = SingleLiveEvent<AMResultItem>()
    val showConfirmPlaylistDownloadDeletionEvent = SingleLiveEvent<AMResultItem>()
    val showFailedPlaylistDownloadEvent = SingleLiveEvent<Void>()
    val showConfirmPlaylistSyncEvent = SingleLiveEvent<Int>()
    val showPremiumDownloadEvent = SingleLiveEvent<PremiumDownloadModel>()
    val showUnlockedToastEvent = SingleLiveEvent<String>()

    val mixpanelButton = MixpanelButtonKebabMenu

    private val _favoriteAction = MutableLiveData<Favorite>()
    val favoriteAction: LiveData<Favorite> get() = _favoriteAction

    private val _addToPlaylistAction = MutableLiveData<AddToPlaylist>()
    val addToPlaylistAction: LiveData<AddToPlaylist> get() = _addToPlaylistAction

    private val _rePostAction = MutableLiveData<RePost>()
    val rePostAction: LiveData<RePost> get() = _rePostAction

    private val _downloadAction = MutableLiveData<Download>()
    val downloadAction: LiveData<Download> get() = _downloadAction

    private val _commentsCount = MutableLiveData<Int>()
    val commentsCount: LiveData<Int> get() = _commentsCount

    private val _viewState = MutableLiveData<ViewState>()
    val viewState: LiveData<ViewState> get() = _viewState

    private val typeForAnalytics = music.artist?.let {
        "Artist"
    } ?: music.let {
        when {
            it.isAlbum -> "Album"
            it.isPlaylist -> "Playlist"
            else -> "Song"
        }
    }

    private var pendingActionAfterLogin: PendingActionAfterLogin? = null

    private val premiumObserver = object : MenuObserver<Boolean>() {
        override fun onNext(premium: Boolean) {
            _downloadAction.postValue(Download(musicDownloadActionStateHelper.downloadState(music)))
        }
    }

    init {
        eventBus.register(this)

        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        premiumDataSource.premiumObservable.subscribe(premiumObserver)

        _favoriteAction.postValue(
            Favorite(if (userDataSource.isMusicFavorited(music)) ACTIVE else DEFAULT)
        )
        _addToPlaylistAction.postValue(
            AddToPlaylist(DEFAULT)
        )
        _rePostAction.postValue(
            RePost(if (userDataSource.isMusicReposted(music)) ACTIVE else DEFAULT)
        )
        _downloadAction.postValue(Download(musicDownloadActionStateHelper.downloadState(music)))

        setFavoriteListener()
        setRePostListener()
        setCommentCountListener()

        refreshCommentCountUseCase.refresh(music).subscribe().addTo(compositeDisposable)

        updateViewState()
    }

    override fun onCleared() {
        super.onCleared()
        eventBus.unregister(this)
    }

    private fun updateViewState() {
        _viewState.postValue(ViewState(
            imageUrl = music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
            title = music.title,
            artist = music.artist,
            addedBy = music.uploaderName,
            uploaderVerified = music.isUploaderVerified,
            uploaderTastemaker = music.isUploaderTastemaker,
            uploaderAuthenticated = music.isUploaderAuthenticated,
            addToPlaylistVisible = !music.isPlaylist && !music.isAlbum,
            repostVisible = !music.isPlaylist && !music.isUploadedByMyself(MainApplication.context),
            downloadVisible = !music.isDownloadedAndNotCached,
            deleteDownloadVisible = music.isDownloadedAndNotCached,
            musicFavorited = userDataSource.isMusicFavorited(music),
            musicHighlighted = userDataSource.isMusicHighlighted(music),
            removeFromDownloadsEnabled = removeFromDownloadsEnabled,
            removeFromQueueEnabled = removeFromQueueEnabled
        ))
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        when (state) {
            EventLoginState.LOGGED_IN -> pendingActionAfterLogin?.let {
                when (it) {
                    is PendingActionAfterLogin.AddToPlaylist -> onAddToPlaylistTapped()
                    is PendingActionAfterLogin.Favorite -> onFavoriteTapped()
                    is PendingActionAfterLogin.Repost -> onRepostTapped()
                    is PendingActionAfterLogin.Highlight -> onHighlightTapped()
                    is PendingActionAfterLogin.Download -> download()
                }
                pendingActionAfterLogin = null
            }
            else -> pendingActionAfterLogin = null
        }
    }

    fun onCancelTapped() {
        dismissEvent.call()
    }

    fun onBackgroundTapped() {
        dismissEvent.call()
    }

    fun onMusicInfoTapped() {
        closeEvent.call()
        musicInfoEvent.call()
    }

    fun onRemoveFromDownloadsTapped() {
        showHUDEvent.postValue(ProgressHUDMode.Loading)
        compositeDisposable.add(
            musicDataSource.removeFromDownloads(music)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    EventBus.getDefault().post(EventRemovedDownloadFromList(music))
                    dismissEvent.call()
                }, {
                    showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    showHUDEvent.postValue(ProgressHUDMode.Failure(MainApplication.context?.getString(R.string.download_delete_list_failed) ?: ""))
                })
        )
    }

    fun onRemoveFromQueueTapped() {
        removeFromQueueIndex?.takeIf { it != -1 }?.let {
            val removingCurrentlyPlayingItem = queueDataSource.index == it
            queueDataSource.removeAt(it)
            if (removingCurrentlyPlayingItem) {
                queueDataSource.skip(it)
            }
        }
        dismissEvent.call()
    }

    fun onPlayNextTapped(activity: Activity, disposables: CompositeDisposable) {
        tryAddingToQueue(ActionToBeResumed.PlayNext) {
            music.playNext(activity, mixpanelSource, mixpanelButton, disposables)
            dismissEvent.call()
        }
    }

    fun onPlayLaterTapped(activity: Activity, disposables: CompositeDisposable) {
        tryAddingToQueue(ActionToBeResumed.PlayLater) {
            music.playLater(activity, mixpanelSource, mixpanelButton, disposables)
            dismissEvent.call()
        }
    }

    private fun tryAddingToQueue(actionToBeResumed: ActionToBeResumed, action: () -> Unit) {
        if (mixpanelSource.isInMyDownloads && music.downloadType == AMResultItem.MusicDownloadType.Limited && premiumDownloadDataSource.getFrozenCount(music) > 0) {
            val availableMusicToUnfreeze = premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount + premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(music) <= premiumDownloadDataSource.premiumDownloadLimit
            showPremiumDownloadEvent.postValue(PremiumDownloadModel(
                music = PremiumDownloadMusicModel(music, premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(music)),
                stats = PremiumDownloadStatsModel(MixpanelButtonList, music.mixpanelSource ?: MixpanelSource.empty, premiumDownloadDataSource.premiumDownloadLimit, premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount),
                alertTypeLimited = if (availableMusicToUnfreeze) PremiumLimitedDownloadAlertViewType.PlayFrozenOfflineWithAvailableUnfreezes else PremiumLimitedDownloadAlertViewType.PlayFrozenOffline,
                actionToBeResumed = actionToBeResumed
            ))
        } else if (mixpanelSource.isInMyDownloads && music.downloadType == AMResultItem.MusicDownloadType.Premium && !premiumDataSource.isPremium) {
            showPremiumDownloadEvent.postValue(PremiumDownloadModel(
                music = PremiumDownloadMusicModel(music),
                alertTypePremium = PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline,
                actionToBeResumed = actionToBeResumed
            ))
        } else {
            action.invoke()
        }
    }

    fun onHighlightTapped() {
        showHUDEvent.postValue(ProgressHUDMode.Loading)
        compositeDisposable.add(
            actionsDataSource.toggleHighlight(music, mixpanelButton, mixpanelSource)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ result ->
                    showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    eventBus.post(EventHighlightsUpdated())
                    updateViewState()
                    if (result is ToggleHighlightResult.Added) {
                        highlightSuccessEvent.postValue(result.title)
                    }
                }, { throwable ->
                    showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    when (throwable) {
                        ToggleHighlightException.Offline -> notifyOfflineEvent.call()
                        ToggleHighlightException.LoggedOut -> {
                            pendingActionAfterLogin = PendingActionAfterLogin.Highlight
                            loginRequiredEvent.postValue(LoginSignupSource.Highlight)
                        }
                        ToggleHighlightException.ReachedLimit -> reachedHighlightsLimitEvent.call()
                        is ToggleHighlightException.Failure -> {
                            if (throwable.highliting) {
                                highlightErrorEvent.call()
                            }
                        }
                        else -> showHUDEvent.postValue(ProgressHUDMode.Failure(""))
                    }
                })
        )
    }

    fun onArtistInfoTapped() {
        artistInfoEvent.postValue(music.uploaderSlug)
        closeEvent.call()
    }

    fun onCopyLinkTapped(activity: Activity?) {
        shareManager.copyMusicLink(activity, music, mixpanelSource, mixpanelButton)
        dismissEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Copy Link")
    }

    fun onShareViaTwitterTapped(activity: Activity?, disposables: CompositeDisposable) {
        shareManager.shareMusic(
            activity,
            music,
            ShareMethod.Twitter,
            mixpanelSource,
            mixpanelButton,
            disposables
        )
        dismissEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Twitter")
    }

    fun onShareViaFacebookTapped(activity: Activity?, disposables: CompositeDisposable) {
        shareManager.shareStory(
                activity,
                music,
                null,
                ShareMethod.Facebook,
                mixpanelSource,
                mixpanelButton,
                disposables
        )
        dismissEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Facebook")
    }

    fun onShareViaContactsTapped(activity: Activity?, disposables: CompositeDisposable) {
        shareManager.shareMusic(
            activity,
            music,
            ShareMethod.SMS,
            mixpanelSource,
            mixpanelButton,
            disposables
        )
        dismissEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Text")
    }

    fun onShareViaOtherTapped(activity: Activity?, disposables: CompositeDisposable) {
        shareManager.shareMusic(
            activity,
            music,
            ShareMethod.Standard,
            mixpanelSource,
            mixpanelButton,
            disposables
        )
        dismissEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, App")
    }

    fun onShareScreenshotTapped(activity: Activity?) {
        shareManager.shareScreenshot(activity, music, null, ShareMethod.Screenshot, null, mixpanelSource, mixpanelButton)
        dismissEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Screenshot")
    }

    fun onShareViaInstagramTapped(activity: Activity?, disposables: CompositeDisposable) {
        shareManager.shareStory(
                activity,
                music,
                null,
                ShareMethod.Instagram,
                mixpanelSource,
                mixpanelButton,
                disposables
        )
        dismissEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Instagram")
    }

    fun onShareViaSnapchatTapped(activity: Activity?, disposables: CompositeDisposable) {
        shareManager.shareStory(
                activity,
                music,
                null,
                ShareMethod.Snapchat,
                mixpanelSource,
                mixpanelButton,
                disposables
        )
        dismissEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Snapchat")
    }

    fun onShareViaWhatsAppTapped(activity: Activity?) {
        val method = ShareMethod.WhatsApp
        shareManager.shareLink(
            activity,
            music,
            null,
            method,
            mixpanelSource,
            mixpanelButton
        )
        dismissEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, ${method.stringValue()}")
    }

    fun onShareViaMessengerTapped(activity: Activity?) {
        val method = ShareMethod.Messenger
        shareManager.shareLink(
            activity,
            music,
            null,
            method,
            mixpanelSource,
            mixpanelButton
        )
        dismissEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, ${method.stringValue()}")
    }

    fun onShareViaWeChatTapped(activity: Activity?) {
        val method = ShareMethod.WeChat
        shareManager.shareLink(
            activity,
            music,
            null,
            method,
            mixpanelSource,
            mixpanelButton
        )
        dismissEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, ${method.stringValue()}")
    }

    fun onVisible() {
        startAnimationEvent.call()
    }

    fun onAddToPlaylistTapped() {
        compositeDisposable.add(
            actionsDataSource.addToPlaylist(music)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    addToPlaylistEvent.postValue(Triple(Collections.singletonList(music), mixpanelSource, mixpanelButton))
                }, {
                    when (it) {
                        is AddToPlaylistException.LoggedOut -> {
                            pendingActionAfterLogin = PendingActionAfterLogin.AddToPlaylist
                            loginRequiredEvent.postValue(LoginSignupSource.AddToPlaylist)
                        }
                    }
                })
                .also { compositeDisposable.add(it) }
        )
    }

    fun onCommentsClick() {
        openCommentsEvent.postValue(music)
        closeEvent.call()
    }

    // Downloads

    fun onDownloadTapped() {
        download()
    }

    fun onDeleteDownloadTapped() {
        download()
    }

    fun onPlaylistSyncConfirmed() {
        download()
        if (userDataSource.isLoggedIn() && !music.isUploadedByMyself(MainApplication.context) && !userDataSource.isMusicFavorited(music)) {
            onFavoriteTapped()
        }
    }

    private fun download() {
        actionsDataSource.toggleDownload(music, MixpanelButtonKebabMenu, mixpanelSource, skipFrozenCheck = !mixpanelSource.isInMyDownloads)
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
                        pendingActionAfterLogin = PendingActionAfterLogin.Download
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

    // Favorite

    fun onFavoriteTapped() {
        actionsDataSource.toggleFavorite(music, mixpanelButton, mixpanelSource)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                when (result) {
                    is ToggleFavoriteResult.Notify -> notifyFavoriteEvent.postValue(result)
                }
            }, { throwable ->
                when (throwable) {
                    is ToggleFavoriteException.LoggedOut -> {
                        pendingActionAfterLogin = PendingActionAfterLogin.Favorite
                        loginRequiredEvent.postValue(LoginSignupSource.Favorite)
                    }
                    is ToggleFavoriteException.Offline -> {
                        notifyOfflineEvent.call()
                    }
                    else -> {
                        val errorMsg = if (userDataSource.isMusicFavorited(music)) {
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

    private fun setFavoriteListener() {
        music.favoriteSubject.subscribe(object : MenuObserver<ItemAPIStatus>() {
            override fun onNext(status: ItemAPIStatus) {
                when (status) {
                    ItemAPIStatus.Loading -> {
                        _favoriteAction.postValue(Favorite(LOADING))
                    }
                    ItemAPIStatus.Off -> {
                        _favoriteAction.postValue(Favorite(DEFAULT))
                    }
                    ItemAPIStatus.On -> {
                        _favoriteAction.postValue(Favorite(ACTIVE))
                    }
                    ItemAPIStatus.Queued -> {}
                }
            }
        })
    }

    // Repost

    fun onRepostTapped() {
        actionsDataSource.toggleRepost(music, mixpanelButton, mixpanelSource)
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
                        val errorMsg = MainApplication.context?.getString(R.string.toast_reposted_song_error)
                        showHUDEvent.postValue(ProgressHUDMode.Failure(errorMsg ?: ""))
                    }
                }
            })
            .also { compositeDisposable.add(it) }
    }

    private fun setRePostListener() {
        music.repostSubject.subscribe(object : MenuObserver<ItemAPIStatus>() {
            override fun onNext(status: ItemAPIStatus) {
                when (status) {
                    ItemAPIStatus.Loading -> {
                        _rePostAction.postValue(RePost(LOADING))
                    }
                    ItemAPIStatus.Off -> {
                        _rePostAction.postValue(RePost(DEFAULT))
                    }
                    ItemAPIStatus.On -> {
                        _rePostAction.postValue(RePost(ACTIVE))
                    }
                    ItemAPIStatus.Queued -> {}
                }
            }
        })
    }

    private fun setCommentCountListener() {
        music.commentsCountSubject.subscribe(object : MenuObserver<Int>() {
            override fun onNext(count: Int) {
                _commentsCount.postValue(count)
            }
        })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventDownload: EventDownload) {
        if (eventDownload.itemId == music.itemId) {
            updateViewState()
            updateDownloadAction(eventDownload.itemId)
        }
    }

    private fun updateDownloadAction(downloadItemId: String?) {
        if (music.itemId == downloadItemId) {
            musicDataSource.getOfflineResource(music.itemId)
                .flatMap { resource ->
                    resource.data?.let { Observable.just(it) }
                        ?: Observable.error(RuntimeException())
                }
                .map { musicDownloadActionStateHelper.downloadState(music) }
                .observeOn(schedulersProvider.main)
                .subscribe({
                    _downloadAction.value = Download(it)
                }, {
                    _downloadAction.value = Download(DEFAULT)
                })
                .also { compositeDisposable.add(it) }
        }
    }

    // Entities

    sealed class PendingActionAfterLogin {
        object AddToPlaylist : PendingActionAfterLogin()
        object Repost : PendingActionAfterLogin()
        object Favorite : PendingActionAfterLogin()
        object Highlight : PendingActionAfterLogin()
        object Download : PendingActionAfterLogin()
    }

    // Utils

    abstract inner class MenuObserver<T> : Observer<T> {
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
        private const val TAG = "SlideUpMenuMusicViewMod"
    }
}
