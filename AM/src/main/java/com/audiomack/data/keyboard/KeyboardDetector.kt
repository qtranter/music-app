package com.audiomack.data.keyboard

import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlin.math.roundToInt

class KeyboardDetector(
    private val view: View,
    private val callback: (state: KeyboardState) -> Unit
) : LifecycleObserver {

    data class KeyboardState(
        val open: Boolean,
        val keyboardHeightPx: Int
    )

    private val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
        private var lastState: KeyboardState = view.isKeyboardOpen()

        override fun onGlobalLayout() {
            val state = view.isKeyboardOpen()
            if (state.open == lastState.open) {
                return
            } else {
                callback(state)
                lastState = state
            }
        }
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_RESUME)
    fun start() {
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_PAUSE)
    fun stop() {
        view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }

    private fun View.isKeyboardOpen(): KeyboardState {
        val visibleBounds = Rect()
        getWindowVisibleDisplayFrame(visibleBounds)
        val heightDiff = height - (visibleBounds.bottom - visibleBounds.top)
        val marginOfError = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100F, resources.displayMetrics).roundToInt()
        val open = heightDiff > marginOfError
        val height = if (open) heightDiff else 0
        return KeyboardState(open, height)
    }
}
