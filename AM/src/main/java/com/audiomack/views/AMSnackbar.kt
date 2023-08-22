package com.audiomack.views

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.view.isVisible
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.snackbar.SnackbarManager
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.getStatusBarHeight
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_FADE
import com.google.android.material.snackbar.Snackbar
import de.hdodenhof.circleimageview.CircleImageView
import timber.log.Timber

class AMSnackbar {

    private var activity: Activity? = null
    @DrawableRes
    private var drawableResId: Int? = null
    @ColorRes
    private var tintColor: Int? = null
    @DrawableRes
    private var secondaryResId: Int? = null
    private var spannableTitle: SpannableString? = null
    private var subtitle: String? = null
    private var imageUrl: String? = null
    private var imageClickListener: View.OnClickListener? = null
    private var duration = Snackbar.LENGTH_LONG

    fun show() {

        val activity = activity ?: run { return }

        Handler(activity.mainLooper).post {

            try {

                val layout = LayoutInflater.from(activity).inflate(R.layout.snackbar, null)
                val ivIcon = layout.findViewById<ImageView>(R.id.ivIcon)
                val ivPhoto = layout.findViewById<CircleImageView>(R.id.ivPhoto)
                val ivArtwork = layout.findViewById<ImageView>(R.id.ivSecondary)
                val tvTitle = layout.findViewById<AMCustomFontTextView>(R.id.tvTitle)
                val tvSubtitle = layout.findViewById<TextView>(R.id.tvSubtitle)

                drawableResId?.takeIf {
                    it > 0
                }?.let {
                    ivIcon.apply {
                        setImageResource(it)
                        tintColor?.let { setColorFilter(getColor(resources, it, null)) }
                        visibility = View.VISIBLE
                    }
                    tvTitle.gravity = Gravity.START
                    tvSubtitle.gravity = Gravity.START
                } ?: run {
                    ivIcon.visibility = View.GONE
                    tvTitle.gravity = Gravity.CENTER
                    tvSubtitle.gravity = Gravity.CENTER
                }

                subtitle?.takeIf {
                    it.isNotEmpty()
                }?.let {
                    tvSubtitle.text = it
                    tvSubtitle.visibility = View.VISIBLE
                } ?: run {
                    tvSubtitle.visibility = View.GONE
                }

                secondaryResId?.takeIf {
                    it > 0
                }?.let {
                    ivArtwork.setImageResource(it)
                    ivArtwork.visibility = View.VISIBLE
                } ?: run {
                    ivArtwork.visibility = View.GONE
                }

                imageUrl?.takeIf {
                    it.isNotEmpty()
                }?.let {
                    PicassoImageLoader.load(ivPhoto.context, it, ivPhoto)
                    ivPhoto.visibility = View.VISIBLE
                    tvTitle.gravity = Gravity.START
                    tvSubtitle.gravity = Gravity.START
                }

                spannableTitle?.takeIf {
                    it.isNotEmpty()
                }?.let {
                    tvTitle.text = it
                    tvTitle.visibility = View.VISIBLE
                } ?: run {
                    tvTitle.visibility = View.GONE
                }

                imageClickListener?.let {
                    if (ivIcon.isVisible) ivIcon.setOnClickListener(it)
                    else if (ivPhoto.isVisible) ivPhoto.setOnClickListener(it)
                    try {
                        tvTitle.movementMethod = LinkMovementMethod()
                    } catch (e: NoSuchMethodError) {
                        Timber.w(e)
                    }
                }

                val statusBarHeight: Int = with(activity) {
                    getStatusBarHeight() + convertDpToPixel(5f)
                }

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.setMargins(0, statusBarHeight, 0, 0)
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL

                val snackBar = Snackbar.make(activity.window.decorView.rootView, "", duration)
                snackBar.animationMode = ANIMATION_MODE_FADE
                val snackBarLayout = snackBar.view as Snackbar.SnackbarLayout
                snackBarLayout.layoutParams = layoutParams

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                    val sidePadding: Int = activity.convertDpToPixel(10f)
                    snackBarLayout.setPadding(sidePadding, statusBarHeight, sidePadding, 0)
                }
                snackBarLayout.addView(layout, 0)
                snackBarLayout.setBackgroundColor(activity.colorCompat(R.color.transparent))

                SnackbarManager.getInstance().show(snackBar)
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }

    class Builder(private val activity: Activity?) {
        @DrawableRes
        private var drawableResId: Int? = null
        @ColorRes
        private var tintColor: Int? = null
        @DrawableRes
        private var secondaryResId: Int? = null
        private var spannableTitle: SpannableString? = null
        private var subtitle: String? = null
        private var imageUrl: String? = null
        private var duration: Int = Snackbar.LENGTH_LONG
        private var imageClickListener: View.OnClickListener? = null

        fun withSpannableTitle(spannableTitle: SpannableString): Builder {
            this.spannableTitle = spannableTitle
            return this
        }

        fun withImageClickListener(imageClickListener: View.OnClickListener): Builder {
            this.imageClickListener = imageClickListener
            return this
        }

        fun withImageUrl(imageUrl: String): Builder {
            this.imageUrl = imageUrl
            return this
        }

        @JvmOverloads
        fun withDrawable(@DrawableRes drawableResId: Int, @ColorRes tintColor: Int? = null): Builder {
            this.drawableResId = drawableResId
            this.tintColor = tintColor
            return this
        }

        fun withSecondary(@DrawableRes secondaryResId: Int): Builder {
            this.secondaryResId = secondaryResId
            return this
        }

        fun withTitle(title: String): Builder {
            this.spannableTitle = SpannableString(title)
            return this
        }

        fun withTitle(@StringRes titleRes: Int) = this.apply {
            spannableTitle = SpannableString(activity?.getString(titleRes))
        }

        fun withSubtitle(subtitle: String): Builder {
            this.subtitle = subtitle
            return this
        }

        fun withSubtitle(@StringRes subtitleRes: Int) = this.apply {
            subtitle = activity?.getString(subtitleRes)
        }

        fun withDuration(duration: Int): Builder {
            this.duration = duration
            return this
        }

        fun build(): AMSnackbar {
            val toast = AMSnackbar()
            toast.activity = activity
            toast.drawableResId = drawableResId
            toast.tintColor = tintColor
            toast.secondaryResId = secondaryResId
            toast.spannableTitle = spannableTitle
            toast.subtitle = subtitle
            toast.imageUrl = imageUrl
            toast.duration = duration
            toast.imageClickListener = imageClickListener
            return toast
        }

        fun show() {
            build().show()
        }
    }
}
