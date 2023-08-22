package com.audiomack.playback.cast

import com.audiomack.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class MediaInfoCustomData(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("token") val token: String?,
    @SerializedName("secret") val secret: String?,
    @SerializedName("hq") val hq: Boolean = false,
    @SerializedName("key") val key: String? = null,
    @SerializedName("apiKey") val apiKey: String = BuildConfig.AM_CONSUMER_KEY,
    @SerializedName("apiSecret") val apiSecret: String = BuildConfig.AM_CONSUMER_SECRET,
    @SerializedName("apiUrl") val apiUrl: String = BuildConfig.AM_WS_URL_LIVE
) {

    @JvmOverloads
    fun toJSON(gson: Gson = Gson()) = JSONObject(gson.toJson(this))
}
