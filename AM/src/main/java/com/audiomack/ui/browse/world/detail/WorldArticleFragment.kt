package com.audiomack.ui.browse.world.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.data.deeplink.Deeplink
import com.audiomack.fragments.TrackedFragment
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.showAlert
import com.audiomack.views.AMSnackbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_world_article.*
import kotlinx.android.synthetic.main.view_placeholder.*
import timber.log.Timber

class WorldArticleFragment : TrackedFragment(R.layout.fragment_world_article, TAG) {

    private val viewModel: WorldArticleViewModel by viewModels()
    private var slugString: String? = null
    private var webView: WebView? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initWebView()
        initViewModelObservers()
        initClickListeners()

        slugString?.let {
            viewModel.initWithSlug(it)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        try {
            webView = WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WorldArticleWebViewClient {
                    viewModel.onHtmlContentLoaded()
                }
                setBackgroundColor(Color.BLACK)
                settings.javaScriptEnabled = true
                addJavascriptInterface(
                    JSBridge { viewModel.onJSMessageReceived(it) },
                    JSBridge.NAME
                )
            }.also {
                webViewContainer.addView(it)
            }
        } catch (exception: PackageManager.NameNotFoundException) {
            context?.showAlert(requireContext().getString(R.string.word_article_webview_updating_error)) {
                activity?.onBackPressed()
            }
            Timber.tag(TAG).e(exception)
        } catch (exception: RuntimeException) {
            context?.showAlert(requireContext().getString(R.string.word_article_webview_updating_error)) {
                activity?.onBackPressed()
            }
            Timber.tag(TAG).e(exception)
        } catch (exception: Exception) {
            Timber.tag(TAG).e(exception)
        }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            viewState.observe(viewLifecycleOwner, viewStateObserver)
            onBackPressedEvent.observe(viewLifecycleOwner, onBackPressedEventObserver)
            sharePostEvent.observe(viewLifecycleOwner, sharePostEventObserver)
            openDeeplinkEvent.observe(viewLifecycleOwner, openDeeplinkEventObserver)
        }
    }

    private fun initClickListeners() {
        buttonBack.setOnClickListener { viewModel.onBackClicked() }
        buttonShare.setOnClickListener { viewModel.onShareClicked() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.slugString = it.getString(SLUG_STRING)
        }
    }

    private val viewStateObserver = Observer<WorldArticleViewModel.ViewState> { state ->
        when (state) {
            WorldArticleViewModel.ViewState.Loading -> {
                animationView.show()
                webViewContainer?.isVisible = false
                noConnectionPlaceholderView.isVisible = false
            }
            is WorldArticleViewModel.ViewState.Content -> {
                noConnectionPlaceholderView.isVisible = false
                webView?.loadDataWithBaseURL(null, state.html, "text/html", "UTF-8", null)
                (webViewContainer?.layoutParams as ConstraintLayout.LayoutParams).apply {
                    bottomMargin = if (state.adsVisible) resources.getDimension(R.dimen.ad_height)
                        .toInt() else 0
                }
            }
            WorldArticleViewModel.ViewState.ContentLoaded -> {
                animationView.hide()
                webViewContainer?.isVisible = true
            }
            WorldArticleViewModel.ViewState.Error -> {
                animationView.hide()
                webViewContainer?.isVisible = false
                cta.isVisible = false
                noConnectionPlaceholderView.isVisible = true
                imageView.setImageResource(R.drawable.ic_world_logo_gray)
                tvMessage.setText(R.string.world_article_detail_not_found)
            }
            WorldArticleViewModel.ViewState.Offline -> {
                animationView.hide()
                webViewContainer?.isVisible = false
                cta.isVisible = true
                noConnectionPlaceholderView.isVisible = true
                imageView.setImageResource(R.drawable.ic_empty_offline)
                tvMessage.setText(R.string.noconnection_placeholder)
                cta.setText(R.string.noconnection_highlighted_placeholder)
                cta.setOnClickListener {
                    slugString?.let {
                        viewModel.initWithSlug(it)
                    }
                }
                activity?.let {
                    AMSnackbar.Builder(it)
                        .withTitle(getString(R.string.download_results_no_connection))
                        .withSubtitle(getString(R.string.please_try_request_later))
                        .withDrawable(R.drawable.ic_snackbar_connection)
                        .withDuration(Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private val onBackPressedEventObserver = Observer<Void> {
        activity?.onBackPressed()
    }

    private val sharePostEventObserver = Observer<String> { shareLink ->
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareLink)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, getString(R.string.options_share))
        startActivity(shareIntent)
    }

    private val openDeeplinkEventObserver = Observer<Deeplink> { deeplink ->
        (activity as? HomeActivity)?.handleDeeplink(deeplink)
    }

    companion object {
        private const val SLUG_STRING = "slug"
        private const val TAG = "WorldArticleFragment"

        @JvmStatic
        fun newInstance(slug: String) =
            WorldArticleFragment().apply {
                arguments = Bundle().apply {
                    putString(SLUG_STRING, slug)
                }
            }
    }

    class JSBridge(val messageHandler: (String) -> Unit) {
        @JavascriptInterface
        fun showMessageInNative(message: String) {
            Timber.tag("WEBVIEW-JS").d(message)
            messageHandler(message)
        }

        companion object {
            const val NAME = "JSBridge"
        }
    }
}
