package com.audiomack.data.database

import com.activeandroid.query.Delete
import com.activeandroid.query.Select
import com.activeandroid.query.Update
import com.audiomack.model.AMPlaylistTracks
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItemSort
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

class MusicDAOImpl : MusicDAO {

    override fun findById(itemId: String): Observable<AMResultItem> {
        return Observable.create {
            val result = Select().from(AMResultItem::class.java).where("item_id = ?", itemId).executeSingle<AMResultItem>()
            if (result != null) {
                it.onNext(result)
                it.onComplete()
            } else {
                it.onError(MusicDAOException("findById returned no results"))
            }
        }
    }

    override fun save(item: AMResultItem): Single<AMResultItem> = Single.create { emitter ->
        val saved = item.save()
        if (saved < 0L) {
            emitter.onError(IllegalStateException("Database is null"))
        } else {
            emitter.onSuccess(item)
        }
    }

    override fun delete(item: AMResultItem): Completable = Completable.create { emitter ->
        item.deepDelete()
        emitter.onComplete()
    }

    override fun findDownloadedById(itemId: String): Observable<AMResultItem> {
        return Observable.create {
            val result = Select().from(AMResultItem::class.java).where("item_id = ? AND (cached = ? OR cached IS NULL)", itemId, false).executeSingle<AMResultItem>()
            if (result != null) {
                it.onNext(result)
            } else {
                it.onError(MusicDAOException("findDownloadedById returned no results"))
            }
        }
    }

    override fun deleteCachedById(itemId: String): Observable<Boolean> {
        return Observable.create {
            Delete().from(AMResultItem::class.java).where("item_id = ? AND cached = ?", itemId, true).execute<AMResultItem>()
            it.onNext(true)
        }
    }

    override fun querySavedItems(query: String): Observable<List<AMResultItem>> {
        return Observable.create {
            val results = Select().from(AMResultItem::class.java).where("(type = ? OR type = ? OR type = ? OR type = ?) AND (title LIKE ? OR artist LIKE ?) AND (cached = ? OR cached IS NULL)", "album", "song", "playlist_track", "album_track", "%$query%", "%$query%", false).execute<AMResultItem>()
            it.onNext(results)
        }
    }

    override fun savedItems(sort: AMResultItemSort, vararg columns: String): Observable<List<AMResultItem>> {
        return Observable.create {
            val results = Select(*columns).from(AMResultItem::class.java).where("(type = ? OR type = ? OR type = ? OR album_track_downloaded_as_single = ?) AND (cached = ? OR cached IS NULL)", "album", "song", "playlist_track", true, false).orderBy(sort.clause()).execute<AMResultItem>()
            it.onNext(results)
        }
    }

    override fun savedSongs(sort: AMResultItemSort, vararg columns: String): Observable<List<AMResultItem>> {
        return Observable.create {
            val results = Select(*columns).from(AMResultItem::class.java).where("(type = ? OR album_track_downloaded_as_single = ?) AND (cached = ? OR cached IS NULL)", "song", true, false).orderBy(sort.clause()).execute<AMResultItem>()
            it.onNext(results)
        }
    }

    override fun savedAlbums(sort: AMResultItemSort, vararg columns: String): Observable<List<AMResultItem>> {
        return Observable.create {
            val results = Select(*columns).from(AMResultItem::class.java).where("type = ? AND (cached = ? OR cached IS NULL)", "album", false).orderBy(sort.clause()).execute<AMResultItem>()
            it.onNext(results)
        }
    }

    override fun savedPlaylists(sort: AMResultItemSort, vararg columns: String): Observable<List<AMResultItem>> {
        return Observable.create {
            val results = Select(*columns).from(AMResultItem::class.java).where("type = ? AND (cached = ? OR cached IS NULL)", "playlist", false).orderBy(sort.clause()).execute<AMResultItem>()
            results.forEach { result -> result.playlistTracksCount = playlistTracksCount(result.itemId) }
            it.onNext(results)
        }
    }

    override fun savedPremiumLimitedUnfrozenTracks(sort: AMResultItemSort, vararg columns: String): Single<List<AMResultItem>> {
        return Single.create { emitter ->
            val results = Select(*columns).from(AMResultItem::class.java).where("(type = ? OR type = ? OR type = ?) AND (cached = ? OR cached IS NULL) AND premium_download = ? AND frozen = 0", "album_track", "song", "playlist_track", false, "premium-limited").orderBy(sort.clause()).execute<AMResultItem>()
            emitter.onSuccess(results)
        }
    }

    override fun unsyncedSavedItemsIds(): Observable<List<String>> {
        return Observable.create { emitter ->
            val results = Select("ID", "item_id").from(AMResultItem::class.java).where("(type = ? OR type = ? OR type = ? OR album_track_downloaded_as_single = ?) AND (cached = ? OR cached IS NULL) AND (synced IS NULL OR synced = ?)", "album", "song", "playlist_track", true, false, false).execute<AMResultItem>()
            emitter.onNext(results.filter { !it.itemId.isNullOrEmpty() }.map { it.itemId })
        }
    }

    override fun cachedItems(): Observable<List<AMResultItem>> {
        return Observable.create {
            val results = Select().from(AMResultItem::class.java).where("cached = ?", true).orderBy("ID DESC").execute<AMResultItem>()
            it.onNext(results)
        }
    }

    override fun playlistTracksCount(playlistId: String): Int {
        return Select("playlist_id").from(AMPlaylistTracks::class.java).where("playlist_id = ?", playlistId).orderBy("number ASC").count()
    }

    override fun updateSongWithFreshData(freshItem: AMResultItem): Observable<Boolean> {

        return findById(freshItem.itemId)
            .flatMap { dbItem ->
                dbItem.updateSongWithFreshData(freshItem)
                dbItem.save()
                Observable.just(true)
            }
    }

    override fun getAllTracks(sort: AMResultItemSort): Observable<List<AMResultItem>> =
        Observable.create {
            val results = Select()
                .from(AMResultItem::class.java)
                .where(
                    "(type = ? OR type = ? OR type = ? OR album_track_downloaded_as_single = ?) AND (cached = ? OR cached IS NULL)",
                    "album_track", "song", "playlist_track", true, false
                )
                .orderBy(sort.clause())
                .execute<AMResultItem>()
            it.onNext(results)
        }

    override fun markDownloadIncomplete(items: List<AMResultItem>) =
        Observable.create<List<AMResultItem>> { emitter ->
            val ids = items.joinToString { it.itemId }

            Update(AMResultItem::class.java)
                .set("download_completed = ?, album_track_downloaded_as_single = ?", false, false)
                .where("item_id IN ($ids)")
                .execute()

            val updatedItems = Select()
                .from(AMResultItem::class.java)
                .where("item_id IN ($ids)")
                .execute<AMResultItem>()
            emitter.onNext(updatedItems)

            emitter.onComplete()
        }

    override fun deleteAllItems() = Completable.create {
        Delete().from(AMResultItem::class.java).execute<AMResultItem>()
        Delete().from(AMPlaylistTracks::class.java).execute<AMPlaylistTracks>()
        it.onComplete()
    }

    override fun getPremiumLimitedSongs() = Single.create<List<String>> { emitter ->
        val ids = Select("ID", "item_id")
            .from(AMResultItem::class.java)
            .where(
                "(type = ? OR type = ? OR type = ? OR album_track_downloaded_as_single = ?) AND premium_download = ? AND download_completed = 1", "album_track", "song", "playlist_track", true, "premium-limited"
            )
            .orderBy("download_date ASC")
            .execute<AMResultItem>()
            .mapNotNull { it.itemId }
        emitter.onSuccess(ids)
    }

    override fun markFrozen(frozen: Boolean, ids: List<String>) = Completable.create { emitter ->
        val commaSeparatedIds = ids.joinToString { it }

        Update(AMResultItem::class.java)
            .set("frozen = ${if (frozen) 1 else 0}")
            .where("item_id IN ($commaSeparatedIds)")
            .execute()

        emitter.onComplete()
    }

    override fun downloadsCount() =
        Select("ID")
            .from(AMResultItem::class.java)
            .where("(item_id <> '' AND item_id IS NOT NULL) AND (type = ? OR type = ? OR type = ? OR album_track_downloaded_as_single = ?) AND (cached = ? OR cached IS NULL)", "album", "song", "playlist_track", true, false)
            .count()

    override fun premiumLimitedDownloadCount() =
        Select("ID")
            .from(AMResultItem::class.java)
            .where("(item_id <> '' AND item_id IS NOT NULL) AND (type = ? OR type = ? OR type = ? OR album_track_downloaded_as_single = ?) AND (cached = ? OR cached IS NULL) AND premium_download = ? AND download_completed = 1", "album_track", "song", "playlist_track", true, false, "premium-limited")
            .count()

    override fun premiumOnlyDownloadCount() =
        Select("ID")
            .from(AMResultItem::class.java)
            .where("(item_id <> '' AND item_id IS NOT NULL) AND (type = ? OR type = ? OR type = ? OR album_track_downloaded_as_single = ?) AND (cached = ? OR cached IS NULL) AND premium_download = ? AND download_completed = 1", "album_track", "song", "playlist_track", true, false, "premium-only")
            .count()

    override fun premiumLimitedUnfrozenDownloadCount() =
        Select("ID")
            .from(AMResultItem::class.java)
            .where("(item_id <> '' AND item_id IS NOT NULL) AND (type = ? OR type = ? OR type = ? OR album_track_downloaded_as_single = ?) AND (cached = ? OR cached IS NULL) AND premium_download = ? AND download_completed = 1 AND frozen = 0", "album_track", "song", "playlist_track", true, false, "premium-limited")
            .count()

    override fun premiumLimitedUnfrozenDownloadCountAsync() = Single.create<Int> { emitter ->
        emitter.onSuccess(premiumLimitedUnfrozenDownloadCount())
    }

    override fun bundleAlbumTracks(albumId: String) = Completable.create { emitter ->
        Update(AMResultItem::class.java)
            .set("album_track_downloaded_as_single = 0")
            .where("parent_id = $albumId")
            .execute()
        emitter.onComplete()
    }
}
