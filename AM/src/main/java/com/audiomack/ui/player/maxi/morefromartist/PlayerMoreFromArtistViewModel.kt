package com.audiomack.ui.player.maxi.morefromartist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.common.Resource
import com.audiomack.ui.common.Resource.Failure
import com.audiomack.ui.common.Resource.Loading
import com.audiomack.ui.common.Resource.Success
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibility
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibilityData
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibilityImpl
import com.audiomack.ui.player.maxi.bottom.playerTabMoreFromArtistIndex
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class PlayerMoreFromArtistViewModel(
    playerDataSource: PlayerDataSource,
    private val playerBottomVisibility: PlayerBottomVisibility = PlayerBottomVisibilityImpl.getInstance()
) : BaseViewModel() {

    private val songObserver = object : Observer<Resource<AMResultItem>> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {}

        override fun onNext(item: Resource<AMResultItem>) {
            when (item) {
                is Success -> item.data?.let {
                    onSongChanged(it)
                    if (playerBottomVisibility.tabIndex == playerTabMoreFromArtistIndex && playerBottomVisibility.tabsVisible) {
                        loadDataEvent.call()
                    }
                }
                is Loading -> {
                    item.data?.let { onSongChanged(it) }
                    showLoading.call()
                }
                is Failure -> showNoConnection.call()
            }
        }
    }

    private val visibilityObserver = object : Observer<PlayerBottomVisibilityData> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {}

        override fun onNext(data: PlayerBottomVisibilityData) {
            if (data.visibleTabIndex == playerTabMoreFromArtistIndex) {
                loadDataEvent.call()
            }
        }
    }

    private val _uploaderName = MutableLiveData<String>()
    val uploaderName: LiveData<String> get() = _uploaderName

    private var _uploaderSlug: String = ""
    val uploaderSlug: String get() = _uploaderSlug

    val loadDataEvent = SingleLiveEvent<Void>()

    val openInternalUrlEvent = SingleLiveEvent<String>()

    val showLoading = SingleLiveEvent<Void>()

    val showNoConnection = SingleLiveEvent<Void>()

    init {
        playerDataSource.subscribeToSong(songObserver)
        playerBottomVisibility.subscribe(visibilityObserver)
    }

    private fun onSongChanged(song: AMResultItem) {
        _uploaderName.postValue(song.uploaderName ?: "")
        _uploaderSlug = song.uploaderSlug ?: ""
    }

    fun onPlaceholderTapped() {
        openUploader()
    }

    fun onFooterTapped() {
        openUploader()
    }

    private fun openUploader() {
        openInternalUrlEvent.postValue("audiomack://artist/$_uploaderSlug")
    }
}
