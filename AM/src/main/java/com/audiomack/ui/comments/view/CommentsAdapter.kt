package com.audiomack.ui.comments.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.adapters.viewholders.EmptyViewHolder
import com.audiomack.adapters.viewholders.LoadMoreViewHolder
import com.audiomack.model.AMComment
import com.audiomack.model.AMExpandComment
import com.audiomack.model.AMShowMoreComments
import timber.log.Timber

class CommentsAdapter(private val listener: CommentsListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val typeComment = 11
    private val typeDelete = 12
    private val typeReply = 13
    private val typeLoading = 14

    private val comments: MutableList<AMComment> = mutableListOf()

    private var loadingMore = false

    interface CommentsListener {

        fun onCommentReplyTapped(comment: AMComment)
        fun onCommentUpVoteTapped(comment: AMComment)
        fun onCommentDownVoteTapped(comment: AMComment)
        fun onCommentActionTapped(comment: AMComment)
        fun onReplyUpVoteTapped(parentComment: AMComment, reply: AMComment)
        fun onReplyDownVoteTapped(parentComment: AMComment, reply: AMComment)
        fun onReplyActionTapped(comment: AMComment)
        fun onCommentViewMoreTapped(more: AMShowMoreComments)
        fun onCommenterTapped(comment: AMComment)
        fun onCommentExpandTapped(expand: AMExpandComment)
    }

    var uploaderSlug: String = ""

    fun update(newComments: List<AMComment>) {
        this.comments.clear()
        this.comments.addAll(newComments)
        this.notifyDataSetChanged()
    }

    fun remove(index: Int) {
        this.comments.removeAt(index)
        this.notifyItemRemoved(index)
    }

    fun showLoading() {
        if (!loadingMore) {
            loadingMore = true
            notifyItemInserted(comments.size)
        }
    }

    fun hideLoading() {
        if (loadingMore) {
            loadingMore = false
            notifyItemRemoved(comments.size + 1)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position == comments.size && loadingMore) {
            return typeLoading
        }
        if (comments[position].deleted) {
            return typeDelete
        }
        return if (comments[position].children.size > 0) typeReply else typeComment
    }

    override fun getItemCount(): Int {
        return comments.size + if (loadingMore) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return try {
            when (viewType) {
                typeComment -> CommentViewHolder(
                    LayoutInflater.from(
                        parent.context
                    ).inflate(R.layout.row_comments, parent, false)
                )
                typeReply -> MoreReplyCommentViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.row_reply_comment,
                        parent,
                        false
                    )
                )
                typeDelete -> DeleteCommentViewHolder(
                    LayoutInflater.from(
                        parent.context
                    ).inflate(R.layout.row_delete_comment, parent, false)
                )
                typeLoading -> LoadMoreViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_loadingmore, parent, false))
                else -> EmptyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_empty, parent, false))
            }
        } catch (e: Exception) {
            Timber.w(e)
            return EmptyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_empty, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            val comment = comments[position]
            when (holder) {
                is CommentViewHolder -> holder.setup(comment, uploaderSlug, position == comments.size - 1, listener)
                is MoreReplyCommentViewHolder -> holder.setup(comment, uploaderSlug, listener)
                is DeleteCommentViewHolder -> holder.setup(comment)
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }
}
