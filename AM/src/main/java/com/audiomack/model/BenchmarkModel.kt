package com.audiomack.model

import android.content.Context
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import com.audiomack.R
import com.audiomack.utils.Utils
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BenchmarkModel(
    var type: BenchmarkType = BenchmarkType.NONE,
    var imageUrl: String ? = null,
    var milestone: Long = 0,
    var selected: Boolean = false,
    @DrawableRes var badgeIconId: Int? = null
) : Parcelable {

    @VisibleForTesting
    fun sampledValue(): Long {
        return breakpoints.firstOrNull { it <= milestone } ?: 0
    }

    fun getPrettyMilestone(context: Context): String {
        val sampled = sampledValue()
        return when {
            sampled >= 1_000_000_000 ->
                String.format(context.resources.getString(R.string.benchmark_billion, sampled / 1_000_000_000))
            sampled >= 1_000_000 ->
                String.format(context.resources.getString(R.string.benchmark_million, sampled / 1_000_000))
            else -> "%,d".format(sampled)
        }
    }

    fun nextMilestone(): String? {
        if (type == BenchmarkType.PLAY || type == BenchmarkType.FAVORITE || type == BenchmarkType.PLAYLIST || type == BenchmarkType.REPOST) {
            breakpoints.reversed().firstOrNull { it > milestone }?.let {
                return Utils.formatShortStatNumberWithoutDecimals(it)
            }
        }
        return null
    }

    val prettyTypeForAnalytics: String
        get() {
            return when (type) {
                BenchmarkType.NONE -> "NowPlaying"
                BenchmarkType.PLAY -> "$milestone Plays"
                BenchmarkType.FAVORITE -> "$milestone Favorites"
                BenchmarkType.PLAYLIST -> "$milestone Adds"
                BenchmarkType.REPOST -> "$milestone Reups"
                BenchmarkType.VERIFIED -> "Verified Artist"
                BenchmarkType.TASTEMAKER -> "Tastemaker Artist"
                BenchmarkType.AUTHENTICATED -> "Authenticated Artist"
                BenchmarkType.ON_AUDIOMACK -> "On Audiomack"
            }
        }

    companion object {
        fun getBenchmarkList(music: AMResultItem): ArrayList<BenchmarkModel> {
            return ArrayList<BenchmarkModel>().apply {
                add(BenchmarkModel(BenchmarkType.NONE, music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall)))
                if (music.hasStats()) {
                    if (music.playsCount.toInt() >= 1000) add(BenchmarkModel(BenchmarkType.PLAY, music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall), music.playsCount))
                    if (music.favoritesCount.toInt() >= 100) add(BenchmarkModel(BenchmarkType.FAVORITE, music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall), music.favoritesCount))
                    if (music.playlistsCount.toInt() >= 100) add(BenchmarkModel(BenchmarkType.PLAYLIST, music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall), music.playlistsCount))
                    if (music.repostsCount.toInt() >= 100) add(BenchmarkModel(BenchmarkType.REPOST, music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall), music.repostsCount))
                }
                when {
                    music.isUploaderVerified -> add(BenchmarkModel(BenchmarkType.VERIFIED, music.uploaderLargeImage))
                    music.isUploaderTastemaker -> add(BenchmarkModel(BenchmarkType.TASTEMAKER, music.uploaderLargeImage))
                    music.isUploaderAuthenticated -> add(BenchmarkModel(BenchmarkType.AUTHENTICATED, music.uploaderLargeImage))
                }
                add(BenchmarkModel(BenchmarkType.ON_AUDIOMACK, music.uploaderLargeImage, badgeIconId = when {
                    music.isUploaderVerified -> R.drawable.ic_verified
                    music.isUploaderAuthenticated -> R.drawable.ic_authenticated
                    music.isUploaderTastemaker -> R.drawable.ic_tastemaker
                    else -> null
                }))
            }
        }
    }
}

private val breakpoints = listOf<Long>(
    1_000_000_000,
    500_000_000,
    100_000_000,
    50_000_000,
    10_000_000,
    5_000_000,
    1_000_000,
    500_000,
    250_000,
    100_000,
    50_000,
    10_000,
    5_000,
    1_000,
    100
)
