package com.audiomack.ui.mylibrary.offline.local

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItem.ItemImagePreset.ItemImagePresetSmall
import com.audiomack.utils.inflate
import com.squareup.picasso.Picasso
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.row_multi_select.bgAlbum
import kotlinx.android.synthetic.main.row_multi_select.imageView
import kotlinx.android.synthetic.main.row_multi_select.imageViewMultiSelect
import kotlinx.android.synthetic.main.row_multi_select.tvArtist
import kotlinx.android.synthetic.main.row_multi_select.tvTitle

class LocalFileSelectionAdapter : ListAdapter<AMResultItem, LocalFileViewHolder>(DiffCallback) {

    var exclusionIds = mutableListOf<Long>()
        set(value) {
            field = value.also { notifyDataSetChanged() }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LocalFileViewHolder(parent)

    override fun onBindViewHolder(holder: LocalFileViewHolder, position: Int) =
        getItem(position).run {
            holder.bind(
                this,
                !exclusionIds.contains(itemId.toLong())
            ) { id ->
                if (!exclusionIds.remove(id)) exclusionIds.add(id)
                notifyItemChanged(holder.bindingAdapterPosition)
            }
        }
}

class LocalFileViewHolder(
    parent: ViewGroup,
    override val containerView: View = parent.inflate(R.layout.row_multi_select)
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: AMResultItem, selected: Boolean, onClick: (Long) -> Unit) {
        with(item) {
            tvTitle.text = title
            tvArtist.text = artist
            bgAlbum.isVisible = isAlbum

            imageViewMultiSelect.setImageResource(
                if (selected) R.drawable.ic_multiselect_on else R.drawable.ic_multiselect_off
            )

            val imageUrl = getImageURLWithPreset(ItemImagePresetSmall)
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.ic_artwork)
                .error(R.drawable.ic_artwork)
                .into(imageView)

            itemView.setOnClickListener { onClick(item.itemId.toLong()) }
        }
    }
}

object DiffCallback : DiffUtil.ItemCallback<AMResultItem>() {
    override fun areItemsTheSame(oldItem: AMResultItem, newItem: AMResultItem) =
        oldItem.itemId == newItem.itemId

    override fun areContentsTheSame(oldItem: AMResultItem, newItem: AMResultItem) =
        oldItem == newItem
}
