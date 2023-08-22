package com.audiomack.adapters.viewholders

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.DISABLED_ALPHA
import com.audiomack.R
import com.audiomack.adapters.DataRecyclerViewAdapter
import com.audiomack.common.MusicViewHolderDownloadHelper
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.queue.QueueRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItem.ItemImagePreset.ItemImagePresetSmall
import com.audiomack.utils.addTo
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressBar
import io.reactivex.disposables.CompositeDisposable
import java.util.Locale

class MusicTinyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
    private val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val imageViewPlaying: ImageView = view.findViewById(R.id.imageViewPlaying)
    private val imageViewDownloaded: ImageView = view.findViewById(R.id.imageViewDownloaded)
    private val imageViewLocalFile: ImageView = view.findViewById(R.id.imageViewLocalFile)
    private val buttonActions: ImageButton = view.findViewById(R.id.buttonActions)
    private val buttonDownload: ImageButton = view.findViewById(R.id.buttonDownload)
    private val progressBarDownloading: AMProgressBar = view.findViewById(R.id.progressBarDownload)
    private val badgeFrozen: TextView = view.findViewById(R.id.badgeFrozen)
    private val viewAlbumLine1 = itemView.findViewById<View>(R.id.viewAlbumLine1)
    private val viewAlbumLine2 = itemView.findViewById<View>(R.id.viewAlbumLine2)

    private var compositeDisposable = CompositeDisposable()

    fun cleanup() {
        compositeDisposable.clear()
    }

    fun setup(
        music: AMResultItem,
        myDownloadsMode: Boolean,
        listener: DataRecyclerViewAdapter.RecyclerViewListener?
    ) {

        compositeDisposable.clear()

        val currentlyPlaying = QueueRepository.getInstance().isCurrentItemOrParent(music)

        imageViewPlaying.visibility = if (currentlyPlaying) View.VISIBLE else View.GONE

        val featString = if (music.featured.isNullOrBlank()) "" else String.format(
            " %s %s",
            tvTitle.resources.getString(R.string.feat_inline),
            music.featured
        )
        tvTitle.text = tvTitle.context.spannableString(
            fullString = String.format("%s%s", music.title, featString),
            highlightedStrings = listOf(featString),
            highlightedColor = tvTitle.context.colorCompat(R.color.orange),
            highlightedFont = R.font.opensans_semibold
        )

        tvSubtitle.text = if (music.isPlaylist) {
            String.format(
                Locale.US,
                "%d %s",
                music.playlistTracksCount,
                tvSubtitle.resources.getString(if (music.playlistTracksCount == 1) R.string.playlist_song_singular else R.string.playlist_song_plural)
            )
        } else {
            music.artist
        }

        viewAlbumLine1.visibility = if (music.isAlbum) View.VISIBLE else View.GONE
        viewAlbumLine2.visibility = if (music.isAlbum) View.VISIBLE else View.GONE

        // Default visibility
        imageView.setImageDrawable(null)
        imageViewDownloaded.visibility = View.INVISIBLE
        imageViewLocalFile.visibility = View.GONE
        buttonDownload.visibility = View.VISIBLE
        buttonDownload.setImageDrawable(null)
        progressBarDownloading.visibility = View.GONE
        buttonActions.setImageResource(R.drawable.ic_list_kebab)
        progressBarDownloading.isEnabled = false

        val views = listOf(
            tvTitle,
            tvSubtitle,
            imageView,
            viewAlbumLine1,
            viewAlbumLine2,
            imageViewPlaying
        )

        if (music.isGeoRestricted || music.isLocal) {
            if (music.isGeoRestricted) {
                views.forEach { it.alpha = DISABLED_ALPHA }
                buttonActions.alpha = DISABLED_ALPHA
            }
            imageViewDownloaded.visibility = View.INVISIBLE
            buttonDownload.visibility = View.GONE
            progressBarDownloading.visibility = View.GONE
            buttonActions.setImageDrawable(buttonActions.context.drawableCompat(R.drawable.ic_list_kebab))
            buttonActions.setOnClickListener {
                if (music.isLocal) {
                    listener?.onClickTwoDots(music)
                } else {
                    listener?.onClickItem(music)
                }
            }
            imageViewLocalFile.isVisible = music.isLocal
        } else {
            views.forEach { it.alpha = 1.0f }
            MusicViewHolderDownloadHelper()
                .configureDownloadStatus(music, badgeFrozen, imageViewDownloaded, buttonDownload, progressBarDownloading, buttonActions, views, myDownloadsMode)
                .addTo(compositeDisposable)
            buttonActions.setOnClickListener { listener?.onClickTwoDots(music) }
        }

        music.getImageURLWithPreset(ItemImagePresetSmall)?.let { artworkPath ->
            PicassoImageLoader.load(
                imageView.context,
                artworkPath,
                imageView,
                R.drawable.ic_artwork
            )
        } ?: run {
            imageView.setImageResource(R.drawable.ic_artwork)
        }

        buttonDownload.setOnClickListener {
            listener?.onClickDownload(music)
        }

        imageViewDownloaded.setOnClickListener {
            if (imageViewDownloaded.visibility == View.VISIBLE) {
                listener?.onClickDownload(music)
            }
        }

        itemView.setOnClickListener { listener?.onClickItem(music) }
    }
}
