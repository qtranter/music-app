package com.audiomack.model

enum class SearchType {

    Trending {
        override fun stringValue() = "Trending"
    },
    Recent {
        override fun stringValue() = "Recent"
    },
    Suggestion {
        override fun stringValue() = "Suggestion"
    },
    Direct {
        override fun stringValue() = "Direct"
    },
    MusicInfo {
        override fun stringValue() = "Music Info"
    },
    NowPlaying {
        override fun stringValue() = "Now Playing"
    },
    LibrarySearch {
        override fun stringValue() = "Library Search"
    },
    Tag {
        override fun stringValue() = "Tag"
    };

    abstract fun stringValue(): String
}
