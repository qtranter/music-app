package com.audiomack.data.preferences

import com.audiomack.model.AMGenre

enum class DefaultGenre {

    ALL {
        override fun genreKey() = null
    },
    HIPHOP {
        override fun genreKey() = AMGenre.Rap.apiValue()
    },
    ELECTRONIC {
        override fun genreKey() = AMGenre.Electronic.apiValue()
    },
    REGGAE {
        override fun genreKey() = AMGenre.Dancehall.apiValue()
    },
    POP {
        override fun genreKey() = AMGenre.Pop.apiValue()
    },
    AFROBEATS {
        override fun genreKey() = AMGenre.Afrobeats.apiValue()
    },
    PODCAST {
        override fun genreKey() = AMGenre.Podcast.apiValue()
    },
    RNB {
        override fun genreKey() = AMGenre.Rnb.apiValue()
    },
    INSTRUMENTALS {
        override fun genreKey() = AMGenre.Instrumental.apiValue()
    },
    LATIN {
        override fun genreKey() = AMGenre.Latin.apiValue()
    };

    abstract fun genreKey(): String?
}
