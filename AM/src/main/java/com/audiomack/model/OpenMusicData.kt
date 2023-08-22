package com.audiomack.model

/**
 * @param item: current song/album/playlist to be played
 * @param items: list of songs/albums/playlists to be added to the queue, including [item]
 * @param mixpanelSource: contains information about the origin of what is being played
 * @param url: optional [String] with the url used to get the [items]
 * @param currentPage: the current page number used to populate [items].
 *                     Together with [url], if provided, will be used to fetch one more page
 *                     of music when the player reaches the end of the queue.
 */
data class OpenMusicData(
    val item: AMResultItem,
    val items: List<AMResultItem>,
    val source: MixpanelSource,
    val openShare: Boolean,
    val url: String?,
    val page: Int
)
