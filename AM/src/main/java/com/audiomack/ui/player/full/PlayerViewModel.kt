package com.audiomack.ui.player.full

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.common.MusicDownloadActionStateHelper
import com.audiomack.common.MusicDownloadActionStateHelperImpl
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.AddToPlaylistException
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleRepostException
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.ads.ShowInterstitialResult
import com.audiomack.data.ads.ShowInterstitialResult.Dismissed
import com.audiomack.data.ads.ShowInterstitialResult.NotShown
import com.audiomack.data.ads.ShowInterstitialResult.Shown
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.player.PlayerRepository
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadRepository
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueException
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonNowPlaying
import com.audiomack.data.user.UserData
import com.audiomack.data.user.UserDataInterface
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItem.ItemAPIStatus
import com.audiomack.model.EventDownload
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.playback.ActionState.ACTIVE
import com.audiomack.playback.ActionState.DEFAULT
import com.audiomack.playback.ActionState.DISABLED
import com.audiomack.playback.ActionState.FROZEN
import com.audiomack.playback.ActionState.LOADING
import com.audiomack.playback.ActionState.QUEUED
import com.audiomack.playback.NowPlayingVisibility
import com.audiomack.playback.NowPlayingVisibilityImpl
import com.audiomack.playback.Playback
import com.audiomack.playback.PlaybackState
import com.audiomack.playback.PlayerError
import com.audiomack.playback.PlayerPlayback
import com.audiomack.playback.RepeatType
import com.audiomack.playback.SongAction
import com.audiomack.playback.fastForward
import com.audiomack.playback.rewind
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.common.Resource
import com.audiomack.ui.common.Resource.Failure
import com.audiomack.ui.common.Resource.Success
import com.audiomack.ui.tooltip.TooltipFragment.TooltipLocation
import com.audiomack.utils.Foreground
import com.audiomack.utils.ForegroundManager
import com.audiomack.utils.GeneralPreferences
import com.audiomack.utils.GeneralPreferencesImpl
import com.audiomack.utils.SimpleObserver
import com.audiomack.utils.SingleLiveEvent
import com.mopub.mobileads.MoPubView
import com.mopub.nativeads.AdapterHelper
import com.mopub.nativeads.NativeAd
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit.MILLISECONDS
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class PlayerViewModel(
    private val playback: Playback = PlayerPlayback.getInstance(),
    private val generalPreferences: GeneralPreferences = GeneralPreferencesImpl(),
    private val foreground: Foreground = ForegroundManager.get(),
    private val playerDataSource: PlayerDataSource = PlayerRepository.getInstance(),
    private val queue: QueueDataSource = QueueRepository.getInstance(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    private val eventBus: EventBus = EventBus.getDefault(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val userData: UserDataInterface = UserData,
    userDataSource: UserDataSource = UserRepository.getInstance(),
    private val nowPlayingVisibility: NowPlayingVisibility = NowPlayingVisibilityImpl,
    private val mixPanelButton: String = MixpanelButtonNowPlaying,
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    private val premiumDownloadDataSource: PremiumDownloadDataSource = PremiumDownloadRepository.getInstance(),
    private val musicDownloadActionStateHelper: MusicDownloadActionStateHelper = MusicDownloadActionStateHelperImpl(premiumDownloadDataSource, premiumDataSource)
) : BaseViewModel() {

    private val isPremium: Boolean
        get() = premiumDataSource.isPremium

    private val _parentTitle = MutableLiveData<String>()
    val parentTitle: LiveData<String> get() = _parentTitle

    private val _isHiFi = MutableLiveData<Boolean>()
    val isHiFi: LiveData<Boolean> get() = _isHiFi

    private val _favoriteAction = MutableLiveData<SongAction.Favorite>()
    val favoriteAction: LiveData<SongAction.Favorite> get() = _favoriteAction

    private val _addToPlaylistAction = MutableLiveData<SongAction.AddToPlaylist>()
    val addToPlaylistAction: LiveData<SongAction.AddToPlaylist> get() = _addToPlaylistAction

    private val _rePostAction = MutableLiveData<SongAction.RePost>()
    val rePostAction: LiveData<SongAction.RePost> get() = _rePostAction

    private val _downloadAction = MutableLiveData<SongAction.Download>()
    val downloadAction: LiveData<SongAction.Download> get() = _downloadAction

    private val _shareAction = MutableLiveData<SongAction.Share>()
    val shareAction: LiveData<SongAction.Share> get() = _shareAction

    private val _currentIndex = MutableLiveData<Int>()
    val currentIndex: LiveData<Int> get() = _currentIndex

    private val _currentPosition = MutableLiveData<Long>()
    val currentPosition: LiveData<Long> get() = _currentPosition

    private val _duration = MutableLiveData<Long>()
    val duration: LiveData<Long> get() = _duration

    private val _volumeData = MutableLiveData<IntArray>()
    val volumeData: LiveData<IntArray> get() = _volumeData

    private val _songList = MutableLiveData<List<AMResultItem>>()
    val songList: LiveData<List<AMResultItem>> get() = _songList

    private val _playbackState = MutableLiveData<PlaybackState>()
    val playbackState: LiveData<PlaybackState> get() = _playbackState

    private val _nextButtonEnabled = MutableLiveData<Boolean>()
    val nextButtonEnabled: LiveData<Boolean> get() = _nextButtonEnabled

    private val _repostVisible = MutableLiveData<Boolean>()
    val repostVisible: LiveData<Boolean> get() = _repostVisible

    private val _repeat = MutableLiveData<RepeatType>()
    val repeat: LiveData<RepeatType> get() = _repeat

    private val _showPodcastControls = MutableLiveData(false)
    val showPodcastControls: LiveData<Boolean> get() = _showPodcastControls

    private val _castEnabled = MutableLiveData(false)
    val castEnabled: LiveData<Boolean> get() = _castEnabled

    // Fragment Events

    val requestPlaylistTooltipEvent = SingleLiveEvent<Void>()
    val requestQueueTooltipEvent = SingleLiveEvent<Void>()

    val showPlaylistTooltipEvent = SingleLiveEvent<TooltipLocation>()
    val showQueueTooltipEvent = SingleLiveEvent<TooltipLocation>()

    val downloadClickEvent = SingleLiveEvent<AMResultItem>()

    val retryDownloadEvent = SingleLiveEvent<AMResultItem>()

    /**
     * Emits whether the View is in the foreground
     *
     * @see Foreground
     */
    val adClosedEvent = SingleLiveEvent<Boolean>()
    val showNativeAdEvent = SingleLiveEvent<Pair<NativeAd, AdapterHelper>>()
    val showAdEvent = SingleLiveEvent<MoPubView>()

    val errorEvent = SingleLiveEvent<PlayerError>()

    // Activity Events

    /**
     * Emits artist name
     */
    val searchArtistEvent = SingleLiveEvent<String>()

    /**
     * Emits artwork url
     */
    val showArtworkEvent = SingleLiveEvent<String>()

    /**
     * Emits premium status
     */
    val showInAppPurchaseEvent = SingleLiveEvent<Unit>()

    val playEvent = SingleLiveEvent<Void>()
    val minimizeEvent = SingleLiveEvent<Void>()
    val showQueueEvent = SingleLiveEvent<Void>()

    val removeAdsEvent = SingleLiveEvent<Void>()
    val adRefreshEvent = SingleLiveEvent<Boolean>()

    val loginRequiredEvent = SingleLiveEvent<LoginSignupSource>()
    val addToPlaylistEvent = SingleLiveEvent<Triple<List<AMResultItem>, MixpanelSource, String>>()
    val shareEvent = SingleLiveEvent<AMResultItem>()
    val openParentPlaylistEvent = SingleLiveEvent<Pair<String, MixpanelSource?>>()
    val openParentAlbumEvent = SingleLiveEvent<Pair<String, MixpanelSource?>>()
    val notifyOfflineEvent = SingleLiveEvent<Void>()
    val notifyRepostEvent = SingleLiveEvent<ToggleRepostResult.Notify>()
    val notifyFavoriteEvent = SingleLiveEvent<ToggleFavoriteResult.Notify>()
    val showConfirmDownloadDeletionEvent = SingleLiveEvent<AMResultItem>()
    val showPremiumDownloadEvent = SingleLiveEvent<PremiumDownloadModel>()
    val showUnlockedToastEvent = SingleLiveEvent<String>()

    var inOfflineScreen: Boolean = false

    @VisibleForTesting
    val playbackStateObserver = object : PlayerObserver<PlaybackState>() {
        override fun onNext(state: PlaybackState) {
            Timber.tag(TAG).d("playbackStateObserver onNext: ${getPlayerStateString(state)}")
            _playbackState.postValue(state)

            if (state == PlaybackState.PAUSED || state == PlaybackState.PLAYING) {
                _duration.postValue(playback.duration)
                _currentPosition.postValue(playback.position)
            }
        }
    }

    @VisibleForTesting
    val playbackTimerObserver = object : PlayerObserver<Long?>() {
        override fun onNext(position: Long) {
            _currentPosition.postValue(position)
        }
    }

    @VisibleForTesting
    val repeatObserver = object : PlayerObserver<RepeatType>() {
        override fun onNext(repeatType: RepeatType) {
            _repeat.value = repeatType
            _nextButtonEnabled.postValue(!queue.atEndOfQueue || repeatType == RepeatType.ALL)
        }
    }

    @VisibleForTesting
    val playbackErrorObserver = object : PlayerObserver<PlayerError>() {
        override fun onNext(error: PlayerError) {
            Timber.tag(TAG).e(error.throwable, "playbackErrorObserver onNext() called")
            errorEvent.postValue(error)
        }
    }

    @VisibleForTesting
    val queueListObserver = object : QueueObserver<List<AMResultItem>>() {
        override fun onNext(songs: List<AMResultItem>) {
            Timber.tag(TAG).d("queueListObserver onNext: ${songs.size} songs")
            _songList.postValue(songs)
        }
    }

    @VisibleForTesting
    val queueIndexObserver = object : QueueObserver<Int>() {
        // Called as soon as the queue index changes. This is not delayed by the debounce.
        override fun onNext(index: Int) {
            Timber.tag(TAG).d("queueIndexObserver onNext: $index")
            val queueItem = queue.currentItem
            if (queueItem != null && loadedItem?.itemId != queueItem.itemId) onSongChanged()
            _currentIndex.postValue(index)
            _currentPosition.postValue(0)
            _nextButtonEnabled.postValue(!queue.atEndOfQueue || _repeat.value == RepeatType.ALL)
        }
    }

    @VisibleForTesting
    val queueCurrentItemObserver = object : QueueObserver<AMResultItem>() {
        // Called after a debounce period with the item at the current queue index.
        override fun onNext(song: AMResultItem) {
            Timber.tag(TAG).d("queueCurrentItemObserver onNext: $song")
            _parentTitle.postValue(song.playlist ?: song.album)
            _repostVisible.postValue(!song.isUploadedByMyself(null))
            _volumeData.postValue(song.volumeData ?: intArrayOf())
            onSongLoaded(song)
            closeAd()
        }
    }

    @VisibleForTesting
    val adTimerObserver = object : PlayerObserver<Long>() {
        override fun onNext(position: Long) {
            Timber.tag(TAG).d("adTimerObserver onNext() called: position = $position")
            if (playback.songSkippedManually) {
                adsDataSource.showInterstitial()
            } else {
                refreshPlayerAd(true)
            }
        }
    }

    @VisibleForTesting
    val dataCurrentFullItemObserver = object : PlayerObserver<Resource<AMResultItem>>() {
        override fun onNext(resource: Resource<AMResultItem>) {
            Timber.tag(TAG).d("dataCurrentFullItemObserver onNext: $resource")
            when (resource) {
                is Success -> resource.data?.let { onSongLoaded(it) }
                is Failure -> {
                    if (resource.data?.id == null) {
                        Timber.tag(TAG).w(resource.error, "Skipping bad song from getStreamURL")
                        playback.next()
                    }
                }
            }
        }
    }

    @VisibleForTesting
    val foregroundListener = object : Foreground.Listener {
        override fun onBecameForeground() {
            refreshPlayerAd(true)
        }

        override fun onBecameBackground() {
            closeAd()
        }
    }

    @VisibleForTesting
    val downloadRequestObserver = object : Observer<AMResultItem> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onNext(item: AMResultItem) {
            retryDownloadEvent.postValue(item)
        }

        override fun onError(e: Throwable) {
            errorEvent.postValue(PlayerError.Resource(e))
        }
    }

    private val premiumObserver = object : PlayerObserver<Boolean>() {
        override fun onNext(premium: Boolean) {
            currentItem?.let { song ->
                _isHiFi.postValue(premium && !song.isLocal)
                _downloadAction.postValue(
                    SongAction.Download(musicDownloadActionStateHelper.downloadState(song, premium))
                )
            }
        }
    }

    private val interstitialObserver = object : PlayerObserver<ShowInterstitialResult>() {
        override fun onNext(result: ShowInterstitialResult) {
            Timber.tag(TAG).i("interstitialObserver observed: $result")
            when (result) {
                is Dismissed -> playback.play()
                is Shown -> skipNextPlayerAd = true
                is NotShown -> {
                    skipNextPlayerAd = false
                    refreshPlayerAd(true)
                }
            }
        }
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        when (state) {
            EventLoginState.LOGGED_IN -> pendingActionAfterLogin?.let {
                when (it) {
                    is PendingActionAfterLogin.AddToPlaylist -> onAddToPlaylistClick()
                    is PendingActionAfterLogin.Repost -> onRePostClick()
                    is PendingActionAfterLogin.Favorite -> onFavoriteClick()
                    is PendingActionAfterLogin.Download -> startDownload(it.song, it.mixpanelButton, it.retry)
                }
                pendingActionAfterLogin = null
            }
            else -> pendingActionAfterLogin = null
        }
    }

    private var pendingActionAfterLogin: PendingActionAfterLogin? = null

    /**
     * The queue does not hold full items, so first check the repo
     */
    val currentItem: AMResultItem?
        get() = playerDataSource.currentSong?.takeIf { it.itemId == queue.currentItem?.itemId }
            ?: queue.currentItem

    /**
     * When loading this and [currentItem] may be different. This allows us to avoid re-binding data to views.
     */
    private var loadedItem: AMResultItem? = null

    @VisibleForTesting
    var skipNextPlayerAd: Boolean = false

    init {
        eventBus.register(this)
        subscribePlaybackObservers()
        subscribeQueueObservers()
        playerDataSource.subscribeToSong(dataCurrentFullItemObserver)
        foreground.addListener(foregroundListener)
        premiumDataSource.premiumObservable.subscribe(premiumObserver)
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        adsDataSource.interstitialObservable
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe(interstitialObserver)
    }

    override fun onCleared() {
        eventBus.unregister(this)
        super.onCleared()
    }

    private fun subscribePlaybackObservers() {
        playback.apply {
            state.observable
                .distinctUntilChanged()
                .debounce(250L, MILLISECONDS)
                .observeOn(schedulersProvider.main)
                .subscribe(playbackStateObserver)

            timer
                .observeOn(schedulersProvider.main)
                .subscribe(playbackTimerObserver)

            error
                .observeOn(schedulersProvider.main)
                .subscribe(playbackErrorObserver)

            adTimer
                .observeOn(schedulersProvider.main)
                .subscribe(adTimerObserver)

            downloadRequest
                .observeOn(schedulersProvider.main)
                .subscribe(downloadRequestObserver)

            repeatType.subscribe(repeatObserver)
        }
    }

    private fun subscribeQueueObservers() {
        queue.apply {
            subscribeToOrderedList(queueListObserver)
            subscribeToIndex(queueIndexObserver)
            subscribeToCurrentItem(queueCurrentItemObserver)
        }
    }

    fun onPlayPauseClick() {
        if (currentItem == null) {
            queue.restoreBookmarks()
        }

        when (playbackState.value) {
            PlaybackState.PLAYING -> playback.pause()
            else -> {
                adsDataSource.showInterstitial()
                playback.play()
                playEvent.call()
                refreshPlayerAd(true)
            }
        }
    }

    fun onTouchSeek(progress: Int) {
        val position = progress.toLong()
        playback.seekTo(position)
        _currentPosition.postValue(position)
    }

    fun onTrackSelected(index: Int) {
        if (index == currentIndex.value) return
        playback.skip(index)
    }

    fun onSkipBackClick() {
        currentItem?.let {
            if (it.isPodcast) {
                playback.rewind()
            } else {
                playback.prev()
            }
        }
    }

    fun onSkipForwardClick() {
        currentItem?.let {
            if (it.isPodcast) {
                playback.fastForward()
            } else {
                playback.next()
            }
        }
    }

    fun restart() {
        playback.seekTo(0)
    }

    fun onMinimizeClick() {
        minimizeEvent.call()
    }

    fun onQueueClick() {
        showQueueEvent.call()
    }

    fun onHiFiClick() {
        if (!isPremium) showInAppPurchaseEvent.call()
    }

    fun isFavorited() = currentItem?.let { userData.isItemFavorited(it) } ?: false

    fun onFavoriteClick() {
        currentItem?.let { item ->
            actionsDataSource.toggleFavorite(item, MixpanelButtonNowPlaying, item.mixpanelSource ?: MixpanelSource.empty)
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
                            errorEvent.postValue(PlayerError.Action(throwable))
                        }
                    }
                })
        }
    }

    fun onAddToPlaylistClick() {
        currentItem?.let { item ->
            compositeDisposable.add(
                actionsDataSource.addToPlaylist(item)
                    .subscribeOn(schedulersProvider.io)
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        val mixpanelSource = item.mixpanelSource ?: MixpanelSource.empty
                        addToPlaylistEvent.postValue(
                            Triple(listOf(item), mixpanelSource, mixPanelButton)
                        )
                    }, { throwable ->
                        if (throwable is AddToPlaylistException.LoggedOut) {
                            pendingActionAfterLogin = PendingActionAfterLogin.AddToPlaylist
                            loginRequiredEvent.postValue(LoginSignupSource.AddToPlaylist)
                        } else {
                            errorEvent.postValue(PlayerError.Action(throwable))
                        }
                    })
            )
        }
    }

    fun onRePostClick() {
        currentItem?.let { item ->
            compositeDisposable.add(
                actionsDataSource.toggleRepost(item, MixpanelButtonNowPlaying, item.mixpanelSource ?: MixpanelSource.empty)
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
                                errorEvent.postValue(PlayerError.Action(throwable))
                            }
                        }
                    })
            )
        }
    }

    fun onDownloadClick() {
        currentItem?.let {
            downloadClickEvent.postValue(it)
        }
    }

    fun onShareClick() {
        currentItem?.let {
            shareEvent.postValue(it)
        }
    }

    fun onArtistClick(artist: String) {
        searchArtistEvent.postValue(artist)
    }

    fun onArtworkClick(url: String) {
        showArtworkEvent.postValue(url)
    }

    fun showTooltip(): Boolean {
        if (generalPreferences.needToShowPlayerPlaylistTooltip()) {
            requestPlaylistTooltipEvent.call()
            return true
        } else if (generalPreferences.needToShowPlayerQueueTooltip()) {
            requestQueueTooltipEvent.call()
            return true
        }
        return false
    }

    fun setPlaylistTooltipLocation(location: TooltipLocation) {
        blockAds()
        showPlaylistTooltipEvent.value = location
    }

    fun setQueueTooltipLocation(location: TooltipLocation) {
        blockAds()
        showQueueTooltipEvent.value = location
    }

    fun showAd(mopubNativeAd: NativeAd, nativeAdsAdapterHelper: AdapterHelper) {
        showNativeAdEvent.value = Pair(mopubNativeAd, nativeAdsAdapterHelper)
        adsDataSource.preloadNativeAd()
    }

    fun showAd(mopubAdView: MoPubView) {
        showAdEvent.value = mopubAdView
    }

    fun refreshPlayerAd(showWhenReady: Boolean) {
        Timber.tag(TAG).i("refreshPlayerAd() called: showWhenReady = $showWhenReady")
        if (queue.currentItem != null && adsDataSource.adsVisible) {
            closeAd()
            if (nowPlayingVisibility.isMaximized && foreground.isForeground && !skipNextPlayerAd) {
                adRefreshEvent.value = showWhenReady
            }
            skipNextPlayerAd = false
        }
    }

    fun onRemoveAdsClick() {
        removeAdsEvent.call()
    }

    fun onCloseAdClick() {
        closeAd()
    }

    private fun closeAd() {
        Timber.tag(TAG).i("closeAd() called")
        val isForeground = foreground.isForeground
        adClosedEvent.value = isForeground
        if (isForeground) {
            adsDataSource.apply {
                resetMopub300x250Ad()
            }
        }
    }

    fun blockAds() {
        Timber.tag(TAG).i("blockAds() called")
        closeAd()
        skipNextPlayerAd = true
    }

    private fun onSongChanged() {
        _favoriteAction.postValue(SongAction.Favorite(DISABLED))
        _addToPlaylistAction.postValue(SongAction.AddToPlaylist(DISABLED))
        _rePostAction.postValue(SongAction.RePost(DISABLED))
        _downloadAction.postValue(SongAction.Download(DISABLED))
        _shareAction.postValue(SongAction.Share(DISABLED))
        _castEnabled.postValue(false)
        _isHiFi.postValue(false)
    }

    private fun onSongLoaded(song: AMResultItem) {
        loadedItem = song

        _castEnabled.postValue(!song.isLocal)
        _isHiFi.postValue(isPremium && !song.isLocal)

        if (song.isLocal) return

        setFavoriteListener(song)
        setRePostListener(song)

        _favoriteAction.postValue(
            SongAction.Favorite(if (userData.isItemFavorited(song)) ACTIVE else DEFAULT)
        )
        _addToPlaylistAction.postValue(SongAction.AddToPlaylist(DEFAULT))
        _rePostAction.postValue(
            SongAction.RePost(if (userData.isItemReuped(song.itemId)) ACTIVE else DEFAULT)
        )
        _downloadAction.postValue(SongAction.Download(musicDownloadActionStateHelper.downloadState(song, isPremium)))
        _shareAction.postValue(SongAction.Share(DEFAULT))

        _showPodcastControls.postValue(song.isPodcast)
    }

    private fun setFavoriteListener(song: AMResultItem) {
        song.favoriteSubject.subscribe(object : PlayerObserver<ItemAPIStatus>() {
            override fun onNext(status: ItemAPIStatus) {
                when (status) {
                    ItemAPIStatus.Loading -> {
                        _favoriteAction.postValue(SongAction.Favorite(LOADING))
                    }
                    ItemAPIStatus.Off -> {
                        _favoriteAction.postValue(SongAction.Favorite(DEFAULT))
                    }
                    ItemAPIStatus.On -> {
                        _favoriteAction.postValue(SongAction.Favorite(ACTIVE))
                    }
                    ItemAPIStatus.Queued -> {}
                }
            }
        })
    }

    private fun setRePostListener(song: AMResultItem) {
        song.repostSubject.subscribe(object : PlayerObserver<ItemAPIStatus>() {
            override fun onNext(status: ItemAPIStatus) {
                when (status) {
                    ItemAPIStatus.Loading -> {
                        _rePostAction.postValue(SongAction.RePost(LOADING))
                    }
                    ItemAPIStatus.Off -> {
                        _rePostAction.postValue(SongAction.RePost(DEFAULT))
                    }
                    ItemAPIStatus.On -> {
                        _rePostAction.postValue(SongAction.RePost(ACTIVE))
                    }
                    ItemAPIStatus.Queued -> {}
                }
            }
        })
    }

    fun onParentClick() {
        queue.currentItem?.let { currentItem ->
            if (currentItem.isLocal) return

            currentItem.parentId?.let { parentId ->
                if (parentId.isNotBlank()) {
                    if (currentItem.isAlbumTrack) {
                        openParentAlbumEvent.postValue(Pair(parentId, currentItem.mixpanelSource))
                    } else {
                        openParentPlaylistEvent.postValue(Pair(parentId, currentItem.mixpanelSource))
                    }
                }
            }
        }
    }

    fun onMinimized() {
        closeAd()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(downloadEvent: EventDownload) {
        currentItem?.itemId?.let { currentId ->
            if (currentId == downloadEvent.itemId) {
                playerDataSource.dbFindById(currentId)
                    .flatMap { resource ->
                        resource.data?.let { Observable.just(it) }
                            ?: Observable.error(RuntimeException())
                    }
                    .map {
                        when {
                            premiumDownloadDataSource.getFrozenCount(it) > 0 -> FROZEN.apply {
                                frozenDownloadsCount = premiumDownloadDataSource.getFrozenCount(it)
                                frozenDownloadsTotal = it.tracks?.size ?: 1
                            }
                            it.isDownloadCompletedIndependentlyFromType -> ACTIVE.apply {
                                downloadType = it.downloadType
                                isPremium = this@PlayerViewModel.isPremium
                            }
                            it.isDownloadInProgress -> LOADING
                            it.isDownloadQueued -> QUEUED
                            else -> DEFAULT.apply {
                                downloadType = it.downloadType
                                isPremium = this@PlayerViewModel.isPremium
                            }
                        }
                    }
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        _downloadAction.value = SongAction.Download(it)
                    }, {
                        _downloadAction.value = SongAction.Download(DEFAULT)
                    })
                    .also { compositeDisposable.add(it) }
            }
        }
    }

    /**
     * Observer that adds disposables to the ViewModel composite, and posts error events
     */
    abstract inner class PlayerObserver<T> : SimpleObserver<T>(compositeDisposable) {
        override fun onError(e: Throwable) {
            Timber.tag(TAG).e(e)
            errorEvent.postValue(PlayerError.Playback(e))
        }
    }

    abstract inner class QueueObserver<T> : SimpleObserver<T>(compositeDisposable) {
        override fun onError(e: Throwable) {
            Timber.tag(TAG).e(e)
            val queueException = QueueException(e)
            trackingDataSource.trackException(queueException)
            throw queueException
        }
    }

    private fun getPlayerStateString(playbackState: PlaybackState?) = when (playbackState) {
        PlaybackState.IDLE -> "IDLE"
        PlaybackState.LOADING -> "LOADING"
        PlaybackState.PLAYING -> "PLAYING"
        PlaybackState.PAUSED -> "PAUSED"
        PlaybackState.ENDED -> "ENDED"
        PlaybackState.ERROR -> "ERROR"
        else -> ""
    }

    fun startDownload(item: AMResultItem, mixpanelButton: String, retry: Boolean) {
        actionsDataSource.toggleDownload(item, mixpanelButton, item.mixpanelSource ?: MixpanelSource.empty, retry, skipFrozenCheck = !(item.mixpanelSource?.isInMyDownloads ?: false))
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                when (result) {
                    ToggleDownloadResult.ConfirmMusicDeletion -> showConfirmDownloadDeletionEvent.postValue(item)
                    is ToggleDownloadResult.ShowUnlockedToast -> showUnlockedToastEvent.postValue(result.musicName)
                }
            }, { throwable ->
                when (throwable) {
                    is ToggleDownloadException.LoggedOut -> {
                        pendingActionAfterLogin =
                            PendingActionAfterLogin.Download(item, mixpanelButton, retry)
                        loginRequiredEvent.postValue(throwable.source)
                    }
                    is ToggleDownloadException.ShowPremiumDownload -> showPremiumDownloadEvent.postValue(throwable.model)
                }
            })
            .also { compositeDisposable.add(it) }
    }

    // Entities

    sealed class PendingActionAfterLogin {
        object AddToPlaylist : PendingActionAfterLogin()
        object Repost : PendingActionAfterLogin()
        object Favorite : PendingActionAfterLogin()
        data class Download(val song: AMResultItem, val mixpanelButton: String, val retry: Boolean) : PendingActionAfterLogin()
    }

    // Static

    companion object {
        private const val TAG = "PlayerViewModel"
    }
}
