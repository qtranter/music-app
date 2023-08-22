package com.audiomack.ui.tooltip

import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.audiomack.R
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.views.AMPulsingView
import java.util.ArrayList
import kotlinx.android.synthetic.main.fragment_tooltip.buttonClose
import kotlinx.android.synthetic.main.fragment_tooltip.icon
import kotlinx.android.synthetic.main.fragment_tooltip.textContainer
import kotlinx.android.synthetic.main.fragment_tooltip.tvTitle
import kotlinx.android.synthetic.main.fragment_tooltip.viewCircle
import timber.log.Timber

class TooltipFragment : Fragment() {

    private var tooltipText: String? = null
    private var drawableResId: Int = 0
    private var corner: TooltipCorner? = null
    private var targetPoints: List<Point>? = null
    private var action: Runnable? = null

    private var closing: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_tooltip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnTouchListener { _, _ ->
            if (!closing) {
                closing = true
                close()
            }
            false
        }

        buttonClose.setOnClickListener { close() }

        tvTitle.text = tooltipText
        icon.setImageDrawable(view.context.drawableCompat(drawableResId))

        viewCircle.scaleX = 0f
        viewCircle.scaleY = 0f
        textContainer.alpha = 0f
        buttonClose.alpha = 0f

        viewCircle.animate().withLayer().scaleX(1f).scaleY(1f).setDuration(300).start()
        textContainer.animate().withLayer().alpha(1f).setDuration(150).setStartDelay(400).start()
        buttonClose.animate().withLayer().alpha(1f).setDuration(150).setStartDelay(400).start()

        val gravity = when (corner) {
            TooltipCorner.TOPLEFT -> Gravity.TOP or Gravity.LEFT or Gravity.START
            TooltipCorner.TOPRIGHT -> Gravity.TOP or Gravity.RIGHT or Gravity.END
            TooltipCorner.BOTTOMLEFT -> Gravity.BOTTOM or Gravity.LEFT or Gravity.START
            else -> Gravity.BOTTOM or Gravity.RIGHT or Gravity.END
        }

        val useSmallerSpacing = (tooltipText?.length ?: 0) > 90
        val smallMargin = view.context.convertDpToPixel(20f)

        (viewCircle.layoutParams as FrameLayout.LayoutParams).also {
            it.gravity = gravity
            viewCircle.layoutParams = it
        }

        (textContainer.layoutParams as FrameLayout.LayoutParams).also {
            it.gravity = gravity
            if (useSmallerSpacing) {
                it.topMargin = smallMargin
                it.bottomMargin = smallMargin
            }
            textContainer.layoutParams = it
        }

        (buttonClose.layoutParams as FrameLayout.LayoutParams).also {
            it.gravity = gravity
            buttonClose.layoutParams = it
        }

        (targetPoints ?: emptyList()).forEach { targetPoint ->
            val pulsingView = AMPulsingView(view.context)
            val layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.leftMargin = targetPoint.x - (context?.convertDpToPixel(60f) ?: 0)
            layoutParams.topMargin = targetPoint.y - (context?.convertDpToPixel(60f) ?: 0)
            pulsingView.layoutParams = layoutParams
            (view as FrameLayout).addView(pulsingView)

            pulsingView.alpha = 0f
            pulsingView.startAnimating()
            pulsingView.animate().withLayer().alpha(1f).setDuration(150).setStartDelay(400).start()
        }
    }

    fun runAction() {
        try {
            action?.run()
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun close() {
        try {
            runAction()
            activity?.onBackPressed()
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            HomeActivity.instance?.tooltipFragmentReference = null
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.tooltipText = it.getString("tooltipText")
            this.drawableResId = it.getInt("drawableResId")
            this.corner = TooltipCorner.values()[it.getInt("corner")]
            if (it.containsKey("targetPoints")) {
                this.targetPoints = it.getParcelableArrayList("targetPoints")
            }
        }
    }

    data class TooltipLocation(val corner: TooltipCorner, val targetPoint: Point)

    companion object {

        const val FRAGMENT_TAG = "TooltipFragment"

        /**
         *
         * @param tooltipText text
         * @param drawableResId image
         * @param corner one of the 4 screen corners
         * @param targetPoints coordinates of the center of each sensible area
         * @param action runnable to be called when detecting a touch
         * @return
         */
        @JvmStatic
        fun newInstance(
            tooltipText: String,
            drawableResId: Int,
            corner: TooltipCorner,
            targetPoints: ArrayList<Point>?,
            action: Runnable
        ): TooltipFragment {

            val args = Bundle()

            val fragment = TooltipFragment()
            args.putString("tooltipText", tooltipText)
            args.putInt("drawableResId", drawableResId)
            args.putInt("corner", corner.ordinal)
            targetPoints?.let {
                args.putParcelableArrayList("targetPoints", it)
            }
            fragment.action = action
            fragment.arguments = args
            return fragment
        }

        /**
         *
         * @see newInstance
         */
        @JvmStatic
        fun newInstance(
            tooltipText: String,
            drawableResId: Int,
            location: TooltipLocation,
            action: Runnable
        ): TooltipFragment {
            return newInstance(
                tooltipText,
                drawableResId,
                location.corner,
                arrayListOf(location.targetPoint),
                action
            )
        }
    }
}
