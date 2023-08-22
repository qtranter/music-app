package com.audiomack.ui.imagezoom

import android.widget.ImageView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.ui.home.NavigationActions
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
import org.mockito.MockitoAnnotations

class ImageZoomViewModelTest {

    @Mock
    private lateinit var imageLoader: ImageLoader

    @Mock
    private lateinit var navigationActions: NavigationActions

    private lateinit var viewModel: ImageZoomViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        viewModel = ImageZoomViewModel(imageLoader, navigationActions)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        viewModel.onCloseTapped()
        verify(navigationActions, times(1)).navigateBack()
    }

    @Test
    fun imageViewValidVerticalFling() {
        viewModel.onImageViewFling(1f, 8f)
        verify(navigationActions, times(1)).navigateBack()
    }

    @Test
    fun imageViewInvalidVerticalFling() {
        viewModel.onImageViewFling(3f, 3f)
        verifyZeroInteractions(navigationActions)
    }

    @Test
    fun loadInvalidImageDoesNotShowProgressBarAndImageView() {
        val observerProgressBar: Observer<Boolean> = mock()
        val observerImageView: Observer<Boolean> = mock()
        viewModel.toggleProgressBar.observeForever(observerProgressBar)
        viewModel.toggleImageView.observeForever(observerImageView)
        viewModel.loadImage(mock(), "", ImageView(mock()))
        verifyZeroInteractions(observerProgressBar)
        verifyZeroInteractions(observerImageView)
        verify(navigationActions, times(1)).navigateBack()
    }

    @Test
    fun loadImageShowsProgressBarAndImageView() {
        val observerProgressBar: Observer<Boolean> = mock()
        val observerImageView: Observer<Boolean> = mock()
        viewModel.toggleProgressBar.observeForever(observerProgressBar)
        viewModel.toggleImageView.observeForever(observerImageView)
        viewModel.loadImage(mock(), "https://audiomack.com/favicon.ico", ImageView(mock()))
        verify(observerProgressBar).onChanged(true)
        verify(observerImageView).onChanged(true)
    }
}
