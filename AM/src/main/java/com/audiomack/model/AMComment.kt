package com.audiomack.model

import com.audiomack.utils.getStringOrNull
import java.util.Date
import kotlin.collections.ArrayList
import org.json.JSONObject

class AMComment {

    var entityKind: String? = null
    var entityId: String? = null
    var voteTotal: Int? = null
    var upVotes: Int? = null
    var downVotes: Int? = null
    var uuid: String? = null
    var threadUuid: String? = null
    var content: String? = null
    var createdAt: Date? = null
    var deleted: Boolean = false
    var upVoted: Boolean = false
    var downVoted: Boolean = false
    var userId: Int? = null
    var children: ArrayList<AMComment> = ArrayList()
    var commenter: AMCommenter? = null
    var expanded: Boolean = false

    companion object {

        const val MaxLineCount = 5

        @JvmStatic
        fun fromJSON(jsonObject: JSONObject): AMComment {
            return AMComment().apply {
                entityKind = jsonObject.optString("kind")
                entityId = jsonObject.optString("id")
                voteTotal = jsonObject.optInt("vote_total")
                upVotes = jsonObject.optInt("vote_up")
                downVotes = jsonObject.optInt("vote_down")
                uuid = jsonObject.optString("uuid")
                content = jsonObject.getStringOrNull("content")?.replace("<br>", "\n")?.trim()
                createdAt = Date(jsonObject.optLong("created_at") * 1000L)
                deleted = jsonObject.optBoolean("deleted")
                userId = jsonObject.optInt("user_id")
                threadUuid = jsonObject.optString("thread")

                jsonObject.optJSONArray("children")?.let { childrenList ->
                    (0 until childrenList.length()).mapNotNull { childrenList.optJSONObject(it) }.forEach { jsonObj ->
                        val subModel = fromJSON(jsonObj)
                        if (!subModel.content.isNullOrBlank() && !subModel.deleted && subModel.commenter?.commentBanned != true) {
                            children.add(subModel)
                        }
                    }
                }

                commenter = jsonObject.optJSONObject("artist")?.let { AMCommenter.fromJSON(it) }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AMComment
        if (uuid != other.uuid) return false
        return true
    }

    override fun hashCode(): Int {
        return uuid?.hashCode() ?: 0
    }
}
