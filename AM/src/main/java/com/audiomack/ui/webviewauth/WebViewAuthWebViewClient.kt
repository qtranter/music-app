package com.audiomack.ui.webviewauth

import android.net.Uri
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.audiomack.R

internal class WebViewAuthWebViewClient(
    private val configuration: WebViewAuthConfiguration,
    private val callback: (WebViewAuthResult) -> Unit
) : WebViewClient() {

    private var ignoreErrors = false

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return isUrlOverridden(view, Uri.parse(url))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return isUrlOverridden(view, request?.url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        val webView = view ?: return
        if (!ignoreErrors && request?.url?.toString()?.contains(configuration.interceptRedirectUri) == false) {
            callback(WebViewAuthResult.Failure(IllegalStateException(webView.resources.getString(R.string.generic_api_error))))
        }
    }

    private fun isUrlOverridden(view: WebView?, url: Uri?): Boolean {
        return when {
            url == null -> {
                false
            }
            url.toString().contains(configuration.allowedDomain) -> {
                view?.loadUrl(url.toString())
                true
            }
            url.toString().startsWith(configuration.interceptRedirectUri) -> {
                ignoreErrors = true
                val token = url.getQueryParameter(configuration.interceptRedirectUriQueryStringParamName)
                token?.let {
                    callback(WebViewAuthResult.Success(it))
                } ?: run {
                    callback(WebViewAuthResult.Failure(IllegalArgumentException("expected '${configuration.interceptRedirectUriQueryStringParamName}' query string parameter not returned")))
                }
                true
            }
            else -> {
                false
            }
        }
    }
}
