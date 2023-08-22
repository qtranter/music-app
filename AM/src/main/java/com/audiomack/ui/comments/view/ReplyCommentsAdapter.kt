package com.audiomack.ui.comments.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.model.AMComment
import timber.log.Timber

class ReplyCommentsAdapter(private val parentComment: AMComment, private val uploaderSlug: String, private val listener: CommentsAdapter.CommentsListener) : RecyclerView.Adapter<ReplyCommentViewHolder>() {

    private val replies: MutableList<AMComment> = mutableListOf()

    override fun getItemCount(): Int {
        return replies.size
    }

    fun update(newComments: List<AMComment>) {
        this.replies.clear()
        this.replies.addAll(newComments)
        this.notifyDataSetChanged()
    }

    fun remove(index: Int) {
        this.replies.removeAt(index)
        this.notifyItemRemoved(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyCommentViewHolder {
        return ReplyCommentViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.row_comments,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ReplyCommentViewHolder, position: Int) {
        try {
            replies.getOrNull(position)?.let { comment ->
                holder.setup(parentComment, comment, uploaderSlug, position != itemCount - 1, listener)
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }
}
