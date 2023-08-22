package com.audiomack.model

import com.audiomack.utils.getStringOrNull
import org.json.JSONObject

class OnboardingArtist(
    val artist: AMArtist?,
    val playlistId: String?,
    val imageUrl: String?
) {

    constructor(jsonObject: JSONObject) : this(
        jsonObject.optJSONObject("artist")?.let { AMArtist.fromJSON(false, it) },
        jsonObject.getStringOrNull("playlist_id"),
        jsonObject.getStringOrNull("image_url")
    )

    fun isValid(): Boolean {
        return artist != null && playlistId != null
    }
}
