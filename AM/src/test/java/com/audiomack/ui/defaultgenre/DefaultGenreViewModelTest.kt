package com.audiomack.ui.defaultgenre

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.preferences.DefaultGenre
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.model.GenreModel
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class DefaultGenreViewModelTest {

    @Mock
    private lateinit var trackingDataSource: TrackingDataSource

    @Mock
    private lateinit var preferencesDataSource: PreferencesDataSource

    private lateinit var viewModel: DefaultGenreViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        viewModel = DefaultGenreViewModel(trackingDataSource, preferencesDataSource)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observer: Observer<Void> = mock()
        viewModel.back.observeForever(observer)
        viewModel.onBackTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun moreGenres() {
        val observer: Observer<Void> = mock()
        viewModel.moreGenres.observeForever(observer)
        viewModel.onMoreGenresTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun onGenreSelected() {
        val observer: Observer<Void> = mock()
        viewModel.back.observeForever(observer)
        viewModel.onGenreSelected(GenreModel(mock(), DefaultGenre.ELECTRONIC, "", "", true))
        verify(trackingDataSource).trackEvent(any(), any(), any())
        verify(preferencesDataSource).defaultGenre = DefaultGenre.ELECTRONIC
        verify(observer).onChanged(null)
    }
}
