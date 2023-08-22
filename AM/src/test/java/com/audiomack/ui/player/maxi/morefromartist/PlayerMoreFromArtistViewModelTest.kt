package com.audiomack.ui.player.maxi.morefromartist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.ui.common.Resource
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibility
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibilityData
import com.audiomack.ui.player.maxi.bottom.playerTabCommentsIndex
import com.audiomack.ui.player.maxi.bottom.playerTabMoreFromArtistIndex
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
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

class PlayerMoreFromArtistViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var playerDataSource: PlayerDataSource

    @Mock
    private lateinit var playerBottomVisibility: PlayerBottomVisibility

    private lateinit var viewModel: PlayerMoreFromArtistViewModel

    private lateinit var songSubscriber: io.reactivex.Observer<Resource<AMResultItem>>

    private lateinit var visibilitySubscriber: io.reactivex.Observer<PlayerBottomVisibilityData>

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        `when`(playerDataSource.subscribeToSong(any())).then {
            songSubscriber = (it.arguments.first() as io.reactivex.Observer<Resource<AMResultItem>>)
            Unit
        }
        `when`(playerBottomVisibility.subscribe(any())).then {
            visibilitySubscriber = (it.arguments.first() as io.reactivex.Observer<PlayerBottomVisibilityData>)
            Unit
        }

        viewModel = PlayerMoreFromArtistViewModel(playerDataSource, playerBottomVisibility)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `on song changed, success, tab visibile and selected`() {
        `when`(playerBottomVisibility.tabsVisible).thenReturn(true)
        `when`(playerBottomVisibility.tabIndex).thenReturn(playerTabMoreFromArtistIndex)

        songSubscriber.onNext(Resource.Success(mock()))

        val observerLoadData: Observer<Void> = mock()
        val observerUploaderName: Observer<String> = mock()

        viewModel.loadDataEvent.observeForever(observerLoadData)
        viewModel.uploaderName.observeForever(observerUploaderName)

        verify(observerLoadData).onChanged(null)
        verify(observerUploaderName).onChanged(anyOrNull())
    }

    @Test
    fun `on song changed, success, tab not visible and selected`() {
        `when`(playerBottomVisibility.tabsVisible).thenReturn(true)
        `when`(playerBottomVisibility.tabIndex).thenReturn(playerTabCommentsIndex)

        songSubscriber.onNext(Resource.Success(mock()))

        val observerLoadData: Observer<Void> = mock()
        val observerUploaderName: Observer<String> = mock()

        viewModel.loadDataEvent.observeForever(observerLoadData)
        viewModel.uploaderName.observeForever(observerUploaderName)

        verifyZeroInteractions(observerLoadData)
        verify(observerUploaderName).onChanged(anyOrNull())
    }

    @Test
    fun `on visibility changed, tab visible and selected`() {
        visibilitySubscriber.onNext(PlayerBottomVisibilityData(playerTabMoreFromArtistIndex, false))

        val observerLoadData: Observer<Void> = mock()
        viewModel.loadDataEvent.observeForever(observerLoadData)
        verify(observerLoadData).onChanged(null)
    }

    @Test
    fun `on visibility changed, tab not visible and selected`() {
        visibilitySubscriber.onNext(PlayerBottomVisibilityData(playerTabCommentsIndex, false))

        val observerLoadData: Observer<Void> = mock()
        viewModel.loadDataEvent.observeForever(observerLoadData)
        verifyZeroInteractions(observerLoadData)
    }

    @Test
    fun `on song changed, loading`() {
        songSubscriber.onNext(Resource.Loading())

        val observerLoadData: Observer<Void> = mock()
        val observerShowLoading: Observer<Void> = mock()

        viewModel.loadDataEvent.observeForever(observerLoadData)
        viewModel.showLoading.observeForever(observerShowLoading)

        verifyZeroInteractions(observerLoadData)
        verify(observerShowLoading).onChanged(null)
    }

    @Test
    fun `on song changed, failure`() {
        songSubscriber.onNext(Resource.Failure(mock()))

        val observerLoadData: Observer<Void> = mock()
        val observerShowNoConnection: Observer<Void> = mock()

        viewModel.loadDataEvent.observeForever(observerLoadData)
        viewModel.showNoConnection.observeForever(observerShowNoConnection)

        verifyZeroInteractions(observerLoadData)
        verify(observerShowNoConnection).onChanged(null)
    }

    @Test
    fun `on placeholder tapped`() {
        val observerOpenUrl: Observer<String> = mock()
        viewModel.openInternalUrlEvent.observeForever(observerOpenUrl)
        viewModel.onPlaceholderTapped()
        verify(observerOpenUrl).onChanged(anyOrNull())
    }

    @Test
    fun `on footer tapped`() {
        val observerOpenUrl: Observer<String> = mock()
        viewModel.openInternalUrlEvent.observeForever(observerOpenUrl)
        viewModel.onFooterTapped()
        verify(observerOpenUrl).onChanged(anyOrNull())
    }

    @Test
    fun `player datasource subscription`() {
        verify(playerDataSource).subscribeToSong(any())
    }
}
