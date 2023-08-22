package com.audiomack.ui.settings

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnLayout
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.Action
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import kotlin.math.floor
import kotlin.math.min
import kotlinx.android.synthetic.main.fragment_option_menu.actionsContainer
import kotlinx.android.synthetic.main.fragment_option_menu.buttonCancel
import kotlinx.android.synthetic.main.fragment_option_menu.mainLayout
import kotlinx.android.synthetic.main.fragment_option_menu.parentLayout
import kotlinx.android.synthetic.main.fragment_option_menu.scrollView
import timber.log.Timber

internal class OptionsMenuFragment : TrackedFragment(R.layout.fragment_option_menu, TAG) {

    private val kMinVisibleRows = 10
    private val kRowHeight = 50f

    private var actions: List<Action> = listOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonCancel.setOnClickListener { close() }

        parentLayout.alpha = 0f
        mainLayout.alpha = 0f

        actions.forEachIndexed { index, action ->
            val actionView = LayoutInflater.from(context).inflate(R.layout.row_option_menu, actionsContainer, false)
            actionView.findViewById<TextView>(R.id.tvTitle).apply {
                text = action.title
                setTextColor(if (action.isSelected) context.colorCompat(R.color.orange) else Color.WHITE)
                if (action.drawableLeft != null && action.drawableLeft != -1) {
                    setCompoundDrawablesWithIntrinsicBounds(action.drawableLeft!!, 0, 0, 0)
                    compoundDrawablePadding = 40
                }
            }
            actionView.findViewById<View>(R.id.divider).visibility = if (index == actions.lastIndex) View.GONE else View.VISIBLE
            actionView.setOnClickListener { action.listener?.onActionExecuted() }
            actionsContainer.addView(actionView)
            action.view = actionView
        }

        scrollView.doOnLayout {
            try {

                val spacing = context?.convertDpToPixel(61f) ?: 0 // Button + divider

                val maxHeight = view.height - buttonCancel.height - spacing
                var visibleChildren = min(kMinVisibleRows, actions.size)
                if (visibleChildren * (context?.convertDpToPixel(kRowHeight) ?: 0) > maxHeight) {
                    visibleChildren = floor(maxHeight.toDouble() / (context?.convertDpToPixel(kRowHeight) ?: 0).toDouble()).toInt() - 1
                }
                var visibleChildrenHeight = (visibleChildren * (context?.convertDpToPixel(kRowHeight) ?: 0))
                visibleChildrenHeight += if (actions.size > visibleChildren) {
                    ((context?.convertDpToPixel(kRowHeight) ?: 0) * 0.55f).toInt()
                } else 0

                val buttonHeight = maxHeight - visibleChildrenHeight

                Button(context).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener { close() }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, buttonHeight)
                }.also { actionsContainer.addView(it, 0) }

                mainLayout.animate().translationY(view.height.toFloat()).duration = 50

                Handler().postDelayed({
                    parentLayout?.animate()?.alpha(1f)?.duration = 50
                    mainLayout?.animate()?.translationY(0f)?.alpha(1f)?.duration = 300
                }, 50)
            } catch (e: Exception) {
                // Fragment may have been destroyed before reaching this point
                Timber.w(e)
            }
        }
    }

    private fun close() {
        try {
            (activity as? BaseActivity)?.popFragment()
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    companion object {
        private const val TAG = "OptionsMenuFragment"
        const val TAG_BACK_STACK = "options"

        @JvmStatic
        fun newInstance(actions: List<Action>): OptionsMenuFragment =
            OptionsMenuFragment().apply {
                this.actions = actions
            }
    }
}
