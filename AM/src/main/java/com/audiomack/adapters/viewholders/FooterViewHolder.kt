package com.audiomack.adapters.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.adapters.DataRecyclerViewAdapter

class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun setup(listener: DataRecyclerViewAdapter.RecyclerViewListener) {
        itemView.setOnClickListener { listener.onClickFooter() }
    }
}
