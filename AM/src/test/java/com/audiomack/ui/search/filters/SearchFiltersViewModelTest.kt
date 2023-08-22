package com.audiomack.ui.search.filters

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.search.SearchDataSource
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class SearchFiltersViewModelTest {

    @Mock
    private lateinit var searchDataSource: SearchDataSource

    private lateinit var viewModel: SearchFiltersViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        viewModel = SearchFiltersViewModel(searchDataSource)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observerClose: Observer<Void> = mock()
        viewModel.close.observeForever(observerClose)

        viewModel.onCloseTapped()

        verify(observerClose).onChanged(null)
    }

    @Test
    fun apply() {
        val observerClose: Observer<Void> = mock()
        viewModel.close.observeForever(observerClose)

        viewModel.onApplyTapped()

        verify(observerClose).onChanged(null)
    }

    @Test
    fun `most recent sorting`() {
        val observerResetSortControls: Observer<Void> = mock()
        viewModel.resetSortControls.observeForever(observerResetSortControls)

        val observerUpdateSortControls: Observer<Void> = mock()
        viewModel.updateSortControls.observeForever(observerUpdateSortControls)

        val observerRecent: Observer<Void> = mock()
        viewModel.mostRecent.observeForever(observerRecent)

        val observerRelevant: Observer<Void> = mock()
        viewModel.mostRelevant.observeForever(observerRelevant)

        val observerPopular: Observer<Void> = mock()
        viewModel.mostPopular.observeForever(observerPopular)

        viewModel.onMostRecentSelected()

        verify(observerResetSortControls).onChanged(null)
        verify(observerRecent).onChanged(null)
        verifyNoMoreInteractions(observerRelevant)
        verifyNoMoreInteractions(observerPopular)
        verify(observerUpdateSortControls).onChanged(null)
    }

    @Test
    fun `most relevant sorting`() {
        val observerResetSortControls: Observer<Void> = mock()
        viewModel.resetSortControls.observeForever(observerResetSortControls)

        val observerUpdateSortControls: Observer<Void> = mock()
        viewModel.updateSortControls.observeForever(observerUpdateSortControls)

        val observerRecent: Observer<Void> = mock()
        viewModel.mostRecent.observeForever(observerRecent)

        val observerRelevant: Observer<Void> = mock()
        viewModel.mostRelevant.observeForever(observerRelevant)

        val observerPopular: Observer<Void> = mock()
        viewModel.mostPopular.observeForever(observerPopular)

        viewModel.onMostRelevantSelected()

        verify(observerResetSortControls).onChanged(null)
        verifyNoMoreInteractions(observerRecent)
        verify(observerRelevant).onChanged(null)
        verifyNoMoreInteractions(observerPopular)
        verify(observerUpdateSortControls).onChanged(null)
    }

    @Test
    fun `most popular sorting`() {
        val observerResetSortControls: Observer<Void> = mock()
        viewModel.resetSortControls.observeForever(observerResetSortControls)

        val observerUpdateSortControls: Observer<Void> = mock()
        viewModel.updateSortControls.observeForever(observerUpdateSortControls)

        val observerRecent: Observer<Void> = mock()
        viewModel.mostRecent.observeForever(observerRecent)

        val observerRelevant: Observer<Void> = mock()
        viewModel.mostRelevant.observeForever(observerRelevant)

        val observerPopular: Observer<Void> = mock()
        viewModel.mostPopular.observeForever(observerPopular)

        viewModel.onMostPopularSelected()

        verify(observerResetSortControls).onChanged(null)
        verifyNoMoreInteractions(observerRecent)
        verifyNoMoreInteractions(observerRelevant)
        verify(observerPopular).onChanged(null)
        verify(observerUpdateSortControls).onChanged(null)
    }

    @Test
    fun `on create`() {
        val observerVerifiedOnly: Observer<Boolean> = mock()
        viewModel.updateVerifiedOnly.observeForever(observerVerifiedOnly)

        val observerUpdateSortControls: Observer<Void> = mock()
        viewModel.updateSortControls.observeForever(observerUpdateSortControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        viewModel.onCreate()

        verify(observerVerifiedOnly).onChanged(any())
        verify(observerUpdateSortControls).onChanged(null)
        verify(observerUpdateGenreControls).onChanged(null)
    }

    @Test
    fun `all genres`() {
        val observerResetGenreControls: Observer<Void> = mock()
        viewModel.resetGenreControls.observeForever(observerResetGenreControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        val observerAllGenres: Observer<Void> = mock()
        viewModel.allGenres.observeForever(observerAllGenres)

        val observerElectronic: Observer<Void> = mock()
        viewModel.electronic.observeForever(observerElectronic)

        viewModel.onAllGenresSelected()

        verify(observerResetGenreControls).onChanged(null)
        verify(observerAllGenres).onChanged(null)
        verifyNoMoreInteractions(observerElectronic)
        verify(observerUpdateGenreControls).onChanged(null)
    }

    @Test
    fun `rap genre`() {
        val observerResetGenreControls: Observer<Void> = mock()
        viewModel.resetGenreControls.observeForever(observerResetGenreControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        val observerAllGenres: Observer<Void> = mock()
        viewModel.allGenres.observeForever(observerAllGenres)

        val observerRap: Observer<Void> = mock()
        viewModel.rap.observeForever(observerRap)

        viewModel.onRapSelected()

        verify(observerResetGenreControls).onChanged(null)
        verifyNoMoreInteractions(observerAllGenres)
        verify(observerRap).onChanged(null)
        verify(observerUpdateGenreControls).onChanged(null)
    }

    @Test
    fun `rnb genre`() {
        val observerResetGenreControls: Observer<Void> = mock()
        viewModel.resetGenreControls.observeForever(observerResetGenreControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        val observerAllGenres: Observer<Void> = mock()
        viewModel.allGenres.observeForever(observerAllGenres)

        val observerRnb: Observer<Void> = mock()
        viewModel.rnb.observeForever(observerRnb)

        viewModel.onRnBSelected()

        verify(observerResetGenreControls).onChanged(null)
        verifyNoMoreInteractions(observerAllGenres)
        verify(observerRnb).onChanged(null)
        verify(observerUpdateGenreControls).onChanged(null)
    }

    @Test
    fun `electronic genre`() {
        val observerResetGenreControls: Observer<Void> = mock()
        viewModel.resetGenreControls.observeForever(observerResetGenreControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        val observerAllGenres: Observer<Void> = mock()
        viewModel.allGenres.observeForever(observerAllGenres)

        val observerElectronic: Observer<Void> = mock()
        viewModel.electronic.observeForever(observerElectronic)

        viewModel.onElectronicSelected()

        verify(observerResetGenreControls).onChanged(null)
        verifyNoMoreInteractions(observerAllGenres)
        verify(observerElectronic).onChanged(null)
        verify(observerUpdateGenreControls).onChanged(null)
    }

    @Test
    fun `reggae genre`() {
        val observerResetGenreControls: Observer<Void> = mock()
        viewModel.resetGenreControls.observeForever(observerResetGenreControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        val observerAllGenres: Observer<Void> = mock()
        viewModel.allGenres.observeForever(observerAllGenres)

        val observerReggae: Observer<Void> = mock()
        viewModel.reggae.observeForever(observerReggae)

        viewModel.onReggaeSelected()

        verify(observerResetGenreControls).onChanged(null)
        verifyNoMoreInteractions(observerAllGenres)
        verify(observerReggae).onChanged(null)
        verify(observerUpdateGenreControls).onChanged(null)
    }

    @Test
    fun `rock genre`() {
        val observerResetGenreControls: Observer<Void> = mock()
        viewModel.resetGenreControls.observeForever(observerResetGenreControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        val observerAllGenres: Observer<Void> = mock()
        viewModel.allGenres.observeForever(observerAllGenres)

        val observerRock: Observer<Void> = mock()
        viewModel.rock.observeForever(observerRock)

        viewModel.onRockSelected()

        verify(observerResetGenreControls).onChanged(null)
        verifyNoMoreInteractions(observerAllGenres)
        verify(observerRock).onChanged(null)
        verify(observerUpdateGenreControls).onChanged(null)
    }

    @Test
    fun `pop genre`() {
        val observerResetGenreControls: Observer<Void> = mock()
        viewModel.resetGenreControls.observeForever(observerResetGenreControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        val observerAllGenres: Observer<Void> = mock()
        viewModel.allGenres.observeForever(observerAllGenres)

        val observerPop: Observer<Void> = mock()
        viewModel.pop.observeForever(observerPop)

        viewModel.onPopSelected()

        verify(observerResetGenreControls).onChanged(null)
        verifyNoMoreInteractions(observerAllGenres)
        verify(observerPop).onChanged(null)
        verify(observerUpdateGenreControls).onChanged(null)
    }

    @Test
    fun `afrobeats genre`() {
        val observerResetGenreControls: Observer<Void> = mock()
        viewModel.resetGenreControls.observeForever(observerResetGenreControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        val observerAllGenres: Observer<Void> = mock()
        viewModel.allGenres.observeForever(observerAllGenres)

        val observerAfrobeats: Observer<Void> = mock()
        viewModel.afrobeats.observeForever(observerAfrobeats)

        viewModel.onAfrobeatsSelected()

        verify(observerResetGenreControls).onChanged(null)
        verifyNoMoreInteractions(observerAllGenres)
        verify(observerAfrobeats).onChanged(null)
        verify(observerUpdateGenreControls).onChanged(null)
    }

    @Test
    fun `podcast genre`() {
        val observerResetGenreControls: Observer<Void> = mock()
        viewModel.resetGenreControls.observeForever(observerResetGenreControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        val observerAllGenres: Observer<Void> = mock()
        viewModel.allGenres.observeForever(observerAllGenres)

        val observerPodcast: Observer<Void> = mock()
        viewModel.podcast.observeForever(observerPodcast)

        viewModel.onPodcastSelected()

        verify(observerResetGenreControls).onChanged(null)
        verifyNoMoreInteractions(observerAllGenres)
        verify(observerPodcast).onChanged(null)
        verify(observerUpdateGenreControls).onChanged(null)
    }

    @Test
    fun `latin genre`() {
        val observerResetGenreControls: Observer<Void> = mock()
        viewModel.resetGenreControls.observeForever(observerResetGenreControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        val observerAllGenres: Observer<Void> = mock()
        viewModel.allGenres.observeForever(observerAllGenres)

        val observerLatin: Observer<Void> = mock()
        viewModel.latin.observeForever(observerLatin)

        viewModel.onLatinSelected()

        verify(observerResetGenreControls).onChanged(null)
        verifyNoMoreInteractions(observerAllGenres)
        verify(observerLatin).onChanged(null)
        verify(observerUpdateGenreControls).onChanged(null)
    }

    @Test
    fun `instrumental genre`() {
        val observerResetGenreControls: Observer<Void> = mock()
        viewModel.resetGenreControls.observeForever(observerResetGenreControls)

        val observerUpdateGenreControls: Observer<Void> = mock()
        viewModel.updateGenreControls.observeForever(observerUpdateGenreControls)

        val observerAllGenres: Observer<Void> = mock()
        viewModel.allGenres.observeForever(observerAllGenres)

        val observerInstrumental: Observer<Void> = mock()
        viewModel.instrumental.observeForever(observerInstrumental)

        viewModel.onInstrumentalSelected()

        verify(observerResetGenreControls).onChanged(null)
        verifyNoMoreInteractions(observerAllGenres)
        verify(observerInstrumental).onChanged(null)
        verify(observerUpdateGenreControls).onChanged(null)
    }
}
