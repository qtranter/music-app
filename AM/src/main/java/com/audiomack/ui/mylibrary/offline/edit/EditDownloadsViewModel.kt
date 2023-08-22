package com.audiomack.ui.mylibrary.offline.edit

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.download.AMMusicDownloader
import com.audiomack.download.MusicDownloader
import com.audiomack.model.AMMusicType
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItemSort
import com.audiomack.model.EventDeletedDownload
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.mylibrary.offline.local.AddLocalMediaExclusionUseCase
import com.audiomack.ui.mylibrary.offline.local.AddLocalMediaExclusionUseCaseImpl
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import io.reactivex.Observable
import org.greenrobot.eventbus.EventBus

class EditDownloadsViewModel(
    private val musicRepo: MusicDataSource = MusicRepository(),
    private val musicDownloader: MusicDownloader = AMMusicDownloader.getInstance(),
    private val addLocalMediaExclusion: AddLocalMediaExclusionUseCase = AddLocalMediaExclusionUseCaseImpl(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val eventBus: EventBus = EventBus.getDefault()
) : BaseViewModel() {

    val closeEvent = SingleLiveEvent<Void>()
    val showMusicListEvent = SingleLiveEvent<List<AMResultItem>>()
    val removeSelectedMusicEvent = SingleLiveEvent<Void>()

    private var _removeButtonEnabled = MutableLiveData<Boolean>()
    val removeButtonEnabled: LiveData<Boolean> get() = _removeButtonEnabled

    private var musicToBeRemoved = listOf<AMResultItem>()

    init {
        musicRepo.getOfflineItems(AMMusicType.All, AMResultItemSort.OldestFirst)
            .subscribeOn(schedulersProvider.io)
            .map { savedItems ->
                savedItems.filter { !musicDownloader.isMusicBeingDownloaded(it) && !musicDownloader.isMusicWaitingForDownload(it) }
            }
            .observeOn(schedulersProvider.main)
            .subscribe({
                showMusicListEvent.postValue(it)
            }, {})
            .addTo(compositeDisposable)
    }

    fun onCloseButtonClick() {
        closeEvent.call()
    }

    fun onRemoveButtonClick() {
        Observable.fromArray(musicToBeRemoved)
            .subscribeOn(schedulersProvider.io)
            .map { selectedItems ->
                val groups = selectedItems.groupBy { it.isLocal }
                groups[true]?.let { exclusions ->
                    addLocalMediaExclusion.addExclusionsFrom(exclusions).blockingGet()
                }
                groups[false]?.takeIf { it.isNotEmpty() } ?: listOf()
            }
            .flatMapIterable { it }
            .flatMapCompletable { musicRepo.deleteMusicFromDB(it) }
            .observeOn(schedulersProvider.main)
            .subscribe({
                musicToBeRemoved = emptyList()
                removeSelectedMusicEvent.call()
                eventBus.post(EventDownloadsEdited())
            }, {})
            .addTo(compositeDisposable)
    }

    // When removing the last item with a swipe, the DB operation is slower and the event may not get triggered in time. That's why we don't dispose this.
    @SuppressLint("CheckResult")
    fun onMusicRemoved(music: AMResultItem) {
        musicRepo.deleteMusicFromDB(music)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                eventBus.post(EventDeletedDownload(music))
            }, {})
    }

    fun onSelectionChanged(musicList: List<AMResultItem>) {
        _removeButtonEnabled.postValue(musicList.isNotEmpty())
        musicToBeRemoved = musicList
    }

    fun onDownloadsCompletelyRemoved() {
        closeEvent.call()
    }
}
