package com.audiomack.data.api

import com.audiomack.data.database.MusicDAO
import com.audiomack.data.database.MusicDAOImpl
import com.audiomack.data.music.local.LocalMediaDataSource
import com.audiomack.data.music.local.LocalMediaRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.model.AMMusicType
import com.audiomack.model.AMMusicType.Albums
import com.audiomack.model.AMMusicType.All
import com.audiomack.model.AMMusicType.Songs
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItemSort
import com.audiomack.model.AMResultItemSort.AToZ
import com.audiomack.model.AMResultItemSort.NewestFirst
import com.audiomack.model.AMResultItemSort.OldestFirst
import com.audiomack.model.EventDeletedDownload
import com.audiomack.model.EventDownload
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PlaylistCategory
import com.audiomack.network.API
import com.audiomack.network.playlistCategories
import com.audiomack.network.removeDownload
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.common.Resource
import com.audiomack.utils.nullSafeCompareTo
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import org.greenrobot.eventbus.EventBus

class MusicRepository(
    private val api: API = API.getInstance(),
    private val musicDao: MusicDAO = MusicDAOImpl(),
    private val localMedia: LocalMediaDataSource = LocalMediaRepository.getInstance(),
    private val tracking: TrackingDataSource = TrackingRepository(),
    private val eventBus: EventBus = EventBus.getDefault(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : MusicDataSource {

    override fun getPlaylistInfo(id: String): Observable<AMResultItem> {
        return api.getPlaylistInfo(id)
    }

    override fun getMusicInfo(item: AMResultItem): Observable<AMResultItem> {
        return when {
            item.isAlbum -> API.getInstance().getAlbumInfo(item)
            item.isPlaylist -> API.getInstance().getPlaylistInfo(item.itemId)
            else -> API.getInstance().getSongInfo(item)
        }
    }

    override fun getMusicInfo(id: String, type: String): Observable<AMResultItem> {
        return when (type) {
            "album" -> API.getInstance().getAlbumInfo(id)
            "playlist" -> API.getInstance().getPlaylistInfo(id)
            else -> API.getInstance().getSongInfo(id)
        }
    }

    override fun getSongInfo(id: String): Observable<AMResultItem> {
        return API.getInstance().getSongInfo(id)
    }

    override fun getAlbumInfo(id: String): Observable<AMResultItem> {
        return API.getInstance().getAlbumInfo(id)
    }

    override fun getOfflineResource(itemId: String): Observable<Resource<AMResultItem>> =
        musicDao.findById(itemId)
            .subscribeOn(schedulersProvider.io)
            .map<Resource<AMResultItem>> { Resource.Success(it) }
            .onErrorReturn { Resource.Failure(it) }

    override fun getOfflineItem(itemId: String): Single<AMResultItem> =
        musicDao.findById(itemId).firstOrError()

    override fun getHighlights(
        userSlug: String,
        myAccount: Boolean
    ): Observable<List<AMResultItem>> {
        return api.getHighlights(userSlug, myAccount)
    }

    override fun addToHighlights(
        item: AMResultItem,
        mixpanelSource: MixpanelSource
    ): Observable<Boolean> {
        return if (item.isPlaylist) {
            api.addHighlight(item)
        } else {
            api.repost(item, mixpanelSource.page)
                .flatMap { api.addHighlight(item) }
        }
    }

    override fun removeFromHighlights(item: AMResultItem): Observable<Boolean> {
        return api.removeHighlight(item)
    }

    override fun reorderHighlights(musicList: List<AMResultItem>): Observable<List<AMResultItem>> {
        return api.reorderHighlights(musicList)
    }

    override fun removeFromDownloads(music: AMResultItem): Observable<Boolean> {
        return api.removeDownload(music.itemId)
    }

    override fun deletePlaylist(playlistId: String): Observable<Boolean> {
        return api.deletePlaylist(playlistId)
    }

    override fun reorderPlaylist(
        playlistId: String,
        title: String,
        genre: String,
        desc: String,
        privatePlaylist: Boolean,
        musicId: String
    ): Observable<AMResultItem> {
        return api.editPlaylist(
            playlistId,
            title,
            genre,
            desc,
            privatePlaylist,
            musicId,
            null,
            null
        )
    }

    override fun createPlaylist(
        title: String,
        genre: String,
        desc: String,
        privatePlaylist: Boolean,
        musicId: String,
        imageBase64: String?,
        bannerImageBase64: String?,
        mixpanelPage: String
    ): Observable<AMResultItem> {
        return api.createPlaylist(
            title,
            genre,
            desc,
            privatePlaylist,
            musicId,
            imageBase64,
            bannerImageBase64,
            mixpanelPage
        )
    }

    override fun playlistCategories(): Observable<List<PlaylistCategory>> {
        return api.playlistCategories()
    }

    override fun repost(music: AMResultItem, mixpanelSource: MixpanelSource): Observable<Boolean> {
        return api.repost(music, mixpanelSource.page)
    }

    override fun unrepost(music: AMResultItem): Observable<Boolean> {
        return api.unrepost(music)
    }

    override fun favorite(music: AMResultItem, mixpanelSource: MixpanelSource): Observable<Boolean> {
        return Observable.create { emitter ->
            val listener = object : API.FavoriteListener {
                override fun onSuccess() {
                    emitter.onNext(true)
                    emitter.onComplete()
                }

                override fun onAlreadyFavorite() {
                    emitter.onNext(true)
                    emitter.onComplete()
                }

                override fun onFailure() {
                    emitter.onNext(false)
                    emitter.onComplete()
                }
            }
            if (music.isPlaylist) {
                api.favoritePlaylist(music.itemId, listener, mixpanelSource.page)
            } else {
                api.favorite(music, listener, mixpanelSource.page)
            }
        }
    }

    override fun unfavorite(music: AMResultItem): Observable<Boolean> {
        return Observable.create { emitter ->
            val listener = object : API.FavoriteListener {
                override fun onSuccess() {
                    emitter.onNext(true)
                    emitter.onComplete()
                }

                override fun onAlreadyFavorite() {
                    emitter.onNext(true)
                    emitter.onComplete()
                }

                override fun onFailure() {
                    emitter.onNext(false)
                    emitter.onComplete()
                }
            }
            if (music.isPlaylist) {
                api.unfavoritePlaylist(music.itemId, listener)
            } else {
                api.unfavorite(music, listener)
            }
        }
    }

    override fun markDownloadIncomplete(music: List<AMResultItem>) =
        musicDao.markDownloadIncomplete(music)

    override fun getDownloads(sort: AMResultItemSort): Observable<List<AMResultItem>> =
        musicDao.getAllTracks(sort)

    override fun markFrozenDownloads(frozen: Boolean, ids: List<String>) = musicDao.markFrozen(frozen, ids)

    override fun deleteMusicFromDB(music: AMResultItem) = Completable.create { emitter ->
        music.deepDelete()
        eventBus.post(EventDownload(music.itemId, false))
        eventBus.post(EventDeletedDownload(music))
        emitter.onComplete()
    }

    override fun savedPremiumLimitedUnfrozenTracks(sort: AMResultItemSort, vararg columns: String) =
        musicDao.savedPremiumLimitedUnfrozenTracks(sort, *columns)

    override fun bundleAlbumTracks(albumId: String) = musicDao.bundleAlbumTracks(albumId)

    override fun getOfflineItems(
        type: AMMusicType,
        sort: AMResultItemSort
    ): Observable<List<AMResultItem>> {
        return when (type) {
            Songs -> getOfflineTracks(sort)
            Albums -> getOfflineAlbums(sort)
            All -> getOfflineMedia(sort)
            else -> Observable.just(listOf())
        }
    }

    private fun getVisibleLocalMedia() = localMedia.visibleItems
        .doOnError { tracking.trackException(it) }
        .onErrorReturnItem(listOf())

    private fun getOfflineMedia(sort: AMResultItemSort): Observable<List<AMResultItem>> =
        Observable.combineLatest(
            getVisibleLocalMedia(),
            musicDao.savedItems(sort),
            zipItems()
        ).subscribeOn(schedulersProvider.io)
            .map { it.sortedWith(getOfflineComparator(sort)) }
            .doOnError { tracking.trackException(it) }

    private fun getOfflineTracks(sort: AMResultItemSort): Observable<List<AMResultItem>> =
        Observable.combineLatest(
            getVisibleLocalMedia().map { items -> items.filter { it.isSong || it.isAlbumTrack } },
            musicDao.savedSongs(sort),
            zipItems()
        ).subscribeOn(schedulersProvider.io)
            .map { it.sortedWith(getOfflineComparator(sort)) }
            .doOnError { tracking.trackException(it) }

    private fun getOfflineAlbums(sort: AMResultItemSort): Observable<List<AMResultItem>> =
        Observable.combineLatest(
            getVisibleLocalMedia().map { items -> items.filter { it.isAlbum } },
            musicDao.savedAlbums(sort),
            zipItems()
        ).subscribeOn(schedulersProvider.io)
            .map { it.sortedWith(getOfflineComparator(sort)) }
            .doOnError { tracking.trackException(it) }

    private fun zipItems() =
        BiFunction<List<AMResultItem>, List<AMResultItem>, List<AMResultItem>> { list1, list2 ->
            list1 + list2
        }

    private fun getOfflineComparator(sort: AMResultItemSort) =
        Comparator<AMResultItem> { item1, item2 ->
            when (sort) {
                NewestFirst -> item2.downloadDate.nullSafeCompareTo(item1.downloadDate)
                OldestFirst -> item1.downloadDate.nullSafeCompareTo(item2.downloadDate)
                AToZ -> item1.title.nullSafeCompareTo(item2.title)
            }
        }

    companion object {
        private const val TAG = "MusicRepository"
    }
}
