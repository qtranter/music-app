package com.audiomack.ui.mylibrary.offline.local.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.audiomack.data.music.local.LocalMediaDataSource
import com.audiomack.data.music.local.LocalMediaRepository
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryOffline
import com.audiomack.data.tracking.mixpanel.MixpanelTabMyLibrary
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.playback.Playback
import com.audiomack.playback.PlayerPlayback
import com.audiomack.playback.PlayerQueue.Album
import com.audiomack.playback.PlayerQueue.Song
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.home.AlertManager
import com.audiomack.ui.home.AlertTriggers
import com.audiomack.ui.home.NavigationActions
import com.audiomack.ui.home.NavigationManager
import com.audiomack.ui.mylibrary.offline.local.AddLocalMediaExclusionUseCase
import com.audiomack.ui.mylibrary.offline.local.AddLocalMediaExclusionUseCaseImpl
import io.reactivex.Single
import timber.log.Timber

class SlideUpMenuLocalMediaViewModel(
    private val localMedia: LocalMediaDataSource = LocalMediaRepository.getInstance(),
    private val addLocalMediaExclusion: AddLocalMediaExclusionUseCase = AddLocalMediaExclusionUseCaseImpl(),
    private val navigation: NavigationActions = NavigationManager.getInstance(),
    private val alertTriggers: AlertTriggers = AlertManager,
    private val playback: Playback = PlayerPlayback.getInstance(),
    private val queue: QueueDataSource = QueueRepository.getInstance(),
    private val schedulers: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    private val _item = MutableLiveData<AMResultItem>()
    val item: LiveData<AMResultItem> get() = _item

    val isAlbum: LiveData<Boolean> = Transformations.map(item) { it.isAlbum }

    var id: Long? = null
        set(value) {
            field = value
            value?.let { loadItem(it) }
        }

    val mixpanelSource = MixpanelSource(MixpanelTabMyLibrary, MixpanelPageMyLibraryOffline)

    private fun loadItem(id: Long) {
        localMedia.getTrack(id)
            .onErrorResumeNext { localMedia.getAlbum(id) }
            .subscribeOn(schedulers.io)
            .subscribe { item, e ->
                e?.let {
                    Timber.tag(TAG).e(it)
                    alertTriggers.onGenericError()
                    navigation.navigateBack()
                }
                item?.let { _item.postValue(it) }
            }
            .composite()
    }

    fun onPlayNextClick() {
        addToQueue(QueueDataSource.CURRENT_INDEX)
    }

    fun onAddToQueueClick() {
        addToQueue()
    }

    fun onRemoveFromQueueClick(queueIndex: Int) {
        val removingCurrentlyPlayingItem = queue.index == queueIndex
        queue.removeAt(queueIndex)
        if (removingCurrentlyPlayingItem) {
            queue.skip(queueIndex)
        }
    }

    fun onHideClick() {
        val item = item.value ?: return

        addLocalMediaExclusion.addExclusionFrom(item)
            .subscribe { exclusions, e ->
                e?.let { alertTriggers.onGenericError() }
                exclusions?.let { Timber.tag(TAG).d("onHideClick : added ${it.size} exclusions") }
                navigation.navigateBack()
            }.composite()
    }

    private fun addToQueue(index: Int? = null) {
        val item = item.value ?: return

        Single.just(item)
            .subscribeOn(schedulers.io)
            .map {
                when {
                    it.isSong || it.isAlbumTrack -> Song(
                        item = it,
                        source = mixpanelSource,
                        inOfflineScreen = true
                    )
                    it.isAlbum -> Album(
                        album = it,
                        source = mixpanelSource,
                        inOfflineScreen = true
                    )
                    else -> throw IllegalArgumentException("Unsupported item type")
                }
            }
            .observeOn(schedulers.main)
            .doAfterSuccess { navigation.navigateBack() }
            .subscribe { queue, e ->
                e?.let { Timber.tag(TAG).e(it) }
                queue?.let {
                    Timber.tag(TAG).d("addToQueue : Adding queue $it")
                    playback.addQueue(it, index)
                }
            }
            .composite()
    }

    companion object {
        private const val TAG = "SlideUpMenuLocalMediaVM"
    }
}
