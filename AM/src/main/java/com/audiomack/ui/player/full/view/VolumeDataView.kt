package com.audiomack.ui.player.full.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import androidx.core.animation.addListener
import androidx.core.content.res.ResourcesCompat
import com.audiomack.R
import kotlin.math.ceil
import kotlin.math.floor

class VolumeDataView(context: Context?, attrs: AttributeSet?) : SeekBar(context, attrs) {

    private var density = resources.displayMetrics.density

    private var rawData = intArrayOf()
    private var volumeData = intArrayOf()

    private var maxBars = 0
    private var maxAmplitude = MAX_AMPLITUDE
    private var isProcessed: Boolean = false

    private var animatedValue = 0f
    private val valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = ANIM_DURATION
        addUpdateListener { animation ->
            this@VolumeDataView.animatedValue = animation.animatedValue as Float
            invalidate()
        }
    }
    private val fadeAnimator = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f).apply {
        duration = ANIM_DURATION
    }
    private var animatorSet: AnimatorSet? = null

    private var inactivePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ResourcesCompat.getColor(resources, R.color.audiowave_grey_alpha20, null)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = density * LINE_STROKE_POINTS
    }

    private var activePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ResourcesCompat.getColor(resources, R.color.orange, null)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = density * LINE_STROKE_POINTS
    }

    init {
        thumb = null
        progressDrawable = null
        background = null
        splitTrack = false

        if (isInEditMode) progress = 63
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        animatorSet?.cancel()
    }

    fun setVolumeData(data: IntArray) {
        rawData = data
        isProcessed = false
        progress = 0

        if (isAttachedToWindow && !isInLayout) {
            requestLayout()

            animatorSet = AnimatorSet().apply {
                playTogether(valueAnimator, fadeAnimator)
                start()
            }
        }
    }

    fun clear() {
        if (alpha == 1f) {
            ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0f).apply {
                duration = 150L
                addListener {
                    setVolumeData(IntArray(0))
                }
                start()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val minWidth: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val width: Int = resolveSizeAndState(minWidth, widthMeasureSpec, 1)

        val minHeight: Int = width / 6
        val height: Int = resolveSizeAndState(minHeight, heightMeasureSpec, 1)

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        maxBars = floor(w / density / ONE_SECOND_TO_POINTS).toInt()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (!isProcessed) {
            processVolumeData()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        volumeData.forEachIndexed { i, data ->
            val barX = i * ONE_SECOND_TO_POINTS * density + LINE_STROKE_POINTS / 2 * density
            val active = volumeData.size * (progress.toFloat() / max) > i
            canvas?.drawLine(
                barX,
                height - (height * (data.toFloat() / maxAmplitude) * animatedValue),
                barX,
                height.toFloat(),
                if (active) activePaint else inactivePaint
            )
        }
    }

    private fun processVolumeData() {
        val data = if (rawData.isEmpty()) generateRandomData() else rawData
        volumeData = getInterpolatedData(data)
        maxAmplitude = volumeData.max() ?: Int.MAX_VALUE
        isProcessed = true
    }

    private fun generateRandomData(): IntArray {
        val data = IntArray(maxBars)
        for (i in 0 until maxBars) {
            data[i] = MIN_AMPLITUDE + (Math.random() * (MAX_AMPLITUDE - MIN_AMPLITUDE)).toInt()
        }
        return data
    }

    private fun getInterpolatedData(data: IntArray): IntArray {
        if (maxBars == 0 || data.isEmpty()) {
            return data
        }
        val newData = IntArray(maxBars)
        val springFactor = (data.size - 1).toFloat() / (maxBars - 1).toFloat()

        newData[0] = data[0]

        for (i in 1 until maxBars - 1) {
            val tmp = i * springFactor
            val before = floor(tmp.toDouble()).toInt()
            val after = ceil(tmp.toDouble()).toInt()
            val current = (tmp - before).toInt()
            newData[i] = getLinearInterpolatedPoint(data[before], data[after], current)
        }
        newData[maxBars - 1] = data[data.size - 1]
        return newData
    }

    private fun getLinearInterpolatedPoint(before: Int, after: Int, current: Int): Int {
        return before + (after - before) * current
    }

    companion object {
        const val ONE_SECOND_TO_POINTS = 3
        const val LINE_STROKE_POINTS = 2

        const val MIN_AMPLITUDE = 20
        const val MAX_AMPLITUDE = 60

        const val ANIM_DURATION = 350L
    }
}
