@file:JvmName("DownloadAnalyticsUtil")

package com.audiomack.download

import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.model.AMResultItem

interface DownloadAnalytics {
    fun eventDownloadStart(track: AMResultItem)
    fun eventDownloadFailure(exception: Exception, extraInfo: String?)
    fun eventDownloadComplete(track: AMResultItem)
}

private object DownloadAnalyticEvent {
    const val DOWNLOAD_SUCCESSFUL = "Download Successful"
    const val DOWNLOAD_FAIL = "Download Failed"
    const val DOWNLOAD_SONG = "Download Song"
}

enum class DownloadOrigin(val analyticsName: String) {
    PLAYER("Now Playing"),
    ALBUM("Album"),
    BASE_PLAYLIST("Playlist"),
    BASE_SINGLE("Single"),
    RESTORE_DOWNLOADS("Restore")
}

class AmDownloadAnalytics(
    private val origin: DownloadOrigin,
    private val trackingDataSource: TrackingDataSource
) : DownloadAnalytics {

    override fun eventDownloadStart(track: AMResultItem) {
        trackingDataSource.trackGA(
            DownloadAnalyticEvent.DOWNLOAD_SONG,
            origin.analyticsName,
            track.itemId + " - " + track.artist + " - " + track.title
        )
    }

    override fun eventDownloadFailure(exception: Exception, extraInfo: String?) {
        trackingDataSource.trackGA(
            DownloadAnalyticEvent.DOWNLOAD_FAIL,
            exception.message,
            extraInfo
        )
    }

    override fun eventDownloadComplete(track: AMResultItem) {
        trackingDataSource.trackGA(
            DownloadAnalyticEvent.DOWNLOAD_SUCCESSFUL,
            track.type ?: "song",
            track.itemId + " - " + track.artist + " - " + track.title
        )
    }
}
