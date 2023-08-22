package com.audiomack.ui.artistinfo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.EventContentReported
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.ReportContentModel
import com.audiomack.model.ReportContentType
import com.audiomack.model.ReportType
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.atMost
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class ArtistInfoViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var imageLoader: ImageLoader

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var actionsDataSource: ActionsDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    @Mock
    private lateinit var eventBus: EventBus

    private lateinit var viewModel: ArtistInfoViewModel

    private val mixpanelSource = MixpanelSource("", "")

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        loginStateChangeSubject = BehaviorSubject.create()
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)
        viewModel = ArtistInfoViewModel(
            imageLoader,
            userDataSource,
            actionsDataSource,
            schedulersProvider,
            eventBus
        ).also {
            it.initArtist(mock())
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `on init subscribe EventBus`() {
        verify(eventBus, times(1)).register(any())
    }

    @Test
    fun `on cleared, unsubscribe from EventBus`() {
        viewModel.onCleared()
        verify(eventBus, times(1)).unregister(any())
    }

    @Test
    fun close() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun image() {
        val observer: Observer<Void> = mock()
        viewModel.imageEvent.observeForever(observer)
        viewModel.onImageTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun openTwitter() {
        val observer: Observer<String> = mock()
        viewModel.openUrlEvent.observeForever(observer)
        viewModel.onTwitterTapped()
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun openFacebook() {
        val observer: Observer<String> = mock()
        viewModel.openUrlEvent.observeForever(observer)
        viewModel.onFacebookTapped()
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun openInstagram() {
        val observer: Observer<String> = mock()
        viewModel.openUrlEvent.observeForever(observer)
        viewModel.onInstagramTapped()
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun openYoutube() {
        val observer: Observer<String> = mock()
        viewModel.openUrlEvent.observeForever(observer)
        viewModel.onYoutubeTapped()
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun openWebsite() {
        val observer: Observer<String> = mock()
        viewModel.openUrlEvent.observeForever(observer)
        viewModel.onWebsiteTapped()
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun followStatus() {
        val observer: Observer<Boolean> = mock()
        viewModel.followStatus.observeForever(observer)
        verify(observer).onChanged(any())
    }

    @Test
    fun share() {
        val observer: Observer<Void> = mock()
        viewModel.shareEvent.observeForever(observer)
        viewModel.onShareTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `toggle follow, logged in`() {
        val result = true
        val observerFollowStatus: Observer<Boolean> = mock()
        Mockito.`when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Observable.just(ToggleFollowResult.Finished(true)))
        viewModel.followStatus.observeForever(observerFollowStatus)
        viewModel.onFollowTapped(mixpanelSource)
        verify(observerFollowStatus, atLeast(1)).onChanged(result)
    }

    @Test
    fun `toggle follow, ask for permissions`() {
        val redirect = PermissionRedirect.Settings
        Mockito.`when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.just(ToggleFollowResult.AskForPermission(redirect)))
        val promptNotificationPermissionEventObserver: Observer<PermissionRedirect> = mock()
        viewModel.promptNotificationPermissionEvent.observeForever(promptNotificationPermissionEventObserver)
        viewModel.onFollowTapped(mixpanelSource)
        verify(promptNotificationPermissionEventObserver).onChanged(redirect)
    }

    @Test
    fun `toggle follow, offline`() {
        Mockito.`when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Observable.error(ToggleFollowException.Offline))
        val observerFollowStatus: Observer<Boolean> = mock()
        val observerOfflineAlert: Observer<Void> = mock()
        viewModel.followStatus.observeForever(observerFollowStatus)
        viewModel.offlineAlert.observeForever(observerOfflineAlert)
        viewModel.onFollowTapped(mixpanelSource)
        verify(observerFollowStatus, times(1)).onChanged(ArgumentMatchers.anyBoolean())
        verify(observerOfflineAlert).onChanged(null)
    }

    @Test
    fun `toggle follow, logged out`() {
        Mockito.`when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Observable.error(ToggleFollowException.LoggedOut))
        val observerFollowStatus: Observer<Boolean> = mock()
        val observerLoggedOutAlert: Observer<LoginSignupSource> = mock()
        viewModel.followStatus.observeForever(observerFollowStatus)
        viewModel.loggedOutAlert.observeForever(observerLoggedOutAlert)
        viewModel.onFollowTapped(mixpanelSource)
        verify(observerFollowStatus, atMost(1)).onChanged(Mockito.anyBoolean())
        verify(observerLoggedOutAlert).onChanged(eq(LoginSignupSource.AccountFollow))

        val result = true
        Mockito.`when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Observable.just(ToggleFollowResult.Finished(true)))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)
        verify(observerFollowStatus, atLeast(1)).onChanged(result)
    }

    @Test
    fun `report content`() {
        val contentId = "123"
        val reportType = ReportType.Report
        val model = ReportContentModel(contentId, ReportContentType.Artist, reportType, null)
        val item = mock<AMArtist> {
            on { artistId } doReturn contentId
        }
        viewModel.initArtist(item)
        val observerShowReportReason: Observer<ReportContentModel> = mock()
        viewModel.showReportReasonEvent.observeForever(observerShowReportReason)
        viewModel.onReportTapped()
        verify(observerShowReportReason).onChanged(eq(model))
    }

    @Test
    fun `block content`() {
        val contentId = "123"
        val reportType = ReportType.Block
        val model = ReportContentModel(contentId, ReportContentType.Artist, reportType, null)
        val item = mock<AMArtist> {
            on { artistId } doReturn contentId
        }
        viewModel.initArtist(item)
        val observerShowReportReason: Observer<ReportContentModel> = mock()
        viewModel.showReportReasonEvent.observeForever(observerShowReportReason)
        viewModel.onBlockTapped()
        verify(observerShowReportReason).onChanged(eq(model))
    }

    @Test
    fun `show report alert through EventBus`() {
        val reportType = ReportType.Report
        viewModel.onMessageEvent(EventContentReported(reportType))
        val observerShowReportAlert: Observer<ReportType> = mock()
        viewModel.showReportAlertEvent.observeForever(observerShowReportAlert)
        verify(observerShowReportAlert).onChanged(reportType)
    }

    @Test
    fun `show block alert through EventBus`() {
        val reportType = ReportType.Block
        viewModel.onMessageEvent(EventContentReported(reportType))
        val observerShowReportAlert: Observer<ReportType> = mock()
        viewModel.showReportAlertEvent.observeForever(observerShowReportAlert)
        verify(observerShowReportAlert).onChanged(reportType)
    }
}
