package com.audiomack.ui.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.keyboard.KeyboardDetector
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.search.SearchDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.SearchReturnType
import com.audiomack.model.SearchType
import com.audiomack.playback.Playback
import com.audiomack.playback.PlaybackItem
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class SearchViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var trackingDataSource: TrackingDataSource

    @Mock
    private lateinit var searchDataSource: SearchDataSource

    @Mock
    private lateinit var searchTrendingUseCase: SearchTrendingUseCase

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var playerPlayback: Playback

    private lateinit var viewModel: SearchViewModel

    private lateinit var schedulersProvider: SchedulersProvider

    // Utils

    private lateinit var playbackItemSubject: BehaviorSubject<PlaybackItem>

    // Observers

    @Mock private lateinit var observerCancel: Observer<Void>
    @Mock private lateinit var observerShowKeyboard: Observer<Void>
    @Mock private lateinit var observerHideKeyboard: Observer<Void>
    @Mock private lateinit var observerClear: Observer<Void>
    @Mock private lateinit var observerClearButtonVisible: Observer<Boolean>
    @Mock private lateinit var observerOpenPlaylist: Observer<AMResultItem>
    @Mock private lateinit var observerOpenAlbum: Observer<AMResultItem>
    @Mock private lateinit var observerOpenSong: Observer<Pair<AMResultItem, List<AMResultItem>>>
    @Mock private lateinit var observerOpenArtist: Observer<AMArtist>
    @Mock private lateinit var observerUpdateTrendingHistoryList: Observer<List<SearchTrendingHistoryItem>>
    @Mock private lateinit var observerNotifyTrendingAdapter: Observer<Void>
    @Mock private lateinit var observerNotifyTabs: Observer<Void>
    @Mock private lateinit var observerRecyclerViewPadding: Observer<SearchViewModel.RecyclerViewPadding>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        whenever(searchDataSource.autosuggest(any())).thenReturn(Observable.error(Exception("Error for test")))
        whenever(searchTrendingUseCase.getTrendingSearches()).thenReturn(Single.just(emptyList()))
        playbackItemSubject = BehaviorSubject.create()
        whenever(playerPlayback.item).thenReturn(playbackItemSubject)
        whenever(premiumDataSource.premiumObservable).thenReturn(Observable.just(false))
        viewModel = SearchViewModel(
            trackingDataSource,
            searchDataSource,
            searchTrendingUseCase,
            premiumDataSource,
            mixpanelDataSource,
            schedulersProvider,
            playerPlayback
        ).apply {
            cancelEvent.observeForever(observerCancel)
            showKeyboardEvent.observeForever(observerShowKeyboard)
            hideKeyboardEvent.observeForever(observerHideKeyboard)
            clearEvent.observeForever(observerClear)
            clearButtonVisible.observeForever(observerClearButtonVisible)
            openPlaylistEvent.observeForever(observerOpenPlaylist)
            openAlbumEvent.observeForever(observerOpenAlbum)
            openSongEvent.observeForever(observerOpenSong)
            openArtistEvent.observeForever(observerOpenArtist)
            updateTrendingHistoryListEvent.observeForever(observerUpdateTrendingHistoryList)
            notifyTrendingAdapterEvent.observeForever(observerNotifyTrendingAdapter)
            notifyTabsEvent.observeForever(observerNotifyTabs)
            recyclerViewPadding.observeForever(observerRecyclerViewPadding)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `init subscriptions`() {
        verify(searchTrendingUseCase, times(1)).getTrendingSearches()
        verify(observerUpdateTrendingHistoryList).onChanged(any())
    }

    @Test
    fun `onCancelTapped observed`() {
        viewModel.onCancelTapped()

        verify(observerCancel).onChanged(null)
        verify(observerHideKeyboard).onChanged(null)
    }

    @Test
    fun `onClearTapped observed`() {
        viewModel.onClearTapped()

        verify(observerClear).onChanged(null)
        verify(observerClearButtonVisible, atLeast(1)).onChanged(false)
        verify(observerShowKeyboard).onChanged(null)
    }

    @Test
    fun `onMusicTapped playlist`() {
        val playlist = mock<AMResultItem> {
            on { isPlaylist } doReturn true
            on { itemId } doReturn "1"
        }
        viewModel.onMusicTapped(playlist)

        verify(observerOpenPlaylist).onChanged(eq(playlist))
    }

    @Test
    fun `onMusicTapped album`() {
        val album = mock<AMResultItem> {
            on { isAlbum } doReturn true
            on { itemId } doReturn "1"
        }
        viewModel.onMusicTapped(album)

        verify(observerOpenAlbum).onChanged(eq(album))
    }

    @Test
    fun `onMusicTapped song`() {
        val song = mock<AMResultItem> {
            on { isSong } doReturn true
            on { itemId } doReturn "1"
        }

        viewModel.onMusicTapped(song)

        verify(observerOpenSong).onChanged(argWhere { it.first == song && it.second.isEmpty() })
    }

    @Test
    fun `onArtistTapped observed`() {
        val artist = mock<AMArtist>()

        viewModel.onArtistTapped(artist)

        verify(observerOpenArtist).onChanged(eq(artist))
    }

    @Test
    fun `destroy observed`() {
        viewModel.onDestroy()

        verify(observerHideKeyboard).onChanged(null)
    }

    @Test
    fun `onTextChanged suggestions short text`() {
        val observerHideSuggestions: Observer<Void> = mock()
        viewModel.hideSuggestionsEvent.observeForever(observerHideSuggestions)

        viewModel.onTextChanged("Q")

        verify(observerHideSuggestions).onChanged(null)
    }

    @Test
    fun `onTextChanged suggestions long text`() {
        val observerShowPlaceholder: Observer<Void> = mock()
        viewModel.showPlaceholderEvent.observeForever(observerShowPlaceholder)

        viewModel.onTextChanged("Queen")

        verify(observerShowPlaceholder).onChanged(null)
    }

    @Test
    fun `onTextChanged clear button shows up after typing`() {
        viewModel.onTextChanged("Q")

        verify(observerClearButtonVisible).onChanged(true)
    }

    @Test
    fun `onTextChanged clear button doesn't show up with empty query`() {
        viewModel.onTextChanged("")

        verify(observerClearButtonVisible, atLeast(1)).onChanged(false)
    }

    @Test
    fun `request keyboard focus`() {
        viewModel.onKeyboardFocusRequested()

        verify(observerShowKeyboard).onChanged(null)
    }

    @Test
    fun `onKeyboardVisibilityChanged open - shows recent searches`() {
        val recentSearches = listOf("Quarantine", "Corona")
        whenever(searchDataSource.getRecentSearches()).thenReturn(recentSearches)

        viewModel.onKeyboardVisibilityChanged(KeyboardDetector.KeyboardState(true, 200))

        verify(observerUpdateTrendingHistoryList).onChanged(argWhere {
            it.firstOrNull() == SearchTrendingHistoryItem.RecentHeader && it.last() == SearchTrendingHistoryItem.RecentSearch("Corona")
        })
        verify(observerRecyclerViewPadding).onChanged(SearchViewModel.RecyclerViewPadding(200, false))
    }

    @Test
    fun `onKeyboardVisibilityChanged closed`() {
        viewModel.onKeyboardVisibilityChanged(KeyboardDetector.KeyboardState(false, 0))

        verify(observerUpdateTrendingHistoryList, atLeast(1)).onChanged(argWhere {
            it.firstOrNull() == SearchTrendingHistoryItem.RecommendationsHeader &&
            it.none { it is SearchTrendingHistoryItem.TrendingMusic || it is SearchTrendingHistoryItem.TrendingArtist }
        })
        verify(observerRecyclerViewPadding, times(2)).onChanged(SearchViewModel.RecyclerViewPadding(0, true)) // 1 comes from the init
    }

    @Test
    fun `empty search`() {
        val observerSearch: Observer<Pair<String, Boolean>> = mock()
        viewModel.startSearchEvent.observeForever(observerSearch)

        viewModel.onSearchTapped("", SearchType.Direct)

        verify(observerCancel).onChanged(null)
        verify(observerHideKeyboard).onChanged(null)
        verifyZeroInteractions(observerSearch)
        verifyZeroInteractions(mixpanelDataSource)
    }

    @Test
    fun `null search`() {
        val observerSearch: Observer<Pair<String, Boolean>> = mock()
        viewModel.startSearchEvent.observeForever(observerSearch)

        viewModel.onSearchTapped(null, SearchType.Direct)

        verifyZeroInteractions(observerCancel)
        verify(observerHideKeyboard).onChanged(null)
        verifyZeroInteractions(observerSearch)
        verifyZeroInteractions(mixpanelDataSource)
    }

    @Test
    fun `valid search`() {
        val observerSearch: Observer<Pair<String, Boolean>> = mock()
        viewModel.startSearchEvent.observeForever(observerSearch)

        viewModel.onSearchTapped("Queen", SearchType.Direct)

        verifyZeroInteractions(observerCancel)
        verify(observerHideKeyboard).onChanged(null)
        verify(observerSearch).onChanged(any())
        verifyZeroInteractions(mixpanelDataSource)
    }

    @Test
    fun `search response`() {
        val QUERY = "Queen"
        val TYPE = SearchType.Direct

        val observerSearch: Observer<Pair<String, Boolean>> = mock()
        viewModel.startSearchEvent.observeForever(observerSearch)

        viewModel.onSearchTapped(QUERY, TYPE)
        viewModel.onSearchCompleted(true)

        verify(observerSearch).onChanged(any())
        verify(mixpanelDataSource).trackSearch(eq(QUERY), eq(TYPE), eq(SearchReturnType.Replacement))

        viewModel.onSearchTapped(QUERY, TYPE)
        viewModel.onSearchCompleted(false)

        verify(mixpanelDataSource).trackSearch(eq(QUERY), eq(TYPE), eq(SearchReturnType.Requested))
    }

    @Test
    fun `delete recent search`() {
        viewModel.onDeleteRecentSearch("Queen")

        verify(searchDataSource).removeRecentSearch(eq("Queen"))
    }

    @Test
    fun `on playback item changed notify adapter`() {
        playbackItemSubject.onNext(mock())

        verify(observerNotifyTrendingAdapter).onChanged(null)
    }

    @Test
    fun `notify tabs observed`() {
        viewModel.notifyTabs()

        verify(observerNotifyTabs).onChanged(null)
    }
}
