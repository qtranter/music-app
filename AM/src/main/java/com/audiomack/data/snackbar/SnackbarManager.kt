package com.audiomack.data.snackbar

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import com.google.android.material.snackbar.Snackbar

class SnackbarManager {

    private var queueSnackbar: ArrayList<Snackbar> = arrayListOf()
    private var currentSnackbar: Snackbar? = null

    @SuppressLint("ClickableViewAccessibility")
    fun show(snackbar: Snackbar?) {

        snackbar?.let {
            queueSnackbar.add(snackbar)
        }

        if (currentSnackbar == null) {

            val nextSnackBar = queueSnackbar.first()
            nextSnackBar.show()

            val gesture = GestureDetector(nextSnackBar.context,
                object : GestureDetector.SimpleOnGestureListener() {

                    override fun onDown(e: MotionEvent?): Boolean = true

                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent?,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        nextSnackBar.dismiss()
                        dismiss(nextSnackBar)
                        return super.onFling(e1, e2, velocityX, velocityY)
                    }
                })

            val snackBarLayout = nextSnackBar.view as Snackbar.SnackbarLayout
            val layout = snackBarLayout.getChildAt(0)

            layout.setOnTouchListener { _, event ->
                event?.let { gesture.onTouchEvent(it) }
                false
            }

            nextSnackBar.addCallback(SnackbarCallback())
        }
    }

    private fun dismiss(snackbar: Snackbar) {
        queueSnackbar.remove(snackbar)
        currentSnackbar = null
        if (queueSnackbar.isNotEmpty()) {
            show(null)
        }
    }

    private inner class SnackbarCallback : Snackbar.Callback() {
        override fun onDismissed(snackbar: Snackbar?, event: Int) {
            if (snackbar == null) return
            super.onDismissed(snackbar, event)
            snackbar.removeCallback(this)
            dismiss(snackbar)
        }

        override fun onShown(snackbar: Snackbar?) {
            if (snackbar == null) return
            super.onShown(snackbar)
            currentSnackbar = snackbar
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SnackbarManager? = null

        fun getInstance(): SnackbarManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: SnackbarManager().also { INSTANCE = it }
        }
    }
}
