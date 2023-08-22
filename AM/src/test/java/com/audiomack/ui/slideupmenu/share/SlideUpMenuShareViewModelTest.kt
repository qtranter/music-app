package com.audiomack.ui.slideupmenu.share

import android.app.Activity
import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ToggleHighlightException
import com.audiomack.data.actions.ToggleHighlightResult
import com.audiomack.data.device.DeviceDataSource
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.share.ShareManager
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelButtonMusicInfo
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventHighlightsUpdated
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.ProgressHUDMode
import com.audiomack.model.ShareMethod
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyBoolean
import org.mockito.MockitoAnnotations

class SlideUpMenuShareViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    // Dependencies

    @Mock
    private lateinit var shareManager: ShareManager

    @Mock
    private lateinit var music: AMResultItem

    @Mock
    private lateinit var artist: AMArtist

    @Mock
    private lateinit var trackingDataSource: TrackingDataSource

    @Mock
    private lateinit var imageLoader: ImageLoader

    @Mock
    private lateinit var deviceDataSource: DeviceDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    @Mock
    private lateinit var actionsDataSource: ActionsDataSource

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var eventBus: EventBus

    private lateinit var mixpanelSource: MixpanelSource

    private lateinit var mixpanelButton: String

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>

    @Mock
    private lateinit var remoteVariablesProvider: RemoteVariablesProvider

    // SUT

    private lateinit var viewModelMusic: SlideUpMenuShareViewModel

    private lateinit var viewModelArtist: SlideUpMenuShareViewModel

    // Observers

    @Mock
    private lateinit var observerHighlighted: Observer<Boolean>

    @Mock
    private lateinit var observerLoginRequired: Observer<LoginSignupSource>

    @Mock
    private lateinit var observerNotifyOffline: Observer<Void>

    @Mock
    private lateinit var observerShowHUD: Observer<ProgressHUDMode>

    @Mock
    private lateinit var observerReachedHighlightsLimit: Observer<Void>

    @Mock
    private lateinit var observerHighlightError: Observer<Void>

    @Mock
    private lateinit var observerHighlightSuccessEvent: Observer<String>

    @Mock
    private lateinit var observerShareMenuListMode: Observer<Boolean>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        mixpanelSource = MixpanelSource("", "")
        mixpanelButton = MixpanelButtonMusicInfo
        loginStateChangeSubject = BehaviorSubject.create()
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)
        viewModelArtist = SlideUpMenuShareViewModel(
            null,
            artist,
            mixpanelSource,
            mixpanelButton,
            shareManager,
            imageLoader,
            trackingDataSource,
            deviceDataSource,
            schedulersProvider,
            actionsDataSource,
            userDataSource,
            eventBus
        )
        viewModelMusic = SlideUpMenuShareViewModel(
            music,
            null,
            mixpanelSource,
            mixpanelButton,
            shareManager,
            imageLoader,
            trackingDataSource,
            deviceDataSource,
            schedulersProvider,
            actionsDataSource,
            userDataSource,
            eventBus,
            remoteVariablesProvider
        ).apply {
            highlighted.observeForever(observerHighlighted)
            loginRequiredEvent.observeForever(observerLoginRequired)
            notifyOfflineEvent.observeForever(observerNotifyOffline)
            showHUDEvent.observeForever(observerShowHUD)
            reachedHighlightsLimitEvent.observeForever(observerReachedHighlightsLimit)
            highlightErrorEvent.observeForever(observerHighlightError)
            highlightSuccessEvent.observeForever(observerHighlightSuccessEvent)
            shareMenuListMode.observeForever(observerShareMenuListMode)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun cancel() {
        val observer: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observer)
        viewModelMusic.onCancelTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun tapBackground() {
        val observer: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observer)
        viewModelMusic.onBackgroundTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun init() {
        verify(observerHighlighted, times(1)).onChanged(anyBoolean())
        verify(trackingDataSource, times(2)).trackEvent(eq("share"), anyOrNull(), any())
        verify(trackingDataSource, times(2)).trackScreen(any())
        verify(observerShareMenuListMode, times(1)).onChanged(anyBoolean())
    }

    @Test
    fun visible() {
        val observerStartAnimation: Observer<Void> = mock()
        viewModelMusic.startAnimationEvent.observeForever(observerStartAnimation)
        viewModelMusic.onVisible()
        verify(observerStartAnimation).onChanged(null)
    }

    @Test
    fun copyLinkMusic() {
        val activity: Activity = mock()
        val observerCloseEvent: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observerCloseEvent)
        viewModelMusic.onCopyLinkTapped(activity)
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).copyMusicLink(anyOrNull(), eq(music), eq(mixpanelSource), eq(mixpanelButton))
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Copy Link") })
    }

    @Test
    fun copyLinkArtist() {
        val activity: Activity = mock()
        val observerCloseEvent: Observer<Void> = mock()
        viewModelArtist.closeEvent.observeForever(observerCloseEvent)
        viewModelArtist.onCopyLinkTapped(activity)
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).copyArtistink(anyOrNull(), eq(artist), eq(mixpanelSource), eq(mixpanelButton))
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Copy Link") })
    }

    @Test
    fun twitterMusic() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelMusic.onShareViaTwitterTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareMusic(
            eq(activity),
            eq(music),
            eq(ShareMethod.Twitter),
            eq(mixpanelSource),
            eq(mixpanelButton),
            any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Twitter") })
    }

    @Test
    fun twitterArtist() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelArtist.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelArtist.onShareViaTwitterTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareArtist(eq(activity), eq(artist), eq(ShareMethod.Twitter), eq(mixpanelSource), eq(mixpanelButton))
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Twitter") })
    }

    // Highlight

    @Test
    fun `add highlight, success`() {
        val title = "My song"
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.just(
            ToggleHighlightResult.Added(title)))

        viewModelMusic.onHighlightTapped(mock())

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(observerHighlighted).onChanged(true)
        verify(eventBus).post(argWhere { it is EventHighlightsUpdated })
        verify(observerHighlightSuccessEvent).onChanged(title)
    }

    @Test
    fun `remove highlight, success`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.just(
            ToggleHighlightResult.Removed))

        viewModelMusic.onHighlightTapped(mock())

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(observerHighlighted, atLeast(1)).onChanged(false)
        verify(eventBus).post(argWhere { it is EventHighlightsUpdated })
        com.nhaarman.mockitokotlin2.verifyZeroInteractions(observerHighlightSuccessEvent)
    }

    @Test
    fun `toggle highlight, offline`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(
            ToggleHighlightException.Offline))

        viewModelMusic.onHighlightTapped(mock())

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(observerNotifyOffline).onChanged(null)
    }

    @Test
    fun `toggle highlight, logged out`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(
            ToggleHighlightException.LoggedOut))

        viewModelMusic.onHighlightTapped(mock())

        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(observerLoginRequired).onChanged(LoginSignupSource.Highlight)

        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)

        verify(actionsDataSource, times(2)).toggleHighlight(any(), any(), any())
    }

    @Test
    fun `add highlight, reached limit`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(
            ToggleHighlightException.ReachedLimit))

        viewModelMusic.onHighlightTapped(mock())

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(observerReachedHighlightsLimit).onChanged(null)
    }

    @Test
    fun `add highlight, error`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(
            ToggleHighlightException.Failure(true)))

        viewModelMusic.onHighlightTapped(mock())

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(observerHighlightError).onChanged(null)
    }

    @Test
    fun `remove highlight, error`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(
            ToggleHighlightException.Failure(false)))

        viewModelMusic.onHighlightTapped(mock())

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(observerHighlightError, never()).onChanged(null)
    }

    @Test
    fun `toggle highlight, generic error`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(Exception("")))

        viewModelMusic.onHighlightTapped(mock())

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(observerShowHUD, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
    }

    @Test
    fun facebookMusic() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelMusic.onShareViaFacebookTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareStory(
                eq(activity),
                eq(music),
                anyOrNull(),
                eq(ShareMethod.Facebook),
                eq(mixpanelSource),
                eq(mixpanelButton),
                any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Facebook") })
    }

    @Test
    fun facebookArtist() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelArtist.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelArtist.onShareViaFacebookTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareStory(
                eq(activity),
                anyOrNull(),
                eq(artist),
                eq(ShareMethod.Facebook),
                eq(mixpanelSource),
                eq(mixpanelButton),
                any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Facebook") })
    }

    @Test
    fun snapchatMusic() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelMusic.onShareViaSnapchatTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareStory(
                eq(activity),
                eq(music),
                anyOrNull(),
                eq(ShareMethod.Snapchat),
                eq(mixpanelSource),
                eq(mixpanelButton),
                any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Snapchat") })
    }

    @Test
    fun snapchatArtist() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelArtist.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelArtist.onShareViaSnapchatTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareStory(
                eq(activity),
                anyOrNull(),
                eq(artist),
                eq(ShareMethod.Snapchat),
                eq(mixpanelSource),
                eq(mixpanelButton),
                any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Snapchat") })
    }

    @Test
    fun instagramMusic() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelMusic.onShareViaInstagramTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareStory(
                eq(activity),
                eq(music),
                anyOrNull(),
                eq(ShareMethod.Instagram),
                eq(mixpanelSource),
                eq(mixpanelButton),
                any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Instagram") })
    }

    @Test
    fun instagramArtist() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelArtist.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelArtist.onShareViaInstagramTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareStory(
                eq(activity),
                anyOrNull(),
                eq(artist),
                eq(ShareMethod.Instagram),
                eq(mixpanelSource),
                eq(mixpanelButton),
                any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Instagram") })
    }

    @Test
    fun textMusic() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelMusic.onShareViaContactsTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareMusic(
            eq(activity),
            eq(music),
            eq(ShareMethod.SMS),
            eq(mixpanelSource),
            eq(mixpanelButton),
            any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Text") })
    }

    @Test
    fun textArtist() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelArtist.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelArtist.onShareViaContactsTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareArtist(eq(activity), eq(artist), eq(ShareMethod.SMS), eq(mixpanelSource), eq(mixpanelButton))
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Text") })
    }

    @Test
    fun moreMusic() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelMusic.onShareViaOtherTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareMusic(
            eq(activity),
            eq(music),
            eq(ShareMethod.Standard),
            eq(mixpanelSource),
            eq(mixpanelButton),
            any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("App") })
    }

    @Test
    fun moreArtist() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelArtist.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelArtist.onShareViaOtherTapped(activity, mock())
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareArtist(eq(activity), eq(artist), eq(ShareMethod.Standard), eq(mixpanelSource), eq(mixpanelButton))
        verify(trackingDataSource).trackScreen(argWhere { it.contains("App") })
    }

    @Test
    fun screenshotMusic() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelMusic.onShareScreenshotTapped(activity)
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareScreenshot(eq(activity), eq(music), anyOrNull(), eq(ShareMethod.Screenshot), anyOrNull(), eq(mixpanelSource), eq(mixpanelButton))
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Screenshot") })
    }

    @Test
    fun screenshotArtist() {
        val observerCloseEvent: Observer<Void> = mock()
        viewModelArtist.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelArtist.onShareScreenshotTapped(activity)
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareScreenshot(eq(activity), anyOrNull(), eq(artist), eq(ShareMethod.Screenshot), anyOrNull(), eq(mixpanelSource), eq(mixpanelButton))
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Screenshot") })
    }

    @Test
    fun onLoadAndBlur() {
        whenever(remoteVariablesProvider.slideUpMenuShareMode).thenReturn(RemoteVariablesProvider.FIREBASE_SLIDE_UP_MENU_SHARE_MODE_GRID)
        whenever(imageLoader.load(any(), any())).thenReturn(Single.just(mock()))
        val observer: Observer<Bitmap> = mock()
        viewModelMusic.loadBitmapEvent.observeForever(observer)
        viewModelMusic.onLoadAndBlur(mock())
        verify(observer).onChanged(any())
    }

    @Test
    fun whatsappMusic() {
        val method = ShareMethod.WhatsApp
        val observerCloseEvent: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelMusic.onShareWhatsAppTapped(activity)
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareLink(
            eq(activity),
            eq(music),
            anyOrNull(),
            eq(method),
            eq(mixpanelSource),
            eq(mixpanelButton)
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains(method.stringValue()) })
    }

    @Test
    fun whatsappArtist() {
        val method = ShareMethod.WhatsApp
        val observerCloseEvent: Observer<Void> = mock()
        viewModelArtist.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelArtist.onShareWhatsAppTapped(activity)
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareLink(
            eq(activity),
            anyOrNull(),
            eq(artist),
            eq(method),
            eq(mixpanelSource),
            eq(mixpanelButton)
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains(method.stringValue()) })
    }

    @Test
    fun messengerMusic() {
        val method = ShareMethod.Messenger
        val observerCloseEvent: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelMusic.onShareMessengerTapped(activity)
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareLink(
            eq(activity),
            eq(music),
            anyOrNull(),
            eq(method),
            eq(mixpanelSource),
            eq(mixpanelButton)
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains(method.stringValue()) })
    }

    @Test
    fun messengerArtist() {
        val method = ShareMethod.Messenger
        val observerCloseEvent: Observer<Void> = mock()
        viewModelArtist.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelArtist.onShareMessengerTapped(activity)
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareLink(
            eq(activity),
            anyOrNull(),
            eq(artist),
            eq(method),
            eq(mixpanelSource),
            eq(mixpanelButton)
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains(method.stringValue()) })
    }

    @Test
    fun wechatMusic() {
        val method = ShareMethod.WeChat
        val observerCloseEvent: Observer<Void> = mock()
        viewModelMusic.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelMusic.onShareWeChatTapped(activity)
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareLink(
            eq(activity),
            eq(music),
            anyOrNull(),
            eq(method),
            eq(mixpanelSource),
            eq(mixpanelButton)
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains(method.stringValue()) })
    }

    @Test
    fun wechatArtist() {
        val method = ShareMethod.WeChat
        val observerCloseEvent: Observer<Void> = mock()
        viewModelArtist.closeEvent.observeForever(observerCloseEvent)
        val activity: Activity = mock()
        viewModelArtist.onShareWeChatTapped(activity)
        verify(observerCloseEvent).onChanged(null)
        verify(shareManager).shareLink(
            eq(activity),
            anyOrNull(),
            eq(artist),
            eq(method),
            eq(mixpanelSource),
            eq(mixpanelButton)
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains(method.stringValue()) })
    }

    @Test
    fun updateMenuShareModeList() {
        whenever(remoteVariablesProvider.slideUpMenuShareMode).thenReturn("list")
        viewModelMusic.updateMenuShareMode()
        verify(observerShareMenuListMode, times(1)).onChanged(true)
    }

    @Test
    fun updateMenuShareModeGrid() {
        whenever(remoteVariablesProvider.slideUpMenuShareMode).thenReturn("grid")
        viewModelMusic.updateMenuShareMode()
        verify(observerShareMenuListMode, times(2)).onChanged(false)
    }
}
