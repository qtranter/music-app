package com.audiomack.ui.removedcontent

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.removedcontent.RemovedContentDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class RemovedContentViewModelTest {

    @Mock
    lateinit var trackingRepository: TrackingDataSource

    @Mock
    lateinit var removedContentRepository: RemovedContentDataSource

    lateinit var viewModel: RemovedContentViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        viewModel = RemovedContentViewModel(trackingRepository, removedContentRepository)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observer: Observer<Void> = mock()
        viewModel.close.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun ok() {
        val observer: Observer<Void> = mock()
        viewModel.ok.observeForever(observer)
        viewModel.onOkTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun trackScreen() {
        viewModel.trackScreen()
        verify(trackingRepository).trackScreen(eq("Removed Content"))
    }

    @Test
    fun clearItems() {
        viewModel.clearItems()
        verify(removedContentRepository).clearItems()
    }
}
