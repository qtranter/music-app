package com.audiomack.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import com.audiomack.utils.SwipeDetector
import com.audiomack.utils.SwipeDetector.DragListener

class SwipeNestedScrollView(
    context: Context,
    attrs: AttributeSet?
) : NestedScrollView(context, attrs) {

    var dragListener: DragListener? = null

    private var swipeDetector: SwipeDetector? = null

    private var inOverScroll = false

    init {
        overScrollMode = View.OVER_SCROLL_ALWAYS
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (!isInEditMode) {
            (layoutParams as CoordinatorLayout.LayoutParams).behavior = Behavior()
        }

        dragListener?.let {
            swipeDetector = SwipeDetector(w, h, null, null, dragListener, null)
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) swipeDetector?.startY = event.rawY
        return super.onInterceptTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (inOverScroll) swipeDetector?.onTouch(this, event)
            ?: false else super.onTouchEvent(event)
    }

    fun showScrollBars() {
        isScrollbarFadingEnabled = false
        postInvalidate()
    }

    fun hideScrollBars() {
        isScrollbarFadingEnabled = true
        postInvalidate()
    }

    inner class Behavior : CoordinatorLayout.Behavior<View>() {
        override fun onStartNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: View,
            directTargetChild: View,
            target: View,
            axes: Int
        ) = true

        override fun onNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: View,
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int
        ) {
            if (dyUnconsumed == 0) return
            inOverScroll = true
        }

        override fun onStopNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: View,
            target: View
        ) {
            inOverScroll = false
        }
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "SwipeNestedScrollView"
    }
}
