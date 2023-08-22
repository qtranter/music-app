package com.audiomack.ui.filter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.GENERAL_PREFERENCES_INCLUDE_LOCAL_FILES
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMGenre
import com.audiomack.model.AMMusicType
import com.audiomack.model.AMPeriod
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource.OfflineFilter
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.home.NavigationActions
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class FilterViewModelTest {

    @Mock lateinit var preferencesRepo: PreferencesDataSource
    @Mock lateinit var userRepo: UserDataSource
    @Mock lateinit var navigationActions: NavigationActions
    @Mock lateinit var trackingRepo: TrackingDataSource

    private lateinit var filterData: FilterData
    private lateinit var viewModel: FilterViewModel

    private val schedulers = TestSchedulersProvider()

    private val includeLocalFilesSubject = PublishSubject.create<Boolean>()
    private val loginStateChangeSubject = PublishSubject.create<EventLoginState>()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        whenever(preferencesRepo.observeBoolean(eq(GENERAL_PREFERENCES_INCLUDE_LOCAL_FILES)))
            .thenReturn(includeLocalFilesSubject)
        whenever(userRepo.loginEvents).thenReturn(loginStateChangeSubject)

        filterData = FilterData(
            "fragment",
            "title",
            listOf(FilterSection.Type),
            FilterSelection(
                AMGenre.Electronic,
                AMPeriod.Month
            )
        )

        viewModel = FilterViewModel(
            filterData,
            preferencesRepo,
            navigationActions,
            userRepo,
            trackingRepo,
            schedulers
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onCloseClick()
        verify(observer).onChanged(null)
    }

    @Test
    fun save() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onApplyClick()
        verify(observer).onChanged(null)
    }

    @Test
    fun `on create`() {
        val observer: Observer<Void> = mock()
        viewModel.updateUIEvent.observeForever(observer)
        viewModel.onCreate()
        verify(observer).onChanged(null)
    }

    @Test
    fun `initial selection`() {
        assert(viewModel.typeVisible)
        assert(!viewModel.sortVisible)
    }

    @Test
    fun `change selection`() {
        val observer: Observer<Void> = mock()
        viewModel.updateUIEvent.observeForever(observer)

        viewModel.onTypeAllClick()
        assert(viewModel.typeAllSelected)

        viewModel.onTypeSongsClick()
        assert(viewModel.typeSongsSelected)

        viewModel.onTypeAlbumsClick()
        assert(viewModel.typeAlbumsSelected)

        viewModel.onTypePlaylistsClick()
        assert(viewModel.typePlaylistsSelected)

        viewModel.onSortNewestClick()
        assert(viewModel.sortNewestSelected)

        viewModel.onSortOldestClick()
        assert(viewModel.sortOldestSelected)

        viewModel.onSortAZClick()
        assert(viewModel.sortAZSelected)

        verify(observer, times(7)).onChanged(null)
    }

    @Test
    fun `change radio selection`() {
        val observer: Observer<Void> = mock()
        viewModel.updateUIEvent.observeForever(observer)

        var aMMusicType = AMMusicType.All
        viewModel.onFilterTypeChanged(aMMusicType)
        assert(viewModel.typeAllSelected)

        aMMusicType = AMMusicType.Songs
        viewModel.onFilterTypeChanged(aMMusicType)
        assert(viewModel.typeSongsSelected)

        aMMusicType = AMMusicType.Albums
        viewModel.onFilterTypeChanged(aMMusicType)
        assert(viewModel.typeAlbumsSelected)

        aMMusicType = AMMusicType.Playlists
        viewModel.onFilterTypeChanged(aMMusicType)
        assert(viewModel.typePlaylistsSelected)

        verify(observer, times(4)).onChanged(null)
    }

    @Test
    fun `login shown when selecting local files without being logged in`() {
        whenever(userRepo.isLoggedIn()).thenReturn(false)
        viewModel.onSelectLocalFilesClick()
        verify(navigationActions, times(1)).launchLogin(OfflineFilter)
        verify(navigationActions, never()).launchLocalFilesSelection()
    }

    @Test
    fun `local file selection launched when selecting local files while logged in`() {
        whenever(userRepo.isLoggedIn()).thenReturn(true)

        viewModel.onSelectLocalFilesClick()

        verify(navigationActions, times(1)).launchLocalFilesSelection()
    }

    @Test
    fun `login shown when toggling local files on without being logged in`() {
        whenever(userRepo.isLoggedIn()).thenReturn(false)
        viewModel.onIncludeLocalFilesToggle(true)
        verify(navigationActions, times(1)).launchLogin(OfflineFilter)
    }

    @Test
    fun `preferences updated when toggling local files while logged in`() {
        whenever(userRepo.isLoggedIn()).thenReturn(true)

        viewModel.onIncludeLocalFilesToggle(true)

        verify(preferencesRepo).includeLocalFiles = true
    }

    @Test
    fun `local file selection toggle reflects preferences`() {
        whenever(preferencesRepo.includeLocalFiles).thenReturn(false)

        val observer = mock<Observer<Boolean>>()
        viewModel.includeLocalFiles.observeForever(observer)

        includeLocalFilesSubject.onNext(true)

        verify(observer, times(1)).onChanged(true)
    }

    @Test
    fun `local files toggle updated on preference change`() {
        whenever(preferencesRepo.localFileSelectionShown).thenReturn(true)

        val observer = mock<Observer<Boolean>>()
        viewModel.includeLocalFiles.observeForever(observer)

        includeLocalFilesSubject.onNext(true)

        verify(observer, times(1)).onChanged(true)
    }

    @Test
    fun `show local file selection when toggling local files on for the first time`() {
        whenever(userRepo.isLoggedIn()).thenReturn(true)
        whenever(preferencesRepo.localFileSelectionShown).thenReturn(false)

        includeLocalFilesSubject.onNext(true)

        verify(navigationActions, times(1)).launchLocalFilesSelection()
    }

    @Test
    fun `show local file selection not shown when toggling local files on after first time`() {
        whenever(userRepo.isLoggedIn()).thenReturn(true)
        whenever(preferencesRepo.localFileSelectionShown).thenReturn(true)

        includeLocalFilesSubject.onNext(true)

        verify(navigationActions, never()).launchLocalFilesSelection()
    }
}
