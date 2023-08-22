package com.audiomack.ui.playlist.details

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.tracking.mixpanel.MixpanelButtonKebabMenu
import com.audiomack.data.tracking.mixpanel.MixpanelButtonPlaylistDetails
import com.audiomack.data.tracking.mixpanel.MixpanelPagePlaylistDetails
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMResultItem
import com.audiomack.model.Action
import com.audiomack.model.ArtistWithBadge
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.playback.SongAction
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.home.HomeViewModel
import com.audiomack.ui.player.full.view.SongActionButton
import com.audiomack.ui.playlist.edit.EditPlaylistActivity
import com.audiomack.ui.playlist.edit.EditPlaylistMode.EDIT
import com.audiomack.ui.playlist.reorder.ReorderPlaylistActivity
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.audiomack.ui.settings.OptionsMenuFragment
import com.audiomack.ui.slideupmenu.music.SlideUpMenuMusicFragment
import com.audiomack.ui.tooltip.TooltipCorner
import com.audiomack.ui.tooltip.TooltipFragment
import com.audiomack.utils.askFollowNotificationPermissions
import com.audiomack.utils.confirmDownloadDeletion
import com.audiomack.utils.confirmPlaylistDownloadDeletion
import com.audiomack.utils.confirmPlaylistSync
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.showDownloadUnlockedToast
import com.audiomack.utils.showFailedPlaylistDownload
import com.audiomack.utils.showFavoritedToast
import com.audiomack.utils.showFollowedToast
import com.audiomack.utils.showLoggedOutAlert
import com.audiomack.utils.showOfflineAlert
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressHUD
import com.audiomack.views.AMRecyclerViewTopSpaceDecoration
import com.audiomack.views.AMSnackbar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.android.synthetic.main.fragment_playlist.actionDownload
import kotlinx.android.synthetic.main.fragment_playlist.actionEdit
import kotlinx.android.synthetic.main.fragment_playlist.actionFavorite
import kotlinx.android.synthetic.main.fragment_playlist.actionShare
import kotlinx.android.synthetic.main.fragment_playlist.buttonBack
import kotlinx.android.synthetic.main.fragment_playlist.buttonFollow
import kotlinx.android.synthetic.main.fragment_playlist.buttonInfo
import kotlinx.android.synthetic.main.fragment_playlist.buttonPlayAll
import kotlinx.android.synthetic.main.fragment_playlist.buttonShuffle
import kotlinx.android.synthetic.main.fragment_playlist.buttonSync
import kotlinx.android.synthetic.main.fragment_playlist.buttonViewComment
import kotlinx.android.synthetic.main.fragment_playlist.imageView
import kotlinx.android.synthetic.main.fragment_playlist.imageViewBanner
import kotlinx.android.synthetic.main.fragment_playlist.imageViewBlurredTop
import kotlinx.android.synthetic.main.fragment_playlist.imageViewSmall
import kotlinx.android.synthetic.main.fragment_playlist.imageViewVerified
import kotlinx.android.synthetic.main.fragment_playlist.recyclerView
import kotlinx.android.synthetic.main.fragment_playlist.shadowImageView
import kotlinx.android.synthetic.main.fragment_playlist.sizingViewBis
import kotlinx.android.synthetic.main.fragment_playlist.topView
import kotlinx.android.synthetic.main.fragment_playlist.tvDescription
import kotlinx.android.synthetic.main.fragment_playlist.tvTitle
import kotlinx.android.synthetic.main.fragment_playlist.tvTopPlaylistTitle
import kotlinx.android.synthetic.main.fragment_playlist.tvUploadedBy
import kotlinx.android.synthetic.main.fragment_playlist.upperLayout
import timber.log.Timber

class PlaylistFragment : TrackedFragment(R.layout.fragment_playlist, TAG) {

    private lateinit var playlist: AMResultItem
    private var online: Boolean = false
    private var deleted: Boolean = false
    private var openShare: Boolean = false
    private lateinit var viewModel: PlaylistViewModel
    private lateinit var homeViewModel: HomeViewModel
    private var adapter: PlaylistTracksAdapter? = null

    val playlistId: String
        get() {
            return playlist.itemId ?: ""
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!::playlist.isInitialized) {
            activity?.onBackPressed()
            return
        }

        viewModel = ViewModelProvider(this,
            PlaylistViewModelFactory(
                playlist,
                online,
                deleted,
                playlist.mixpanelSource
                    ?: MixpanelSource(MainApplication.currentTab, MixpanelPagePlaylistDetails),
                openShare
            )
        ).get(PlaylistViewModel::class.java)

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
        buttonShuffle.setOnClickListener { viewModel.onShuffleTapped() }
        buttonSync.setOnClickListener { viewModel.onSyncTapped() }
        tvUploadedBy.setOnClickListener { viewModel.onUploaderTapped() }
        buttonFollow.setOnClickListener { viewModel.onFollowTapped() }
        actionShare.setOnClickListener { viewModel.onShareTapped() }
        actionFavorite.setOnClickListener { onFavoriteClick() }
        actionDownload.setOnClickListener { viewModel.onDownloadTapped() }
        actionEdit.setOnClickListener { viewModel.onEditTapped() }
        buttonViewComment.setOnClickListener { viewModel.onCommentsTapped() }
    }

    private fun initObservers() {
        viewModel.apply {
            title.observe(viewLifecycleOwner, titleObserver)
            uploader.observe(viewLifecycleOwner, uploaderObserver)
            followStatus.observe(viewLifecycleOwner, followStatusObserver)
            followVisible.observe(viewLifecycleOwner, followVisibleObserver)
            description.observe(viewLifecycleOwner, descriptionObserver)
            descriptionVisible.observe(viewLifecycleOwner, descriptionVisibleObserver)
            highResImage.observe(viewLifecycleOwner, highResImageObserver)
            lowResImage.observe(viewLifecycleOwner, lowResImageObserver)
            banner.observe(viewLifecycleOwner, bannerObserver)
            playButtonActive.observe(viewLifecycleOwner, playButtonActiveObserver)
            favoriteVisible.observe(viewLifecycleOwner, favoriteVisibleObserver)
            editVisible.observe(viewLifecycleOwner, editVisibleObserver)
            syncVisible.observe(viewLifecycleOwner, syncVisibleObserver)
            commentsCount.observe(viewLifecycleOwner, commentsCountObserver)

            setupTracksEvent.observe(viewLifecycleOwner, setupTracksEventObserver)
            closeEvent.observe(viewLifecycleOwner, backEventObserver)
            openMusicInfoEvent.observe(viewLifecycleOwner, openInfoEventObserver)
            shareEvent.observe(viewLifecycleOwner, shareEventObserver)
            showEditMenuEvent.observe(viewLifecycleOwner, showEditMenuEventObserver)
            closeOptionsEvent.observe(viewLifecycleOwner, closeOptionsEventObserver)
            openEditEvent.observe(viewLifecycleOwner, openEditEventObserver)
            openReorderEvent.observe(viewLifecycleOwner, openReorderEventObserver)
            showDeleteConfirmationEvent.observe(viewLifecycleOwner, showDeleteConfirmationEventObserver)
            deletePlaylistStatusEvent.observe(viewLifecycleOwner, deletePlaylistStatusEventObserver)
            shuffleEvent.observe(viewLifecycleOwner, shuffleEventObserver)
            openTrackEvent.observe(viewLifecycleOwner, openTrackEventObserver)
            openTrackOptionsEvent.observe(viewLifecycleOwner, openTrackOptionsEventObserver)
            openTrackOptionsFailedDownloadEvent.observe(viewLifecycleOwner, openTrackOptionsFailedDownloadEventObserver)
            openUploaderEvent.observe(viewLifecycleOwner, openUploaderEventObserver)
            showPlaylistTakenDownAlertEvent.observe(viewLifecycleOwner, showPlaylistTakenDownAlertEventObserver)
            openPlaylistEvent.observe(viewLifecycleOwner, openPlaylistEventObserver)
            createPlaylistStatusEvent.observe(viewLifecycleOwner, createPlaylistStatusEventObserver)
            performSyncEvent.observe(viewLifecycleOwner, performSyncEventObserver)
            scrollEvent.observe(viewLifecycleOwner, scrollEventObserver)
            showFavoriteTooltipEvent.observe(viewLifecycleOwner, showFavoriteTooltipEventObserver)
            showDownloadTooltipEvent.observe(viewLifecycleOwner, showDownloadTooltipEventObserver)
            removeTrackEvent.observe(viewLifecycleOwner, removeTrackEventObserver)
            notifyFollowToast.observe(viewLifecycleOwner, notifyFollowToastObserver)
            notifyOfflineEvent.observe(viewLifecycleOwner, notifyOfflineEventObserver)
            loginRequiredEvent.observe(viewLifecycleOwner, loginRequiredEventObserver)
            georestrictedMusicClickedEvent.observe(viewLifecycleOwner, georestrictedMusicClickedEventObserver)
            openCommentsEvent.observe(viewLifecycleOwner, openCommentsEventObserver)
            showHUDEvent.observe(viewLifecycleOwner, showHUDEventObserver)
            notifyFavoriteEvent.observe(viewLifecycleOwner, notifyFavoriteEventObserver)
            reloadAdapterTracksEvent.observe(viewLifecycleOwner, reloadAdapterTracksEventObserver)
            reloadAdapterTracksRangeEvent.observe(viewLifecycleOwner, reloadAdapterTracksRangeEventObserver)
            reloadAdapterTrackEvent.observe(viewLifecycleOwner, reloadAdapterTrackEventObserver)
            premiumRequiredEvent.observe(viewLifecycleOwner, premiumRequiredEventObserver)
            showConfirmDownloadDeletionEvent.observe(viewLifecycleOwner, showConfirmDownloadDeletionEventObserver)
            showConfirmPlaylistDownloadDeletionEvent.observe(viewLifecycleOwner, showConfirmPlaylistDownloadDeletionEventObserver)
            showFailedPlaylistDownloadEvent.observe(viewLifecycleOwner, showFailedPlaylistDownloadEventObserver)
            showConfirmPlaylistSyncEvent.observe(viewLifecycleOwner, showConfirmPlaylistSyncEventObserver)
            showPremiumDownloadEvent.observe(viewLifecycleOwner, showPremiumDownloadEventObserver)
            showUnlockedToastEvent.observe(viewLifecycleOwner, showUnlockedToastEventObserver)
            promptNotificationPermissionEvent.observe(viewLifecycleOwner, promptNotificationPermissionEventObserver)

            favoriteAction.observe(viewLifecycleOwner, ActionObserver(actionFavorite))
            downloadAction.observe(viewLifecycleOwner, ActionObserver(actionDownload))
            editAction.observe(viewLifecycleOwner, ActionObserver(actionEdit))
        }
    }

    private fun onFavoriteClick() {
        if (!viewModel.isPlaylistFavorited) {
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
        tvTopPlaylistTitle.text = title
    }

    private val uploaderObserver = Observer<ArtistWithBadge> { data ->
        tvUploadedBy.text = tvUploadedBy.context.spannableString(
            fullString = getString(R.string.by) + " " + data.name,
            highlightedStrings = listOf(data.name),
            highlightedColor = tvUploadedBy.context.colorCompat(R.color.orange)
        )
        when {
            data.verified -> {
                imageViewVerified.setImageResource(R.drawable.ic_verified)
                imageViewVerified.visibility = View.VISIBLE
            }
            data.tastemaker -> {
                imageViewVerified.setImageResource(R.drawable.ic_tastemaker)
                imageViewVerified.visibility = View.VISIBLE
            }
            data.authenticated -> {
                imageViewVerified.setImageResource(R.drawable.ic_authenticated)
                imageViewVerified.visibility = View.VISIBLE
            }
            else -> {
                imageViewVerified.visibility = View.GONE
            }
        }
    }

    private val setupTracksEventObserver = Observer<AMResultItem> { playlist ->
        if (adapter == null) {
            adapter =
                PlaylistTracksAdapter(playlist, playlist.tracks ?: arrayListOf(), true, viewModel)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter
        } else {
            adapter?.updateTracks(playlist.tracks ?: arrayListOf())
        }
    }

    private val backEventObserver = Observer<Void> {
        activity?.onBackPressed()
    }

    private val openInfoEventObserver = Observer<AMResultItem> { playlist ->
        homeViewModel.onMusicInfoTapped(playlist)
    }

    private val shareEventObserver = Observer<AMResultItem> { playlist ->
        val source = playlist.mixpanelSource ?: MixpanelSource.empty
        playlist.openShareSheet(activity, source, MixpanelButtonPlaylistDetails)
    }

    private val showEditMenuEventObserver = Observer<Void> {
        val actions = listOf(
            Action(getString(R.string.options_reorder_remove_tracks), false, R.drawable.menu_reorder_playlist, object : Action.ActionListener {
                override fun onActionExecuted() {
                    viewModel.onOptionReorderRemoveTracksTapped()
                }
            }),
            Action(getString(R.string.options_edit_playlist_details), false, R.drawable.menu_edit_playlist, object : Action.ActionListener {
                override fun onActionExecuted() {
                    viewModel.onOptionEditPlaylistTapped()
                }
            }),
            Action(getString(R.string.options_delete_playlist), false, R.drawable.menu_delete, object : Action.ActionListener {
                override fun onActionExecuted() {
                    viewModel.onOptionDeletePlaylistTapped()
                }
            })
        )
        (activity as? BaseActivity)?.openOptionsFragment(OptionsMenuFragment.newInstance(actions))
    }

    private val closeOptionsEventObserver = Observer<Void> {
        (activity as? BaseActivity)?.popFragment()
    }

    private val openEditEventObserver = Observer<AMResultItem> { playlist ->
        MainApplication.playlist = playlist
        context?.let { context ->
            startActivity(
                EditPlaylistActivity.getLaunchIntent(context, EDIT)
            )
        }
    }

    private val openReorderEventObserver = Observer<AMResultItem> { playlist ->
        MainApplication.playlist = playlist
        startActivity(Intent(activity, ReorderPlaylistActivity::class.java))
    }

    private val showDeleteConfirmationEventObserver = Observer<AMResultItem> { playlist ->
        val activity = activity ?: return@Observer
        AMAlertFragment.show(
            activity,
            activity.spannableString(
                fullString = getString(R.string.playlist_delete_title_template, playlist.title),
                highlightedStrings = listOf(playlist.title ?: ""),
                highlightedColor = activity.colorCompat(R.color.orange)
            ),
            getString(R.string.playlist_delete_message),
            getString(R.string.playlist_delete_yes),
            getString(R.string.playlist_delete_no),
            Runnable {
                viewModel.onConfirmDeletePlaylistTapped()
            },
            null,
            null
        )
    }

    private val deletePlaylistStatusEventObserver = Observer<PlaylistViewModel.DeletePlaylistStatus> { status ->
        val activity = activity ?: return@Observer
        when (status) {
            is PlaylistViewModel.DeletePlaylistStatus.Success -> {
                AMProgressHUD.dismiss()
                AMSnackbar.Builder(activity)
                    .withTitle(status.message)
                    .withDrawable(R.drawable.ic_snackbar_playlist)
                    .show()
                (activity as? HomeActivity)?.popFragment()
            }
            is PlaylistViewModel.DeletePlaylistStatus.Error -> {
                AMSnackbar.Builder(activity)
                    .withTitle(status.message)
                    .withSubtitle(getString(R.string.please_try_again_later))
                    .withDrawable(R.drawable.ic_snackbar_error)
                    .withSecondary(R.drawable.ic_snackbar_playlist_grey)
                    .show()
            }
            PlaylistViewModel.DeletePlaylistStatus.Loading -> AMProgressHUD.showWithStatus(activity)
        }
    }

    private val shuffleEventObserver = Observer<Pair<AMResultItem, AMResultItem>> { (track, playlist) ->
        homeViewModel.onMaximizePlayerRequested(
            MaximizePlayerData(
                track,
                playlist,
                null,
                null,
                false,
                true,
                0,
                mixpanelSource.apply { shuffled = true },
                true,
                false,
                animated = true
            )
        )
    }

    private val openTrackEventObserver = Observer<Triple<AMResultItem, AMResultItem?, Int>> { (track, playlist, index) ->
        homeViewModel.onMaximizePlayerRequested(MaximizePlayerData(
            item = track,
            collection = playlist,
            loadFullPlaylist = true,
            albumPlaylistIndex = index,
            mixpanelSource = mixpanelSource
        ))
    }

    private val openTrackOptionsEventObserver = Observer<AMResultItem> { track ->
        (activity as? BaseActivity)?.openOptionsFragment(SlideUpMenuMusicFragment.newInstance(track, mixpanelSource, false, false, null))
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
                    val index = adapter?.indexOfItemId(track.itemId) ?: -1
                    track.deepDelete()
                    if (index != -1) {
                        adapter?.notifyItemChanged(index)
                    }
                }
            })
        )
        (activity as? BaseActivity)?.openOptionsFragment(OptionsMenuFragment.newInstance(actions))
    }

    private val openUploaderEventObserver = Observer<String> { uploaderSlug ->
        homeViewModel.onArtistScreenRequested(uploaderSlug)
    }

    private val followStatusObserver = Observer<Boolean> { followed ->
        buttonFollow.setImageDrawable(buttonFollow.context.drawableCompat(if (followed) R.drawable.player_unfollow else R.drawable.player_follow))
    }

    private val followVisibleObserver = Observer<Boolean> { visible ->
        buttonFollow.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val descriptionObserver = Observer<CharSequence> { description ->
        tvDescription.text = description
        try {
            tvDescription.movementMethod = LinkMovementMethod()
        } catch (e: NoSuchMethodError) {
            Timber.w(e)
        }
    }

    private val descriptionVisibleObserver = Observer<Boolean> { visible ->
        tvDescription.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val highResImageObserver = Observer<String> { image ->
        viewModel.imageLoader.load(imageViewSmall.context, image, imageViewSmall)
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

    private val bannerObserver = Observer<String> { image ->
        imageView.setImageBitmap(null)
        viewModel.imageLoader.load(imageViewBanner.context, image, imageViewBanner)
    }

    private val playButtonActiveObserver = Observer<Boolean> { active ->
        buttonPlayAll.setText(if (active) R.string.playlist_pause else R.string.playlist_play)
        buttonPlayAll.setCompoundDrawablesWithIntrinsicBounds(buttonShuffle.context.drawableCompat(
            if (active) R.drawable.artists_onboarding_playlist_pause else R.drawable.artists_onboarding_playlist_play
        ), null, null, null)
    }

    private val favoriteVisibleObserver = Observer<Boolean> { visible ->
        actionFavorite.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val editVisibleObserver = Observer<Boolean> { visible ->
        actionEdit.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val syncVisibleObserver = Observer<Boolean> { visible ->
        buttonSync.visibility = if (visible) View.VISIBLE else View.GONE
        upperLayout.doOnLayout {
            updateRecyclerViewSpacing()
            viewModel.onLayoutReady()
        }
    }

    private val commentsCountObserver = Observer<Int> { count ->
        buttonViewComment.commentsCount = count
        adapter?.updateCollection(playlist)
    }

    private val scrollEventObserver = Observer<Void> { handleScroll() }

    private val showPlaylistTakenDownAlertEventObserver = Observer<Void> {
        val activity = activity ?: return@Observer
        AMAlertFragment.show(
            activity,
            SpannableString(getString(R.string.playlist_takendown_title)),
            getString(R.string.playlist_takendown_message),
            getString(R.string.playlist_takendown_yes),
            getString(R.string.playlist_takendown_no),
            Runnable { viewModel.onCreatePlaylistTapped() },
            Runnable { viewModel.onDeleteTakendownPlaylistTapped() },
            null
        )
    }

    private val openPlaylistEventObserver = Observer<AMResultItem> { playlist ->
        HomeActivity.instance?.openPlaylist(
            playlist = playlist,
            online = false,
            deleted = false,
            mixpanelSource = playlist.mixpanelSource ?: MixpanelSource.empty,
            openShare = false
        )
    }

    private val createPlaylistStatusEventObserver = Observer<PlaylistViewModel.CreatePlaylistStatus> {
        when (it) {
            is PlaylistViewModel.CreatePlaylistStatus.Error -> {
                AMSnackbar.Builder(activity)
                    .withTitle(it.message)
                    .withSubtitle(getString(R.string.please_try_again_later))
                    .withDrawable(R.drawable.ic_snackbar_error)
                    .withSecondary(R.drawable.ic_snackbar_playlist_grey)
                    .show()
            }
            is PlaylistViewModel.CreatePlaylistStatus.Success -> {
                AMProgressHUD.dismiss()
                AMSnackbar.Builder(activity)
                    .withTitle(it.message)
                    .withDrawable(R.drawable.ic_snackbar_playlist)
                    .show()
            }
            is PlaylistViewModel.CreatePlaylistStatus.Loading -> AMProgressHUD.showWithStatus(activity)
        }
    }

    private val performSyncEventObserver = Observer<Void> {
        buttonSync.visibility = View.GONE
        upperLayout.post { updateRecyclerViewSpacing() }
    }

    private val showFavoriteTooltipEventObserver = Observer<Void> {
        val rect = Rect()
        actionFavorite.getGlobalVisibleRect(rect)
        val targetPoint = Point(rect.left + rect.width() / 2, rect.top)
        val tooltipFragment = TooltipFragment.newInstance(
            getString(R.string.tooltip_playlists_favorite),
            R.drawable.tooltip_playlists,
            TooltipCorner.TOPRIGHT,
            arrayListOf(targetPoint),
            Runnable { }
        )
        (activity as? HomeActivity)?.openTooltipFragment(tooltipFragment)
    }

    private val showDownloadTooltipEventObserver = Observer<Void> {
        val rect = Rect()
        actionDownload.getGlobalVisibleRect(rect)
        val targetPoint = Point(rect.left + rect.width() / 2, rect.top)
        val tooltipFragment = TooltipFragment.newInstance(
            getString(R.string.tooltip_playlists_offline),
            R.drawable.tooltip_playlists_offline,
            TooltipCorner.TOPRIGHT,
            arrayListOf(targetPoint),
            Runnable { }
        )
        (activity as? HomeActivity)?.openTooltipFragment(tooltipFragment)
    }

    private val removeTrackEventObserver = Observer<Int> { position ->
        adapter?.notifyItemRemoved(position)
        adapter?.notifyDataSetChanged()
    }

    private val notifyFollowToastObserver = Observer<ToggleFollowResult.Notify> {
        showFollowedToast(it)
    }

    private val notifyOfflineEventObserver = Observer<Void> {
        showOfflineAlert()
    }

    private val loginRequiredEventObserver = Observer<LoginSignupSource> { loginSignupSource ->
        showLoggedOutAlert(loginSignupSource)
    }

    private val georestrictedMusicClickedEventObserver = Observer<AMResultItem> { track ->
        (activity as? HomeActivity)?.homeViewModel?.onGeorestrictedMusicClicked(if (viewModel.editVisible.value == true) Runnable {
            viewModel.removeGeorestrictedTrack(track)
        } else null)
    }

    private val openCommentsEventObserver = Observer<AMResultItem> { playlist ->
        (activity as? HomeActivity)?.openComments(playlist, null, null)
    }

    private val showHUDEventObserver = Observer<ProgressHUDMode> { mode ->
        AMProgressHUD.show(activity, mode)
    }

    private val notifyFavoriteEventObserver = Observer<ToggleFavoriteResult.Notify> {
        activity?.showFavoritedToast(it)
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

    private val premiumRequiredEventObserver = Observer<InAppPurchaseMode> { mode ->
        InAppPurchaseActivity.show(activity, mode)
    }

    private val showConfirmDownloadDeletionEventObserver = Observer<AMResultItem> { music ->
        confirmDownloadDeletion(music)
    }

    private val showConfirmPlaylistDownloadDeletionEventObserver = Observer<AMResultItem> { music ->
        confirmPlaylistDownloadDeletion(music)
    }

    private val showFailedPlaylistDownloadEventObserver = Observer<Void> {
        showFailedPlaylistDownload()
    }

    private val showConfirmPlaylistSyncEventObserver = Observer<Int> { tracksCount ->
        confirmPlaylistSync(tracksCount, Runnable {
            viewModel.onPlaylistSyncConfirmed()
        })
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

    private fun updateRecyclerViewSpacing() {
        val height = upperLayout.measuredHeight
        if (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }
        recyclerView.addItemDecoration(AMRecyclerViewTopSpaceDecoration(height))
        recyclerView.setPadding(0, 0, 0, activity?.convertDpToPixel(if (viewModel.adsVisible) 80F else 20F) ?: 0)
        viewModel.recyclerviewConfigured = true
        recyclerView.listener = viewModel
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
                tvTopPlaylistTitle.visibility = View.VISIBLE
            }
        } else {
            if (viewModel.recyclerviewConfigured) {
                shadowImageView.visibility = View.INVISIBLE
                topView.visibility = View.INVISIBLE
                tvTopPlaylistTitle.visibility = View.INVISIBLE
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

        val bannerAlpha = 1F - (min(1F, max(0F, scrollY.toFloat() / maxScrollY.toFloat())) * 100F).roundToInt().toFloat() / 100F
        if (imageViewBanner.alpha != bannerAlpha) {
            imageViewBanner.alpha = bannerAlpha
        }
    }

    private val mixpanelSource: MixpanelSource
        get() = playlist.mixpanelSource ?: MixpanelSource(MainApplication.currentTab, MixpanelPagePlaylistDetails, null)

    // Utils

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
        online = requireArguments().getBoolean("online")
        deleted = requireArguments().getBoolean("deleted")
        openShare = requireArguments().getBoolean("openShare")
    }

    // Static

    companion object {
        private const val TAG = "PlaylistFragment"
        fun newInstance(playlist: AMResultItem, online: Boolean, deleted: Boolean, externalMixpanelSource: MixpanelSource, openShare: Boolean): PlaylistFragment {
            return PlaylistFragment().apply {
                playlist.mixpanelSource = externalMixpanelSource
                this.playlist = playlist
                arguments = bundleOf(
                    "online" to online,
                    "deleted" to deleted,
                    "openShare" to openShare
                )
            }
        }
    }
}
