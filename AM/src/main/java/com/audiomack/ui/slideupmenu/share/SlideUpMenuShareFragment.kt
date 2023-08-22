package com.audiomack.ui.slideupmenu.share

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.tracking.mixpanel.MixpanelPageMusicInfo
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.showHighlightErrorToast
import com.audiomack.utils.showHighlightSuccessAlert
import com.audiomack.utils.showLoggedOutAlert
import com.audiomack.utils.showOfflineAlert
import com.audiomack.utils.showReachedLimitOfHighlightsAlert
import com.audiomack.views.AMProgressHUD
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonCancel
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonCopyLink
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonFacebook
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonHighlight
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonInstagram
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonMessenger
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonMore
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonShareImage
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonSms
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonSnapchat
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonTwitter
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonWeChat
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.buttonWhatsApp
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.gridLayoutButtons
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.highlightPinImageView
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.layoutHighlight
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.layoutRowFour
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.layoutRowOne
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.layoutRowThree
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.layoutRowTwo
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonCopyLink
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonFacebook
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonHighlight
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonInstagram
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonMessenger
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonMore
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonSms
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonSnapchat
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonTrophies
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonTwitter
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonWeChat
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listButtonWhatsapp
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.listLayoutButtons
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.mainLayout
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.parentLayout
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.shareImageView
import kotlinx.android.synthetic.main.fragment_slideup_menu_share.viewBuffer

class SlideUpMenuShareFragment : TrackedFragment(R.layout.fragment_slideup_menu_share, TAG) {

    private lateinit var viewModel: SlideUpMenuShareViewModel
    private var music: AMResultItem? = null
    private var artist: AMArtist? = null
    private lateinit var externalMixpanelSource: MixpanelSource
    private lateinit var externalMixpanelButton: String

    val mixpanelSource: MixpanelSource
        get() = if (!::externalMixpanelSource.isInitialized) MixpanelSource(
            MainApplication.currentTab,
            MixpanelPageMusicInfo
        ) else externalMixpanelSource

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!::externalMixpanelButton.isInitialized) {
            activity?.onBackPressed()
            return
        }

        viewModel = ViewModelProvider(
            this,
            SlideUpMenuShareViewModelFactory(
                music,
                artist,
                mixpanelSource,
                externalMixpanelButton
            )
        ).get(SlideUpMenuShareViewModel::class.java)

        initViewModelObservers()
        initClickListeners()

        artist?.let {
            viewBuffer.visibility = View.VISIBLE
            layoutHighlight.visibility = View.GONE
            layoutRowTwo.removeView(buttonInstagram)
            layoutRowThree.removeView(buttonFacebook)
            layoutRowFour.removeView(buttonMessenger)
            layoutRowOne.addView(buttonInstagram)
            layoutRowTwo.addView(buttonFacebook)
            layoutRowThree.addView(buttonMessenger)
            listButtonHighlight.visibility = View.GONE
        }

        mainLayout.doOnLayout {
            viewModel.onLoadAndBlur(context)
            viewModel.onVisible()
        }

        mainLayout.visibility = View.INVISIBLE
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            closeEvent.observe(viewLifecycleOwner) { activity?.onBackPressed() }
            startAnimationEvent.observe(viewLifecycleOwner) {
                mainLayout.visibility = View.VISIBLE
                mainLayout.startAnimation(
                    AnimationUtils.loadAnimation(
                        mainLayout.context,
                        R.anim.slide_up
                    )
                )
            }
            loadBitmapEvent.observe(viewLifecycleOwner) {
                shareImageView.setImageBitmap(it)
            }
            highlighted.observe(viewLifecycleOwner) { highlighted ->
                buttonHighlight.text = getString(if (highlighted) R.string.highlights_highlighted else R.string.highlights_add)
                listButtonHighlight.text = getString(if (highlighted) R.string.highlights_highlighted else R.string.highlights_add)
                highlightPinImageView.visibility = if (highlighted) View.VISIBLE else View.GONE
            }
            showHUDEvent.observe(viewLifecycleOwner) { mode ->
                AMProgressHUD.show(activity, mode)
            }
            notifyOfflineEvent.observe(viewLifecycleOwner) {
                showOfflineAlert()
            }
            loginRequiredEvent.observe(viewLifecycleOwner) {
                showLoggedOutAlert(it)
            }
            reachedHighlightsLimitEvent.observe(viewLifecycleOwner) {
                showReachedLimitOfHighlightsAlert()
            }
            highlightErrorEvent.observe(viewLifecycleOwner) {
                showHighlightErrorToast()
            }
            highlightSuccessEvent.observe(viewLifecycleOwner) {
                showHighlightSuccessAlert(it)
            }
            shareMenuListMode.observe(viewLifecycleOwner) { isShareMenuListMode ->
                listLayoutButtons.isVisible = isShareMenuListMode
                gridLayoutButtons.isVisible = !isShareMenuListMode
            }
        }
    }

    private fun initClickListeners() {
        buttonCancel.setOnClickListener { viewModel.onCancelTapped() }

        parentLayout.setOnClickListener { viewModel.onBackgroundTapped() }

        buttonCopyLink.setOnClickListener { copyLinkClickAction() }
        listButtonCopyLink.setOnClickListener { copyLinkClickAction() }

        buttonTwitter.setOnClickListener { twitterClickAction() }
        listButtonTwitter.setOnClickListener { twitterClickAction() }

        buttonFacebook.setOnClickListener { facebookClickAction() }
        listButtonFacebook.setOnClickListener { facebookClickAction() }

        buttonInstagram.setOnClickListener { instagramClickAction() }
        listButtonInstagram.setOnClickListener { instagramClickAction() }

        buttonSnapchat.setOnClickListener { shapchatClickAction() }
        listButtonSnapchat.setOnClickListener { shapchatClickAction() }

        buttonSms.setOnClickListener { smsClickAction() }
        listButtonSms.setOnClickListener { smsClickAction() }

        buttonMore.setOnClickListener { moreClickAction() }
        listButtonMore.setOnClickListener { moreClickAction() }

        buttonShareImage.setOnClickListener { trophiesClickAction() }
        listButtonTrophies.setOnClickListener { trophiesClickAction() }

        music?.let { notNullMusic ->
            buttonHighlight.setOnClickListener { viewModel.onHighlightTapped(notNullMusic) }
            listButtonHighlight.setOnClickListener { viewModel.onHighlightTapped(notNullMusic) }
        }

        buttonWhatsApp.setOnClickListener { whatsappClickAction() }
        listButtonWhatsapp.setOnClickListener { whatsappClickAction() }

        buttonMessenger.setOnClickListener { messengerClickAction() }
        listButtonMessenger.setOnClickListener { messengerClickAction() }

        buttonWeChat.setOnClickListener { wechatClickAction() }
        listButtonWeChat.setOnClickListener { wechatClickAction() }

        mainLayout.doOnLayout {
            viewModel.onLoadAndBlur(context)
            viewModel.onVisible()
        }

        mainLayout.visibility = View.INVISIBLE
    }

    private fun trophiesClickAction() {
        HomeActivity.instance?.let {
            viewModel.onShareScreenshotTapped(it)
        }
    }

    private fun moreClickAction() {
        HomeActivity.instance?.let {
            viewModel.onShareViaOtherTapped(it, it.disposables)
        }
    }

    private fun smsClickAction() {
        HomeActivity.instance?.let {
            viewModel.onShareViaContactsTapped(it, it.disposables)
        }
    }

    private fun shapchatClickAction() {
        HomeActivity.instance?.let {
            viewModel.onShareViaSnapchatTapped(it, it.disposables)
        }
    }

    private fun instagramClickAction() {
        HomeActivity.instance?.let {
            viewModel.onShareViaInstagramTapped(it, it.disposables)
        }
    }

    private fun facebookClickAction() {
        HomeActivity.instance?.let {
            viewModel.onShareViaFacebookTapped(it, it.disposables)
        }
    }

    private fun twitterClickAction() {
        HomeActivity.instance?.let {
            viewModel.onShareViaTwitterTapped(it, it.disposables)
        }
    }

    private fun copyLinkClickAction() {
        HomeActivity.instance?.let {
            viewModel.onCopyLinkTapped(it)
        }
    }

    private fun whatsappClickAction() {
        HomeActivity.instance?.let {
            viewModel.onShareWhatsAppTapped(it)
        }
    }

    private fun messengerClickAction() {
        HomeActivity.instance?.let {
            viewModel.onShareMessengerTapped(it)
        }
    }

    private fun wechatClickAction() {
        HomeActivity.instance?.let {
            viewModel.onShareWeChatTapped(it)
        }
    }

    companion object {
        private const val TAG = "SlideUpMenuShareFragment"
        @JvmStatic
        fun newInstance(
            music: AMResultItem?,
            artist: AMArtist?,
            externalMixpanelSource: MixpanelSource,
            externalMixpanelButton: String
        ) = SlideUpMenuShareFragment().also {
                it.music = music
                it.artist = artist
                it.externalMixpanelSource = externalMixpanelSource
                it.externalMixpanelButton = externalMixpanelButton
            }
    }
}
