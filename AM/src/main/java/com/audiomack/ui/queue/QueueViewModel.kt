package com.audiomack.ui.queue

import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueRepository
import com.audiomack.model.AMResultItem
import com.audiomack.playback.Playback
import com.audiomack.playback.PlayerPlayback
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.Timer
import kotlin.concurrent.timerTask

class QueueViewModel(
    private val preferencesDataSource: PreferencesDataSource = PreferencesRepository(),
    val queueDataSource: QueueDataSource = QueueRepository.getInstance(),
    val playback: Playback = PlayerPlayback.getInstance()
) : BaseViewModel() {

    private val _queue = MutableLiveData<List<AMResultItem>>()
    val queue: LiveData<List<AMResultItem>> get() = _queue

    val backEvent = SingleLiveEvent<Void>()
    val refreshData = SingleLiveEvent<Void>()
    val showOptionsEvent = SingleLiveEvent<Pair<AMResultItem, Int>>()
    val showTooltip = SingleLiveEvent<Void>()
    val setCurrentSongEvent = SingleLiveEvent<Int>()
    val errorEvent = SingleLiveEvent<Throwable>()

    private var timer: Timer? = null

    @VisibleForTesting
    val queueDataObserver = object : Observer<List<AMResultItem>> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onNext(list: List<AMResultItem>) {
            _queue.postValue(list)
        }

        override fun onError(e: Throwable) {
            errorEvent.value = e
        }
    }

    @VisibleForTesting
    val queueIndexObserver = object : Observer<Int> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onNext(t: Int) {
            refreshData.call()
        }

        override fun onError(e: Throwable) {
            errorEvent.value = e
        }
    }

    init {
        queueDataSource.subscribeToOrderedList(queueDataObserver)
        queueDataSource.subscribeToIndex(queueIndexObserver)
    }

    fun didTapKebab(item: AMResultItem?, index: Int) {
        item?.let { showOptionsEvent.postValue(Pair(it, index)) }
    }

    fun didDeleteCurrentlyPlayingSong() {
        playback.next()
    }

    fun onBackTapped() {
        backEvent.call()
    }

    fun onCreate(activity: FragmentActivity) {
        if (!preferencesDataSource.queueAddToPlaylistTooltipShown) {
            timer = Timer().also {
                it.schedule(timerTask {
                    activity.runOnUiThread {
                        showTooltip.call()
                    }
                }, 500)
            }
        }
    }

    fun scrollToCurrentlyPlayingSong() {
        setCurrentSongEvent.postValue(queueDataSource.index)
    }

    fun onSongTapped(index: Int) {
        playback.skip(index)
    }

    fun onSongMoved(fromIndex: Int, toIndex: Int) {
        queueDataSource.move(fromIndex, toIndex)
    }

    fun onSongDeleted(index: Int) {
        queueDataSource.removeAt(index)
    }

    fun onTooltipClosed() {
        preferencesDataSource.queueAddToPlaylistTooltipShown = true
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
