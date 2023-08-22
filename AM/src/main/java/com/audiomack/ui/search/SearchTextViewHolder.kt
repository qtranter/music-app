package com.audiomack.ui.search

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R

class SearchTextViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {

    private val tvText = view.findViewById<TextView>(R.id.tvText)
    private val buttonDelete = view.findViewById<ImageButton>(R.id.buttonDelete)

    fun setup(text: String, canBeDeleted: Boolean, tapAction: ((String) -> Unit), deleteAction: ((String) -> Unit)? = null) {
        tvText.text = text
        buttonDelete.visibility = if (canBeDeleted) View.VISIBLE else View.GONE
        deleteAction?.let { delete ->
            buttonDelete.setOnClickListener { delete(tvText.text.toString()) }
        }
        itemView.setOnClickListener { tapAction(tvText.text.toString()) }
    }
}
