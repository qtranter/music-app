package com.audiomack.ui.logviewer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.logviewer.LogDataSource
import com.audiomack.data.logviewer.LogType
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class LogViewerViewModelTest {

    @Mock
    private lateinit var logDataSource: LogDataSource
    private lateinit var viewModel: LogViewerViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        viewModel = LogViewerViewModel(logDataSource)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `back tapped`() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onBackTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `share tapped`() {
        val observer: Observer<String> = mock()
        viewModel.shareEvent.observeForever(observer)
        viewModel.onShareTapped()
        verify(observer).onChanged(any())
    }

    @Test
    fun `type changed`() {
        val list = listOf("A", "B", "C")
        `when`(logDataSource.provideData(any())).thenReturn(list)
        val observer: Observer<List<String>> = mock()
        viewModel.logs.observeForever(observer)
        viewModel.onTypeChanged(LogType.MIXPANEL)
        verify(observer).onChanged(list)
        verify(logDataSource, times(1)).provideData(LogType.MIXPANEL)
    }

    @Test
    fun `refresh triggered, no type selected`() {
        val list = listOf("A", "B", "C")
        `when`(logDataSource.provideData(any())).thenReturn(list)
        val observer: Observer<List<String>> = mock()
        viewModel.logs.observeForever(observer)
        viewModel.onRefreshTriggered()
        verifyZeroInteractions(observer)
    }

    @Test
    fun `refresh triggered, type selected`() {
        val list = listOf("A", "B", "C")
        `when`(logDataSource.provideData(any())).thenReturn(list)
        val observer: Observer<List<String>> = mock()
        viewModel.logs.observeForever(observer)
        viewModel.onTypeChanged(LogType.ADS)
        viewModel.onRefreshTriggered()
        verify(observer, times(2)).onChanged(list)
    }
}
