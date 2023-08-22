package com.audiomack.ui.browse.world.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.model.WorldPage

class WorldHeaderPillsAdapter(private val request: (WorldPage) -> Unit) :
        ListAdapter<WorldFilterItem, RecyclerView.ViewHolder>(FILTER_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        WorldHeaderPillViewHolder.create(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val filterItem = getItem(position)
        if (filterItem != null) {
            (holder as WorldHeaderPillViewHolder).bind(filterItem)
        }

        holder.itemView.setOnClickListener {
            request(filterItem.page)
        }
    }

    companion object {
        private val FILTER_COMPARATOR = object : DiffUtil.ItemCallback<WorldFilterItem>() {
            override fun areItemsTheSame(oldItem: WorldFilterItem, newItem: WorldFilterItem): Boolean =
                    oldItem.page.slug == newItem.page.slug

            override fun areContentsTheSame(oldItem: WorldFilterItem, newItem: WorldFilterItem): Boolean =
                    oldItem == newItem
        }
    }
}
