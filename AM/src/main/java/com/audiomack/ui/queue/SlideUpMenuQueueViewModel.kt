package com.audiomack.ui.queue

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.MainApplication
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonQueue
import com.audiomack.data.tracking.mixpanel.MixpanelPageQueue
import com.audiomack.model.AMResultItem
import com.audiomack.model.AddToPlaylistModel
import com.audiomack.model.MixpanelSource
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.home.AlertManager
import com.audiomack.ui.home.AlertTriggers
import com.audiomack.ui.home.NavigationActions
import com.audiomack.ui.home.NavigationManager

class SlideUpMenuQueueViewModel(
    private val queueRepo: QueueDataSource = QueueRepository.getInstance(),
    private val alerts: AlertTriggers = AlertManager,
    private val navigation: NavigationActions = NavigationManager.getInstance(),
    schedulers: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    private val _showSaveToPlaylist = MutableLiveData<Boolean>()
    val showSaveToPlaylist: LiveData<Boolean> get() = _showSaveToPlaylist

    private var playlistItems: List<AMResultItem> = listOf()

    init {
        queueRepo.orderedItems
            .subscribeOn(schedulers.computation)
            .take(1)
            .map { items -> items.filterNot { it.isLocal } }
            .observeOn(schedulers.main)
            .subscribe({
                playlistItems = it
                _showSaveToPlaylist.postValue(playlistItems.isNotEmpty())
            }, {
                alerts.onGenericError()
            })
            .composite()
    }

    private val mixpanelSource =
        MixpanelSource(MainApplication.currentTab, MixpanelPageQueue, emptyList())

    fun onSaveToPlaylistClick() {
        if (!playlistItems.isNullOrEmpty()) {
            navigation.launchAddToPlaylist(
                AddToPlaylistModel(playlistItems, mixpanelSource, MixpanelButtonQueue)
            )
        }
    }

    fun onClearAllClick() {
        queueRepo.clear()
        navigation.navigateBack()
    }

    fun onClearUpcomingClick() {
        queueRepo.clear(queueRepo.index.inc())
    }
}
