package com.audiomack.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.audiomack.model.AMResultItem
import com.audiomack.model.Credentials
import com.audiomack.playback.cast.MediaInfoCustomData
import com.audiomack.playback.cast.albumTitle
import com.audiomack.playback.cast.artist
import com.audiomack.playback.cast.releaseDate
import com.audiomack.playback.cast.title
import com.audiomack.playback.cast.trackNumber
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.common.images.WebImage

object CastUtils {

    fun buildMediaQueueItem(
        context: Context,
        item: AMResultItem,
        playWhenReady: Boolean,
        url: String?
    ): MediaQueueItem {
        val duration = item.duration.times(1000L)

        val songMetadata: MediaMetadata = buildSongMetadata(item)
        val customData: MediaInfoCustomData = buildCustomData(context, item)

        val mediaInfo = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(getMimeType(url))
            .setStreamDuration(duration)
            .setMetadata(songMetadata)
            .setCustomData(customData.toJSON())
            .build()

        return MediaQueueItem.Builder(mediaInfo)
            .setAutoplay(playWhenReady)
            .build()
    }

    private fun buildSongMetadata(item: AMResultItem): MediaMetadata {
        val songMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            title = item.title
            artist = item.artist
            releaseDate = item.releaseDate
            if (item.isAlbumTrack) albumTitle = item.album
            if (item.isAlbumTrack || item.isPlaylistTrack) trackNumber = item.trackNumber
        }

        item.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetOriginal)
            ?.let { imageUrl ->
                songMetadata.addImage(WebImage(Uri.parse(imageUrl)))
            }

        return songMetadata
    }

    private fun buildCustomData(context: Context, item: AMResultItem): MediaInfoCustomData {
        val credentials = Credentials.load(context)
        val loggedIn = Credentials.isLogged(context)
        val token = if (loggedIn) credentials?.token else null
        val tokenSecret = if (loggedIn) credentials?.tokenSecret else null
        return MediaInfoCustomData(
            if (item.isSong) item.itemId else item.parentId ?: "",
            item.typeForCastApi,
            token,
            tokenSecret,
            true
        )
    }

    private fun getMimeType(url: String?): String {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type ?: "audio/*"
    }
}
