package com.audiomack.data.onboarding

import com.audiomack.MainApplication
import com.audiomack.ONBOARDING_PREFERENCES
import com.audiomack.ONBOARDING_PREFERENCES_GENRE
import com.audiomack.ONBOARDING_PREFERENCES_SESSION_COUNT
import com.audiomack.utils.SecureSharedPreferences

interface OnboardingPlaylistsGenreProvider {
    val categoryId: String?
    fun setOnboardingGenre(genre: String?)
    fun trackAppSession()
}

class OnboardingPlaylistsGenreProviderImpl : OnboardingPlaylistsGenreProvider {

    private val genreToCategoryIdMap = mapOf(
        "rnb" to "hip-hop-rb",
        "r&b" to "hip-hop-rb",
        "rap" to "hip-hop-rb",
        "hiphop" to "hip-hop-rb",
        "hip-hop" to "hip-hop-rb",
        "reggae" to "caribbean",
        "dancehall" to "caribbean",
        "afrobeats" to "afrobeats",
        "podcast" to "podcast",
        "latin" to "latin",
        "electronic" to "electronic",
        "edm" to "electronic",
        "pop" to "pop",
        "rock" to "rock"
    )
    private val maxOnboardingSessionCount = 5

    private val prefs = SecureSharedPreferences(MainApplication.context, ONBOARDING_PREFERENCES)

    override val categoryId: String?
        get() {
            val sessionCount = prefs.getString(ONBOARDING_PREFERENCES_SESSION_COUNT)?.toIntOrNull() ?: 0
            if (sessionCount >= maxOnboardingSessionCount) {
                return null
            }
            val genre = savedGenre
            return if (genreToCategoryIdMap.containsKey(genre)) genreToCategoryIdMap[genre] else null
        }

    override fun setOnboardingGenre(genre: String?) {
        savedGenre = genre
    }

    override fun trackAppSession() {
        val sessionCount = prefs.getString(ONBOARDING_PREFERENCES_SESSION_COUNT)?.toIntOrNull() ?: 0
        if (sessionCount < maxOnboardingSessionCount) {
            prefs.put(ONBOARDING_PREFERENCES_SESSION_COUNT, (sessionCount + 1).toString())
        }
    }

    private var savedGenre: String? = null
        get() {
            return prefs.getString(ONBOARDING_PREFERENCES_GENRE)
        }
        set(value) {
            prefs.put(ONBOARDING_PREFERENCES_GENRE, value)
            field = value
        }
}
