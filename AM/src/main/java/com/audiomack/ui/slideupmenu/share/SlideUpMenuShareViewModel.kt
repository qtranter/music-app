package com.audiomack.ui.slideupmenu.share

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.ToggleHighlightException
import com.audiomack.data.actions.ToggleHighlightResult
import com.audiomack.data.device.DeviceDataSource
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.share.ShareManager
import com.audiomack.data.share.ShareManagerImpl
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingProvider
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventHighlightsUpdated
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.ProgressHUDMode
import com.audiomack.model.ShareMethod
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import org.greenrobot.eventbus.EventBus

class SlideUpMenuShareViewModel(
    val music: AMResultItem?,
    val artist: AMArtist?,
    val mixpanelSource: MixpanelSource,
    private val mixpanelButton: String,
    private val shareManager: ShareManager = ShareManagerImpl(),
    val imageLoader: ImageLoader = PicassoImageLoader,
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    val deviceDataSource: DeviceDataSource = DeviceRepository,
    val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    val actionsDataSource: ActionsDataSource = ActionsRepository(),
    val userDataSource: UserDataSource = UserRepository.getInstance(),
    val eventBus: EventBus = EventBus.getDefault(),
    private val remoteVariables: RemoteVariablesProvider = RemoteVariablesProviderImpl()
) : BaseViewModel() {

    private var _highlighted = MutableLiveData<Boolean>()
    val highlighted: LiveData<Boolean> get() = _highlighted

    val closeEvent = SingleLiveEvent<Void>()
    val startAnimationEvent = SingleLiveEvent<Void>()
    val loadBitmapEvent = SingleLiveEvent<Bitmap>()
    val showHUDEvent = SingleLiveEvent<ProgressHUDMode>()
    val notifyOfflineEvent = SingleLiveEvent<Void>()
    val loginRequiredEvent = SingleLiveEvent<LoginSignupSource>()
    val reachedHighlightsLimitEvent = SingleLiveEvent<Void>()
    val highlightErrorEvent = SingleLiveEvent<Void>()
    val highlightSuccessEvent = SingleLiveEvent<String>()
    val shareMenuListMode = SingleLiveEvent<Boolean>()

    val imageUrl: String = artist?.smallImage
        ?: music?.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall) ?: ""

    private val typeForAnalytics = artist?.let {
        "Artist"
    } ?: music?.let {
        when {
            it.isAlbum -> "Album"
            it.isPlaylist -> "Playlist"
            else -> "Song"
        }
    } ?: ""

    private var pendingActionAfterLogin: PendingActionAfterLogin? = null

    init {
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        music?.let {
            _highlighted.postValue(userDataSource.isMusicHighlighted(it))
        }
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Share Button")
        trackingDataSource.trackEvent("share", null, listOf(TrackingProvider.Firebase))
        updateMenuShareMode()
    }

    @VisibleForTesting
    fun updateMenuShareMode() {
        shareMenuListMode.postValue(remoteVariables.slideUpMenuShareMode == RemoteVariablesProvider.FIREBASE_SLIDE_UP_MENU_SHARE_MODE_LIST)
    }

    fun onCancelTapped() {
        closeEvent.call()
    }

    fun onBackgroundTapped() {
        closeEvent.call()
    }

    fun onCopyLinkTapped(activity: Activity?) {
        music?.let {
            shareManager.copyMusicLink(activity, it, mixpanelSource, mixpanelButton)
        }
        artist?.let {
            shareManager.copyArtistink(activity, it, mixpanelSource, mixpanelButton)
        }
        closeEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Copy Link")
    }

    fun onShareViaTwitterTapped(activity: Activity?, disposable: CompositeDisposable) {
        music?.let {
            shareManager.shareMusic(
                activity,
                it,
                ShareMethod.Twitter,
                mixpanelSource,
                mixpanelButton,
                disposable
            )
        }
        artist?.let {
            shareManager.shareArtist(
                activity,
                it,
                ShareMethod.Twitter,
                mixpanelSource,
                mixpanelButton
            )
        }
        closeEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Twitter")
    }

    fun onShareViaFacebookTapped(activity: Activity?, disposables: CompositeDisposable) {
        shareManager.shareStory(
            activity,
            music,
            artist,
            ShareMethod.Facebook,
            mixpanelSource,
            mixpanelButton,
            disposables
        )
        closeEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Facebook")
    }

    fun onShareViaInstagramTapped(activity: Activity?, disposables: CompositeDisposable) {
        shareManager.shareStory(
            activity,
            music,
            artist,
            ShareMethod.Instagram,
            mixpanelSource,
            mixpanelButton,
            disposables
        )
        closeEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Instagram")
    }

    fun onShareViaSnapchatTapped(activity: Activity?, disposables: CompositeDisposable) {
        shareManager.shareStory(
            activity,
            music,
            artist,
            ShareMethod.Snapchat,
            mixpanelSource,
            mixpanelButton,
            disposables
        )
        closeEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Snapchat")
    }

    fun onShareViaContactsTapped(activity: Activity?, disposable: CompositeDisposable) {
        music?.let {
            shareManager.shareMusic(
                activity,
                it,
                ShareMethod.SMS,
                mixpanelSource,
                mixpanelButton,
                disposable
            )
        }
        artist?.let {
            shareManager.shareArtist(activity, it, ShareMethod.SMS, mixpanelSource, mixpanelButton)
        }
        closeEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Text")
    }

    fun onShareViaOtherTapped(activity: Activity?, disposable: CompositeDisposable) {
        music?.let {
            shareManager.shareMusic(
                activity,
                it,
                ShareMethod.Standard,
                mixpanelSource,
                mixpanelButton,
                disposable
            )
        }
        artist?.let {
            shareManager.shareArtist(
                activity,
                it,
                ShareMethod.Standard,
                mixpanelSource,
                mixpanelButton
            )
        }
        closeEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, App")
    }

    fun onShareScreenshotTapped(activity: Activity?) {
        shareManager.shareScreenshot(
            activity,
            music,
            artist,
            ShareMethod.Screenshot,
            null,
            mixpanelSource,
            mixpanelButton
        )
        closeEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, Screenshot")
    }

    fun onShareWhatsAppTapped(activity: Activity?) {
        val method = ShareMethod.WhatsApp
        shareManager.shareLink(
            activity,
            music,
            artist,
            method,
            mixpanelSource,
            mixpanelButton
        )
        closeEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, ${method.stringValue()}")
    }

    fun onShareMessengerTapped(activity: Activity?) {
        val method = ShareMethod.Messenger
        shareManager.shareLink(
            activity,
            music,
            artist,
            method,
            mixpanelSource,
            mixpanelButton
        )
        closeEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, ${method.stringValue()}")
    }

    fun onShareWeChatTapped(activity: Activity?) {
        val method = ShareMethod.WeChat
        shareManager.shareLink(
            activity,
            music,
            artist,
            method,
            mixpanelSource,
            mixpanelButton
        )
        closeEvent.call()
        trackingDataSource.trackScreen("Share Menu, $typeForAnalytics, ${method.stringValue()}")
    }

    fun onHighlightTapped(music: AMResultItem) {
        showHUDEvent.postValue(ProgressHUDMode.Loading)
        compositeDisposable.add(
            actionsDataSource.toggleHighlight(music, mixpanelButton, mixpanelSource)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ result ->
                    showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    eventBus.post(EventHighlightsUpdated())
                    _highlighted.postValue(result is ToggleHighlightResult.Added)
                    if (result is ToggleHighlightResult.Added) {
                        highlightSuccessEvent.postValue(result.title)
                    }
                }, { throwable ->
                    showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    when (throwable) {
                        ToggleHighlightException.Offline -> notifyOfflineEvent.call()
                        ToggleHighlightException.LoggedOut -> {
                            pendingActionAfterLogin = PendingActionAfterLogin.Highlight(music)
                            loginRequiredEvent.postValue(LoginSignupSource.Highlight)
                        }
                        ToggleHighlightException.ReachedLimit -> reachedHighlightsLimitEvent.call()
                        is ToggleHighlightException.Failure -> {
                            if (throwable.highliting) {
                                highlightErrorEvent.call()
                            }
                        }
                        else -> showHUDEvent.postValue(ProgressHUDMode.Failure(""))
                    }
                })
        )
    }

    fun onVisible() {
        startAnimationEvent.call()
    }

    fun onLoadAndBlur(context: Context?) {
        if (remoteVariables.slideUpMenuShareMode == RemoteVariablesProvider.FIREBASE_SLIDE_UP_MENU_SHARE_MODE_GRID) {
            compositeDisposable.add(
                imageLoader.load(context, imageUrl)
                    .subscribeOn(schedulersProvider.main)
                    .observeOn(schedulersProvider.main)
                    .subscribe({ bitmap ->
                        loadBitmapEvent.postValue(bitmap)
                    }, {})
            )
        }
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        when (state) {
            EventLoginState.LOGGED_IN -> pendingActionAfterLogin?.let {
                when (it) {
                    is PendingActionAfterLogin.Highlight -> onHighlightTapped(it.music)
                }
                pendingActionAfterLogin = null
            }
            else -> pendingActionAfterLogin = null
        }
    }

    sealed class PendingActionAfterLogin {
        data class Highlight(val music: AMResultItem) : PendingActionAfterLogin()
    }
}
