package com.audiomack.ui.picker

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker

class AMCustomNumberPicker : NumberPicker {

    private var customLetterspacing: Float = 0.toFloat()

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    override fun addView(child: View?, index: Int) {
        super.addView(child, index)
        updateView(child)
    }

    override fun addView(
        child: View,
        index: Int,
        params: ViewGroup.LayoutParams?
    ) {
        super.addView(child, index, params)
        updateView(child)
    }

    private fun updateView(view: View?) {
        val textSpacing = customLetterspacing
        view?.let {
            (it as? EditText)?.let { editText ->
                editText.letterSpacing = textSpacing / (editText.textSize / resources.displayMetrics.density)
            }
        }
    }
}
