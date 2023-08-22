package com.audiomack.ui.notifications
import com.audiomack.model.AMResultItem
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent

class NotificationsViewModel : BaseViewModel() {

    val closeEvent = SingleLiveEvent<Void>()
    val showNotificationsFragmentEvent = SingleLiveEvent<Void>()
    val showPlaylistsGridEvent = SingleLiveEvent<List<AMResultItem>>()

    fun onCreate() {
        showNotificationsFragmentEvent.call()
    }

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onRequestedPlaylistsGrid(playlists: List<AMResultItem>) {
        showPlaylistsGridEvent.postValue(playlists)
    }
}
