package com.audiomack.ui.onboarding.artists

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.onboarding.ArtistsOnboardingDataSource
import com.audiomack.data.onboarding.OnboardingPlaylistsGenreProvider
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.OnboardingArtist
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ArtistsOnboardingViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var dataSource: ArtistsOnboardingDataSource

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var preferencesDataSource: PreferencesDataSource

    @Mock
    private lateinit var onboardingPlaylistsGenreProvider: OnboardingPlaylistsGenreProvider

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: ArtistsOnboardingViewModel

    val artist = mock<AMArtist>()
    val playlist = mock<AMResultItem>()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        viewModel = ArtistsOnboardingViewModel(
            dataSource,
            musicDataSource,
            mixpanelDataSource,
            preferencesDataSource,
            onboardingPlaylistsGenreProvider,
            schedulersProvider
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `tap close`() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `tap footer`() {
        val observerTrending: Observer<Void> = mock()
        val observerClose: Observer<Void> = mock()
        viewModel.openTrendingEvent.observeForever(observerTrending)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onTapFooter()
        verify(observerTrending).onChanged(null)
        verify(observerClose).onChanged(null)
        verify(onboardingPlaylistsGenreProvider).setOnboardingGenre(null)
    }

    @Test
    fun `tap item`() {
        val observerChangedSelection: Observer<Int?> = mock()
        val observerEnableListenButton: Observer<Boolean> = mock()
        viewModel.changedSelectionEvent.observeForever(observerChangedSelection)
        viewModel.enableListenButtonEvent.observeForever(observerEnableListenButton)
        viewModel.onItemTapped(7)
        verify(observerChangedSelection).onChanged(eq(7))
        verify(observerEnableListenButton).onChanged(true)
    }

    @Test
    fun `refresh with failure on API call`() {
        `when`(dataSource.onboardingItems()).thenReturn(Observable.create { it.onError(Exception("")) })
        val observerHideLoading: Observer<Void> = mock()
        val observerUpdateList: Observer<List<OnboardingArtist>> = mock()
        viewModel.hideLoadingEvent.observeForever(observerHideLoading)
        viewModel.updateListEvent.observeForever(observerUpdateList)
        viewModel.onRefreshTriggered()
        verify(dataSource).onboardingItems()
        verify(observerHideLoading).onChanged(null)
        verifyZeroInteractions(observerUpdateList)
    }

    @Test
    fun `refresh with success on API call`() {
        `when`(dataSource.onboardingItems()).thenReturn(Observable.just(emptyList()))
        val observerHideLoading: Observer<Void> = mock()
        val observerUpdateList: Observer<List<OnboardingArtist>> = mock()
        viewModel.hideLoadingEvent.observeForever(observerHideLoading)
        viewModel.updateListEvent.observeForever(observerUpdateList)
        viewModel.onRefreshTriggered()
        verify(dataSource).onboardingItems()
        verify(observerHideLoading).onChanged(null)
        verify(observerUpdateList).onChanged(any())
    }

    @Test
    fun `create with failure on API call`() {
        `when`(dataSource.onboardingItems()).thenReturn(Observable.create { it.onError(Exception("")) })
        val observerChangedSelection: Observer<Int?> = mock()
        val observerEnableListenButton: Observer<Boolean> = mock()
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerUpdateList: Observer<List<OnboardingArtist>> = mock()
        viewModel.changedSelectionEvent.observeForever(observerChangedSelection)
        viewModel.enableListenButtonEvent.observeForever(observerEnableListenButton)
        viewModel.showLoadingEvent.observeForever(observerShowLoading)
        viewModel.hideLoadingEvent.observeForever(observerHideLoading)
        viewModel.updateListEvent.observeForever(observerUpdateList)
        viewModel.onCreate()
        verify(observerChangedSelection).onChanged(null)
        verify(observerEnableListenButton).onChanged(false)
        verify(dataSource).onboardingItems()
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verifyZeroInteractions(observerUpdateList)
    }

    @Test
    fun `create with success on API call`() {
        `when`(dataSource.onboardingItems()).thenReturn(Observable.just(emptyList()))
        val observerChangedSelection: Observer<Int?> = mock()
        val observerEnableListenButton: Observer<Boolean> = mock()
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerUpdateList: Observer<List<OnboardingArtist>> = mock()
        viewModel.changedSelectionEvent.observeForever(observerChangedSelection)
        viewModel.enableListenButtonEvent.observeForever(observerEnableListenButton)
        viewModel.showLoadingEvent.observeForever(observerShowLoading)
        viewModel.hideLoadingEvent.observeForever(observerHideLoading)
        viewModel.updateListEvent.observeForever(observerUpdateList)
        viewModel.onCreate()
        verify(observerChangedSelection).onChanged(null)
        verify(observerEnableListenButton).onChanged(false)
        verify(dataSource).onboardingItems()
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verify(observerUpdateList).onChanged(any())
    }

    @Test
    fun `on listen now tapped without selection`() {
        val observerShowHUD: Observer<Void> = mock()
        val observerHideHUD: Observer<Void> = mock()
        val observerShowHUDError: Observer<String> = mock()
        val observerShowPlaylist: Observer<Pair<String, AMResultItem>> = mock()
        viewModel.showHUDEvent.observeForever(observerShowHUD)
        viewModel.hideHUDEvent.observeForever(observerHideHUD)
        viewModel.showHUDErrorEvent.observeForever(observerShowHUDError)
        viewModel.showPlaylistEvent.observeForever(observerShowPlaylist)

        viewModel.onListenNowTapped()

        verifyZeroInteractions(mixpanelDataSource)
        verifyZeroInteractions(observerShowHUD)
        verifyZeroInteractions(observerHideHUD)
        verifyZeroInteractions(observerShowHUDError)
        verifyZeroInteractions(observerShowPlaylist)
    }

    @Test
    fun `on listen now tapped with valid selection, API call success`() {
        `when`(dataSource.onboardingItems()).thenReturn(Observable.just(listOf(OnboardingArtist(
            artist, "", ""
        ))))
        `when`(musicDataSource.getPlaylistInfo(any())).thenReturn(Observable.just(playlist))

        val observerShowHUD: Observer<Void> = mock()
        val observerHideHUD: Observer<Void> = mock()
        val observerShowHUDError: Observer<String> = mock()
        val observerShowPlaylist: Observer<Pair<String, AMResultItem>> = mock()
        viewModel.showHUDEvent.observeForever(observerShowHUD)
        viewModel.hideHUDEvent.observeForever(observerHideHUD)
        viewModel.showHUDErrorEvent.observeForever(observerShowHUDError)
        viewModel.showPlaylistEvent.observeForever(observerShowPlaylist)

        viewModel.onCreate()
        viewModel.onItemTapped(0)
        viewModel.onListenNowTapped()

        verify(mixpanelDataSource).trackOnboarding(any(), any(), any())
        verify(preferencesDataSource).onboardingGenre = anyOrNull()
        verify(onboardingPlaylistsGenreProvider).setOnboardingGenre(anyOrNull())
        verify(observerShowHUD).onChanged(null)
        verify(observerHideHUD).onChanged(null)
        verifyZeroInteractions(observerShowHUDError)
        verify(observerShowPlaylist).onChanged(any())
    }

    @Test
    fun `on listen now tapped with valid selection, API call failure`() {
        `when`(dataSource.onboardingItems()).thenReturn(Observable.just(listOf(OnboardingArtist(artist, "", ""))))
        `when`(musicDataSource.getPlaylistInfo(any())).thenReturn(Observable.error(Exception("")))

        val observerShowHUD: Observer<Void> = mock()
        val observerHideHUD: Observer<Void> = mock()
        val observerShowHUDError: Observer<String> = mock()
        val observerShowPlaylist: Observer<Pair<String, AMResultItem>> = mock()
        viewModel.showHUDEvent.observeForever(observerShowHUD)
        viewModel.hideHUDEvent.observeForever(observerHideHUD)
        viewModel.showHUDErrorEvent.observeForever(observerShowHUDError)
        viewModel.showPlaylistEvent.observeForever(observerShowPlaylist)

        viewModel.onCreate()
        viewModel.onItemTapped(0)
        viewModel.onListenNowTapped()

        verifyZeroInteractions(mixpanelDataSource)
        verifyZeroInteractions(onboardingPlaylistsGenreProvider)
        verify(observerShowHUD).onChanged(null)
        verifyZeroInteractions(observerHideHUD)
        verify(observerShowHUDError).onChanged(any())
        verifyZeroInteractions(observerShowPlaylist)
    }
}
