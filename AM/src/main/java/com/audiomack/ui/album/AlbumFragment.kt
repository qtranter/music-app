package com.audiomack.ui.album

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.tracking.mixpanel.MixpanelButtonAlbumDetails
import com.audiomack.data.tracking.mixpanel.MixpanelButtonKebabMenu
import com.audiomack.data.tracking.mixpanel.MixpanelPageAlbumDetails
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMResultItem
import com.audiomack.model.Action
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.model.SearchType
import com.audiomack.playback.ActionState.ACTIVE
import com.audiomack.playback.ActionState.DISABLED
import com.audiomack.playback.SongAction
import com.audiomack.playback.SongAction.Share
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.home.HomeViewModel
import com.audiomack.ui.mylibrary.offline.local.menu.SlideUpMenuLocalMediaFragment
import com.audiomack.ui.player.full.view.SongActionButton
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.audiomack.ui.settings.OptionsMenuFragment
import com.audiomack.ui.slideupmenu.music.SlideUpMenuMusicFragment
import com.audiomack.ui.tooltip.TooltipCorner
import com.audiomack.ui.tooltip.TooltipFragment
import com.audiomack.utils.askFollowNotificationPermissions
import com.audiomack.utils.confirmDownloadDeletion
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.openUrlInAudiomack
import com.audiomack.utils.showDownloadUnlockedToast
import com.audiomack.utils.showFavoritedToast
import com.audiomack.utils.showFollowedToast
import com.audiomack.utils.showLoggedOutAlert
import com.audiomack.utils.showOfflineAlert
import com.audiomack.utils.showRepostedToast
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressHUD
import com.audiomack.views.AMRecyclerViewTopSpaceDecoration
import kotlin.math.min
import kotlinx.android.synthetic.main.fragment_album.actionDownload
import kotlinx.android.synthetic.main.fragment_album.actionFavorite
import kotlinx.android.synthetic.main.fragment_album.actionRePost
import kotlinx.android.synthetic.main.fragment_album.actionShare
import kotlinx.android.synthetic.main.fragment_album.buttonBack
import kotlinx.android.synthetic.main.fragment_album.buttonInfo
import kotlinx.android.synthetic.main.fragment_album.buttonPlayAll
import kotlinx.android.synthetic.main.fragment_album.buttonShuffle
import kotlinx.android.synthetic.main.fragment_album.buttonViewComment
import kotlinx.android.synthetic.main.fragment_album.imageView
import kotlinx.android.synthetic.main.fragment_album.imageViewBlurredTop
import kotlinx.android.synthetic.main.fragment_album.imageViewSmall
import kotlinx.android.synthetic.main.fragment_album.recyclerView
import kotlinx.android.synthetic.main.fragment_album.shadowImageView
import kotlinx.android.synthetic.main.fragment_album.sizingViewBis
import kotlinx.android.synthetic.main.fragment_album.topView
import kotlinx.android.synthetic.main.fragment_album.tvArtist
import kotlinx.android.synthetic.main.fragment_album.tvFeat
import kotlinx.android.synthetic.main.fragment_album.tvTitle
import kotlinx.android.synthetic.main.fragment_album.tvTopAlbumTitle
import kotlinx.android.synthetic.main.fragment_album.tvTopArtistTitle
import kotlinx.android.synthetic.main.fragment_album.upperLayout

class AlbumFragment : TrackedFragment(R.layout.fragment_album, TAG) {

    lateinit var album: AMResultItem
    private var openShare: Boolean = false
    private lateinit var viewModel: AlbumViewModel
    private lateinit var homeViewModel: HomeViewModel
    private var adapter: AlbumTracksAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!::album.isInitialized) {
            activity?.onBackPressed()
            return
        }

        viewModel = ViewModelProvider(this, AlbumViewModelFactory(
            album,
            mixpanelSource,
            openShare
        )).get(AlbumViewModel::class.java)

        initObservers()
        initClickListeners()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        homeViewModel = (requireActivity() as HomeActivity).homeViewModel
    }

    private fun initClickListeners() {
        buttonBack.setOnClickListener { viewModel.onBackTapped() }
        buttonInfo.setOnClickListener { viewModel.onInfoTapped() }
        buttonPlayAll.setOnClickListener { viewModel.onPlayAllTapped() }
        buttonShuffle.setOnClickListener { viewModel.onShuffleTapped() }
        actionShare.setOnClickListener { viewModel.onShareTapped() }
        actionFavorite.setOnClickListener { onFavoriteClick() }
        actionRePost.setOnClickListener { viewModel.onRepostTapped() }
        actionDownload.setOnClickListener { viewModel.onDownloadTapped() }
        buttonViewComment.setOnClickListener { viewModel.onCommentsTapped() }
    }

    private fun initObservers() {
        viewModel.apply {
            title.observe(viewLifecycleOwner, titleObserver)
            artist.observe(viewLifecycleOwner, artistObserver)
            feat.observe(viewLifecycleOwner, featObserver)
            featVisible.observe(viewLifecycleOwner, featVisibleObserver)
            followStatus.observe(viewLifecycleOwner, followStatusObserver)
            followVisible.observe(viewLifecycleOwner, followVisibleObserver)
            repostVisible.observe(viewLifecycleOwner, repostVisibleObserver)
            highResImage.observe(viewLifecycleOwner, highResImageObserver)
            lowResImage.observe(viewLifecycleOwner, lowResImageObserver)
            playButtonActive.observe(viewLifecycleOwner, playButtonActiveObserver)
            commentsCount.observe(viewLifecycleOwner, commentsCountObserver)
            enableCommentsButton.observe(viewLifecycleOwner) { buttonViewComment.isEnabled = it }
            enableShareButton.observe(viewLifecycleOwner) { enabled ->
                actionShare.action = Share(if (enabled) ACTIVE else DISABLED)
            }
            showInfoButton.observe(viewLifecycleOwner) { buttonInfo.isVisible = it }
            showUploader.observe(viewLifecycleOwner) { buttonInfo.isVisible = it }

            setupTracksEvent.observe(viewLifecycleOwner, setupTracksEventObserver)
            closeEvent.observe(viewLifecycleOwner, closeEventObserver)
            notifyFollowToastEvent.observe(viewLifecycleOwner, notifyFollowToastObserver)
            notifyOfflineEvent.observe(viewLifecycleOwner, offlineAlertObserver)
            notifyRepostEvent.observe(viewLifecycleOwner, notifyRepostEventObserver)
            notifyFavoriteEvent.observe(viewLifecycleOwner, notifyFavoriteEventObserver)
            showErrorEvent.observe(viewLifecycleOwner, showErrorEventObserver)
            loginRequiredEvent.observe(viewLifecycleOwner, loggedOutAlertObserver)
            openUploaderEvent.observe(viewLifecycleOwner, openUploaderEventObserver)
            downloadTooltipEvent.observe(viewLifecycleOwner, downloadTooltipEventObserver)
            openMusicInfoEvent.observe(viewLifecycleOwner, openMusicInfoEventObserver)
            scrollEvent.observe(viewLifecycleOwner, scrollEventObserver)
            shuffleEvent.observe(viewLifecycleOwner, shuffleEventObserver)
            shareEvent.observe(viewLifecycleOwner, shareEventObserver)
            georestrictedMusicClickedEvent.observe(viewLifecycleOwner, georestrictedMusicClickedEventObserver)
            openTrackEvent.observe(viewLifecycleOwner, openTrackEventObserver)
            openTrackOptionsEvent.observe(viewLifecycleOwner, openTrackOptionsEventObserver)
            openTrackOptionsFailedDownloadEvent.observe(viewLifecycleOwner, openTrackOptionsFailedDownloadEventObserver)
            openCommentsEvent.observe(viewLifecycleOwner, openCommentsEventObserver)
            reloadAdapterTracksEvent.observe(viewLifecycleOwner, reloadAdapterTracksEventObserver)
            reloadAdapterTracksRangeEvent.observe(viewLifecycleOwner, reloadAdapterTracksRangeEventObserver)
            reloadAdapterTrackEvent.observe(viewLifecycleOwner, reloadAdapterTrackEventObserver)
            removeTrackFromAdapterEvent.observe(viewLifecycleOwner, removeTrackFromAdapterEventObserver)
            premiumRequiredEvent.observe(viewLifecycleOwner, premiumRequiredEventObserver)
            showHUDEvent.observe(viewLifecycleOwner, showHUDEventObserver)
            showConfirmDownloadDeletionEvent.observe(viewLifecycleOwner, showConfirmDownloadDeletionEventObserver)
            showPremiumDownloadEvent.observe(viewLifecycleOwner, showPremiumDownloadEventObserver)
            showUnlockedToastEvent.observe(viewLifecycleOwner, showUnlockedToastEventObserver)
            promptNotificationPermissionEvent.observe(viewLifecycleOwner, promptNotificationPermissionEventObserver)
            genreEvent.observe(viewLifecycleOwner, genreEventObserver)
            tagEvent.observe(viewLifecycleOwner, tagEventObserver)
            adapterTracksChangedEvent.observe(viewLifecycleOwner) { adapter?.updateTracks(it) }

            favoriteAction.observe(viewLifecycleOwner, ActionObserver(actionFavorite))
            rePostAction.observe(viewLifecycleOwner, ActionObserver(actionRePost))
            downloadAction.observe(viewLifecycleOwner, ActionObserver(actionDownload))
        }
    }

    private fun onFavoriteClick() {
        if (!viewModel.isAlbumFavorited) {
            val heartView =
                actionFavorite.findViewById<View>(R.id.playerActionBtnContentImage)
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

    private val titleObserver = Observer<String> { title ->
        tvTitle.text = title
        tvTopAlbumTitle.text = title
    }

    private val artistObserver = Observer<String> { artist ->
        tvArtist.text = artist
        tvTopArtistTitle.text = artist
    }

    private val featObserver = Observer<String> { feat ->
        val fullString = String.format("%s %s", getString(R.string.feat), feat)
        tvFeat.text = tvFeat.context.spannableString(
            fullString = fullString,
            highlightedStrings = listOf(feat),
            highlightedColor = tvFeat.context.colorCompat(R.color.orange),
            highlightedFont = R.font.opensans_semibold
        )
    }

    private val featVisibleObserver = Observer<Boolean> { visible ->
        tvFeat.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val followStatusObserver = Observer<Boolean> { followed ->
        adapter?.updateFollowStatus(followed)
    }

    private val followVisibleObserver = Observer<Boolean> { visible ->
        adapter?.updateFollowVisibility(visible)
    }

    private val repostVisibleObserver = Observer<Boolean> { visible ->
        actionRePost.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val highResImageObserver = Observer<String> { image ->
        viewModel.imageLoader.load(
            imageViewSmall.context,
            image,
            imageViewSmall,
            R.drawable.ic_artwork
        )
    }

    private val lowResImageObserver = Observer<String> { image ->
        viewModel.compositeDisposable.add(
            viewModel.imageLoader.loadAndBlur(imageView.context, image)
                .subscribeOn(viewModel.schedulersProvider.main)
                .observeOn(viewModel.schedulersProvider.main)
                .subscribe { blurredBitmap ->
                    imageView.setImageBitmap(blurredBitmap)
                    imageViewBlurredTop.setImageBitmap(blurredBitmap)
                }
        )
    }

    private val playButtonActiveObserver = Observer<Boolean> { active ->
        buttonPlayAll.setText(if (active) R.string.album_pause else R.string.album_play)
        buttonPlayAll.setCompoundDrawablesWithIntrinsicBounds(buttonShuffle.context.drawableCompat(
            if (active) R.drawable.artists_onboarding_playlist_pause else R.drawable.artists_onboarding_playlist_play
        ), null, null, null)
    }

    private val commentsCountObserver = Observer<Int> { count ->
        buttonViewComment.commentsCount = count
        adapter?.updateCollection(album)
    }

    private val setupTracksEventObserver = Observer<AMResultItem> { album ->
        adapter = AlbumTracksAdapter(
            album,
            album.tracks ?: arrayListOf(),
            viewModel.followVisible.value,
            viewModel.followStatus.value,
            mixpanelSource.isInMyDownloads,
            viewModel
        )
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        upperLayout.doOnLayout {
            updateRecyclerViewSpacing()
            viewModel.onLayoutReady()
        }
    }

    private fun updateRecyclerViewSpacing() {
        if (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }
        recyclerView.addItemDecoration(AMRecyclerViewTopSpaceDecoration(upperLayout.measuredHeight))
        recyclerView.setPadding(0, 0, 0, recyclerView.context.convertDpToPixel(if (viewModel.adsVisible) 80F else 20F))
        recyclerView.listener = viewModel
        viewModel.recyclerviewConfigured = true
        handleScroll()
        recyclerView.post { recyclerView?.visibility = View.VISIBLE }
    }

    private fun handleScroll() {
        val maxScrollY = upperLayout.height - topView.height
        var scrollY = recyclerView.offsetY
        if (scrollY >= maxScrollY) {
            scrollY = maxScrollY
            if (viewModel.recyclerviewConfigured) {
                shadowImageView.visibility = View.VISIBLE
                topView.visibility = View.VISIBLE
                tvTopArtistTitle.visibility = View.VISIBLE
                tvTopAlbumTitle.visibility = View.VISIBLE
                upperLayout.visibility = View.GONE
            }
        } else {
            if (viewModel.recyclerviewConfigured) {
                shadowImageView.visibility = View.INVISIBLE
                topView.visibility = View.INVISIBLE
                tvTopArtistTitle.visibility = View.INVISIBLE
                tvTopAlbumTitle.visibility = View.INVISIBLE
                upperLayout.visibility = View.VISIBLE
            }
        }

        val upperLayoutLayoutParams = upperLayout.layoutParams as FrameLayout.LayoutParams
        if (upperLayoutLayoutParams.topMargin != -scrollY) {
            upperLayoutLayoutParams.topMargin = -scrollY
            upperLayout.layoutParams = upperLayoutLayoutParams
        }

        val imageViewSmallLayoutParams = imageViewSmall.layoutParams as ConstraintLayout.LayoutParams
        val newTopMargin = min(scrollY, (sizingViewBis.height * 0.6f).toInt())
        if (newTopMargin != imageViewSmallLayoutParams.topMargin) {
            imageViewSmallLayoutParams.topMargin = newTopMargin
            imageViewSmall.layoutParams = imageViewSmallLayoutParams
        }
    }

    private val closeEventObserver = Observer<Void> {
        activity?.onBackPressed()
    }

    private val notifyFollowToastObserver = Observer<ToggleFollowResult.Notify> { notify ->
        showFollowedToast(notify)
    }

    private val offlineAlertObserver = Observer<Void> { showOfflineAlert() }

    private val notifyRepostEventObserver = Observer<ToggleRepostResult.Notify> {
        activity?.showRepostedToast(it)
    }

    private val notifyFavoriteEventObserver = Observer<ToggleFavoriteResult.Notify> {
        activity?.showFavoritedToast(it)
    }

    private val showErrorEventObserver = Observer<String> {
        AMProgressHUD.showWithError(activity, it)
    }

    private val loggedOutAlertObserver = Observer<LoginSignupSource> { source ->
        showLoggedOutAlert(source)
    }

    private val openUploaderEventObserver = Observer<String> { uploaderSlug ->
        homeViewModel.onArtistScreenRequested(uploaderSlug)
    }

    private val downloadTooltipEventObserver = Observer<Void> {
        val rect = Rect()
        actionDownload.getGlobalVisibleRect(rect)
        val targetPoint = Point(rect.left + rect.width() / 2, rect.top)
        val tooltipFragment = TooltipFragment.newInstance(
            getString(R.string.tooltip_albums),
            R.drawable.tooltip_albums,
            TooltipCorner.TOPRIGHT,
            arrayListOf(targetPoint),
            Runnable {}
        )
        (activity as? HomeActivity)?.openTooltipFragment(tooltipFragment)
    }

    private val openMusicInfoEventObserver = Observer<AMResultItem> { album ->
        homeViewModel.onMusicInfoTapped(album)
    }

    private val scrollEventObserver = Observer<Void> { handleScroll() }

    private val shuffleEventObserver = Observer<Pair<AMResultItem, AMResultItem>> { (track, album) ->
        homeViewModel.onMaximizePlayerRequested(
            MaximizePlayerData(
                item = track,
                collection = album,
                albumPlaylistIndex = 0,
                inOfflineScreen = mixpanelSource.isInMyDownloads,
                mixpanelSource = mixpanelSource.apply { shuffled = true },
                shuffle = true
            )
        )
    }

    private val shareEventObserver = Observer<AMResultItem> { album ->
        album.openShareSheet(activity, album.mixpanelSource ?: MixpanelSource.empty, MixpanelButtonAlbumDetails)
    }

    private val georestrictedMusicClickedEventObserver = Observer<Void> {
        homeViewModel.onGeorestrictedMusicClicked()
    }

    private val openTrackOptionsEventObserver = Observer<AMResultItem> { track ->
        val fragment = if (track.isLocal) {
            SlideUpMenuLocalMediaFragment.newInstance(track.itemId.toLong())
        } else {
            SlideUpMenuMusicFragment.newInstance(track, mixpanelSource, false, false, null)
        }

        (activity as? BaseActivity)?.openOptionsFragment(fragment)
    }

    private val openTrackEventObserver = Observer<Triple<AMResultItem, AMResultItem?, Int>> { (track, album, index) ->
        homeViewModel.onMaximizePlayerRequested(
            MaximizePlayerData(
                item = track,
                collection = album,
                inOfflineScreen = mixpanelSource.isInMyDownloads,
                albumPlaylistIndex = index,
                mixpanelSource = mixpanelSource
            )
        )
    }

    private val openTrackOptionsFailedDownloadEventObserver = Observer<AMResultItem> { track ->
        val actions = listOf(
            Action(getString(R.string.options_retry_download), object : Action.ActionListener {
                override fun onActionExecuted() {
                    (activity as? BaseActivity)?.popFragment()
                    viewModel.onTrackDownloadTapped(track, MixpanelButtonKebabMenu)
                }
            }),
            Action(getString(R.string.options_delete_download), object : Action.ActionListener {
                override fun onActionExecuted() {
                    (activity as? BaseActivity)?.popFragment()
                    viewModel.onRemoveTrackFromAdapter(track)
                    track.deepDelete()
                }
            })
        )
        (activity as? BaseActivity)?.openOptionsFragment(OptionsMenuFragment.newInstance(actions))
    }

    private val openCommentsEventObserver = Observer<AMResultItem> { album ->
        (activity as? HomeActivity)?.openComments(album, null, null)
    }

    private val reloadAdapterTracksEventObserver = Observer<Void> {
        adapter?.notifyItemRangeChanged(0, adapter?.itemCount ?: 0)
    }

    private val reloadAdapterTracksRangeEventObserver = Observer<List<Int>> {
        val min = it.min() ?: 0
        val max = it.max() ?: 0
        adapter?.notifyItemRangeChanged(min, max - min + 1)
    }

    private val reloadAdapterTrackEventObserver = Observer<Int> { position ->
        adapter?.notifyItemChanged(position)
    }

    private val removeTrackFromAdapterEventObserver = Observer<AMResultItem> { track ->
        val extraHeight = getExtraHeight()

        adapter?.removeItem(track)?.let { itemRemoved ->
            if (itemRemoved) {
                recyclerView.reduceOffsetYBy(extraHeight)
            }
        }
    }

    private fun getExtraHeight(): Int {
        var extraHeight = 0
        (recyclerView.layoutManager as? LinearLayoutManager)?.apply {
            if (findLastCompletelyVisibleItemPosition() == itemCount - 1) {
                if (itemCount >= 2) {
                    extraHeight = getChildAt(0)?.height ?: 0
                }
            }

            if (findLastCompletelyVisibleItemPosition() != itemCount - 1 &&
                    findLastVisibleItemPosition() == itemCount - 1
            ) {
                val firstItemHeight = getChildAt(0)?.height ?: 0
                val lastItemHeight = getChildAt(childCount - 1)?.height ?: 0
                val normalItemHeights = firstItemHeight * (itemCount - 1)
                val totalScrollHeight = normalItemHeights + lastItemHeight +
                        upperLayout.height - recyclerView.height

                // the 80dp is the magic number here, try to find if it belongs to a view
                val maxOffsetY = totalScrollHeight + requireContext().convertDpToPixel(80f)

                val difference = maxOffsetY - recyclerView.offsetY
                if (firstItemHeight > 0 && difference in 1..firstItemHeight) {
                    extraHeight = firstItemHeight - difference
                }
            }
        }

        return extraHeight
    }

    private val premiumRequiredEventObserver = Observer<InAppPurchaseMode> { mode ->
        InAppPurchaseActivity.show(activity, mode)
    }

    private val showHUDEventObserver = Observer<ProgressHUDMode> { mode ->
        AMProgressHUD.show(activity, mode)
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

    private val promptNotificationPermissionEventObserver = Observer<PermissionRedirect> {
        askFollowNotificationPermissions(it)
    }

    private val genreEventObserver = Observer<String> {
        context?.openUrlInAudiomack("audiomack://music_${it}_trending")
        (activity as? HomeActivity)?.playerViewModel?.onMinimizeClick()
    }

    private val tagEventObserver = Observer<String> {
        (activity as? HomeActivity)?.openSearch(it, SearchType.Tag)
    }

    private val mixpanelSource: MixpanelSource
        get() = album.mixpanelSource ?: MixpanelSource(MainApplication.currentTab, MixpanelPageAlbumDetails)

    inner class ActionObserver(private val button: SongActionButton) : Observer<SongAction> {
        override fun onChanged(action: SongAction?) {
            view?.post {
                button.action = action
            }
        }
    }

    // Read arguments

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openShare = requireArguments().getBoolean("openShare")
    }

    // Static

    companion object {
        private const val TAG = "AlbumFragment"

        fun newInstance(album: AMResultItem, externalMixpanelSource: MixpanelSource?, openShare: Boolean): AlbumFragment {
            return AlbumFragment().apply {
                album.mixpanelSource = externalMixpanelSource
                this.album = album
                arguments = bundleOf("openShare" to openShare)
            }
        }
    }
}
