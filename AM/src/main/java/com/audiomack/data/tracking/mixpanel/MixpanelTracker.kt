package com.audiomack.data.tracking.mixpanel

interface MixpanelTracker {

    fun isAppInForeground(): Boolean

    fun flush()

    fun reset()

    fun trackEvent(eventName: String, properties: Map<String, Any>)

    fun trackSuperProperties(superProperties: Map<String, Any>)

    fun trackUserProperties(userProperties: Map<String, Any>)

    fun identifyUser(userId: String)

    fun incrementUserProperty(property: String, value: Double)

    fun setUserPropertyOnce(name: String, value: Any)

    fun setPushToken(token: String)
}
