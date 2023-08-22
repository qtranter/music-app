package com.audiomack.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import com.audiomack.R

class AMCustomFontButton : AppCompatButton {

    private var customLetterspacing: Float = 0.toFloat()

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context) : super(context)

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.CustomFont)
            customLetterspacing = a.getFloat(R.styleable.CustomFont_customletterspacing, 0f)
            a.recycle()
        }
        applyLetterSpacing()
    }

    private fun applyLetterSpacing() {
        if (customLetterspacing != 0f) {
            letterSpacing = customLetterspacing / (textSize / resources.displayMetrics.density)
        }
    }
}
