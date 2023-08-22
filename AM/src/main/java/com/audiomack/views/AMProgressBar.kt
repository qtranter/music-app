package com.audiomack.views

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ProgressBar
import androidx.annotation.ColorRes
import com.audiomack.R
import com.audiomack.data.device.DeviceRepository
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat

class AMProgressBar : ProgressBar {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isEnabled) {
            indeterminateDrawable?.setColorFilter(
                context.colorCompat(R.color.orange),
                PorterDuff.Mode.SRC_IN
            )
        }
    }

    override fun setIndeterminateDrawable(d: Drawable) {
        var drawable: Drawable? = d
        if (DeviceRepository.runningEspressoTest) {
            drawable = context.drawableCompat(R.drawable.ic_ad_close)
        }
        super.setIndeterminateDrawable(drawable)
    }

    fun applyColor(@ColorRes colorResId: Int) {
        indeterminateDrawable?.setColorFilter(
            context.colorCompat(colorResId),
            PorterDuff.Mode.SRC_IN
        )
    }
}
