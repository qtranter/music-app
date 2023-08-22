package com.audiomack.ui.musicinfo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMResultItem
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
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class MusicInfoViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var imageLoader: ImageLoader

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    @Mock
    private lateinit var actionsDataSource: ActionsDataSource

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var eventBus: EventBus

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: MusicInfoViewModel

    private val mixpanelSource = MixpanelSource("", "")

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        loginStateChangeSubject = BehaviorSubject.create()
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)
        viewModel = MusicInfoViewModel(
            imageLoader,
            musicDataSource,
            actionsDataSource,
            userDataSource,
            schedulersProvider,
            eventBus
        ).also {
            it.initItem(mock())
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
    fun openUploader() {
        val observer: Observer<String> = mock()
        viewModel.uploaderEvent.observeForever(observer)
        viewModel.onUploaderTapped()
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun openArtist() {
        val observer: Observer<String> = mock()
        viewModel.artistEvent.observeForever(observer)
        viewModel.onArtistNameTapped()
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun openFeatArtist() {
        val observer: Observer<String> = mock()
        viewModel.artistEvent.observeForever(observer)
        viewModel.onFeatNameTapped("Cardi B")
        verify(observer).onChanged("Cardi B")
    }

    @Test
    fun followStatus() {
        val observer: Observer<Boolean> = mock()
        viewModel.followStatus.observeForever(observer)
        viewModel.updateFollowButton()
        verify(observer).onChanged(any())
    }

    @Test
    fun `toggle follow`() {
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
    fun openPlaylistCreator() {
        val observer: Observer<String> = mock()
        viewModel.artistEvent.observeForever(observer)
        viewModel.onPlaylistCreatorTapped()
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun `report content`() {
        val contentId = "123"
        val model = ReportContentModel(contentId, ReportContentType.Song, ReportType.Report, null)
        val item = mock<AMResultItem> {
            on { itemId } doReturn contentId
            on { type } doReturn "song"
        }
        viewModel.initItem(item)
        val observerShowReportReason: Observer<ReportContentModel> = mock()
        viewModel.showReportReasonEvent.observeForever(observerShowReportReason)
        viewModel.onReportTapped()
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
}
