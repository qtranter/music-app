package com.audiomack.ui.feed

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.MainApplication
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.api.ArtistsDataSource
import com.audiomack.data.api.ArtistsRepository
import com.audiomack.data.feed.FeedDataSource
import com.audiomack.data.feed.FeedRepository
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonKebabMenu
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.data.tracking.mixpanel.MixpanelFilterReup
import com.audiomack.data.tracking.mixpanel.MixpanelFilterReup_Exclude
import com.audiomack.data.tracking.mixpanel.MixpanelFilterReup_Include
import com.audiomack.data.tracking.mixpanel.MixpanelPageFeedSuggestedFollows
import com.audiomack.data.tracking.mixpanel.MixpanelPageFeedTimeline
import com.audiomack.data.tracking.mixpanel.MixpanelTabFeed
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.APIResponseData
import com.audiomack.model.EventDownload
import com.audiomack.model.EventLoginState
import com.audiomack.model.EventSongChange
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.OpenMusicData
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.data.DataViewModel
import com.audiomack.usecases.FetchSuggestedAccountsUseCase
import com.audiomack.usecases.FetchSuggestedAccountsUseCaseImpl
import com.audiomack.usecases.download.DownloadEvents
import com.audiomack.usecases.download.DownloadEventsManager
import com.audiomack.usecases.download.DownloadUseCase
import com.audiomack.usecases.download.DownloadUseCaseImpl
import com.audiomack.usecases.favorite.FavoriteEvents
import com.audiomack.usecases.favorite.FavoriteEventsManager
import com.audiomack.usecases.favorite.FavoriteUseCase
import com.audiomack.usecases.favorite.FavoriteUseCaseImpl
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.Completable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class FeedViewModel(
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val fetchSuggestedAccountsUseCase: FetchSuggestedAccountsUseCase = FetchSuggestedAccountsUseCaseImpl(),
    private val feedDataSource: FeedDataSource = FeedRepository(),
    private val preferencesDataSource: PreferencesDataSource = PreferencesRepository(),
    private val artistsDataSource: ArtistsDataSource = ArtistsRepository(),
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    private val queueDataSource: QueueDataSource = QueueRepository.getInstance(),
    private val downloadUseCase: DownloadUseCase = DownloadUseCaseImpl(),
    private val downloadEventsManager: DownloadEventsManager = DownloadEventsManager,
    private val favoritesEventsManager: FavoriteEventsManager = FavoriteEventsManager,
    private val favoriteUseCase: FavoriteUseCase = FavoriteUseCaseImpl(),
    private val eventBus: EventBus = EventBus.getDefault()
) : BaseViewModel(), DownloadEvents by downloadEventsManager,
    FavoriteEvents by favoritesEventsManager {

    val adsVisible: Boolean get() = adsDataSource.adsVisible
    val promptNotificationPermissionEvent = SingleLiveEvent<PermissionRedirect>()
    val offlineAlertEvent = SingleLiveEvent<Unit>()
    val loggedOutAlertEvent = SingleLiveEvent<LoginSignupSource>()
    val optionsFragmentEvent = SingleLiveEvent<AMResultItem>()
    val openMusicEvent = SingleLiveEvent<OpenMusicData>()
    val updateUIEvent = SingleLiveEvent<AMArtist>()
    val feedPlaceHolderVisibilityEvent = SingleLiveEvent<Boolean>()
    val reloadFeedEvent = SingleLiveEvent<Unit>()
    val downloadItemEvent = SingleLiveEvent<String>()
    val songChangeEvent = SingleLiveEvent<String?>()
    val notifyFollowToastEvent = SingleLiveEvent<ToggleFollowResult.Notify>()

    private val _suggestedAccounts = MutableLiveData<List<AMArtist>>()
    val suggestedAccounts: LiveData<List<AMArtist>> = _suggestedAccounts

    private val _feedItems = MutableLiveData<List<AMResultItem>>()
    val feedItems: LiveData<List<AMResultItem>> = _feedItems

    private val accountsMixPanelSource = MixpanelSource(MixpanelTabFeed, MixpanelPageFeedSuggestedFollows)
    val feedMixPanelSource = MixpanelSource(
        MixpanelTabFeed, MixpanelPageFeedTimeline, listOf(
            Pair(
                MixpanelFilterReup,
                if (preferencesDataSource.excludeReUps) MixpanelFilterReup_Exclude else MixpanelFilterReup_Include
            )
        )
    )
    private var pendingActionAfterLogin: DataViewModel.PendingActionAfterLogin? = null
    private val allFeedItems: MutableList<AMResultItem> = mutableListOf()
    private var currentPage = 0
    private var currentUrl: String? = null
    var hasMoreSuggestedAccounts = true
        private set

    @VisibleForTesting
    var currentFeedPage = 0
    val isLoggedIn get() = userDataSource.isLoggedIn()
    var excludeReUps
        get() = preferencesDataSource.excludeReUps
        set(value) {
            if (preferencesDataSource.excludeReUps != value) {
                preferencesDataSource.excludeReUps = value
                reloadFeed()
            }
        }

    init {
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        userDataSource.artistFollowEvents
            .filter { it.followed }
            .subscribe(onNextObserver(onNext = { onArtistFollowed() }))
        loadMoreSuggestedAccounts()
        loadMoreFeedItems()
        eventBus.register(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventDownload: EventDownload) {
        downloadItemEvent.value = eventDownload.itemId
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventSongChange: EventSongChange?) {
        songChangeEvent.value = queueDataSource.currentItem?.itemId
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        when (state) {
            EventLoginState.LOGGED_IN -> pendingActionAfterLogin?.let {
                when (it) {
                    is DataViewModel.PendingActionAfterLogin.Download ->
                        download(it.music)
                    is DataViewModel.PendingActionAfterLogin.Follow ->
                        it.artist?.let { artist -> onFollowTapped(artist) }
                    is DataViewModel.PendingActionAfterLogin.Favorite ->
                        onFavoriteTapped(it.music)
                }
                reloadItems()
                pendingActionAfterLogin = null
            }
            else -> pendingActionAfterLogin = null
        }
    }

    private fun onArtistFollowed() {
        updateSuggestedAccountsAfterFollowTapped()
        Completable.timer(5, TimeUnit.SECONDS)
            .subscribeOn(schedulersProvider.interval)
            .observeOn(schedulersProvider.main)
            .subscribe { reloadFeed() }
            .composite()
    }

    private fun reloadItems() {
        currentPage = 0
        loadMoreSuggestedAccounts()
        reloadFeed()
    }

    fun onFollowTapped(artist: AMArtist) {
        actionsDataSource.toggleFollow(null, artist, MixpanelButtonList, accountsMixPanelSource)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                when (it) {
                    is ToggleFollowResult.Finished -> if (it.followed) updateSuggestedAccountsAfterFollowTapped()
                    is ToggleFollowResult.Notify -> notifyFollowToastEvent.postValue(it)
                    is ToggleFollowResult.AskForPermission -> promptNotificationPermissionEvent.postValue(
                        it.redirect
                    )
                }
            }, {
                when (it) {
                    is ToggleFollowException.LoggedOut -> {
                        pendingActionAfterLogin = DataViewModel.PendingActionAfterLogin.Follow(
                            null,
                            artist,
                            accountsMixPanelSource,
                            MixpanelButtonList
                        )
                        loggedOutAlertEvent.postValue(LoginSignupSource.AccountFollow)
                    }
                    is ToggleFollowException.Offline -> offlineAlertEvent.call()
                }
            }).composite()
    }

    private fun updateSuggestedAccountsAfterFollowTapped() {
        _suggestedAccounts.value =
            _suggestedAccounts.value?.filterNot { userDataSource.isArtistFollowed(it.artistId) }
                ?.toList()
    }

    fun loadMoreSuggestedAccounts() {
        fetchSuggestedAccountsUseCase(currentPage)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ onSuggestedAccountsNext(it) }, { Timber.tag(TAG).e(it) })
            .composite()
    }

    private fun onSuggestedAccountsNext(artists: List<AMArtist>) {
        currentPage++
        val unFollowedArtists = artists.filterNot { userDataSource.isArtistFollowed(it.artistId) }
        if (unFollowedArtists.isEmpty() && artists.isNotEmpty()) {
            loadMoreSuggestedAccounts()
        } else {
            hasMoreSuggestedAccounts = unFollowedArtists.isNotEmpty()

            _suggestedAccounts.value?.let {
                _suggestedAccounts.value = it.toMutableList().apply {
                    addAll(unFollowedArtists)
                    toList()
                }
            } ?: let {
                _suggestedAccounts.value = unFollowedArtists
            }
        }
    }

    fun loadMoreFeedItems() {
        if (userDataSource.isLoggedIn()) {
            feedDataSource.getMyFeed(currentFeedPage, excludeReUps, true)
                .also { currentUrl = it.url }
                .observable
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    onFeedItemsNext(it)
                }, {
                    Timber.tag(TAG).e(it)
                }).composite()
        } else {
            feedPlaceHolderVisibilityEvent.postValue(true)
        }
    }

    private fun onFeedItemsNext(apiResponseData: APIResponseData) {
        val newItems = (apiResponseData.objects as? List<AMResultItem>).orEmpty()
        _feedItems.value = newItems
        allFeedItems.addAll(newItems)
        currentFeedPage++
        updateUI()
        feedPlaceHolderVisibilityEvent.postValue(allFeedItems.isEmpty())
    }

    private fun updateUI() {
        artistsDataSource.findLoggedArtist()
            .subscribeOn(schedulersProvider.io)
            .flatMap {
                it.feedCount = 0
                artistsDataSource.save(it).toObservable()
            }.observeOn(schedulersProvider.main)
            .subscribe({
                updateUIEvent.postValue(it)
            }, {})
            .composite()
    }

    fun onClickTwoDots(item: AMResultItem) {
        optionsFragmentEvent.postValue(item)
    }

    fun onClickItem(item: AMResultItem) {
        openMusicEvent.postValue(OpenMusicData(
            item,
            allFeedItems,
            feedMixPanelSource,
            false,
            currentUrl,
            currentPage
        ))
    }

    fun onClickDownload(item: AMResultItem) {
        download(item)
    }

    fun onPlaylistSyncConfirmed(playlist: AMResultItem) {
        download(playlist)

        if (userDataSource.isLoggedIn() &&
            !playlist.isUploadedByMyself(MainApplication.context) &&
            !userDataSource.isMusicFavorited(playlist)
        ) {
            onFavoriteTapped(playlist)
        }
    }

    private fun download(item: AMResultItem) {
        downloadUseCase(item, feedMixPanelSource, MixpanelButtonKebabMenu, onLoginRequired = {
            pendingActionAfterLogin = DataViewModel.PendingActionAfterLogin.Download(
                item,
                feedMixPanelSource,
                MixpanelButtonList
            )
        })?.composite()
    }

    private fun onFavoriteTapped(music: AMResultItem) {
        favoriteUseCase(music, feedMixPanelSource, compositeDisposable) {
            pendingActionAfterLogin = DataViewModel.PendingActionAfterLogin.Favorite(
                music,
                feedMixPanelSource,
                MixpanelButtonList
            )
        }
    }

    @VisibleForTesting
    fun reloadFeed() {
        currentFeedPage = 0
        allFeedItems.clear()
        reloadFeedEvent.postValue(Unit)
        loadMoreFeedItems()
    }

    private fun <T> onNextObserver(
        onNext: (t: T) -> Unit,
        onError: (e: Throwable) -> Unit = {}
    ): FeedItemsObserver<T> = object : FeedItemsObserver<T>() {
        override fun onNext(t: T) {
            onNext(t)
        }

        override fun onError(e: Throwable) {
            super.onError(e)
            onError(e)
        }
    }

    override fun onCleared() {
        eventBus.unregister(this)
        super.onCleared()
    }

    abstract inner class FeedItemsObserver<T> : Observer<T> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            Timber.tag(TAG).e(e)
        }
    }

    companion object {
        private const val TAG = "FeedViewModel"
    }
}
