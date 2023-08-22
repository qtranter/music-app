package com.audiomack.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.airbnb.lottie.LottieAnimationView

class ProgressLogoView : LottieAnimationView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun show() {
        visibility = View.VISIBLE
        super.playAnimation()
    }

    fun hide() {
        super.pauseAnimation()
        visibility = View.GONE
    }
}
