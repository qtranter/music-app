package com.audiomack.ui.player.maxi.info

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.ui.common.Resource
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyBoolean
import org.mockito.MockitoAnnotations

class PlayerInfoViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var playerDataSource: PlayerDataSource

    private lateinit var viewModel: PlayerInfoViewModel

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        `when`(playerDataSource.subscribeToSong(any())).then { (it.arguments.first() as io.reactivex.Observer<Resource<AMResultItem>>).onNext(Resource.Success(
            mock {
                on { tags } doReturn emptyArray()
            }
        )) }
        viewModel = PlayerInfoViewModel(playerDataSource)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `on song changed`() {
        val observerTags: Observer<List<String>> = mock()
        val observerTotalPlays: Observer<String> = mock()
        val observerAlbum: Observer<String> = mock()
        val observerProducer: Observer<String> = mock()
        val observerAddedOn: Observer<String> = mock()
        val observerGenre: Observer<String> = mock()
        val observerDescription: Observer<String> = mock()
        val observerDescriptionExpanded: Observer<Boolean> = mock()
        val observerRankVisible: Observer<Boolean> = mock()
        val observerRankToday: Observer<String> = mock()
        val observerRankWeek: Observer<String> = mock()
        val observerRankMonth: Observer<String> = mock()
        val observerRankAllTime: Observer<String> = mock()

        viewModel.tags.observeForever(observerTags)
        viewModel.totalPlays.observeForever(observerTotalPlays)
        viewModel.album.observeForever(observerAlbum)
        viewModel.producer.observeForever(observerProducer)
        viewModel.addedOn.observeForever(observerAddedOn)
        viewModel.genre.observeForever(observerGenre)
        viewModel.description.observeForever(observerDescription)
        viewModel.descriptionExpanded.observeForever(observerDescriptionExpanded)
        viewModel.rankVisible.observeForever(observerRankVisible)
        viewModel.rankToday.observeForever(observerRankToday)
        viewModel.rankWeek.observeForever(observerRankWeek)
        viewModel.rankMonth.observeForever(observerRankMonth)
        viewModel.rankAllTime.observeForever(observerRankAllTime)

        verify(observerTags).onChanged(any())
        verify(observerTotalPlays).onChanged(anyOrNull())
        verify(observerAlbum).onChanged(anyOrNull())
        verify(observerProducer).onChanged(anyOrNull())
        verify(observerAddedOn).onChanged(anyOrNull())
        verify(observerGenre).onChanged(anyOrNull())
        verify(observerDescription).onChanged(anyOrNull())
        verify(observerDescriptionExpanded, times(1)).onChanged(true)
        verify(observerRankVisible).onChanged(anyBoolean())
        verify(observerRankToday).onChanged(anyOrNull())
        verify(observerRankWeek).onChanged(anyOrNull())
        verify(observerRankMonth).onChanged(anyOrNull())
        verify(observerRankAllTime).onChanged(anyOrNull())

        viewModel.onDescriptionReadMoreTapped()
        verify(observerDescriptionExpanded, times(2)).onChanged(true)
    }

    @Test
    fun `rank today`() {
        val openURLObserver: Observer<String> = mock()
        val closePlayerObserver: Observer<Void> = mock()
        viewModel.openInternalURLEvent.observeForever(openURLObserver)
        viewModel.closePlayer.observeForever(closePlayerObserver)
        viewModel.onTodayTapped()
        verify(openURLObserver).onChanged(any())
        verify(closePlayerObserver).onChanged(null)
    }

    @Test
    fun `rank week`() {
        val openURLObserver: Observer<String> = mock()
        val closePlayerObserver: Observer<Void> = mock()
        viewModel.openInternalURLEvent.observeForever(openURLObserver)
        viewModel.closePlayer.observeForever(closePlayerObserver)
        viewModel.onWeekTapped()
        verify(openURLObserver).onChanged(any())
        verify(closePlayerObserver).onChanged(null)
    }

    @Test
    fun `rank month`() {
        val openURLObserver: Observer<String> = mock()
        val closePlayerObserver: Observer<Void> = mock()
        viewModel.openInternalURLEvent.observeForever(openURLObserver)
        viewModel.closePlayer.observeForever(closePlayerObserver)
        viewModel.onMonthTapped()
        verify(openURLObserver).onChanged(any())
        verify(closePlayerObserver).onChanged(null)
    }

    @Test
    fun `rank all time`() {
        val openURLObserver: Observer<String> = mock()
        val closePlayerObserver: Observer<Void> = mock()
        viewModel.openInternalURLEvent.observeForever(openURLObserver)
        viewModel.closePlayer.observeForever(closePlayerObserver)
        viewModel.onAllTimeTapped()
        verify(openURLObserver).onChanged(any())
        verify(closePlayerObserver).onChanged(null)
    }

    @Test
    fun `player datasource subscription`() {
        verify(playerDataSource).subscribeToSong(any())
    }
}
