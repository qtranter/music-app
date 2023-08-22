package com.audiomack.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R

class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val linearLayout = itemView.findViewById<LinearLayout>(R.id.linearLayout)

    fun setup(headerView: View?) {
        linearLayout.removeAllViews()
        headerView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            linearLayout.addView(it)
        }
    }
}
