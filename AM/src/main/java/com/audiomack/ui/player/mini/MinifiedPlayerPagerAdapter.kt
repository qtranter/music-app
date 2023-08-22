package com.audiomack.ui.player.mini

import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.audiomack.R
import com.audiomack.model.AMResultItem
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString
import timber.log.Timber

class MinifiedPlayerPagerAdapter(val songs: List<AMResultItem>) : PagerAdapter() {

    override fun getCount(): Int {
        return songs.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        try {
            val view = LayoutInflater.from(container.context)
                .inflate(R.layout.page_minifiedplayer, container, false)

            val viewHolder = MinifiedPlayerPageViewHolder(view)

            val song = songs[position]

            val featString = if (TextUtils.isEmpty(song.featured)) "" else String.format(
                "  %s %s",
                viewHolder.tvArtist.resources.getString(R.string.feat_inline),
                song.featured
            )
            val fullString = String.format("%s%s", song.title, featString)
            val titleString = viewHolder.tvArtist.context.spannableString(
                fullString = fullString,
                highlightedStrings = listOf(featString),
                fullColor = Color.WHITE,
                highlightedColor = viewHolder.tvArtist.context.colorCompat(R.color.orange),
                fullFont = R.font.opensans_bold,
                highlightedFont = R.font.opensans_semibold,
                highlightedSize = 13
            )
            viewHolder.tvTitle.text = titleString
            viewHolder.tvArtist.text = song.artist
            viewHolder.tvTitle.visibility = View.VISIBLE
            viewHolder.tvArtist.visibility = View.VISIBLE
            viewHolder.tvPlaceholder.visibility = View.GONE

            viewHolder.tvPlaceholder.text = when (position) {
                0 -> viewHolder.tvPlaceholder.resources.getString(R.string.miniplayer_prev_song)
                2 -> viewHolder.tvPlaceholder.resources.getString(R.string.miniplayer_next_song)
                else -> null
            }

            container.addView(view)
            return view
        } catch (e: Exception) {
            Timber.w(e)
            return View(container.context)
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
        container.removeView(view as View)
    }
}
