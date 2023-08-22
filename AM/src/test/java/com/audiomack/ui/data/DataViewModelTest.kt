package com.audiomack.ui.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.actions.ToggleHighlightException
import com.audiomack.data.actions.ToggleHighlightResult
import com.audiomack.data.actions.ToggleRepostException
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMNotification
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventLoginState
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.model.SubscriptionNotification
import com.audiomack.model.SubscriptionStore
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.atMost
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class DataViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var actionsDataSource: ActionsDataSource

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    @Mock
    private lateinit var remoteVariablesProvider: RemoteVariablesProvider

    private lateinit var viewModel: DataViewModel

    // Observers

    @Mock
    private lateinit var followStatusObserver: Observer<Boolean>

    @Mock
    private lateinit var notifyFollowToastEventObserver: Observer<ToggleFollowResult.Notify>

    @Mock
    private lateinit var offlineAlertEventObserver: Observer<Void>

    @Mock
    private lateinit var loggedOutAlertEventObserver: Observer<LoginSignupSource>

    @Mock
    private lateinit var showHUDEventObserver: Observer<ProgressHUDMode>

    @Mock
    private lateinit var openURLObserver: Observer<String>

    @Mock
    private lateinit var showPremiumObserver: Observer<InAppPurchaseMode>

    @Mock
    private lateinit var removeHighlightAtPositionObserver: Observer<Int>

    @Mock
    private lateinit var observerShowConfirmPlaylistSyncEvent: Observer<Pair<AMResultItem, Int>>

    @Mock
    private lateinit var observerShowConfirmDownloadDeletionEvent: Observer<AMResultItem>

    @Mock
    private lateinit var observerShowConfirmPlaylistDownloadDeletionEvent: Observer<AMResultItem>

    @Mock
    private lateinit var observerShowFailedPlaylistDownloadEvent: Observer<Void>

    @Mock
    private lateinit var showPremiumDownloadObserver: Observer<PremiumDownloadModel>

    @Mock
    private lateinit var showUnlockedToastEventObserver: Observer<String>

    @Mock
    private lateinit var promptNotificationPermissionEventObserver: Observer<PermissionRedirect>

    @Mock
    private lateinit var showFollowBtnObserver: Observer<Boolean>

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>

    // Dummy inputs
    private val mixpanelSource = MixpanelSource.empty
    private val mixpanelButton = ""

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        loginStateChangeSubject = BehaviorSubject.create()
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)
        whenever(premiumDataSource.premiumObservable).thenReturn(mock())
        viewModel = DataViewModel(
            userDataSource,
            actionsDataSource,
            musicDataSource,
            premiumDataSource,
            mixpanelDataSource,
            schedulersProvider,
            remoteVariablesProvider
        ).apply {
            followStatus.observeForever(followStatusObserver)
            notifyFollowToastEvent.observeForever(notifyFollowToastEventObserver)
            offlineAlertEvent.observeForever(offlineAlertEventObserver)
            loggedOutAlertEvent.observeForever(loggedOutAlertEventObserver)
            showHUDEvent.observeForever(showHUDEventObserver)
            openURLEvent.observeForever(openURLObserver)
            showPremiumEvent.observeForever(showPremiumObserver)
            removeHighlightAtPositionEvent.observeForever(removeHighlightAtPositionObserver)
            showConfirmPlaylistSyncEvent.observeForever(observerShowConfirmPlaylistSyncEvent)
            showConfirmDownloadDeletionEvent.observeForever(observerShowConfirmDownloadDeletionEvent)
            showConfirmPlaylistDownloadDeletionEvent.observeForever(observerShowConfirmPlaylistDownloadDeletionEvent)
            showFailedPlaylistDownloadEvent.observeForever(observerShowFailedPlaylistDownloadEvent)
            showPremiumDownloadEvent.observeForever(showPremiumDownloadObserver)
            showUnlockedToastEvent.observeForever(showUnlockedToastEventObserver)
            promptNotificationPermissionEvent.observeForever(promptNotificationPermissionEventObserver)
            showFollowBtn.observeForever(showFollowBtnObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    // Tracking

    @Test
    fun `track bell notifications click`() {
        val type = AMNotification.NotificationType.Favorite
        viewModel.onBellNotificationClicked(type)
        verify(mixpanelDataSource).trackBellNotification(type.analyticsType)
    }

    // Upsell

    @Test
    fun `on upsell click - no billing issues`() {
        whenever(premiumDataSource.subscriptionNotification).thenReturn(SubscriptionNotification.None)
        viewModel.onUpsellClicked()
        verifyZeroInteractions(openURLObserver)
        verify(showPremiumObserver).onChanged(eq(InAppPurchaseMode.MyLibraryBar))
        verify(mixpanelDataSource, never()).trackBillingIssue()
    }

    @Test
    fun `on upsell click - billing issues - valid store url`() {
        whenever(premiumDataSource.subscriptionStore).thenReturn(SubscriptionStore.PlayStore)
        viewModel.onUpsellClicked()
        verify(openURLObserver).onChanged(SubscriptionStore.PlayStore.url)
        verifyZeroInteractions(showPremiumObserver)
        verify(mixpanelDataSource).trackBillingIssue()
    }

    @Test
    fun `on upsell click - billing issues - unknown store url`() {
        whenever(premiumDataSource.subscriptionStore).thenReturn(SubscriptionStore.None)
        viewModel.onUpsellClicked()
        verifyZeroInteractions(openURLObserver)
        verifyZeroInteractions(showPremiumObserver)
        verifyZeroInteractions(mixpanelDataSource)
    }

    // Follow

    @Test
    fun `toggle follow, logged in`() {
        val result = true
        Mockito.`when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Observable.just(ToggleFollowResult.Finished(true)))

        viewModel.onFollowTapped(null, mock(), mixpanelSource, mixpanelButton)

        verify(followStatusObserver, atLeast(1)).onChanged(result)
    }

    @Test
    fun `toggle follow, ask for permissions`() {
        val redirect = PermissionRedirect.Settings
        Mockito.`when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.just(ToggleFollowResult.AskForPermission(redirect)))

        viewModel.onFollowTapped(null, mock(), mixpanelSource, mixpanelButton)

        verify(promptNotificationPermissionEventObserver).onChanged(redirect)
    }

    @Test
    fun `toggle follow, offline`() {
        Mockito.`when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Observable.error(ToggleFollowException.Offline))

        viewModel.onFollowTapped(null, mock(), mixpanelSource, mixpanelButton)

        verify(followStatusObserver, never()).onChanged(ArgumentMatchers.anyBoolean())
        verify(offlineAlertEventObserver).onChanged(null)
    }

    @Test
    fun `toggle follow, logged out`() {
        Mockito.`when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Observable.error(ToggleFollowException.LoggedOut))

        viewModel.onFollowTapped(null, mock(), mixpanelSource, mixpanelButton)

        verify(followStatusObserver, atMost(1)).onChanged(Mockito.anyBoolean())
        verify(loggedOutAlertEventObserver).onChanged(eq(LoginSignupSource.AccountFollow))

        val result = true
        Mockito.`when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(Observable.just(ToggleFollowResult.Finished(true)))

        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)

        verify(followStatusObserver, atLeast(1)).onChanged(result)
    }

    // Remove georestricted music

    @Test
    fun `remove georestricted favorite music, successful`() {
        val music = mock<AMResultItem>()
        whenever(actionsDataSource.toggleFavorite(any(), any(), any())).thenReturn(Observable.just(ToggleFavoriteResult.Notify(true, false, false, false, true, "title", "artist")))

        viewModel.removeGeorestrictedItemFromFavorites(music, mixpanelSource, mixpanelButton)

        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDEventObserver, never()).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(offlineAlertEventObserver, never()).onChanged(null)
    }

    @Test
    fun `remove georestricted favorite music, failure with offline status`() {
        val music = mock<AMResultItem>()
        whenever(actionsDataSource.toggleFavorite(any(), any(), any())).thenReturn(Observable.error(ToggleFavoriteException.Offline))

        viewModel.removeGeorestrictedItemFromFavorites(music, mixpanelSource, mixpanelButton)

        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDEventObserver, never()).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(offlineAlertEventObserver, times(1)).onChanged(null)
    }

    @Test
    fun `remove georestricted favorite music, failure with generic error`() {
        val music = mock<AMResultItem>()
        whenever(actionsDataSource.toggleFavorite(any(), any(), any())).thenReturn(Observable.error(Exception("")))

        viewModel.removeGeorestrictedItemFromFavorites(music, mixpanelSource, mixpanelButton)

        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, never()).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDEventObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(offlineAlertEventObserver, never()).onChanged(null)
    }

    @Test
    fun `remove georestricted upload music, successful and also removed from highlights`() {
        val music = mock<AMResultItem>()
        whenever(musicDataSource.getHighlights(any(), any())).thenReturn(Observable.just(listOf(music, music)))
        whenever(musicDataSource.reorderHighlights(any())).thenReturn(Observable.just(listOf(music)))
        whenever(actionsDataSource.toggleRepost(any(), any(), any())).thenReturn(Observable.just(ToggleRepostResult.Notify(true, false, "title", "artist")))

        viewModel.removeGeorestrictedItemFromUploads("userSlug", music, mixpanelSource, mixpanelButton)

        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDEventObserver, never()).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(offlineAlertEventObserver, never()).onChanged(null)
        verify(musicDataSource, times(1)).reorderHighlights(any())
    }

    @Test
    fun `remove georestricted upload music, successful but does not need to remove from higlights`() {
        val music = mock<AMResultItem>()
        whenever(musicDataSource.getHighlights(any(), any())).thenReturn(Observable.just(emptyList()))
        whenever(actionsDataSource.toggleRepost(any(), any(), any())).thenReturn(Observable.just(ToggleRepostResult.Notify(true, false, "title", "artist")))

        viewModel.removeGeorestrictedItemFromUploads("userSlug", music, mixpanelSource, mixpanelButton)

        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDEventObserver, never()).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(offlineAlertEventObserver, never()).onChanged(null)
        verify(musicDataSource, never()).reorderHighlights(any())
    }

    @Test
    fun `remove georestricted upload music, failure on highlights`() {
        val music = mock<AMResultItem>()
        whenever(musicDataSource.getHighlights(any(), any())).thenReturn(Observable.error(Exception("")))

        viewModel.removeGeorestrictedItemFromUploads("userSlug", music, mixpanelSource, mixpanelButton)

        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, never()).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDEventObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(offlineAlertEventObserver, never()).onChanged(null)
        verify(musicDataSource, never()).reorderHighlights(any())
        verify(actionsDataSource, never()).toggleRepost(any(), any(), any())
    }

    @Test
    fun `remove georestricted upload music, failure on unrepost with offline status`() {
        val music = mock<AMResultItem>()
        whenever(musicDataSource.getHighlights(any(), any())).thenReturn(Observable.just(emptyList()))
        whenever(actionsDataSource.toggleRepost(any(), any(), any())).thenReturn(Observable.error(ToggleRepostException.Offline))

        viewModel.removeGeorestrictedItemFromUploads("userSlug", music, mixpanelSource, mixpanelButton)

        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDEventObserver, never()).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(offlineAlertEventObserver, times(1)).onChanged(null)
    }

    @Test
    fun `remove georestricted upload music, failure on unrepost with generic error`() {
        val music = mock<AMResultItem>()
        whenever(musicDataSource.getHighlights(any(), any())).thenReturn(Observable.just(emptyList()))
        whenever(actionsDataSource.toggleRepost(any(), any(), any())).thenReturn(Observable.error(Exception("")))

        viewModel.removeGeorestrictedItemFromUploads("userSlug", music, mixpanelSource, mixpanelButton)

        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, never()).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDEventObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(offlineAlertEventObserver, never()).onChanged(null)
    }

    // Remove highlight

    @Test
    fun `remove highlight, success`() {
        val position = 1
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.just(ToggleHighlightResult.Removed))

        viewModel.onHighlightRemoved(mock(), position, mixpanelButton, mixpanelSource)

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(removeHighlightAtPositionObserver).onChanged(position)
    }

    @Test
    fun `remove highlight, offline`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(ToggleHighlightException.Offline))

        viewModel.onHighlightRemoved(mock(), 1, mixpanelButton, mixpanelSource)

        verify(offlineAlertEventObserver).onChanged(null)
        verifyZeroInteractions(removeHighlightAtPositionObserver)
    }

    @Test
    fun `remove highlight, logged out`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(ToggleHighlightException.LoggedOut))

        viewModel.onHighlightRemoved(mock(), 1, mixpanelButton, mixpanelSource)

        verify(loggedOutAlertEventObserver).onChanged(LoginSignupSource.Highlight)
        verifyZeroInteractions(removeHighlightAtPositionObserver)

        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)

        verify(actionsDataSource, times(2)).toggleHighlight(any(), any(), any())
    }

    @Test
    fun `remove highlight, generic error`() {
        val position = 1
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(Exception("")))

        viewModel.onHighlightRemoved(mock(), position, mixpanelButton, mixpanelSource)

        verify(showHUDEventObserver).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verifyZeroInteractions(removeHighlightAtPositionObserver)
    }

    // Download

    @Test
    fun `download playlist, ask confirmation, accept`() {
        val tracksCount = 10
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.just(
            ToggleDownloadResult.ConfirmPlaylistDownload(tracksCount)
        ))

        viewModel.onDownloadTapped(mock(), mock(), "")

        verify(observerShowConfirmPlaylistSyncEvent).onChanged(argWhere { it.second == tracksCount })

        viewModel.onPlaylistSyncConfirmed(mock(), mock(), "")
        verify(actionsDataSource, times(2)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, test all results`() {
        val tracksCount = 10
        val musicTitle = "title"
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.just(
            ToggleDownloadResult.ConfirmPlaylistDeletion,
            ToggleDownloadResult.ConfirmMusicDeletion,
            ToggleDownloadResult.StartedBlockingAPICall,
            ToggleDownloadResult.EndedBlockingAPICall,
            ToggleDownloadResult.ConfirmPlaylistDownload(tracksCount),
            ToggleDownloadResult.ShowUnlockedToast(musicTitle)
        ))

        viewModel.onDownloadTapped(mock(), mock(), "")

        verify(observerShowConfirmPlaylistDownloadDeletionEvent).onChanged(any())
        verify(observerShowConfirmDownloadDeletionEvent).onChanged(any())
        verify(observerShowConfirmPlaylistSyncEvent).onChanged(argWhere { it.second == tracksCount })
        verify(showHUDEventObserver).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver).onChanged(ProgressHUDMode.Dismiss)
        verify(showUnlockedToastEventObserver).onChanged(musicTitle)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, logged out`() {
        val source = LoginSignupSource.Download
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.LoggedOut(source)
        ))

        viewModel.onDownloadTapped(mock(), mock(), "")
        verify(loggedOutAlertEventObserver).onChanged(source)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, not premium`() {
        val mode = InAppPurchaseMode.PlaylistDownload
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.Unsubscribed(mode)
        ))

        viewModel.onDownloadTapped(mock(), mock(), "")
        verify(showPremiumObserver).onChanged(mode)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, show premium downloads`() {
        val model = PremiumDownloadModel(mock(), mock(), mock())
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.ShowPremiumDownload(model)
        ))

        viewModel.onDownloadTapped(mock(), mock(), "")
        verify(showPremiumDownloadObserver).onChanged(model)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, failed downloading playlist`() {
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.FailedDownloadingPlaylist
        ))

        viewModel.onDownloadTapped(mock(), mock(), "")
        verify(observerShowFailedPlaylistDownloadEvent).onChanged(null)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun setShouldHideLogoutFollow() {
        whenever(remoteVariablesProvider.hideFollowOnSearchForLoggedOutUsers).thenReturn(true)

        viewModel.setShouldHideLogoutFollow()

        verify(userDataSource, atLeast(1)).isLoggedIn()
        verify(remoteVariablesProvider, atLeast(1)).hideFollowOnSearchForLoggedOutUsers
    }

    @Test
    fun setShouldHideLogoutFollowValue() {
        whenever(remoteVariablesProvider.hideFollowOnSearchForLoggedOutUsers).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(false)

        viewModel.setShouldHideLogoutFollow()

        verify(showFollowBtnObserver).onChanged(true)
    }
}
