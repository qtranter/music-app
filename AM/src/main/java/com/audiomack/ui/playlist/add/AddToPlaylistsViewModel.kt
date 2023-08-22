package com.audiomack.ui.playlist.add

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.database.MusicDAO
import com.audiomack.data.database.MusicDAOImpl
import com.audiomack.data.inapprating.InAppRating
import com.audiomack.data.inapprating.InAppRatingManager
import com.audiomack.data.playlist.PlayListDataSource
import com.audiomack.data.playlist.PlaylistRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserData
import com.audiomack.data.user.UserDataInterface
import com.audiomack.model.AMResultItem
import com.audiomack.model.AddToPlaylistModel
import com.audiomack.model.EventTrackRemoved
import com.audiomack.model.MixpanelSource
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.Observable
import org.greenrobot.eventbus.EventBus

class AddToPlaylistsViewModel(
    private val userDataInterface: UserDataInterface = UserData,
    private val playListDataSource: PlayListDataSource = PlaylistRepository(),
    private val musicDAO: MusicDAO = MusicDAOImpl(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    private val eventBus: EventBus = EventBus.getDefault(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val inAppRating: InAppRating = InAppRatingManager.getInstance()
) : BaseViewModel(), SelectPlaylistsAdapter.SelectPlaylistsAdapterListener {

    private var _progressBarVisible = MutableLiveData<Boolean>()
    val progressBarVisible: LiveData<Boolean> get() = _progressBarVisible

    val reloadAdapterPositionEvent = SingleLiveEvent<Int>()
    val addDataToAdapterEvent = SingleLiveEvent<List<AMResultItem>>()
    val hideLoadMoreEvent = SingleLiveEvent<Void>()
    val enableLoadMoreEvent = SingleLiveEvent<Void>()
    val disableLoadMoreEvent = SingleLiveEvent<Void>()

    val closeEvent = SingleLiveEvent<Void>()
    val showPlaylistsEvent = SingleLiveEvent<Void>()
    val newPlaylistEvent = SingleLiveEvent<Void>()
    val playlistCannotBeEditedEvent = SingleLiveEvent<Void>()
    val songCannotBeAddedEvent = SingleLiveEvent<Void>()
    val cannotRemoveLastTrackEvent = SingleLiveEvent<Void>()
    val addedSongEvent = SingleLiveEvent<Void>()
    val failedToAddSongEvent = SingleLiveEvent<Void>()
    val removedSongEvent = SingleLiveEvent<Void>()
    val failedToRemoveSongEvent = SingleLiveEvent<Void>()
    val failedToFetchPlaylistEvent = SingleLiveEvent<Void>()

    val mixpanelSource: MixpanelSource
        get() = data.mixpanelSource

    private var currentPage = 0
    private var downloading = false
    private var allPlaylistsWithThatSong: MutableList<AMResultItem> = mutableListOf()

    private lateinit var data: AddToPlaylistModel

    fun init(
        data: AddToPlaylistModel
    ) {
        this.data = data
    }

    fun onCreate() {
        if (userDataInterface.myPlaylistsCount == 0) {
            newPlaylistEvent.call()
        } else {
            showPlaylistsEvent.call()
        }
    }

    fun onCloseCliked() {
        closeEvent.call()
    }

    fun requestPlaylists() {
        downloadPlaylists()
    }

    // SelectPlaylistsAdapter.SelectPlaylistsAdapterListener

    override fun didTapNew() {
        newPlaylistEvent.call()
    }

    override fun didStartLoadMore() {
        if (!downloading) {
            currentPage++
            downloadPlaylists()
        }
    }

    override fun didTogglePlaylist(playlist: AMResultItem, index: Int) {

        if (playlist.addToPlaylistStatus == AMResultItem.ItemAPIStatus.Loading) {
            return
        }

        if (playlist.itemId == null) {
            playlistCannotBeEditedEvent.call()
            return
        }

        if (data.songs.isEmpty()) {
            songCannotBeAddedEvent.call()
            return
        }

        val itemIds = data.songs.map { it.id }.joinToString(",")

        if (playlist.addToPlaylistStatus == AMResultItem.ItemAPIStatus.Off) {

            playlist.addToPlaylistStatus = AMResultItem.ItemAPIStatus.Loading

            compositeDisposable.add(
                playListDataSource.addSongsToPlaylist(playlist.itemId, itemIds, mixpanelSource.page)
                    .subscribeOn(schedulersProvider.io)
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        playlist.addToPlaylistStatus = AMResultItem.ItemAPIStatus.On
                        addedSongEvent.call()
                        playlist.playlistTracksCount = playlist.playlistTracksCount + data.songs.size
                        reloadAdapterPositionEvent.postValue(index)
                        if (data.songs.size == 1) {
                            trackingDataSource.trackGA("playlist", "addSong", data.songs[0].title)
                            mixpanelDataSource.trackAddToPlaylist(data.songs[0], playlist, data.mixpanelSource, data.mixpanelButton)
                        }
                        inAppRating.request()

                        // Download tracks right away if the playlist is offline
                        compositeDisposable.add(
                            musicDAO.findById(playlist.itemId)
                                .subscribeOn(schedulersProvider.io)
                                .flatMap { playListDataSource.getPlaylistInfo(playlist.itemId) }
                                .flatMap { actionsDataSource.toggleDownload(it, data.mixpanelButton, mixpanelSource) }
                                .observeOn(schedulersProvider.main)
                                .subscribe({}, {})
                        )
                    }, {
                        playlist.addToPlaylistStatus = AMResultItem.ItemAPIStatus.Off
                        failedToAddSongEvent.call()
                    })
            )
        } else if (playlist.addToPlaylistStatus == AMResultItem.ItemAPIStatus.On && data.songs.size == 1) {

            if (playlist.playlistTracksCount == 1) {
                cannotRemoveLastTrackEvent.call()
                return
            }

            playlist.addToPlaylistStatus = AMResultItem.ItemAPIStatus.Loading

            compositeDisposable.add(
                playListDataSource.deleteSongsFromPlaylist(playlist.itemId, itemIds)
                    .subscribeOn(schedulersProvider.io)
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        playlist.addToPlaylistStatus = AMResultItem.ItemAPIStatus.Off
                        removedSongEvent.call()
                        playlist.playlistTracksCount = playlist.playlistTracksCount - data.songs.size
                        reloadAdapterPositionEvent.postValue(index)
                        eventBus.post(EventTrackRemoved(data.songs.map { it.id }))
                    }, {
                        playlist.addToPlaylistStatus = AMResultItem.ItemAPIStatus.On
                        failedToRemoveSongEvent.call()
                    })
            )
        }
    }

    // Internal

    private fun downloadPlaylists() {

        if (data.songs.isEmpty()) {
            return
        }

        downloading = true

        if (currentPage == 0) {
            _progressBarVisible.postValue(true)
        }

        val filteredPlaylistsObservable = if (data.songs.size == 1) playListDataSource.getMyPlaylists(currentPage, "all", data.songs[0].id, false) else Observable.just(emptyList())
        val allPlaylistsObservable = playListDataSource.getMyPlaylists(currentPage, "all", null, false)

        compositeDisposable.add(
            filteredPlaylistsObservable
                .flatMap {
                    allPlaylistsWithThatSong.addAll(it)
                    allPlaylistsObservable
                }
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ newPlaylists ->

                    newPlaylists.forEach { newPlaylist ->
                        userDataInterface.addPlaylistToMyPlaylists(newPlaylist.itemId)
                        val checked = allPlaylistsWithThatSong.any { it.itemId == newPlaylist.itemId }
                        newPlaylist.addToPlaylistStatus = if (checked) AMResultItem.ItemAPIStatus.On else AMResultItem.ItemAPIStatus.Off
                    }

                    addDataToAdapterEvent.postValue(newPlaylists)

                    _progressBarVisible.postValue(false)

                    if (newPlaylists.isEmpty()) {
                        disableLoadMoreEvent.call()
                    } else {
                        enableLoadMoreEvent.call()
                    }

                    downloading = false
                }, {

                    hideLoadMoreEvent.call()
                    _progressBarVisible.postValue(false)
                    if (currentPage == 0) {
                        failedToFetchPlaylistEvent.call()
                    }
                    downloading = false
                })
        )
    }
}
