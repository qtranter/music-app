package com.audiomack.model

enum class SearchReturnType {

    Requested {
        override fun stringValue() = "Requested"
    },
    Replacement {
        override fun stringValue() = "Replacement"
    };

    abstract fun stringValue(): String
}
