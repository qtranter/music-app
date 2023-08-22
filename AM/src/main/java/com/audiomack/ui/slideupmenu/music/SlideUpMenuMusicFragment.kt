package com.audiomack.ui.slideupmenu.music

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.doOnLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.tracking.mixpanel.MixpanelPageMusicInfo
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMResultItem
import com.audiomack.model.AddToPlaylistModel
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.playback.SongAction
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.player.full.view.SongActionButton
import com.audiomack.ui.playlist.add.AddToPlaylistsActivity
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.audiomack.utils.confirmDownloadDeletion
import com.audiomack.utils.confirmPlaylistDownloadDeletion
import com.audiomack.utils.confirmPlaylistSync
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.showDownloadUnlockedToast
import com.audiomack.utils.showFailedPlaylistDownload
import com.audiomack.utils.showFavoritedToast
import com.audiomack.utils.showHighlightErrorToast
import com.audiomack.utils.showHighlightSuccessAlert
import com.audiomack.utils.showLoggedOutAlert
import com.audiomack.utils.showOfflineAlert
import com.audiomack.utils.showReachedLimitOfHighlightsAlert
import com.audiomack.utils.showRepostedToast
import com.audiomack.utils.spannableString
import com.audiomack.utils.spannableStringWithImageAtTheEnd
import com.audiomack.views.AMProgressHUD
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonCancel
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonCopyLink
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonDeleteDownload
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonDownload
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonFacebook
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonHighlight
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonInfo
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonInstagram
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonMessenger
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonMore
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonPlayLater
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonPlayNext
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonRemoveFromDownloads
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonRemoveFromQueue
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonSMS
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonSnapchat
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonTrophies
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonTwitter
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonViewAdd
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonViewComment
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonViewDownload
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonViewFavorite
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonViewRepost
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonWeChat
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.buttonWhatsapp
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.imageView
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.layoutAddToQueueControls
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.mainLayout
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.parentLayout
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.scrollViewButtons
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.tvAddedBy
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.tvArtist
import kotlinx.android.synthetic.main.fragment_slideup_menu_music.tvTitle

class SlideUpMenuMusicFragment : TrackedFragment(R.layout.fragment_slideup_menu_music, TAG) {

    private lateinit var viewModel: SlideUpMenuMusicViewModel
    private lateinit var music: AMResultItem
    private lateinit var externalMixpanelSource: MixpanelSource
    private var removeFromDownloadsEnabled: Boolean = false
    private var removeFromQueueEnabled: Boolean = false
    private var removeFromQueueIndex: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!::music.isInitialized) {
            activity?.onBackPressed()
            return
        }

        viewModel = ViewModelProvider(this,
            SlideUpMenuMusicViewModelFactory(
                music,
                mixpanelSource,
                removeFromDownloadsEnabled,
                removeFromQueueEnabled,
                removeFromQueueIndex
            )
        ).get(SlideUpMenuMusicViewModel::class.java)

        initClickListeners()
        initViewModelObservers()

        mainLayout.doOnLayout {
            val offset = mainLayout.context.convertDpToPixel(50F)
            val correction = if ((mainLayout.height + offset) > view.height) - (mainLayout.height + offset - view.height) else 0
            scrollViewButtons.layoutParams.apply {
                height += correction
            }
            viewModel.onVisible()
        }
    }

    private fun initClickListeners() {
        tvAddedBy.setOnClickListener { viewModel.onArtistInfoTapped() }
        buttonCancel.setOnClickListener { viewModel.onCancelTapped() }
        parentLayout.setOnClickListener { viewModel.onBackgroundTapped() }
        buttonInfo.setOnClickListener { viewModel.onMusicInfoTapped() }
        buttonViewAdd.setOnClickListener { viewModel.onAddToPlaylistTapped() }
        buttonViewFavorite.setOnClickListener { onFavoriteClick() }
        buttonViewRepost.setOnClickListener { viewModel.onRepostTapped() }
        buttonViewComment.setOnClickListener { viewModel.onCommentsClick() }
        buttonViewDownload.setOnClickListener { viewModel.onDownloadTapped() }
        buttonRemoveFromDownloads.setOnClickListener { viewModel.onRemoveFromDownloadsTapped() }
        buttonRemoveFromQueue.setOnClickListener { viewModel.onRemoveFromQueueTapped() }
        buttonPlayNext.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onPlayNextTapped(it, it.disposables)
            }
        }
        buttonPlayLater.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onPlayLaterTapped(it, it.disposables)
            }
        }
        buttonDownload.setOnClickListener { viewModel.onDownloadTapped() }
        buttonDeleteDownload.setOnClickListener { viewModel.onDeleteDownloadTapped() }
        buttonHighlight.setOnClickListener { viewModel.onHighlightTapped() }
        buttonCopyLink.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onCopyLinkTapped(it)
            }
        }
        buttonTwitter.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onShareViaTwitterTapped(it, it.disposables)
            }
        }
        buttonFacebook.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onShareViaFacebookTapped(it, it.disposables)
            }
        }
        buttonSMS.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onShareViaContactsTapped(it, it.disposables)
            }
        }
        buttonMore.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onShareViaOtherTapped(it, it.disposables)
            }
        }
        buttonTrophies.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onShareScreenshotTapped(it)
            }
        }
        buttonInstagram.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onShareViaInstagramTapped(it, it.disposables)
            }
        }
        buttonSnapchat.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onShareViaSnapchatTapped(it, it.disposables)
            }
        }
        buttonWhatsapp.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onShareViaWhatsAppTapped(it)
            }
        }
        buttonMessenger.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onShareViaMessengerTapped(it)
            }
        }
        buttonWeChat.setOnClickListener {
            HomeActivity.instance?.let {
                viewModel.onShareViaWeChatTapped(it)
            }
        }
    }

    private fun initViewModelObservers() {
        viewModel.apply {

            closeEvent.observe(viewLifecycleOwner) {
                activity?.onBackPressed()
            }
            dismissEvent.observe(viewLifecycleOwner) { activity?.onBackPressed() }
            musicInfoEvent.observe(viewLifecycleOwner) { HomeActivity.instance?.openMusicInfo(music) }
            artistInfoEvent.observe(viewLifecycleOwner) {
                it?.let { uploaderSlug ->
                    HomeActivity.instance?.homeViewModel?.onArtistScreenRequested(uploaderSlug)
                }
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
            addToPlaylistEvent.observe(viewLifecycleOwner) { (music, mixpanelSource, mixpanelButton) ->
                AddToPlaylistsActivity.show(activity, AddToPlaylistModel(music, mixpanelSource, mixpanelButton))
            }
            startAnimationEvent.observe(viewLifecycleOwner) {
                mainLayout.visibility = View.VISIBLE
                mainLayout.startAnimation(AnimationUtils.loadAnimation(mainLayout.context, R.anim.slide_up))
            }
            showHUDEvent.observe(viewLifecycleOwner) { mode -> AMProgressHUD.show(activity, mode) }
            notifyOfflineEvent.observe(viewLifecycleOwner) {
                activity?.showOfflineAlert()
            }
            notifyRepostEvent.observe(viewLifecycleOwner) {
                activity?.showRepostedToast(it)
            }
            notifyFavoriteEvent.observe(viewLifecycleOwner) {
                activity?.showFavoritedToast(it)
            }
            loginRequiredEvent.observe(viewLifecycleOwner) { source ->
                showLoggedOutAlert(source)
            }
            premiumRequiredEvent.observe(viewLifecycleOwner) { mode ->
                InAppPurchaseActivity.show(activity, mode)
            }
            openCommentsEvent.observe(viewLifecycleOwner) { music ->
                HomeActivity.instance?.openComments(music, null, null)
            }
            showConfirmDownloadDeletionEvent.observe(viewLifecycleOwner) { music ->
                confirmDownloadDeletion(music)
            }
            showConfirmPlaylistDownloadDeletionEvent.observe(viewLifecycleOwner) { music ->
                confirmPlaylistDownloadDeletion(music)
            }
            showFailedPlaylistDownloadEvent.observe(viewLifecycleOwner) {
                showFailedPlaylistDownload()
            }
            showConfirmPlaylistSyncEvent.observe(viewLifecycleOwner) { tracksCount ->
                confirmPlaylistSync(tracksCount) {
                    viewModel.onPlaylistSyncConfirmed()
                }
            }
            showPremiumDownloadEvent.observe(viewLifecycleOwner, showPremiumDownloadEventObserver)
            showUnlockedToastEvent.observe(viewLifecycleOwner, showUnlockedToastEventObserver)

            favoriteAction.observe(viewLifecycleOwner, ActionObserver(buttonViewFavorite))
            addToPlaylistAction.observe(viewLifecycleOwner, ActionObserver(buttonViewAdd))
            rePostAction.observe(viewLifecycleOwner, ActionObserver(buttonViewRepost))
            downloadAction.observe(viewLifecycleOwner, ActionObserver(buttonViewDownload))
            commentsCount.observe(viewLifecycleOwner) { count ->
                buttonViewComment.commentsCount = count
            }
            viewState.observe(viewLifecycleOwner, { viewState ->
                PicassoImageLoader.load(activity, viewState.imageUrl, imageView)
                tvArtist.text = viewState.artist
                tvTitle.text = viewState.title

                val addedByString = tvAddedBy.context.spannableString(
                    fullString = getString(R.string.slideupmenu_music_added_by_template, viewState.addedBy),
                    highlightedStrings = listOf(viewState.addedBy ?: ""),
                    highlightedColor = tvAddedBy.context.colorCompat(R.color.orange)
                )
                val addedByImageString =
                    when {
                        viewState.uploaderVerified -> tvAddedBy.spannableStringWithImageAtTheEnd("", R.drawable.ic_verified, 12)
                        viewState.uploaderTastemaker -> tvAddedBy.spannableStringWithImageAtTheEnd("", R.drawable.ic_tastemaker, 12)
                        viewState.uploaderAuthenticated -> tvAddedBy.spannableStringWithImageAtTheEnd("", R.drawable.ic_authenticated, 12)
                        else -> ""
                    }
                tvAddedBy.text = TextUtils.concat(addedByString, addedByImageString)

                buttonDownload.visibility = if (viewState.downloadVisible) View.VISIBLE else View.GONE
                buttonDeleteDownload.visibility = if (viewState.deleteDownloadVisible) View.VISIBLE else View.GONE
                buttonHighlight.text = getString(if (viewState.musicHighlighted) R.string.highlights_highlighted else R.string.highlights_add)
                buttonViewAdd.visibility = if (viewState.addToPlaylistVisible) View.VISIBLE else View.GONE
                buttonViewRepost.visibility = if (viewState.repostVisible) View.VISIBLE else View.GONE

                buttonRemoveFromDownloads.visibility = if (viewState.removeFromDownloadsEnabled) View.VISIBLE else View.GONE
                buttonRemoveFromQueue.visibility = if (viewState.removeFromQueueEnabled) View.VISIBLE else View.GONE
                layoutAddToQueueControls.visibility = if (viewState.removeFromQueueEnabled) View.GONE else View.VISIBLE
            })

            mainLayout.visibility = View.INVISIBLE
        }
    }

    private val showPremiumDownloadEventObserver = Observer<PremiumDownloadModel> { model ->
        HomeActivity.instance?.requestPremiumDownloads(model)
    }

    private val showUnlockedToastEventObserver = Observer<String> { musicName ->
        showDownloadUnlockedToast(musicName)
    }

    val mixpanelSource: MixpanelSource
        get() = if (!::externalMixpanelSource.isInitialized) MixpanelSource(MainApplication.currentTab, MixpanelPageMusicInfo) else externalMixpanelSource

    private fun onFavoriteClick() {
        if (viewModel.viewState.value?.musicFavorited == false) {
            val heartView =
                buttonViewFavorite.findViewById<View>(R.id.playerActionBtnContentImage)
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(heartView, View.SCALE_X, 1f, 1.3f, 1f, 1.3f, 1f),
                    ObjectAnimator.ofFloat(heartView, View.SCALE_Y, 1f, 1.3f, 1f, 1.3f, 1f)
                )
                duration = 500L
                start()
            }
        }
        viewModel.onFavoriteTapped()
    }

    // Utils

    inner class ActionObserver(private val button: SongActionButton) : Observer<SongAction> {
        override fun onChanged(action: SongAction?) {
            view?.post {
                button.action = action
            }
        }
    }

    // Static

    companion object {
        private const val TAG = "SlideUpMenuMusicFragment"
        @JvmStatic
        fun newInstance(music: AMResultItem, externalMixpanelSource: MixpanelSource, removeFromDownloadsEnabled: Boolean, removeFromQueueEnabled: Boolean, removeFromQueueIndex: Int?): SlideUpMenuMusicFragment {
            return SlideUpMenuMusicFragment().also {
                it.music = music
                it.externalMixpanelSource = externalMixpanelSource
                it.removeFromDownloadsEnabled = removeFromDownloadsEnabled
                it.removeFromQueueEnabled = removeFromQueueEnabled
                it.removeFromQueueIndex = removeFromQueueIndex
            }
        }
    }
}
