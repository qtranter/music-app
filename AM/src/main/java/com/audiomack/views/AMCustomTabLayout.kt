package com.audiomack.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.audiomack.R
import com.audiomack.utils.applyLetterspacing
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.getTypefaceSafely
import com.google.android.material.tabs.TabLayout
import java.lang.reflect.Field
import timber.log.Timber

class AMCustomTabLayout : TabLayout {

    constructor(context: Context) : super(context) {
        adjust()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        adjust()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        adjust()
    }

    private fun adjust() {
        if (tabMode == MODE_SCROLLABLE) {
            val tabMinWidth = context?.convertDpToPixel(50f) ?: 0
            val field: Field
            try {
                field = TabLayout::class.java.getDeclaredField("scrollableTabMinWidth")
                field.isAccessible = true
                field.set(this, tabMinWidth)
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }

    override fun setupWithViewPager(viewPager: ViewPager?) {
        super.setupWithViewPager(viewPager)
        customizeLayout(viewPager)
    }

    override fun setupWithViewPager(viewPager: ViewPager?, autoRefresh: Boolean) {
        super.setupWithViewPager(viewPager, autoRefresh)
        customizeLayout(viewPager)
    }

    private fun customizeLayout(viewPager: ViewPager?) {
        if (context != null) {
            this.removeAllTabs()

            val slidingTabStrip = getChildAt(0) as ViewGroup

            viewPager?.adapter?.let { adapter ->
                var i = 0
                val count = adapter.count
                while (i < count) {
                    val tab = this.newTab()
                    this.addTab(tab.setText(adapter.getPageTitle(i)))
                    val view =
                        (slidingTabStrip.getChildAt(i) as ViewGroup).getChildAt(1) as TextView
                    view.typeface = view.context.getTypefaceSafely(R.font.opensans_extrabold)
                    view.applyLetterspacing(-0.89f)
                    i++
                }
            }
        }
    }
}
