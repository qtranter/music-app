package com.audiomack.data.inapprating

import com.audiomack.INAPPRATING_PREFERENCES
import com.audiomack.INAPPRATING_PREFERENCES_ANSWER
import com.audiomack.INAPPRATING_PREFERENCES_DOWNLOADS
import com.audiomack.INAPPRATING_PREFERENCES_FAVORITES
import com.audiomack.INAPPRATING_PREFERENCES_TIMESTAMP
import com.audiomack.MainApplication
import com.audiomack.utils.SecureSharedPreferences

interface InAppRatingPreferences {
    val downloadsCount: Long
    val favoritesCount: Long
    var answer: String
    var timestamp: Long
    fun incrementDownloadCount()
    fun incrementFavoriteCount()
}

object InAppRatingPreferencesImpl : InAppRatingPreferences {

    private val preferences = SecureSharedPreferences(
        MainApplication.context,
        INAPPRATING_PREFERENCES
    )

    private var downloads: Long = 0
        get() = preferences.getString(INAPPRATING_PREFERENCES_DOWNLOADS)?.toLongOrNull() ?: 0
        set(value) {
            field = value
            preferences.put(INAPPRATING_PREFERENCES_DOWNLOADS, value.toString())
        }

    private var favorites: Long = 0
        get() = preferences.getString(INAPPRATING_PREFERENCES_FAVORITES)?.toLongOrNull() ?: 0
        set(value) {
            field = value
            preferences.put(INAPPRATING_PREFERENCES_FAVORITES, value.toString())
        }

    override var answer: String = ""
        get() = preferences.getString(INAPPRATING_PREFERENCES_ANSWER) ?: ""
        set(value) {
            field = value
            preferences.put(INAPPRATING_PREFERENCES_ANSWER, value)
        }

    override var timestamp: Long = 0
        get() = preferences.getString(INAPPRATING_PREFERENCES_TIMESTAMP)?.toLongOrNull() ?: 0
        set(value) {
            field = value
            preferences.put(INAPPRATING_PREFERENCES_TIMESTAMP, value.toString())
        }

    override fun incrementDownloadCount() {
        downloads += 1
    }

    override fun incrementFavoriteCount() {
        favorites += 1
    }

    override val downloadsCount: Long
        get() = downloads

    override val favoritesCount: Long
        get() = favorites
}
