package com.audiomack.utils

import android.content.Context
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.audiomack.R
import com.audiomack.utils.extensions.colorCompat

/**
 * Prefer this over [ClickableSpan] since it takes care of removing underlines and applying the app tint color.
 * @param context: used to get resources
 * @param action: a block that must be executed on onClick
 *  **/
class AMClickableSpan(val context: Context, val action: () -> Unit) : ClickableSpan() {
    override fun onClick(widget: View) {
        action()
        widget.invalidate()
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = false
        ds.color = context.colorCompat(R.color.orange)
    }
}
