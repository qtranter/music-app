package com.audiomack.model

enum class CommentSort {

    Top {
        override fun stringValue() = "vote"
    },
    Newest {
        override fun stringValue() = "date"
    },
    Oldest {
        override fun stringValue() = "old"
    };

    abstract fun stringValue(): String
}
