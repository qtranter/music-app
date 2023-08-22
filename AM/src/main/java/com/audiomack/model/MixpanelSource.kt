package com.audiomack.model

import android.os.Parcelable
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryOffline
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibrarySearchOffline
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

@Parcelize
data class MixpanelSource @JvmOverloads constructor(
    val tab: String,
    val page: String,
    val extraParams: List<Pair<String, String>>? = null,
    var shuffled: Boolean = false
) : Parcelable {

    fun toJSON(): String {
        return JSONObject().apply {
            put("tab", tab)
            put("page", page)

            val extraParamsArray = JSONArray()
            extraParams?.forEach { extraParam ->
                extraParamsArray.put(JSONArray().apply {
                    put(extraParam.first)
                    put(extraParam.second)
                })
            }
            put("extraParams", extraParamsArray)
        }.toString()
    }

    val isInMyDownloads: Boolean
        get() = page == MixpanelPageMyLibraryOffline || page == MixpanelPageMyLibrarySearchOffline

    companion object {
        @JvmStatic
        fun fromJSON(jsonString: String?): MixpanelSource? {
            if (!jsonString.isNullOrBlank()) {
                try {
                    val json = JSONObject(jsonString)
                    val tab = json.optString("tab")
                    if (tab.isNullOrBlank()) {
                        return null
                    }
                    val page = json.optString("page")
                    var extraParams: List<Pair<String, String>>? = null
                    json.optJSONArray("extraParams")?.let { extraParamsArray ->
                        extraParams = (0 until extraParamsArray.length()).map {
                            val array = extraParamsArray.optJSONArray(it)
                            Pair(array.optString(0), array.optString(1))
                        }
                    }
                    return MixpanelSource(
                        tab,
                        page,
                        extraParams
                    )
                } catch (e: Exception) {
                    Timber.w(e)
                }
            }
            return null
        }

        val empty = MixpanelSource("", "", listOf())
    }
}
