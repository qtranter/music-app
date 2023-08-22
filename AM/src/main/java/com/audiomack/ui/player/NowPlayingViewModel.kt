package com.audiomack.ui.player

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.audiomack.data.device.DeviceDataSource
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.player.PlayerRepository
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.playback.NowPlayingVisibility
import com.audiomack.playback.NowPlayingVisibilityImpl
import com.audiomack.playback.Playback
import com.audiomack.playback.PlayerPlayback
import com.audiomack.playback.RepeatType
import com.audiomack.playback.ShuffleState
import com.audiomack.playback.ShuffleState.OFF
import com.audiomack.playback.ShuffleState.ON
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibility
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibilityImpl
import com.audiomack.ui.tooltip.TooltipFragment.TooltipLocation
import com.audiomack.utils.GeneralPreferences
import com.audiomack.utils.GeneralPreferencesImpl
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import timber.log.Timber

class NowPlayingViewModel(
    private val playback: Playback = PlayerPlayback.getInstance(),
    private val generalPreferences: GeneralPreferences = GeneralPreferencesImpl.getInstance(),
    private val queue: QueueDataSource = QueueRepository.getInstance(),
    private val playerDataSource: PlayerDataSource = PlayerRepository.getInstance(),
    private val nowPlayingVisibility: NowPlayingVisibility = NowPlayingVisibilityImpl,
    private val playerBottomVisibility: PlayerBottomVisibility = PlayerBottomVisibilityImpl.getInstance(),
    deviceDataSource: DeviceDataSource = DeviceRepository,
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance()
) : BaseViewModel() {

    // Activity events

    val itemLoadedEvent = SingleLiveEvent<AMResultItem>()
    val playerVisibilityChangeEvent = SingleLiveEvent<Boolean>()
    val launchEqEvent = SingleLiveEvent<Int>()
    val launchUpgradeEvent = SingleLiveEvent<InAppPurchaseMode>()

    // Fragment events

    val maximizeEvent = MutableLiveData<Boolean>()
    val onMinimizeEvent = SingleLiveEvent<Void>()
    val scrollToTopEvent = SingleLiveEvent<Void>()

    val bottomPageSelectedEvent = SingleLiveEvent<Int>()
    val bottomTabClickEvent = SingleLiveEvent<Int>()
    val bottomVisibilityChangeEvent = SingleLiveEvent<Boolean>()

    val requestScrollTooltipEvent = SingleLiveEvent<Void>()
    val requestEqTooltipEvent = SingleLiveEvent<Void>()
    val showScrollTooltipEvent = SingleLiveEvent<TooltipLocation>()
    val showEqTooltipEvent = SingleLiveEvent<TooltipLocation>()
    val onTooltipDismissEvent = SingleLiveEvent<Void>()

    val blockAdsEvent = SingleLiveEvent<Void>()

    private val _shuffle = MutableLiveData<ShuffleState>()
    val shuffle: LiveData<ShuffleState> get() = _shuffle

    private val _repeat = MutableLiveData<RepeatType>()
    val repeat: LiveData<RepeatType> get() = _repeat

    private val _equalizerEnabled = MutableLiveData(deviceDataSource.hasEqualizer())
    val equalizerEnabled: LiveData<Boolean> get() = _equalizerEnabled

    val isLocalMedia: LiveData<Boolean> = Transformations.map(itemLoadedEvent) { it.isLocal }

    var isMaximized: Boolean
        get() = nowPlayingVisibility.isMaximized
        set(value) {
            nowPlayingVisibility.isMaximized = value
            maximizeEvent.value = value
        }

    val isMaximizedAndShowingBottomTabs: Boolean
        get() = nowPlayingVisibility.isMaximized && playerBottomVisibility.tabsVisible

    private var pendingEqualizer = false

    @VisibleForTesting
    var itemLoaded: AMResultItem? = null

    @VisibleForTesting
    val shuffleObserver = object : NowPlayingObserver<Boolean>() {
        override fun onNext(enabled: Boolean) {
            _shuffle.postValue(
                if (enabled) ON else OFF
            )
        }
    }

    @VisibleForTesting
    val repeatObserver = object : NowPlayingObserver<RepeatType>() {
        override fun onNext(repeatType: RepeatType) {
            _repeat.value = repeatType
        }
    }

    @VisibleForTesting
    val queueItemObserver = object : NowPlayingObserver<AMResultItem>() {
        override fun onNext(item: AMResultItem) {
            playerDataSource.unloadSong(item)

            if (bottomVisibilityChangeEvent.value == true) {
                itemLoaded = item
                playerDataSource.loadSong(item)
            }

            itemLoadedEvent.postValue(item)
        }
    }

    private val premiumObserver = object : NowPlayingObserver<Boolean>() {
        override fun onNext(premium: Boolean) {
            if (premium) {
                resumePendingActions()
            } else {
                clearPendingActions()
            }
        }
    }

    init {
        Timber.tag(TAG).i("init() called")
        playback.repeatType.subscribe(repeatObserver)
        queue.subscribeToShuffle(shuffleObserver)
        queue.subscribeToCurrentItem(queueItemObserver)
        premiumDataSource.premiumObservable.subscribe(premiumObserver)
    }

    fun onShuffleClick() {
        val shuffle = !queue.shuffle
        queue.setShuffle(shuffle)
    }

    fun onRepeatClick() {
        playback.repeat()
    }

    fun onEqClick() {
        if (premiumDataSource.isPremium) {
            launchEqEvent.postValue(playback.audioSessionId)
        } else {
            pendingEqualizer = true
            launchUpgradeEvent.postValue(InAppPurchaseMode.Equalizer)
        }
    }

    fun onPlayerVisibilityChanged(visible: Boolean) {
        playerVisibilityChangeEvent.postValue(visible)
    }

    fun onBottomTabSelected(position: Int) {
        bottomTabClickEvent.postValue(position)
    }

    fun onBottomVisibilityChanged(visible: Boolean) {
        Timber.tag(TAG).i("onBottomVisibilityChanged(): visible = $visible")
        bottomVisibilityChangeEvent.postValue(visible)
        if (visible) {
            queue.currentItem?.let { item ->
                if (item != itemLoaded) {
                    itemLoaded = item
                    playerDataSource.loadSong(item)
                }
            }
        }
    }

    fun onTabsVisibilityChanged(visible: Boolean) {
        playerBottomVisibility.tabsVisible = visible
    }

    fun onScrollViewReachedBottomChange(reachedBottom: Boolean) {
        playerBottomVisibility.reachedBottom = reachedBottom
    }

    fun onMinimized() {
        onMinimizeEvent.call()
    }

    fun scrollToTop() {
        scrollToTopEvent.call()
    }

    fun showTooltip(): Boolean {
        if (generalPreferences.needToShowPlayerScrollTooltip()) {
            requestScrollTooltipEvent.call()
            return true
        } else if (generalPreferences.needToShowPlayerEqTooltip()) {
            requestEqTooltipEvent.call()
            return true
        }
        return false
    }

    fun setScrollTooltipLocation(location: TooltipLocation) {
        blockAdsEvent.call()
        showScrollTooltipEvent.value = location
    }

    fun setEqTooltipLocation(location: TooltipLocation) {
        blockAdsEvent.call()
        showEqTooltipEvent.value = location
    }

    fun onTooltipDismissed() {
        onTooltipDismissEvent.call()
    }

    fun onBottomPageSelected(index: Int) {
        bottomPageSelectedEvent.postValue(index)
        playerBottomVisibility.tabIndex = index
    }

    private fun resumePendingActions() {
        when {
            pendingEqualizer -> onEqClick()
        }
        clearPendingActions()
    }

    private fun clearPendingActions() {
        pendingEqualizer = false
    }

    abstract inner class NowPlayingObserver<T> : Observer<T> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            Timber.tag(TAG).e(e)
        }
    }

    companion object {
        private const val TAG = "NowPlayingViewModel"
    }
}
