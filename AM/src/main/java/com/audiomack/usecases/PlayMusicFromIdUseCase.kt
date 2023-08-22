package com.audiomack.usecases

import com.audiomack.R
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import com.audiomack.model.ProgressHUDMode
import io.reactivex.Observable

sealed class PlayMusicFromIdResult {
    data class ReadyToPlay(val data: MaximizePlayerData) : PlayMusicFromIdResult()
    data class ToggleLoader(val mode: ProgressHUDMode) : PlayMusicFromIdResult()
    object Georestricted : PlayMusicFromIdResult()
}

interface PlayMusicFromIdUseCase {
    /**
     * Takes care of loading a song/album/playlist remotely and loading it into the queue, replacing the previous queue.
     * @param musicId: a [String] with the music id represented as "{uploaderSlug}/{musicSlug}"
     * @param musicType: a [MusicType] emum case representing the type of music
     * @param mixpanelSource: a [MixpanelSource] instance representing the origin of this music for tracking purposes
     * @return an [Observable] of [PlayMusicFromIdResult] sealed class with the various states of the request
     */
    fun loadAndPlay(
        musicId: String,
        musicType: MusicType,
        mixpanelSource: MixpanelSource
    ): Observable<PlayMusicFromIdResult>
}

class PlayMusicFromIdUseCaseImpl(
    private val musicDataSource: MusicDataSource = MusicRepository()
) : PlayMusicFromIdUseCase {

    override fun loadAndPlay(
        musicId: String,
        musicType: MusicType,
        mixpanelSource: MixpanelSource
    ) = Observable.create<PlayMusicFromIdResult> { emitter ->
        val errorMessageResId = when (musicType) {
            MusicType.Song -> R.string.song_info_failed
            MusicType.Album -> R.string.album_info_failed
            MusicType.Playlist -> R.string.playlist_info_failed
        }
        emitter.onNext(PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Loading))
        try {
            val music = musicDataSource.getMusicInfo(musicId, musicType.typeForMusicApi).blockingFirst()
            emitter.onNext(PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Dismiss))
            val validTracks = music.tracksWithoutRestricted ?: emptyList()
            if (music.isGeoRestricted || (musicType != MusicType.Song && validTracks.isEmpty())) {
                emitter.onNext(PlayMusicFromIdResult.Georestricted)
            } else {
                val data = when (musicType) {
                    MusicType.Song -> MaximizePlayerData(
                            item = music,
                            mixpanelSource = mixpanelSource
                        )
                    MusicType.Album -> MaximizePlayerData(
                            item = validTracks.first(),
                            collection = music,
                            albumPlaylistIndex = 0,
                            mixpanelSource = mixpanelSource
                        )
                    MusicType.Playlist -> MaximizePlayerData(
                            item = validTracks.first(),
                            collection = music,
                            albumPlaylistIndex = 0,
                            loadFullPlaylist = true,
                            mixpanelSource = mixpanelSource
                        )
                }
                emitter.onNext(PlayMusicFromIdResult.ReadyToPlay(data))
            }
        } catch (e: Exception) {
            emitter.onNext(PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Failure("", errorMessageResId)))
        }
        emitter.onComplete()
    }
}
