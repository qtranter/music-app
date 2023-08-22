package com.audiomack.adapters.viewholders

import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R

class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val v: View? = itemView.findViewById(R.id.v)

    fun setup(bottomSectionHeight: Int) {
        (v?.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.height = bottomSectionHeight
            v.layoutParams = it
        }
    }
}
