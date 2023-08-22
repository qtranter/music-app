package com.audiomack.ui.player.full

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Point
import android.graphics.Rect
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import androidx.lifecycle.Observer
import androidx.mediarouter.media.MediaRouteSelector.Builder
import com.audiomack.BuildConfig
import com.audiomack.R
import com.audiomack.data.reachability.Reachability
import com.audiomack.data.tracking.mixpanel.MixpanelButtonNowPlaying
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItem.ItemImagePreset.ItemImagePresetOriginal
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.playback.PlaybackState
import com.audiomack.playback.PlaybackState.LOADING
import com.audiomack.playback.PlaybackState.PLAYING
import com.audiomack.playback.PlayerError
import com.audiomack.playback.SongAction
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.player.full.view.SongActionButton
import com.audiomack.ui.tooltip.TooltipCorner.BOTTOMRIGHT
import com.audiomack.ui.tooltip.TooltipCorner.TOPRIGHT
import com.audiomack.ui.tooltip.TooltipFragment.TooltipLocation
import com.audiomack.utils.Utils
import com.audiomack.utils.addOnPageSelectedListener
import com.audiomack.utils.confirmDownloadDeletion
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.showDownloadUnlockedToast
import com.audiomack.views.AMMediaRouteButton.CastAvailableClickListener
import com.audiomack.views.AMSnackbar
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.snackbar.Snackbar
import com.mopub.mobileads.MoPubView
import com.mopub.nativeads.AdapterHelper
import com.mopub.nativeads.NativeAd
import com.mopub.nativeads.ViewBinder
import kotlin.math.roundToInt
import kotlinx.android.synthetic.main.fragment_player.playerActionAdd
import kotlinx.android.synthetic.main.fragment_player.playerActionDownload
import kotlinx.android.synthetic.main.fragment_player.playerActionFavorite
import kotlinx.android.synthetic.main.fragment_player.playerActionRePost
import kotlinx.android.synthetic.main.fragment_player.playerActionShare
import kotlinx.android.synthetic.main.fragment_player.playerAdCloseBtn
import kotlinx.android.synthetic.main.fragment_player.playerAdContainer
import kotlinx.android.synthetic.main.fragment_player.playerAdLayout
import kotlinx.android.synthetic.main.fragment_player.playerBackground
import kotlinx.android.synthetic.main.fragment_player.playerCastBtn
import kotlinx.android.synthetic.main.fragment_player.playerHiFiBtn
import kotlinx.android.synthetic.main.fragment_player.playerLoadingView
import kotlinx.android.synthetic.main.fragment_player.playerMinimizeBtn
import kotlinx.android.synthetic.main.fragment_player.playerNativeAdContainer
import kotlinx.android.synthetic.main.fragment_player.playerNextBtn
import kotlinx.android.synthetic.main.fragment_player.playerParentTitle
import kotlinx.android.synthetic.main.fragment_player.playerPlayPauseBtn
import kotlinx.android.synthetic.main.fragment_player.playerPlayingFromLabel
import kotlinx.android.synthetic.main.fragment_player.playerPrevBtn
import kotlinx.android.synthetic.main.fragment_player.playerQueueBtn
import kotlinx.android.synthetic.main.fragment_player.playerSeekBar
import kotlinx.android.synthetic.main.fragment_player.playerTimeElapsed
import kotlinx.android.synthetic.main.fragment_player.playerTimeTotal
import kotlinx.android.synthetic.main.fragment_player.playerTrackViewPager
import kotlinx.android.synthetic.main.fragment_player.tvRemoveAds
import timber.log.Timber

class PlayerFragment : TrackedFragment(R.layout.fragment_player, TAG) {

    private lateinit var playerViewModel: PlayerViewModel

    private var mopubAdView: MoPubView? = null
    private var mopubNativeAd: NativeAd? = null
    private var adExitAnimation: Animation? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerViewModel = (requireActivity() as HomeActivity).playerViewModel

        initViews()
        initObservers()
        initClickListeners()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.let {
            CastButtonFactory.setUpMediaRouteButton(it.applicationContext, playerCastBtn)
            it.volumeControlStream = AudioManager.STREAM_MUSIC
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adExitAnimation?.cancel()
    }

    private fun initViews() {
        initViewPager()
        initCastButton()
        initAdViews()
    }

    private fun initViewPager() {
        playerTrackViewPager.apply {
            offscreenPageLimit = 2
            addOnPageSelectedListener { position ->
                post {
                    playerViewModel.onTrackSelected(position)
                }
            }
        }.also {
            playerBackground.connectTo(it)
        }
    }

    private fun initCastButton() {
        val routeSelector = Builder()
            .addControlCategory(
                CastMediaControlIntent.categoryForCast(BuildConfig.AM_CHROMECAST_RECEIVER_APP_ID)
            )
            .build()
        playerCastBtn.apply {
            setRouteSelector(routeSelector)
            setAlwaysVisible(true)
            castAvailableClickListener = object : CastAvailableClickListener {
                override fun onCastAvailable(available: Boolean) {
                    if (!available) {
                        AMSnackbar.Builder(activity)
                            .withTitle(getString(R.string.cast_unavailable))
                            .withSubtitle(getString(R.string.please_restart_app))
                            .withDrawable(R.drawable.ic_snackbar_error)
                            .show()
                    } else {
                        showDialog()
                    }
                }
            }
        }
    }

    private fun initAdViews() {
        view?.doOnLayout {
            // View width minus internal padding and external margin
            playerNativeAdContainer.layoutParams.width = it.width - playerNativeAdContainer.marginLeft - playerNativeAdContainer.marginEnd - playerNativeAdContainer.context.convertDpToPixel(20F)
            // Fixed height content plus image size based on a fixed aspect ratio
            playerNativeAdContainer.layoutParams.height = (playerNativeAdContainer.context.convertDpToPixel(130F) + playerNativeAdContainer.layoutParams.width / 1.905F).toInt()
        }
    }

    private fun initObservers() {
        playerViewModel.apply {
            parentTitle.observe(viewLifecycleOwner, screenTitleObserver)
            isHiFi.observe(viewLifecycleOwner, isHiFiObserver)
            repostVisible.observe(viewLifecycleOwner, repostVisibleObserver)
            favoriteAction.observe(viewLifecycleOwner, ActionObserver(playerActionFavorite))
            addToPlaylistAction.observe(viewLifecycleOwner, ActionObserver(playerActionAdd))
            rePostAction.observe(viewLifecycleOwner, ActionObserver(playerActionRePost))
            downloadAction.observe(viewLifecycleOwner, ActionObserver(playerActionDownload))
            shareAction.observe(viewLifecycleOwner, ActionObserver(playerActionShare))
            showPodcastControls.observe(viewLifecycleOwner, podcastControlsObserver)
            castEnabled.observe(viewLifecycleOwner) { playerCastBtn.isEnabled = it }

            songList.observe(viewLifecycleOwner, songQueueObserver)
            currentIndex.observe(viewLifecycleOwner, songQueueIndexObserver)
            playbackState.observe(viewLifecycleOwner, playbackObserver)
            currentPosition.observe(viewLifecycleOwner, currentPositionObserver)
            duration.observe(viewLifecycleOwner, durationObserver)
            volumeData.observe(viewLifecycleOwner, volumeDataObserver)
            nextButtonEnabled.observe(viewLifecycleOwner, nextButtonEnabledObserver)

            requestPlaylistTooltipEvent.observe(viewLifecycleOwner, playlistTooltipEventObserver)
            requestQueueTooltipEvent.observe(viewLifecycleOwner, queueTooltipEventObserver)
            errorEvent.observe(viewLifecycleOwner, Observer { onError(it) })

            adClosedEvent.observe(viewLifecycleOwner, adClosedEventObserver)
            showNativeAdEvent.observe(viewLifecycleOwner, showNativeAdObserver)
            showAdEvent.observe(viewLifecycleOwner, showAdObserver)

            downloadClickEvent.observe(viewLifecycleOwner, downloadObserver)
            retryDownloadEvent.observe(viewLifecycleOwner, retryDownloadObserver)

            showConfirmDownloadDeletionEvent.observe(viewLifecycleOwner, showConfirmDownloadDeletionEventObserver)
            showPremiumDownloadEvent.observe(viewLifecycleOwner, showPremiumDownloadEventObserver)
            showUnlockedToastEvent.observe(viewLifecycleOwner, showUnlockedToastEventObserver)
        }
    }

    private fun initClickListeners() {
        playerPlayPauseBtn.setOnClickListener { playerViewModel.onPlayPauseClick() }
        playerSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        playerPrevBtn.setOnClickListener { playerViewModel.onSkipBackClick() }
        playerNextBtn.setOnClickListener { playerViewModel.onSkipForwardClick() }

        playerMinimizeBtn.setOnClickListener { playerViewModel.onMinimizeClick() }
        playerQueueBtn.setOnClickListener { playerViewModel.onQueueClick() }

        playerHiFiBtn.setOnClickListener { playerViewModel.onHiFiClick() }
        playerActionFavorite.setOnClickListener { onFavoriteClick() }
        playerActionAdd.setOnClickListener { playerViewModel.onAddToPlaylistClick() }
        playerActionRePost.setOnClickListener { playerViewModel.onRePostClick() }
        playerActionDownload.setOnClickListener { playerViewModel.onDownloadClick() }
        playerActionShare.setOnClickListener { playerViewModel.onShareClick() }

        tvRemoveAds.setOnClickListener { playerViewModel.onRemoveAdsClick() }
        playerAdCloseBtn.setOnClickListener { playerViewModel.onCloseAdClick() }

        playerParentTitle.setOnClickListener { playerViewModel.onParentClick() }
        playerPlayingFromLabel.setOnClickListener { playerViewModel.onParentClick() }
    }

    private val seekBarChangeListener = object : OnSeekBarChangeListener {
        var isDragging = false

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            playerTimeElapsed.text = Utils.timeFromMilliseconds(progress.toLong())
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            isDragging = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            isDragging = false
            playerViewModel.onTouchSeek(seekBar.progress)
        }
    }

    private val screenTitleObserver = Observer<String> { title ->
        toggleScreenTitleVisibility(!title.isNullOrBlank())
        playerParentTitle.text = title
    }

    private val isHiFiObserver = Observer<Boolean> { premium ->
        val bg = if (premium) R.drawable.bg_hifi_on else R.drawable.bg_hifi_off
        playerHiFiBtn.setBackgroundResource(bg)
    }

    private val repostVisibleObserver = Observer<Boolean> { visible ->
        playerActionRePost.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val songQueueObserver = Observer<List<AMResultItem>> { items ->
        playerBackground.imageUrls =
            items.mapNotNull { it.getImageURLWithPreset(ItemImagePresetOriginal) }
        playerTrackViewPager.adapter = PlayerSongPagerAdapter(
            items,
            onArtistClick = { playerViewModel.onArtistClick(it) },
            onArtworkClick = { playerViewModel.onArtworkClick(it) }
        )
    }

    private val songQueueIndexObserver = Observer<Int> { index ->
        playerTrackViewPager.setCurrentItem(index, true)
    }

    private val playbackObserver = Observer<PlaybackState> { state ->
        view?.post {
            if (view == null) {
                return@post
            }
            when (state) {
                PLAYING -> showPlaying()
                LOADING -> showLoading()
                else -> showPaused()
            }
        }
    }

    private val currentPositionObserver = Observer<Long> { position ->
        if (!seekBarChangeListener.isDragging && playerViewModel.playbackState.value != LOADING) {
            view?.post {
                playerSeekBar?.progress = position.toInt()
            }
        }
    }

    private val durationObserver = Observer<Long> { duration ->
        playerTimeTotal.text = Utils.timeFromMilliseconds(duration)
        playerSeekBar.max = duration.toInt()
        playerViewModel.currentPosition.value?.let { playerSeekBar.progress = it.toInt() }
    }

    private val volumeDataObserver = Observer<IntArray> { volumeData ->
        view?.post {
            if (view == null) {
                return@post
            }
            playerSeekBar.setVolumeData(volumeData)
            playerTimeElapsed.animate().alpha(1f)
            playerTimeTotal.animate().alpha(1f)
        }
    }

    private val playlistTooltipEventObserver = Observer<Void> {
        val rect = Rect()
        playerActionAdd.getGlobalVisibleRect(rect)
        val target = Point(
            rect.left + playerActionAdd.width / 2,
            rect.top - playerActionAdd.context.convertDpToPixel(2f)
        )
        playerViewModel.setPlaylistTooltipLocation(TooltipLocation(TOPRIGHT, target))
    }

    private val queueTooltipEventObserver = Observer<Void> {
        val rect = Rect()
        playerQueueBtn.getGlobalVisibleRect(rect)
        val target = Point(rect.left + playerQueueBtn.width / 2, rect.top)
        playerViewModel.setQueueTooltipLocation(TooltipLocation(BOTTOMRIGHT, target))
    }

    private val nextButtonEnabledObserver = Observer<Boolean> {
        playerNextBtn.isEnabled = it
    }

    private val adClosedEventObserver = Observer<Boolean> { foreground ->
        animateAdViewExit()
        if (foreground) {
            mopubAdView = null
            mopubNativeAd = null
        }
    }

    private val showNativeAdObserver = Observer<Pair<NativeAd, AdapterHelper>> {
        Timber.tag(TAG).d("showNativeAdObserver: observed")
        val mopubNativeAd = it.first
        val nativeAdsAdapterHelper = it.second

        try {
            this.mopubNativeAd = mopubNativeAd

            val adView = nativeAdsAdapterHelper.getAdView(
                null,
                playerNativeAdContainer,
                mopubNativeAd,
                ViewBinder.Builder(0).build()
            )
            adView.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            playerNativeAdContainer.addView(adView)

            playerNativeAdContainer.visibility = View.VISIBLE
            playerAdContainer.visibility = View.GONE
            animateAdViewEnter()
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private val showAdObserver = Observer<MoPubView> {
        Timber.tag(TAG).d("showAdObserver: observed")
        try {
            mopubAdView = it
            playerAdContainer.addView(it)
            playerNativeAdContainer.visibility = View.GONE
            playerAdContainer.visibility = View.VISIBLE
            animateAdViewEnter()
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private val downloadObserver = Observer<AMResultItem> { item ->
        downloadItem(item, false)
    }

    private val retryDownloadObserver = Observer<AMResultItem> { item ->
        downloadItem(item, true)
    }

    private val podcastControlsObserver = Observer<Boolean> { isPodcast ->
        playerPrevBtn.setImageResource(if (isPodcast) R.drawable.ic_skip_back_15 else R.drawable.player_prev)
        playerNextBtn.setImageResource(if (isPodcast) R.drawable.ic_skip_forward_30 else R.drawable.player_next)
    }

    private val showConfirmDownloadDeletionEventObserver = Observer<AMResultItem> { music ->
        confirmDownloadDeletion(music)
    }

    private val showPremiumDownloadEventObserver = Observer<PremiumDownloadModel> { model ->
        HomeActivity.instance?.requestPremiumDownloads(model)
    }

    private val showUnlockedToastEventObserver = Observer<String> { musicName ->
        showDownloadUnlockedToast(musicName)
    }

    private fun downloadItem(item: AMResultItem, retry: Boolean) {
        context?.let {
            if (Reachability.getInstance().networkAvailable) {
                playerViewModel.startDownload(item, MixpanelButtonNowPlaying, retry)
            } else {
                AMSnackbar.Builder(activity)
                    .withTitle(getString(R.string.player_file_error_download_again))
                    .withSubtitle(getString(R.string.please_check_connection_try_download_again))
                    .withDrawable(R.drawable.ic_snackbar_download_failure)
                    .show()
            }
        }
    }

    private fun onFavoriteClick() {
        if (!playerViewModel.isFavorited()) {
            val heartView =
                playerActionFavorite.findViewById<View>(R.id.playerActionBtnContentImage)
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(heartView, View.SCALE_X, 1f, 1.3f, 1f, 1.3f, 1f),
                    ObjectAnimator.ofFloat(heartView, View.SCALE_Y, 1f, 1.3f, 1f, 1.3f, 1f)
                )
                duration = 500L
                start()
            }
        }
        playerViewModel.onFavoriteClick()
    }

    private fun toggleScreenTitleVisibility(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        playerPlayingFromLabel.visibility = visibility
        playerParentTitle.visibility = visibility
    }

    private fun showPaused() {
        playerPlayPauseBtn.apply {
            setImageResource(R.drawable.ic_player_play)
            isEnabled = true
        }
        playerLoadingView.visibility = View.GONE
    }

    private fun showPlaying() {
        playerPlayPauseBtn.apply {
            setImageResource(R.drawable.ic_player_pause)
            isEnabled = true
        }
        playerLoadingView.visibility = View.GONE
    }

    private fun showLoading() {
        playerPlayPauseBtn.isEnabled = false
        playerLoadingView.visibility = View.VISIBLE
    }

    private fun onError(error: PlayerError) {
        Timber.tag(TAG).e(error.throwable)
        val title = when (error) {
            is PlayerError.Resource -> getString(R.string.player_file_error)
            is PlayerError.Storage -> getString(R.string.player_storage_error)
            is PlayerError.Playback -> getString(R.string.player_playback_error)
            is PlayerError.Queue -> getString(R.string.player_queue_error)
            is PlayerError.Action -> getString(R.string.generic_api_error)
            is PlayerError.Seek -> getString(R.string.seeking_unsupported)
        }
        val subTitle = when (error) {
            is PlayerError.Seek, is PlayerError.Storage -> null
            else -> getString(R.string.please_try_again_later)
        }
        AMSnackbar.Builder(activity)
            .withTitle(title)
            .withDrawable(R.drawable.ic_snackbar_error)
            .withDuration(Snackbar.LENGTH_SHORT)
            .apply {
                subTitle?.let { withSubtitle(it) }
            }
            .show()
    }

    private fun animateAdViewEnter() {
        if (playerAdLayout?.isVisible == true) return

        playerAdLayout.visibility = View.VISIBLE

        val startValue = playerAdLayout.marginTop.toFloat()
        val endValue = playerAdLayout.context.convertDpToPixel(50f).toFloat()
        object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                playerAdLayout.alpha = interpolatedTime
                (playerAdLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
                    this.topMargin = (startValue + (endValue - startValue) * interpolatedTime).roundToInt()
                    playerAdLayout.requestLayout()
                }
            }
        }.apply {
            duration = AD_ANIMATION_DURATION
            interpolator = AccelerateDecelerateInterpolator()
        }.also { playerAdLayout.startAnimation(it) }
    }

    private fun animateAdViewExit() {
        if (playerAdLayout?.isGone == true) return

        val startValue = playerAdLayout.marginTop.toFloat()
        val endValue = 0.coerceAtLeast(playerTrackViewPager.height - playerAdLayout.height).toFloat() / 2f
        object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                playerAdLayout.alpha = 1F - interpolatedTime
                (playerAdLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
                    this.topMargin = (startValue + (endValue - startValue) * interpolatedTime).roundToInt()
                    playerAdLayout.requestLayout()
                }
            }
        }.apply {
            duration = AD_ANIMATION_DURATION
            interpolator = AccelerateInterpolator()
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    playerAdLayout?.visibility = View.GONE
                    playerAdContainer?.removeAllViews()
                    playerNativeAdContainer?.removeAllViews()
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }.also {
            playerAdLayout.startAnimation(it)
            adExitAnimation = it
        }
    }

    inner class ActionObserver(private val button: SongActionButton) : Observer<SongAction> {
        override fun onChanged(action: SongAction?) {
            view?.post {
                button.action = action
            }
        }
    }

    // Static

    companion object {
        private const val TAG = "PlayerFragment"
        private const val AD_ANIMATION_DURATION = 200L

        fun newInstance() = PlayerFragment()
    }
}
