package com.audiomack.data.player

import androidx.annotation.VisibleForTesting
import com.audiomack.data.database.MusicDAO
import com.audiomack.data.database.MusicDAOImpl
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.model.NextPageData
import com.audiomack.network.API
import com.audiomack.network.reportUnplayable
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.common.Resource
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber

interface PlayerDataSource {
    val currentSong: AMResultItem?

    fun loadSong(item: AMResultItem)
    fun unloadSong(nextItem: AMResultItem)
    fun subscribeToSong(observer: Observer<Resource<AMResultItem>>)

    fun loadUrl(song: AMResultItem, skipSession: Boolean, notify: Boolean)
    fun subscribeToUrl(observer: Observer<Resource<Pair<AMResultItem, String>>>)

    fun dbFindById(itemId: String): Observable<Resource<AMResultItem>>

    fun getNextPage(nextPageData: NextPageData): Observable<AMResultItem>
    fun getAllPages(nextPageData: NextPageData): Observable<AMResultItem>

    fun trackMonetizedPlay(song: AMResultItem)

    fun reportUnplayable(item: AMResultItem): Completable

    fun release()
}

class PlayerRepository private constructor(
    private val api: API,
    private val musicDao: MusicDAO,
    private val schedulersProvider: SchedulersProvider
) : PlayerDataSource {

    private val songSubject = BehaviorSubject.create<Resource<AMResultItem>>()
    private var urlSubject: Subject<Resource<Pair<AMResultItem, String>>> = BehaviorSubject.create()

    private var songInfoDisposable: Disposable? = null
    private var urlDisposable: Disposable? = null
    private var updateOfflineSongDisposable: Disposable? = null

    override val currentSong: AMResultItem?
        get() = songSubject.value?.data

    override fun loadSong(item: AMResultItem) {
        Timber.tag(TAG).d("loadSong: $item")
        songInfoDisposable?.dispose()

        songInfoDisposable = api.getSongInfo(item.itemId)
            .subscribeOn(schedulersProvider.io)
            .doOnSubscribe { songSubject.onNext(Resource.Loading(item)) }
            .observeOn(schedulersProvider.main)
            .doOnNext { Timber.tag(TAG).d("getSongInfo: got $it") }
            .map { mergeItems(item, it) }
            .map<Resource<AMResultItem>> { Resource.Success(it) }
            .doOnError { Timber.tag(TAG).w(it) }
            .onErrorReturn { Resource.Failure(it, item) }
            .subscribe {
                it.takeIf { it is Resource.Success }?.data?.let { freshItem ->
                    updateOfflineSongData(freshItem)
                }
                songSubject.onNext(it)
            }
    }

    override fun unloadSong(nextItem: AMResultItem) {
        Timber.tag(TAG).i("unloadSong() called")
        songInfoDisposable?.dispose()
        songSubject.onNext(Resource.Loading(nextItem))
    }

    override fun dbFindById(itemId: String): Observable<Resource<AMResultItem>> =
        musicDao.findById(itemId)
            .subscribeOn(schedulersProvider.io)
            .map<Resource<AMResultItem>> { Resource.Success(it) }
            .onErrorReturn { Resource.Failure(it) }

    /**
     * Here we can update offline songs with the fresh data from the APIs
     */
    private fun updateOfflineSongData(dataItem: AMResultItem) {
        updateOfflineSongDisposable?.dispose()
        updateOfflineSongDisposable = musicDao.updateSongWithFreshData(dataItem)
            .subscribeOn(schedulersProvider.io)
            .subscribe()
    }

    /**
     * Queue items contain metadata not stored in the API or database, so we copy those fields here
     */
    private fun mergeItems(queueItem: AMResultItem, dataItem: AMResultItem) = dataItem.apply {
        type = queueItem.type
        album = queueItem.album
        parentId = queueItem.parentId
        playlist = queueItem.playlist
        queueItem.mixpanelSource?.let { mixpanelSource = it }
    }

    override fun subscribeToSong(observer: Observer<Resource<AMResultItem>>) {
        songSubject.observeOn(schedulersProvider.main)
            .subscribe(observer)
    }

    override fun loadUrl(song: AMResultItem, skipSession: Boolean, notify: Boolean) {
        Timber.tag(TAG).d("loadUrl: song = $song, skipSession = $skipSession, notify = $notify")
        urlDisposable?.dispose()

        val getUrl = if (song.isPlaylistTrack && !song.parentId.isNullOrEmpty()) {
            api.getStreamURLForPlaylistWithSession(song.parentId, song.itemId, skipSession, song.mixpanelSource?.page ?: MixpanelSource.empty.page, song.extraKey)
        } else if (song.isAlbumTrack && !song.parentId.isNullOrEmpty()) {
            api.getStreamURLForAlbumWithSession(song.parentId, song.itemId, skipSession, song.mixpanelSource?.page ?: MixpanelSource.empty.page, song.extraKey)
        } else {
            api.getStreamURLWithSession(song.itemId, skipSession, song.mixpanelSource?.page ?: MixpanelSource.empty.page, song.extraKey)
        }

        urlDisposable = getUrl.subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .doOnError { Timber.tag(TAG).w(it) }
            .subscribe({
                Timber.tag(TAG).d("loadUrl: success")
                if (notify) urlSubject.onNext(Resource.Success(Pair(song, it)))
            }, {
                Timber.tag(TAG).e(it, "loadUrl: failure")
                if (notify) urlSubject.onNext(Resource.Failure(it, Pair(song, "")))
            })
    }

    override fun subscribeToUrl(observer: Observer<Resource<Pair<AMResultItem, String>>>) {
        urlSubject.subscribe(observer)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getNextPage(nextPageData: NextPageData): Observable<AMResultItem> {
        Timber.tag(TAG).d("getNextPage: $nextPageData")
        return api.getNextPage(nextPageData)
            .subscribeOn(schedulersProvider.io)
            .flatMapIterable { it.objects }
            .filter { it is AMResultItem }
            .cast(AMResultItem::class.java)
            .doOnError { Timber.tag(TAG).w(it, "getNextPage()") }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getAllPages(nextPageData: NextPageData): Observable<AMResultItem> {
        Timber.tag(TAG).i("getAllPages: $nextPageData")
        return api.getAllMusicPages(nextPageData)
            .subscribeOn(schedulersProvider.io)
            .flatMapIterable { it.objects }
            .filter { it is AMResultItem }
            .cast(AMResultItem::class.java)
            .doOnError { Timber.tag(TAG).w(it, "getAllPages()") }
    }

    override fun trackMonetizedPlay(song: AMResultItem) {
        Timber.tag(TAG).i("trackMonetizedPlay: ${song.itemId}")
        api.trackMonetizedPlay(song.itemId, (song.mixpanelSource ?: MixpanelSource.empty).page)
    }

    override fun reportUnplayable(item: AMResultItem) = api.reportUnplayable(item)
        .subscribeOn(schedulersProvider.io)

    override fun release() {
        songInfoDisposable?.dispose()
        urlDisposable?.dispose()
        updateOfflineSongDisposable?.dispose()
    }

    companion object {
        private const val TAG = "PlayerRepository"

        @Volatile
        private var INSTANCE: PlayerRepository? = null

        fun getInstance(
            api: API = API.getInstance(),
            musicDao: MusicDAO = MusicDAOImpl(),
            schedulersProvider: SchedulersProvider = AMSchedulersProvider()
        ): PlayerRepository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: PlayerRepository(api, musicDao, schedulersProvider).also { INSTANCE = it }
        }

        @VisibleForTesting
        fun destroy() {
            INSTANCE = null
        }
    }
}
