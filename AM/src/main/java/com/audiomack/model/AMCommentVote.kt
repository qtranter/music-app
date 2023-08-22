package com.audiomack.model

import org.json.JSONObject

class AMCommentVote {

    var voteTotal: Int = 0
    var upVotes: Int = 0
    var downVotes: Int = 0

    companion object {

        @JvmStatic
        fun fromJSON(jsonObject: JSONObject): AMCommentVote {
            return AMCommentVote().apply {
                voteTotal = jsonObject.optInt("vote_total")
                upVotes = jsonObject.optInt("vote_up")
                downVotes = jsonObject.optInt("vote_down")
            }
        }
    }
}
