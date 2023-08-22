package com.audiomack.ui.player.maxi.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.player.PlayerRepository
import com.audiomack.model.AMGenre
import com.audiomack.model.AMResultItem
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.common.Resource
import com.audiomack.ui.common.Resource.Loading
import com.audiomack.ui.common.Resource.Success
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class PlayerInfoViewModel(
    playerDataSource: PlayerDataSource = PlayerRepository.getInstance()
) : BaseViewModel() {

    private val songObserver = object : Observer<Resource<AMResultItem>> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {}

        override fun onNext(item: Resource<AMResultItem>) {
            if (item is Success) {
                item.data?.let { onSongChanged(it) }
            } else if (item is Loading) {
                item.data?.let { onSongChanged(it) }
            }
        }
    }

    private val _tags = MutableLiveData<List<String>>()
    val tags: LiveData<List<String>> get() = _tags

    private val _totalPlays = MutableLiveData<String>()
    val totalPlays: LiveData<String> get() = _totalPlays

    private val _album = MutableLiveData<String>()
    val album: LiveData<String> get() = _album

    private val _producer = MutableLiveData<String>()
    val producer: LiveData<String> get() = _producer

    private val _addedOn = MutableLiveData<String>()
    val addedOn: LiveData<String> get() = _addedOn

    private val _genre = MutableLiveData<String>()
    val genre: LiveData<String> get() = _genre

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> get() = _description

    private val _descriptionExpanded = MutableLiveData<Boolean>()
    val descriptionExpanded: LiveData<Boolean> get() = _descriptionExpanded

    private val _rankVisible = MutableLiveData<Boolean>()
    val rankVisible: LiveData<Boolean> get() = _rankVisible

    private val _rankToday = MutableLiveData<String>()
    val rankToday: LiveData<String> get() = _rankToday

    private val _rankWeek = MutableLiveData<String>()
    val rankWeek: LiveData<String> get() = _rankWeek

    private val _rankMonth = MutableLiveData<String>()
    val rankMonth: LiveData<String> get() = _rankMonth

    private val _rankAllTime = MutableLiveData<String>()
    val rankAllTime: LiveData<String> get() = _rankAllTime

    /**
     * Emits the search string to be used to find music relevant to a tag
     */
    val searchTagEvent = SingleLiveEvent<String>()

    val openInternalURLEvent = SingleLiveEvent<String>()

    val closePlayer = SingleLiveEvent<Void>()

    init {
        playerDataSource.subscribeToSong(songObserver)
    }

    private fun onSongChanged(song: AMResultItem) {
        _tags.postValue(song.tags.toList())
        _totalPlays.postValue(if (song.hasStats()) song.playsExtended else "")
        _album.postValue(song.album ?: "")
        _producer.postValue(song.producer ?: "")
        _addedOn.postValue(song.released ?: "")
        _genre.postValue(AMGenre.fromApiValue(song.genre).humanValue(null))
        _description.postValue(song.desc ?: "")
        _descriptionExpanded.postValue((song.desc ?: "").length < 100)
        _rankVisible.postValue(song.hasStats())
        _rankToday.postValue(song.rankDaily ?: "")
        _rankWeek.postValue(song.rankWeekly ?: "")
        _rankMonth.postValue(song.rankMonthly ?: "")
        _rankAllTime.postValue(song.rankAllTime ?: "")
    }

    fun onDescriptionReadMoreTapped() {
        _descriptionExpanded.postValue(true)
    }

    fun onTodayTapped() {
        openInternalURLEvent.postValue("audiomack://music_songs")
        closePlayer.call()
    }

    fun onWeekTapped() {
        openInternalURLEvent.postValue("audiomack://music_songs")
        closePlayer.call()
    }

    fun onMonthTapped() {
        openInternalURLEvent.postValue("audiomack://music_songs")
        closePlayer.call()
    }

    fun onAllTimeTapped() {
        openInternalURLEvent.postValue("audiomack://music_songs")
        closePlayer.call()
    }
}
