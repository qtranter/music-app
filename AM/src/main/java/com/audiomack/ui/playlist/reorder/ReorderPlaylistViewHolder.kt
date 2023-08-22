package com.audiomack.ui.playlist.reorder

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMResultItem

class ReorderPlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
    val imageView: ImageView = itemView.findViewById(R.id.imageView)
    val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)

    fun setup(music: AMResultItem, deleteHandler: () -> (Unit)) {
        tvTitle.text = music.title
        tvSubtitle.text = music.artist
        PicassoImageLoader.load(imageView.context, music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall), imageView)
        buttonDelete.setOnClickListener { deleteHandler() }
    }
}
