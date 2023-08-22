package com.audiomack.ui.search

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString

class SearchSuggestionsViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val tvText = view.findViewById<TextView>(R.id.tvText)

    fun setup(text: String, highlight: String) {
        tvText.text = tvText.context.spannableString(
            fullString = text,
            highlightedStrings = listOf(highlight),
            highlightedColor = tvText.context.colorCompat(R.color.white),
            highlightedFont = R.font.opensans_bold
        )
    }
}
