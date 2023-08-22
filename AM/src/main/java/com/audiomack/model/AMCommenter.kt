package com.audiomack.model

import com.audiomack.data.sizes.SizesRepository
import com.audiomack.utils.getStringOrNull
import org.json.JSONObject

class AMCommenter {

    var commentBanned: Boolean = false
    var image: String? = null
    var name: String? = null
    var urlSlug: String? = null
    var verified: Boolean = false
    var tastemaker: Boolean = false
    var authenticated: Boolean = false

    companion object {
        fun fromJSON(jsonObject: JSONObject): AMCommenter {
            return AMCommenter().apply {
                commentBanned = jsonObject.optBoolean("comment_banned")
                image = jsonObject.getStringOrNull("image")?.plus("?width=${SizesRepository.tinyArtist}")
                name = jsonObject.getStringOrNull("name")
                urlSlug = jsonObject.getStringOrNull("url_slug")
                verified = jsonObject.optString("verified") == "yes"
                tastemaker = jsonObject.optString("verified") == "tastemaker"
                authenticated = listOf("authenticated", "verify-pending", "verify-declined")
                        .contains(jsonObject.optString("verified"))
            }
        }
    }
}
