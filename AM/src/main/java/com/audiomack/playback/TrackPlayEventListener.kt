package com.audiomack.playback

import com.audiomack.MainApplication
import com.audiomack.model.AMResultItem
import com.audiomack.model.Credentials
import com.audiomack.network.AnalyticsHelper

class TrackPlayEventListener(
    private var credentials: Credentials? = null
) : PlayEventListener {

    init {
        if (credentials == null) {
            credentials = Credentials.load(MainApplication.context)
        }
    }

    override fun trackPlayEvent(item: AMResultItem) {
        val userSlug = credentials?.userUrlSlug ?: ""
        val songTitle = item.title
        val albumTitle = item.album
        val fullTitle = (if (!albumTitle.isNullOrEmpty()) "$albumTitle/" else "") + songTitle!!
        val genreCodeNonNull = item.genre ?: ""

        AnalyticsHelper.getInstance()
            .trackEventOnFirebase(
                "play", hashMapOf(
                    "Action" to userSlug,
                    "Label" to fullTitle,
                    "Genre" to genreCodeNonNull
                )
            )
    }
}
