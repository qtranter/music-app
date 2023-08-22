package com.audiomack.ui.player.maxi.uploader

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import java.util.Locale
import kotlinx.android.synthetic.main.item_player_tag.view.tvTag

class TagsAdapter(private val onTagClicked: (String) -> Unit) :
    ListAdapter<String, TagViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        return TagViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_player_tag, parent, false)
        )
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position), onTagClicked)
    }
}

class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    @SuppressLint("SetTextI18n")
    fun bind(tag: String, onTagClicked: (String) -> Unit) {
        with(itemView.tvTag) {
            text = "#${tag.toUpperCase(Locale.US)}"
            setOnClickListener { onTagClicked(tag) }
        }
    }
}

object DiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
        oldItem == newItem
}
