package com.audiomack.ui.onboarding.playlist

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.music.MusicManager
import com.audiomack.data.music.MusicManagerImpl
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.data.tracking.mixpanel.MixpanelButtonPlaylistDetails
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventLoginState
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.playback.ActionState
import com.audiomack.playback.Playback
import com.audiomack.playback.PlaybackItem
import com.audiomack.playback.PlayerPlayback
import com.audiomack.playback.SongAction
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.playlist.details.PlaylistTracksAdapter
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import com.audiomack.views.AMRecyclerView
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import kotlin.math.max
import timber.log.Timber

class PlaylistOnboardingViewModel(
    private val artistImage: String,
    private val playlist: AMResultItem,
    private val mixpanelSource: MixpanelSource,
    val imageLoader: ImageLoader = PicassoImageLoader,
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val musicManager: MusicManager = MusicManagerImpl(),
    private val queueDataSource: QueueDataSource = QueueRepository.getInstance(),
    private val playerPlayback: Playback = PlayerPlayback.getInstance(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository()
) : BaseViewModel(), PlaylistTracksAdapter.Listener, AMRecyclerView.ScrollListener {

    val backEvent = SingleLiveEvent<Void>()
    val shuffleEvent = SingleLiveEvent<Void>()
    val openTrackEvent = SingleLiveEvent<Triple<AMResultItem, AMResultItem?, Int>>()
    val openTrackOptionsEvent = SingleLiveEvent<AMResultItem>()
    val openTrackOptionsFailedDownloadEvent = SingleLiveEvent<AMResultItem>()
    val cleanupEvent = SingleLiveEvent<Void>()
    val scrollEvent = SingleLiveEvent<Void>()
    val openUploaderEvent = SingleLiveEvent<String>()
    // Emits a boolean stating if the playlist is currently being played and the player is in playing state
    val updatePlayEvent = SingleLiveEvent<Boolean>()
    val updateListEvent = SingleLiveEvent<Void>()
    val updateDetailsEvent = SingleLiveEvent<Void>()
    val updateTrackEvent = SingleLiveEvent<String>()
    val notifyFavoriteEvent = SingleLiveEvent<ToggleFavoriteResult.Notify>()
    val notifyOfflineEvent = SingleLiveEvent<Void>()
    val loginRequiredEvent = SingleLiveEvent<LoginSignupSource>()
    val premiumRequiredEvent = SingleLiveEvent<InAppPurchaseMode>()
    val showHUDEvent = SingleLiveEvent<ProgressHUDMode>()
    val showConfirmDownloadDeletionEvent = SingleLiveEvent<AMResultItem>()
    val showPremiumDownloadEvent = SingleLiveEvent<PremiumDownloadModel>()
    val showUnlockedToastEvent = SingleLiveEvent<String>()

    private val _favoriteAction = MutableLiveData<SongAction.Favorite>()
    val favoriteAction: LiveData<SongAction.Favorite> get() = _favoriteAction

    private var recyclerviewConfigured = false

    private var pendingActionAfterLogin: PendingActionAfterLogin? = null

    val tracks: List<AMResultItem>
        get() = playlist.tracks ?: emptyList()

    val artistPicture: String
        get() = artistImage

    val title: String
        get() = playlist.title ?: ""

    val uploaderName: String
        get() = playlist.uploaderName ?: ""

    val uploaderVerified: Boolean
        get() = playlist.isUploaderVerified

    val uploaderTastemaker: Boolean
        get() = playlist.isUploaderTastemaker

    val uploaderAuthenticated: Boolean
        get() = playlist.isUploaderAuthenticated

    val adsVisible: Boolean
        get() = adsDataSource.adsVisible

    val isPlaylistFavorited: Boolean
        get() = userDataSource.isMusicFavorited(playlist)

    @VisibleForTesting
    val playbackItemObserver = object : PlaylistObserver<PlaybackItem>() {
        override fun onNext(t: PlaybackItem) {
            callUpdatePlayEvent()
            updateListEvent.call()
        }
    }

    init {
        // Actions
        _favoriteAction.postValue(
            SongAction.Favorite(if (isPlaylistFavorited) ActionState.ACTIVE else ActionState.DEFAULT)
        )

        // Subscriptions
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        setFavoriteListener()
        subscribeToPlayback()
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        when (state) {
            EventLoginState.LOGGED_IN -> pendingActionAfterLogin?.let {
                when (it) {
                    is PendingActionAfterLogin.Favorite -> onFavoriteTapped()
                }
                pendingActionAfterLogin = null
            }
            else -> pendingActionAfterLogin = null
        }
    }

    private fun subscribeToPlayback() {
        playerPlayback.apply {
            item
                .distinctUntilChanged()
                .observeOn(schedulersProvider.main)
                .subscribe(playbackItemObserver)
        }
    }

    fun onBackTapped() {
        backEvent.call()
    }

    fun onPlayAllTapped() {
        if (queueDataSource.isCurrentItemOrParent(playlist)) {
            playerPlayback.apply { if (isPlaying) pause() else play() }
        } else {
            tracks.firstOrNull()?.let { onTrackTapped(it) }
        }
    }

    fun onShuffleTapped() {
        shuffleEvent.call()
    }

    fun onDestroy() {
        cleanupEvent.call()
    }

    fun onTrackDownloadTapped(track: AMResultItem, mixpanelButton: String) {
        download(track, mixpanelButton)
    }

    private fun download(music: AMResultItem, mixpanelButton: String) {
        actionsDataSource.toggleDownload(music, mixpanelButton, mixpanelSource)
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

    // PlaylistPlaylistTracksAdapter.Listener

    override fun onTrackTapped(track: AMResultItem) {
        musicManager.isDownloadFailed(track)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ failed ->
                if (failed) {
                    openTrackOptionsFailedDownloadEvent.postValue(track)
                } else {
                    val index = max(0, tracks.indexOfFirst { it.itemId == track.itemId })
                    openTrackEvent.postValue(Triple(track, playlist, index))
                }
            }, {})
            .addTo(compositeDisposable)
    }

    override fun onTrackActionsTapped(track: AMResultItem) {
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
            .addTo(compositeDisposable)
    }

    override fun onTrackDownloadTapped(track: AMResultItem) {
        download(track, MixpanelButtonList)
    }

    override fun onTrackFavoriteTapped(track: AMResultItem) {
        // Nothing to do here
    }

    override fun onCommentsTapped() {
        // Nothing to do here
    }

    // AMRecyclerView.ScrollListener

    override fun onScroll() {
        scrollEvent.call()
    }

    fun onRecyclerViewConfigured() {
        recyclerviewConfigured = true
        scrollEvent.call()
    }

    fun onUploaderTapped() {
        playlist.uploaderSlug?.let {
            openUploaderEvent.postValue(it)
        }
    }

    fun onCreate() {
        updateDetailsEvent.call()
        callUpdatePlayEvent()
    }

    fun onDownloadStatusChanged(itemId: String?) {
        if (itemId == null) return
        if (tracks.any { it.itemId == itemId }) {
            updateTrackEvent.postValue(itemId)
        }
    }

    fun onPlayPauseChanged() {
        callUpdatePlayEvent()
    }

    private fun callUpdatePlayEvent() {
        updatePlayEvent.postValue(queueDataSource.isCurrentItemOrParent(playlist) && playerPlayback.isPlaying)
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
                        pendingActionAfterLogin = PendingActionAfterLogin.Favorite
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
                        _favoriteAction.postValue(SongAction.Favorite(ActionState.LOADING))
                    }
                    AMResultItem.ItemAPIStatus.Off -> {
                        _favoriteAction.postValue(SongAction.Favorite(ActionState.DEFAULT))
                    }
                    AMResultItem.ItemAPIStatus.On -> {
                        _favoriteAction.postValue(SongAction.Favorite(ActionState.ACTIVE))
                    }
                    AMResultItem.ItemAPIStatus.Queued -> {}
                }
            }
        })
    }

    // Entities

    sealed class PendingActionAfterLogin {
        object Favorite : PendingActionAfterLogin()
        data class Download(val music: AMResultItem, val mixpanelButton: String) : PendingActionAfterLogin()
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

    // Static

    companion object {
        private const val TAG = "PlaylistOnboardingVM"
    }
}
