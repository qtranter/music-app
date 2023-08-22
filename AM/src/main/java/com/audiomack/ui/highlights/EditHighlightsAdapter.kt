package com.audiomack.ui.highlights

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.model.AMResultItem
import com.audiomack.utils.ItemTouchHelperAdapter
import java.util.Collections

class EditHighlightsAdapter(var items: MutableList<AMResultItem>) : RecyclerView.Adapter<EditHighlightsViewHolder>(), ItemTouchHelperAdapter {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditHighlightsViewHolder {
        return EditHighlightsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_edit_highlights, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(viewHolder: EditHighlightsViewHolder, position: Int) {
        viewHolder.setup(items[position]) { index ->
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
    override fun onItemMove(from: Int, to: Int) {
        Collections.swap(items, from, to)
        notifyItemMoved(from, to)
    }

    override fun onItemDismiss(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onMoveComplete(from: Int, to: Int) {
        // no-op
    }
}
