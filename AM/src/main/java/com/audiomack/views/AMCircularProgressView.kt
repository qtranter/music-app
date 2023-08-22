package com.audiomack.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.audiomack.R
import com.audiomack.utils.extensions.colorCompat

class AMCircularProgressView : View {

    private var paint: Paint? = null
    private var size = 0
    private var bounds: RectF? = null

    private var isIndeterminate: Boolean = false
    private var autoStartAnimation: Boolean = false
    private var currentProgress: Float = 0.toFloat()
    private var maxProgress: Float = 0.toFloat()
    private var indeterminateSweep: Float = 0.toFloat()
    private var indeterminateRotateOffset: Float = 0.toFloat()
    private var thickness: Int = 0
    private var color: Int = 0
    private var animDuration: Int = 0
    private var animSwoopDuration: Int = 0
    private var animSyncDuration: Int = 0
    private var animSteps: Int = 0

    private var startAngle: Float = 0.toFloat()
    private var actualProgress: Float = 0.toFloat()
    private var startAngleRotate: ValueAnimator? = null
    private var progressAnimator: ValueAnimator? = null
    private var indeterminateAnimator: AnimatorSet? = null
    private var initialStartAngle: Float = 0.toFloat()

    // Reset the determinate animation to approach the new currentProgress
    var progress: Float
        get() = currentProgress
        set(currentProgress) {
            this.currentProgress = currentProgress
            if (!isIndeterminate) {
                if (progressAnimator != null && progressAnimator!!.isRunning) {
                    progressAnimator!!.cancel()
                }
                progressAnimator = ValueAnimator.ofFloat(actualProgress, currentProgress)
                progressAnimator?.duration = animSyncDuration.toLong()
                progressAnimator?.interpolator = LinearInterpolator()
                progressAnimator?.addUpdateListener { animation ->
                    actualProgress = animation.animatedValue as Float
                    invalidate()
                }
                progressAnimator?.start()
            }
            invalidate()
        }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        initAttributes(attrs, defStyle)

        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        updatePaint()

        bounds = RectF()
    }

    private fun initAttributes(attrs: AttributeSet?, defStyle: Int) {
        val a =
            context.obtainStyledAttributes(attrs, R.styleable.AMCircularProgressView, defStyle, 0)
        currentProgress = a.getFloat(R.styleable.AMCircularProgressView_amcpv_progress, 0f)
        maxProgress = a.getFloat(R.styleable.AMCircularProgressView_amcpv_maxProgress, 100f)
        thickness = a.getDimensionPixelSize(
            R.styleable.AMCircularProgressView_amcpv_thickness,
            (resources.displayMetrics.density * 2.50f).toInt()
        )
        isIndeterminate = a.getBoolean(R.styleable.AMCircularProgressView_amcpv_indeterminate, true)
        autoStartAnimation =
            a.getBoolean(R.styleable.AMCircularProgressView_amcpv_animAutostart, false)
        initialStartAngle = a.getFloat(R.styleable.AMCircularProgressView_amcpv_startAngle, 90f)
        startAngle = initialStartAngle
        color = a.getColor(
            R.styleable.AMCircularProgressView_amcpv_color,
            context.colorCompat(R.color.orange)
        )
        animDuration = a.getInteger(R.styleable.AMCircularProgressView_amcpv_animDuration, 6000)
        animSwoopDuration =
            a.getInteger(R.styleable.AMCircularProgressView_amcpv_animSwoopDuration, 8000)
        animSyncDuration =
            a.getInteger(R.styleable.AMCircularProgressView_amcpv_animSyncDuration, 4000)
        animSteps = a.getInteger(R.styleable.AMCircularProgressView_amcpv_animSteps, 5)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val xPad = paddingLeft + paddingRight
        val yPad = paddingTop + paddingBottom
        val width = measuredWidth - xPad
        val height = measuredHeight - yPad
        size = if (width < height) width else height
        setMeasuredDimension(size + xPad, size + yPad)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        size = if (w < h) w else h
        updateBounds()
    }

    private fun updateBounds() {
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        bounds?.set(
            (paddingLeft + thickness).toFloat(),
            (paddingTop + thickness).toFloat(),
            (size - paddingLeft - thickness).toFloat(),
            (size - paddingTop - thickness).toFloat()
        )
    }

    private fun updatePaint() {
        paint?.color = color
        paint?.style = Paint.Style.STROKE
        paint?.strokeWidth = thickness.toFloat()
        paint?.strokeCap = Paint.Cap.BUTT
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the arc
        val sweepAngle =
            if (isInEditMode) currentProgress / maxProgress * 360 else actualProgress / maxProgress * 360
        if (!isIndeterminate) {
            canvas.drawArc(bounds!!, startAngle, sweepAngle, false, paint!!)
        } else {
            canvas.drawArc(
                bounds!!,
                startAngle + indeterminateRotateOffset,
                indeterminateSweep,
                false,
                paint!!
            )
        }
    }

    fun getColor(): Int {
        return color
    }

    private fun startAnimation() {
        resetAnimation()
    }

    fun resetAnimation() {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        // Cancel all the old animators
        if (startAngleRotate != null && startAngleRotate!!.isRunning) {
            startAngleRotate!!.cancel()
        }
        if (progressAnimator != null && progressAnimator!!.isRunning) {
            progressAnimator!!.cancel()
        }
        if (indeterminateAnimator != null && indeterminateAnimator!!.isRunning) {
            indeterminateAnimator!!.cancel()
        }

        if (!isIndeterminate) {
            // The cool 360 swoop animation at the start of the animation
            startAngle = initialStartAngle
            startAngleRotate = ValueAnimator.ofFloat(startAngle, startAngle + 360)
            startAngleRotate?.duration = animSwoopDuration.toLong()
            startAngleRotate?.interpolator = DecelerateInterpolator(2f)
            startAngleRotate?.addUpdateListener { animation ->
                startAngle = animation.animatedValue as Float
                invalidate()
            }
            startAngleRotate?.start()

            // The linear animation shown when progress is updated
            actualProgress = 0f
            progressAnimator = ValueAnimator.ofFloat(actualProgress, currentProgress)
            progressAnimator?.duration = animSyncDuration.toLong()
            progressAnimator?.interpolator = LinearInterpolator()
            progressAnimator?.addUpdateListener { animation ->
                actualProgress = animation.animatedValue as Float
                invalidate()
            }
            progressAnimator?.start()
        } else {
            indeterminateSweep = INDETERMINANT_MIN_SWEEP
            // Build the whole AnimatorSet
            indeterminateAnimator = AnimatorSet()
            var prevSet: AnimatorSet? = null
            var nextSet: AnimatorSet
            for (k in 0 until animSteps) {
                nextSet = createIndeterminateAnimator(k.toFloat())
                val builder = indeterminateAnimator!!.play(nextSet)
                if (prevSet != null) {
                    builder.after(prevSet)
                }
                prevSet = nextSet
            }

            // Listen to end of animation so we can infinitely loop
            indeterminateAnimator?.addListener(object : AnimatorListenerAdapter() {
                var wasCancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    wasCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!wasCancelled) {
                        resetAnimation()
                    }
                }
            })
            indeterminateAnimator?.start()
        }
    }

    private fun stopAnimation() {
        startAngleRotate?.cancel()
        startAngleRotate = null

        progressAnimator?.cancel()
        progressAnimator = null

        indeterminateAnimator?.cancel()
        indeterminateAnimator = null

        setLayerType(LAYER_TYPE_NONE, null)
    }

    private fun createIndeterminateAnimator(step: Float): AnimatorSet {

        val maxSweep = 360f * (animSteps - 1) / animSteps + INDETERMINANT_MIN_SWEEP
        val start = -90f + step * (maxSweep - INDETERMINANT_MIN_SWEEP)

        // Extending the front of the arc
        val frontEndExtend = ValueAnimator.ofFloat(INDETERMINANT_MIN_SWEEP, maxSweep)
        frontEndExtend.duration = (animDuration / animSteps / 2).toLong()
        frontEndExtend.interpolator = DecelerateInterpolator(1f)
        frontEndExtend.addUpdateListener { animation ->
            indeterminateSweep = animation.animatedValue as Float
            invalidate()
        }

        // Overall rotation
        val rotateAnimator1 =
            ValueAnimator.ofFloat(step * 720f / animSteps, (step + .5f) * 720f / animSteps)
        rotateAnimator1.duration = (animDuration / animSteps / 2).toLong()
        rotateAnimator1.interpolator = LinearInterpolator()
        rotateAnimator1.addUpdateListener { animation ->
            indeterminateRotateOffset = animation.animatedValue as Float
        }

        // Followed by...

        // Retracting the back end of the arc
        val backEndRetract =
            ValueAnimator.ofFloat(start, start + maxSweep - INDETERMINANT_MIN_SWEEP)
        backEndRetract.duration = (animDuration / animSteps / 2).toLong()
        backEndRetract.interpolator = DecelerateInterpolator(1f)
        backEndRetract.addUpdateListener { animation ->
            startAngle = animation.animatedValue as Float
            indeterminateSweep = maxSweep - startAngle + start
            invalidate()
        }

        // More overall rotation
        val rotateAnimator2 =
            ValueAnimator.ofFloat((step + .5f) * 720f / animSteps, (step + 1) * 720f / animSteps)
        rotateAnimator2.duration = (animDuration / animSteps / 2).toLong()
        rotateAnimator2.interpolator = LinearInterpolator()
        rotateAnimator2.addUpdateListener { animation ->
            indeterminateRotateOffset = animation.animatedValue as Float
        }

        val set = AnimatorSet()
        set.play(frontEndExtend).with(rotateAnimator1)
        set.play(backEndRetract).with(rotateAnimator2).after(rotateAnimator1)
        return set
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (autoStartAnimation) {
            startAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun setVisibility(visibility: Int) {
        val currentVisibility = getVisibility()
        super.setVisibility(visibility)
        if (visibility != currentVisibility) {
            if (visibility == VISIBLE) {
                resetAnimation()
            } else if (visibility == GONE || visibility == INVISIBLE) {
                stopAnimation()
            }
        }
    }

    companion object {

        private const val INDETERMINANT_MIN_SWEEP = 15f
    }
}
