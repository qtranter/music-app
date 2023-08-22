package com.audiomack.model

import android.os.Parcelable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WorldArticle(
    val id: String?,
    val title: String?,
    val slug: String?,
    val html: String?,
    val feature_image: String?,
    val excerpt: String?,
    val published_at: String?,
    val tagName: String?
) : Parcelable {

    fun publishedDate(): Date? {
        return published_at?.let {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).parse(published_at)
            } catch (e: Exception) { null }
        }
    }
}
