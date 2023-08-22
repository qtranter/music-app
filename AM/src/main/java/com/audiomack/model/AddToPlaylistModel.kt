package com.audiomack.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AddToPlaylistModel(
    val songs: List<Music>,
    val genre: String,
    val thumbnail: String,
    val mixpanelSource: MixpanelSource,
    val mixpanelButton: String
) : Parcelable {

    constructor(songs: List<AMResultItem>, mixpanelSource: MixpanelSource, mixpanelButton: String) :
        this(
            songs.map { Music(it) }.filterNot { it.id.isEmpty() },
            songs.firstOrNull()?.genre ?: AMGenre.Rap.apiValue(),
            songs.firstOrNull()?.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall) ?: "",
            mixpanelSource,
            mixpanelButton
    )
}
