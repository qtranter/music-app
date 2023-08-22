package com.audiomack.ui.queue

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.views.AMNowPlayingImageView

class QueueViewHolder(itemView: View, private val queueDataSource: QueueDataSource) :
    RecyclerView.ViewHolder(itemView) {

    private val tvTitle: AppCompatTextView = itemView.findViewById(R.id.tvTitle)
    private val tvArtist: AppCompatTextView = itemView.findViewById(R.id.tvArtist)
    private val imageView: ImageView = itemView.findViewById(R.id.imageView)
    private val imageViewPlaying: AMNowPlayingImageView = itemView.findViewById(R.id.imageViewPlaying)
    val buttonKebab: ImageButton = itemView.findViewById(R.id.buttonKebab)

    var item: AMResultItem? = null
        private set

    fun setup(item: AMResultItem) {
        this.item = item

        PicassoImageLoader.load(
            imageView.context,
            item.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
            imageView,
            R.drawable.ic_artwork
        )
        tvTitle.setTextFuture(PrecomputedTextCompat.getTextFuture(item.title ?: "", tvTitle.textMetricsParamsCompat, null))
        tvArtist.setTextFuture(PrecomputedTextCompat.getTextFuture(item.artist ?: "", tvArtist.textMetricsParamsCompat, null))
        imageViewPlaying.visibility =
            if (queueDataSource.isCurrentItemOrParent(item)) View.VISIBLE else View.GONE

        // Local files don't have ids, so the filename is used as the itemId
        buttonKebab.isVisible = item.itemId.isDigitsOnly()
    }
}
