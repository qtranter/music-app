package com.audiomack.adapters.viewholders

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.PrecomputedTextCompat
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.adapters.DataRecyclerViewAdapter
import com.audiomack.common.MusicViewHolderDownloadHelper
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMResultItem
import com.audiomack.utils.addTo
import com.audiomack.utils.convertDpToPixel
import com.audiomack.views.AMProgressBar
import io.reactivex.disposables.CompositeDisposable

class PlaylistGridViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val tvTitle: AppCompatTextView = view.findViewById(R.id.tvTitle)
    private val tvSongs: AppCompatTextView = view.findViewById(R.id.tvSubtitle)
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val imageViewDownloaded: ImageView = view.findViewById(R.id.imageViewDownloaded)
    private val buttonMenu: ImageButton = view.findViewById(R.id.buttonMenu)
    private val buttonDownload: ImageButton = view.findViewById(R.id.buttonDownload)
    private val progressBarDownloading: AMProgressBar = view.findViewById(R.id.progressBarDownload)
    private val badgeFrozen: TextView = view.findViewById(R.id.badgeFrozen)

    private var compositeDisposable = CompositeDisposable()

    fun cleanup() {
        compositeDisposable.clear()
    }

    fun setup(item: AMResultItem, position: Int, listener: DataRecyclerViewAdapter.RecyclerViewListener) {

        compositeDisposable.clear()

        tvTitle.setTextFuture(PrecomputedTextCompat.getTextFuture(item.title ?: "", tvTitle.textMetricsParamsCompat, null))

        val songsString = item.newlyAddedSongs?.let { newlyAddedSongs ->
            String.format(
                tvSongs.resources.getString(if (newlyAddedSongs == 1) R.string.notifications_playlists_new_songs_single else R.string.notifications_playlists_new_songs),
                newlyAddedSongs
            )
        } ?: String.format(
                "%d %s",
                item.playlistTracksCount,
                tvSongs.resources.getString(if (item.playlistTracksCount == 1) R.string.playlist_song_singular else R.string.playlist_song_plural)
            )
        tvSongs.setTextFuture(PrecomputedTextCompat.getTextFuture(songsString, tvSongs.textMetricsParamsCompat, null))

        PicassoImageLoader.load(imageView.context, item.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetOriginal), imageView)

        val smallPadding = itemView.context.convertDpToPixel(12f)
        val largePadding = itemView.context.convertDpToPixel(20f)
        val tenDp = itemView.context.convertDpToPixel(10f)

        val layoutParamsImageView = imageView.layoutParams as ConstraintLayout.LayoutParams
        layoutParamsImageView.setMargins(
            if (position % 2 == 0) largePadding else smallPadding,
            if (position < 3) tenDp else largePadding,
            if (position % 2 == 0) smallPadding else largePadding,
            layoutParamsImageView.bottomMargin
        )
        imageView.layoutParams = layoutParamsImageView

        // Default visibility
        imageViewDownloaded.visibility = View.INVISIBLE
        buttonDownload.visibility = View.VISIBLE
        progressBarDownloading.visibility = View.GONE
        progressBarDownloading.isEnabled = false

        MusicViewHolderDownloadHelper()
            .configureDownloadStatus(item, badgeFrozen, imageViewDownloaded, buttonDownload, progressBarDownloading, null, null, false)
            .addTo(compositeDisposable)

        buttonMenu.visibility = if (item.newlyAddedSongs == null) View.VISIBLE else View.GONE

        buttonMenu.setOnClickListener { listener.onClickTwoDots(item) }

        itemView.setOnClickListener { listener.onClickItem(item) }

        buttonDownload.setOnClickListener {
            listener.onClickDownload(item)
        }

        imageViewDownloaded.setOnClickListener {
            if (imageViewDownloaded.visibility == View.VISIBLE) {
                listener.onClickDownload(item)
            }
        }
    }
}
