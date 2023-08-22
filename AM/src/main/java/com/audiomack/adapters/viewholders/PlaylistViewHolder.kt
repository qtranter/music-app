package com.audiomack.adapters.viewholders

import android.graphics.Color
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.adapters.DataRecyclerViewAdapter
import com.audiomack.common.MusicViewHolderDownloadHelper
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.queue.QueueRepository
import com.audiomack.model.AMResultItem
import com.audiomack.utils.addTo
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.spannableStringWithImageAtTheEnd
import com.audiomack.views.AMProgressBar
import io.reactivex.disposables.CompositeDisposable

class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val tvFeatured: TextView = view.findViewById(R.id.tvFeatured)
    private val tvArtist: TextView = view.findViewById(R.id.tvArtist)
    private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
    private val tvTotalSongs: TextView = view.findViewById(R.id.tvTotalSongs)
    private val layoutTexts: ViewGroup = view.findViewById(R.id.layoutTexts)
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val imageViewPlaying: ImageView = view.findViewById(R.id.imageViewPlaying)
    private val imageViewDownloaded: ImageView = view.findViewById(R.id.imageViewDownloaded)
    private val buttonActions: ImageButton = view.findViewById(R.id.buttonActions)
    private val buttonDownload: ImageButton = view.findViewById(R.id.buttonDownload)
    private val progressBarDownloading: AMProgressBar = view.findViewById(R.id.progressBarDownload)
    private val badgeFrozen: TextView = view.findViewById(R.id.badgeFrozen)

    private var compositeDisposable = CompositeDisposable()

    fun cleanup() {
        compositeDisposable.clear()
    }

    fun setup(
        item: AMResultItem,
        featuredText: String?,
        featured: Boolean,
        listener: DataRecyclerViewAdapter.RecyclerViewListener?,
        position: Int,
        removePaddingFromFirstPosition: Boolean = false,
        hideActions: Boolean = false
    ) {

        compositeDisposable.clear()

        val currentlyPlaying = QueueRepository.getInstance().isCurrentItemOrParent(item)

        (imageView.layoutParams as ConstraintLayout.LayoutParams).also {
            it.topMargin =
                if (position == 1 && removePaddingFromFirstPosition) 6 else imageView.context.convertDpToPixel(16f)
            imageView.layoutParams = it
        }
        (layoutTexts.layoutParams as ConstraintLayout.LayoutParams).also {
            it.bottomMargin =
                layoutTexts.context.convertDpToPixel(
                    if (position == 1 && removePaddingFromFirstPosition) 10f else 6f
                )
            layoutTexts.layoutParams = it
        }

        tvFeatured.text = featuredText
        tvFeatured.visibility = if (TextUtils.isEmpty(featuredText)) View.GONE else View.VISIBLE
        itemView.setBackgroundColor(
            if (!featured) Color.TRANSPARENT else itemView.context.colorCompat(
                R.color.featured_music_highlight
            )
        )
        imageViewPlaying.visibility = if (currentlyPlaying) View.VISIBLE else View.GONE
        tvTitle.text = item.title

        tvArtist.text = when {
            item.isUploaderVerified -> tvArtist.spannableStringWithImageAtTheEnd(
                item.artist,
                R.drawable.ic_verified,
                (tvArtist.textSize / tvArtist.resources.displayMetrics.density * 0.9).toInt()
            )
            item.isUploaderTastemaker -> tvArtist.spannableStringWithImageAtTheEnd(
                item.artist,
                R.drawable.ic_tastemaker,
                (tvArtist.textSize / tvArtist.resources.displayMetrics.density * 0.9).toInt()
            )
            item.isUploaderAuthenticated -> tvArtist.spannableStringWithImageAtTheEnd(
                item.artist,
                R.drawable.ic_authenticated,
                (tvArtist.textSize / tvArtist.resources.displayMetrics.density * 0.9).toInt()
            )
            else -> item.artist
        }

        tvTotalSongs.text = String.format("%d", item.playlistTracksCount)

        // Default visibility
        imageViewDownloaded.visibility = View.INVISIBLE
        buttonDownload.visibility = View.VISIBLE
        progressBarDownloading.visibility = View.GONE
        buttonActions.setImageResource(R.drawable.ic_list_kebab)
        progressBarDownloading.isEnabled = false

        if (hideActions) {
            imageViewDownloaded.visibility = View.INVISIBLE
            buttonDownload.visibility = View.GONE
            progressBarDownloading.visibility = View.GONE
            buttonActions.setImageDrawable(buttonActions.context.drawableCompat(R.drawable.settings_chevron))
            buttonActions.setOnClickListener { listener?.onClickItem(item) }
        } else {
            MusicViewHolderDownloadHelper()
                .configureDownloadStatus(item, badgeFrozen, imageViewDownloaded, buttonDownload, progressBarDownloading, buttonActions, null, false)
                .addTo(compositeDisposable)
            buttonActions.setOnClickListener { listener?.onClickTwoDots(item) }
        }

        PicassoImageLoader.load(
            imageView.context,
            item.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
            imageView
        )

        buttonDownload.setOnClickListener {
            listener?.onClickDownload(item)
        }

        imageViewDownloaded.setOnClickListener {
            if (imageViewDownloaded.visibility == View.VISIBLE) {
                listener?.onClickDownload(item)
            }
        }

        itemView.setOnClickListener { listener?.onClickItem(item) }
    }
}
