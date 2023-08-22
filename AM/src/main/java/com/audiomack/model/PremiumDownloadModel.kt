package com.audiomack.model

import android.os.Parcelable
import kotlin.math.max
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PremiumDownloadModel(
    val music: PremiumDownloadMusicModel? = null,
    val stats: PremiumDownloadStatsModel = PremiumDownloadStatsModel("", MixpanelSource.empty, 0, 0),
    val infoTypeLimited: PremiumLimitedDownloadInfoViewType = PremiumLimitedDownloadInfoViewType.Download,
    val alertTypeLimited: PremiumLimitedDownloadAlertViewType? = null,
    val alertTypePremium: PremiumOnlyDownloadAlertViewType? = null,
    val actionToBeResumed: ActionToBeResumed = ActionToBeResumed.Play
) : Parcelable

enum class PremiumLimitedDownloadInfoViewType {
    Download,
    FirstDownload
}

enum class PremiumLimitedDownloadAlertViewType {
    ReachedLimit,
    DownloadAlbumLargerThanLimit,
    DownloadAlbumLargerThanLimitAlreadyDownloaded,
    PlayFrozenOffline,
    DownloadFrozen,
    PlayFrozenOfflineWithAvailableUnfreezes
}

enum class PremiumOnlyDownloadAlertViewType {
    Download,
    DownloadFrozenOrPlayFrozenOffline
}

@Parcelize
data class PremiumDownloadStatsModel(
    val mixpanelButton: String,
    val mixpanelSource: MixpanelSource,
    val premiumLimitCount: Int,
    val premiumLimitUnfrozenDownloadCount: Int
) : Parcelable {

    fun replaceCount(downloadCount: Int): Int {
        return downloadCount - availableCount
    }

    val availableCount: Int
        get() {
            return max(premiumLimitCount - premiumLimitUnfrozenDownloadCount, 0)
        }
}

@Parcelize
data class PremiumDownloadMusicModel(
    val musicId: String,
    val type: MusicType,
    val countOfSongsToBeDownloaded: Int,
    val albumTracksIds: List<String>
) : Parcelable {
    constructor(item: AMResultItem, downloadCount: Int = 1) : this(
        musicId = item.getItemId(),
        type = if (item.isPlaylist) MusicType.Playlist else if (item.isAlbum) MusicType.Album else MusicType.Song,
        countOfSongsToBeDownloaded = downloadCount,
        albumTracksIds = if (item.isAlbum) item.tracks?.mapNotNull { it.itemId } ?: emptyList() else emptyList()
    )
}

enum class ActionToBeResumed {
    Play,
    PlayNext,
    PlayLater,
    Shuffle
}
