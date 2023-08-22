package com.audiomack.ui.comments.view

import android.text.Layout
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMComment
import com.audiomack.model.AMExpandComment
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.squareup.picasso.Picasso
import java.util.Locale
import org.ocpsoft.prettytime.PrettyTime

class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val imgProfile: ImageView = itemView.findViewById(R.id.imgProfile)
    private val imageViewVerified: ImageView = itemView.findViewById(R.id.imageViewVerified)
    private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
    private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
    private val tvMinAgo: TextView = itemView.findViewById(R.id.tvMinAgo)
    private val tvUpVote: TextView = itemView.findViewById(R.id.tvUpVote)
    private val buttonUpVote: ImageButton = itemView.findViewById(R.id.buttonUpVote)
    private val buttonDownVote: ImageButton = itemView.findViewById(R.id.buttonDownVote)
    private val tvReply: TextView = itemView.findViewById(R.id.tvReply)
    private val buttonActions: ImageButton = itemView.findViewById(R.id.buttonActions)
    private val tvExpand: TextView = itemView.findViewById(R.id.tvExpand)
    private val divider: View = itemView.findViewById(R.id.divider)

    fun setup(
        comment: AMComment,
        uploaderSlug: String,
        isLast: Boolean,
        listener: CommentsAdapter.CommentsListener
    ) {

        tvMessage.text = comment.content
        tvUserName.text = comment.commenter?.name
        tvUpVote.text = String.format("%d", comment.voteTotal)

        tvMinAgo.text = if ((comment.createdAt?.time ?: 0) > 0) PrettyTime(Locale.US).format(comment.createdAt) else ""

        imgProfile.setOnClickListener { listener.onCommenterTapped(comment) }
        tvUserName.setOnClickListener { listener.onCommenterTapped(comment) }
        buttonUpVote.setOnClickListener { listener.onCommentUpVoteTapped(comment) }
        buttonDownVote.setOnClickListener { listener.onCommentDownVoteTapped(comment) }
        tvReply.setOnClickListener { listener.onCommentReplyTapped(comment) }
        buttonActions.setOnClickListener { listener.onCommentActionTapped(comment) }
        tvExpand.setOnClickListener { listener.onCommentExpandTapped(AMExpandComment(tvMessage, tvExpand, comment)) }

        var upIcon = R.drawable.up_vote_icon
        var downIcon = R.drawable.down_vote_icon

        if (comment.upVoted) upIcon = R.drawable.up_vote_selected_icon
        else if (comment.downVoted) downIcon = R.drawable.down_vote_selected_icon

        buttonUpVote.setImageDrawable(buttonUpVote.context.drawableCompat(upIcon))
        buttonDownVote.setImageDrawable(buttonDownVote.context.drawableCompat(downIcon))

        comment.commenter?.image?.let {
            PicassoImageLoader.load(imgProfile.context, it, imgProfile)
        } ?: run {
            Picasso.get().load(R.drawable.comment_placeholder_icon).into(imgProfile)
        }

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

        divider.visibility = if (isLast) View.GONE else View.VISIBLE
    }
}
