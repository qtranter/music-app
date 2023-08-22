package com.audiomack.model

import android.os.Parcelable
import com.audiomack.network.retrofitApi.WorldPostService
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WorldPage(
    val title: String,
    val slug: String
) : Parcelable {

    companion object {
        val all: WorldPage = WorldPage(
            "Home",
            WorldPostService.FILTER_ALL
        )
    }
}
