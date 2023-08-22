package com.audiomack.ui.common

import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.audiomack.ui.mylibrary.DataTabFragment
import com.audiomack.utils.scrollListToTop
import com.google.android.material.tabs.TabLayout

class ViewPagerTabs(private val viewPager: ViewPager) :
        TabLayout.ViewPagerOnTabSelectedListener(viewPager) {

    private val currentFrag: Fragment?
        get() = viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as? Fragment

    fun connect(tabLayout: TabLayout) {
        tabLayout.addOnTabSelectedListener(this)
    }

    fun remove(tabLayout: TabLayout) {
        tabLayout.removeOnTabSelectedListener(this)
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        currentFrag?.scrollListToTop()
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

    override fun onTabSelected(tab: TabLayout.Tab) {
        (currentFrag as? DataTabFragment)?.onTabSelected(currentFrag!!)
    }
}
