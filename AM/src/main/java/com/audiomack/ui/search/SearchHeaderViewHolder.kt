package com.audiomack.ui.search

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R

class SearchHeaderViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {

    private val tvText = view.findViewById<TextView>(R.id.tvText)

    fun setup(text: String) {
        tvText.text = text
    }
}
