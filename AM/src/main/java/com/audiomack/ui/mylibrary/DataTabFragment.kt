package com.audiomack.ui.mylibrary

import androidx.fragment.app.Fragment

interface DataTabFragment {
    /**
     * Called when the [fragment] becomes visible
     */
    fun onTabSelected(fragment: Fragment)
}
