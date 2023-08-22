package com.audiomack.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * This is meant to replace [AMResultItem] as the only representation of a music object: song, album, playlist, album track or playlist track.
 * It has no dependencies on the underlying DB nor network layer.
 * It's implementing the [Parcelable] interface, so that we can easily pass data through Bundles.
 * **/

@Parcelize
data class Music(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val genre: String = "",
    val feat: String = "",
    val type: MusicType = MusicType.Song,
    val originalImageUrl: String = "",
    val uploaderName: String = "",
    val uploaderSlug: String = "",
    val uploaderLargeImage: String = ""
) : Parcelable {

    constructor(item: AMResultItem) : this(
        item.itemId ?: "",
        item.title ?: "",
        item.artist ?: "",
        item.genre ?: "",
        item.featured ?: "",
        when {
            item.isPlaylist -> MusicType.Playlist
            item.isAlbum -> MusicType.Album
            else -> MusicType.Song
        },
        item.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetOriginal) ?: "",
        item.uploaderName ?: "",
        item.uploaderSlug ?: "",
        item.uploaderLargeImage ?: ""
    )
}

enum class MusicType(val typeForMusicApi: String) {
    Playlist("playlist"),
    Album("album"),
    Song("song")
}
