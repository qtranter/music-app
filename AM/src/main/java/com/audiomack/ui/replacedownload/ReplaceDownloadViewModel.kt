package com.audiomack.ui.replacedownload

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.database.MusicDAOException
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItemSort
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.model.PremiumDownloadInfoModel
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import io.reactivex.Completable
import io.reactivex.Observable
import org.greenrobot.eventbus.EventBus

class ReplaceDownloadViewModel(
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val eventBus: EventBus = EventBus.getDefault()
) : BaseViewModel(), ReplaceDownloadAdapter.ReplaceDownloadListener {

    private var selectedItems: ArrayList<AMResultItem> = ArrayList()

    private lateinit var data: PremiumDownloadModel

    sealed class PendingActionAfterLogin {
        object Download : PendingActionAfterLogin()
    }

    val openDownloadsEvent = SingleLiveEvent<Void>()
    val closeEvent = SingleLiveEvent<Void>()
    val showHUDEvent = SingleLiveEvent<ProgressHUDMode>()
    val showUnlockedToastEvent = SingleLiveEvent<String>()

    private val _subtitleText = MutableLiveData<Int>()
    val subtitleText: LiveData<Int> = _subtitleText

    private val _replaceTextData = MutableLiveData<PremiumDownloadInfoModel>()
    val replaceTextData: LiveData<PremiumDownloadInfoModel> = _replaceTextData

    private val _items = MutableLiveData<List<AMResultItem>>()
    val items: LiveData<List<AMResultItem>> = _items

    private val _itemsSelected = MutableLiveData<List<AMResultItem>>()
    val itemsSelected: LiveData<List<AMResultItem>> = _itemsSelected

    fun init(
        data: PremiumDownloadModel
    ) {
        this.data = data
        loadTracks()
    }

    private fun loadTracks() {
        val music = data.music ?: return
        showHUDEvent.postValue(ProgressHUDMode.Loading)
        musicDataSource.savedPremiumLimitedUnfrozenTracks(AMResultItemSort.OldestFirst)
            .subscribeOn(schedulersProvider.io)
            .map { unfrozenTracks ->
                unfrozenTracks.filter { !music.albumTracksIds.contains(it.itemId) }
            }
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                _items.postValue(result)
                _replaceTextData.postValue(PremiumDownloadInfoModel(selectedItems.size, this.data.stats.replaceCount(this.data.music?.countOfSongsToBeDownloaded ?: 0)))
                _subtitleText.postValue(this.data.stats.replaceCount(this.data.music?.countOfSongsToBeDownloaded ?: 0))
            }, {
                showHUDEvent.postValue(ProgressHUDMode.Failure(MainApplication.context?.getString(R.string.restoredownlods_noresults_placeholder) ?: ""))
            }).addTo(compositeDisposable)
    }

    fun onCloseClick() {
        closeEvent.call()
    }

    fun onReplaceClick() {
        val music = data.music ?: return

        showHUDEvent.postValue(ProgressHUDMode.Loading)
        musicDataSource.getOfflineItem(music.musicId)
            .subscribeOn(schedulersProvider.io)
            .doOnSuccess { it.loadTracks() }
            .observeOn(schedulersProvider.main)
            .subscribe({ localMusic ->
                val idsToBeUnfreezed = localMusic.tracks?.mapNotNull { it.itemId } ?: listOf(localMusic.itemId)
                Completable.concat(selectedItems.map { musicDataSource.deleteMusicFromDB(it) })
                    .subscribeOn(schedulersProvider.io)
                    .andThen(musicDataSource.markFrozenDownloads(false, idsToBeUnfreezed))
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        showUnlockedToastEvent.postValue(localMusic.title ?: "")
                        showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                        closeEvent.call()
                        eventBus.post(EventDownloadsEdited())
                    }, {
                        showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    })
                    .addTo(compositeDisposable)
            }, {
                if (it is MusicDAOException) {
                    musicDataSource.getMusicInfo(music.musicId, music.type.typeForMusicApi)
                        .subscribeOn(schedulersProvider.io)
                        .flatMap { remoteMusic -> Completable.concat(selectedItems.map { musicDataSource.deleteMusicFromDB(it) }).andThen(Observable.just(remoteMusic)) }
                        .flatMap { remoteMusic -> actionsDataSource.toggleDownload(remoteMusic, data.stats.mixpanelButton, data.stats.mixpanelSource) }
                        .observeOn(schedulersProvider.main)
                        .subscribe({ result ->
                            if (result is ToggleDownloadResult.DownloadStarted) {
                                showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                                closeEvent.call()
                                eventBus.post(EventDownloadsEdited())
                            }
                        }, {
                            showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                        })
                        .addTo(compositeDisposable)
                } else {
                    showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                }
            })
            .addTo(compositeDisposable)
    }

    override fun onSongClick(song: AMResultItem, isSelected: Boolean) {
        val countOfSongsToBeDownloaded = data.music?.countOfSongsToBeDownloaded ?: 0
        if (countOfSongsToBeDownloaded == 1) {
            selectedItems.clear()
            if (!isSelected) selectedItems.add(song)
        } else {
            when {
                isSelected -> selectedItems.remove(song)
                countOfSongsToBeDownloaded >= selectedItems.size -> selectedItems.add(song)
                else -> return
            }
        }
        _itemsSelected.postValue(selectedItems)
        val info = PremiumDownloadInfoModel(selectedItems.size, this.data.stats.replaceCount(countOfSongsToBeDownloaded))
        _replaceTextData.postValue(info)
    }
}
