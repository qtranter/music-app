package com.audiomack.data.comment

import com.audiomack.model.AMComment
import com.audiomack.model.AMCommentVote
import com.audiomack.model.AMCommentsResponse
import com.audiomack.model.AMVoteStatus
import io.reactivex.Single

interface CommentDataSource {

    fun getSingleComments(kind: String, id: String, uuid: String, threadId: String?): Single<AMCommentsResponse>

    fun getComments(kind: String, id: String, limit: String, offset: String, sort: String): Single<AMCommentsResponse>

    fun postComment(content: String, kind: String, id: String, thread: String?): Single<AMComment>

    fun reportComment(kind: String, id: String, uuid: String, thread: String?): Single<Boolean>

    fun deleteComment(kind: String, id: String, uuid: String, thread: String?): Single<Boolean>

    fun getVoteStatus(kind: String, id: String): Single<ArrayList<AMVoteStatus>>

    fun voteComment(comment: AMComment, isUpVote: Boolean, kind: String, id: String): Single<AMCommentVote>
}
