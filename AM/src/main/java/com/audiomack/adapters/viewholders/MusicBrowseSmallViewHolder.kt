package com.audiomack.adapters.viewholders

import android.graphics.Color
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.DISABLED_ALPHA
import com.audiomack.R
import com.audiomack.adapters.DataRecyclerViewAdapter
import com.audiomack.common.MusicViewHolderDownloadHelper
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.queue.QueueRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.CellType
import com.audiomack.utils.addTo
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressBar
import io.reactivex.disposables.CompositeDisposable
import java.util.Locale

class MusicBrowseSmallViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val tvReposted: TextView = view.findViewById(R.id.tvReposted)
    private val tvFeatured: TextView = view.findViewById(R.id.tvFeatured)
    private val tvChart: TextView = view.findViewById(R.id.tvChart)
    private val tvArtist: TextView = view.findViewById(R.id.tvArtist)
    private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
    private val tvFeat: TextView = view.findViewById(R.id.tvFeat)
    private val tvPlays: TextView = view.findViewById(R.id.tvPlays)
    private val tvFavs: TextView = view.findViewById(R.id.tvFavs)
    private val tvReups: TextView = view.findViewById(R.id.tvReups)
    private val tvAdds: TextView = view.findViewById(R.id.tvAdds)
    private val layoutTexts: ViewGroup = view.findViewById(R.id.layoutTexts)
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val imageViewPlaying: ImageView = view.findViewById(R.id.imageViewPlaying)
    private val imageViewDownloaded: ImageView = view.findViewById(R.id.imageViewDownloaded)
    private val viewAlbum1: View = view.findViewById(R.id.viewAlbumLine1)
    private val viewAlbum2: View = view.findViewById(R.id.viewAlbumLine2)
    private val buttonActions: ImageButton = view.findViewById(R.id.buttonActions)
    private val buttonDownload: ImageButton = view.findViewById(R.id.buttonDownload)
    private val layoutStats: ViewGroup = view.findViewById(R.id.layoutStats)
    private val progressBarDownloading: AMProgressBar = view.findViewById(R.id.progressBarDownload)
    private val badgeFrozen: TextView = view.findViewById(R.id.badgeFrozen)

    private var compositeDisposable = CompositeDisposable()

    private val typefaceBold = ResourcesCompat.getFont(view.context, R.font.opensans_bold)
    private val typefaceRegular = ResourcesCompat.getFont(view.context, R.font.opensans_regular)
    private val greyTextColor = ResourcesCompat.getColor(view.resources, R.color.gray_text, null)
    private val whiteTextColor = ResourcesCompat.getColor(view.resources, R.color.white, null)

    fun cleanup() {
        compositeDisposable.clear()
    }

    fun setup(
        item: AMResultItem,
        featuredText: String?,
        featured: Boolean,
        showRepostedInfo: Boolean = false,
        listener: DataRecyclerViewAdapter.RecyclerViewListener?,
        position: Int = 0,
        removePaddingFromFirstPosition: Boolean = false,
        cellType: CellType,
        hideStats: Boolean = false,
        hideActions: Boolean = false
    ) {

        compositeDisposable.clear()

        val currentlyPlaying = QueueRepository.getInstance().isCurrentItemOrParent(item)

        (imageView.layoutParams as ConstraintLayout.LayoutParams).also {
            it.topMargin =
                if (position == 1 && removePaddingFromFirstPosition) 0 else imageView.context.convertDpToPixel(10f)
            imageView.layoutParams = it
        }
        (layoutTexts.layoutParams as ConstraintLayout.LayoutParams).also {
            it.bottomMargin =
                if (position == 1 && removePaddingFromFirstPosition)
                    layoutTexts.context.convertDpToPixel(10f)
                else 0
            layoutTexts.layoutParams = it
        }

        tvReposted.text =
            if (item.repostArtistName != null) item.repostArtistName.toUpperCase() else null
        tvReposted.visibility =
            if (tvReposted.text.toString().isBlank() || !showRepostedInfo) View.GONE else View.VISIBLE
        tvFeatured.text = featuredText
        tvFeatured.visibility = if (TextUtils.isEmpty(featuredText)) View.GONE else View.VISIBLE
        itemView.setBackgroundColor(
            if (!featured) Color.TRANSPARENT else itemView.context.colorCompat(
                R.color.featured_music_highlight
            )
        )
        layoutStats.visibility =
            if (!TextUtils.isEmpty(featuredText) && !TextUtils.isEmpty(item.featured) || !item.hasStats() || hideStats) View.GONE else View.VISIBLE
        tvChart.visibility =
            if (cellType == CellType.MUSIC_BROWSE_SMALL_CHART && !currentlyPlaying) View.VISIBLE else View.INVISIBLE
        tvChart.text = String.format(Locale.US, "%d", position)
        imageViewPlaying.visibility = if (currentlyPlaying) View.VISIBLE else View.GONE

        if (item.isPlaylist) {
            // swap places between playlist title and playlist creator's name as requested
            // the playlist title
            tvArtist.text = item.title
            tvArtist.setSingleLine(false)
            tvArtist.maxLines = 2
            tvArtist.typeface = typefaceBold

            // the playlist creator's name
            tvTitle.text = item.artist
            tvTitle.setTextColor(greyTextColor)
            tvTitle.typeface = typefaceRegular
        } else {
            tvArtist.text = item.artist
            tvArtist.setSingleLine(true)
            tvArtist.maxLines = 1
            tvArtist.typeface = typefaceRegular

            tvTitle.text = item.title
            tvTitle.setTextColor(whiteTextColor)
            tvTitle.typeface = typefaceBold
        }

        if (!TextUtils.isEmpty(item.featured)) {
            tvFeat.text = tvFeat.context.spannableString(
                fullString = tvFeat.resources.getString(R.string.feat) + " " + item.featured,
                highlightedStrings = listOf(item.featured ?: ""),
                highlightedColor = tvFeat.context.colorCompat(R.color.orange)
            )
            tvFeat.visibility = View.VISIBLE
        } else {
            tvFeat.visibility = View.GONE
        }

        tvPlays.text = item.playsShort
        tvFavs.text = item.favoritesShort
        tvReups.text = item.repostsShort
        tvAdds.text = item.playlistsShort
        tvAdds.visibility =
            if (item.isSong || item.isPlaylistTrack || item.isAlbumTrack) View.VISIBLE else View.GONE

        // Default visibility
        imageViewDownloaded.visibility = View.INVISIBLE
        buttonDownload.visibility = View.VISIBLE
        progressBarDownloading.visibility = View.GONE
        progressBarDownloading.isEnabled = false
        buttonActions.setImageResource(R.drawable.ic_list_kebab)

        val views = listOf(
            tvReposted,
            tvFeatured,
            tvArtist,
            tvTitle,
            tvFeat,
            tvPlays,
            tvFavs,
            tvReups,
            tvAdds,
            imageView,
            viewAlbum1,
            viewAlbum2,
            imageViewPlaying
        )

        if (item.isGeoRestricted) {
            views.forEach { it.alpha = DISABLED_ALPHA }
            buttonActions.alpha = DISABLED_ALPHA
            imageViewDownloaded.visibility = View.INVISIBLE
            buttonDownload.visibility = View.GONE
            progressBarDownloading.visibility = View.GONE
            buttonActions.setImageDrawable(buttonActions.context.drawableCompat(if (hideActions) R.drawable.settings_chevron else R.drawable.ic_list_kebab))
            buttonActions.setOnClickListener { listener?.onClickItem(item) }
        } else {
            if (hideActions) {
                imageViewDownloaded.visibility = View.INVISIBLE
                buttonDownload.visibility = View.GONE
                progressBarDownloading.visibility = View.GONE
                buttonActions.setImageDrawable(buttonActions.context.drawableCompat(R.drawable.settings_chevron))
                buttonActions.setOnClickListener { listener?.onClickItem(item) }
            } else {
                buttonActions.alpha = 1.0f
                views.forEach { it.alpha = 1.0f }
                MusicViewHolderDownloadHelper()
                    .configureDownloadStatus(item, badgeFrozen, imageViewDownloaded, buttonDownload, progressBarDownloading, buttonActions, views, false)
                    .addTo(compositeDisposable)
                buttonActions.setOnClickListener { listener?.onClickTwoDots(item) }
            }
        }

        PicassoImageLoader.load(
            imageView.context,
            item.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
            imageView,
            R.drawable.ic_artwork
        )
        viewAlbum1.visibility = if (item.isAlbum) View.VISIBLE else View.INVISIBLE
        viewAlbum2.visibility = if (item.isAlbum) View.VISIBLE else View.INVISIBLE

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
