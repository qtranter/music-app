package com.audiomack.ui.playlists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.model.PlaylistCategory
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo

class PlaylistsViewModel(
    private var deeplinkSlug: String?,
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    private var categories: List<PlaylistCategory> = emptyList()
    private var downloading: Boolean = false

    val openMenuEvent = SingleLiveEvent<List<PlaylistCategory>>()

    val setupPagerEvent = SingleLiveEvent<List<PlaylistCategory>>()

    private val _loaderVisible = MutableLiveData<Boolean>()
    val loaderVisible: LiveData<Boolean> = _loaderVisible

    private val _contentVisible = MutableLiveData<Boolean>()
    val contentVisible: LiveData<Boolean> = _contentVisible

    private val _placeholderVisible = MutableLiveData<Boolean>()
    val placeholderVisible: LiveData<Boolean> = _placeholderVisible

    fun onAllCategoriesTapped() {
        openMenuEvent.postValue(categories)
    }

    fun onPlaceholderTapped() {
        downloadCategories()
    }

    fun downloadCategories() {

        if (downloading || categories.isNotEmpty()) {
            return
        }
        downloading = true

        _loaderVisible.postValue(true)
        _contentVisible.postValue(false)
        _placeholderVisible.postValue(false)

        musicDataSource.playlistCategories()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ categories ->

                deeplinkSlug?.let { tag ->
                    val fixedCategories = categories.toMutableList()

                    fixedCategories.firstOrNull { it.slug == tag }?.let { categoryBySlug ->
                        // If we can find an already existing category with the specified slug then move it in first position
                        fixedCategories.remove(categoryBySlug)
                        fixedCategories.add(0, categoryBySlug)
                    }

                    deeplinkSlug = null
                    this.categories = fixedCategories.toList()
                } ?: run {
                    this.categories = categories
                }

                setupPagerEvent.postValue(this.categories)
                _loaderVisible.postValue(false)
                _contentVisible.postValue(true)
                _placeholderVisible.postValue(false)

                downloading = false
            }, {
                _loaderVisible.postValue(false)
                _contentVisible.postValue(false)
                _placeholderVisible.postValue(true)

                downloading = false
            }).addTo(compositeDisposable)
    }
}
