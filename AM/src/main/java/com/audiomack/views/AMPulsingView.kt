package com.audiomack.views

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import com.audiomack.R
import timber.log.Timber

class AMPulsingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var viewDot: View? = null
    private var viewRing: View? = null
    private var animatorSet: AnimatorSet? = null

    private val kDotScaleMin = 0.8f
    private val kDotScaleMax = 1.0f
    private val kDotAlphaMin = 0.16f
    private val kDotAlphaMax = 0.5f
    private val kRingScaleMin = 48f / 120f
    private val kRingScaleMax = 1.0f
    private val kRingAlphaMin = 0.0f
    private val kRingAlphaMax = 0.4f
    private val kDuration = 600
    private val kDurationSlower = 800

    init {
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layoutInflater.inflate(R.layout.view_pulse, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewDot = findViewById(R.id.viewDot)
        viewRing = findViewById(R.id.viewRing)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    private fun resetAnimation() {
        viewDot?.scaleX = kDotScaleMin
        viewDot?.scaleY = kDotScaleMin
        viewDot?.alpha = kDotAlphaMax
        viewRing?.scaleX = kRingScaleMin
        viewRing?.scaleY = kRingScaleMin
        viewRing?.alpha = kRingAlphaMin
    }

    @SuppressLint("ObjectAnimatorBinding")
    fun startAnimating() {

        if (viewDot == null || viewRing == null) {
            return
        }

        resetAnimation()

        val dotScaleXUp = ObjectAnimator.ofFloat(viewDot, "scaleX", kDotScaleMin, kDotScaleMax)
            .setDuration(kDuration.toLong())
        val dotScaleYUp = ObjectAnimator.ofFloat(viewDot, "scaleY", kDotScaleMin, kDotScaleMax)
            .setDuration(kDuration.toLong())
        val dotScaleXDown = ObjectAnimator.ofFloat(viewDot, "scaleX", kDotScaleMax, kDotScaleMin)
            .setDuration(kDuration.toLong())
        val dotScaleYDown = ObjectAnimator.ofFloat(viewDot, "scaleY", kDotScaleMax, kDotScaleMin)
            .setDuration(kDuration.toLong())
        val dotAlphaUp = ObjectAnimator.ofFloat(viewDot, "alpha", kDotAlphaMin, kDotAlphaMax)
            .setDuration(kDuration.toLong())
        val dotAlphaDown =
            ObjectAnimator.ofFloat(viewDot, "alpha", kDotAlphaMax, kDotAlphaMin).setDuration(0)
        val ringScaleXUp = ObjectAnimator.ofFloat(viewRing, "scaleX", kRingScaleMin, kRingScaleMax)
            .setDuration(kDurationSlower.toLong())
        ringScaleXUp.interpolator = DecelerateInterpolator()
        val ringScaleYUp = ObjectAnimator.ofFloat(viewRing, "scaleY", kRingScaleMin, kRingScaleMax)
            .setDuration(kDurationSlower.toLong())
        ringScaleYUp.interpolator = DecelerateInterpolator()
        val ringAlphaDown = ObjectAnimator.ofFloat(viewRing, "alpha", kRingAlphaMax, kRingAlphaMin)
            .setDuration(kDuration.toLong())

        animatorSet = AnimatorSet()
        animatorSet!!.play(dotScaleXUp).with(dotScaleYUp).with(ringScaleXUp).with(ringScaleYUp)
            .with(dotAlphaDown).with(dotAlphaUp).with(ringAlphaDown)
        animatorSet!!.play(dotScaleXDown).with(dotScaleYDown).after(dotScaleXUp)

        animatorSet?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                try {
                    animatorSet?.let {
                        resetAnimation()
                        it.start()
                    }
                } catch (e: Exception) {
                    Timber.w(e)
                }
            }

            override fun onAnimationCancel(animation: Animator) {
            }

            override fun onAnimationRepeat(animation: Animator) {
            }
        })

        val durationScale: Float = Settings.Global.getFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        if (durationScale > 0) { animatorSet?.start() }
    }

    private fun stop() {
        if (animatorSet?.isRunning == true) {
            animatorSet?.cancel()
            animatorSet = null
        }
    }
}
