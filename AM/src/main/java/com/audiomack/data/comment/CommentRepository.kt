package com.audiomack.data.comment

import com.audiomack.model.AMComment
import com.audiomack.model.AMCommentVote
import com.audiomack.model.AMCommentsResponse
import com.audiomack.model.AMVoteStatus
import com.audiomack.network.API
import com.audiomack.network.APIInterface
import io.reactivex.Single

class CommentRepository(
    private val api: APIInterface.CommentsInterface = API.getInstance()
) : CommentDataSource {

    override fun getSingleComments(kind: String, id: String, uuid: String, threadId: String?): Single<AMCommentsResponse> {
        return api.getSingleComments(kind, id, uuid, threadId)
    }

    override fun getComments(kind: String, id: String, limit: String, offset: String, sort: String): Single<AMCommentsResponse> {
        return api.getComments(kind, id, limit, offset, sort)
    }

    override fun postComment(content: String, kind: String, id: String, thread: String?): Single<AMComment> {
        return api.postComment(content, kind, id, thread)
    }

    override fun reportComment(kind: String, id: String, uuid: String, thread: String?): Single<Boolean> {
        return api.reportComment(kind, id, uuid, thread)
    }

    override fun deleteComment(kind: String, id: String, uuid: String, thread: String?): Single<Boolean> {
        return api.deleteComment(kind, id, uuid, thread)
    }

    override fun getVoteStatus(kind: String, id: String): Single<ArrayList<AMVoteStatus>> {
        return api.getVoteStatus(kind, id)
    }

    override fun voteComment(comment: AMComment, isUpVote: Boolean, kind: String, id: String): Single<AMCommentVote> {
        val upvote = if ((comment.upVoted && isUpVote) || (comment.downVoted && !isUpVote)) null else isUpVote
        return api.voteComment(comment, upvote, kind, id)
    }
}
