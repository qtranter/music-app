package com.audiomack.data.tracking.appsflyer

interface AppsFlyerTracker {

    fun trackEvent(name: String)

    fun trackUserId(userId: String)
}
