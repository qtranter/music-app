package com.audiomack.ui.onboarding.artists

import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.onboarding.ArtistsOnboardingDataSource
import com.audiomack.data.onboarding.ArtistsOnboardingRepository
import com.audiomack.data.onboarding.OnboardingPlaylistsGenreProvider
import com.audiomack.data.onboarding.OnboardingPlaylistsGenreProviderImpl
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.OnboardingArtist
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent

class ArtistsOnboardingViewModel(
    private val artistsOnboardingDataSource: ArtistsOnboardingDataSource = ArtistsOnboardingRepository(),
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val preferencesDataSource: PreferencesDataSource = PreferencesRepository(),
    private val onboardingPlaylistsGenreProvider: OnboardingPlaylistsGenreProvider = OnboardingPlaylistsGenreProviderImpl(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    val closeEvent = SingleLiveEvent<Void>()
    val updateListEvent = SingleLiveEvent<List<OnboardingArtist>>()
    val showLoadingEvent = SingleLiveEvent<Void>()
    val hideLoadingEvent = SingleLiveEvent<Void>()
    val showHUDEvent = SingleLiveEvent<Void>()
    val hideHUDEvent = SingleLiveEvent<Void>()
    val showHUDErrorEvent = SingleLiveEvent<String>()
    val changedSelectionEvent = SingleLiveEvent<Int?>()
    val enableListenButtonEvent = SingleLiveEvent<Boolean>()
    val openTrendingEvent = SingleLiveEvent<Void>()
    val showPlaylistEvent = SingleLiveEvent<Pair<String, AMResultItem>>()

    private var items: List<OnboardingArtist> = emptyList()
    private var position: Int? = null

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onItemTapped(position: Int) {
        if (this.position != position) {
            this.position = position
        } else {
            this.position = null
        }
        changedSelectionEvent.postValue(this.position)
        enableListenButtonEvent.postValue(this.position != null)
    }

    fun onTapFooter() {
        onboardingPlaylistsGenreProvider.setOnboardingGenre(null)
        closeEvent.call()
        openTrendingEvent.call()
    }

    fun onCreate() {
        changedSelectionEvent.postValue(null)
        enableListenButtonEvent.postValue(false)
        showLoadingEvent.call()
        downloadData()
    }

    fun onRefreshTriggered() {
        downloadData()
    }

    private fun downloadData() {
        compositeDisposable.add(
            artistsOnboardingDataSource.onboardingItems()
                .map { it.shuffled() }
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    items = it
                    hideLoadingEvent.call()
                    updateListEvent.postValue(it)
                }, {
                    hideLoadingEvent.call()
                })
        )
    }

    fun onListenNowTapped() {
        position?.let {
            val item = items.getOrNull(it) ?: return
            val artist = item.artist ?: return
            showHUDEvent.call()
            compositeDisposable.add(
                musicDataSource.getPlaylistInfo(item.playlistId ?: "")
                    .subscribeOn(schedulersProvider.io)
                    .observeOn(schedulersProvider.main)
                    .subscribe({ playlist ->
                        mixpanelDataSource.trackOnboarding(artist.name ?: "", playlist.title ?: "", playlist.genre ?: "")
                        preferencesDataSource.onboardingGenre = playlist.genre
                        onboardingPlaylistsGenreProvider.setOnboardingGenre(playlist.genre)
                        hideHUDEvent.call()
                        showPlaylistEvent.postValue(Pair(if (!item.imageUrl.isNullOrEmpty()) item.imageUrl else artist.smallImage ?: "", playlist))
                    }, {
                        showHUDErrorEvent.postValue(MainApplication.context?.getString(R.string.playlist_info_failed) ?: "")
                    })
            )
        }
    }

    fun onDestroy() {
        if (preferencesDataSource.onboardingGenre == null) {
            mixpanelDataSource.trackOnboarding()
        }
    }
}
