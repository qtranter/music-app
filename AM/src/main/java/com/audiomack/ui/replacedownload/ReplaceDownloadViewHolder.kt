package com.audiomack.ui.replacedownload

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMResultItem
import com.audiomack.utils.extensions.drawableCompat

class ReplaceDownloadViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {

    private val tvTitle: AppCompatTextView = itemView.findViewById(R.id.tvTitle)
    private val tvArtist: AppCompatTextView = itemView.findViewById(R.id.tvArtist)
    private val imageView: ImageView = itemView.findViewById(R.id.imageView)
    private val buttonAction: ImageButton = itemView.findViewById(R.id.buttonAction)

    var item: AMResultItem? = null
        private set

    fun setup(
        item: AMResultItem,
        isSelected: Boolean,
        listener: ReplaceDownloadAdapter.ReplaceDownloadListener
    ) {
        this.item = item

        PicassoImageLoader.load(
            imageView.context,
            item.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
            imageView,
            R.drawable.ic_artwork
        )

        val selectedIcon = R.drawable.ic_download_remove_selected
        val normalIcon = R.drawable.ic_download_remove_plain

        buttonAction.setImageDrawable(buttonAction.context.drawableCompat(if (isSelected) selectedIcon else normalIcon))

        tvTitle.setTextFuture(PrecomputedTextCompat.getTextFuture(item.title
            ?: "", tvTitle.textMetricsParamsCompat, null))
        tvArtist.setTextFuture(PrecomputedTextCompat.getTextFuture(item.artist
            ?: "", tvArtist.textMetricsParamsCompat, null))

        buttonAction.setOnClickListener { listener.onSongClick(item, isSelected) }
    }
}
