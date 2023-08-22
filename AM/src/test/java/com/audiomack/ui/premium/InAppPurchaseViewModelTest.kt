package com.audiomack.ui.premium

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.premium.EntitlementManager
import com.audiomack.data.premium.InAppPurchaseDataSource
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.appsflyer.AppsFlyerDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.EventLoginState
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.lang.Exception
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class InAppPurchaseViewModelTest {

    @Mock
    private lateinit var inAppPurchaseDataSource: InAppPurchaseDataSource

    @Mock
    private lateinit var entitlementManager: EntitlementManager

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var adsDataSource: AdsDataSource

    @Mock
    private lateinit var appsFlyerDataSource: AppsFlyerDataSource

    @Mock
    private lateinit var trackingDataSource: TrackingDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: InAppPurchaseViewModel

    private lateinit var premiumObservable: Subject<Boolean>

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        premiumObservable = PublishSubject.create()
        whenever(premiumDataSource.premiumObservable).thenReturn(premiumObservable)

        loginStateChangeSubject = BehaviorSubject.create()
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)

        schedulersProvider = TestSchedulersProvider()
        viewModel = InAppPurchaseViewModel(
            inAppPurchaseDataSource,
            entitlementManager,
            premiumDataSource,
            userDataSource,
            mixpanelDataSource,
            appsFlyerDataSource,
            trackingDataSource,
            adsDataSource,
            schedulersProvider
        ).apply {
            mode = InAppPurchaseMode.Settings
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observer: Observer<Unit> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `on create free, UI updated twice because fetchOfferings succeeds`() {
        whenever(inAppPurchaseDataSource.fetchOfferings()).thenReturn(Single.just(mock()))

        val observerClose: Observer<Unit> = mock()
        val observerSubscriptionPrice: Observer<String> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.subscriptionPriceString.observeForever(observerSubscriptionPrice)
        viewModel.onCreate()
        verify(observerSubscriptionPrice, times(2)).onChanged(null)
        verifyZeroInteractions(observerClose)
        verify(mixpanelDataSource).trackViewPremiumSubscription(any())
        verify(appsFlyerDataSource).trackPremiumView()
        verify(inAppPurchaseDataSource, times(1)).fetchOfferings()
    }

    @Test
    fun `close on premium`() {
        val closeObserver: Observer<Unit> = mock()
        viewModel.closeEvent.observeForever(closeObserver)

        premiumObservable.onNext(true)

        verify(closeObserver, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `reload entitlement on create`() {
        whenever(inAppPurchaseDataSource.fetchOfferings()).thenReturn(Single.just(mock()))
        viewModel.onCreate()
        verify(entitlementManager, times(1)).reload()
    }

    @Test
    fun `on create free, UI updated once because fetchOfferings fails`() {
        whenever(inAppPurchaseDataSource.fetchOfferings()).thenReturn(Single.error(Exception("")))

        val observerClose: Observer<Unit> = mock()
        val observerSubscriptionPrice: Observer<String> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.subscriptionPriceString.observeForever(observerSubscriptionPrice)
        viewModel.onCreate()
        verify(observerSubscriptionPrice, times(1)).onChanged(anyOrNull())
        verifyZeroInteractions(observerClose)
        verify(mixpanelDataSource).trackViewPremiumSubscription(any())
        verify(appsFlyerDataSource).trackPremiumView()
        verify(inAppPurchaseDataSource, times(1)).fetchOfferings()
    }

    @Test
    fun `upgrade not logged in, then login an succeed`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)
        `when`(inAppPurchaseDataSource.purchase(any())).thenReturn(Single.just(true))

        val observerLogin: Observer<Unit> = mock()
        val observerClose: Observer<Unit> = mock()
        val observerRequestUpgrade: Observer<Unit> = mock()
        viewModel.startLoginFlowEvent.observeForever(observerLogin)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.requestUpgradeEvent.observeForever(observerRequestUpgrade)
        viewModel.onUpgradeTapped(mock())
        verify(observerLogin).onChanged(null)
        verify(mixpanelDataSource, never()).trackPremiumCheckoutStarted(any())
        verify(appsFlyerDataSource, never()).trackPremiumStart()

        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)

        verify(observerRequestUpgrade).onChanged(null)
    }

    @Test
    fun `upgrade loggedin purchase successful`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(inAppPurchaseDataSource.purchase(any())).thenReturn(Single.just(true))

        val observerLogin: Observer<Unit> = mock()
        val observerClose: Observer<Unit> = mock()
        viewModel.startLoginFlowEvent.observeForever(observerLogin)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onUpgradeTapped(mock())
        verifyZeroInteractions(observerLogin)
        verify(observerClose).onChanged(null)
        verify(mixpanelDataSource).trackPremiumCheckoutStarted(any())
        verify(mixpanelDataSource).trackPurchasePremiumTrial(any(), any())
        verify(appsFlyerDataSource).trackPremiumStart()
        verify(appsFlyerDataSource).trackPremiumTrial()
        verify(adsDataSource).toggle()
    }

    @Test
    fun `upgrade loggedin purchase failed`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(inAppPurchaseDataSource.purchase(any())).thenReturn(Single.create {
            it.onError(
                Exception("")
            )
        })

        val observerLogin: Observer<Unit> = mock()
        val observerClose: Observer<Unit> = mock()
        viewModel.startLoginFlowEvent.observeForever(observerLogin)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onUpgradeTapped(mock())
        verifyZeroInteractions(observerLogin)
        verifyZeroInteractions(observerClose)
        verify(mixpanelDataSource).trackPremiumCheckoutStarted(any())
        verify(appsFlyerDataSource).trackPremiumStart()
    }

    @Test
    fun `restore not logged in, then login and succeed`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)

        val observerLogin: Observer<Unit> = mock()
        val observerShowRestoreLoading: Observer<Unit> = mock()
        val observerHideRestoreLoading: Observer<Unit> = mock()
        val observerClose: Observer<Unit> = mock()
        viewModel.startLoginFlowEvent.observeForever(observerLogin)
        viewModel.showRestoreLoadingEvent.observeForever(observerShowRestoreLoading)
        viewModel.hideRestoreLoadingEvent.observeForever(observerHideRestoreLoading)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onRestoreTapped()
        verify(observerLogin).onChanged(null)

        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(entitlementManager.restore()).thenReturn(Single.create { it.onSuccess(true) })

        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)

        verify(entitlementManager).restore()
        verify(observerShowRestoreLoading).onChanged(null)
        verify(observerHideRestoreLoading).onChanged(null)
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `restore logged in, fails`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(entitlementManager.restore()).thenReturn(Single.create { it.onError(Exception("")) })

        val observerShowRestoreLoading: Observer<Unit> = mock()
        val observerHideRestoreLoading: Observer<Unit> = mock()
        val observerShowRestoreError: Observer<Unit> = mock()
        val observerShowRestoreErrorNoSubs: Observer<Unit> = mock()
        val observerClose: Observer<Unit> = mock()
        viewModel.showRestoreLoadingEvent.observeForever(observerShowRestoreLoading)
        viewModel.hideRestoreLoadingEvent.observeForever(observerHideRestoreLoading)
        viewModel.showRestoreFailureErrorEvent.observeForever(observerShowRestoreError)
        viewModel.showRestoreFailureNoSubscriptionsEvent.observeForever(observerShowRestoreErrorNoSubs)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onRestoreTapped()
        verify(observerShowRestoreLoading).onChanged(null)
        verify(observerHideRestoreLoading).onChanged(null)
        verify(observerShowRestoreError).onChanged(null)
        verifyZeroInteractions(observerShowRestoreErrorNoSubs)
        verifyZeroInteractions(observerClose)
    }

    @Test
    fun `restore logged in, succeeds, nothing to be restored`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(entitlementManager.restore()).thenReturn(Single.create { it.onSuccess(false) })

        val observerShowRestoreLoading: Observer<Unit> = mock()
        val observerHideRestoreLoading: Observer<Unit> = mock()
        val observerShowRestoreError: Observer<Unit> = mock()
        val observerShowRestoreErrorNoSubs: Observer<Unit> = mock()
        val observerClose: Observer<Unit> = mock()
        viewModel.showRestoreLoadingEvent.observeForever(observerShowRestoreLoading)
        viewModel.hideRestoreLoadingEvent.observeForever(observerHideRestoreLoading)
        viewModel.showRestoreFailureErrorEvent.observeForever(observerShowRestoreError)
        viewModel.showRestoreFailureNoSubscriptionsEvent.observeForever(observerShowRestoreErrorNoSubs)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onRestoreTapped()
        verify(observerShowRestoreLoading).onChanged(null)
        verify(observerHideRestoreLoading).onChanged(null)
        verifyZeroInteractions(observerShowRestoreError)
        verify(observerShowRestoreErrorNoSubs).onChanged(null)
        verifyZeroInteractions(observerClose)
    }

    @Test
    fun `restore logged in, succeeds, user is premium`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(entitlementManager.restore()).thenReturn(Single.create { it.onSuccess(true) })

        val observerShowRestoreLoading: Observer<Unit> = mock()
        val observerHideRestoreLoading: Observer<Unit> = mock()
        val observerShowRestoreError: Observer<Unit> = mock()
        val observerShowRestoreErrorNoSubs: Observer<Unit> = mock()
        val observerClose: Observer<Unit> = mock()
        viewModel.showRestoreLoadingEvent.observeForever(observerShowRestoreLoading)
        viewModel.hideRestoreLoadingEvent.observeForever(observerHideRestoreLoading)
        viewModel.showRestoreFailureErrorEvent.observeForever(observerShowRestoreError)
        viewModel.showRestoreFailureNoSubscriptionsEvent.observeForever(observerShowRestoreErrorNoSubs)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onRestoreTapped()
        verify(observerShowRestoreLoading).onChanged(null)
        verify(observerHideRestoreLoading).onChanged(null)
        verifyZeroInteractions(observerShowRestoreError)
        verifyZeroInteractions(observerShowRestoreErrorNoSubs)
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `on login state change, canceled login`() {
        val observerShowRestoreLoading: Observer<Unit> = mock()
        val observerHideRestoreLoading: Observer<Unit> = mock()
        loginStateChangeSubject.onNext(EventLoginState.CANCELED_LOGIN)
        verifyZeroInteractions(observerShowRestoreLoading)
        verifyZeroInteractions(observerHideRestoreLoading)
    }
}
