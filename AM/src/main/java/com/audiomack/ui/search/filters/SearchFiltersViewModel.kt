package com.audiomack.ui.search.filters

import androidx.lifecycle.MutableLiveData
import com.audiomack.data.search.SearchDataSource
import com.audiomack.data.search.SearchRepository
import com.audiomack.model.AMGenre
import com.audiomack.model.EventSearchFiltersChanged
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import org.greenrobot.eventbus.EventBus

class SearchFiltersViewModel(
    private val searchDataSource: SearchDataSource = SearchRepository()
) : BaseViewModel() {

    private val POPULAR = "popular"
    private val RECENT = "recent"
    private val RELEVANT = "relevance"

    val close = SingleLiveEvent<Void>()

    val resetSortControls = SingleLiveEvent<Void>()
    val updateSortControls = SingleLiveEvent<Void>()
    val mostPopular = SingleLiveEvent<Void>()
    val mostRecent = SingleLiveEvent<Void>()
    val mostRelevant = SingleLiveEvent<Void>()

    val updateVerifiedOnly = MutableLiveData<Boolean>()

    val resetGenreControls = SingleLiveEvent<Void>()
    val updateGenreControls = SingleLiveEvent<Void>()
    val allGenres = SingleLiveEvent<Void>()
    val rap = SingleLiveEvent<Void>()
    val rnb = SingleLiveEvent<Void>()
    val electronic = SingleLiveEvent<Void>()
    val reggae = SingleLiveEvent<Void>()
    val rock = SingleLiveEvent<Void>()
    val pop = SingleLiveEvent<Void>()
    val afrobeats = SingleLiveEvent<Void>()
    val podcast = SingleLiveEvent<Void>()
    val latin = SingleLiveEvent<Void>()
    val instrumental = SingleLiveEvent<Void>()

    private var verifiedOnly = searchDataSource.verifiedOnly
    private var categoryCode = searchDataSource.categoryCode
    private var genreCode = searchDataSource.genreCode

    fun onCloseTapped() {
        close.call()
    }

    fun onApplyTapped() {
        searchDataSource.verifiedOnly = verifiedOnly
        searchDataSource.categoryCode = categoryCode
        searchDataSource.genreCode = genreCode

        EventBus.getDefault().post(EventSearchFiltersChanged())

        close.call()
    }

    fun onVerifiedSwitchChanged(checked: Boolean) {
        verifiedOnly = checked
    }

    fun onMostPopularSelected() {
        categoryCode = POPULAR
        resetSortControls.call()
        mostPopular.call()
        updateSortControls.call()
    }

    fun onMostRecentSelected() {
        categoryCode = RECENT
        resetSortControls.call()
        mostRecent.call()
        updateSortControls.call()
    }

    fun onMostRelevantSelected() {
        categoryCode = RELEVANT
        resetSortControls.call()
        mostRelevant.call()
        updateSortControls.call()
    }

    fun onAllGenresSelected() {
        genreCode = AMGenre.All.apiValue()
        resetGenreControls.call()
        allGenres.call()
        updateGenreControls.call()
    }

    fun onRapSelected() {
        genreCode = AMGenre.Rap.apiValue()
        resetGenreControls.call()
        rap.call()
        updateGenreControls.call()
    }

    fun onRnBSelected() {
        genreCode = AMGenre.Rnb.apiValue()
        resetGenreControls.call()
        rnb.call()
        updateGenreControls.call()
    }

    fun onElectronicSelected() {
        genreCode = AMGenre.Electronic.apiValue()
        resetGenreControls.call()
        electronic.call()
        updateGenreControls.call()
    }

    fun onReggaeSelected() {
        genreCode = AMGenre.Dancehall.apiValue()
        resetGenreControls.call()
        reggae.call()
        updateGenreControls.call()
    }

    fun onRockSelected() {
        genreCode = AMGenre.Rock.apiValue()
        resetGenreControls.call()
        rock.call()
        updateGenreControls.call()
    }

    fun onPopSelected() {
        genreCode = AMGenre.Pop.apiValue()
        resetGenreControls.call()
        pop.call()
        updateGenreControls.call()
    }

    fun onAfrobeatsSelected() {
        genreCode = AMGenre.Afrobeats.apiValue()
        resetGenreControls.call()
        afrobeats.call()
        updateGenreControls.call()
    }

    fun onPodcastSelected() {
        genreCode = AMGenre.Podcast.apiValue()
        resetGenreControls.call()
        podcast.call()
        updateGenreControls.call()
    }

    fun onLatinSelected() {
        genreCode = AMGenre.Latin.apiValue()
        resetGenreControls.call()
        latin.call()
        updateGenreControls.call()
    }

    fun onInstrumentalSelected() {
        genreCode = AMGenre.Instrumental.apiValue()
        resetGenreControls.call()
        instrumental.call()
        updateGenreControls.call()
    }

    fun onCreate() {
        when (categoryCode) {
            RECENT -> onMostRecentSelected()
            RELEVANT -> onMostRelevantSelected()
            else -> onMostPopularSelected()
        }
        when (genreCode) {
            AMGenre.Rap.apiValue() -> onRapSelected()
            AMGenre.Rnb.apiValue() -> onRnBSelected()
            AMGenre.Electronic.apiValue() -> onElectronicSelected()
            AMGenre.Dancehall.apiValue() -> onReggaeSelected()
            AMGenre.Rock.apiValue() -> onRockSelected()
            AMGenre.Pop.apiValue() -> onPopSelected()
            AMGenre.Afrobeats.apiValue() -> onAfrobeatsSelected()
            AMGenre.Podcast.apiValue() -> onPodcastSelected()
            AMGenre.Latin.apiValue() -> onLatinSelected()
            AMGenre.Instrumental.apiValue() -> onInstrumentalSelected()
            else -> onAllGenresSelected()
        }
        updateVerifiedOnly.postValue(verifiedOnly)
    }
}
