package com.audiomack.data.playlist

import com.audiomack.model.AMResultItem
import io.reactivex.Completable
import io.reactivex.Observable

interface PlayListDataSource {

    fun createPlaylist(
        title: String,
        genre: String?,
        desc: String?,
        privatePlaylist: Boolean,
        musicIds: String?,
        imageBase64: String?,
        bannerImageBase64: String?,
        mixpanelPage: String
    ): Observable<AMResultItem>

    fun editPlaylist(
        id: String,
        title: String,
        genre: String?,
        desc: String?,
        privatePlaylist: Boolean,
        musicId: String,
        imageBase64: String?,
        bannerImageBase64: String?
    ): Observable<AMResultItem>

    fun deletePlaylist(playlistId: String): Observable<Boolean>

    fun getPlaylistInfo(playlistId: String): Observable<AMResultItem>

    fun addSongsToPlaylist(playlistId: String, songsIds: String, mixpanelPage: String): Completable

    fun deleteSongsFromPlaylist(playlistId: String, songsIds: String): Completable

    fun getMyPlaylists(page: Int, genre: String, biasedWithMusicId: String? = null, ignoreGeorestrictedMusic: Boolean): Observable<List<AMResultItem>>
}
