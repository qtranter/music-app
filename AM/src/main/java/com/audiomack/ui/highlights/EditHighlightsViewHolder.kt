package com.audiomack.ui.highlights

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMResultItem

class EditHighlightsViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

    val imageView = itemView.findViewById<ImageView>(R.id.imageView)
    val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
    val tvSubtitle = itemView.findViewById<TextView>(R.id.tvSubtitle)
    private val buttonDelete = itemView.findViewById<ImageButton>(R.id.buttonDelete)

    fun setup(music: AMResultItem, deleteHandler: (Int) -> (Unit)) {
        tvTitle.text = music.title
        tvSubtitle.text = music.artist
        PicassoImageLoader.load(
            itemView.context,
            music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
            imageView,
            R.drawable.ic_artwork
        )
        buttonDelete.setOnClickListener { deleteHandler(adapterPosition) }
    }
}
