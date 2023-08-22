package com.audiomack.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * This is meant to replace [AMArtist] as the only representation of a artist object.
 * It has no dependencies on the underlying DB nor network layer.
 * It's implementing the [Parcelable] interface, so that we can easily pass data through Bundles.
 * **/

@Parcelize
data class Artist(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val largeImage: String = ""
) : Parcelable {

    constructor(artist: AMArtist) : this(
        artist.artistId ?: "",
        artist.name ?: "",
        artist.urlSlug ?: "",
        artist.largeImage ?: ""
    )
}
