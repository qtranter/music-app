package com.audiomack.ui.highlights

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.DISABLED_ALPHA
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMResultItem
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString

class HighlightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val viewAlbum1 = itemView.findViewById<View>(R.id.viewAlbum1)
    private val viewAlbum2 = itemView.findViewById<View>(R.id.viewAlbum2)
    private val imageView = itemView.findViewById<ImageView>(R.id.imageView)
    private val tvArtist = itemView.findViewById<TextView>(R.id.tvArtist)
    private val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
    private val tvFeat = itemView.findViewById<TextView>(R.id.tvFeat)
    private val tvPlaylistSongs = itemView.findViewById<TextView>(R.id.tvPlaylistSongs)
    private val buttonMenu = itemView.findViewById<ImageButton>(R.id.buttonMenu)

    @SuppressLint("SetTextI18n")
    fun setup(myAccount: Boolean, music: AMResultItem, menuHandler: () -> (Unit)) {
        val musicCollection = music.isPlaylist || music.isAlbum
        viewAlbum1.visibility = if (musicCollection) View.VISIBLE else View.GONE
        viewAlbum2.visibility = if (musicCollection) View.VISIBLE else View.GONE
        (imageView.layoutParams as ConstraintLayout.LayoutParams).apply {
            topMargin = if (musicCollection) imageView.context.convertDpToPixel(10.toFloat()) else 0
            width = imageView.context.convertDpToPixel(if (musicCollection) 145.toFloat() else 155.toFloat())
            height = width
        }
        PicassoImageLoader.load(
            imageView.context,
            music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetOriginal),
            imageView,
            R.drawable.ic_artwork
        )

        tvArtist.text = music.artist
        tvArtist.visibility = if (music.isPlaylist) View.GONE else View.VISIBLE

        tvTitle.text = music.title

        if (music.isPlaylist && !music.featured.isNullOrBlank()) {
            tvFeat.text = tvFeat.context.spannableString(
                fullString = "${tvFeat.context.getString(R.string.feat)} ${music.featured}",
                highlightedStrings = listOf(music.featured ?: ""),
                highlightedColor = tvFeat.context.colorCompat(R.color.orange)
            )
            tvFeat.visibility = View.VISIBLE
        } else {
            tvFeat.visibility = View.GONE
        }

        tvPlaylistSongs.text = "${music.playlistTracksCount} ${tvPlaylistSongs.context.getString(if (music.playlistTracksCount == 1) R.string.playlist_song_singular else R.string.playlist_song_plural)}"
        tvPlaylistSongs.visibility = if (music.isPlaylist) View.VISIBLE else View.GONE

        buttonMenu.visibility = if (myAccount) View.VISIBLE else View.GONE
        buttonMenu.setOnClickListener { menuHandler() }

        val views = listOf(
            viewAlbum1,
            viewAlbum2,
            imageView,
            tvArtist,
            tvTitle,
            tvFeat,
            tvPlaylistSongs,
            buttonMenu
        )
        for (view in views) {
            view.alpha = if (music.isGeoRestricted) DISABLED_ALPHA else 1.0f
        }
    }
}
