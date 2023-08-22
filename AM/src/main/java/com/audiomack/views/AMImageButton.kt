package com.audiomack.views

import android.content.Context
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton

class AMImageButton : AppCompatImageButton {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun setImageDrawable(drawable: Drawable?) {
        if (drawable != null) {
            val layer = OverlayButtonBackgroundDrawable(drawable)
            super.setImageDrawable(layer)
        } else {
            super.setImageDrawable(drawable)
        }
    }

    // Adds a dark overlay on the pressed state
    private inner class OverlayButtonBackgroundDrawable constructor(d: Drawable) :
        LayerDrawable(arrayOf(d)) {

        private val _pressedFilter = LightingColorFilter(Color.GRAY, 1)
        private val _disabledAlpha = 100

        override fun onStateChange(states: IntArray): Boolean {
            var enabled = false
            var pressed = false

            for (state in states) {
                if (state == android.R.attr.state_enabled)
                    enabled = true
                else if (state == android.R.attr.state_pressed)
                    pressed = true
            }

            mutate()
            if (enabled && pressed) {
                colorFilter = _pressedFilter
            } else if (!enabled) {
                colorFilter = null
                alpha = _disabledAlpha
            } else {
                colorFilter = null
                alpha = 255
            }

            invalidateSelf()

            return super.onStateChange(states)
        }

        override fun isStateful(): Boolean {
            return true
        }
    }
}
