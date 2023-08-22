package com.audiomack.model

enum class SongEndType {

    Skip {
        override fun stringValue() = "Skip"
    },
    Completed {
        override fun stringValue() = "Listen to Full Song"
    },
    ChangedSong {
        override fun stringValue() = "Play Different Song"
    };

    abstract fun stringValue(): String
}
