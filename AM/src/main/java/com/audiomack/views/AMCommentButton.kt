package com.audiomack.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.audiomack.DISABLED_ALPHA
import com.audiomack.R

class AMCommentButton constructor (
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var tvBadge: AMCustomFontTextView

    init {
        View.inflate(context, R.layout.view_commentbutton, this)
        tvBadge = findViewById(R.id.tvCommentBadge)
    }

    var commentsCount: Int = 0
        set(value) {
            field = value
            updateView()
        }

    private fun updateView() {
        tvBadge.visibility = if (commentsCount <= 0) View.GONE else View.VISIBLE
        tvBadge.text = if (commentsCount > 99) "99+" else commentsCount.toString()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else DISABLED_ALPHA
    }
}
