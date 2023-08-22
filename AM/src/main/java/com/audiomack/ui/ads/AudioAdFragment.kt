package com.audiomack.ui.ads

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import com.ad.core.companion.AdCompanionView
import com.audiomack.R
import com.audiomack.utils.AMClickableSpan
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString
import kotlinx.android.synthetic.main.fragment_audio_ad.audioAdCompanionView
import kotlinx.android.synthetic.main.fragment_audio_ad.audioAdCountdown
import kotlinx.android.synthetic.main.fragment_audio_ad.audioAdDefaultCompanion
import kotlinx.android.synthetic.main.fragment_audio_ad.audioAdDefaultCompanionBg
import kotlinx.android.synthetic.main.fragment_audio_ad.audioAdDefaultCompanionBtn
import kotlinx.android.synthetic.main.fragment_audio_ad.audioAdUpsell

class AudioAdFragment : Fragment(R.layout.fragment_audio_ad), AdCompanionView.Listener {

    private val viewModel: AudioAdViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        initViewModelObservers()
    }

    override fun onResume() {
        super.onResume()

        viewModel.onViewVisible()
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            secondsRemaining.observe(viewLifecycleOwner) { seconds ->
                audioAdCountdown.text =
                    getString(R.string.audio_ad_now_playing_info_artist, seconds)
            }
            companionAdDisplayedEvent.observe(viewLifecycleOwner) {
                audioAdDefaultCompanion.visibility = View.GONE
            }
            showHouseAdEvent.observe(viewLifecycleOwner) {
                audioAdDefaultCompanion.visibility = View.VISIBLE
            }
        }
    }

    private fun initViews(view: View) {
        audioAdUpsell.apply {
            movementMethod = LinkMovementMethod()
            text = view.context.spannableString(
                fullString = getString(R.string.audio_ad_support_artists) + "\n" + getString(R.string.audio_ad_upgrade_premium),
                highlightedStrings = listOf(getString(R.string.audio_ad_upgrade_premium)),
                highlightedColor = view.context.colorCompat(R.color.orange),
                clickableSpans = listOf(AMClickableSpan(view.context) { viewModel.onUpSellClick() })
            )
        }

        audioAdCompanionView.listener = this

        audioAdDefaultCompanionBg.setOnClickListener {
            viewModel.onUpSellClick()
        }
        audioAdDefaultCompanionBtn.setOnClickListener {
            viewModel.onStartTrialClick()
        }
    }

    override fun didDisplayAd(adCompanionView: AdCompanionView) {
        viewModel.onCompanionAdDisplayed()
    }

    override fun didEndDisplay(adCompanionView: AdCompanionView) {
        viewModel.onCompanionAdEnded()
    }

    override fun didFailToDisplayAd(adCompanionView: AdCompanionView, error: Error) {
        viewModel.onError(error.cause)
    }

    override fun shouldOverrideClickThrough(adCompanionView: AdCompanionView, url: Uri) = false

    override fun willLeaveApplication(adCompanionView: AdCompanionView) = Unit

    override fun willLoadAd(adCompanionView: AdCompanionView) = Unit

    companion object {
        const val TAG = "AudioAdFragment"

        fun newInstance(): AudioAdFragment = AudioAdFragment()
    }
}
