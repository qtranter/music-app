package com.audiomack.views

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.use
import com.audiomack.R
import com.audiomack.utils.extensions.colorCompat

class AMNowPlayingImageView(context: Context, attrs: AttributeSet) :
    AppCompatImageView(context, attrs) {

    enum class Style {
        Orange, White
    }

    private var animDrawable: AnimationDrawable? = null

    init {
        val style = context.obtainStyledAttributes(attrs, R.styleable.AMNowPlayingImageView).use {
            it.getInt(R.styleable.AMNowPlayingImageView_amnpi_style, Style.Orange.ordinal)
        }

        if (style == Style.Orange.ordinal) {
            setImageResource(R.drawable.anim_playlist_track_playing)
            setBackgroundColor(context.colorCompat(R.color.black_alpha50))
        } else {
            setImageResource(R.drawable.anim_feed_playing)
            setBackgroundColor(context.colorCompat(R.color.orange))
        }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        animDrawable = drawable as AnimationDrawable
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == View.VISIBLE) {
            startAnimating()
        } else {
            stopAnimating()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimating()
    }

    private fun startAnimating() {
        animDrawable?.start()
    }

    private fun stopAnimating() {
        animDrawable?.stop()
    }
}
