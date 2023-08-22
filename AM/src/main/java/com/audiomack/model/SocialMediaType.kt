package com.audiomack.model

enum class SocialMediaType {

    Twitter {
        override fun stringValue() = "Twitter"
    },
    Facebook {
        override fun stringValue() = "Facebook"
    },
    Instagram {
        override fun stringValue() = "Instagram"
    },
    YouTube {
        override fun stringValue() = "YouTube"
    };

    abstract fun stringValue(): String
}
