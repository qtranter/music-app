package com.audiomack.model

enum class QueueType {

    PlayNext {
        override fun stringValue() = "Add Next"
    },
    AddToQueue {
        override fun stringValue() = "Add to End"
    };

    abstract fun stringValue(): String
}
