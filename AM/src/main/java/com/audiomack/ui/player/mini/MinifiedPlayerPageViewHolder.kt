package com.audiomack.ui.player.mini

import android.view.View
import android.widget.TextView
import com.audiomack.R

class MinifiedPlayerPageViewHolder(view: View) {

    val tvTitle: TextView = view.findViewById(R.id.tvTitle)
    val tvArtist: TextView = view.findViewById(R.id.tvArtist)
    val tvPlaceholder: TextView = view.findViewById(R.id.tvPlaceholder)
}
