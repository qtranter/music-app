package com.audiomack.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import com.audiomack.R

class AMCustomSwitch : SwitchCompat {

    private var customLetterspacing: Float = 0.toFloat()

    private var checkedChangeListener: OnCheckedChangeListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

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

    override fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        if (checkedChangeListener == null) {
            checkedChangeListener = listener
        }
        super.setOnCheckedChangeListener(listener)
    }

    /**
     * Call this instead of [isChecked] to prevent the [OnCheckedChangeListener] from being notified
     */
    fun setCheckedProgrammatically(checked: Boolean) {
        super.setOnCheckedChangeListener(null)
        super.setChecked(checked)
        super.setOnCheckedChangeListener(checkedChangeListener)
    }
}
