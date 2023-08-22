package com.audiomack.model

enum class InAppPurchaseMode {

    BannerAdDismissal {
        override fun stringValue() = "BannerAdDismissal"
    },
    NowPlayingAdDismissal {
        override fun stringValue() = "NowPlayingAdDismissal"
    },
    PlaylistDownload {
        override fun stringValue() = "PlaylistDownload"
    },
    PlaylistBrowseDownload {
        override fun stringValue() = "PlaylistBrowseDownload"
    },
    HiFi {
        override fun stringValue() = "HiFi"
    },
    Settings {
        override fun stringValue() = "Settings"
    },
    Deeplink {
        override fun stringValue() = "Deeplink"
    },
    MyLibraryBar {
        override fun stringValue() = "MyLibraryBar"
    },
    AudioAd {
        override fun stringValue() = "AudioAd"
    },
    Equalizer {
        override fun stringValue() = "Equalizer"
    },
    SleepTimer {
        override fun stringValue() = "SleepTimer"
    },
    SleepTimerPrompt {
        override fun stringValue(): String = "SleepTimerPrompt"
    },
    PremiumDownload {
        override fun stringValue() = "PremiumDownload"
    };

    abstract fun stringValue(): String
}
