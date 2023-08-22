package com.audiomack.model

enum class ShareMethod {

    Twitter {
        override fun stringValue() = "Twitter"
    },
    SMS {
        override fun stringValue() = "Contacts"
    },
    Standard {
        override fun stringValue() = "Via App"
    },
    CopyLink {
        override fun stringValue() = "Copy Link"
    },
    Facebook {
        override fun stringValue() = "Facebook"
    },
    Screenshot {
        override fun stringValue() = "Screenshot"
    },
    Instagram {
        override fun stringValue() = "Instagram"
    },
    Snapchat {
        override fun stringValue() = "Snapchat"
    },
    WhatsApp {
        override fun stringValue() = "Whatsapp"
    },
    Messenger {
        override fun stringValue() = "Messenger"
    },
    WeChat {
        override fun stringValue() = "WeChat"
    };

    abstract fun stringValue(): String
}
