package com.audiomack.ui.playlist.reorder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.model.AMResultItem
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.ItemTouchHelperAdapter
import com.audiomack.views.AMSnackbar
import java.util.Collections

class ReorderPlaylistAdapter(private val items: MutableList<AMResultItem>) : RecyclerView.Adapter<ReorderPlaylistViewHolder>(), ItemTouchHelperAdapter {

    fun getItems(): List<AMResultItem> {
        return items
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReorderPlaylistViewHolder {
        return ReorderPlaylistViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_reorder_playlist, parent, false))
    }

    override fun onBindViewHolder(holder: ReorderPlaylistViewHolder, position: Int) {
        val item = items[position]
        holder.setup(item) {
            if (items.size == 1) {
                AMSnackbar.Builder(HomeActivity.instance)
                    .withTitle(holder.buttonDelete.context.getString(R.string.edit_playlist_tracks_reorder_error_last_track))
                    .withDrawable(R.drawable.ic_snackbar_error)
                    .withSecondary(R.drawable.ic_snackbar_playlist_grey)
                    .show()
            } else {
                items.removeAt(holder.adapterPosition)
                notifyDataSetChanged()
            }
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
