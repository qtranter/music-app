package com.audiomack.data.queue

import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource

@Suppress("unused")
class QueueException : Exception {
    constructor(message: String?) : super(message)
    constructor(throwable: Throwable) : super(throwable)
    constructor(message: String?, throwable: Throwable) : super(message, throwable)
}

/**
 * Returns a list of tracks, replacing any album items with it's tracks
 */
fun List<AMResultItem>.flatten(
    offlineScreen: Boolean = false,
    source: MixpanelSource? = null,
    allowFrozenTracks: Boolean = false
): List<AMResultItem> {
    val list = mutableListOf<AMResultItem>()
    forEach { item ->
        if (item.isAlbum) {
            val tracks = item.tracks.also { if (it.isNullOrEmpty()) item.loadTracks() }
            tracks?.forEach { track ->
                if (shouldBeIncluded(track, allowFrozenTracks || !offlineScreen)) {
                    addSource(track, offlineScreen, source)
                    list.add(track)
                }
            }
        } else if (!item.isPlaylist && shouldBeIncluded(item, allowFrozenTracks || !offlineScreen)) {
            addSource(item, offlineScreen, source)
            list.add(item)
        }
    }
    return list
}

private fun addSource(
    item: AMResultItem,
    offlineScreen: Boolean,
    source: MixpanelSource?
) {
    if (source != null && (!offlineScreen || item.isDownloadCompleted(false))) {
        item.mixpanelSource = source
    }
}

private fun shouldBeIncluded(
    item: AMResultItem,
    allowFrozenTracks: Boolean
): Boolean = !item.isGeoRestricted && (allowFrozenTracks || !(item.isDownloadFrozen || item.isPremiumOnlyDownloadFrozen))
