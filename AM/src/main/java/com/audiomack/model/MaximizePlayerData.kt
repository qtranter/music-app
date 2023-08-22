package com.audiomack.model

data class MaximizePlayerData(
    val item: AMResultItem? = null,
    val collection: AMResultItem? = null,
    val items: List<AMResultItem>? = null,
    val nextPageData: NextPageData? = null,
    val inOfflineScreen: Boolean = false,
    val loadFullPlaylist: Boolean = false,
    val albumPlaylistIndex: Int? = null,
    val mixpanelSource: MixpanelSource? = null,
    val shuffle: Boolean = false,
    val scrollToTop: Boolean = false,
    val openShare: Boolean = false,
    val allowFrozenTracks: Boolean = false,
    val animated: Boolean = true
) {
    override fun toString(): String {
        return "MaximizePlayerData(" +
            "animated=$animated, " +
            "item=$item, " +
            "collection=$collection, " +
            "items=${items?.size}, " +
            "nextPageData=$nextPageData, " +
            "inOfflineScreen=$inOfflineScreen, " +
            "loadFullPlaylist=$loadFullPlaylist, " +
            "albumPlaylistIndex=$albumPlaylistIndex, " +
            "mixpanelSource=$mixpanelSource, " +
            "shuffle=$shuffle, " +
            "scrollToTop=$scrollToTop, " +
            "openShare=$openShare, " +
            "allowFrozenTracks=$allowFrozenTracks" +
            ")"
    }
}
