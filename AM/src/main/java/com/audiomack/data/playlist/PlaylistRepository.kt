package com.audiomack.data.playlist

import com.audiomack.model.AMResultItem
import com.audiomack.network.API
import com.audiomack.network.addSongToPlaylist
import com.audiomack.network.deleteSongFromPlaylist
import io.reactivex.Completable
import io.reactivex.Observable

class PlaylistRepository(
    private val api: API = API.getInstance()
) : PlayListDataSource {

    override fun createPlaylist(
        title: String,
        genre: String?,
        desc: String?,
        privatePlaylist: Boolean,
        musicIds: String?,
        imageBase64: String?,
        bannerImageBase64: String?,
        mixpanelPage: String
    ): Observable<AMResultItem> {
        return api.createPlaylist(
            title,
            genre,
            desc,
            privatePlaylist,
            musicIds,
            imageBase64,
            bannerImageBase64,
            mixpanelPage
        )
    }

    override fun editPlaylist(
        id: String,
        title: String,
        genre: String?,
        desc: String?,
        privatePlaylist: Boolean,
        musicId: String,
        imageBase64: String?,
        bannerImageBase64: String?
    ): Observable<AMResultItem> {
        return api.editPlaylist(
            id,
            title,
            genre,
            desc,
            privatePlaylist,
            musicId,
            imageBase64,
            bannerImageBase64
        )
    }

    override fun deletePlaylist(playlistId: String): Observable<Boolean> {
        return api.deletePlaylist(playlistId)
    }

    override fun getPlaylistInfo(playlistId: String): Observable<AMResultItem> {
        return api.getPlaylistInfo(playlistId)
    }

    override fun addSongsToPlaylist(playlistId: String, songsIds: String, mixpanelPage: String): Completable {
        return api.addSongToPlaylist(playlistId, songsIds, mixpanelPage)
    }

    override fun deleteSongsFromPlaylist(
        playlistId: String,
        songsIds: String
    ): Completable {
        return api.deleteSongFromPlaylist(playlistId, songsIds)
    }

    override fun getMyPlaylists(
        page: Int,
        genre: String,
        biasedWithMusicId: String?,
        ignoreGeorestrictedMusic: Boolean
    ): Observable<List<AMResultItem>> {
        return api.getMyPlaylists(page, genre, biasedWithMusicId, ignoreGeorestrictedMusic)
            .observable
            .flatMap {
                Observable.just(it.objects as List<AMResultItem>)
            }
    }
}
