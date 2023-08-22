package com.audiomack.ui.highlights

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class EditHighlightsViewModelTest {

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    @Mock
    private lateinit var userDataSource: UserDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: EditHighlightsViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        viewModel = EditHighlightsViewModel(musicDataSource, userDataSource, schedulersProvider)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        `when`(musicDataSource.getHighlights(any(), any())).doAnswer { Observable.just(emptyList()) }
        `when`(musicDataSource.reorderHighlights(any())).doAnswer { Observable.just(emptyList()) }

        val observer: Observer<Void> = mock()
        viewModel.close.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `initial load success`() {
        `when`(musicDataSource.getHighlights(any(), any())).doAnswer { Observable.just(emptyList()) }
        `when`(userDataSource.getUserSlug()).doAnswer { "matteinn" }

        val loadingObserver: Observer<Boolean> = mock()
        val highlightsObserver: Observer<List<AMResultItem>> = mock()
        viewModel.loadingStatus.observeForever(loadingObserver)
        viewModel.highlightsReady.observeForever(highlightsObserver)
        viewModel.onHighlightsRequested()
        verify(loadingObserver, times(1)).onChanged(true)
        verify(loadingObserver, times(1)).onChanged(false)
        verify(highlightsObserver).onChanged(any())
    }

    @Test
    fun `initial load error`() {
        `when`(musicDataSource.getHighlights(any(), any())).doAnswer { Observable.error(Exception("Unknown error for tests")) }
        `when`(userDataSource.getUserSlug()).doAnswer { null }

        val loadingObserver: Observer<Boolean> = mock()
        val highlightsObserver: Observer<List<AMResultItem>> = mock()
        viewModel.loadingStatus.observeForever(loadingObserver)
        viewModel.highlightsReady.observeForever(highlightsObserver)
        viewModel.onHighlightsRequested()
        verify(loadingObserver, times(1)).onChanged(true)
        verify(loadingObserver, times(1)).onChanged(false)
        verify(highlightsObserver).onChanged(any())
    }

    @Test
    fun `save success`() {
        `when`(musicDataSource.reorderHighlights(any())).doAnswer { Observable.just(emptyList()) }

        val observer: Observer<EditHighlightsStatus> = mock()
        viewModel.saveResult.observeForever(observer)
        viewModel.onSaveTapped(emptyList())
        verify(observer, times(1)).onChanged(EditHighlightsStatus.InProgress)
        verify(observer, times(1)).onChanged(EditHighlightsStatus.Succeeded)
    }

    @Test
    fun `save error`() {
        `when`(musicDataSource.reorderHighlights(any())).doAnswer { Observable.error(Exception("Unknown error for tests")) }

        val observer: Observer<EditHighlightsStatus> = mock()
        viewModel.saveResult.observeForever(observer)
        viewModel.onSaveTapped(emptyList())
        verify(observer, times(1)).onChanged(EditHighlightsStatus.InProgress)
        verify(observer, times(1)).onChanged(EditHighlightsStatus.Failed)
    }
}
