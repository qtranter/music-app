package com.audiomack.ui.webviewauth

import androidx.fragment.app.FragmentManager

class WebViewAuthManager(
    private val fragmentManager: FragmentManager,
    private val fragmentTag: String,
    private val configuration: WebViewAuthConfiguration,
    private val callback: (WebViewAuthResult) -> Unit
) {

    init {
        val fragmentIfShown =
            fragmentManager.findFragmentByTag(fragmentTag) as? WebViewAuthDialogFragment
        fragmentIfShown?.configure(callback)
    }

    fun show() {
        val fragment = WebViewAuthDialogFragment.newInstance(configuration)
        fragment.configure(callback)
        fragment.show(fragmentManager, fragmentTag)
    }
}
