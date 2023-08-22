package com.audiomack.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.keyboard.KeyboardDetector
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.search.SearchDataSource
import com.audiomack.data.search.SearchRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.AMFeaturedSpot
import com.audiomack.model.AMResultItem
import com.audiomack.model.SearchReturnType
import com.audiomack.model.SearchType
import com.audiomack.playback.Playback
import com.audiomack.playback.PlayerPlayback
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import java.util.Timer
import kotlin.concurrent.timerTask

class SearchViewModel(
    private val trackingRepository: TrackingDataSource = TrackingRepository(),
    private val searchDataSource: SearchDataSource = SearchRepository(),
    searchTrendingUseCase: SearchTrendingUseCase = SearchTrendingUseCaseImpl(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    playerPlayback: Playback = PlayerPlayback.getInstance()
) : BaseViewModel() {

    private var _clearButtonVisible = MutableLiveData(false)
    val clearButtonVisible: LiveData<Boolean> get() = _clearButtonVisible

    val startSearchEvent = MutableLiveData<Pair<String, Boolean>>()
    /** Emits the suggestions to be shown based on the current input text **/
    val suggestionsEvent = MutableLiveData<Pair<String, List<String>>>()
    val cancelEvent = SingleLiveEvent<Void>()
    val clearEvent = SingleLiveEvent<Void>()
    val showKeyboardEvent = SingleLiveEvent<Void>()
    val hideKeyboardEvent = SingleLiveEvent<Void>()
    val hideSuggestionsEvent = SingleLiveEvent<Void>()
    val showPlaceholderEvent = SingleLiveEvent<Void>()
    val notifyTrendingAdapterEvent = SingleLiveEvent<Void>()
    /** Emits the [List<SearchTrendingHistoryItem>] to be shown in the list **/
    val updateTrendingHistoryListEvent = SingleLiveEvent<List<SearchTrendingHistoryItem>>()
    /** Emits the [AMResultItem] song that needs to be played together with the new player queue **/
    val openSongEvent = SingleLiveEvent<Pair<AMResultItem, List<AMResultItem>>>()
    /** Emits the [AMResultItem] album that needs to be shown **/
    val openAlbumEvent = SingleLiveEvent<AMResultItem>()
    /** Emits the [AMResultItem] playlist that needs to be shown **/
    val openPlaylistEvent = SingleLiveEvent<AMResultItem>()
    /** Emits the [AMArtist] that needs to be shown **/
    val openArtistEvent = SingleLiveEvent<AMArtist>()
    /** Used to notify the search sub-sections of the need to reload themselves **/
    val notifyTabsEvent = SingleLiveEvent<Void>()

    private var lastQuery: String? = null
    private var lastSearchType: SearchType? = null
    private var lastSuggestionQuery: String? = null
    private var suggestionsDisabled: Boolean = false
    private var suggestionsDebounceTimer: Timer? = null

    private var featuredItems: List<AMFeaturedSpot> = emptyList()

    /** Contains info about the bottom padding to be applied to the lists and the fact that banner ad is visible or not **/
    private val _recyclerViewPadding = MutableLiveData<RecyclerViewPadding>()
    val recyclerViewPadding: LiveData<RecyclerViewPadding> get() = _recyclerViewPadding
    data class RecyclerViewPadding(
        val bottomPadding: Int,
        val adsVisible: Boolean
    )

    private var premium: Boolean = false

    enum class PlaceholderMode {
        Trending, History
    }

    private var placeholderMode: PlaceholderMode = PlaceholderMode.Trending
    set(value) {
        field = value
        when (value) {
            PlaceholderMode.Trending -> showFeaturedItems()
            PlaceholderMode.History -> {
                showPlaceholderEvent.call()
                showRecentSearches()
            }
        }
    }

    init {
        searchTrendingUseCase.getTrendingSearches()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                featuredItems = it
                if (placeholderMode == PlaceholderMode.Trending) {
                    showFeaturedItems()
                }
            }, {})
            .addTo(compositeDisposable)

        playerPlayback.item
            .distinctUntilChanged()
            .observeOn(schedulersProvider.main)
            .subscribe({
                if (placeholderMode == PlaceholderMode.Trending) {
                    notifyTrendingAdapterEvent.call()
                }
            }, {})
            .addTo(compositeDisposable)

        premiumDataSource.premiumObservable
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                premium = it
                updateListsPadding()
            }, {})
            .addTo(compositeDisposable)

        placeholderMode = PlaceholderMode.Trending
    }

    private fun showFeaturedItems() {
        val list = mutableListOf<SearchTrendingHistoryItem>().apply {
            add(SearchTrendingHistoryItem.RecommendationsHeader)
            addAll(featuredItems.mapNotNull {
                it.artist?.let { artist -> SearchTrendingHistoryItem.TrendingArtist(artist) } ?: it.item?.let { music -> SearchTrendingHistoryItem.TrendingMusic(music) }
            })
        }
        updateTrendingHistoryListEvent.postValue(list)
    }

    private fun showRecentSearches() {
        val list = mutableListOf<SearchTrendingHistoryItem>().apply {
            add(SearchTrendingHistoryItem.RecentHeader)
            addAll(searchDataSource.getRecentSearches().map { SearchTrendingHistoryItem.RecentSearch(it) })
        }
        updateTrendingHistoryListEvent.postValue(list)
    }

    fun onSearchTapped(query: String?, type: SearchType) {
        query?.let {
            if (it.isEmpty()) {
                cancelEvent.call()
                lastQuery = null
                lastSearchType = null
            } else {
                suggestionsDisabled = true
                startSearchEvent.postValue(Pair(it, type == SearchType.Trending))
                searchDataSource.addRecentSearch(it)
                trackingRepository.trackGA("Search", it, null)
                lastQuery = it
                lastSearchType = type
            }
        }
        hideKeyboardEvent.call()
    }

    fun onSearchCompleted(replacementSearch: Boolean) {
        val lastQuery = lastQuery ?: return
        val lastSearchType = lastSearchType ?: return
        mixpanelDataSource.trackSearch(lastQuery, lastSearchType, if (replacementSearch) SearchReturnType.Replacement else SearchReturnType.Requested)
    }

    fun onDeleteRecentSearch(query: String) {
        searchDataSource.removeRecentSearch(query)
    }

    fun onCancelTapped() {
        cancelEvent.call()
        hideKeyboardEvent.call()
    }

    fun onClearTapped() {
        placeholderMode = PlaceholderMode.History
        clearEvent.call()
        _clearButtonVisible.postValue(false)
        showKeyboardEvent.call()
    }

    fun onMusicTapped(music: AMResultItem) {
        when {
            music.isPlaylist -> openPlaylistEvent.postValue(music)
            music.isAlbum -> openAlbumEvent.postValue(music)
            else -> openSongEvent.postValue(Pair(music, featuredItems.mapNotNull { it.item }))
        }
    }

    fun onArtistTapped(artist: AMArtist) {
        openArtistEvent.postValue(artist)
    }

    fun onDestroy() {
        hideKeyboardEvent.call()
    }

    fun onTextChanged(query: String) {
        if (query.length >= 2) {
            if (query != lastSuggestionQuery) {
                showPlaceholderEvent.call()
                cancelSuggestionsSearch()
                suggestionsDebounceTimer = Timer().apply {
                    schedule(timerTask {
                        if (!suggestionsDisabled) {
                            onSuggestionsRequested(query)
                        }
                        suggestionsDisabled = false
                    }, 500)
                }
            }
        } else {
            cancelSuggestionsSearch()
            hideSuggestionsEvent.call()
        }
        _clearButtonVisible.postValue(query.isNotEmpty())
    }

    fun onKeyboardFocusRequested() {
        showKeyboardEvent.call()
    }

    fun onKeyboardVisibilityChanged(state: KeyboardDetector.KeyboardState) {
        placeholderMode = if (state.open) PlaceholderMode.History else PlaceholderMode.Trending
        updateListsPadding(state.keyboardHeightPx)
    }

    private fun updateListsPadding(height: Int = 0) {
        _recyclerViewPadding.postValue(RecyclerViewPadding(height, height == 0 && !premium))
    }

    fun notifyTabs() {
        notifyTabsEvent.call()
    }

    private fun onSuggestionsRequested(query: String) {
        compositeDisposable.add(
            searchDataSource.autosuggest(query)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ res ->
                    lastSuggestionQuery = query
                    if (!suggestionsDisabled) {
                        suggestionsEvent.postValue(Pair(query, res))
                    }
                }, { })
        )
    }

    private fun cancelSuggestionsSearch() {
        suggestionsDebounceTimer?.cancel()
    }

    override fun onCleared() {
        cancelSuggestionsSearch()
        super.onCleared()
    }
}
