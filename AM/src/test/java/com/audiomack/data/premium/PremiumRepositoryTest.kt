package com.audiomack.data.premium

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.data.premium.BillingIssue.Subscribed
import com.audiomack.data.premium.BillingIssue.UnSubscribed
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.model.SubscriptionNotification
import com.audiomack.model.SubscriptionStore
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class PremiumRepositoryTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var entitlements: EntitlementManager

    @Mock
    private lateinit var settings: PremiumSettingsDataSource

    @Mock
    private lateinit var tracking: TrackingDataSource

    private lateinit var entitlementObservable: Subject<Entitlement>
    private lateinit var adminGrantPremiumObservable: Subject<IsPremium>

    private fun initRepository(savedPremium: Boolean = false): PremiumRepository {
        whenever(settings.savedPremium).thenReturn(savedPremium)
        return PremiumRepository.init(entitlements, settings, tracking, TestSchedulersProvider())
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        entitlementObservable = PublishSubject.create()
        whenever(entitlements.entitlementObservable).thenReturn(entitlementObservable)

        adminGrantPremiumObservable = PublishSubject.create()
        whenever(settings.adminGrantPremiumObservable).thenReturn(adminGrantPremiumObservable)
    }

    @After
    fun tearDown() {
        PremiumRepository.destroy()
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `premium observable emits restored saved premium status`() {
        val premiumObserver = TestObserver<IsPremium>()
        initRepository(true).premiumObservable.subscribe(premiumObserver)

        premiumObserver.assertNoErrors()
        premiumObserver.assertValue(true)
    }

    @Test
    fun `premium observable emits true when entitlement becomes active`() {
        val premiumObserver = TestObserver<IsPremium>()
        initRepository().premiumObservable.subscribe(premiumObserver)

        val entitlement = mock<Entitlement> {
            on { active } doReturn true
        }
        entitlementObservable.onNext(entitlement)

        premiumObserver.assertNoErrors()
        premiumObserver.assertValueAt(0, false)
        premiumObserver.assertValueAt(1, true)
    }

    @Test
    fun `premium observable emits false when entitlement is active but admin override disables premium`() {
        whenever(settings.adminOverride).thenReturn(true)
        whenever(settings.adminGrantPremium).thenReturn(false)

        val premiumObserver = TestObserver<IsPremium>()
        initRepository(true).premiumObservable.subscribe(premiumObserver)

        val entitlement = mock<Entitlement> {
            on { active } doReturn true
        }
        entitlementObservable.onNext(entitlement)

        premiumObserver.assertNoErrors()
        premiumObserver.assertValueAt(0, false)
        premiumObserver.assertValueAt(1, false)
    }

    @Test
    fun `premium observable emits false when saved premium is true but admin override disables premium`() {
        whenever(settings.adminOverride).thenReturn(true)
        whenever(settings.adminGrantPremium).thenReturn(false)

        val premiumObserver = TestObserver<IsPremium>()
        initRepository(true).premiumObservable.subscribe(premiumObserver)

        premiumObserver.assertNoErrors()
        premiumObserver.assertValueAt(0, false)
    }

    @Test
    fun `premium reflects latest value emitted by premium observable`() {
        val repository = initRepository()

        val premiumObserver = TestObserver<IsPremium>()
        repository.premiumObservable.subscribe(premiumObserver)

        val entitlement = mock<Entitlement> {
            on { active } doReturn true
        }
        entitlementObservable.onNext(entitlement)

        assertEquals(repository.isPremium, premiumObserver.values().last())
    }

    @Test
    fun `premium returns true when admin override grants premium`() {
        whenever(settings.adminOverride).thenReturn(true)
        whenever(settings.adminGrantPremium).thenReturn(true)

        val repository = initRepository()

        assertEquals(repository.isPremium, true)
    }

    @Test
    fun `premium returns false when saved premium is true but admin override disables premium`() {
        whenever(settings.adminOverride).thenReturn(true)
        whenever(settings.adminGrantPremium).thenReturn(false)

        val repository = initRepository(true)

        assertEquals(repository.isPremium, false)
    }

    @Test
    fun `no subscription notification when no entitlement billing issue`() {
        val entitlement = Entitlement(false)
        whenever(entitlements.entitlement).thenReturn(entitlement)

        val repository = initRepository()

        assertEquals(repository.subscriptionNotification, SubscriptionNotification.None)
    }

    @Test
    fun `unsubscribed subscription notification on unsubscribed entitlement billing issue`() {
        val entitlement = Entitlement(false, UnSubscribed)
        whenever(entitlements.entitlement).thenReturn(entitlement)

        val repository = initRepository()

        assertEquals(
            repository.subscriptionNotification,
            SubscriptionNotification.BillingIssueWhileUnsubscribed
        )
    }

    @Test
    fun `subscribed subscription notification on subscribed entitlement billing issue`() {
        val entitlement = Entitlement(false, Subscribed)
        whenever(entitlements.entitlement).thenReturn(entitlement)

        val repository = initRepository()

        assertEquals(
            repository.subscriptionNotification,
            SubscriptionNotification.BillingIssueWhileSubscribed
        )
    }

    @Test
    fun `entitlements are reloaded on refresh`() {
        val repository = initRepository()
        repository.refresh()
        verify(entitlements, times(1)).reload()
    }

    @Test
    fun `play store subscription on subscribed entitlement`() {
        val entitlement = Entitlement(true, null, SubscriptionStore.PlayStore)
        whenever(entitlements.entitlement).thenReturn(entitlement)

        val repository = initRepository()

        assertEquals(
            repository.subscriptionStore,
            SubscriptionStore.PlayStore
        )
    }

    @Test
    fun `app store subscription on subscribed entitlement`() {
        val entitlement = Entitlement(true, null, SubscriptionStore.AppStore)
        whenever(entitlements.entitlement).thenReturn(entitlement)

        val repository = initRepository()

        assertEquals(
            repository.subscriptionStore,
            SubscriptionStore.AppStore
        )
    }

    @Test
    fun `stripe subscription on subscribed entitlement`() {
        val entitlement = Entitlement(true, null, SubscriptionStore.Stripe)
        whenever(entitlements.entitlement).thenReturn(entitlement)

        val repository = initRepository()

        assertEquals(
            repository.subscriptionStore,
            SubscriptionStore.Stripe
        )
    }

    @Test
    fun `unknown source subscription on subscribed entitlement`() {
        val entitlement = Entitlement(true, null, SubscriptionStore.None)
        whenever(entitlements.entitlement).thenReturn(entitlement)

        val repository = initRepository()

        assertEquals(
            repository.subscriptionStore,
            SubscriptionStore.None
        )
    }
}
