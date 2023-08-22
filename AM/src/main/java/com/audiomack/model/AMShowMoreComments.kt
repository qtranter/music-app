package com.audiomack.model

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.ui.comments.view.CommentsAdapter

class AMShowMoreComments(
    val comment: AMComment,
    val uploaderSlug: String?,
    val textView: TextView,
    val recyclerView: RecyclerView,
    val listener: CommentsAdapter.CommentsListener
)
