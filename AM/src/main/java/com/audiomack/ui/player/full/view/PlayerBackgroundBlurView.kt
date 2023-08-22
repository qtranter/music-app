package com.audiomack.ui.player.full.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff.Mode.SRC_OVER
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.audiomack.R
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import jp.wasabeef.picasso.transformations.BlurTransformation
import kotlin.math.abs

class PlayerBackgroundBlurView(context: Context, attrs: AttributeSet?) :
    ViewPager(context, attrs) {

    var imageUrls: List<String> = ArrayList()
        set(value) {
            field = value
            adapter = Adapter(value)
        }

    /**
     * Limit MotionEvents to those started with the connected ViewPager
     */
    var inChainedTouchEvent = false

    init {
        if (!isInEditMode) {
            offscreenPageLimit = 2
            setPageTransformer(false, FadeTransformer())
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun connectTo(viewPager: ViewPager) {
        viewPager.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    SCROLL_STATE_SETTLING -> inChainedTouchEvent = false
                }
            }

            override fun onPageSelected(position: Int) {
                setCurrentItem(position, true)
            }
        })
        viewPager.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> inChainedTouchEvent = true
            }
            onTouchEvent(event)
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (!inChainedTouchEvent) return false
        return try {
            super.onTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private class BlurImageView(context: Context) : AppCompatImageView(context) {

        val colorOverlay = ResourcesCompat.getColor(resources, R.color.black_alpha30, null)

        init {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            scaleType = ScaleType.CENTER_CROP
        }

        var url: String? = null
            set(value) {
                if (!value.isNullOrEmpty()) {
                    Picasso.get()
                        .load(value)
                        .transform(BlurTransformation(context, 60, 2))
                        .transform(OverlayTransformation(colorOverlay))
                        .into(this)
                } else {
                    setImageDrawable(null)
                }
                field = value
            }

        fun cancel() {
            Picasso.get().cancelRequest(this)
        }

        override fun onDraw(canvas: Canvas?) {
            canvas?.apply {
                save()
                scale(1.15f, 1.15f, width / 2.0f, 0f)
                restore()
            }
            super.onDraw(canvas)
        }
    }

    private class Adapter(private val urls: List<String>) : PagerAdapter() {
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val view = BlurImageView(container.context).apply { url = urls[position] }
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
            (view as BlurImageView).let {
                it.cancel()
                container.removeView(it)
            }
        }

        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view == obj
        }

        override fun getCount() = urls.size
    }

    private class FadeTransformer : PageTransformer {
        override fun transformPage(view: View, position: Float) {
            view.translationX = -position * view.width
            view.alpha = 1 - abs(position)
        }
    }

    class OverlayTransformation(private val mColor: Int) : Transformation {
        private val paint = Paint().apply {
            isAntiAlias = true
            colorFilter = PorterDuffColorFilter(mColor, SRC_OVER)
        }

        override fun transform(source: Bitmap): Bitmap {
            val bitmap = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            canvas.drawBitmap(source, 0f, 0f, paint)
            source.recycle()

            return bitmap
        }

        override fun key(): String {
            return "OverlayTransformation(color=$mColor)"
        }
    }
}
