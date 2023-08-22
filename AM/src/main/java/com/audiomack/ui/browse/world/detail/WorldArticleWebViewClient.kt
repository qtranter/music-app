package com.audiomack.ui.browse.world.detail

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.audiomack.utils.openUrlInAudiomack

class WorldArticleWebViewClient(
    private val onPageFinished: () -> Unit
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        request?.url?.let { uri ->
            view?.context?.openUrlInAudiomack(uri.toString())
        }
        return true
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished()
    }
}
