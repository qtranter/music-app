package com.audiomack.data.preferences

import android.app.Activity
import com.audiomack.model.AMResultItemSort
import com.audiomack.model.AMViewMode
import io.reactivex.Observable

interface PreferencesDataSource {

    fun observeBoolean(key: String): Observable<Boolean>

    fun observeString(key: String): Observable<String>

    fun observeLong(key: String): Observable<Long>

    var liveEnvironment: Boolean

    var trackingAds: Boolean

    var defaultGenre: DefaultGenre

    var needToShowHighlightsPlaceholder: Boolean

    fun needToShowPermissions(activity: Activity): Boolean

    fun setPermissionsShown(activity: Activity, answer: String)

    var queueAddToPlaylistTooltipShown: Boolean

    var onboardingGenre: String?

    var needToShowPlaylistFavoriteTooltip: Boolean

    var needToShowPlaylistDownloadTooltip: Boolean

    var needToShowPlayerPlaylistTooltip: Boolean

    var needToShowPlayerQueueTooltip: Boolean

    var needToShowCommentTooltip: Boolean

    var musicCellViewMode: AMViewMode

    var needToShowContactTooltip: Boolean

    var offlineSorting: AMResultItemSort

    var screenshotHintShown: Boolean

    var sleepTimerTimestamp: Long

    var excludeReUps: Boolean

    var includeLocalFiles: Boolean

    var localFileSelectionShown: Boolean

    var localFilePromptShown: Boolean

    var sleepTimerPromptStatus: SleepTimerPromptStatus

    val playCount: Long

    fun incrementPlayCount()
}

enum class SleepTimerPromptStatus(val key: String) {
    Unknown("unknown"),
    Shown("shown"),
    Skipped("skipped"),
    NotShown("notShown")
}
