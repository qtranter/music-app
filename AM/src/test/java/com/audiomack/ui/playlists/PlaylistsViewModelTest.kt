package com.audiomack.ui.playlists

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.api.MusicDataSource
import com.audiomack.model.PlaylistCategory
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class PlaylistsViewModelTest {

    @Mock private lateinit var musicDataSource: MusicDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: PlaylistsViewModel

    @Mock private lateinit var observerSetupPager: Observer<List<PlaylistCategory>>
    @Mock private lateinit var observerLoaderVisible: Observer<Boolean>
    @Mock private lateinit var observerContentVisible: Observer<Boolean>
    @Mock private lateinit var observerPlaceholderVisible: Observer<Boolean>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        viewModel = PlaylistsViewModel("hometown-heroes", musicDataSource, schedulersProvider).apply {
            setupPagerEvent.observeForever(observerSetupPager)
            loaderVisible.observeForever(observerLoaderVisible)
            contentVisible.observeForever(observerContentVisible)
            placeholderVisible.observeForever(observerPlaceholderVisible)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `on all categories tapped`() {
        val observerOpenMenu: Observer<List<PlaylistCategory>> = mock()
        viewModel.openMenuEvent.observeForever(observerOpenMenu)
        viewModel.onAllCategoriesTapped()
        verify(observerOpenMenu).onChanged(any())
    }

    @Test
    fun `on placeholder tapped, success, with deeplink already contained in the categories`() {
        `when`(musicDataSource.playlistCategories()).thenReturn(Observable.just(listOf(
            PlaylistCategory("A", "A", "A"),
            PlaylistCategory("Hometown Heroes", "Hometown Heroes", "hometown-heroes"),
            PlaylistCategory("B", "B", "B")
        )))

        viewModel.onPlaceholderTapped()

        verify(observerLoaderVisible, times(1)).onChanged(true)
        verify(observerLoaderVisible, times(1)).onChanged(false)
        verify(observerContentVisible, times(1)).onChanged(false)
        verify(observerContentVisible, times(1)).onChanged(true)
        verify(observerPlaceholderVisible, times(2)).onChanged(false)
        verify(observerSetupPager).onChanged(eq(listOf(
            PlaylistCategory("Hometown Heroes", "Hometown Heroes", "hometown-heroes"),
            PlaylistCategory("A", "A", "A"),
            PlaylistCategory("B", "B", "B")
        )))
    }

    @Test
    fun `on placeholder tapped, success, with deeplink not contained in the categories`() {
        `when`(musicDataSource.playlistCategories()).thenReturn(Observable.just(listOf(
            PlaylistCategory("A", "A", "A"),
            PlaylistCategory("B", "B", "B"),
            PlaylistCategory("C", "C", "C")
        )))

        viewModel.onPlaceholderTapped()

        verify(observerLoaderVisible, times(1)).onChanged(true)
        verify(observerLoaderVisible, times(1)).onChanged(false)
        verify(observerContentVisible, times(1)).onChanged(false)
        verify(observerContentVisible, times(1)).onChanged(true)
        verify(observerPlaceholderVisible, times(2)).onChanged(false)
        verify(observerSetupPager).onChanged(eq(listOf(
            PlaylistCategory("A", "A", "A"),
            PlaylistCategory("B", "B", "B"),
            PlaylistCategory("C", "C", "C")
        )))
    }

    @Test
    fun `on placeholder tapped, failure`() {
        `when`(musicDataSource.playlistCategories()).thenReturn(Observable.error(Exception("Unknown error for tests")))

        viewModel.onPlaceholderTapped()

        verify(observerLoaderVisible, times(1)).onChanged(true)
        verify(observerLoaderVisible, times(1)).onChanged(false)
        verify(observerContentVisible, times(2)).onChanged(false)
        verify(observerPlaceholderVisible, times(1)).onChanged(false)
        verify(observerPlaceholderVisible, times(1)).onChanged(true)
        verifyZeroInteractions(observerSetupPager)
    }

    @Test
    fun `on placeholder tapped, success, no deeplink set`() {
        `when`(musicDataSource.playlistCategories()).thenReturn(Observable.just(listOf(
            PlaylistCategory("A", "A", "A"),
            PlaylistCategory("B", "B", "B"),
            PlaylistCategory("C", "C", "C")
        )))

        val viewModel = PlaylistsViewModel(null, musicDataSource, schedulersProvider).apply {
            setupPagerEvent.observeForever(observerSetupPager)
            loaderVisible.observeForever(observerLoaderVisible)
            contentVisible.observeForever(observerContentVisible)
            placeholderVisible.observeForever(observerPlaceholderVisible)
        }

        viewModel.onPlaceholderTapped()

        verify(observerLoaderVisible, times(1)).onChanged(true)
        verify(observerLoaderVisible, times(1)).onChanged(false)
        verify(observerContentVisible, times(1)).onChanged(false)
        verify(observerContentVisible, times(1)).onChanged(true)
        verify(observerPlaceholderVisible, times(2)).onChanged(false)
        verify(observerSetupPager).onChanged(argWhere { it.size == 3 })
    }

    @Test
    fun `on placeholder tapped twice, only hit API once if previous API call is not finished yet`() {
        `when`(musicDataSource.playlistCategories()).thenReturn(Observable.timer(1, TimeUnit.MILLISECONDS).flatMap {
            Observable.just(listOf(PlaylistCategory("A", "A", "A")))
        })

        viewModel.onPlaceholderTapped()

        `when`(musicDataSource.playlistCategories()).thenReturn(Observable.just(emptyList()))

        viewModel.onPlaceholderTapped()

        verify(musicDataSource, times(1)).playlistCategories()
    }

    @Test
    fun `on placeholder tapped twice, only hit API once if categories are already set`() {
        `when`(musicDataSource.playlistCategories()).thenReturn(Observable.just(listOf(
            PlaylistCategory("A", "A", "A"),
            PlaylistCategory("B", "B", "B"),
            PlaylistCategory("C", "C", "C")
        )))

        viewModel.onPlaceholderTapped()

        viewModel.onPlaceholderTapped()

        verify(musicDataSource, times(1)).playlistCategories()
    }
}
