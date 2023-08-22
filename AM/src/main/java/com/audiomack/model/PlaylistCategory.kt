package com.audiomack.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

@Parcelize
data class PlaylistCategory(
    var id: String,
    var title: String,
    var slug: String
) : Parcelable {

    constructor(json: JSONObject) : this(
        json.optString("id"),
        json.optString("title"),
        json.optString("url_slug")
    )
}
