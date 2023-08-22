package com.audiomack.usecases

import com.audiomack.R
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.database.MusicDAOException
import com.audiomack.data.reachability.Reachability
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryOffline
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibrarySearchOffline
import com.audiomack.data.tracking.mixpanel.MixpanelPageProfileUploads
import com.audiomack.data.tracking.mixpanel.MixpanelPageRestoreDownloads
import com.audiomack.model.AMResultItem
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.MixpanelSource
import com.audiomack.model.NextPageData
import com.audiomack.model.OpenMusicData
import com.audiomack.model.ProgressHUDMode
import com.audiomack.network.APIException
import com.audiomack.ui.common.Resource
import io.reactivex.Observable
import kotlin.RuntimeException

sealed class OpenMusicResult {
    data class ToggleLoader(val mode: ProgressHUDMode) : OpenMusicResult()
    data class ShowPlaylist(
        val playlist: AMResultItem,
        val online: Boolean,
        val deleted: Boolean,
        val mixpanelSource: MixpanelSource,
        val openShare: Boolean = false
    ) : OpenMusicResult()

    data class ShowAlbum(
        val album: AMResultItem,
        val mixpanelSource: MixpanelSource,
        val openShare: Boolean = false
    ) : OpenMusicResult()

    data class ReadyToPlay(val data: MaximizePlayerData) : OpenMusicResult()
    object GeoRestricted : OpenMusicResult()
}

interface OpenMusicUseCase {
    operator fun invoke(data: OpenMusicData): Observable<OpenMusicResult>
}

class OpenMusicUseCaseImpl(
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val reachabilityDataSource: ReachabilityDataSource = Reachability.getInstance()
) : OpenMusicUseCase {
    override operator fun invoke(data: OpenMusicData) = Observable.create<OpenMusicResult> { emitter ->

        val errorMessageResId = when {
            data.item.isAlbum -> R.string.album_info_failed
            data.item.isPlaylist -> R.string.playlist_info_failed
            data.item.isSong -> R.string.song_info_failed
            else -> R.string.song_info_failed
        }

        fun emitFailureLoader() = emitter.onNext(
            OpenMusicResult.ToggleLoader(
                ProgressHUDMode.Failure(
                    "",
                    errorMessageResId
                )
            )
        )

        if (data.item.isGeoRestricted) {
            emitter.onNext(OpenMusicResult.GeoRestricted)
        } else {

            // Handle Playlist
            if (data.item.isPlaylist) {
                var remotePlaylist: AMResultItem? = null

                emitter.onNext(OpenMusicResult.ToggleLoader(ProgressHUDMode.Loading))

                if (!reachabilityDataSource.networkAvailable) {
                    val playlistResource =
                        musicDataSource.getOfflineResource(data.item.itemId).blockingFirst()
                    playlistResource.takeIf { it is Resource.Success }?.data?.let { dbPlaylist ->
                        dbPlaylist.loadTracks()
                        emitter.onNext(OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss))
                        emitter.onNext(
                            OpenMusicResult.ShowPlaylist(
                                dbPlaylist,
                                online = false,
                                deleted = false,
                                mixpanelSource = data.source,
                                openShare = data.openShare
                            )
                        )
                    } ?: emitFailureLoader()
                } else {
                    try {
                        val playlistResource =
                            musicDataSource.getPlaylistInfo(data.item.itemId).blockingFirst()
                        remotePlaylist = playlistResource
                        val dbPlaylistResource =
                            musicDataSource.getOfflineResource(playlistResource.itemId)
                                .blockingFirst()
                        dbPlaylistResource.takeIf { it is Resource.Success }?.data?.updatePlaylist(
                            remotePlaylist
                        )
                        emitter.onNext(OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss))
                        emitter.onNext(
                            OpenMusicResult.ShowPlaylist(
                                playlistResource,
                                online = true,
                                deleted = false,
                                mixpanelSource = data.source,
                                openShare = data.openShare
                            )
                        )
                    } catch (throwable: Throwable) {
                        if (throwable is RuntimeException && throwable.cause is MusicDAOException) {
                            emitter.onNext(OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss))
                            emitter.onNext(
                                OpenMusicResult.ShowPlaylist(
                                    remotePlaylist!!,
                                    online = true,
                                    deleted = false,
                                    mixpanelSource = data.source,
                                    openShare = data.openShare
                                )
                            )
                        } else if (throwable is RuntimeException && throwable.cause is APIException) {
                            val apiException = throwable.cause as? APIException
                            if (apiException?.statusCode == 404 || apiException?.statusCode == 403) {
                                val playlistResource =
                                    musicDataSource.getOfflineResource(data.item.itemId).blockingFirst()
                                playlistResource.takeIf { it is Resource.Success }?.data?.let { dbPlaylist ->
                                    dbPlaylist.loadTracks()
                                    emitter.onNext(OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss))
                                    emitter.onNext(
                                        OpenMusicResult.ShowPlaylist(
                                            dbPlaylist,
                                            online = true,
                                            deleted = false,
                                            mixpanelSource = data.source,
                                            openShare = data.openShare
                                        )
                                    )
                                } ?: emitFailureLoader()
                            } else {
                                emitFailureLoader()
                            }
                        } else {
                            emitFailureLoader()
                        }
                    }
                }
                // Hanlde album
            } else if (data.item.isAlbum) {

                emitter.onNext(OpenMusicResult.ToggleLoader(ProgressHUDMode.Loading))

                try {
                    if (data.source.page == MixpanelPageMyLibraryOffline || data.source.page == MixpanelPageMyLibrarySearchOffline) {
                        val albumResource =
                            musicDataSource.getOfflineResource(data.item.itemId).blockingFirst()
                        albumResource.takeIf { it is Resource.Success }?.data?.let { dbAblum ->
                            dbAblum.loadTracks()
                            emitter.onNext(OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss))
                            emitter.onNext(
                                OpenMusicResult.ShowAlbum(
                                    album = dbAblum,
                                    mixpanelSource = data.source,
                                    openShare = data.openShare
                                )
                            )
                        } ?: emitFailureLoader()
                    } else {
                        val album = musicDataSource.getAlbumInfo(data.item.itemId).blockingFirst()
                        emitter.onNext(OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss))
                        emitter.onNext(
                            OpenMusicResult.ShowAlbum(
                                album = album,
                                mixpanelSource = data.source,
                                openShare = data.openShare
                            )
                        )
                    }
                } catch (throwable: Throwable) {
                    emitFailureLoader()
                }

                // Handle songs
            } else {
                val offlineMode =
                    data.source.page == MixpanelPageMyLibraryOffline || data.source.page == MixpanelPageRestoreDownloads
                val scrollToTop = data.source.page == MixpanelPageProfileUploads
                val nextPage = getNextPageData(
                    data.source,
                    offlineMode,
                    data.url,
                    data.page
                )

                emitter.onNext(
                    OpenMusicResult.ReadyToPlay(
                        MaximizePlayerData(
                            item = data.item,
                            items = data.items,
                            nextPageData = nextPage,
                            inOfflineScreen = offlineMode,
                            mixpanelSource = data.source,
                            scrollToTop = scrollToTop,
                            openShare = data.openShare
                        )
                    )
                )
            }
        }
        emitter.onComplete()
    }

    private fun getNextPageData(
        mixpanelSource: MixpanelSource,
        offlineScreen: Boolean,
        url: String?,
        currentPage: Int
    ): NextPageData? {
        return url?.let {
            NextPageData(url, currentPage, mixpanelSource, offlineScreen)
        }
    }
}
