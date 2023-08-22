package com.audiomack.utils

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class SwipeDetector(
    viewWidth: Int,
    viewHeight: Int,
    private val horizontalSwipeListener: HorizontalSwipeListener?,
    private val verticalSwipeListener: VerticalSwipeListener?,
    private val dragListener: DragListener?,
    private val clickListener: ClickListener?
) : View.OnTouchListener {

    var startY: Float = 0F

    private val horizontalSwipeMinDistance: Int = (viewWidth.toFloat() * 0.15f).toInt()
    private val verticalSwipeMinDistance: Int = (viewHeight.toFloat() * 0.15f).toInt()
    private val maxClickDuration: Int = 200
    private val maxClickDistance: Int = 20
    private var clickStartTime: Long = 0
    private var startX: Float = 0F
    private var downX: Float = 0F
    private var downY: Float = 0F
    private var mSwipeDetected = Action.None

    enum class Action {
        LR, // Left to right
        RL, // Right to left
        DW, // Downwards
        UW, // Upwards
        None // Action not found
    }

    interface HorizontalSwipeListener {
        fun didSwipeToPrev(): Boolean
        fun didSwipeToNext(): Boolean
    }

    interface VerticalSwipeListener {
        fun didSwipeUpwards(): Boolean
        fun didSwipeDownwards(): Boolean
    }

    interface DragListener {
        fun onDragStart(view: View, startX: Float, startY: Float): Boolean
        fun onDrag(view: View, rawX: Float, rawY: Float, startX: Float, startY: Float): Boolean
        fun onDragEnd(view: View, endX: Float, endY: Float, startX: Float, startY: Float): Boolean
    }

    interface ClickListener {
        fun onClickDetected()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                downX = event.x
                downY = event.y
                mSwipeDetected = Action.None

                clickListener?.let {
                    clickStartTime = System.currentTimeMillis()
                }

                dragListener?.let {
                    if (it.onDragStart(v, startX, startY)) {
                        return true
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                dragListener?.let {
                    val rawX = event.rawX
                    val rawY = event.rawY

                    if (it.onDrag(v, rawX, rawY, startX, startY)) {
                        return true
                    }
                }

                val deltaX = downX - event.x
                val deltaY = downY - event.y
                // horizontal swipe detection
                if (abs(deltaX) > horizontalSwipeMinDistance) {
                    mSwipeDetected = if (deltaX < 0) Action.LR else Action.RL
                } else if (abs(deltaY) > verticalSwipeMinDistance) {
                    mSwipeDetected = if (deltaY < 0) Action.DW else Action.UW
                }
            }

            MotionEvent.ACTION_UP -> {

                clickListener?.let {

                    val rawX = event.rawX
                    val rawY = event.rawY

                    val clicktTimingOk = (System.currentTimeMillis() - clickStartTime) < maxClickDuration
                    val clickDistanceOk = sqrt((startX - rawX).toDouble().pow(2.toDouble()) + (startY - rawY).toDouble().pow(2.toDouble())) < maxClickDistance

                    if (clicktTimingOk && clickDistanceOk) {
                        it.onClickDetected()
                        return true
                    }
                }

                dragListener?.let {

                    val rawX = event.rawX
                    val rawY = event.rawY

                    val consumed = it.onDragEnd(v, rawX, rawY, startX, startY)

                    startX = 0F
                    startY = 0F

                    if (consumed) {
                        return true
                    }
                }

                horizontalSwipeListener?.let {
                    if ((mSwipeDetected == Action.RL && it.didSwipeToNext()) || (mSwipeDetected == Action.LR && it.didSwipeToPrev())) {
                        return true
                    }
                }

                verticalSwipeListener?.let {
                    if ((mSwipeDetected == Action.UW && it.didSwipeUpwards()) || (mSwipeDetected == Action.DW && it.didSwipeDownwards())) {
                        return true
                    }
                }
            }
        }

        return false
    }
}
