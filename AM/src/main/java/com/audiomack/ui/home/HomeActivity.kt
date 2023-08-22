package com.audiomack.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.PorterDuff
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.text.SpannableString
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.Transformation
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkInfo.State.FAILED
import androidx.work.WorkInfo.State.RUNNING
import com.audiomack.MainApplication
import com.audiomack.PREMIUM_SUPPORT_URL
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.data.deeplink.Deeplink
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.location.LocationDetector
import com.audiomack.data.queue.QueueDataSource.Companion.CURRENT_INDEX
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.reachability.Reachability
import com.audiomack.data.support.ZendeskRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonExternal
import com.audiomack.data.tracking.mixpanel.MixpanelButtonNowPlaying
import com.audiomack.data.tracking.mixpanel.MixpanelPageDeeplink
import com.audiomack.data.tracking.mixpanel.PremiumDownloadType
import com.audiomack.data.tracking.mixpanel.SleepTimerSource
import com.audiomack.fragments.DataFragment
import com.audiomack.model.AMArtist
import com.audiomack.model.AMComment
import com.audiomack.model.AMResultItem
import com.audiomack.model.AddToPlaylistModel
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.Credentials
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.InAppPurchaseMode.AudioAd
import com.audiomack.model.InAppPurchaseMode.SleepTimerPrompt
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import com.audiomack.model.NextPageData
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.PremiumLimitedDownloadAlertViewType
import com.audiomack.model.PremiumOnlyDownloadAlertViewType
import com.audiomack.model.ProgressHUDMode
import com.audiomack.model.SearchType
import com.audiomack.model.WorldPage
import com.audiomack.playback.MusicService
import com.audiomack.playback.MusicViewModel
import com.audiomack.playback.PlayerPlayback
import com.audiomack.playback.PlayerQueue
import com.audiomack.ui.ads.AudioAdFragment
import com.audiomack.ui.ads.AudioAdViewModel
import com.audiomack.ui.album.AlbumFragment
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.artist.ArtistFragment
import com.audiomack.ui.artistinfo.ArtistInfoFragment
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.ui.authentication.changepw.ChangePasswordActivity
import com.audiomack.ui.authentication.resetpw.ResetPasswordActivity
import com.audiomack.ui.browse.BrowseFragment
import com.audiomack.ui.browse.world.detail.WorldArticleFragment
import com.audiomack.ui.comments.add.AddCommentFragment
import com.audiomack.ui.comments.view.CommentsFragment
import com.audiomack.ui.feed.FeedFragment
import com.audiomack.ui.feed.suggested.SuggestedAccountsFragment
import com.audiomack.ui.help.HelpActivity
import com.audiomack.ui.imagezoom.ImageZoomFragment
import com.audiomack.ui.musicinfo.MusicInfoFragment
import com.audiomack.ui.mylibrary.MyLibraryFragment
import com.audiomack.ui.mylibrary.offline.edit.EditDownloadsFragment
import com.audiomack.ui.mylibrary.offline.local.menu.SlideUpMenuLocalMediaFragment
import com.audiomack.ui.notifications.NotificationsContainerFragment
import com.audiomack.ui.onboarding.artists.ArtistsOnboardingFragment
import com.audiomack.ui.onboarding.playlist.PlaylistOnboardingFragment
import com.audiomack.ui.player.NowPlayingFragment
import com.audiomack.ui.player.NowPlayingViewModel
import com.audiomack.ui.player.full.PlayerViewModel
import com.audiomack.ui.player.maxi.PlayerDragDirection
import com.audiomack.ui.player.maxi.bottom.PlayerBottomFragment
import com.audiomack.ui.player.maxi.info.PlayerInfoViewModel
import com.audiomack.ui.player.maxi.uploader.PlayerUploaderTagsFragment
import com.audiomack.ui.player.mini.MinifiedPlayerFragment
import com.audiomack.ui.playlist.add.AddToPlaylistsActivity
import com.audiomack.ui.playlist.details.PlaylistFragment
import com.audiomack.ui.playlists.PlaylistsFragment
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.audiomack.ui.premiumdownload.PremiumDownloadFragment
import com.audiomack.ui.queue.QueueFragment
import com.audiomack.ui.replacedownload.ReplaceDownloadFragment
import com.audiomack.ui.search.SearchFragment
import com.audiomack.ui.settings.OptionsMenuFragment
import com.audiomack.ui.sleeptimer.SleepTimerAlertFragment
import com.audiomack.ui.slideupmenu.music.SlideUpMenuMusicFragment
import com.audiomack.ui.slideupmenu.share.SlideUpMenuShareFragment
import com.audiomack.ui.tooltip.TooltipCorner
import com.audiomack.ui.tooltip.TooltipFragment
import com.audiomack.usecases.LoginAlertUseCase
import com.audiomack.usecases.SleepTimerPromptMode
import com.audiomack.utils.adjustColorAlpha
import com.audiomack.utils.confirmDownloadDeletion
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.openUrlExcludingAudiomack
import com.audiomack.utils.showAddedToQueueToast
import com.audiomack.utils.showFavoritedToast
import com.audiomack.utils.showOfflineAlert
import com.audiomack.utils.showRepostedToast
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressHUD
import com.audiomack.views.AMSnackbar
import com.audiomack.views.ProgressLogoDialog
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.snackbar.Snackbar
import com.mopub.common.MoPub
import com.mopub.mobileads.MoPubView
import com.mopub.nativeads.AdapterHelper
import com.mopub.nativeads.NativeAd
import de.hdodenhof.circleimageview.CircleImageView
import io.embrace.android.embracesdk.Embrace
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt
import kotlinx.android.synthetic.main.activity_home.adLayout
import kotlinx.android.synthetic.main.activity_home.adOverlayContainer
import kotlinx.android.synthetic.main.activity_home.buttonRemoveAd
import kotlinx.android.synthetic.main.activity_home.fullScreenContainer
import kotlinx.android.synthetic.main.activity_home.imageViewTabBrowse
import kotlinx.android.synthetic.main.activity_home.imageViewTabFeed
import kotlinx.android.synthetic.main.activity_home.imageViewTabMyLibrary
import kotlinx.android.synthetic.main.activity_home.imageViewTabPlaylists
import kotlinx.android.synthetic.main.activity_home.imageViewTabSearch
import kotlinx.android.synthetic.main.activity_home.layoutBrowse
import kotlinx.android.synthetic.main.activity_home.layoutFeed
import kotlinx.android.synthetic.main.activity_home.layoutMyLibrary
import kotlinx.android.synthetic.main.activity_home.layoutPlaylists
import kotlinx.android.synthetic.main.activity_home.layoutSearch
import kotlinx.android.synthetic.main.activity_home.miniPlayerContainer
import kotlinx.android.synthetic.main.activity_home.mopubAdViewHome
import kotlinx.android.synthetic.main.activity_home.playerContainer
import kotlinx.android.synthetic.main.activity_home.rootLayout
import kotlinx.android.synthetic.main.activity_home.tabbarLayout
import kotlinx.android.synthetic.main.activity_home.tvFeedBadge
import kotlinx.android.synthetic.main.activity_home.tvNotificationsBadge
import kotlinx.android.synthetic.main.activity_home.tvTabBrowse
import kotlinx.android.synthetic.main.activity_home.tvTabFeed
import kotlinx.android.synthetic.main.activity_home.tvTabMyLibrary
import kotlinx.android.synthetic.main.activity_home.tvTabPlaylists
import kotlinx.android.synthetic.main.activity_home.tvTabSearch
import kotlinx.android.synthetic.main.activity_home.upperLayout
import timber.log.Timber
import zendesk.support.request.RequestActivity
import zendesk.support.requestlist.RequestListActivity

class HomeActivity : BaseActivity() {

    lateinit var homeViewModel: HomeViewModel
    lateinit var nowPlayingViewModel: NowPlayingViewModel
    lateinit var playerViewModel: PlayerViewModel
    lateinit var playerInfoViewModel: PlayerInfoViewModel
    private val audioAdViewModel: AudioAdViewModel by viewModels()

    private val musicViewModel: MusicViewModel by lazy { initMusicViewModel() }

    private val playerPlayback: PlayerPlayback = PlayerPlayback.getInstance()

    private var nowPlayingFragment: NowPlayingFragment? = null

    var tooltipFragmentReference: WeakReference<TooltipFragment>? = null

    private var tabAnimation: AnimatorSet? = null

    private var animationDialog: ProgressLogoDialog? = null

    private val hasOfflineExtra: Boolean
        get() {
            return intent.extras?.getBoolean(EXTRA_OFFLINE, false) == true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        instance = this

        homeViewModel = initHomeViewModel()
        playerViewModel = initPlayerViewModel()
        nowPlayingViewModel = initNowPlayingViewModel()
        playerInfoViewModel = initPlayerInfoViewModel()
        setAudioAdViewModelObservers()

        if (savedInstanceState == null && hasOfflineExtra) {
            homeViewModel.onOfflineRedirectDetected()
            MainApplication.isFreshInstallTooltipShown = true
        }
        if (savedInstanceState != null) {
            clearFragmentManager()
        }

        HomeNavigationHandler(this, homeViewModel)

        initAlertEventObservers()
        initClickListeners()

        rootLayout.doOnLayout {
            playerContainer.translationY = round(playerContainer.height.toFloat())
            miniPlayerContainer.translationY = round(resources.getDimension(R.dimen.minified_player_height) + tabbarLayout.height)
            (adLayout.layoutParams as FrameLayout.LayoutParams).apply { bottomMargin = tabbarLayout.height }.also { adLayout.requestLayout() }
        }

        initCast()
        initPlayer()

        lifecycle.addObserver(LocationDetector(this))

        homeViewModel.onAdLayoutReady(mopubAdViewHome)

        homeViewModel.onCreate(intent)

        MoPub.onCreate(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        instance = null

        homeViewModel.onDestroy()
        nowPlayingViewModel.isMaximized = false

        MoPub.onDestroy(this)
    }

    override fun onResume() {
        try {
            super.onResume()
        } catch (e: IllegalArgumentException) {
            finish()
            return
        }

        Embrace.getInstance().endAppStartup()
        homeViewModel.onResume(intent)
        volumeControlStream = AudioManager.STREAM_MUSIC

        MoPub.onResume(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        homeViewModel.onIntentReceived(intent)
        intent?.getIntExtra("notificationId", - 1)?.takeIf { it != -1 }?.let { notificationId ->
            (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(notificationId)
        }
    }

    override fun onPause() {
        super.onPause()
        homeViewModel.onPause()
        MoPub.onPause(this)
    }

    override fun onStart() {
        super.onStart()
        musicViewModel.connect()
        MoPub.onStart(this)
    }

    override fun onStop() {
        super.onStop()
        MoPub.onStop(this)
    }

    override fun onRestart() {
        super.onRestart()
        MoPub.onRestart(this)
    }

    private fun initHomeViewModel(): HomeViewModel {
        val factory = HomeViewModelFactory(activityResultRegistry)
        return ViewModelProvider(this, factory).get(HomeViewModel::class.java).apply {
            deeplinkEvent.observe(this@HomeActivity, Observer {
                handleDeeplink(it)
                homeViewModel.onDeeplinkConsumed(intent)
            })
            showSmartLockEvent.observe(this@HomeActivity, Observer {
                val credentialsRequest = CredentialRequest.Builder()
                    .setPasswordLoginSupported(true)
                    .build()
                Auth.CredentialsApi.request(credentialsApiClient, credentialsRequest)
                    .setResultCallback { result ->
                        if (result.status.isSuccess) {
                            Timber.tag(TAG).d("SmartLock: found just one credential")
                            homeViewModel.loginWithSmartLockCredentials(result.credential)
                        } else {
                            if (result.status.statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                                Timber.tag(TAG).d("SmartLock: needs resolution")
                                try {
                                    result.status.startResolutionForResult(
                                        this@HomeActivity,
                                        REQ_CODE_CREDENTIALS_RESOLUTION
                                    )
                                } catch (e: Exception) {
                                    Timber.tag(TAG).d("SmartLock: error during resolution")
                                    Timber.w(e)
                                }
                            } else {
                                Timber.tag(TAG).d("SmartLock: didn't find credentials")
                            }
                        }
                    }
            })
            deleteSmartLockCredentialsEvent.observe(this@HomeActivity, Observer { credential ->
                Auth.CredentialsApi.delete(credentialsApiClient, credential)
                    .setResultCallback { Timber.tag(TAG).d("SmartLock: deleted credentials: $it") }
            })
            restoreMiniplayerEvent.observe(this@HomeActivity, Observer {
                if (!isPlayerMaximized()) {
                    val upperLayoutBottomMarginStartValue =
                        resources.getDimensionPixelSize(R.dimen.tabbar_layout_height)
                    val upperLayoutBottomMarginEndValue =
                        resources.getDimensionPixelSize(R.dimen.tabbar_layout_height) + resources.getDimension(
                            R.dimen.minified_player_height
                        ).toInt()
                    val miniPlayerStartTranslateY = round(miniPlayerContainer.translationY)
                    val miniPlayerEndTranslateY = 0
                    val adStartTranslateY = (adLayout.layoutParams as FrameLayout.LayoutParams).bottomMargin
                    val adEndTranslateY = resources.getDimensionPixelSize(R.dimen.tabbar_layout_height) + resources.getDimension(R.dimen.minified_player_height)

                    object : Animation() {
                        override fun applyTransformation(
                            interpolatedTime: Float,
                            t: Transformation?
                        ) {
                            super.applyTransformation(interpolatedTime, t)
                            val params = upperLayout.layoutParams as FrameLayout.LayoutParams
                            params.bottomMargin =
                                ((upperLayoutBottomMarginEndValue - upperLayoutBottomMarginStartValue) * interpolatedTime).toInt() + upperLayoutBottomMarginStartValue
                            upperLayout.layoutParams = params
                            miniPlayerContainer.translationY = miniPlayerStartTranslateY + (miniPlayerEndTranslateY - miniPlayerStartTranslateY) * interpolatedTime
                            (adLayout.layoutParams as FrameLayout.LayoutParams).apply {
                                bottomMargin = adStartTranslateY + ((adEndTranslateY - adStartTranslateY) * interpolatedTime).roundToInt()
                            }.also { adLayout.requestLayout() }
                        }
                    }.also {
                        it.duration = PLAYER_ANIMATION_DURATION
                        it.setAnimationListener(object : AnimationListener {
                            override fun onAnimationRepeat(animation: Animation?) {}
                            override fun onAnimationStart(animation: Animation?) {}
                            override fun onAnimationEnd(animation: Animation?) {
                                miniPlayerContainer.visibility = View.VISIBLE
                            }
                        })
                        miniPlayerContainer.startAnimation(it)
                    }
                }
            })
            showUnreadTicketsAlert.observe(this@HomeActivity, Observer {
                AMAlertFragment.show(
                    this@HomeActivity,
                    SpannableString(getString(R.string.help_alert_title)),
                    null,
                    getString(R.string.help_alert_yes),
                    getString(R.string.help_alert_no),
                    Runnable {
                        RequestListActivity.builder().show(this@HomeActivity, ZendeskRepository().getUIConfigs())
                    },
                    null,
                    null
                )
            })
            myLibraryAvatar.observe(this@HomeActivity, Observer { image ->
                val authenticated = !image.isNullOrEmpty()
                val lp = imageViewTabMyLibrary.layoutParams as FrameLayout.LayoutParams
                imageViewTabMyLibrary.borderWidth = imageViewTabMyLibrary.context.convertDpToPixel(if (authenticated) 1.5.toFloat() else 0.toFloat())
                if (authenticated) {
                    imageViewTabMyLibrary.colorFilter = null
                    imageViewTabMyLibrary.borderColor = if (homeViewModel.currentTab.value?.index == 4) imageViewTabMyLibrary.context.colorCompat(R.color.orange) else Color.WHITE
                    PicassoImageLoader.load(imageViewTabMyLibrary.context, image, imageViewTabMyLibrary)
                } else {
                    if (homeViewModel.currentTab.value?.index == 4) {
                        imageViewTabMyLibrary.setColorFilter(imageViewTabMyLibrary.context.colorCompat(R.color.orange), PorterDuff.Mode.SRC_ATOP)
                    } else {
                        imageViewTabMyLibrary.colorFilter = null
                    }
                    imageViewTabMyLibrary.setImageResource(R.drawable.tab_account_guest)
                }
                lp.width = imageViewTabMyLibrary.context.convertDpToPixel(if (authenticated) 25.toFloat() else 20.toFloat())
                lp.height = imageViewTabMyLibrary.context.convertDpToPixel(if (authenticated) 25.toFloat() else 20.toFloat())
                lp.topMargin = imageViewTabMyLibrary.context.convertDpToPixel(if (authenticated) 5.toFloat() else 7.toFloat())
                lp.bottomMargin = imageViewTabMyLibrary.context.convertDpToPixel(if (authenticated) 4.toFloat() else 7.toFloat())
                imageViewTabMyLibrary.layoutParams = lp
            })
            myLibraryNotifications.observe(this@HomeActivity, Observer { notifications ->
                tvNotificationsBadge.text = notifications
                tvNotificationsBadge.visibility = if (notifications.isNullOrBlank()) View.GONE else View.VISIBLE
            })
            feedNotifications.observe(this@HomeActivity, Observer { notifications ->
                tvFeedBadge.text = notifications
                tvFeedBadge.visibility = if (notifications.isNullOrBlank()) View.GONE else View.VISIBLE
            })
            adLayoutVisible.observe(this@HomeActivity, Observer { visible ->
                adLayout.visibility = if (visible) View.VISIBLE else View.GONE
            })
            currentTab.observe(this@HomeActivity, Observer { currentTab ->

                tabAnimation?.cancel()

                val textViews = listOf(tvTabFeed, tvTabPlaylists, tvTabBrowse, tvTabSearch, tvTabMyLibrary)
                val imageViews = listOf(imageViewTabFeed, imageViewTabPlaylists, imageViewTabBrowse, imageViewTabSearch, imageViewTabMyLibrary)
                textViews.forEach {
                    it.setTextColor(Color.argb(255, 153, 153, 153))
                }
                imageViews.forEach {
                    it.colorFilter = null
                    (it as? CircleImageView)?.borderColor = Color.WHITE
                }

                val tv = textViews[currentTab.index]
                val iv = imageViews[currentTab.index]

                val orange = this@HomeActivity.colorCompat(R.color.orange)

                val scaleDownAnimator = ObjectAnimator.ofFloat(1f, 0.75f).apply {
                    addUpdateListener {
                        val value = it.animatedValue as Float
                        iv.scaleX = value
                        iv.scaleY = value
                    }
                    duration = 125
                    interpolator = DecelerateInterpolator()
                }

                val scaleUpAnimator = ObjectAnimator.ofFloat(0.75f, 1f).apply {
                    addUpdateListener {
                        val value = it.animatedValue as Float
                        iv.scaleX = value
                        iv.scaleY = value
                    }
                    duration = 125
                    interpolator = OvershootInterpolator()
                }

                val colorAnimator = ObjectAnimator.ofFloat(0f, 1f).apply {
                    addUpdateListener {
                        val value = min(1.toFloat(), it.animatedValue as Float)
                        val alphaOrange = orange.adjustColorAlpha(value)
                        if (currentTab.loggedIn && iv is CircleImageView) {
                            iv.borderColor = orange
                        } else {
                            iv.setColorFilter(alphaOrange, PorterDuff.Mode.SRC_ATOP)
                        }
                        tv.setTextColor(alphaOrange)
                    }
                    duration = 125
                    interpolator = OvershootInterpolator()
                }

                tabAnimation = AnimatorSet().apply {
                    play(scaleDownAnimator)
                    play(scaleUpAnimator).with(colorAnimator).after(scaleDownAnimator)
                    start()
                }
            })
            showDownloadSuccessToastEvent.observe(this@HomeActivity, Observer { event ->
                val music = event.music
                if (music.isPlaylist) {
                    AMSnackbar.Builder(this@HomeActivity)
                        .withTitle(getString(R.string.download_playlist_succeeded))
                        .withDrawable(R.drawable.ic_snackbar_download_success)
                        .show()
                } else {
                    AMSnackbar.Builder(this@HomeActivity)
                        .withTitle(music.artist + " - " + music.title)
                        .withSubtitle(getString(R.string.download_succeeded))
                        .withDrawable(R.drawable.ic_snackbar_download_success)
                        .show()
                }
            })
            showDownloadFailureToastEvent.observe(this@HomeActivity, Observer {
                AMSnackbar.Builder(this@HomeActivity)
                    .withTitle(getString(R.string.download_failed_song))
                    .withDrawable(R.drawable.ic_snackbar_download_failure)
                    .show()
            })
            showAddedToOfflineInAppMessageEvent.observe(this@HomeActivity, Observer {
                AMAlertFragment.show(
                    this@HomeActivity,
                    SpannableString(getString(R.string.downloadmessage_title)),
                    getString(R.string.downloadmessage_message),
                    getString(R.string.downloadmessage_button),
                    null,
                    Runnable { openMyAccount("downloads", null) },
                    null,
                    null
                )
            })
            openPlayerEvent.observe(this@HomeActivity, Observer {
                if (isPlayerMaximized()) {
                    nowPlayingViewModel.scrollToTop()
                } else {
                    maximizePlayer(MaximizePlayerData())
                }
            })
            openPlayerMenuEvent.observe(this@HomeActivity, Observer {
                QueueRepository.getInstance().currentItem?.let { currentItem ->
                    val source = currentItem.mixpanelSource ?: MixpanelSource.empty
                    val fragment = if (currentItem.isLocal) {
                        SlideUpMenuLocalMediaFragment.newInstance(currentItem.itemId.toLong())
                    } else {
                        SlideUpMenuMusicFragment.newInstance(currentItem, source, false, false, null)
                    }
                    openOptionsFragment(fragment)
                }
            })
            setupBackStackListenerEvent.observe(this@HomeActivity, Observer {
                // Used to trigger initial tooltip again when modal screens get closed
                supportFragmentManager.addOnBackStackChangedListener {
                    fullScreenContainer.alpha = if (fullScreenContainer.childCount == 0) 0F else 1F
                    if (supportFragmentManager.backStackEntryCount == 0) {
                        supportFragmentManager
                            .fragments
                            .firstOrNull { it is BrowseFragment }
                            ?.childFragmentManager
                            ?.fragments
                            ?.filter { it is DataFragment && it.isAdded }
                            ?.forEach {
                                it.userVisibleHint = it.userVisibleHint
                            }
                    }
                }
            })
            showMiniplayerTooltipEvent.observe(this@HomeActivity, Observer { location ->
                playerViewModel.blockAds()
                val tooltipFragment = TooltipFragment.newInstance(
                    getString(R.string.tooltip_play),
                    R.drawable.tooltip_play,
                    location,
                    Runnable {
                        homeViewModel.onMiniplayerTooltipShown()
                    }
                )
                openTooltipFragment(tooltipFragment)
            })
            toggleHUDModeEvent.observe(this@HomeActivity, Observer { mode ->
                AMProgressHUD.show(this@HomeActivity, mode)
            })
            showArtistEvent.observe(this@HomeActivity, Observer { result ->
                openArtist(result.artist, result.tab, result.openShare)
            })
            showSongEvent.observe(this@HomeActivity, Observer { result ->
                maximizePlayer(MaximizePlayerData(item = result.song, mixpanelSource = result.mixpanelSource, openShare = result.openShare))
            })
            showAlbumEvent.observe(this@HomeActivity, Observer { result ->
                openAlbum(result.album, result.mixpanelSource, result.openShare)
            })
            showPlaylistEvent.observe(this@HomeActivity, Observer { result ->
                openPlaylist(result.playlist, result.online, result.deleted, result.mixpanelSource, result.openShare)
            })
            showCommentEvent.observe(this@HomeActivity, Observer { result ->
                openComments(result.music, result.comment, null)
            })
            showBenchmarkEvent.observe(this@HomeActivity, Observer { result ->
                openScreenshotWithBenchmark(result.entity, result.benchmark, result.mixpanelSource, result.mixpanelButton)
            })
            launchPlayerEvent.observe(this@HomeActivity, Observer { data ->
                maximizePlayer(data)
            })
            closeTooltipEvent.observe(this@HomeActivity, Observer {
                closeTooltipFragment()
            })
            notifyOfflineEvent.observe(this@HomeActivity, Observer {
                showOfflineAlert()
            })
            showGeorestrictedAlertEvent.observe(this@HomeActivity, Observer { onDelete ->
                AMAlertFragment.show(
                    this@HomeActivity,
                    SpannableString(getString(R.string.georestriction_alert_message)),
                    null,
                    getString(R.string.ok),
                    if (onDelete != null) getString(R.string.georestriction_alert_delete) else null,
                    null,
                    onDelete,
                    null
                )
            })
            openMusicInfoEvent.observe(this@HomeActivity, Observer { music ->
                openMusicInfo(music)
            })
            triggerAppUpdateEvent.observe(this@HomeActivity, Observer {
                homeViewModel.triggerUpdate(this@HomeActivity)
            })
            showInAppUpdateConfirmationEvent.observe(this@HomeActivity, Observer {
                AMAlertFragment.show(
                    this@HomeActivity,
                    SpannableString(getString(R.string.in_app_update_downloaded_title)),
                    null,
                    getString(R.string.in_app_update_downloaded_restart),
                    getString(R.string.in_app_update_downloaded_cancel),
                    Runnable { homeViewModel.restartAfterUpdate() },
                    null,
                    null
                )
            })
            showInAppUpdateDownloadStartedEvent.observe(this@HomeActivity, Observer {
                AMSnackbar.Builder(this@HomeActivity)
                    .withTitle(getString(R.string.in_app_update_download_started))
                    .build()
                    .show()
            })
            showAgeGenderEvent.observe(this@HomeActivity, Observer {
                AuthenticationActivity.show(this@HomeActivity, LoginSignupSource.AppLaunch, profileCompletion = true)
            })
            showDeleteDownloadAlertEvent.observe(this@HomeActivity, Observer { model ->
                supportFragmentManager.fragments.last().confirmDownloadDeletion(model)
            })
            showPremiumDownloadEvent.observe(this@HomeActivity, Observer { model ->
                showPremiumDownloads(model)
            })
            promptRestoreDownloadsEvent.observe(this@HomeActivity, Observer { items ->
                showRestoreDownloadsDialog(items.size)
            })
            restoreDownloadsEvent.observe(this@HomeActivity, Observer { workInfo: WorkInfo? ->
                workInfo?.let {
                    onRestoreDownloadsStateChange(it.state)
                }
            })
            showEmailVerificationResultEvent.observe(this@HomeActivity, Observer { success ->
                if (success) {
                    AMSnackbar.Builder(this@HomeActivity)
                        .withTitle(getString(R.string.email_verification_toast_success))
                        .show()
                } else {
                    AMSnackbar.Builder(this@HomeActivity)
                        .withTitle(getString(R.string.email_verification_toast_failure))
                        .withDrawable(R.drawable.ic_snackbar_error)
                        .show()
                }
            })
            showInterstitialLoaderEvent.observe(this@HomeActivity) {
                if (it) showInterstitialLoader() else hideInterstitialLoader()
            }
            sleepTimerTriggeredEvent.observe(this@HomeActivity) {
                onSleepTimerTriggered()
            }
            showRatingPromptEvent.observe(this@HomeActivity, Observer {
                AMAlertFragment.Builder(this@HomeActivity)
                    .title(SpannableString(getString(R.string.inapprating_alert_title)))
                    .message(SpannableString(getString(R.string.inapprating_alert_message)))
                    .solidButton(SpannableString(getString(R.string.inapprating_alert_positive))) {
                        onRatingPromptAccepted()
                    }
                    .plain1Button(SpannableString(getString(R.string.inapprating_alert_negative))) {
                        onRatingPromptDeclined()
                    }
                    .show(supportFragmentManager)
            })
            showDeclinedRatingPromptEvent.observe(this@HomeActivity, Observer {
                AMAlertFragment.Builder(this@HomeActivity)
                    .title(SpannableString(getString(R.string.inapprating_alert_followup_title)))
                    .message(SpannableString(getString(R.string.inapprating_alert_followup_message)))
                    .solidButton(SpannableString(getString(R.string.inapprating_alert_followup_positive))) {
                        onDeclinedRatingPromptAccepted()
                    }
                    .plain1Button(SpannableString(getString(R.string.inapprating_alert_followup_negative))) {
                        onDeclinedRatingPromptDeclined()
                    }
                    .show(supportFragmentManager)
            })
            openAppRatingEvent.observe(this@HomeActivity, Observer {
                homeViewModel.onAppRatingRequested(this@HomeActivity)
            })
            showAddedToQueueToastEvent.observe(this@HomeActivity, Observer {
                showAddedToQueueToast()
            })
            showPasswordResetScreenEvent.observe(this@HomeActivity, Observer { token ->
                startActivity(ResetPasswordActivity.buildIntent(this@HomeActivity, token))
            })
            showPasswordResetErrorEvent.observe(this@HomeActivity, Observer {
                AMAlertFragment.Builder(this@HomeActivity)
                    .title(SpannableString(getString(R.string.reset_password_invalid_token_title)))
                    .message(SpannableString(getString(R.string.reset_password_invalid_token_message)))
                    .solidButton(SpannableString(getString(R.string.ok)), null)
                    .show(supportFragmentManager)
            })
            showSleepTimerPromptEvent.observe(this@HomeActivity, Observer { mode ->
                when (mode) {
                    SleepTimerPromptMode.Locked -> {
                        AMAlertFragment.Builder(this@HomeActivity)
                            .title(SpannableString(getString(R.string.sleep_timer_notification_title)))
                            .message(SpannableString(getString(R.string.sleep_timer_notification_subtitle)))
                            .solidButton(SpannableString(getString(R.string.sleep_timer_notification_button))) {
                                InAppPurchaseActivity.show(this@HomeActivity, SleepTimerPrompt)
                            }
                            .show(supportFragmentManager)
                    }
                    SleepTimerPromptMode.Unlocked -> {
                        AMAlertFragment.Builder(this@HomeActivity)
                            .title(SpannableString(getString(R.string.sleep_timer_notification_premium_title)))
                            .message(SpannableString(getString(R.string.sleep_timer_notification_premium_subtitle)))
                            .solidButton(SpannableString(getString(R.string.sleep_timer_notification_button))) {
                                SleepTimerAlertFragment.show(this@HomeActivity, SleepTimerSource.Prompt)
                            }
                            .show(supportFragmentManager)
                    }
                }
            })
            triggerFacebookExpressLoginEvent.observe(this@HomeActivity) {
                homeViewModel.runFacebookExpressLogin(this@HomeActivity)
            }
        }
    }

    fun handleDeeplink(deeplink: Deeplink) {
        Timber.tag("HomeViewModel").d("Handling deeplink $deeplink")
        when (deeplink) {
            // Support
            is Deeplink.Support -> HelpActivity.show(this)
            is Deeplink.SupportTicket -> {
                RequestActivity
                    .builder()
                    .withRequestId(deeplink.ticketId)
                    .show(this, ZendeskRepository().getUIConfigs())
            }
            // Generic
            is Deeplink.Premium -> {
                InAppPurchaseActivity.show(this@HomeActivity, deeplink.mode)
            }
            is Deeplink.Home -> closeFragments()
            is Deeplink.NowPlaying -> homeViewModel.onPlayerShowRequested()
            is Deeplink.Link -> openUrlExcludingAudiomack(deeplink.uri.toString())
            is Deeplink.EmailVerification -> homeViewModel.handleEmailVerification(deeplink.hash)
            is Deeplink.ChangePassword -> if (Credentials.isLogged(this)) {
                startActivity(Intent(this, ChangePasswordActivity::class.java))
            }
            is Deeplink.ResetPassword -> homeViewModel.handleResetPassword(deeplink.token)
            is Deeplink.Login -> AuthenticationActivity.show(this, LoginSignupSource.AppLaunch)
            is Deeplink.ArtistSelectOnboarding -> openArtistsSelectionOnboarding()
            // My Library
            is Deeplink.MyDownloads -> openMyAccount("downloads")
            is Deeplink.MyFavorites -> openMyAccount("favorites")
            is Deeplink.MyUploads -> openMyAccount("uploads")
            is Deeplink.MyPlaylists -> openMyAccount("playlists")
            is Deeplink.MyFollowers -> openMyAccount("followers")
            is Deeplink.MyFollowing -> openMyAccount("following")
            is Deeplink.Notifications -> homeViewModel.onNotificationsRequested()
            // Artist
            is Deeplink.Artist -> homeViewModel.onArtistScreenRequested(deeplink.id)
            is Deeplink.ArtistFavorites ->
                homeViewModel.onArtistScreenRequested(deeplink.id, "favorites")
            is Deeplink.ArtistUploads ->
                homeViewModel.onArtistScreenRequested(deeplink.id, "uploads")
            is Deeplink.ArtistPlaylists ->
                homeViewModel.onArtistScreenRequested(deeplink.id, "playlists")
            is Deeplink.ArtistFollowers ->
                homeViewModel.onArtistScreenRequested(deeplink.id, "followers")
            is Deeplink.ArtistFollowing ->
                homeViewModel.onArtistScreenRequested(deeplink.id, "following")
            is Deeplink.ArtistShare -> homeViewModel.onArtistScreenRequested(deeplink.id, openShare = true)
            // Music
            is Deeplink.Playlist -> requestPlaylist(deeplink.id, MixpanelSource(MainApplication.currentTab, MixpanelPageDeeplink))
            is Deeplink.PlaylistShare -> requestPlaylist(deeplink.id, MixpanelSource(MainApplication.currentTab, MixpanelPageDeeplink), true)
            is Deeplink.PlaylistPlay -> homeViewModel.onPlayRemoteMusicRequested(deeplink.id, MusicType.Playlist, deeplink.mixpanelSource)
            is Deeplink.Album -> requestAlbum(deeplink.id, MixpanelSource(MainApplication.currentTab, MixpanelPageDeeplink))
            is Deeplink.AlbumShare -> requestAlbum(deeplink.id, MixpanelSource(MainApplication.currentTab, MixpanelPageDeeplink), true)
            is Deeplink.AlbumPlay -> homeViewModel.onPlayRemoteMusicRequested(deeplink.id, MusicType.Album, deeplink.mixpanelSource)
            is Deeplink.Song -> homeViewModel.onSongRequested(deeplink.id, MixpanelSource(MainApplication.currentTab, MixpanelPageDeeplink))
            is Deeplink.SongShare -> homeViewModel.onSongRequested(deeplink.id, MixpanelSource(MainApplication.currentTab, MixpanelPageDeeplink), true)
            is Deeplink.Playlists -> openPlaylists(deeplink.tag)
            is Deeplink.World -> openBrowse(tab = "world")
            is Deeplink.WorldPage -> openBrowse(tab = "world", worldPage = deeplink.page)
            is Deeplink.WorldPost -> openPostDetail(deeplink.slug)
            is Deeplink.TopSongs -> openBrowse(deeplink.genre, "songs")
            is Deeplink.TopAlbums -> openBrowse(deeplink.genre, "albums")
            is Deeplink.Trending -> openBrowse(deeplink.genre, "trending")
            is Deeplink.RecentlyAdded -> openBrowse(deeplink.genre, "recent")
            is Deeplink.AddToQueue -> homeViewModel.playLater(deeplink.id, deeplink.type, deeplink.mixpanelSource)
            // Suggested accounts
            is Deeplink.SuggestedFollows -> openSuggestedAccounts()
            // Internal usage only
            is Deeplink.Timeline -> openFeedScreen()
            is Deeplink.Search -> openSearch(deeplink.query, deeplink.searchType)
            is Deeplink.Comments -> homeViewModel.onCommentsRequested(deeplink.id, deeplink.type, deeplink.uuid, deeplink.threadId)
            is Deeplink.Benchmark -> homeViewModel.onBenchmarkRequested(deeplink.entityId, deeplink.entityType, deeplink.benchmark, MixpanelSource(MainApplication.currentTab, MixpanelPageDeeplink), MixpanelButtonExternal)
        }
    }

    private fun initAlertEventObservers() {
        val owner = this
        homeViewModel.run {
            genericErrorEvent.observe(owner) {
                AMSnackbar.Builder(owner)
                    .withDrawable(R.drawable.ic_snackbar_error)
                    .withTitle(R.string.generic_error_occurred)
                    .show()
            }

            itemAddedToQueueEvent.observe(owner) {
                AMSnackbar.Builder(owner)
                    .withDrawable(R.drawable.ic_snackbar_queue)
                    .withTitle(R.string.queue_added)
                    .show()
            }

            localFilesSelectionSuccessEvent.observe(owner) {
                AMSnackbar.Builder(owner)
                    .withDrawable(R.drawable.ic_snackbar_success)
                    .withTitle(R.string.local_file_selection_applied)
                    .show()
            }

            storagePermissionDenied.observe(owner) {
                AMSnackbar.Builder(owner)
                    .withDrawable(R.drawable.ic_snackbar_error)
                    .withTitle(R.string.local_file_selection_permissions_rationale)
                    .show()
            }

            adEvent.observe(owner) { title ->
                AMSnackbar.Builder(owner)
                    .withDrawable(R.drawable.ic_snackbar_ad)
                    .withTitle(title)
                    .show()
            }

            playUnsupportedFileAttempt.observe(owner) {
                AMProgressHUD.show(this@HomeActivity, ProgressHUDMode.Failure(
                    getString(R.string.local_file_unsupported)
                ))
            }
        }
    }

    private fun showRestoreDownloadsDialog(size: Int) {
        AMAlertFragment.Builder(this)
            .title(
                SpannableString(getString(if (size == 1) R.string.restore_downloads_prompt_title_singular else R.string.restore_downloads_prompt_title_plural, size))
            )
            .solidButton(
                SpannableString(getString(R.string.restore_downloads_dialog_positive))
            ) { homeViewModel.onRestoreDownloadsRequested(size) }
            .plain1Button(
                spannableString(
                    fullString = getString(R.string.restore_downloads_dialog_select),
                    highlightedStrings = listOf(getString(R.string.restore_downloads_dialog_select_highlighted)),
                    highlightedColor = colorCompat(R.color.orange)
                )
            ) {
                homeViewModel.onRestoreDownloadsRejected(size)
                openMyAccount(
                    "downloads",
                    offlineCategory = getString(R.string.offline_filter_notondevice)
                )
            }
            .dismissWithoutSelectionHandler { homeViewModel.onRestoreDownloadsRejected(size) }
            .dismissOnTouchOutside(false)
            .cancellable(false)
            .show(supportFragmentManager)
    }

    private fun onRestoreDownloadsStateChange(state: WorkInfo.State) {
        // Sometimes the work is enqueued shortly even when constraints are met
        if (state == ENQUEUED && Reachability.getInstance().connectedToWiFi) return

        val (drawable, title) = when (state) {
            ENQUEUED -> Pair(
                R.drawable.ic_download,
                R.string.restore_downloads_queued
            )
            RUNNING -> Pair(
                R.drawable.ic_download,
                R.string.restore_downloads_started
            )
            FAILED -> Pair(
                R.drawable.ic_snackbar_download_failure,
                R.string.restore_downloads_error
            )
            else -> return
        }

        AMSnackbar.Builder(this@HomeActivity)
            .withDrawable(drawable)
            .withTitle(getString(title))
            .show()
    }

    private fun initPlayerViewModel(): PlayerViewModel {
        return ViewModelProvider(this).get(PlayerViewModel::class.java).apply {
            playEvent.observe(this@HomeActivity, Observer {
                startMusicService(true)
            })
            minimizeEvent.observe(this@HomeActivity, Observer {
                minimizePlayer(true)
            })
            showQueueEvent.observe(this@HomeActivity, Observer {
                showQueueFragment()
            })
            searchArtistEvent.observe(this@HomeActivity, Observer {
                homeViewModel.onSearchRequested(it.trim(), SearchType.NowPlaying)
            })
            showArtworkEvent.observe(this@HomeActivity, Observer { url ->
                openImageZoomFragment(ImageZoomFragment.newInstance(url))
            })
            showInAppPurchaseEvent.observe(this@HomeActivity, Observer {
                InAppPurchaseActivity.show(this@HomeActivity, InAppPurchaseMode.HiFi)
            })
            showPlaylistTooltipEvent.observe(this@HomeActivity, Observer { location ->
                val tooltipFragment = TooltipFragment.newInstance(
                    getString(R.string.tooltip_playlists),
                    R.drawable.tooltip_playlists,
                    location,
                    Runnable { homeViewModel.onPlayerPlaylistTooltipClosed() }
                )
                openTooltipFragment(tooltipFragment)
            })
            showQueueTooltipEvent.observe(this@HomeActivity, Observer { location ->
                val tooltipFragment = TooltipFragment.newInstance(
                    getString(R.string.tooltip_player_queue),
                    R.drawable.tooltip_player_queue,
                    location,
                    Runnable { homeViewModel.onPlayerQueueTooltipClosed() }
                )
                openTooltipFragment(tooltipFragment)
            })
            removeAdsEvent.observe(this@HomeActivity, Observer {
                InAppPurchaseActivity.show(this@HomeActivity, InAppPurchaseMode.NowPlayingAdDismissal)
            })
            adRefreshEvent.observe(this@HomeActivity, Observer {
                if (isPlayerMaximized() && isTaskRoot && !isTooltipVisible()) {
                    homeViewModel.showPlayerAd(it)
                }
            })
            loginRequiredEvent.observe(this@HomeActivity, Observer { source ->
                showLoginRequiredAlert(source)
            })
            addToPlaylistEvent.observe(this@HomeActivity, Observer { (songs, source, button) ->
                AddToPlaylistsActivity.show(this@HomeActivity, AddToPlaylistModel(songs, source, button))
            })
            shareEvent.observe(this@HomeActivity, Observer { item ->
                item.openShareSheet(this@HomeActivity, item.mixpanelSource ?: MixpanelSource.empty, MixpanelButtonNowPlaying)
            })
            openParentAlbumEvent.observe(this@HomeActivity, Observer { (albumId, mixpanelSource) ->
                requestAlbum(albumId, mixpanelSource ?: MixpanelSource.empty)
            })
            openParentPlaylistEvent.observe(this@HomeActivity, Observer { (playlistId, mixpanelSource) ->
                requestPlaylist(playlistId, mixpanelSource ?: MixpanelSource.empty)
            })
            notifyOfflineEvent.observe(this@HomeActivity, Observer {
                showOfflineAlert()
            })
            notifyRepostEvent.observe(this@HomeActivity, Observer {
                showRepostedToast(it)
            })
            notifyFavoriteEvent.observe(this@HomeActivity, Observer {
                showFavoritedToast(it)
            })
        }
    }

    private fun initNowPlayingViewModel(): NowPlayingViewModel {
        return ViewModelProvider(this).get(NowPlayingViewModel::class.java).apply {
            itemLoadedEvent.observe(this@HomeActivity, Observer {
                startMusicService(false)
            })
            playerVisibilityChangeEvent.observe(this@HomeActivity, Observer { playerVisible ->
                if (playerVisible) {
                    val adYStartValue = (adLayout.layoutParams as FrameLayout.LayoutParams).bottomMargin
                    val adYEndValue = resources.getDimension(R.dimen.minified_player_height)
                    miniPlayerContainer.animate()
                        .translationY(tabbarLayout.height.toFloat())
                        .setDuration(PLAYER_ANIMATION_DURATION)
                        .setUpdateListener {
                            val adTranslateY = adYStartValue + (adYEndValue - adYStartValue) * it.animatedFraction
                            (adLayout.layoutParams as FrameLayout.LayoutParams).apply {
                                bottomMargin = adTranslateY.toInt()
                            }.also { adLayout.requestLayout() }
                        }
                        .withStartAction { miniPlayerContainer.visibility = View.VISIBLE }
                } else {
                    val adYStartValue = (adLayout.layoutParams as FrameLayout.LayoutParams).bottomMargin
                    val adYEndValue = 0
                    miniPlayerContainer.animate()
                        .translationY((miniPlayerContainer.height + tabbarLayout.height).toFloat())
                        .setDuration(PLAYER_ANIMATION_DURATION)
                        .setUpdateListener {
                            val adTranslateY = adYStartValue + (adYEndValue - adYStartValue) * it.animatedFraction
                            (adLayout.layoutParams as FrameLayout.LayoutParams).apply {
                                bottomMargin = adTranslateY.toInt()
                            }.also { adLayout.requestLayout() }
                        }
                        .withEndAction { miniPlayerContainer.visibility = View.GONE }
                }
            })
            launchEqEvent.observe(this@HomeActivity, Observer { audioSessionId ->
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                    .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                if (audioSessionId != null) {
                    intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                }
                if ((intent.resolveActivity(packageManager) != null)) {
                    startActivityForResult(intent, 123)
                }
            })
            launchUpgradeEvent.observe(this@HomeActivity, Observer { mode ->
                InAppPurchaseActivity.show(this@HomeActivity, mode)
            })
            showEqTooltipEvent.observe(this@HomeActivity, Observer { location ->
                playerViewModel.blockAds()
                val tooltipFragment = TooltipFragment.newInstance(
                    getString(R.string.tooltip_player_equalizer),
                    R.drawable.ic_eq,
                    location,
                    Runnable { homeViewModel.onPlayerEqTooltipClosed() }
                )
                openTooltipFragment(tooltipFragment)
            })
            showScrollTooltipEvent.observe(this@HomeActivity, Observer { location ->
                playerViewModel.blockAds()
                val tooltipFragment = TooltipFragment.newInstance(
                    getString(R.string.tooltip_player_scroll),
                    R.drawable.ic_music_note_sixteenth,
                    location,
                    Runnable {
                        homeViewModel.onPlayerScrollTooltipShown()
                        nowPlayingViewModel.onTooltipDismissed()
                    }
                )
                openTooltipFragment(tooltipFragment)
            })
        }
    }

    private fun initMusicViewModel(): MusicViewModel =
        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))
            .get(MusicViewModel::class.java)

    private fun initPlayerInfoViewModel(): PlayerInfoViewModel {
        return ViewModelProvider(this).get(PlayerInfoViewModel::class.java).apply {
            searchTagEvent.observe(this@HomeActivity, Observer {
                homeViewModel.onSearchRequested(it.trim(), SearchType.Tag)
            })
        }
    }

    private fun setAudioAdViewModelObservers() {
        val context = this@HomeActivity
        audioAdViewModel.apply {
            audioAdEvent.observe(context) { audioAdPlaying ->
                if (audioAdPlaying) {
                    showAudioAdFragment()
                } else {
                    removeAudioAdFragment()
                }
            }
            upSellClickEvent.observe(context) { startTrial ->
                InAppPurchaseActivity.show(context, AudioAd, startTrial)
            }
        }
    }

    private fun initClickListeners() {
        layoutFeed.setOnClickListener { homeViewModel.onFeedTabClicked() }
        layoutPlaylists.setOnClickListener { homeViewModel.onPlaylistsTabClicked() }
        layoutBrowse.setOnClickListener { homeViewModel.onBrowseTabClicked() }
        layoutSearch.setOnClickListener { homeViewModel.onSearchTabClicked() }
        layoutMyLibrary.setOnClickListener { homeViewModel.onMyLibraryTabClicked() }
        buttonRemoveAd.setOnClickListener { homeViewModel.onRemoveBannerClicked() }
    }

    private fun initCast() {
        try {
            CastContext.getSharedInstance(applicationContext)
        } catch (e: Exception) {
            homeViewModel.onCastInitException()
        }
    }

    private fun initPlayer() {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.miniPlayerContainer, MinifiedPlayerFragment())
            .commit()

        nowPlayingFragment = NowPlayingFragment.newInstance().apply {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.playerContainer, this)
                .runOnCommit {
                    homeViewModel.onPlayerInstantiated()
                }
                .commit()
        }
    }

    private fun openArtistsSelectionOnboarding() {
        val fragment = ArtistsOnboardingFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fullScreenContainer, fragment, ArtistsOnboardingFragment.TAG)
            .addToBackStack(ArtistsOnboardingFragment.TAG)
            .commitAllowingStateLoss()
    }

    private fun openPlaylists(tag: String? = null) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainContainer, PlaylistsFragment.newInstance(tag))
            .commitAllowingStateLoss()
    }

    fun showLoginRequiredAlert(source: LoginSignupSource) {
        AMAlertFragment.show(
            this@HomeActivity,
            SpannableString(LoginAlertUseCase().getMessage(this@HomeActivity)),
            null,
            getString(R.string.login_needed_yes),
            getString(R.string.login_needed_no),
            Runnable { homeViewModel.onLoginRequiredAccepted(source) },
            Runnable { homeViewModel.onLoginRequiredDeclined() },
            Runnable { homeViewModel.onLoginRequiredDeclined() }
        )
    }

    // Public methods (to be changed)

    fun openOnboardingPlaylist(customImage: String, playlist: AMResultItem) {
        supportFragmentManager.popBackStack()
        val fragment = PlaylistOnboardingFragment.newInstance(customImage, playlist)
        supportFragmentManager
            .beginTransaction()
            .add(R.id.mainContainer, fragment)
            .addToBackStack(fragment::class.java.simpleName)
            .commitAllowingStateLoss()
        minimizePlayer(false)
    }

    fun openFilters(filterFragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fullScreenContainer, filterFragment, "filters")
            .addToBackStack("filters")
            .commit()
    }

    fun openEditDownloads(fragment: EditDownloadsFragment) {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fullScreenContainer, fragment, EditDownloadsFragment.TAG)
            .addToBackStack(EditDownloadsFragment.TAG)
            .commit()
    }

    fun openMusicInfo(item: AMResultItem) {
        val musicInfoFragment = MusicInfoFragment.newInstance(item)
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fullScreenContainer, musicInfoFragment)
            .addToBackStack(musicInfoFragment::class.java.simpleName)
            .commitAllowingStateLoss()
    }

    fun openArtistMore(artist: AMArtist) {
        val accountInfoFragment = ArtistInfoFragment.newInstance(artist)
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fullScreenContainer, accountInfoFragment)
            .addToBackStack(accountInfoFragment::class.java.simpleName)
            .commitAllowingStateLoss()
        closeFragments()
    }

    private fun openFeedScreen() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainContainer, FeedFragment.newInstance())
            .commitAllowingStateLoss()
    }

    fun openBrowse(genre: String? = null, tab: String? = null, worldPage: WorldPage? = null) {
        minimizePlayer(true)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainContainer, BrowseFragment.newInstance(tab, genre, worldPage))
            .commitAllowingStateLoss()
    }

    fun openPostDetail(slug: String? = null) {
        supportFragmentManager
                .beginTransaction()
                .add(R.id.mainContainer, WorldArticleFragment.newInstance(slug ?: ""))
                .addToBackStack(WorldArticleFragment::class.java.simpleName)
                .commitAllowingStateLoss()
    }

    fun openSearch(query: String? = null, searchType: SearchType? = null) {
        (supportFragmentManager.fragments.lastOrNull() as? SearchFragment)
            ?.takeIf { query == null }
            ?.viewModel?.onKeyboardFocusRequested()
            ?: run {
                minimizePlayer(true)
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.mainContainer, SearchFragment.newInstance(query, searchType))
                    .commitAllowingStateLoss()
            }
    }

    fun openMyAccount(
        tab: String? = null,
        playlistsCategory: String? = null,
        offlineCategory: String? = null
    ) {
        closeFragments()
        val accountFragment = MyLibraryFragment.newInstance(tab, playlistsCategory, offlineCategory)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainContainer, accountFragment)
            .commitAllowingStateLoss()
    }

    fun openSuggestedAccounts() {
        supportFragmentManager.commit(true) {
            add(R.id.mainContainer, SuggestedAccountsFragment.newInstance(), SuggestedAccountsFragment.TAG)
            addToBackStack(SuggestedAccountsFragment.TAG)
        }
    }

    private fun closeNotificationsIfOpen() {
        supportFragmentManager.findFragmentByTag(NotificationsContainerFragment.TAG)?.let {
            // Need to close DataNotificationUpdatedPlaylistsFragment and NotificationsContainerFragment
            onBackPressed()
            onBackPressed()
        }
    }

    fun openComments(music: AMResultItem?, comment: AMComment?, comments: ArrayList<AMComment>?) {
        try {
            closeMusicInfo()
            minimizePlayer(true)
            closeTooltipFragment()

            val commentsFragment = CommentsFragment.newInstance(if (comment != null) CommentsFragment.Mode.Single else CommentsFragment.Mode.Standalone, music, comment, comments)
            val fragmentTag = commentsFragment::class.java.simpleName

            (supportFragmentManager.findFragmentByTag(fragmentTag) as? CommentsFragment)?.takeIf { it.isDisplayingSameData(music, comments) }?.let {
                supportFragmentManager
                    .beginTransaction()
                    .remove(it)
                    .commitAllowingStateLoss()
            }

            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_left, R.anim.slide_right)
                .add(R.id.mainContainer, commentsFragment, fragmentTag)
                .addToBackStack(fragmentTag)
                .commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    fun openAlbum(album: AMResultItem, externalSource: MixpanelSource, openShare: Boolean) {
        try {
            val albumFragment = AlbumFragment.newInstance(album, externalSource, openShare)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.mainContainer, albumFragment)
                .addToBackStack(albumFragment::class.java.simpleName)
                .commitAllowingStateLoss()
            minimizePlayer(false)
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    fun openPlaylist(playlist: AMResultItem, online: Boolean = true, deleted: Boolean = false, mixpanelSource: MixpanelSource, openShare: Boolean) {
        try {
            closeNotificationsIfOpen()
            val playlistFragment = PlaylistFragment.newInstance(playlist, online, deleted, mixpanelSource, openShare)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.mainContainer, playlistFragment)
                .addToBackStack(playlistFragment::class.java.simpleName)
                .commitAllowingStateLoss()
            minimizePlayer(false)
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    fun openArtist(artist: AMArtist, tab: String? = null, openShare: Boolean = false) {
        try {
            closeMusicInfo()
            minimizePlayer(true)
            closeTooltipFragment()

            val artistFragment = ArtistFragment.newInstance(tab, null, true, openShare)
            artistFragment.setArtist(artist)
            val fragmentTag = artistFragment::class.java.simpleName

            (supportFragmentManager.findFragmentByTag(fragmentTag) as? ArtistFragment)
                ?.takeIf { it.isDisplayingSameData(artist) }
                ?.let { previousArtistFragment ->
                    supportFragmentManager
                        .beginTransaction()
                        .remove(previousArtistFragment)
                        .commitAllowingStateLoss()
                }

            supportFragmentManager
                .beginTransaction()
                .add(R.id.mainContainer, artistFragment, fragmentTag)
                .addToBackStack(artistFragment::class.java.simpleName)
                .commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    private fun openScreenshotWithBenchmark(music: AMResultItem?, benchmark: BenchmarkModel, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        try {
            homeViewModel.onBenchmarkOpened(this@HomeActivity, music, null, benchmark, mixpanelSource, mixpanelButton)
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    private fun showPremiumDownloads(model: PremiumDownloadModel) {

        model.alertTypeLimited?.let { alertType ->
            // Need to show an alert
            when (alertType) {
                PremiumLimitedDownloadAlertViewType.ReachedLimit ->
                    AMAlertFragment.Builder(this)
                        .title(SpannableString(getString(R.string.premium_limited_reached_limit_alert_title)))
                        .message(SpannableString(getString(R.string.premium_limited_reached_limit_alert_message)))
                        .solidButton(SpannableString(getString(R.string.premium_limited_reached_limit_alert_ugprade))) {
                            InAppPurchaseActivity.show(this@HomeActivity, InAppPurchaseMode.PremiumDownload)
                        }
                        .outlineButton(SpannableString(getString(R.string.premium_limited_reached_limit_alert_replace))) {
                            openReplaceDownloads(model)
                        }
                        .plain1Button(
                            spannableString(
                                fullString = getString(R.string.premium_limited_reached_limit_alert_learn),
                                highlightedStrings = listOf(getString(R.string.premium_limited_reached_limit_alert_learn_highlighted)),
                                highlightedColor = colorCompat(R.color.orange)
                            )
                        ) {
                            openUrlExcludingAudiomack(PREMIUM_SUPPORT_URL)
                        }
                        .drawableResId(R.drawable.ic_premium_download_alert_star)
                        .show(supportFragmentManager)

                PremiumLimitedDownloadAlertViewType.DownloadAlbumLargerThanLimit ->
                    AMAlertFragment.Builder(this)
                        .title(SpannableString(getString(R.string.premium_download_alert_exceed_limit_title)))
                        .message(SpannableString(getString(R.string.premium_download_alert_exceed_limit_subtitle)))
                        .solidButton(SpannableString(getString(R.string.premium_download_alert_upgrade))) {
                            InAppPurchaseActivity.show(this@HomeActivity, InAppPurchaseMode.PremiumDownload)
                        }
                        .plain1Button(SpannableString(getString(R.string.premium_download_alert_exceed_limit_cancel)))
                        .show(supportFragmentManager)

                PremiumLimitedDownloadAlertViewType.DownloadAlbumLargerThanLimitAlreadyDownloaded ->
                    AMAlertFragment.Builder(this)
                        .title(SpannableString(getString(R.string.premium_download_alert_exceed_limit_title)))
                        .message(SpannableString(getString(R.string.premium_download_alert_exceed_limit_subtitle)))
                        .solidButton(SpannableString(getString(R.string.premium_download_alert_upgrade))) {
                            InAppPurchaseActivity.show(this@HomeActivity, InAppPurchaseMode.PremiumDownload)
                        }
                        .plain1Button(
                            spannableString(
                                fullString = getString(R.string.premium_download_alert_exceed_limit_delete),
                                fullColor = colorCompat(R.color.orange)
                            )
                        ) {
                            model.music?.let { homeViewModel.onDeleteDownloadRequested(it.musicId) }
                        }
                        .plain2Button(SpannableString(getString(R.string.premium_download_alert_exceed_limit_cancel)))
                        .show(supportFragmentManager)

                PremiumLimitedDownloadAlertViewType.PlayFrozenOffline ->
                    AMAlertFragment.Builder(this)
                        .title(SpannableString(getString(R.string.premium_limited_play_frozen_offline_alert_title)))
                        .message(SpannableString(getString(R.string.premium_limited_play_frozen_offline_alert_message, model.stats.replaceCount(model.music?.countOfSongsToBeDownloaded ?: 1))))
                        .solidButton(SpannableString(getString(R.string.premium_limited_play_frozen_offline_alert_ugprade))) {
                            InAppPurchaseActivity.show(this@HomeActivity, InAppPurchaseMode.PremiumDownload)
                        }
                        .outlineButton(SpannableString(getString(R.string.premium_limited_play_frozen_offline_alert_replace))) {
                            openReplaceDownloads(model)
                        }
                        .plain1Button(
                            spannableString(
                                fullString = getString(R.string.premium_limited_play_frozen_offline_alert_stream),
                                highlightedStrings = listOf(getString(R.string.premium_limited_play_frozen_offline_alert_stream_highlighted)),
                                highlightedColor = colorCompat(R.color.orange)
                            )
                        ) {
                            model.music?.let {
                                homeViewModel.streamFrozenMusic(
                                    this@HomeActivity,
                                    it.musicId,
                                    it.type,
                                    model.stats.mixpanelSource,
                                    model.stats.mixpanelButton,
                                    model.actionToBeResumed
                                )
                            }
                        }
                        .plain2Button(
                            spannableString(
                                fullString = getString(R.string.premium_limited_play_frozen_offline_alert_delete),
                                fullColor = colorCompat(R.color.orange)
                            )
                        ) {
                            model.music?.let { homeViewModel.onDeleteDownloadRequested(it.musicId) }
                        }
                        .drawableResId(R.drawable.ic_premium_download_alert_lock_open)
                        .show(supportFragmentManager)

                PremiumLimitedDownloadAlertViewType.DownloadFrozen ->
                    AMAlertFragment.Builder(this)
                        .title(SpannableString(getString(R.string.premium_limited_download_frozen_offline_alert_title)))
                        .message(SpannableString(getString(R.string.premium_limited_download_frozen_offline_alert_message, model.stats.replaceCount(model.music?.countOfSongsToBeDownloaded ?: 1))))
                        .solidButton(SpannableString(getString(R.string.premium_limited_download_frozen_offline_alert_ugprade))) {
                            InAppPurchaseActivity.show(this@HomeActivity, InAppPurchaseMode.PremiumDownload)
                        }
                        .outlineButton(SpannableString(getString(R.string.premium_limited_download_frozen_offline_alert_replace))) {
                            openReplaceDownloads(model)
                        }
                        .plain1Button(
                            spannableString(
                                fullString = getString(R.string.premium_limited_download_frozen_offline_alert_stream),
                                highlightedStrings = listOf(getString(R.string.premium_limited_download_frozen_offline_alert_stream_highlighted)),
                                highlightedColor = colorCompat(R.color.orange)
                            )
                        ) {
                            model.music?.let {
                                homeViewModel.streamFrozenMusic(
                                    this@HomeActivity,
                                    it.musicId,
                                    it.type,
                                    model.stats.mixpanelSource,
                                    model.stats.mixpanelButton,
                                    model.actionToBeResumed
                                )
                            }
                        }
                        .plain2Button(
                            spannableString(
                                fullString = getString(R.string.premium_limited_download_frozen_offline_alert_delete),
                                fullColor = colorCompat(R.color.orange)
                            )
                        ) {
                            model.music?.let { homeViewModel.onDeleteDownloadRequested(it.musicId) }
                        }
                        .drawableResId(R.drawable.ic_premium_download_alert_lock_open)
                        .show(supportFragmentManager)

                PremiumLimitedDownloadAlertViewType.PlayFrozenOfflineWithAvailableUnfreezes ->
                    AMAlertFragment.Builder(this)
                        .title(SpannableString(getString(R.string.premium_limited_play_frozen_offline_available_unfreezes_alert_title)))
                        .message(SpannableString(getString(R.string.premium_limited_play_frozen_offline_available_unfreezes_alert_message, model.stats.replaceCount(model.music?.countOfSongsToBeDownloaded ?: 1))))
                        .solidButton(SpannableString(getString(R.string.premium_limited_play_frozen_offline_available_unfreezes_alert_ugprade))) {
                            InAppPurchaseActivity.show(this@HomeActivity, InAppPurchaseMode.PremiumDownload)
                        }
                        .outlineButton(SpannableString(getString(R.string.premium_limited_play_frozen_offline_available_unfreezes_alert_replace))) {
                            model.music?.let { homeViewModel.unlockFrozenDownload(it.musicId) }
                        }
                        .plain1Button(
                            spannableString(
                                fullString = getString(R.string.premium_limited_play_frozen_offline_available_unfreezes_alert_stream),
                                highlightedStrings = listOf(getString(R.string.premium_limited_play_frozen_offline_available_unfreezes_alert_stream_highlighted)),
                                highlightedColor = colorCompat(R.color.orange)
                            )
                        ) {
                            model.music?.let {
                                homeViewModel.streamFrozenMusic(
                                    this@HomeActivity,
                                    it.musicId,
                                    it.type,
                                    model.stats.mixpanelSource,
                                    model.stats.mixpanelButton,
                                    model.actionToBeResumed
                                )
                            }
                        }
                        .plain2Button(
                            spannableString(
                                fullString = getString(R.string.premium_limited_play_frozen_offline_available_unfreezes_alert_delete),
                                fullColor = colorCompat(R.color.orange)
                            )
                        ) {
                            model.music?.let { homeViewModel.onDeleteDownloadRequested(it.musicId) }
                        }
                        .drawableResId(R.drawable.ic_premium_download_alert_lock_open)
                        .show(supportFragmentManager)
            }
            homeViewModel.onPremiumDownloadNotificationShown(PremiumDownloadType.Limited)
            return
        }

        model.alertTypePremium?.let { alertType ->
            // Need to show an alert
            when (alertType) {
                PremiumOnlyDownloadAlertViewType.Download ->
                    AMAlertFragment.Builder(this)
                        .title(SpannableString(getString(R.string.premium_only_downloads_alert_title)))
                        .message(SpannableString(getString(R.string.premium_only_downloads_alert_subtitle)))
                        .solidButton(SpannableString(getString(R.string.premium_only_downloads_alert_yes))) {
                            InAppPurchaseActivity.show(this@HomeActivity, InAppPurchaseMode.PremiumDownload)
                        }
                        .plain1Button(SpannableString(getString(R.string.premium_only_downloads_alert_no)))
                        .drawableResId(R.drawable.ic_premium_download_alert_download)
                        .show(supportFragmentManager)

                PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline ->
                    AMAlertFragment.Builder(this)
                        .title(SpannableString(getString(R.string.premium_only_play_frozen_offline_alert_title)))
                        .message(SpannableString(getString(R.string.premium_only_play_frozen_offline_alert_message)))
                        .solidButton(SpannableString(getString(R.string.premium_only_play_frozen_offline_alert_ugprade))) {
                            InAppPurchaseActivity.show(this@HomeActivity, InAppPurchaseMode.PremiumDownload)
                        }
                        .plain1Button(
                            spannableString(
                                fullString = getString(R.string.premium_only_play_frozen_offline_alert_stream),
                                highlightedStrings = listOf(getString(R.string.premium_only_play_frozen_offline_alert_stream_highlighted)),
                                highlightedColor = colorCompat(R.color.orange)
                            )
                        ) {
                            model.music?.let {
                                homeViewModel.streamFrozenMusic(
                                    this@HomeActivity,
                                    it.musicId,
                                    it.type,
                                    model.stats.mixpanelSource,
                                    model.stats.mixpanelButton,
                                    model.actionToBeResumed
                                )
                            }
                        }
                        .plain2Button(
                            spannableString(
                                fullString = getString(R.string.premium_only_play_frozen_offline_alert_delete),
                                fullColor = colorCompat(R.color.orange)
                            )
                        ) {
                            model.music?.let { homeViewModel.onDeleteDownloadRequested(it.musicId) }
                        }
                        .drawableResId(R.drawable.ic_premium_download_alert_lock_close)
                        .show(supportFragmentManager)
            }
            homeViewModel.onPremiumDownloadNotificationShown(PremiumDownloadType.PremiumOnly)
            return
        }

        // Need to show the fullscreen view
        val premiumDownloadFragment = PremiumDownloadFragment.newInstance(model)
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fullScreenContainer, premiumDownloadFragment)
            .addToBackStack(premiumDownloadFragment::class.java.simpleName)
            .commitAllowingStateLoss()
        homeViewModel.onPremiumDownloadNotificationShown(PremiumDownloadType.Limited)
    }

    private fun openReplaceDownloads(model: PremiumDownloadModel) {
        val replaceCount = model.stats.replaceCount(model.music?.countOfSongsToBeDownloaded ?: 1)
        if (replaceCount > model.stats.premiumLimitCount) {
            showPremiumDownloads(model.copy(
                alertTypeLimited = PremiumLimitedDownloadAlertViewType.DownloadAlbumLargerThanLimit
            ))
            return
        }
        try {
            val replaceDownloadFragment = ReplaceDownloadFragment.newInstance(model)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fullScreenContainer, replaceDownloadFragment)
                .addToBackStack(replaceDownloadFragment::class.java.simpleName)
                .commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    fun closeTooltipFragment() {
        try {
            tooltipFragmentReference?.get()?.let {
                supportFragmentManager.popBackStackImmediate()
            }
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    fun openTooltipFragment(tooltipFragment: TooltipFragment) {
        closeTooltipFragment()
        tooltipFragmentReference = WeakReference(tooltipFragment)
        try {
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, 0, 0, R.anim.fade_out)
                .add(
                    R.id.fullScreenContainer,
                    tooltipFragment,
                    TooltipFragment.FRAGMENT_TAG
                )
                .addToBackStack(TooltipFragment.FRAGMENT_TAG)
                .commit()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    fun requestPlaylist(id: String, mixpanelSource: MixpanelSource, openShare: Boolean = false) {
        supportFragmentManager.fragments
            .lastOrNull()
            ?.takeIf { it is PlaylistFragment && it.playlistId == id }
            ?.let { minimizePlayer(true) }
            ?: homeViewModel.onPlaylistRequested(id, mixpanelSource, openShare)
    }

    fun requestAlbum(id: String, mixpanelSource: MixpanelSource, openShare: Boolean = false) {
        supportFragmentManager.fragments
            .lastOrNull()
            ?.takeIf { it is AlbumFragment && it.album.itemId == id }
            ?.let { minimizePlayer(true) }
            ?: homeViewModel.onAlbumRequested(id, mixpanelSource, openShare)
    }

    fun requestPremiumDownloads(model: PremiumDownloadModel) {
        homeViewModel.onPremiumDownloadsRequested(model)
    }

    fun requestShuffled(nextPageData: NextPageData, firstPage: List<AMResultItem> = listOf()) {
        homeViewModel.onShuffleRequested(nextPageData, firstPage)
    }

    private fun isTooltipVisible(): Boolean {
        return supportFragmentManager.findFragmentByTag(TooltipFragment.FRAGMENT_TAG) != null
    }

    fun isPlayerMaximized(): Boolean {
        return nowPlayingViewModel.isMaximized
    }

    fun dragPlayer(deltaY: Int, direction: PlayerDragDirection) {
        if (nowPlayingFragment == null) {
            Timber.tag(TAG).w("Player is already closed, ignoring drag event")
            return
        }

        var translate = deltaY

        val playerHeight = playerContainer.height

        if (direction == PlayerDragDirection.UP) {

            val tabbarHeight = tabbarLayout.height
            val miniPlayerHeight = miniPlayerContainer.height
            val adTranslationWhenPlayerMinimized = (resources.getDimension(R.dimen.tabbar_layout_height) + resources.getDimension(R.dimen.minified_player_height)).roundToInt()

            translate = playerHeight - (deltaY + tabbarHeight + miniPlayerHeight)

            val tabbarTranslate = max(0, min(tabbarHeight, deltaY))
            val miniPlayerTranslate = max(0, min(miniPlayerHeight + tabbarHeight, deltaY))
            val adTranslate = max(0, min(adTranslationWhenPlayerMinimized, adTranslationWhenPlayerMinimized - deltaY))

            tabbarLayout.translationY = tabbarTranslate.toFloat()
            miniPlayerContainer.translationY = miniPlayerTranslate.toFloat()
            (adLayout.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = adTranslate
            }.also { adLayout.requestLayout() }
        }

        if ((isPlayerMaximized() && translate < 0) || (!isPlayerMaximized() && translate > playerHeight)) {
            return
        }

        playerContainer.translationY = translate.toFloat()
    }

    fun resetPlayerDrag(duration: Int, direction: PlayerDragDirection) {
        if (direction == PlayerDragDirection.DOWN) {
            playerContainer.animate()
                .translationY(0.toFloat())
                .setDuration(duration.toLong())
                .start()
        }

        if (direction == PlayerDragDirection.UP) {
            playerContainer.animate()
                .translationY(playerContainer.height.toFloat())
                .setDuration(duration.toLong())
                .start()

            tabbarLayout.animate()
                .translationY(0.toFloat())
                .setDuration(duration.toLong())
                .start()

            miniPlayerContainer.translationY = 0.toFloat()

            (adLayout.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = (resources.getDimension(R.dimen.tabbar_layout_height) + resources.getDimension(R.dimen.minified_player_height)).toInt()
            }.also { adLayout.requestLayout() }
        }
    }

    fun isMyLibraryTheTopMostFragment(): Boolean {
        return supportFragmentManager.fragments.lastOrNull() is MyLibraryFragment
    }

    private fun isCurrentlyPlaying(item: AMResultItem): Boolean {
        return QueueRepository.getInstance().isCurrentItemOrParent(item)
    }

    fun addSongs(songs: List<AMResultItem>, atTheEnd: Boolean, mixpanelSource: MixpanelSource) {
        val playerQueue = PlayerQueue.Collection(songs, 0, mixpanelSource, false)
        playerPlayback.addQueue(playerQueue, if (atTheEnd) null else CURRENT_INDEX)
    }

    fun showMopubNativeAd(mopubNativeAd: NativeAd, nativeAdsAdapterHelper: AdapterHelper) {
        playerViewModel.showAd(mopubNativeAd, nativeAdsAdapterHelper)
    }

    fun showMopub300x250Ad(mopubAdView: MoPubView) {
        playerViewModel.showAd(mopubAdView)
    }

    // Private crap

    private fun closeMusicInfo() {
        if (supportFragmentManager.backStackEntryCount > 0 && supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1).name == MusicInfoFragment::class.java.simpleName) {
            popFragment()
        }
    }

    private fun closeFragments() {
        minimizePlayer(false)
        closeTooltipFragment()
    }

    // Animations

    private fun minimizePlayer(animated: Boolean) {
        if (!isPlayerMaximized()) {
            Timber.tag(TAG).w("Player is already minimized, ignoring close event")
            return
        }

        miniPlayerContainer.animate().cancel()

        val showMiniPlayer = playerViewModel.currentItem != null

        val tabBarTranslateYStartValue = round(tabbarLayout.translationY)
        val tabBarTranslateYEndValue = 0
        val playerTranslateYStartValue = round(playerContainer.translationY)
        val playerTranslateYEndValue = playerContainer.height
        val miniTranslateYStartValue = round(miniPlayerContainer.translationY)
        val miniTranslateYEndValue = if (showMiniPlayer) 0f else miniTranslateYStartValue
        val adYStartValue = (adLayout.layoutParams as FrameLayout.LayoutParams).bottomMargin
        val adYEndValue = resources.getDimension(R.dimen.tabbar_layout_height).plus(
            if (showMiniPlayer) resources.getDimension(R.dimen.minified_player_height)
            else 0f
        )

        val upperLayoutParams = upperLayout.layoutParams as FrameLayout.LayoutParams
        upperLayoutParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.tabbar_layout_height) + resources.getDimension(R.dimen.minified_player_height).toInt()
        upperLayout.layoutParams = upperLayoutParams

        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                val playerMultiplier = min(1.toFloat(), (playerTranslateYStartValue / playerTranslateYEndValue.toFloat()) + interpolatedTime)
                val playerTranslateY = (playerTranslateYEndValue * playerMultiplier)
                val miniPlayerTranslateY = ((miniTranslateYStartValue - miniTranslateYEndValue) * (1.toFloat() - interpolatedTime))
                val adTranslateY = adYStartValue + (adYEndValue - adYStartValue) * interpolatedTime
                val tabbarTranslateY = ((tabBarTranslateYStartValue - tabBarTranslateYEndValue) * (1.toFloat() - interpolatedTime))

                tabbarLayout.translationY = tabbarTranslateY
                playerContainer.translationY = playerTranslateY
                if (showMiniPlayer) miniPlayerContainer.translationY = miniPlayerTranslateY
                (adLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = adTranslateY.toInt()
                }.also { adLayout.requestLayout() }
            }
        }.apply {
            setAnimationListener(object : AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    nowPlayingViewModel.isMaximized = false
                    nowPlayingViewModel.onMinimized()

                    playerViewModel.onMinimized()

                    homeViewModel.setMiniplayerTooltipLocation(
                        TooltipFragment.TooltipLocation(
                            TooltipCorner.TOPRIGHT,
                            Point(miniPlayerContainer.width / 2, rootLayout.height - tabbarLayout.height - miniPlayerContainer.height / 2)
                        )
                    )
                }

                override fun onAnimationStart(animation: Animation?) {
                    miniPlayerContainer.visibility = View.VISIBLE
                    tabbarLayout.visibility = View.VISIBLE
                }
            })
            duration = if (animated) PLAYER_ANIMATION_DURATION else 0
        }
        tabbarLayout.startAnimation(animation)
    }

    private fun maximizePlayer(data: MaximizePlayerData) {
        Timber.tag(TAG).i("maximizePlayer() called with: data = $data")

        if (data.scrollToTop) {
            nowPlayingViewModel.scrollToTop()
        }

        if (!isPlayerMaximized()) {
            val needToRefreshAd = data.item == null || isCurrentlyPlaying(data.item)
            animatePlayerMaximize(data.animated, data.openShare, needToRefreshAd)
        }

        val item = data.item ?: return

        playerViewModel.inOfflineScreen = data.inOfflineScreen
        var index = data.albumPlaylistIndex ?: 0
        if (item.isAlbumTrack && !item.isAlbumTrackDownloadedAsSingle && data.items.isNullOrEmpty()) {
            data.collection?.let {
                playerPlayback.setQueue(
                    PlayerQueue.Album(
                        it,
                        index,
                        data.mixpanelSource,
                        data.inOfflineScreen,
                        data.shuffle,
                        data.allowFrozenTracks
                    ), true
                )
            } ?: run {
                playerPlayback.setQueue(
                    PlayerQueue.Song(
                        item,
                        data.mixpanelSource,
                        data.inOfflineScreen,
                        data.allowFrozenTracks
                    ),
                    true
                )
            }
        } else if (item.isPlaylistTrack && data.loadFullPlaylist) {
            data.collection?.let {
                playerPlayback.setQueue(
                    PlayerQueue.Playlist(
                        it,
                        index,
                        data.mixpanelSource,
                        data.inOfflineScreen,
                        data.shuffle,
                        data.allowFrozenTracks
                    ), true
                )
            } ?: run {
                playerPlayback.setQueue(
                    PlayerQueue.Song(
                        item,
                        data.mixpanelSource,
                        data.inOfflineScreen,
                        data.allowFrozenTracks
                    ),
                    true
                )
            }
        } else if (data.items != null) {
            index = if (data.shuffle) 0 else max(0, data.items.indexOfFirst { it.itemId == item.itemId })
            playerPlayback.setQueue(
                PlayerQueue.Collection(
                    data.items,
                    index,
                    data.mixpanelSource,
                    data.inOfflineScreen,
                    data.shuffle,
                    data.nextPageData,
                    data.allowFrozenTracks
                ), true
            )
        } else {
            data.mixpanelSource?.let { item.mixpanelSource = it }
            playerPlayback.setQueue(
                PlayerQueue.Song(
                    item,
                    data.mixpanelSource,
                    data.inOfflineScreen,
                    data.allowFrozenTracks
                ),
                true
            )
        }

        if (data.openShare) {
            playerViewModel.shareEvent.postValue(item)
        }
    }

    private fun animatePlayerMaximize(
        animated: Boolean,
        openShare: Boolean,
        needToRefreshAd: Boolean
    ) {
        val miniPlayerHeight = resources.getDimensionPixelOffset(R.dimen.minified_player_height)
        val tabBarTranslateYEndValue = tabbarLayout.height
        val playerTranslateYStartValue = round(playerContainer.translationY)
        val playerTranslateYEndValue = 0
        val miniPlayerTranslateYStartValue = round(miniPlayerContainer.translationY)
        val miniPlayerTranslateYEndValue = miniPlayerHeight + tabbarLayout.height
        val tabBarTranslateYStartValue = round(miniPlayerContainer.translationY)
        val adYStartValue = (adLayout.layoutParams as FrameLayout.LayoutParams).bottomMargin.toDouble()
        val adYEndValue = 0

        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                val playerTranslateY =
                    (abs(playerTranslateYEndValue - playerTranslateYStartValue) * (1.0f - interpolatedTime))
                val miniPlayerTranslateY =
                    miniPlayerTranslateYStartValue + (abs(miniPlayerTranslateYEndValue - miniPlayerTranslateYStartValue) * interpolatedTime)
                val tabbarTranslateY =
                    tabBarTranslateYStartValue + (abs(tabBarTranslateYEndValue - tabBarTranslateYStartValue) * interpolatedTime)
                val adTranslateY = adYStartValue + (adYEndValue - adYStartValue) * interpolatedTime

                tabbarLayout.translationY = tabbarTranslateY
                playerContainer.translationY = playerTranslateY
                miniPlayerContainer.translationY = miniPlayerTranslateY
                (adLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = adTranslateY.toInt()
                }.also { adLayout.requestLayout() }
            }
        }.apply {
            setAnimationListener(object : AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    nowPlayingViewModel.isMaximized = true

                    miniPlayerContainer.visibility = View.GONE
                    tabbarLayout.visibility = View.GONE

                    homeViewModel.onPlayerMaximized()

                    if (needToRefreshAd) {
                        playerViewModel.refreshPlayerAd(true)
                    }

                    if (!openShare && !playerViewModel.showTooltip()) {
                        nowPlayingViewModel.showTooltip()
                    }
                }

                override fun onAnimationStart(animation: Animation?) {
                    miniPlayerContainer.visibility = View.VISIBLE
                    tabbarLayout.visibility = View.VISIBLE
                }
            })
            duration = if (animated) PLAYER_ANIMATION_DURATION else 0
        }
        tabbarLayout.startAnimation(animation)
    }

    private fun startMusicService(playWhenReady: Boolean) {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(MusicService.EXTRA_PLAY_WHEN_READY, playWhenReady)

        // Somehow this is being triggered while the app is in the background
        try {
            startService(intent)
        } catch (e: IllegalStateException) {
            Timber.tag(TAG).w(e, "Error starting music service")
        }
    }

    // Overrides

    override fun openOptionsFragment(optionsMenuFragment: Fragment) {
        try {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fullScreenContainer, optionsMenuFragment, "options")
                .addToBackStack("options")
                .commit()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    override fun openImageZoomFragment(imageZoomFragment: Fragment) {
        try {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fullScreenContainer, imageZoomFragment)
                .addToBackStack(imageZoomFragment.javaClass.simpleName)
                .commit()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    override fun popFragment(): Boolean {
        try {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
                return true
            }
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
        return false
    }

    override fun onBackPressed() {
        (supportFragmentManager.findFragmentByTag(TooltipFragment.FRAGMENT_TAG) as? TooltipFragment)?.let { tooltipFragment ->
            tooltipFragment.runAction()
            supportFragmentManager.popBackStackImmediate()
            return
        }
        val topFragment = if (supportFragmentManager.fragments.size > 0) supportFragmentManager.fragments.lastOrNull { it !is PlayerUploaderTagsFragment && it !is PlayerBottomFragment } else null
        if (isPlayerMaximized() && (supportFragmentManager.backStackEntryCount == 0 ||
                (topFragment != null && topFragment !is MusicInfoFragment &&
                    topFragment !is ImageZoomFragment && topFragment !is OptionsMenuFragment &&
                    topFragment !is SlideUpMenuMusicFragment && topFragment !is SlideUpMenuShareFragment &&
                    topFragment !is AddCommentFragment && topFragment !is QueueFragment))) {
            minimizePlayer(true)
            return
        }
        if (topFragment is PlaylistOnboardingFragment) {
            super.onBackPressed()
            openArtistsSelectionOnboarding()
            return
        }
        if (!popFragment()) {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_CREDENTIALS_RESOLUTION && resultCode == Activity.RESULT_OK && data != null) {
            val credential = data.getParcelableExtra(Credential.EXTRA_KEY) as? Credential ?: return
            homeViewModel.loginWithSmartLockCredentials(credential)
        }
    }

    override fun onConnected(bundle: Bundle?) {
        super.onConnected(bundle)
        homeViewModel.onSmartLockReady(
            supportFragmentManager.findFragmentByTag(ArtistsOnboardingFragment.TAG) != null)
    }

    private fun showAudioAdFragment() {
        val fragment = supportFragmentManager.findFragmentByTag(AudioAdFragment.TAG)
        if (fragment != null) return

        adOverlayContainer.alpha = 1F
        supportFragmentManager.commit {
            replace(
                R.id.adOverlayContainer,
                AudioAdFragment.newInstance(),
                AudioAdFragment.TAG
            )
        }
    }

    private fun removeAudioAdFragment() {
        supportFragmentManager.findFragmentByTag(AudioAdFragment.TAG)?.let { fragment ->
            supportFragmentManager.commit {
                remove(fragment)
            }
            adOverlayContainer.alpha = 0F
        }
    }

    private fun showInterstitialLoader() {
        try {
            animationDialog = ProgressLogoDialog(this).apply { show() }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun hideInterstitialLoader() {
        try {
            animationDialog?.dismiss()
        } catch (e: Exception) {
            Timber.w(e)
        } finally {
            animationDialog = null
        }
    }

    private fun onSleepTimerTriggered() {
        AMSnackbar.Builder(this)
            .withTitle(getString(R.string.sleep_timer_triggered))
            .withDrawable(R.drawable.ic_snackbar_timer)
            .withDuration(Snackbar.LENGTH_LONG)
            .show()
    }

    private fun showQueueFragment() {
        supportFragmentManager.commit {
            add(R.id.fullScreenContainer, QueueFragment.newInstance())
            addToBackStack(QueueFragment.TAG)
        }
    }

    companion object {
        private const val TAG = "HomeActivity"
        private const val PLAYER_ANIMATION_DURATION = 300L
        private const val REQ_CODE_CREDENTIALS_RESOLUTION = 202
        const val REQ_CODE_INSTAGRAM_SHARE = 203
        const val EXTRA_OFFLINE = "com.audiomack.intent.extra.EXTRA_OFFLINE"
        const val EXTRA_LOGIN_FAVORITE = "com.audiomack.intent.extra.LOGIN_FAVORITE"
        const val EXTRA_LOGIN_REPOST = "com.audiomack.intent.extra.LOGIN_REPOST"
        const val ACTION_LOGIN_REQUIRED = "com.audiomack.intent.action.LOGIN_REQUIRED"
        const val ACTION_NOTIFY_OFFLINE = "com.audiomack.intent.action.NOTIFY_OFFLINE"

        @JvmStatic
        var instance: HomeActivity? = null
    }
}
