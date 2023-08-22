package com.audiomack.ui.replacedownload

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.model.AMResultItem

class ReplaceDownloadAdapter(private val listener: ReplaceDownloadListener) : RecyclerView.Adapter<ReplaceDownloadViewHolder>() {

    private val items: MutableList<AMResultItem> = mutableListOf()
    private val selectedItems: MutableList<AMResultItem> = mutableListOf()

    interface ReplaceDownloadListener {
        fun onSongClick(song: AMResultItem, isSelected: Boolean)
    }

    private fun isSelected(item: AMResultItem): Boolean = selectedItems.contains(item)

    fun indexOfItemId(itemId: String?): Int {
        if (itemId == null) return -1
        return items.indexOfFirst { (it as? AMResultItem)?.itemId == itemId }
    }

    fun update(newItems: List<AMResultItem>) {
        this.items.clear()
        this.items.addAll(newItems)
        this.notifyDataSetChanged()
    }

    fun updateSelectedItems(newItems: List<AMResultItem>) {
        this.selectedItems.clear()
        this.selectedItems.addAll(newItems)
        this.notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplaceDownloadViewHolder {
        return ReplaceDownloadViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.row_replace_download,
                parent,
                false
            ))
    }

    override fun onBindViewHolder(holder: ReplaceDownloadViewHolder, position: Int) {
        val item = items[position]
        holder.setup(item, isSelected(item), listener)
    }
}
