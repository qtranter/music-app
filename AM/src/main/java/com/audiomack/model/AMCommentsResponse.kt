package com.audiomack.model

import java.util.ArrayList
import org.json.JSONObject

class AMCommentsResponse {

    var list: ArrayList<AMComment> = ArrayList()
    var count: Int = 0

    companion object {

        @JvmStatic
        fun fromJSON(jsonObject: JSONObject): AMCommentsResponse {

            val model = AMCommentsResponse()

            model.count = jsonObject.optInt("total")

            var shouldMakeCountZero = true

            jsonObject.optJSONArray("result")?.let { commentList ->

                for (i in 0 until commentList.length()) {

                    commentList.optJSONObject(i)?.let { jsonObj ->

                        val subModel = AMComment.fromJSON(jsonObj)

                        if (subModel.deleted && subModel.children.size == 0) {
                            // Rule: show the deleted row only if the root comment having replies is deleted
                        } else {
                            model.list.add(subModel)
                        }

                        // Rule: show the count as zero if the first 10 root comments and their replies are deleted
                        if (i < 10 && shouldMakeCountZero) {

                            if (!subModel.deleted) shouldMakeCountZero = false

                            val children = subModel.children

                            for (j in 0 until children.size) {

                                val child = children[j]

                                if (!child.deleted) shouldMakeCountZero = false
                            }
                        }
                    }
                }
            }

            if (shouldMakeCountZero) model.count = 0

            return model
        }
    }
}
