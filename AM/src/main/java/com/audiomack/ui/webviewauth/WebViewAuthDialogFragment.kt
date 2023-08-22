package com.audiomack.ui.webviewauth

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import androidx.fragment.app.DialogFragment
import com.audiomack.R
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
class WebViewAuthDialogFragment : DialogFragment() {

    companion object {
        private const val AUTHENTICATION_CONFIGURATION_KEY = "authenticationConfiguration"
        private const val WEB_VIEW_KEY = "webView"
        private const val TAG = "WebViewDialogFragment"

        fun newInstance(configuration: WebViewAuthConfiguration): WebViewAuthDialogFragment =
            WebViewAuthDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(AUTHENTICATION_CONFIGURATION_KEY, configuration)
                }
            }
    }

    private lateinit var configuration: WebViewAuthConfiguration
    private var callback: ((WebViewAuthResult) -> Unit)? = null

    private val webViewIfCreated: WebView?
        get() = view as? WebView

    fun configure(callback: (WebViewAuthResult) -> Unit) {
        this.callback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configuration = requireArguments().getParcelable(AUTHENTICATION_CONFIGURATION_KEY)!!
        setStyle(STYLE_NORMAL, R.style.WebViewDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
            }
        }

        webView.webViewClient = WebViewAuthWebViewClient(configuration, ::onCallback)

        if (savedInstanceState != null) {
            savedInstanceState.getBundle(WEB_VIEW_KEY)?.run {
                webView.restoreState(this)
            }
        } else {
            webView.loadUrl(configuration.authenticationUri)
        }

        return webView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(
            WEB_VIEW_KEY,
            Bundle().apply {
                webViewIfCreated?.saveState(this)
            }
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCallback(WebViewAuthResult.Cancel)
    }

    // SignInWithAppleCallback

    private fun onCallback(result: WebViewAuthResult) {
        dialog?.dismiss()
        val callback = callback
        if (callback == null) {
            Timber.tag(TAG).e("Callback is not configured")
            return
        }
        callback(result)
    }
}
