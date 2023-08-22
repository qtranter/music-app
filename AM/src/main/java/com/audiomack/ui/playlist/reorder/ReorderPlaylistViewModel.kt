package com.audiomack.ui.playlist.reorder

import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.api.MusicDataSource
import com.audiomack.model.AMPlaylistTracks
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventPlaylistEdited
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import org.greenrobot.eventbus.EventBus

class ReorderPlaylistViewModel(
    private val playlist: AMResultItem,
    private val schedulersProvider: SchedulersProvider,
    private val musicDataSource: MusicDataSource
) : BaseViewModel() {

    sealed class ReorderPlaylistLoadingStatus {
        object Loading : ReorderPlaylistLoadingStatus()
        class Success(val message: String) : ReorderPlaylistLoadingStatus()
        class Error(val message: String) : ReorderPlaylistLoadingStatus()
    }

    val showTracksEvent = SingleLiveEvent<MutableList<AMResultItem>>()
    val closeEvent = SingleLiveEvent<Void>()
    val loadingEvent = SingleLiveEvent<ReorderPlaylistLoadingStatus >()

    fun onCreate() {
        val data = mutableListOf<AMResultItem>().apply { addAll(playlist.tracks ?: ArrayList()) }
        showTracksEvent.postValue(data)
    }

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onSaveTapped(tracks: List<AMResultItem>) {
        loadingEvent.postValue(ReorderPlaylistLoadingStatus.Loading)
        compositeDisposable.add(
            musicDataSource.reorderPlaylist(
                playlist.itemId,
                playlist.title ?: "",
                playlist.genre ?: "",
                playlist.desc ?: "",
                playlist.isPrivatePlaylist,
                tracks.joinToString(",") { it.itemId })
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ updatedPlaylist ->
                    MainApplication.playlist = updatedPlaylist
                    if (updatedPlaylist.isDownloaded) {
                        AMPlaylistTracks.savePlaylist(updatedPlaylist)
                        playlist.updatePlaylist(updatedPlaylist)
                    }
                    EventBus.getDefault().post(EventPlaylistEdited(updatedPlaylist))
                    loadingEvent.postValue(ReorderPlaylistLoadingStatus.Success(MainApplication.context?.getString(R.string.edit_playlist_success, updatedPlaylist.title) ?: ""))
                    closeEvent.call()
                }, {
                    loadingEvent.postValue(ReorderPlaylistLoadingStatus.Error(MainApplication.context?.getString(R.string.edit_playlist_error) ?: ""))
                })
        )
    }
}
