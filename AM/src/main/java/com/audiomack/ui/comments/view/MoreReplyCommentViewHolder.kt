package com.audiomack.ui.comments.view

import android.text.Layout
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMComment
import com.audiomack.model.AMExpandComment
import com.audiomack.model.AMShowMoreComments
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import java.util.Locale
import org.ocpsoft.prettytime.PrettyTime

class MoreReplyCommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val layoutDeletedComment: ViewGroup = itemView.findViewById(R.id.layoutDeletedComment)
    private val tvDeletedCommentMinAgo: TextView = itemView.findViewById(R.id.tvDeletedCommentMinAgo)

    private val layoutValidComment: ViewGroup = itemView.findViewById(R.id.layoutValidComment)
    private val imgProfile: ImageView = itemView.findViewById(R.id.imgProfile)
    private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
    private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
    private val imageViewVerified: ImageView = itemView.findViewById(R.id.imageViewVerified)
    private val tvMinAgo: TextView = itemView.findViewById(R.id.tvMinAgo)
    private val tvUpVote: TextView = itemView.findViewById(R.id.tvUpVote)
    private val buttonUpVote: ImageButton = itemView.findViewById(R.id.buttonUpVote)
    private val buttonDownVote: ImageButton = itemView.findViewById(R.id.buttonDownVote)
    private val tvReplyCount: TextView = itemView.findViewById(R.id.tvReplyCount)
    private val tvReply: TextView = itemView.findViewById(R.id.tvReply)
    private val buttonActions: ImageButton = itemView.findViewById(R.id.buttonActions)

    private val replyImgProfile: ImageView = itemView.findViewById(R.id.replyImgProfile)
    private val replyTvMessage: TextView = itemView.findViewById(R.id.replyTvMessage)
    private val replyTvUserName: TextView = itemView.findViewById(R.id.replyTvUserName)
    private val replyImageViewVerified: ImageView = itemView.findViewById(R.id.replyImageViewVerified)
    private val replyTvMinAgo: TextView = itemView.findViewById(R.id.replyTvMinAgo)
    private val replyTvUpVote: TextView = itemView.findViewById(R.id.replyTvUpVote)
    private val replyButtonUpVote: ImageButton = itemView.findViewById(R.id.replyButtonUpVote)
    private val replyButtonDownVote: ImageButton = itemView.findViewById(R.id.replyButtonDownVote)
    private val replyButtonActions: ImageButton = itemView.findViewById(R.id.replyButtonActions)
    private val tvMoreReply: TextView = itemView.findViewById(R.id.tvMoreReply)
    private val recyclerView: RecyclerView = itemView.findViewById(R.id.recyclerView)
    private val tvExpand: TextView = itemView.findViewById(R.id.tvExpand)
    private val replyTvExpand: TextView = itemView.findViewById(R.id.replyTvExpand)

    fun setup(
        comment: AMComment,
        uploaderSlug: String,
        listener: CommentsAdapter.CommentsListener
    ) {

        val commentDeleted = comment.deleted || comment.commenter?.commentBanned == true
        layoutDeletedComment.visibility = if (commentDeleted) View.VISIBLE else View.GONE
        layoutValidComment.visibility = if (commentDeleted) View.GONE else View.VISIBLE

        val timeAgoString = if ((comment.createdAt?.time ?: 0) > 0) PrettyTime(Locale.US).format(comment.createdAt) else ""
        tvMinAgo.text = timeAgoString
        tvDeletedCommentMinAgo.text = timeAgoString

        tvMessage.text = comment.content

        tvMessage.post {
            val layout: Layout? = tvMessage.layout
            val lineCount = layout?.lineCount ?: 0
            val ellipsisCount = layout?.getEllipsisCount(lineCount - 1) ?: 0
            if ((lineCount > AMComment.MaxLineCount) || (lineCount == AMComment.MaxLineCount && ellipsisCount > 0)) {
                tvExpand.visibility = View.VISIBLE
                if (comment.expanded) {
                    tvMessage.maxLines = Int.MAX_VALUE
                    tvExpand.text = tvExpand.context.getString(R.string.comments_minimize)
                } else {
                    tvMessage.maxLines = AMComment.MaxLineCount
                    tvExpand.text = tvExpand.context.getString(R.string.comments_expand)
                }
            } else {
                tvExpand.visibility = View.GONE
            }
        }

        tvUserName.text = comment.commenter?.name
        tvUpVote.text = String.format("%d", comment.voteTotal)
        tvReplyCount.text = String.format("%d", comment.children.size)

        imgProfile.setOnClickListener { listener.onCommenterTapped(comment) }
        tvUserName.setOnClickListener { listener.onCommenterTapped(comment) }
        buttonUpVote.setOnClickListener { listener.onCommentUpVoteTapped(comment) }
        buttonDownVote.setOnClickListener { listener.onCommentDownVoteTapped(comment) }
        buttonActions.setOnClickListener { listener.onCommentActionTapped(comment) }
        tvMoreReply.setOnClickListener { listener.onCommentViewMoreTapped(AMShowMoreComments(comment, uploaderSlug, tvMoreReply, recyclerView, listener)) }
        tvReply.setOnClickListener { listener.onCommentReplyTapped(comment) }
        tvExpand.setOnClickListener { listener.onCommentExpandTapped(AMExpandComment(tvMessage, tvExpand, comment)) }

        var upIcon = R.drawable.up_vote_icon
        var downIcon = R.drawable.down_vote_icon

        if (comment.upVoted) upIcon = R.drawable.up_vote_selected_icon
        else if (comment.downVoted) downIcon = R.drawable.down_vote_selected_icon

        buttonUpVote.setImageDrawable(buttonUpVote.context.drawableCompat(upIcon))
        buttonDownVote.setImageDrawable(buttonDownVote.context.drawableCompat(downIcon))

        PicassoImageLoader.load(imgProfile.context, comment.commenter?.image, imgProfile)

        comment.commenter?.let { artist ->
            when {
                artist.verified -> {
                    imageViewVerified.setImageResource(R.drawable.ic_verified)
                    imageViewVerified.visibility = View.VISIBLE
                }
                artist.tastemaker -> {
                    imageViewVerified.setImageResource(R.drawable.ic_tastemaker)
                    imageViewVerified.visibility = View.VISIBLE
                }
                artist.authenticated -> {
                    imageViewVerified.setImageResource(R.drawable.ic_authenticated)
                    imageViewVerified.visibility = View.VISIBLE
                }
                else -> imageViewVerified.visibility = View.GONE
            }
        } ?: run {
            imageViewVerified.visibility = View.GONE
        }

        val postedByUploader = comment.commenter?.urlSlug == uploaderSlug && uploaderSlug.isNotBlank()
        tvUserName.setTextColor(tvUserName.context.colorCompat(if (postedByUploader) R.color.black else R.color.orange))
        tvUserName.background = if (postedByUploader) tvUserName.context.drawableCompat(R.drawable.comment_uploader_background) else null

        tvMoreReply.visibility = View.GONE

        val reply = comment.children.first()

        val replyTimeAgoString =
                if ((reply.createdAt?.time ?: 0) > 0) PrettyTime(
                        Locale.US
                ).format(reply.createdAt) else ""
        replyTvMinAgo.text = replyTimeAgoString

        replyTvMessage.text = reply.content

        replyTvMessage.post {
            val layout: Layout? = replyTvMessage.layout
            val lineCount = layout?.lineCount ?: 0
            val ellipsisCount = layout?.getEllipsisCount(lineCount - 1) ?: 0
            if ((lineCount > AMComment.MaxLineCount) || (lineCount == AMComment.MaxLineCount && ellipsisCount > 0)) {
                replyTvExpand.visibility = View.VISIBLE
                if (reply.expanded) {
                    replyTvMessage.maxLines = Int.MAX_VALUE
                    replyTvExpand.text = tvExpand.context.getString(R.string.comments_minimize)
                } else {
                    replyTvMessage.maxLines = AMComment.MaxLineCount
                    replyTvExpand.text = tvExpand.context.getString(R.string.comments_expand)
                }
            } else {
                replyTvExpand.visibility = View.GONE
            }
        }

        replyTvUserName.text = reply.commenter?.name
        replyTvUpVote.text = String.format("%d", reply.upVotes)

        replyImgProfile.setOnClickListener { listener.onCommenterTapped(reply) }
        replyTvUserName.setOnClickListener { listener.onCommenterTapped(reply) }
        replyButtonUpVote.setOnClickListener { listener.onReplyUpVoteTapped(comment, reply) }
        replyButtonDownVote.setOnClickListener { listener.onReplyDownVoteTapped(comment, reply) }
        replyButtonActions.setOnClickListener { listener.onReplyActionTapped(reply) }
        replyTvExpand.setOnClickListener { listener.onCommentExpandTapped(AMExpandComment(replyTvMessage, replyTvExpand, reply)) }

        var upIconReply = R.drawable.up_vote_icon
        var downIconReply = R.drawable.down_vote_icon

        if (reply.upVoted) upIconReply = R.drawable.up_vote_selected_icon
        else if (reply.downVoted) downIconReply = R.drawable.down_vote_selected_icon

        replyButtonUpVote.setImageDrawable(replyButtonUpVote.context.drawableCompat(upIconReply))
        replyButtonDownVote.setImageDrawable(replyButtonDownVote.context.drawableCompat(downIconReply))

        PicassoImageLoader.load(replyImgProfile.context, reply.commenter?.image, replyImgProfile)

        reply.commenter?.let { artist ->
            when {
                artist.verified -> {
                    replyImageViewVerified.setImageResource(R.drawable.ic_verified)
                    replyImageViewVerified.visibility = View.VISIBLE
                }
                artist.tastemaker -> {
                    replyImageViewVerified.setImageResource(R.drawable.ic_tastemaker)
                    replyImageViewVerified.visibility = View.VISIBLE
                }
                artist.authenticated -> {
                    replyImageViewVerified.setImageResource(R.drawable.ic_authenticated)
                    replyImageViewVerified.visibility = View.VISIBLE
                }
                else -> replyImageViewVerified.visibility = View.GONE
            }
        } ?: run {
            replyImageViewVerified.visibility = View.GONE
        }

        val replyPostedByUploader = reply.commenter?.urlSlug == uploaderSlug && uploaderSlug.isNotBlank()
        replyTvUserName.setTextColor(replyTvUserName.context.colorCompat(if (replyPostedByUploader) R.color.black else R.color.orange))
        replyTvUserName.background = if (replyPostedByUploader) replyTvUserName.context.drawableCompat(R.drawable.comment_uploader_background) else null

        when (comment.children.size) {
            1 -> {
                tvMoreReply.visibility = View.GONE
                tvMoreReply.text = String.format(tvMoreReply.context.getString(R.string.comments_reply))
            }
            2 -> {
                tvMoreReply.visibility = View.VISIBLE
                tvMoreReply.text = String.format(tvMoreReply.context.getString(R.string.comments_view_more_reply))
            }
            else -> {
                tvMoreReply.visibility = View.VISIBLE
                tvMoreReply.text = String.format(tvMoreReply.context.getString(R.string.comments_view_more_replies), comment.children.size - 1)
            }
        }
        if (recyclerView.visibility == View.VISIBLE) {
            tvMoreReply.visibility = View.GONE
            val commentsAdapter = ReplyCommentsAdapter(
                comment,
                uploaderSlug,
                listener
            )
            recyclerView.visibility = View.VISIBLE
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = commentsAdapter
            commentsAdapter.update(comment.children.subList(1, comment.children.size))
        }
    }
}
