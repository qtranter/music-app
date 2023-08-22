package com.audiomack.ui.mylibrary.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.database.ArtistDAO
import com.audiomack.model.AMArtist
import com.audiomack.model.ArtistWithBadge
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class MyLibrarySearchViewModelTest {

    private lateinit var viewModel: MyLibrarySearchViewModel

    @Mock private lateinit var artistsDAO: ArtistDAO
    private lateinit var schedulersProvider: SchedulersProvider

    @Mock private lateinit var artistNameObserver: Observer<ArtistWithBadge>
    @Mock private lateinit var clearSearchVisibleObserver: Observer<Boolean>
    @Mock private lateinit var searchQueryObserver: Observer<String?>
    @Mock private lateinit var closeEventObserver: Observer<Void>
    @Mock private lateinit var clearSearchbarEventObserver: Observer<Void>
    @Mock private lateinit var showKeyboardEventObserver: Observer<Void>
    @Mock private lateinit var hideKeyboardEventObserver: Observer<Void>

    private lateinit var artistObservable: Observable<AMArtist>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val artist = mock<AMArtist>()
        artistObservable = Observable.just(artist)
        whenever(artistsDAO.find()).thenReturn(artistObservable)
        schedulersProvider = TestSchedulersProvider()
        viewModel = MyLibrarySearchViewModel(
            artistsDAO,
            schedulersProvider
        ).apply {
            artistName.observeForever(artistNameObserver)
            clearSearchVisible.observeForever(clearSearchVisibleObserver)
            searchQuery.observeForever(searchQueryObserver)
            closeEvent.observeForever(closeEventObserver)
            clearSearchbarEvent.observeForever(clearSearchbarEventObserver)
            showKeyboardEvent.observeForever(showKeyboardEventObserver)
            hideKeyboardEvent.observeForever(hideKeyboardEventObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `init code`() {
        verify(artistNameObserver).onChanged(any())
        verify(clearSearchVisibleObserver).onChanged(false)
        verify(clearSearchbarEventObserver).onChanged(null)
    }

    @Test
    fun `on back clicked observed`() {
        viewModel.onBackTapped()
        verify(closeEventObserver).onChanged(null)
    }

    @Test
    fun `on cancel clicked observed`() {
        viewModel.onCancelTapped()
        verify(closeEventObserver).onChanged(null)
    }

    @Test
    fun `on clear clicked observed`() {
        viewModel.onClearTapped()
        verify(clearSearchbarEventObserver, times(2)).onChanged(null)
        verify(showKeyboardEventObserver).onChanged(null)
    }

    @Test
    fun `on search clicked - empty string - doesn't trigger search`() {
        viewModel.onSearchClicked(" ")
        verifyZeroInteractions(searchQueryObserver)
        verify(hideKeyboardEventObserver).onChanged(null)
    }

    @Test
    fun `on search clicked - valid string - triggers search`() {
        val query = "asd"
        viewModel.onSearchClicked(query)
        verify(searchQueryObserver).onChanged(query)
        verify(hideKeyboardEventObserver).onChanged(null)
    }

    @Test
    fun `on search text changed - empty string - clear button invisible`() {
        viewModel.onSearchTextChanged(" ")
        verify(clearSearchVisibleObserver).onChanged(false)
    }

    @Test
    fun `on search text changed - valid string - clear button visible`() {
        viewModel.onSearchTextChanged("asd")
        verify(clearSearchVisibleObserver).onChanged(true)
    }
}
