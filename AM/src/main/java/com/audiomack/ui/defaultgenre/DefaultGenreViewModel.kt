package com.audiomack.ui.defaultgenre

import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingProvider
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.model.GenreModel
import com.audiomack.network.AnalyticsKeys
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent

class DefaultGenreViewModel(
    private val trackingRepository: TrackingDataSource = TrackingRepository(),
    private val preferencesRepository: PreferencesDataSource = PreferencesRepository()
) : BaseViewModel() {

    val back = SingleLiveEvent<Void>()
    val moreGenres = SingleLiveEvent<Void>()

    val defaultGenre = preferencesRepository.defaultGenre

    fun onBackTapped() {
        back.call()
    }

    fun onMoreGenresTapped() {
        moreGenres.call()
    }

    fun onGenreSelected(genreModel: GenreModel) {

        trackingRepository.trackEvent(
            AnalyticsKeys.EVENT_DEFAULTGENRE_SELECTION,
            hashMapOf(Pair(AnalyticsKeys.PARAM_DEFAULTGENRE_SELECTION, genreModel.leanplumKey)),
            listOf(TrackingProvider.Firebase)
        )

        preferencesRepository.defaultGenre = genreModel.genreKey

        back.call()
    }
}
