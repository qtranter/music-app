package com.audiomack.ui.common

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import com.audiomack.R
import com.audiomack.R.style
import com.audiomack.fragments.TrackedFragment
import com.audiomack.utils.onHidden
import com.audiomack.utils.setStartDrawable
import com.audiomack.views.AMCustomFontTextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import kotlinx.android.synthetic.main.fragment_slideup_menu.cancelButton
import kotlinx.android.synthetic.main.fragment_slideup_menu.container
import kotlinx.android.synthetic.main.fragment_slideup_menu.menuContainer

abstract class SlideUpMenuFragment(logTag: String) :
    TrackedFragment(R.layout.fragment_slideup_menu, logTag) {

    /**
     * Called when the bottom sheet is hidden
     */
    protected open fun onDismissed() {
        parentFragmentManager.popBackStack()
    }

    /**
     * Hide the bottom sheet
     */
    protected fun dismiss() {
        bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
    }

    /**
     * Sets the list of menu items to be displayed
     */
    protected fun setMenuItems(menuItems: List<SlideUpMenuItem>) {
        addMenuItems(menuItems)
    }

    private lateinit var bottomSheet: BottomSheetBehavior<ViewGroup>
    private lateinit var bottomSheetCallback: BottomSheetCallback

    private val itemLayoutParams: LayoutParams =
        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews()
    }

    @CallSuper
    override fun onDestroyView() {
        bottomSheet.removeBottomSheetCallback(bottomSheetCallback)
        super.onDestroyView()
    }

    private fun initViews() {
        bottomSheet = BottomSheetBehavior.from(menuContainer)
        bottomSheetCallback = bottomSheet.onHidden { onDismissed() }
        cancelButton.setOnClickListener { dismiss() }
        container.setOnClickListener { dismiss() }
    }

    private fun addMenuItems(menuItems: List<SlideUpMenuItem>) {
        val context = view?.context ?: return

        menuItems.forEachIndexed { index, slideUpMenuItem ->
            val slideUpMenuItemView = SlideUpMenuItemView(context, slideUpMenuItem).apply {
                setOnClickListener {
                    slideUpMenuItem.onClick()
                    dismiss()
                }
            }
            menuContainer.addView(slideUpMenuItemView, index, itemLayoutParams)
        }
    }
}

data class SlideUpMenuItem(
    @DrawableRes val iconRes: Int,
    @StringRes val textRes: Int,
    val onClick: () -> Unit
)

private class SlideUpMenuItemView(context: Context, slideUpMenuItem: SlideUpMenuItem) :
    AMCustomFontTextView(ContextThemeWrapper(context, style.Widget_Audiomack_TextView_MenuItem)) {

    init {
        setText(slideUpMenuItem.textRes)
        setStartDrawable(slideUpMenuItem.iconRes)
        setOnClickListener { slideUpMenuItem.onClick() }
    }
}
