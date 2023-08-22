package com.audiomack.usecases

import com.audiomack.R
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.queue.QueueDataSource.Companion.CURRENT_INDEX
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import com.audiomack.model.ProgressHUDMode
import com.audiomack.playback.Playback
import com.audiomack.playback.PlayerPlayback
import com.audiomack.playback.PlayerQueue
import io.reactivex.Observable

enum class AddMusicToQueuePosition {
    Next, Later
}

sealed class AddMusicToQueueUseCaseResult {
    object Success : AddMusicToQueueUseCaseResult()
    data class ToggleLoader(val mode: ProgressHUDMode) : AddMusicToQueueUseCaseResult()
    object Georestricted : AddMusicToQueueUseCaseResult()
}

interface AddMusicToQueueUseCase {
    /**
     * Takes care of loading a song/album/playlist remotely and adding it to the queue.
     * This is meant to replace [AMResultItem.playNext] and [AMResultItem.playLater]
     * @param musicId: a [String] with the music id represented as "{uploaderSlug}/{musicSlug}"
     * @param musicType: a [MusicType] emum case representing the type of music
     * @param mixpanelSource: a [MixpanelSource] instance representing the origin of this music for tracking purposes
     * @param position: a [AddMusicToQueuePosition] enum case to specify where the new song(s) should be added
     * @return an [Observable] of [AddMusicToQueueUseCaseResult] sealed class with the various states of the request
     */
    fun loadAndAdd(
        musicId: String,
        musicType: MusicType,
        mixpanelSource: MixpanelSource,
        position: AddMusicToQueuePosition
    ): Observable<AddMusicToQueueUseCaseResult>
}

class AddMusicToQueueUseCaseImpl(
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val playback: Playback = PlayerPlayback.getInstance()
) : AddMusicToQueueUseCase {
    override fun loadAndAdd(
        musicId: String,
        musicType: MusicType,
        mixpanelSource: MixpanelSource,
        position: AddMusicToQueuePosition
    ) = Observable.create<AddMusicToQueueUseCaseResult> { emitter ->
        val errorMessageResId = when (musicType) {
            MusicType.Song -> R.string.song_info_failed
            MusicType.Album -> R.string.album_info_failed
            MusicType.Playlist -> R.string.playlist_info_failed
        }
        emitter.onNext(AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Loading))
        try {
            val music = musicDataSource.getMusicInfo(musicId, musicType.typeForMusicApi).blockingFirst()
            emitter.onNext(AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Dismiss))
            val validTracks = music.tracksWithoutRestricted ?: emptyList()
            if (music.isGeoRestricted || (musicType != MusicType.Song && validTracks.isEmpty())) {
                emitter.onNext(AddMusicToQueueUseCaseResult.Georestricted)
            } else {
                val songs = when (musicType) {
                    MusicType.Song -> listOf(music)
                    else -> validTracks
                }
                val playerQueue = PlayerQueue.Collection(songs, 0, mixpanelSource, false)
                playback.addQueue(playerQueue, if (position == AddMusicToQueuePosition.Later) null else CURRENT_INDEX)
                emitter.onNext(AddMusicToQueueUseCaseResult.Success)
            }
        } catch (e: Exception) {
            emitter.onNext(AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Failure("", errorMessageResId)))
        }
        emitter.onComplete()
    }
}
