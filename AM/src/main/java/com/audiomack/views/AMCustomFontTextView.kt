package com.audiomack.views

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.text.Layout.HYPHENATION_FREQUENCY_NONE
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.audiomack.R
import com.audiomack.utils.extensions.colorCompat

open class AMCustomFontTextView : AppCompatTextView {

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hyphenationFrequency = HYPHENATION_FREQUENCY_NONE
        }
        applyLetterSpacing()
    }

    private fun applyLetterSpacing() {
        if (customLetterspacing != 0f) {
            letterSpacing = customLetterspacing / (textSize / resources.displayMetrics.density)
        }
    }

    fun applyGradient() {
        this.paint.shader = LinearGradient(
            0f,
            this.height.toFloat() * 1.5f,
            this.width.toFloat() * 1.1f,
            this.height.toFloat() * 0.1f,
            context.colorCompat(R.color.benchmark_yellow),
            context.colorCompat(R.color.benchmark_orange),
            Shader.TileMode.REPEAT
        )
    }
}
