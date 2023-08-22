package com.audiomack.ui.comments.view

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.model.AMComment
import java.util.Locale
import org.ocpsoft.prettytime.PrettyTime

class DeleteCommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val tvMinAgo: TextView = itemView.findViewById(R.id.tvMinAgo)

    fun setup(
        comment: AMComment
    ) {

        val timeAgoString =
                if ((comment.createdAt?.time ?: 0) > 0) PrettyTime(
                        Locale.US
                ).format(comment.createdAt) else ""
        tvMinAgo.text = timeAgoString
    }
}
