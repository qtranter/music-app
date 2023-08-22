package com.audiomack.model

enum class SocialNetwork {

    Twitter {
        override fun stringValue() = "twitter"
    },
    Facebook {
        override fun stringValue() = "facebook"
    },
    Instagram {
        override fun stringValue() = "instagram"
    },
    YouTube {
        override fun stringValue() = "youTube"
    };

    abstract fun stringValue(): String
}
