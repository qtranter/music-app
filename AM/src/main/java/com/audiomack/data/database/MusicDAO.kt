package com.audiomack.data.database

import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItemSort
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface MusicDAO {

    fun findById(itemId: String): Observable<AMResultItem>

    fun save(item: AMResultItem): Single<AMResultItem>

    fun delete(item: AMResultItem): Completable

    fun findDownloadedById(itemId: String): Observable<AMResultItem>

    fun deleteCachedById(itemId: String): Observable<Boolean>

    fun querySavedItems(query: String): Observable<List<AMResultItem>>

    fun savedItems(sort: AMResultItemSort, vararg columns: String): Observable<List<AMResultItem>>

    fun savedSongs(sort: AMResultItemSort, vararg columns: String): Observable<List<AMResultItem>>

    fun savedAlbums(sort: AMResultItemSort, vararg columns: String): Observable<List<AMResultItem>>

    fun savedPlaylists(sort: AMResultItemSort, vararg columns: String): Observable<List<AMResultItem>>

    fun savedPremiumLimitedUnfrozenTracks(sort: AMResultItemSort, vararg columns: String): Single<List<AMResultItem>>

    fun unsyncedSavedItemsIds(): Observable<List<String>>

    fun cachedItems(): Observable<List<AMResultItem>>

    fun playlistTracksCount(playlistId: String): Int

    fun updateSongWithFreshData(freshItem: AMResultItem): Observable<Boolean>

    fun markDownloadIncomplete(items: List<AMResultItem>): Observable<List<AMResultItem>>

    fun getAllTracks(sort: AMResultItemSort = AMResultItemSort.OldestFirst): Observable<List<AMResultItem>>

    fun deleteAllItems(): Completable

    fun getPremiumLimitedSongs(): Single<List<String>>

    fun markFrozen(frozen: Boolean, ids: List<String>): Completable

    fun downloadsCount(): Int

    fun premiumLimitedDownloadCount(): Int

    fun premiumOnlyDownloadCount(): Int

    fun premiumLimitedUnfrozenDownloadCount(): Int

    fun premiumLimitedUnfrozenDownloadCountAsync(): Single<Int>

    fun bundleAlbumTracks(albumId: String): Completable
}

class MusicDAOException(message: String) : Exception(message)
