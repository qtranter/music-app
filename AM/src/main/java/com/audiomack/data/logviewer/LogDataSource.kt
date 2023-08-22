package com.audiomack.data.logviewer

interface LogDataSource {

    fun provideData(type: LogType): List<String>

    fun addLog(type: LogType, log: String)
}

enum class LogType(val tag: String) {
    ADS("Ads"),
    MIXPANEL("MixpanelTrackerImpl"),
    PLAYBACK("PlayerPlayback"),
    QUEUE("QueueRepository"),
    PLAYER_VM("PlayerViewModel"),
    PLAYER_REPO("PlayerRepository")
}
