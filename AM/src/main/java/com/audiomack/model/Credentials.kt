package com.audiomack.model

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.audiomack.PREFERENCES
import com.audiomack.PREFERENCES_CREDENTIALS
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.support.ZendeskRepository
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.user.UserData
import com.audiomack.data.user.UserRepository
import com.audiomack.network.API
import com.audiomack.network.AnalyticsHelper
import com.audiomack.utils.GeneralPreferencesHelper
import com.audiomack.utils.SecureSharedPreferences
import com.audiomack.utils.getLongOrNull
import com.audiomack.utils.getStringOrNull
import com.audiomack.utils.putIfNotNull
import java.util.UUID
import org.json.JSONObject
import timber.log.Timber

class Credentials {

    var email: String? = null
    var password: String? = null

    var token: String? = null
        private set
    var tokenSecret: String? = null
        private set
    private var tokenExpiration: Long? = null

    var userId: String? = null
        private set
    var userScreenName: String? = null
    var userUrlSlug: String? = null
        private set

    var facebookId: String? = null

    var twitterToken: String? = null
    var twitterSecret: String? = null

    var googleToken: String? = null

    var appleIdToken: String? = null

    var socialEmail: String? = null

    var deviceId: String? = null
        private set

    // Contains the name of the social network in case we have successfully signed up. This is empty or null in case of a login or signup via email/password.
    private var registeredViaSocial: String? = null

    val isLoggedViaFacebook: Boolean
        get() = facebookId != null

    val isLoggedViaGoogle: Boolean
        get() = googleToken != null

    val isLoggedViaTwitter: Boolean
        get() = twitterToken != null

    val isLoggedViaApple: Boolean
        get() = appleIdToken != null

    val isRegisteredViaSocial: Boolean
        get() = (registeredViaSocial ?: "").endsWith("signUp")

    companion object {

        // Used for storing data in memory: no need to read from filesystem everytime
        private var credentials: Credentials? = null

        @JvmStatic
        fun isLogged(context: Context?): Boolean {
            return load(context) != null
        }

        @JvmStatic
        fun logout(context: Context, resetEnvironment: Boolean, reason: LogoutReason) {
            UserData.clear()
            AMArtist.logout()
            save(null, context)
            GeneralPreferencesHelper.getInstance(context).setExcludeReups(context, false)
            GeneralPreferencesHelper.getInstance(context).setTrackAds(context, false)
            PreferencesRepository().needToShowHighlightsPlaceholder = true
            UserRepository.getInstance().onLoggedOut()
            TrackingRepository().trackLogout()
            ZendeskRepository().logout()

            if (resetEnvironment) {
                GeneralPreferencesHelper.getInstance(context).setLiveEnvironmentStatus(context, true)
                API.getInstance().updateEnvironment()
            }

            AnalyticsHelper.getInstance().trackException(Exception("Logout with reason: ${reason.name}"))
        }

        @JvmStatic
        fun load(context: Context?): Credentials? {
            if (credentials == null && context != null) {
                val prefs = SecureSharedPreferences(context, PREFERENCES)
                val credentialsString = prefs.getString(PREFERENCES_CREDENTIALS)
                credentials = deserialize(credentialsString)
            }
            return credentials
        }

        @JvmStatic
        fun save(credentials: Credentials?, context: Context) {
            if (credentials == null) {
                AnalyticsHelper.getInstance().trackIdentity()
            }
            val prefs = SecureSharedPreferences(context, PREFERENCES)
            prefs.put(PREFERENCES_CREDENTIALS, serialize(credentials))
            Credentials.credentials = credentials
        }

        private fun serialize(c: Credentials?): String? {
            c?.let {
                try {
                    return JSONObject().apply {
                        putIfNotNull("email", it.email)
                        putIfNotNull("password", it.password)
                        putIfNotNull("token", it.token)
                        putIfNotNull("tokenSecret", it.tokenSecret)
                        putIfNotNull("tokenExpiration", it.tokenExpiration)
                        putIfNotNull("userId", it.userId)
                        putIfNotNull("userScreenName", it.userScreenName)
                        putIfNotNull("userUrlSlug", it.userUrlSlug)
                        putIfNotNull("facebookId", it.facebookId)
                        putIfNotNull("twitterToken", it.twitterToken)
                        putIfNotNull("twitterSecret", it.twitterSecret)
                        putIfNotNull("googleToken", it.googleToken)
                        putIfNotNull("socialEmail", it.socialEmail)
                        putIfNotNull("deviceId", it.deviceId)
                        putIfNotNull("registeredViaSocial", it.registeredViaSocial)
                        putIfNotNull("appleIdToken", it.appleIdToken)
                    }.toString()
                } catch (e: Exception) {
                    Timber.w(e)
                }
            }
            return null
        }

        private fun deserialize(string: String?): Credentials? {
            try {
                if (!string.isNullOrEmpty()) {
                    val json = JSONObject(string)
                    return Credentials().apply {
                        email = json.getStringOrNull("email")
                        password = json.getStringOrNull("password")
                        token = json.getStringOrNull("token")
                        tokenSecret = json.getStringOrNull("tokenSecret")
                        tokenExpiration = json.getLongOrNull("tokenExpiration")
                        userId = json.getStringOrNull("userId")
                        userScreenName = json.getStringOrNull("userScreenName")
                        userUrlSlug = json.getStringOrNull("userUrlSlug")
                        facebookId = json.getStringOrNull("facebookId")
                        twitterToken = json.getStringOrNull("twitterToken")
                        twitterSecret = json.getStringOrNull("twitterSecret")
                        googleToken = json.getStringOrNull("googleToken")
                        socialEmail = json.getStringOrNull("socialEmail")
                        deviceId = json.getStringOrNull("deviceId")
                        registeredViaSocial = json.getStringOrNull("registeredViaSocial")
                        appleIdToken = json.getStringOrNull("appleIdToken")
                    }
                }
            } catch (e: Exception) {
                Timber.w(e)
            }
            return null
        }

        @JvmStatic
        fun saveFromJson(context: Context, jsonObj: JSONObject) {
            val credentials = load(context) ?: Credentials()
            credentials.token = jsonObj.optString("oauth_token")
            credentials.tokenSecret = jsonObj.optString("oauth_token_secret")
            credentials.tokenExpiration = jsonObj.optLong("oauth_token_expiration")
            credentials.userId = jsonObj.optJSONObject("user").optString("id")
            credentials.userScreenName = jsonObj.optJSONObject("user").optString("screen_name")
            credentials.userUrlSlug = jsonObj.optJSONObject("user").optString("url_slug")
            credentials.deviceId = generateDeviceId(context)
            credentials.registeredViaSocial = jsonObj.optString("social_registration")

            save(credentials, context)

            AnalyticsHelper.getInstance().trackIdentity()
        }

        @JvmStatic
        fun generateDeviceId(context: Context?): String {
            // Secure ID
            if (context != null) {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)?.let {
                    return it
                }
            }

            // Serial ID
            try {
                val serial = android.os.Build::class.java.getField("SERIAL").get(null).toString()
                val deviceId =
                    "35" + Build.BOARD.length % 10 + Build.BRAND.length % 10 + Build.CPU_ABI.length % 10 + Build.DEVICE.length % 10 + Build.MANUFACTURER.length % 10 + Build.MODEL.length % 10 + Build.PRODUCT.length % 10
                return UUID(
                    deviceId.hashCode().toLong(),
                    serial.hashCode().toLong()
                ).toString()
            } catch (e: Exception) {
                Timber.w(e)
            }

            // Random ID
            return UUID.randomUUID().toString()
        }

        fun itsMe(context: Context?, uploaderId: String): Boolean {
            try {
                val credentials = load(context)
                if (credentials != null) {
                    return credentials.userId == uploaderId
                }
            } catch (e: Exception) {
                Timber.w(e)
            }
            return false
        }
    }
}
