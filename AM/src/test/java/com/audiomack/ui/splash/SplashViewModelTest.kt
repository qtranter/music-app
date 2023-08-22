package com.audiomack.ui.splash

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.deeplink.DeeplinkDataSource
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.socialauth.SocialAuthManager
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.Credentials
import com.audiomack.model.PermissionType
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class SplashViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var preferencesDataSource: PreferencesDataSource

    @Mock
    private lateinit var reachabilityDataSource: ReachabilityDataSource

    @Mock
    private lateinit var socialAuthManager: SocialAuthManager

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var remoteVariablesProvider: RemoteVariablesProvider

    @Mock
    private lateinit var adsDataSource: AdsDataSource

    @Mock
    private lateinit var deeplinkDataSource: DeeplinkDataSource

    @Mock
    private lateinit var trackingDataSource: TrackingDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: SplashViewModel

    // Observers

    @Mock
    private lateinit var observerShowRetryLoginEvent: Observer<Void>

    @Mock
    private lateinit var observerGoHomeEvent: Observer<Void>

    @Mock
    private lateinit var observerShowPermissionsViewEvent: Observer<Void>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        viewModel = SplashViewModel(
            userDataSource,
            preferencesDataSource,
            reachabilityDataSource,
            socialAuthManager,
            mixpanelDataSource,
            remoteVariablesProvider,
            adsDataSource,
            deeplinkDataSource,
            trackingDataSource,
            TestSchedulersProvider()
        ).apply {
            showRetryLoginEvent.observeForever(observerShowRetryLoginEvent)
            goHomeEvent.observeForever(observerGoHomeEvent)
            showPermissionsViewEvent.observeForever(observerShowPermissionsViewEvent)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun tryAgain() {
        `when`(userDataSource.credentials).thenReturn(Credentials())
        val observerShowLoadingUI: Observer<Void> = mock()
        val observerRunAutologin: Observer<Void> = mock()
        viewModel.showLoadingUIEvent.observeForever(observerShowLoadingUI)
        viewModel.runAutologinEvent.observeForever(observerRunAutologin)
        viewModel.onTryAgainTapped()
        verify(observerShowLoadingUI).onChanged(null)
        verify(observerRunAutologin).onChanged(anyOrNull())
    }

    @Test
    fun grantPermissions() {
        val observer: Observer<Void> = mock()
        viewModel.grantPermissionsEvent.observeForever(observer)
        viewModel.onGrantPermissionsTapped(mock())
        verify(observer).onChanged(null)
        verify(preferencesDataSource).setPermissionsShown(any(), eq("yes"))
    }

    @Test
    fun goToDownloads() {
        val observer: Observer<Void> = mock()
        viewModel.goToDownloadsEvent.observeForever(observer)
        viewModel.onGoToDownloadsTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `on permissions result, valid request code`() {
        viewModel.onRequestPermissionsResult(mock(), viewModel.reqCodePermissions, emptyArray(), intArrayOf())
        verify(mixpanelDataSource).trackEnablePermissions(any(), any(), any())
        verify(observerGoHomeEvent).onChanged(null)
    }

    @Test
    fun `on permissions result, invalid request code`() {
        viewModel.onRequestPermissionsResult(mock(), viewModel.reqCodePermissions + 1, emptyArray(), intArrayOf())
        verify(mixpanelDataSource).trackEnablePermissions(any(), any(), any())
        verifyZeroInteractions(observerGoHomeEvent)
    }

    @Test
    fun `onCreate logged in, not fresh install`() {
        `when`(remoteVariablesProvider.initialise()).thenReturn(Observable.just(true))
        `when`(adsDataSource.isFreshInstall()).thenReturn(false)
        `when`(preferencesDataSource.needToShowPermissions(any())).thenReturn(false)
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(userDataSource.credentials).thenReturn(Credentials())
        val observerDeleteNotifications: Observer<Void> = mock()
        val observerRunAutologin: Observer<Void> = mock()
        val observerShowPermissions: Observer<Void> = mock()
        viewModel.deleteNotificationEvent.observeForever(observerDeleteNotifications)
        viewModel.runAutologinEvent.observeForever(observerRunAutologin)
        viewModel.showPermissionsViewEvent.observeForever(observerShowPermissions)
        viewModel.onCreate(mock(), 0)
        verify(observerDeleteNotifications).onChanged(null)
        verify(observerRunAutologin).onChanged(null)
        verifyZeroInteractions(observerShowPermissions)
        verifyZeroInteractions(observerGoHomeEvent)
        verify(mixpanelDataSource).trackGeneralProperties(anyOrNull())
        verifyZeroInteractions(trackingDataSource)
    }

    @Test
    fun `onCreate fresh install, remote var init succeeds`() {
        `when`(remoteVariablesProvider.initialise()).thenReturn(Observable.just(true))
        `when`(adsDataSource.isFreshInstall()).thenReturn(true)
        val observerRunAutologin: Observer<Void> = mock()
        viewModel.runAutologinEvent.observeForever(observerRunAutologin)
        viewModel.onCreate(mock(), 0)
        verifyZeroInteractions(observerRunAutologin)
        verifyZeroInteractions(observerShowPermissionsViewEvent)
        verify(observerGoHomeEvent).onChanged(null)
        verify(mixpanelDataSource).trackGeneralProperties(anyOrNull())
        verify(trackingDataSource).trackFirstSession()
    }

    @Test
    fun `onCreate fresh install, remote var init fails`() {
        `when`(remoteVariablesProvider.initialise()).thenReturn(Observable.error(Exception("Test")))
        `when`(adsDataSource.isFreshInstall()).thenReturn(true)
        val observerRunAutologin: Observer<Void> = mock()
        viewModel.runAutologinEvent.observeForever(observerRunAutologin)
        viewModel.onCreate(mock(), 0)
        verifyZeroInteractions(observerRunAutologin)
        verifyZeroInteractions(observerShowPermissionsViewEvent)
        verify(observerGoHomeEvent).onChanged(null)
        verify(mixpanelDataSource).trackGeneralProperties(anyOrNull())
        verify(trackingDataSource).trackFirstSession()
    }

    @Test
    fun `request location permission`() {
        viewModel.onRequestedLocationPermission()
        verify(mixpanelDataSource).trackPromptPermissions(eq(PermissionType.Location))
    }

    @Test
    fun `privacy policy`() {
        val observer: Observer<String> = mock()
        viewModel.openURLEvent.observeForever(observer)
        viewModel.onPrivacyPolicyTapped()
        verify(observer).onChanged(any())
    }

    @Test
    fun `permissions already granted when clicking on request permissions button`() {
        viewModel.onPermissionsAlreadyGranted()
        verify(observerGoHomeEvent).onChanged(null)
    }

    @Test
    fun `on branch deeplink detected`() {
        val deeplink = "bla"
        viewModel.onBranchDeeplinkDetected(mock(), deeplink)
        verify(deeplinkDataSource).updateBranchDeeplink(eq(deeplink))
        verify(preferencesDataSource).needToShowPermissions(any())
    }

    @Test
    fun `autologin offline`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(false)
        viewModel.autologin(mock())
        verify(observerShowRetryLoginEvent).onChanged(null)
    }

    @Test
    fun `autologin online, facebook token refresh succeeds, need to show permissions`() {
        whenever(preferencesDataSource.needToShowPermissions(any())).thenReturn(true)
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.credentials).thenReturn(Credentials().apply { facebookId = "123" })
        whenever(socialAuthManager.refreshFacebookToken()).thenReturn(Observable.just(true))
        viewModel.autologin(mock())
        verifyZeroInteractions(observerShowRetryLoginEvent)
        verify(socialAuthManager).refreshFacebookToken()
        verify(observerShowPermissionsViewEvent).onChanged(null)
        verifyZeroInteractions(observerGoHomeEvent)
    }

    @Test
    fun `autologin online, facebook token refresh succeeds, no need to show permissions`() {
        whenever(preferencesDataSource.needToShowPermissions(any())).thenReturn(false)
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.credentials).thenReturn(Credentials().apply { facebookId = "123" })
        whenever(socialAuthManager.refreshFacebookToken()).thenReturn(Observable.just(true))
        viewModel.autologin(mock())
        verifyZeroInteractions(observerShowRetryLoginEvent)
        verify(socialAuthManager).refreshFacebookToken()
        verifyZeroInteractions(observerShowPermissionsViewEvent)
        verify(observerGoHomeEvent).onChanged(null)
    }

    @Test
    fun `autologin online, google token refresh succeeds, need to show permissions`() {
        whenever(preferencesDataSource.needToShowPermissions(any())).thenReturn(true)
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.credentials).thenReturn(Credentials().apply { googleToken = "123" })
        whenever(socialAuthManager.refreshGoogleToken()).thenReturn(Observable.just(true))
        viewModel.autologin(mock())
        verifyZeroInteractions(observerShowRetryLoginEvent)
        verify(socialAuthManager).refreshGoogleToken()
        verify(observerShowPermissionsViewEvent).onChanged(null)
        verifyZeroInteractions(observerGoHomeEvent)
    }

    @Test
    fun `autologin online, google token refresh succeeds, no need to show permissions`() {
        whenever(preferencesDataSource.needToShowPermissions(any())).thenReturn(false)
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.credentials).thenReturn(Credentials().apply { googleToken = "123" })
        whenever(socialAuthManager.refreshGoogleToken()).thenReturn(Observable.just(true))
        viewModel.autologin(mock())
        verifyZeroInteractions(observerShowRetryLoginEvent)
        verify(socialAuthManager).refreshGoogleToken()
        verifyZeroInteractions(observerShowPermissionsViewEvent)
        verify(observerGoHomeEvent).onChanged(null)
    }

    @Test
    fun `autologin online, other login method`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.credentials).thenReturn(Credentials())
        viewModel.autologin(mock())
        verifyZeroInteractions(observerShowRetryLoginEvent)
        verifyZeroInteractions(socialAuthManager)
    }
}
