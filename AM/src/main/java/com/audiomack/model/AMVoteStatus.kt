package com.audiomack.model

import com.audiomack.utils.getStringOrNull
import org.json.JSONObject

class AMVoteStatus {

    var isUpVote: Boolean = false
    var uuid: String = ""
    var thread: String? = null

    companion object {
        @JvmStatic
        fun fromJSON(jsonObject: JSONObject): AMVoteStatus {
            return AMVoteStatus().apply {
                isUpVote = jsonObject.optBoolean("vote_up")
                uuid = jsonObject.optString("uuid")
                thread = jsonObject.getStringOrNull("thread")
            }
        }
    }
}
