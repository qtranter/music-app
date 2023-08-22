package com.audiomack.ui.browse.world.list

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.paging.PagingData
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.world.WorldDataSource
import com.audiomack.model.WorldArticle
import com.audiomack.model.WorldPage
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Flowable
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class WorldViewModelTest {

    @Mock
    private lateinit var repository: WorldDataSource

    @Mock
    private lateinit var reachability: ReachabilityDataSource

    @Mock
    private lateinit var adsDataSource: AdsDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: WorldViewModel

    @Mock
    private lateinit var viewStateObserver: Observer<WorldViewModel.ViewState>

    @Mock
    private lateinit var adsEnabledObserver: Observer<Boolean>

    @Mock
    private lateinit var openPostDetailEventObserver: Observer<String>

    @Mock
    private lateinit var setupPostsEventObserver: Observer<PagingData<WorldArticle>>

    @Mock
    private lateinit var showOfflineToastEventObserver: Observer<Void>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        whenever(repository.getPostsStream(any())).thenReturn(Flowable.just(PagingData.empty()))
        whenever(repository.getPages()).thenReturn(Single.just(emptyList()))
        whenever(adsDataSource.adsVisible).thenReturn(true)
        schedulersProvider = TestSchedulersProvider()
        viewModel = WorldViewModel(repository, reachability, adsDataSource, schedulersProvider).apply {
            viewState.observeForever(viewStateObserver)
            adsEnabled.observeForever(adsEnabledObserver)
            openPostDetailEvent.observeForever(openPostDetailEventObserver)
            setupPostsEvent.observeForever(setupPostsEventObserver)
            showOfflineToastEvent.observeForever(showOfflineToastEventObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `observe ads enabled value on init`() {
        verify(adsEnabledObserver).onChanged(adsDataSource.adsVisible)
    }

    @Test
    fun `page request observed`() {
        val page = WorldPage("Features", "features")
        viewModel.onPageRequested(page)
        verify(viewStateObserver).onChanged(argWhere { (it as? WorldViewModel.ViewState.LoadedPages)?.filterItems?.firstOrNull()?.page == page })
    }

    @Test
    fun `slug request observed`() {
        val slug = "123"
        viewModel.onSlugRequested(slug)
        verify(openPostDetailEventObserver).onChanged(slug)
    }

    @Test
    fun `handle error, offline`() {
        whenever(reachability.networkAvailable).thenReturn(false)
        viewModel.handleError()
        verify(showOfflineToastEventObserver).onChanged(null)
        verify(viewStateObserver).onChanged(WorldViewModel.ViewState.Error)
    }

    @Test
    fun `handle error, online`() {
        whenever(reachability.networkAvailable).thenReturn(true)
        viewModel.handleError()
        verifyZeroInteractions(showOfflineToastEventObserver)
        verify(viewStateObserver).onChanged(WorldViewModel.ViewState.Error)
    }
}
