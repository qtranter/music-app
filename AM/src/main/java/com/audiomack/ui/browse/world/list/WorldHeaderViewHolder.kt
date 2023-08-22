package com.audiomack.ui.browse.world.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R

class WorldHeaderViewHolder(
    view: View,
    onlinePillsAdapter: WorldHeaderPillsAdapter
) : RecyclerView.ViewHolder(view) {

    private val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewPostHeader)

    init {
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = onlinePillsAdapter
    }

    companion object {
        fun create(parent: ViewGroup, onlinePillsAdapter: WorldHeaderPillsAdapter): WorldHeaderViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.row_world_post_header, parent, false)
            return WorldHeaderViewHolder(view, onlinePillsAdapter)
        }
    }
}
