package com.audiomack.model

import android.content.Context
import com.audiomack.R

enum class AMGenre {
    All {
        override fun apiValue(): String = "all"
        override fun humanValue(context: Context?): String = context?.getString(R.string.browse_filter_allgenres) ?: ""
    }, Rnb {
        override fun apiValue(): String = "rnb"
        override fun humanValue(context: Context?): String = "R&B"
    }, Dancehall {
        override fun apiValue(): String = "dancehall"
        override fun humanValue(context: Context?): String = "Reggae/Dancehall"
    }, Afrobeats {
        override fun apiValue(): String = "afrobeats"
        override fun humanValue(context: Context?): String = "Afrobeats"
    }, Instrumental {
        override fun apiValue(): String = "instrumental"
        override fun humanValue(context: Context?): String = "Instrumentals"
    }, Rap {
        override fun apiValue(): String = "rap"
        override fun humanValue(context: Context?): String = "Hip-Hop"
    }, Electronic {
        override fun apiValue(): String = "electronic"
        override fun humanValue(context: Context?): String = "Electronic"
    }, Latin {
        override fun apiValue(): String = "latin"
        override fun humanValue(context: Context?): String = "Latin"
    }, Pop {
        override fun apiValue(): String = "pop"
        override fun humanValue(context: Context?): String = "Pop"
    }, Podcast {
        override fun apiValue(): String = "podcast"
        override fun humanValue(context: Context?): String = "Podcast"
    }, Rock {
        override fun apiValue(): String = "rock"
        override fun humanValue(context: Context?): String = "Rock"
    }, Jazz {
        override fun apiValue(): String = "jazz"
        override fun humanValue(context: Context?): String = "Jazz/Blues"
    }, Country {
        override fun apiValue(): String = "country"
        override fun humanValue(context: Context?): String = "Country"
    }, World {
        override fun apiValue(): String = "world"
        override fun humanValue(context: Context?): String = "World"
    }, Classical {
        override fun apiValue(): String = "classical"
        override fun humanValue(context: Context?): String = "Classical"
    }, Gospel {
        override fun apiValue(): String = "gospel"
        override fun humanValue(context: Context?): String = "Gospel"
    }, Acapella {
        override fun apiValue(): String = "acapella"
        override fun humanValue(context: Context?): String = "Acapella"
    }, Djmix {
        override fun apiValue(): String = "dj-mix"
        override fun humanValue(context: Context?): String = "DJ Mix"
    }, Folk {
        override fun apiValue(): String = "folk"
        override fun humanValue(context: Context?): String = "Folk"
    }, Other {
        override fun apiValue(): String = "other"
        override fun humanValue(context: Context?): String = "Other"
    };

    abstract fun apiValue(): String
    abstract fun humanValue(context: Context?): String

    companion object {
        @JvmStatic
        fun fromApiValue(apiValue: String?): AMGenre {
            if (apiValue == "afropop") {
                return Afrobeats
            }
            return values().firstOrNull { it.apiValue() == apiValue } ?: Other
        }
        @JvmStatic
        fun fromHumanValue(humanValue: String?, context: Context?): AMGenre {
            return values().firstOrNull { it.humanValue(context) == humanValue } ?: Other
        }
    }
}
