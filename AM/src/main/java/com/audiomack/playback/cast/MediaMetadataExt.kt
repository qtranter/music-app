package com.audiomack.playback.cast

import com.google.android.gms.cast.MediaMetadata
import java.util.Calendar

inline var MediaMetadata.title: String?
    get() = getString(MediaMetadata.KEY_TITLE)
    set(value) {
        putString(MediaMetadata.KEY_TITLE, value)
    }

inline var MediaMetadata.artist: String?
    get() = getString(MediaMetadata.KEY_ARTIST)
    set(value) {
        putString(MediaMetadata.KEY_ARTIST, value)
    }

inline var MediaMetadata.releaseDate: Calendar?
    get() = getDate(MediaMetadata.KEY_RELEASE_DATE)
    set(value) {
        putDate(MediaMetadata.KEY_RELEASE_DATE, value)
    }

inline var MediaMetadata.albumTitle: String?
    get() = getString(MediaMetadata.KEY_ALBUM_TITLE)
    set(value) {
        putString(MediaMetadata.KEY_ALBUM_TITLE, value)
    }

inline var MediaMetadata.trackNumber: Int?
    get() = getInt(MediaMetadata.KEY_TRACK_NUMBER)
    set(value) {
        putInt(MediaMetadata.KEY_TRACK_NUMBER, value ?: 0)
    }
