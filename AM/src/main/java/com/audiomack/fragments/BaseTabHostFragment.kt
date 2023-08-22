package com.audiomack.fragments

import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.audiomack.views.AMCustomTabLayout

open class BaseTabHostFragment(logTag: String) : TrackedFragment(logTag) {

    var topLayout: View? = null
    var tabLayout: AMCustomTabLayout? = null

    open val topLayoutHeight: Int
        get() = 0

    open fun didScrollTo(verticalOffset: Int) {}

    fun topLayoutMargin(): Int {
        return when (topLayout?.parent) {
            is LinearLayout -> ((topLayout!!.parent as LinearLayout).layoutParams as FrameLayout.LayoutParams).topMargin
            is FrameLayout -> ((topLayout!!.parent as FrameLayout).layoutParams as FrameLayout.LayoutParams).topMargin
            else -> 0
        }
    }
}
