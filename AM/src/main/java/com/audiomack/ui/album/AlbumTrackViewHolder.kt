package com.audiomack.ui.album

import android.graphics.Color
import android.text.TextUtils
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.DISABLED_ALPHA
import com.audiomack.R
import com.audiomack.common.MusicViewHolderDownloadHelper
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.user.UserData
import com.audiomack.model.AMResultItem
import com.audiomack.utils.addTo
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressBar
import io.reactivex.disposables.CompositeDisposable
import java.util.Locale

class AlbumTrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
    private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    private val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
    private val buttonActions: ImageButton = itemView.findViewById(R.id.buttonActions)
    private val buttonDownload: ImageButton = itemView.findViewById(R.id.buttonDownload)
    private val imageViewPlaying: ImageView = itemView.findViewById(R.id.imageViewPlaying)
    private val imageViewDownloaded: ImageView = itemView.findViewById(R.id.imageViewDownloaded)
    private val imageViewLocal: ImageView = itemView.findViewById(R.id.imageViewLocalFile)
    private val progressBarDownloading: AMProgressBar = itemView.findViewById(R.id.progressBarDownload)
    private val buttonFavorite: ImageButton = itemView.findViewById(R.id.buttonFavorite)

    private var compositeDisposable = CompositeDisposable()

    fun setup(
        track: AMResultItem,
        myDownloadsMode: Boolean,
        listener: AlbumTracksAdapter.Listener?
    ) {

        compositeDisposable.clear()

        val trackNumber = if (track.trackNumber == 0) bindingAdapterPosition.inc() else track.trackNumber
        tvNumber.text = String.format(Locale.US, "%d.", trackNumber)

        val featString = if (TextUtils.isEmpty(track.featured)) "" else String.format(
            " %s %s",
            tvTitle.resources.getString(R.string.feat_inline),
            track.featured
        )
        val fullString = String.format("%s%s", track.title, featString)
        val titleString = tvTitle.context.spannableString(
            fullString = fullString,
            highlightedStrings = listOf(featString),
            fullColor = Color.WHITE,
            highlightedColor = tvTitle.context.colorCompat(R.color.orange),
            fullFont = R.font.opensans_bold,
            highlightedFont = R.font.opensans_semibold,
            highlightedSize = 13
        )
        tvTitle.text = titleString
        tvArtist.text = track.artist

        val currentlyPlaying = QueueRepository.getInstance().isCurrentItemOrParent(track)
        imageViewPlaying.visibility = if (currentlyPlaying) View.VISIBLE else View.GONE
        tvNumber.visibility = if (currentlyPlaying) View.GONE else View.VISIBLE

        // Default visibility
        imageViewDownloaded.visibility = View.GONE
        imageViewLocal.visibility = View.GONE
        buttonDownload.visibility = View.VISIBLE
        progressBarDownloading.visibility = View.GONE
        progressBarDownloading.isEnabled = false
        buttonFavorite.isVisible = false

        val views = listOf(tvArtist, tvTitle, tvNumber, imageViewPlaying)

        if (track.isGeoRestricted || track.isLocal) {
            if (track.isGeoRestricted) {
                views.forEach { it.alpha = DISABLED_ALPHA }
                buttonActions.alpha = DISABLED_ALPHA
                buttonFavorite.isVisible = true
            }
            imageViewDownloaded.visibility = View.INVISIBLE
            buttonDownload.visibility = View.GONE
            progressBarDownloading.visibility = View.GONE
            imageViewLocal.isVisible = track.isLocal
            buttonActions.setImageDrawable(
                buttonActions.context.drawableCompat(R.drawable.ic_list_kebab)
            )
        } else {
            buttonActions.alpha = 1.0f
            views.forEach { it.alpha = 1.0f }
            MusicViewHolderDownloadHelper()
                .configureDownloadStatus(track, null, imageViewDownloaded, buttonDownload, progressBarDownloading, buttonActions, views, myDownloadsMode)
                .addTo(compositeDisposable)
        }

        buttonFavorite.setImageResource(
            when {
                track.favoriteStatus == AMResultItem.ItemAPIStatus.On -> R.drawable.ic_list_heart_filled
                track.favoriteStatus == AMResultItem.ItemAPIStatus.Loading && !UserData.isItemFavorited(track) -> R.drawable.ic_list_heart_filled
                else -> R.drawable.ic_list_heart_empty
            }
        )

        buttonDownload.setOnClickListener {
            listener?.onTrackDownloadTapped(track)
        }

        imageViewDownloaded.setOnClickListener {
            if (imageViewDownloaded.visibility == View.VISIBLE) {
                listener?.onTrackDownloadTapped(track)
            }
        }

        buttonActions.setOnClickListener { listener?.onTrackActionsTapped(track) }

        buttonFavorite.setOnClickListener { listener?.onTrackFavoriteTapped(track) }

        itemView.setOnClickListener { listener?.onTrackTapped(track) }
    }
}
