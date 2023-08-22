package com.audiomack.ui.mylibrary.offline.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.music.local.LocalMediaDataSource
import com.audiomack.data.music.local.LocalMediaRepository
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.model.AMResultItem
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.home.AlertManager
import com.audiomack.ui.home.AlertTriggers
import com.audiomack.ui.home.NavigationActions
import com.audiomack.ui.home.NavigationManager
import com.audiomack.utils.addTo
import io.reactivex.Completable

class LocalMediaSelectionViewModel(
    private val localMediaDataSource: LocalMediaDataSource = LocalMediaRepository.getInstance(),
    private val localMediaExclusionsDataSource: LocalMediaExclusionsDataSource = LocalMediaExclusionsRepository.getInstance(),
    private val preferencesDataSource: PreferencesDataSource = PreferencesRepository(),
    private val navigationActions: NavigationActions = NavigationManager.getInstance(),
    private val alertTriggers: AlertTriggers = AlertManager,
    private val tracking: TrackingDataSource = TrackingRepository(),
    private val schedulers: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    private val _items = MutableLiveData<List<AMResultItem>>()
    val items: LiveData<List<AMResultItem>> get() = _items

    private val _exclusions = MutableLiveData<List<Long>>()
    val exclusions: LiveData<List<Long>> get() = _exclusions

    private val _showEmptyView = MutableLiveData<Boolean>()
    val showEmptyView: LiveData<Boolean> get() = _showEmptyView

    init {
        loadItems()
        loadExclusions()
        updateShownPreference()
    }

    fun onSaveExclusionsClick(exclusionIds: List<Long>) {
        localMediaExclusionsDataSource.save(exclusionIds)
            .flatMapCompletable { updateIncludeLocalPreference() }
            .observeOn(schedulers.main)
            .doOnError { tracking.trackException(it) }
            .doOnComplete { tracking.trackBreadcrumb("Saved ${exclusionIds.size} local media exclusions") }
            .subscribe({
                alertTriggers.onLocalFilesSelectionSuccess()
                navigationActions.navigateBack()
            }, {
                alertTriggers.onGenericError()
            })
            .addTo(compositeDisposable)
    }

    fun onCloseClick() {
        navigationActions.navigateBack()
    }

    fun onStoragePermissionDenied() {
        alertTriggers.onStoragePermissionDenied()
        navigationActions.navigateBack()
    }

    private fun loadItems() {
        localMediaDataSource.allTracks
            .subscribe {
                _items.postValue(it)
                _showEmptyView.postValue(it.isEmpty())
            }
            .addTo(compositeDisposable)
    }

    private fun loadExclusions() {
        localMediaExclusionsDataSource.exclusionsObservable
            .map { items -> items.map { it.mediaId } }
            .subscribe { ids -> _exclusions.postValue(ids) }
            .addTo(compositeDisposable)
    }

    private fun updateShownPreference() {
        Completable.fromRunnable {
            preferencesDataSource.localFileSelectionShown = true
        }.subscribeOn(schedulers.io).subscribe().composite()
    }

    private fun updateIncludeLocalPreference() = Completable.fromRunnable {
        preferencesDataSource.includeLocalFiles = true
    }

    fun onRefresh() {
        localMediaDataSource.refresh()
    }
}
