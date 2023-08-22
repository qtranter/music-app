package com.audiomack.ui.feed

import android.graphics.Color
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.audiomack.DISABLED_ALPHA
import com.audiomack.R
import com.audiomack.common.MusicViewHolderDownloadHelper
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMResultItem
import com.audiomack.model.CellType
import com.audiomack.utils.addTo
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.spannableString
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import io.reactivex.disposables.CompositeDisposable
import java.lang.NumberFormatException
import java.util.Locale
import kotlinx.android.synthetic.main.row_browsemusic_small.view.badgeFrozen
import kotlinx.android.synthetic.main.row_browsemusic_small.view.buttonActions
import kotlinx.android.synthetic.main.row_browsemusic_small.view.buttonDownload
import kotlinx.android.synthetic.main.row_browsemusic_small.view.imageView
import kotlinx.android.synthetic.main.row_browsemusic_small.view.imageViewDownloaded
import kotlinx.android.synthetic.main.row_browsemusic_small.view.imageViewPlaying
import kotlinx.android.synthetic.main.row_browsemusic_small.view.layoutStats
import kotlinx.android.synthetic.main.row_browsemusic_small.view.layoutTexts
import kotlinx.android.synthetic.main.row_browsemusic_small.view.progressBarDownload
import kotlinx.android.synthetic.main.row_browsemusic_small.view.tvAdds
import kotlinx.android.synthetic.main.row_browsemusic_small.view.tvArtist
import kotlinx.android.synthetic.main.row_browsemusic_small.view.tvChart
import kotlinx.android.synthetic.main.row_browsemusic_small.view.tvFavs
import kotlinx.android.synthetic.main.row_browsemusic_small.view.tvFeat
import kotlinx.android.synthetic.main.row_browsemusic_small.view.tvFeatured
import kotlinx.android.synthetic.main.row_browsemusic_small.view.tvPlays
import kotlinx.android.synthetic.main.row_browsemusic_small.view.tvReposted
import kotlinx.android.synthetic.main.row_browsemusic_small.view.tvReups
import kotlinx.android.synthetic.main.row_browsemusic_small.view.tvTitle
import kotlinx.android.synthetic.main.row_browsemusic_small.view.viewAlbumLine1
import kotlinx.android.synthetic.main.row_browsemusic_small.view.viewAlbumLine2

class BrowseMusicSmallCardItem(
    val item: AMResultItem,
    var currentlyPlaying: Boolean = false,
    private val featuredText: String?,
    private val showRepostedInfo: Boolean = false,
    private val listener: SocialFeedCardItemListener?,
    private val cellType: CellType,
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()
) : Item() {

    interface SocialFeedCardItemListener {
        fun onClickTwoDots(item: AMResultItem)
        fun onClickDownload(item: AMResultItem)
        fun onClickItem(item: AMResultItem)
    }

    override fun getId(): Long {
        return try {
            item.itemId.toLong()
        } catch (exception: NumberFormatException) {
            0
        }
    }

    override fun getLayout() = R.layout.row_browsemusic_small

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val view = viewHolder.itemView
        val context = viewHolder.itemView.context
        val typefaceBold = ResourcesCompat.getFont(view.context, R.font.opensans_bold)
        val typefaceRegular = ResourcesCompat.getFont(view.context, R.font.opensans_regular)

        compositeDisposable.clear()

        with(viewHolder.itemView) {
            (imageView.layoutParams as ConstraintLayout.LayoutParams).also {
                it.topMargin = imageView.context.convertDpToPixel(10f)
                imageView.layoutParams = it
            }
            (layoutTexts.layoutParams as ConstraintLayout.LayoutParams).also {
                it.bottomMargin = 0
                layoutTexts.layoutParams = it
            }

            tvReposted.text = item.repostArtistName?.toUpperCase()
            tvReposted.isVisible = tvReposted.text.toString()
                .isNotBlank() || showRepostedInfo

            tvFeatured.text = featuredText
            tvFeatured.isVisible = featuredText?.isNotEmpty() ?: false
            setBackgroundColor(Color.TRANSPARENT)
            layoutStats.isVisible =
                item.hasStats() || featuredText?.isEmpty() ?: true && item.featured?.isEmpty() ?: true
            tvChart.isInvisible = cellType != CellType.MUSIC_BROWSE_SMALL_CHART || currentlyPlaying
            tvChart.text = String.format(Locale.US, "%d", position)
            imageViewPlaying.isVisible = currentlyPlaying

            if (item.isPlaylist) {
                // swap places between playlist title and playlist creator's name as requested
                // the playlist title
                tvArtist.text = item.title
                tvArtist.isSingleLine = false
                tvArtist.maxLines = 2
                tvArtist.typeface = typefaceBold

                // the playlist creator's name
                tvTitle.text = item.artist
                tvTitle.setTextColor(context.colorCompat(R.color.gray_text))
                tvTitle.typeface = typefaceRegular
            } else {
                tvArtist.text = item.artist
                tvArtist.isSingleLine = true
                tvArtist.maxLines = 1
                tvArtist.typeface = typefaceRegular

                tvTitle.text = item.title
                tvTitle.setTextColor(context.colorCompat(R.color.white))
                tvTitle.typeface = typefaceBold
            }

            if (item.featured?.isNotEmpty() == true) {
                tvFeat.text = tvFeat.context.spannableString(
                    fullString = tvFeat.resources.getString(R.string.feat) + " " + item.featured,
                    highlightedStrings = listOf(item.featured ?: ""),
                    highlightedColor = context.colorCompat(R.color.orange)
                )
                tvFeat.isVisible = true
            } else {
                tvFeat.isVisible = false
            }

            tvPlays.text = item.playsShort
            tvFavs.text = item.favoritesShort
            tvReups.text = item.repostsShort
            tvAdds.text = item.playlistsShort
            tvAdds.isVisible = item.isSong || item.isPlaylistTrack || item.isAlbumTrack

            // Default visibility
            imageViewDownloaded.visibility = View.INVISIBLE
            buttonDownload.visibility = View.VISIBLE
            progressBarDownload.visibility = View.GONE
            progressBarDownload.isEnabled = false
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
                viewAlbumLine1,
                viewAlbumLine2,
                imageViewPlaying
            )

            if (item.isGeoRestricted) {
                views.forEach { it.alpha = DISABLED_ALPHA }
                buttonActions.alpha = DISABLED_ALPHA
                imageViewDownloaded.visibility = View.INVISIBLE
                buttonDownload.visibility = View.GONE
                progressBarDownload.visibility = View.GONE
                buttonActions.setImageDrawable(buttonActions.context.drawableCompat(R.drawable.ic_list_kebab))
                buttonActions.setOnClickListener { listener?.onClickItem(item) }
            } else {
                buttonActions.alpha = 1.0f
                views.forEach { it.alpha = 1.0f }
                MusicViewHolderDownloadHelper()
                    .configureDownloadStatus(
                        item,
                        badgeFrozen,
                        imageViewDownloaded,
                        buttonDownload,
                        progressBarDownload,
                        buttonActions,
                        views,
                        false
                    ).addTo(compositeDisposable)
                buttonActions.setOnClickListener { listener?.onClickTwoDots(item) }
            }

            PicassoImageLoader.load(
                imageView.context,
                item.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
                imageView
            )
            viewAlbumLine1.isInvisible = !item.isAlbum
            viewAlbumLine2.isInvisible = !item.isAlbum

            buttonDownload.setOnClickListener {
                listener?.onClickDownload(item)
            }

            imageViewDownloaded.setOnClickListener {
                if (imageViewDownloaded.isVisible) {
                    listener?.onClickDownload(item)
                }
            }

            setOnClickListener { listener?.onClickItem(item) }
        }
    }

    override fun unbind(viewHolder: GroupieViewHolder) {
        super.unbind(viewHolder)
        compositeDisposable.clear()
    }
}
