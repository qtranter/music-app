package com.audiomack.ui.premiumdownload

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.openUrlExcludingAudiomack
import com.audiomack.utils.spannableString
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.download_premium_progress.tvDownloadLimit
import kotlinx.android.synthetic.main.download_premium_progress.viewProgress
import kotlinx.android.synthetic.main.download_premium_progress.viewProgressContainer
import kotlinx.android.synthetic.main.fragment_premium_download.buttonBack
import kotlinx.android.synthetic.main.fragment_premium_download.buttonGoToDownloads
import kotlinx.android.synthetic.main.fragment_premium_download.buttonUpgradeNow
import kotlinx.android.synthetic.main.fragment_premium_download.imageViewBackground
import kotlinx.android.synthetic.main.fragment_premium_download.layoutFirstDownload
import kotlinx.android.synthetic.main.fragment_premium_download.tvLearnMore
import kotlinx.android.synthetic.main.fragment_premium_download.tvSubtitle

class PremiumDownloadFragment : TrackedFragment(R.layout.fragment_premium_download, TAG) {

    private val viewModel: PremiumDownloadViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initViewModel()
        initClickListeners()
        initViewModelObservers()
    }

    private fun initView() {
        Picasso.get()
            .load(R.drawable.premium_2019_header)
            .config(Bitmap.Config.RGB_565).into(imageViewBackground)

        val plainString = getString(R.string.premium_download_learn_more_left)
        val highlightedString = getString(R.string.premium_download_learn_more_right)
        val fullString = "$plainString $highlightedString"
        tvLearnMore.text = tvLearnMore.context.spannableString(
            fullString = fullString,
            highlightedStrings = listOf(highlightedString),
            highlightedColor = tvLearnMore.context.colorCompat(R.color.orange)
        )
    }

    private fun initViewModel() {
        val model = arguments?.getParcelable(EXTRA_PREMIUM_DOWNLOADS) as? PremiumDownloadModel
            ?: throw IllegalStateException("Missing 'data' intent extra")
        viewModel.init(model)
    }

    private fun initClickListeners() {
        buttonBack.setOnClickListener { viewModel.onBackClick() }
        buttonUpgradeNow.setOnClickListener { viewModel.onUpgradeClick() }
        buttonGoToDownloads.setOnClickListener { viewModel.onGoToDownloadsClick() }
        tvLearnMore.setOnClickListener { viewModel.onLearnClick() }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            backEvent.observe(viewLifecycleOwner, {
                activity?.onBackPressed()
            })

            upgradeEvent.observe(viewLifecycleOwner, {
                InAppPurchaseActivity.show(activity, InAppPurchaseMode.PremiumDownload)
            })

            goToDownloadsEvent.observe(viewLifecycleOwner, {
                activity?.onBackPressed()
                (activity as? HomeActivity)?.openMyAccount("downloads")
            })

            openURLEvent.observe(viewLifecycleOwner, { urlString ->
                context?.openUrlExcludingAudiomack(urlString)
            })

            firstDownloadLayoutVisible.observe(viewLifecycleOwner, { visible ->
                layoutFirstDownload.visibility = if (visible) View.VISIBLE else View.GONE
            })

            infoText.observe(viewLifecycleOwner, infoTextObserver)
            progressPercentage.observe(viewLifecycleOwner, progressPercentageObserver)
        }
    }

    private val infoTextObserver = Observer<PremiumDownloadProgressInfo> { info ->
        val remainingCount = info.countOfAvailablDownloads
        val totalCount = info.maxDownloads
        val remainingString = "$remainingCount"
        val highlightedString = getString(R.string.premium_download_highlighted_premium_count_message, remainingCount)
        val fullString = highlightedString + " " + getString(R.string.premium_download_plain_remaining)
        tvDownloadLimit.text = tvDownloadLimit.context.spannableString(
            fullString = fullString,
            highlightedStrings = listOf(if (remainingCount == 0) remainingString else highlightedString),
            highlightedColor = tvDownloadLimit.context.colorCompat(if (remainingCount == 0) R.color.red_error else R.color.orange)
        )
        tvSubtitle.text = if (remainingCount == 0) getString(R.string.premium_download_reached_limit) else getString(R.string.premium_download_large_subheader, totalCount)
    }

    private val progressPercentageObserver = Observer<Float> { progress ->
        val lp = viewProgress.layoutParams as FrameLayout.LayoutParams
        lp.width = (progress * viewProgressContainer.width).toInt()
        viewProgress.layoutParams = lp
        viewProgress.background = viewProgress.context.drawableCompat(if (progress == 1F) R.drawable.header_download_progress_full else R.drawable.header_download_progress)
    }

    companion object {
        private const val TAG = "PremiumDownloadFragment"
        private const val EXTRA_PREMIUM_DOWNLOADS = "data"
        fun newInstance(data: PremiumDownloadModel): PremiumDownloadFragment =
            PremiumDownloadFragment().apply {
                arguments = Bundle().apply { putParcelable(EXTRA_PREMIUM_DOWNLOADS, data) }
            }
    }
}
