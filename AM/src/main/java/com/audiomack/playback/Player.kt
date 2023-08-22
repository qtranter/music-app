package com.audiomack.playback

import android.net.Uri
import com.audiomack.common.StateManager
import com.audiomack.common.StateProvider
import com.audiomack.data.queue.QueueDataSource.Companion.CURRENT_INDEX
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.model.NextPageData
import com.audiomack.playback.ActionState.DEFAULT
import com.audiomack.playback.PlaybackState.IDLE
import com.audiomack.utils.Millisecond
import com.audiomack.utils.Url
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.EventListener
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlin.math.max
import kotlin.math.min

/**
 * Controls playback and exposes playback state.
 */
interface Playback : EventListener {
    val item: BehaviorSubject<PlaybackItem>
    val state: StateProvider<PlaybackState>
    val timer: Observable<Long>
    val error: Observable<PlayerError>
    val adTimer: Observable<Long>
    val duration: Millisecond
    val position: Millisecond
    val isPlaying: Boolean
    val isEnded: Boolean
    val repeatType: Observable<RepeatType>
    val downloadRequest: Observable<AMResultItem>
    var audioSessionId: Int?
    val songSkippedManually: Boolean

    fun setPlayer(player: Player?)
    fun isPlayer(player: Player?): Boolean

    fun play()
    fun pause()
    fun stop(reset: Boolean = true)
    fun seekTo(position: Long)
    fun next()
    fun prev()
    fun skip(index: Int)
    fun release()
    fun reload()
    fun repeat(repeatType: RepeatType? = null)

    fun setQueue(playerQueue: PlayerQueue, play: Boolean = true)

    /**
     * If [index] is null, [playerQueue] is added to the end. If [index] is set to [CURRENT_INDEX],
     * [playerQueue] will be added after the current index.
     */
    fun addQueue(playerQueue: PlayerQueue, index: Int? = null)
}

const val SKIP_BACK_DURATION: Millisecond = 15L * 1000L // 15 seconds
const val SKIP_FORWARD_DURATION: Millisecond = 30L * 1000L // 15 seconds

fun Playback.rewind(amount: Millisecond = SKIP_BACK_DURATION) = seekTo(max(0, position - amount))
fun Playback.fastForward(amount: Millisecond = SKIP_FORWARD_DURATION) {
    if (position + amount >= duration) {
        next()
    } else {
        seekTo(min(duration, position + amount))
    }
}

data class PlaybackItem(
    /**
     * The item containing metadata about the playback item
     */
    val track: AMResultItem,

    /**
     * The remote pre-signed URL
     */
    val streamUrl: Url,

    /**
     * A [Uri] suitable for playback. May represent a local, cached, or remote file.
     */
    val uri: Uri,

    /**
     * The position to start playback of the current item, in ms
     */
    val position: Millisecond = C.TIME_UNSET,

    /**
     * Whether the player should play the current track when ready (done with ads, and other
     * interrupting media).
     */
    val playWhenReady: Boolean = false
) {
    override fun equals(other: Any?) = track.itemId == (other as? PlaybackItem)?.track?.itemId
}

fun PlaybackItem?.isPodcast() = this?.track?.isPodcast == true

enum class PlaybackState {
    IDLE, PLAYING, PAUSED, LOADING, ENDED, ERROR
}

internal object PlaybackStateManager : StateManager<PlaybackState>(IDLE)

enum class RepeatType {
    OFF, ONE, ALL
}

enum class ShuffleState {
    ON, OFF, DISABLED
}

enum class ActionState(var isPremium: Boolean = false, var downloadType: AMResultItem.MusicDownloadType = AMResultItem.MusicDownloadType.Free, var frozenDownloadsCount: Int? = null, var frozenDownloadsTotal: Int? = null) {
    DEFAULT, LOADING, ACTIVE, QUEUED, DISABLED, FROZEN
}

sealed class SongAction(val state: ActionState, val text: String? = null) {
    class Favorite(state: ActionState = DEFAULT, text: String? = null) : SongAction(state, text)
    class AddToPlaylist(state: ActionState = DEFAULT, text: String? = null) :
        SongAction(state, text)

    class RePost(state: ActionState = DEFAULT, text: String? = null) : SongAction(state, text)
    class Download(state: ActionState = DEFAULT) : SongAction(state)
    class Share(state: ActionState = DEFAULT) : SongAction(state)
    class Edit(state: ActionState = DEFAULT) : SongAction(state)

    override fun toString(): String {
        return "SongAction(state=$state, text=$text)"
    }
}

/**
 * Convenience classes for creating a payload for [com.audiomack.data.queue.QueueDataSource]
 */
sealed class PlayerQueue(
    /**
     * When the queue represents and Album or Playlist this field is a reference to it
     */
    open val item: AMResultItem?,

    /**
     * The list of items to play, which may include items that represent albums and playlists
     */
    open val items: List<AMResultItem>,

    /**
     * The index in [items] to queue when loading this payload
     */
    open val trackIndex: Int = 0,

    open val source: MixpanelSource? = null,

    open val inOfflineScreen: Boolean = false,

    open val shuffle: Boolean = false,

    open val allowFrozenTracks: Boolean = false
) {
    data class Song(
        override val item: AMResultItem,
        override val source: MixpanelSource? = null,
        override val inOfflineScreen: Boolean = false,
        override val allowFrozenTracks: Boolean = false
    ) : PlayerQueue(
        item = item,
        items = listOf(item),
        source = source,
        inOfflineScreen = inOfflineScreen,
        allowFrozenTracks = allowFrozenTracks
    )

    data class Playlist(
        val playlist: AMResultItem,
        override val trackIndex: Int = 0,
        override val source: MixpanelSource? = null,
        override val inOfflineScreen: Boolean = false,
        override val shuffle: Boolean = false,
        override val allowFrozenTracks: Boolean = false
    ) : PlayerQueue(
        playlist,
        playlist.tracks ?: listOf(),
        trackIndex,
        source,
        inOfflineScreen,
        shuffle,
        allowFrozenTracks
    )

    data class Album(
        val album: AMResultItem,
        override val trackIndex: Int = 0,
        override val source: MixpanelSource? = null,
        override val inOfflineScreen: Boolean = false,
        override val shuffle: Boolean = false,
        override val allowFrozenTracks: Boolean = false
    ) : PlayerQueue(
        album,
        album.tracks?.filter { !it.isGeoRestricted && (allowFrozenTracks || !it.isDownloadFrozen) }
            ?: listOf(),
        trackIndex,
        source,
        inOfflineScreen,
        shuffle,
        allowFrozenTracks)

    data class Collection(
        override val items: List<AMResultItem>,
        override val trackIndex: Int = 0,
        override val source: MixpanelSource? = null,
        override val inOfflineScreen: Boolean = false,
        override val shuffle: Boolean = false,
        val nextPageData: NextPageData? = null,
        override val allowFrozenTracks: Boolean = false
    ) : PlayerQueue(null, items, trackIndex, source, inOfflineScreen, shuffle, allowFrozenTracks)

    override fun toString(): String {
        return "PlayerQueue(item=$item, size=${items.size}, trackIndex=$trackIndex), shuffle=$shuffle)"
    }
}

sealed class PlayerError(val throwable: Throwable? = null) {
    class Resource(throwable: Throwable?) : PlayerError(throwable)
    class Storage(throwable: Throwable? = null) : PlayerError(throwable)
    class Playback(throwable: Throwable?) : PlayerError(throwable)
    class Queue(throwable: Throwable?) : PlayerError(throwable)
    class Action(throwable: Throwable?) : PlayerError(throwable)
    object Seek : PlayerError()
}

interface PlayEventListener {
    fun trackPlayEvent(item: AMResultItem)
}

interface NowPlayingVisibility {
    var isMaximized: Boolean
}

object NowPlayingVisibilityImpl : NowPlayingVisibility {
    override var isMaximized: Boolean = false
}

interface Sources {
    val baseDataSourceFactory: DataSource.Factory
    fun buildMediaSource(uri: Uri): MediaSource
}
