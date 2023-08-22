package com.audiomack.ui.player.full.view

import android.view.View
import androidx.fragment.app.Fragment
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.player.maxi.PlayerDragDirection
import com.audiomack.utils.SwipeDetector.DragListener
import kotlin.math.abs
import kotlin.math.roundToInt

open class DragFragment : Fragment(), DragListener {

    private var isAlreadyDraggingDown = false

    override fun onDragStart(view: View, startX: Float, startY: Float) = false

    override fun onDrag(
        view: View,
        rawX: Float,
        rawY: Float,
        startX: Float,
        startY: Float
    ): Boolean {
        val deltaY = (rawY - startY).roundToInt()

        if (abs(deltaY) <= MIN_DRAG_DISTANCE && !isAlreadyDraggingDown) {
            return false
        }

        isAlreadyDraggingDown = true
        (activity as? HomeActivity)?.dragPlayer(deltaY, PlayerDragDirection.DOWN)

        return true
    }

    override fun onDragEnd(
        view: View,
        endX: Float,
        endY: Float,
        startX: Float,
        startY: Float
    ): Boolean {
        val draggingDown = startY < endY
        val deltaY = (endY + startY).toInt()
        val distance = abs(deltaY)
        val minDistance = view.height / 3

        if (!isAlreadyDraggingDown) {
            return false
        }

        isAlreadyDraggingDown = false

        val activity = activity as? HomeActivity ?: return false

        if (draggingDown) {
            if (distance > minDistance && activity.isPlayerMaximized()) {
                activity.playerViewModel.onMinimizeClick()
                return true
            }
        }

        activity.resetPlayerDrag(250 * distance / minDistance, PlayerDragDirection.DOWN)

        return false
    }

    companion object {
        private const val MIN_DRAG_DISTANCE = 10
    }
}
