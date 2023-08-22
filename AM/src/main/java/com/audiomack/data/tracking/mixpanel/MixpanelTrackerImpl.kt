package com.audiomack.data.tracking.mixpanel

import android.annotation.SuppressLint
import com.audiomack.BuildConfig
import com.audiomack.MainApplication
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.logviewer.LogType
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject
import timber.log.Timber

object MixpanelTrackerImpl : MixpanelTracker {

    @SuppressLint("StaticFieldLeak")
    private val mixPanel: MixpanelAPI? = try {
        MixpanelAPI.getInstance(MainApplication.context, BuildConfig.AM_MIXPANEL_TOKEN)
    } catch (e: Exception) {
        null
    }

    private val TAG = LogType.MIXPANEL.tag
    private val SDK_ENABLED = !BuildConfig.AUDIOMACK_DEBUG && !DeviceRepository.runningEspressoTest

    override fun isAppInForeground(): Boolean {
        return mixPanel?.isAppInForeground ?: false
    }

    override fun flush() {
        Timber.tag(TAG).d("Flush")
        if (SDK_ENABLED) {
            mixPanel?.flush()
        }
    }

    override fun reset() {
        Timber.tag(TAG).d("Reset")
        if (SDK_ENABLED) {
            mixPanel?.reset()
        }
    }

    override fun trackEvent(eventName: String, properties: Map<String, Any>) {
        Timber.tag(TAG).d("Event: $eventName - Properties: $properties")
        if (SDK_ENABLED) {
            mixPanel?.track(eventName, JSONObject(properties))
        }
    }

    override fun trackSuperProperties(superProperties: Map<String, Any>) {
        Timber.tag(TAG).d("Super properties: $superProperties")
        if (SDK_ENABLED) {
            mixPanel?.registerSuperProperties(JSONObject(superProperties))
        }
    }

    override fun trackUserProperties(userProperties: Map<String, Any>) {
        Timber.tag(TAG).d("User properties: $userProperties")
        if (SDK_ENABLED) {
            mixPanel?.people?.set(JSONObject(userProperties))
        }
    }

    override fun identifyUser(userId: String) {
        Timber.tag(TAG).d("Identify: $userId")
        if (SDK_ENABLED) {
            mixPanel?.apply {
                identify(userId)
                people.identify(userId)
            }
        }
    }

    override fun incrementUserProperty(property: String, value: Double) {
        Timber.tag(TAG).d("Increment property: $property - Value: $value")
        if (SDK_ENABLED) {
            mixPanel?.people?.increment(property, value)
        }
    }

    override fun setUserPropertyOnce(name: String, value: Any) {
        Timber.tag(TAG).d("Set once: $name - Value: $value")
        if (SDK_ENABLED) {
            mixPanel?.people?.setOnce(name, value)
        }
    }

    override fun setPushToken(token: String) {
        Timber.tag(TAG).d("Set push token $token")
        if (SDK_ENABLED) {
            mixPanel?.people?.pushRegistrationId = token
        }
    }
}
