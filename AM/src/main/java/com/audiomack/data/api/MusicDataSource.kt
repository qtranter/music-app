package com.audiomack.data.api

import com.audiomack.model.AMMusicType
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItemSort
import com.audiomack.model.AMResultItemSort.NewestFirst
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PlaylistCategory
import com.audiomack.ui.common.Resource
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface MusicDataSource {

    fun getPlaylistInfo(id: String): Observable<AMResultItem>

    fun getMusicInfo(item: AMResultItem): Observable<AMResultItem>

    fun getMusicInfo(id: String, type: String): Observable<AMResultItem>

    fun getSongInfo(id: String): Observable<AMResultItem>

    fun getAlbumInfo(id: String): Observable<AMResultItem>

    fun getOfflineResource(itemId: String): Observable<Resource<AMResultItem>>

    fun getOfflineItem(itemId: String): Single<AMResultItem>

    fun getOfflineItems(type: AMMusicType, sort: AMResultItemSort): Observable<List<AMResultItem>>

    fun getHighlights(userSlug: String, myAccount: Boolean): Observable<List<AMResultItem>>

    /** Emits true if the operation completed successfully, false otherwise **/
    fun addToHighlights(item: AMResultItem, mixpanelSource: MixpanelSource): Observable<Boolean>

    /** Emits true if the operation completed successfully, false otherwise **/
    fun removeFromHighlights(item: AMResultItem): Observable<Boolean>

    fun reorderHighlights(musicList: List<AMResultItem>): Observable<List<AMResultItem>>

    fun removeFromDownloads(music: AMResultItem): Observable<Boolean>

    fun deletePlaylist(playlistId: String): Observable<Boolean>

    fun reorderPlaylist(
        playlistId: String,
        title: String,
        genre: String,
        desc: String,
        privatePlaylist: Boolean,
        musicId: String
    ): Observable<AMResultItem>

    fun createPlaylist(
        title: String,
        genre: String,
        desc: String,
        privatePlaylist: Boolean,
        musicId: String,
        imageBase64: String?,
        bannerImageBase64: String?,
        mixpanelPage: String
    ): Observable<AMResultItem>

    fun playlistCategories(): Observable<List<PlaylistCategory>>

    fun repost(music: AMResultItem, mixpanelSource: MixpanelSource): Observable<Boolean>

    fun unrepost(music: AMResultItem): Observable<Boolean>

    fun favorite(music: AMResultItem, mixpanelSource: MixpanelSource): Observable<Boolean>

    fun unfavorite(music: AMResultItem): Observable<Boolean>

    fun markDownloadIncomplete(music: List<AMResultItem>): Observable<List<AMResultItem>>

    fun getDownloads(sort: AMResultItemSort = NewestFirst): Observable<List<AMResultItem>>

    fun markFrozenDownloads(frozen: Boolean, ids: List<String>): Completable

    fun deleteMusicFromDB(music: AMResultItem): Completable

    fun savedPremiumLimitedUnfrozenTracks(sort: AMResultItemSort, vararg columns: String): Single<List<AMResultItem>>

    /** Bundles all standalone downloaded album tracks in their parent album **/
    fun bundleAlbumTracks(albumId: String): Completable
}
