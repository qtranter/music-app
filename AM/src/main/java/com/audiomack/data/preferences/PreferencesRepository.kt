package com.audiomack.data.preferences

import android.app.Activity
import com.audiomack.GENERAL_PREFERENCES_HIGHLIGHTS_PLACEHOLDER_SHOWN
import com.audiomack.GENERAL_PREFERENCES_INCLUDE_LOCAL_FILES
import com.audiomack.GENERAL_PREFERENCES_LARGEMUSICCELLS
import com.audiomack.GENERAL_PREFERENCES_LOCAL_FILES_PROMPT_SHOWN
import com.audiomack.GENERAL_PREFERENCES_LOCAL_FILES_SELECTION_SHOWN
import com.audiomack.GENERAL_PREFERENCES_OFFLINE_SORTING
import com.audiomack.GENERAL_PREFERENCES_ONBOARDING_GENRE
import com.audiomack.GENERAL_PREFERENCES_SCREENSHOT_HINT_SHOWN
import com.audiomack.GENERAL_PREFERENCES_SLEEP_TIMER_PROMPT_STATUS
import com.audiomack.GENERAL_PREFERENCES_SLEEP_TIMER_TIMESTAMP
import com.audiomack.GENRE_PREFERENCES
import com.audiomack.GENRE_PREFERENCES_GENRE
import com.audiomack.MainApplication
import com.audiomack.model.AMResultItemSort
import com.audiomack.model.AMViewMode
import com.audiomack.utils.GeneralPreferencesHelper
import com.audiomack.utils.SecureSharedPreferences
import io.reactivex.Observable

// TODO move all stuff from GeneralPreferencesHelper

@Suppress("UNUSED_PARAMETER")
class PreferencesRepository : PreferencesDataSource {

    private val genrePreferences = SecureSharedPreferences(
        MainApplication.context,
        GENRE_PREFERENCES
    )
    private val generalPreferences = SecureSharedPreferences(MainApplication.context)

    override fun observeBoolean(key: String): Observable<Boolean> =
        Observable.merge(
            generalPreferences.changeObservable,
            Observable.just(key) // Used to trigger the first emission
        ).filter { it == key }
            .flatMap {
                val b = generalPreferences.getString(key)
                if (b != null) {
                    Observable.just(b.toBoolean())
                } else {
                    Observable.never()
                }
            }

    override fun observeString(key: String): Observable<String> =
        Observable.merge(
            generalPreferences.changeObservable,
            Observable.just(key) // Used to trigger the first emission
        ).filter { it == key }
            .flatMap {
                val s = generalPreferences.getString(key)
                if (s.isNullOrBlank()) {
                    Observable.never()
                } else {
                    Observable.just(s)
                }
            }

    override fun observeLong(key: String): Observable<Long> =
        Observable.merge(
            generalPreferences.changeObservable,
            Observable.just(key) // Used to trigger the first emission
        ).filter { it == key }
            .flatMap {
                val l = generalPreferences.getString(key)
                if (l != null) {
                    Observable.just(l.toLong())
                } else {
                    Observable.never()
                }
            }

    override var liveEnvironment: Boolean
        get() = GeneralPreferencesHelper.getInstance(MainApplication.context).isLiveEnvironment(MainApplication.context!!)
        set(value) {
            GeneralPreferencesHelper.getInstance(MainApplication.context).setLiveEnvironmentStatus(MainApplication.context!!, value)
        }

    override var trackingAds: Boolean
        get() = GeneralPreferencesHelper.getInstance(MainApplication.context).isTrackAds(MainApplication.context!!)
        set(value) {
            GeneralPreferencesHelper.getInstance(MainApplication.context).setTrackAds(MainApplication.context!!, value)
        }

    override var defaultGenre: DefaultGenre
        get() {
            return genrePreferences.getString(GENRE_PREFERENCES_GENRE)?.let {
                if ("AFROPOP" == it) {
                    DefaultGenre.AFROBEATS
                } else {
                    try {
                        DefaultGenre.valueOf(it)
                    } catch (e: Exception) {
                        DefaultGenre.ALL
                    }
                }
            } ?: run {
                DefaultGenre.ALL
            }
        }
        set(value) {
            genrePreferences.put(GENRE_PREFERENCES_GENRE, value.name)
        }

    override var needToShowHighlightsPlaceholder: Boolean
        get() = !(generalPreferences.getString(GENERAL_PREFERENCES_HIGHLIGHTS_PLACEHOLDER_SHOWN)?.toBoolean()
            ?: false)
        set(value) {
            generalPreferences.put(
                GENERAL_PREFERENCES_HIGHLIGHTS_PLACEHOLDER_SHOWN,
                value.toString()
            )
        }

    override fun needToShowPermissions(activity: Activity): Boolean {
        return GeneralPreferencesHelper.getInstance(activity).needToShowPermissions(activity)
    }

    override fun setPermissionsShown(activity: Activity, answer: String) {
        GeneralPreferencesHelper.getInstance(activity).setPermissionsAnswer(activity, answer)
    }

    override var queueAddToPlaylistTooltipShown: Boolean
        get() = !GeneralPreferencesHelper.getInstance(MainApplication.context).needToShowQueueAddToPlaylistTooltip(MainApplication.context!!)
        set(value) {
            if (value) {
                GeneralPreferencesHelper.getInstance(MainApplication.context).setQueueAddToPlaylistTooltipShown(MainApplication.context!!)
            }
        }

    override var onboardingGenre: String?
        get() = generalPreferences.getString(GENERAL_PREFERENCES_ONBOARDING_GENRE)
        set(value) {
            generalPreferences.put(GENERAL_PREFERENCES_ONBOARDING_GENRE, value)
        }

    override var needToShowPlaylistFavoriteTooltip: Boolean
        get() = GeneralPreferencesHelper.getInstance(MainApplication.context).needToShowPlaylistShuffleTooltip(MainApplication.context!!)
        set(value) {
            GeneralPreferencesHelper.getInstance(MainApplication.context).setPlaylistShuffleTooltipShown(MainApplication.context!!)
        }

    override var needToShowPlaylistDownloadTooltip: Boolean
        get() = GeneralPreferencesHelper.getInstance(MainApplication.context).needToShowPlaylistDownloadTooltip(MainApplication.context!!)
        set(value) {
            GeneralPreferencesHelper.getInstance(MainApplication.context).setPlaylistDownloadTooltipShown(MainApplication.context!!)
        }

    override var needToShowPlayerPlaylistTooltip: Boolean
        get() = GeneralPreferencesHelper.getInstance(MainApplication.context).needToShowPlayerPlaylistTooltip(MainApplication.context!!)
        set(value) {
            GeneralPreferencesHelper.getInstance(MainApplication.context).setPlayerPlaylistTooltipShown(MainApplication.context!!)
        }

    override var needToShowPlayerQueueTooltip: Boolean
        get() = GeneralPreferencesHelper.getInstance(MainApplication.context).needToShowPlayerQueueTooltip(MainApplication.context!!)
        set(value) {
            GeneralPreferencesHelper.getInstance(MainApplication.context).setPlayerQueueTooltipShown(MainApplication.context!!)
        }

    override var needToShowCommentTooltip: Boolean
        get() = GeneralPreferencesHelper.getInstance(MainApplication.context).needToShowCommentTooltip(MainApplication.context)
        set(value) {
            GeneralPreferencesHelper.getInstance(MainApplication.context).setCommentTooltipShown(MainApplication.context)
        }

    override var musicCellViewMode: AMViewMode
        get() = if ((generalPreferences.getString(GENERAL_PREFERENCES_LARGEMUSICCELLS)?.toIntOrNull() ?: 0) == 1) AMViewMode.Tile else AMViewMode.List
        set(value) {
            generalPreferences.put(GENERAL_PREFERENCES_LARGEMUSICCELLS, if (value == AMViewMode.Tile) "1" else "0")
        }

    override var needToShowContactTooltip: Boolean
        get() = GeneralPreferencesHelper.getInstance(MainApplication.context).needToShowContactTooltip(MainApplication.context)
        set(value) {
            GeneralPreferencesHelper.getInstance(MainApplication.context).setContactTooltipShown(MainApplication.context)
        }

    override var offlineSorting: AMResultItemSort
        get() {
            return genrePreferences.getString(GENERAL_PREFERENCES_OFFLINE_SORTING)?.let {
                try {
                    AMResultItemSort.valueOf(it)
                } catch (e: Exception) {
                    AMResultItemSort.NewestFirst
                }
            } ?: run {
                AMResultItemSort.NewestFirst
            }
        }
        set(value) {
            genrePreferences.put(GENERAL_PREFERENCES_OFFLINE_SORTING, value.name)
        }

    override var screenshotHintShown: Boolean
        get() = generalPreferences.getString(GENERAL_PREFERENCES_SCREENSHOT_HINT_SHOWN)?.toBoolean() == true
        set(value) {
            generalPreferences.put(GENERAL_PREFERENCES_SCREENSHOT_HINT_SHOWN, value.toString())
        }

    override var sleepTimerTimestamp: Long
        get() = generalPreferences.getString(GENERAL_PREFERENCES_SLEEP_TIMER_TIMESTAMP)?.toLongOrNull() ?: 0L
        set(value) {
            generalPreferences.put(
                GENERAL_PREFERENCES_SLEEP_TIMER_TIMESTAMP,
                if (value == 0L) "" else value.toString()
            )
        }

    override var excludeReUps: Boolean
        get() = GeneralPreferencesHelper.getInstance(MainApplication.context)
            .isExcludeReups(MainApplication.context!!)
        set(value) {
            GeneralPreferencesHelper.getInstance(MainApplication.context)
                .setExcludeReups(MainApplication.context!!, value)
        }

    override var includeLocalFiles: Boolean
        get() = generalPreferences.getString(GENERAL_PREFERENCES_INCLUDE_LOCAL_FILES)
            ?.toBoolean() == true
        set(value) {
            generalPreferences.put(GENERAL_PREFERENCES_INCLUDE_LOCAL_FILES, value.toString())
        }

    override var localFileSelectionShown: Boolean
        get() = generalPreferences.getString(GENERAL_PREFERENCES_LOCAL_FILES_SELECTION_SHOWN)
            ?.toBoolean() == true
        set(value) {
            generalPreferences.put(
                GENERAL_PREFERENCES_LOCAL_FILES_SELECTION_SHOWN,
                value.toString()
            )
        }

    override var localFilePromptShown: Boolean
        get() = generalPreferences.getString(GENERAL_PREFERENCES_LOCAL_FILES_PROMPT_SHOWN)
            ?.toBoolean() == true
        set(value) {
            generalPreferences.put(
                GENERAL_PREFERENCES_LOCAL_FILES_PROMPT_SHOWN,
                value.toString()
            )
        }

    override var sleepTimerPromptStatus: SleepTimerPromptStatus
        get() = generalPreferences.getString(GENERAL_PREFERENCES_SLEEP_TIMER_PROMPT_STATUS)?.let { key ->
            SleepTimerPromptStatus.values().firstOrNull { it.key == key } ?: SleepTimerPromptStatus.Unknown
        } ?: SleepTimerPromptStatus.Unknown
        set(value) {
            generalPreferences.put(GENERAL_PREFERENCES_SLEEP_TIMER_PROMPT_STATUS, value.key)
        }

    override val playCount: Long
        get() = GeneralPreferencesHelper.getInstance(MainApplication.context).getPlayCount(MainApplication.context!!)

    override fun incrementPlayCount() {
        GeneralPreferencesHelper.getInstance(MainApplication.context).incrementPlayCount(MainApplication.context!!)
    }
}
