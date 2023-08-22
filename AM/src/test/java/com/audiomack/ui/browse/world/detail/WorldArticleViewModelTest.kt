package com.audiomack.ui.browse.world.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.deeplink.Deeplink
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.world.WorldDataSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.WorldArticle
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class WorldArticleViewModelTest {

    @Mock
    private lateinit var repository: WorldDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    @Mock
    private lateinit var reachability: ReachabilityDataSource

    @Mock
    private lateinit var adsDataSource: AdsDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var jsMessageHandler: WorldArticleJSMessageHandler

    private lateinit var viewModel: WorldArticleViewModel

    @Mock
    private lateinit var viewStateObserver: Observer<WorldArticleViewModel.ViewState>

    @Mock
    private lateinit var onBackPressedEventObserver: Observer<Void>

    @Mock
    private lateinit var sharePostEventObserver: Observer<String>

    @Mock
    private lateinit var openDeeplinkEventObserver: Observer<Deeplink>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        viewModel = WorldArticleViewModel(
            repository,
            schedulersProvider,
            reachability,
            adsDataSource,
            mixpanelDataSource,
            jsMessageHandler
        ).apply {
            viewState.observeForever(viewStateObserver)
            onBackPressedEvent.observeForever(onBackPressedEventObserver)
            sharePostEvent.observeForever(sharePostEventObserver)
            openDeeplinkEvent.observeForever(openDeeplinkEventObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `back button observed`() {
        viewModel.onBackClicked()
        verify(onBackPressedEventObserver).onChanged(null)
    }

    @Test
    fun `html loaded observed`() {
        viewModel.onHtmlContentLoaded()
        verify(viewStateObserver).onChanged(WorldArticleViewModel.ViewState.ContentLoaded)
    }

    @Test
    fun `share button observed, valid link`() {
        val slug = "123"
        whenever(repository.getPost(slug)).thenReturn(Single.just(mock()))
        viewModel.initWithSlug(slug)
        viewModel.onShareClicked()
        verify(sharePostEventObserver).onChanged("https://audiomack.com/world/post/$slug")
        verify(mixpanelDataSource).trackShareContent(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `share button observed, missing link`() {
        viewModel.onShareClicked()
        verifyZeroInteractions(sharePostEventObserver)
        verify(mixpanelDataSource, never()).trackShareContent(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `share button observed, invalid link`() {
        val slug = ""
        whenever(repository.getPost(slug)).thenReturn(Single.just(mock()))
        viewModel.initWithSlug(slug)
        viewModel.onShareClicked()
        verifyZeroInteractions(sharePostEventObserver)
        verify(mixpanelDataSource, never()).trackShareContent(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `successful post response observed`() {
        val slug = ""
        val htmlReponse = "<html/>"
        val post = mock<WorldArticle> {
            on { html } doReturn htmlReponse
        }
        whenever(repository.getPost(slug)).thenReturn(Single.just(post))
        viewModel.initWithSlug(slug)
        verify(viewStateObserver, times(1)).onChanged(WorldArticleViewModel.ViewState.Loading)
        verify(viewStateObserver, times(1)).onChanged(argWhere {
            (it as? WorldArticleViewModel.ViewState.Content)?.html == htmlReponse &&
                (it as? WorldArticleViewModel.ViewState.Content)?.adsVisible == adsDataSource.adsVisible
        })
        verify(mixpanelDataSource).trackViewArticle(post)
    }

    @Test
    fun `unsuccessful post response observed, online`() {
        val slug = ""
        whenever(reachability.networkAvailable).thenReturn(true)
        whenever(repository.getPost(slug)).thenReturn(Single.error(Exception()))
        viewModel.initWithSlug(slug)
        verify(viewStateObserver, times(1)).onChanged(WorldArticleViewModel.ViewState.Loading)
        verify(viewStateObserver, times(1)).onChanged(WorldArticleViewModel.ViewState.Error)
        verifyZeroInteractions(mixpanelDataSource)
    }

    @Test
    fun `unsuccessful post response observed, offline`() {
        val slug = ""
        whenever(reachability.networkAvailable).thenReturn(false)
        whenever(repository.getPost(slug)).thenReturn(Single.error(Exception()))
        viewModel.initWithSlug(slug)
        verify(viewStateObserver, times(1)).onChanged(WorldArticleViewModel.ViewState.Loading)
        verify(viewStateObserver, times(1)).onChanged(WorldArticleViewModel.ViewState.Offline)
        verifyZeroInteractions(mixpanelDataSource)
    }

    @Test
    fun `handle javascript message, valid deeplink returned`() {
        val message = "{}"
        val deeplink = Deeplink.Song("123")
        whenever(jsMessageHandler.parseMessage(any(), any())).doReturn(deeplink)
        viewModel.onJSMessageReceived(message)
        verify(openDeeplinkEventObserver).onChanged(deeplink)
    }

    @Test
    fun `handle javascript message, invalid deeplink returned`() {
        val message = "{}"
        whenever(jsMessageHandler.parseMessage(message, MixpanelSource.empty)).doReturn(null)
        viewModel.onJSMessageReceived(message)
        verifyZeroInteractions(openDeeplinkEventObserver)
    }
}
