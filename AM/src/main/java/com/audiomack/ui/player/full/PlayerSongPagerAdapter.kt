package com.audiomack.ui.player.full

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.audiomack.R
import com.audiomack.data.imageloader.ImageLoaderCallback
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItem.ItemImagePreset.ItemImagePresetOriginal
import com.audiomack.utils.featArtists
import com.audiomack.utils.isValidUrl
import timber.log.Timber

private const val TAG = "PlayerSongPagerAdapter"

class PlayerSongPagerAdapter(
    private val songs: List<AMResultItem>,
    private val onArtistClick: (String) -> Unit,
    private val onArtworkClick: (String) -> Unit
) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        Timber.tag(TAG).d("instantiateItem with ${songs[position]}")
        val context = container.context
        val song = songs[position]

        val view = LayoutInflater.from(context).inflate(R.layout.page_song, container, false)
        container.addView(view)
        return view.apply {
            findViewById<TextView>(R.id.songArtistView)?.let { artistView ->
                song.artist?.let { artist ->
                    artistView.text = artist
                    artistView.setOnClickListener { onArtistClick(artist) }
                }
            }
            findViewById<TextView>(R.id.songTitleView)?.text = song.title
            findViewById<TextView>(R.id.songFeatView)?.apply {
                movementMethod = LinkMovementMethod.getInstance()
                visibility = song.featured?.let { featured ->
                    text = TextUtils.concat(
                        context.getText(R.string.feat),
                        SpannableString(" "),
                        getFeaturedString(featured)
                    )
                    if (featured.isBlank()) View.GONE else View.VISIBLE
                } ?: View.GONE
            }
            findViewById<ImageView>(R.id.songArtView)?.let { imageView ->
                val url = song.getImageURLWithPreset(ItemImagePresetOriginal)
                PicassoImageLoader.load(
                    imageView.context,
                    url,
                    R.drawable.ic_artwork,
                    callback = object : ImageLoaderCallback {
                        override fun onBitmapLoaded(bitmap: Bitmap?) {
                            imageView.apply {
                                bitmap?.let { setImageBitmap(it) }
                                setOnClickListener {
                                    url.takeIf { it.isValidUrl() }?.let { onArtworkClick(it) }
                                }
                            }
                        }

                        override fun onBitmapFailed(errorDrawable: Drawable?) {
                            imageView.apply {
                                errorDrawable?.let { setImageDrawable(errorDrawable) }
                                setOnClickListener(null)
                            }
                        }
                    })
            }
            findViewById<ImageView>(R.id.albumLinesView)?.apply {
                visibility = if (song.album.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
        container.removeView(view as View)
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view == obj
    }

    override fun getCount() = songs.size

    private fun getFeaturedString(featured: String): SpannableStringBuilder {
        return SpannableStringBuilder(featured).apply {
            featured.featArtists.forEach { artist ->
                val span = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onArtistClick(artist)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                    }
                }
                val startIndex = featured.indexOf(artist)
                val endIndex = startIndex + artist.length

                setSpan(span, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
