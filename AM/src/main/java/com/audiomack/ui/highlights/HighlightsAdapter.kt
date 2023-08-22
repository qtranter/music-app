package com.audiomack.ui.highlights

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.model.AMResultItem

class HighlightsAdapter(var myAccount: Boolean, var items: MutableList<AMResultItem>, val tapHandler: (AMResultItem) -> (Unit), val menuHandler: (AMResultItem, Int) -> (Unit)) : androidx.recyclerview.widget.RecyclerView.Adapter<HighlightViewHolder>() {

    fun reload(items: List<AMResultItem>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HighlightViewHolder {
        return HighlightViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_highlighted_grid, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(viewHolder: HighlightViewHolder, position: Int) {
        viewHolder.setup(myAccount, items[position]) { menuHandler(items[viewHolder.adapterPosition], viewHolder.adapterPosition) }
        viewHolder.itemView.setOnClickListener { tapHandler(items[viewHolder.adapterPosition]) }
    }
}
