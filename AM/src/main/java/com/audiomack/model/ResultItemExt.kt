package com.audiomack.model

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Media
import android.provider.OpenableColumns
import androidx.core.net.toFile
import com.audiomack.data.music.local.getInt
import com.audiomack.data.music.local.getLong
import com.audiomack.data.music.local.getString
import com.audiomack.data.sizes.SizesRepository
import java.util.Calendar

private const val EMPTY: String = ""

private val artworkUri: Uri by lazy { Uri.parse("content://media/external/audio/albumart") }

// TODO Replace with static extension when possible
fun AMResultItem.songFromMediaCursor(cursor: Cursor, albumItem: AMResultItem? = null): AMResultItem? {
    return cursor.getLong(Media._ID)?.let { id ->
        itemId = id.toString()
        title = cursor.getString(Media.TITLE) ?: cursor.getString(Media.DISPLAY_NAME, EMPTY)
        artist = cursor.getString(Media.ARTIST, EMPTY)
        album = cursor.getString(Media.ALBUM, EMPTY)
        parentId = cursor.getLong(Media.ALBUM_ID)?.toString()
        duration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cursor.getLong(Media.DURATION)?.div(1000L) ?: 0L
        } else 0L
        released = cursor.getInt(Media.YEAR)?.toString() ?: EMPTY
        songReleaseDate = cursor.getInt(Media.YEAR)
            ?.let { year -> Calendar.getInstance().apply { set(Calendar.YEAR, year) }.timeInMillis }
            ?: 0L
        downloadDate = Calendar.getInstance().run {
            timeInMillis = cursor.getLong(Media.DATE_ADDED, 0L)
            time
        }
        trackNumber = cursor.getInt(Media.TRACK)?.let { track ->
            val trackString = track.toString()
            if (trackString.length == 4) {
                discNumber = trackString.substring(0, 1).toInt()
                trackString.substring(1).toInt()
            } else {
                track
            }
        } ?: 0
        isLocal = true
        type = if (albumItem != null) AMResultItem.TYPE_ALBUM_TRACK else AMResultItem.TYPE_SONG
        url = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id).toString()
        albumItem?.let {
            // All artwork is contained in the album table
            originalImage = it.originalImage
            smallImage = it.smallImage
        }
        this
    }
}

// TODO Replace with static extension when possible
fun AMResultItem.albumFromMediaCursor(cursor: Cursor): AMResultItem? {
    return cursor.getLong(Albums._ID)?.let { id ->
        itemId = id.toString()
        title = cursor.getString(Albums.ALBUM, EMPTY)
        artist = cursor.getString(Albums.ARTIST, EMPTY)
        released = cursor.getLong(Albums.LAST_YEAR)?.toString() ?: EMPTY
        songReleaseDate = cursor.getInt(Albums.LAST_YEAR)
            ?.let { year -> Calendar.getInstance().apply { set(Calendar.YEAR, year) }.timeInMillis }
            ?: 0L
        downloadDate = Calendar.getInstance().run {
            timeInMillis = cursor.getLong(Albums.LAST_YEAR, 0L)
            time
        }
        tracks = // fill the tracks with nulls so we know whether to add songs later
            arrayOfNulls<AMResultItem>(cursor.getInt(Albums.NUMBER_OF_SONGS, 0)).toMutableList()
        ContentUris.withAppendedId(artworkUri, id).toString().let { url ->
            originalImage = url
            smallImage = url
        }
        isLocal = true
        type = AMResultItem.TYPE_ALBUM
        mixpanelSource = MixpanelSource.empty
        this
    }
}

fun AMResultItem.songFromOpenableCursor(cursor: Cursor, uri: Uri): AMResultItem? {
    return cursor.getString(OpenableColumns.DISPLAY_NAME)?.let { displayName ->
        itemId = displayName
        title = displayName
        artist = EMPTY
        released = EMPTY
        isLocal = true
        type = AMResultItem.TYPE_SONG
        url = uri.toString()
        mixpanelSource = MixpanelSource.empty
        originalImage = DEFAULT_IMAGE_LARGE
        smallImage = DEFAULT_IMAGE_SMALL
        this
    }
}

fun AMResultItem.songFromOpenableFile(uri: Uri): AMResultItem = apply {
    val name = uri.toFile().name
    itemId = name
    title = name
    artist = EMPTY
    released = EMPTY
    isLocal = true
    type = AMResultItem.TYPE_SONG
    url = uri.toString()
    mixpanelSource = MixpanelSource.empty
    originalImage = DEFAULT_IMAGE_LARGE
    smallImage = DEFAULT_IMAGE_SMALL
}

private val DEFAULT_IMAGE_SMALL by lazy { "https://assets.audiomack.com/_default/default-song-image.png?width=${SizesRepository.smallMusic}" }
private val DEFAULT_IMAGE_LARGE by lazy { "https://assets.audiomack.com/_default/default-song-image.png?width=${SizesRepository.largeMusic}" }
