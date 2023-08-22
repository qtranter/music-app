package com.audiomack.model

enum class CommentMethod {

    UpVote {
        override fun stringValue() = "UpVote"
    },
    DownVote {
        override fun stringValue() = "DownVote"
    },
    Report {
        override fun stringValue() = "Report"
    };
    abstract fun stringValue(): String
}
