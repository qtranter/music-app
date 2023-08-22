package com.audiomack.ui.onboarding.playlist

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.tracking.mixpanel.MixpanelButtonKebabMenu
import com.audiomack.data.tracking.mixpanel.MixpanelPageOnboarding
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMResultItem
import com.audiomack.model.Action
import com.audiomack.model.EventDownload
import com.audiomack.model.EventPlayPauseChange
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.playback.SongAction
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.player.full.view.SongActionButton
import com.audiomack.ui.playlist.details.PlaylistTracksAdapter
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.audiomack.ui.settings.OptionsMenuFragment
import com.audiomack.ui.slideupmenu.music.SlideUpMenuMusicFragment
import com.audiomack.utils.confirmDownloadDeletion
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.showDownloadUnlockedToast
import com.audiomack.utils.showFavoritedToast
import com.audiomack.utils.showLoggedOutAlert
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressHUD
import com.audiomack.views.AMRecyclerViewTopSpaceDecoration
import java.util.Collections
import kotlin.math.max
import kotlin.math.min
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.actionFavorite
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.avatarImageView
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.avatarVerifiedImageView
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.buttonBack
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.buttonPlayAll
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.buttonShuffle
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.imageViewUploadedByVerified
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.navigationBar
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.recyclerView
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.shadowImageView
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.tvPlaylistName
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.tvPlaylistNameTop
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.tvUploadedBy
import kotlinx.android.synthetic.main.fragment_playlist_onboarding.upperLayout
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PlaylistOnboardingFragment : TrackedFragment(R.layout.fragment_playlist_onboarding, TAG) {

    private lateinit var artistImage: String
    private lateinit var playlist: AMResultItem
    private lateinit var viewModel: PlaylistOnboardingViewModel
    private var adapter: PlaylistTracksAdapter? = null

    private var dp60: Float? = null
    private var dp24: Float? = null
    private var dp10: Float? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this,
            PlaylistOnboardingViewModelFactory(artistImage, playlist, mixpanelSource)
        ).get(PlaylistOnboardingViewModel::class.java)

        viewModel.backEvent.observe(viewLifecycleOwner, Observer {
            activity?.onBackPressed()
        })
        viewModel.shuffleEvent.observe(viewLifecycleOwner, Observer {
            if (viewModel.tracks.isNotEmpty()) {
                val track = viewModel.tracks[0]
                HomeActivity.instance?.homeViewModel?.onMaximizePlayerRequested(
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
                        true,
                        animated = true
                    )
                )
            }
        })
        viewModel.openTrackEvent.observe(viewLifecycleOwner, Observer { (track, playlist, index) ->
            (activity as? HomeActivity)?.homeViewModel?.onMaximizePlayerRequested(MaximizePlayerData(
                item = track,
                collection = playlist,
                loadFullPlaylist = true,
                albumPlaylistIndex = index,
                mixpanelSource = mixpanelSource
            ))
        })
        viewModel.openTrackOptionsEvent.observe(viewLifecycleOwner, Observer {
            (activity as? BaseActivity)?.openOptionsFragment(SlideUpMenuMusicFragment.newInstance(it, mixpanelSource, false, false, null))
        })
        viewModel.openTrackOptionsFailedDownloadEvent.observe(viewLifecycleOwner, Observer {
            val actions = listOf(
                Action(getString(R.string.options_retry_download), object : Action.ActionListener {
                    override fun onActionExecuted() {
                        (activity as? BaseActivity)?.popFragment()
                        viewModel.onTrackDownloadTapped(it, MixpanelButtonKebabMenu)
                    }
                }),
                Action(getString(R.string.options_delete_download), object : Action.ActionListener {
                    override fun onActionExecuted() {
                        (activity as? BaseActivity)?.popFragment()
                        val index = adapter?.indexOfItemId(it.itemId) ?: -1
                        it.deepDelete()
                        if (index != -1) {
                            adapter?.notifyItemChanged(index)
                        }
                    }
                })
            )
            (activity as? BaseActivity)?.openOptionsFragment(OptionsMenuFragment.newInstance(actions))
        })
        viewModel.cleanupEvent.observe(viewLifecycleOwner, Observer {
            recyclerView?.clearOnScrollListeners()
        })
        viewModel.scrollEvent.observe(viewLifecycleOwner, Observer {

            recyclerView.visibility = View.VISIBLE

            val dp60 = dp60 ?: run {
                this.dp60 = activity?.convertDpToPixel(60F)?.toFloat() ?: 0F
                this.dp60!!
            }
            val dp24 = dp24 ?: run {
                this.dp24 = activity?.convertDpToPixel(24F)?.toFloat() ?: 0F
                this.dp24!!
            }
            val dp10 = dp10 ?: run {
                this.dp10 = activity?.convertDpToPixel(10F)?.toFloat() ?: 0F
                this.dp10!!
            }

            val maxScrollY = upperLayout.height - navigationBar.height
            var scrollY = recyclerView.offsetY

            val scrollPercentage = max(0.toFloat(), min(1.toFloat(), scrollY.toFloat() / maxScrollY.toFloat()))

            if (scrollY >= maxScrollY) {
                scrollY = maxScrollY
                shadowImageView.visibility = View.VISIBLE
                tvPlaylistNameTop.alpha = 1.0F
                avatarImageView.alpha = 0.0F
            } else {
                shadowImageView.visibility = View.INVISIBLE
                tvPlaylistNameTop.alpha = 0.0F
                avatarImageView.alpha = 1.0F
            }

            val upperLayoutLayoutParams = upperLayout.layoutParams as FrameLayout.LayoutParams
            if (upperLayoutLayoutParams.topMargin != -scrollY) {
                upperLayoutLayoutParams.topMargin = -scrollY
                upperLayout.layoutParams = upperLayoutLayoutParams
            }

            val avatarLayoutParams = avatarImageView.layoutParams as FrameLayout.LayoutParams
            val avatarNewSize = dp24.toInt() + ((dp60 - dp24) * (1 - scrollPercentage)).toInt()
            val avatarNewTopMargin = (dp10 * (1 - scrollPercentage)).toInt()
            if (avatarNewSize != avatarLayoutParams.width) {
                avatarLayoutParams.width = avatarNewSize
                avatarLayoutParams.topMargin = avatarNewTopMargin
                avatarImageView.layoutParams = avatarLayoutParams
            }

            if (scrollPercentage > 0.03 && avatarVerifiedImageView.visibility == View.VISIBLE) {
                avatarVerifiedImageView.visibility = View.INVISIBLE
            } else if (scrollPercentage < 0.03 && avatarVerifiedImageView.visibility == View.INVISIBLE) {
                avatarVerifiedImageView.visibility = View.VISIBLE
            }
        })
        viewModel.openUploaderEvent.observe(viewLifecycleOwner, Observer {
            HomeActivity.instance?.homeViewModel?.onArtistScreenRequested(it)
        })
        viewModel.updatePlayEvent.observe(viewLifecycleOwner, Observer { currentlyPlayingThisPlaylist ->
            buttonPlayAll.setText(if (currentlyPlayingThisPlaylist) R.string.artists_onboarding_playlist_pause else R.string.artists_onboarding_playlist_play_all)
            buttonPlayAll.setCompoundDrawablesWithIntrinsicBounds(buttonPlayAll.context.drawableCompat(
                if (currentlyPlayingThisPlaylist) R.drawable.artists_onboarding_playlist_pause else R.drawable.artists_onboarding_playlist_play
            ), null, null, null)
        })
        viewModel.updateListEvent.observe(viewLifecycleOwner, Observer {
            adapter?.notifyItemRangeChanged(0, adapter?.itemCount ?: 0)
        })
        viewModel.updateTrackEvent.observe(viewLifecycleOwner, Observer {
            val index = adapter?.indexOfItemId(it) ?: -1
            if (index != -1) {
                adapter?.notifyItemChanged(index)
            }
        })
        viewModel.updateDetailsEvent.observe(viewLifecycleOwner, Observer {
            viewModel.imageLoader.load(avatarImageView.context, viewModel.artistPicture, avatarImageView)

            tvPlaylistName.text = viewModel.title
            tvPlaylistNameTop.text = viewModel.title
            tvUploadedBy.text = tvUploadedBy.context.spannableString(
                fullString = "${getString(R.string.by)} ${viewModel.uploaderName}",
                highlightedStrings = listOf(viewModel.uploaderName),
                highlightedColor = tvUploadedBy.context.colorCompat(R.color.orange)
            )

            when {
                viewModel.uploaderVerified -> {
                    imageViewUploadedByVerified.setImageResource(R.drawable.ic_verified)
                    imageViewUploadedByVerified.visibility = View.VISIBLE
                    avatarVerifiedImageView.setImageResource(R.drawable.ic_verified)
                    avatarVerifiedImageView.visibility = View.VISIBLE
                }
                viewModel.uploaderTastemaker -> {
                    imageViewUploadedByVerified.setImageResource(R.drawable.ic_tastemaker)
                    imageViewUploadedByVerified.visibility = View.VISIBLE
                    avatarVerifiedImageView.setImageResource(R.drawable.ic_tastemaker)
                    avatarVerifiedImageView.visibility = View.VISIBLE
                }
                viewModel.uploaderAuthenticated -> {
                    imageViewUploadedByVerified.setImageResource(R.drawable.ic_authenticated)
                    imageViewUploadedByVerified.visibility = View.VISIBLE
                    avatarVerifiedImageView.setImageResource(R.drawable.ic_authenticated)
                    avatarVerifiedImageView.visibility = View.VISIBLE
                }
                else -> {
                    imageViewUploadedByVerified.visibility = View.GONE
                    avatarVerifiedImageView.visibility = View.GONE
                }
            }

            adapter = PlaylistTracksAdapter(
                playlist,
                viewModel.tracks.toMutableList(),
                false,
                viewModel
            )
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter
            recyclerView.listener = viewModel

            val vto = upperLayout.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    vto.removeOnGlobalLayoutListener(this)
                    val upperLayout = upperLayout ?: return
                    val height = upperLayout.measuredHeight - navigationBar.height
                    if (recyclerView.itemDecorationCount > 0) {
                        recyclerView.removeItemDecorationAt(0)
                    }
                    recyclerView.addItemDecoration(AMRecyclerViewTopSpaceDecoration(height))
                    recyclerView.setPadding(0, 0, 0, if (viewModel.adsVisible) resources.getDimensionPixelSize(R.dimen.ad_height) else 0)
                    viewModel.onRecyclerViewConfigured()
                }
            })
        })

        viewModel.notifyFavoriteEvent.observe(viewLifecycleOwner, notifyFavoriteEventObserver)
        viewModel.loginRequiredEvent.observe(viewLifecycleOwner, loginRequiredEventObserver)
        viewModel.premiumRequiredEvent.observe(viewLifecycleOwner, premiumRequiredEventObserver)
        viewModel.showHUDEvent.observe(viewLifecycleOwner, showHUDEventObserver)
        viewModel.showConfirmDownloadDeletionEvent.observe(viewLifecycleOwner, showConfirmDownloadDeletionEventObserver)
        viewModel.showPremiumDownloadEvent.observe(viewLifecycleOwner, showPremiumDownloadEventObserver)
        viewModel.showUnlockedToastEvent.observe(viewLifecycleOwner, showUnlockedToastEventObserver)

        viewModel.favoriteAction.observe(viewLifecycleOwner, ActionObserver(actionFavorite))

        buttonBack.setOnClickListener { viewModel.onBackTapped() }
        buttonPlayAll.setOnClickListener { viewModel.onPlayAllTapped() }
        buttonShuffle.setOnClickListener { viewModel.onShuffleTapped() }
        tvUploadedBy.setOnClickListener { viewModel.onUploaderTapped() }
        actionFavorite.setOnClickListener { onFavoriteClick() }

        viewModel.onCreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!::viewModel.isInitialized) {
            return
        }
        viewModel.onDestroy()
        EventBus.getDefault().unregister(this)
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

    private val notifyFavoriteEventObserver = Observer<ToggleFavoriteResult.Notify> {
        activity?.showFavoritedToast(it)
    }

    private val loginRequiredEventObserver = Observer<LoginSignupSource> { loginSignupSource ->
        showLoggedOutAlert(loginSignupSource)
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventDownload: EventDownload) {
        viewModel.onDownloadStatusChanged(eventDownload.itemId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventPlayPauseChange: EventPlayPauseChange) {
        viewModel.onPlayPauseChanged()
    }

    private val mixpanelSource: MixpanelSource
        get() = MixpanelSource(MainApplication.currentTab, MixpanelPageOnboarding, Collections.emptyList())

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
        private const val TAG = "PlaylistOnboardingFragment"
        fun newInstance(artistImage: String, playlist: AMResultItem): PlaylistOnboardingFragment {
            return PlaylistOnboardingFragment().apply {
                this.artistImage = artistImage
                this.playlist = playlist
            }
        }
    }
}
