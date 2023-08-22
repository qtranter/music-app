package com.audiomack.data.tracking.appsflyer

enum class AppsFlyerEvents {

    Signup {
        override fun stringValue() = "af_user_signup"
    },
    AddToFavorites {
        override fun stringValue() = "af_user_favorite"
    },
    CreatePlaylist {
        override fun stringValue() = "af_create_playlist"
    },
    ShareContent {
        override fun stringValue() = "af_user_share"
    },
    PlaySong {
        override fun stringValue() = "af_song_play"
    },
    PremiumView {
        override fun stringValue() = "af_premium_view"
    },
    PremiumStart {
        override fun stringValue() = "af_premium_start"
    },
    PremiumTrial {
        override fun stringValue() = "af_premium_trial"
    },
    AdWatched {
        override fun stringValue() = "af_ad_watched"
    };

    abstract fun stringValue(): String
}
