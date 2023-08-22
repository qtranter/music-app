package com.audiomack.data.premium

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.audiomack.MainApplication
import com.audiomack.data.premium.BillingIssue.Subscribed
import com.audiomack.data.premium.BillingIssue.UnSubscribed
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.model.SubscriptionNotification
import com.audiomack.model.SubscriptionNotification.BillingIssueWhileSubscribed
import com.audiomack.model.SubscriptionNotification.BillingIssueWhileUnsubscribed
import com.audiomack.model.SubscriptionNotification.None
import com.audiomack.model.SubscriptionStore
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class PremiumRepository private constructor(
    private val entitlements: EntitlementManager,
    private val settings: PremiumSettingsDataSource,
    private val tracking: TrackingDataSource,
    private val schedulers: SchedulersProvider
) : PremiumDataSource {

    private val adminOverride: Boolean
        get() = settings.adminOverride

    private val adminGrantPremium: Boolean
        get() = settings.adminGrantPremium

    private val disposables = CompositeDisposable()

    private val entitlementObservable = PublishSubject.create<IsEntitlementActive>()

    private val _premiumObservable = BehaviorSubject.create<IsPremium>()
    override val premiumObservable: Observable<IsPremium> get() = _premiumObservable

    override val isPremium: Boolean
        get() = _premiumObservable.value ?: isPremiumOrOverridden(settings.savedPremium)

    override val subscriptionNotification: SubscriptionNotification
        get() = when (entitlements.entitlement?.billingIssue) {
            UnSubscribed -> BillingIssueWhileUnsubscribed
            Subscribed -> BillingIssueWhileSubscribed
            else -> None
        }

    override val subscriptionStore: SubscriptionStore
        get() = entitlements.entitlement?.store ?: SubscriptionStore.PlayStore

    init {
        // Initially load the saved premium status
        load {
            // Then start listening for entitlement updates
            subscribeEntitlementObserver()
        }
        // Update premium status on entitlement updates, allowing for admin override
        subscribePremiumObserver()
        // Save the entitlement active status to preferences on change
        savePremiumOnEntitlementChange()
        // Listen for admin override changes
        subscribeAdminOverride()
        // Log an changes to premium emitted by premium observer
        trackPremiumChanges()
    }

    override fun refresh() {
        log("Reloading entitlement data")
        entitlements.reload()
    }

    private fun isPremiumOrOverridden(premium: Boolean) =
        if (adminOverride) adminGrantPremium else premium

    private fun load(onSuccess: () -> Unit) {
        Single.just(settings.savedPremium)
            .subscribeOn(schedulers.io)
            .map {
                log("Loaded saved premium status: $it")
                isPremiumOrOverridden(it)
            }
            .observeOn(schedulers.main)
            .subscribe { premium ->
                _premiumObservable.onNext(premium)
                onSuccess()
            }
            .also { disposables.add(it) }
    }

    private fun subscribeEntitlementObserver() {
        entitlements.entitlementObservable
            .map { it.active }
            .subscribe(entitlementObservable)
    }

    private fun subscribePremiumObserver() {
        entitlementObservable
            .subscribeOn(schedulers.io)
            .doOnNext { log("Found active entitlement = $it") }
            .map(::isPremiumOrOverridden)
            .distinctUntilChanged()
            .observeOn(schedulers.main)
            .subscribe(_premiumObservable)
    }

    private fun subscribeAdminOverride() {
        settings.adminGrantPremiumObservable
            .observeOn(schedulers.main)
            .doOnNext { log("Observed admin override change: $it") }
            .subscribe { entitlements.reload() }
            .also { disposables.add(it) }
    }

    private fun savePremiumOnEntitlementChange() {
        entitlementObservable
            .subscribeOn(schedulers.io)
            .filter { it != settings.savedPremium }
            .observeOn(schedulers.main)
            .subscribe(this::savePremium)
            .also { disposables.add(it) }
    }

    private fun savePremium(premium: Boolean) {
        log("Saving premium status: $premium")
        settings.savedPremium = premium
    }

    private fun trackPremiumChanges() {
        premiumObservable.subscribe {
            log("Premium status set to $it")
        }.also { disposables.add(it) }
    }

    private fun log(msg: String) {
        Timber.tag(TAG).d(msg)
        tracking.trackBreadcrumb(msg)
    }

    companion object {
        private const val TAG = "PremiumRepository"

        @Volatile
        private var instance: PremiumRepository? = null

        fun isPremium(): Boolean = instance?.isPremium == true

        @JvmStatic
        fun init(
            context: Context,
            tracking: TrackingDataSource = TrackingRepository(),
            schedulers: SchedulersProvider = AMSchedulersProvider()
        ): PremiumRepository = init(
                PurchasesManager.init(context),
                PremiumSettingsRepository.init(context),
            tracking,
            schedulers
        )

        @JvmStatic
        fun getInstance(): PremiumRepository =
            instance ?: MainApplication.context?.let { init(it) }
            ?: throw IllegalStateException("PremiumRepository was not initialized")

        @VisibleForTesting
        @JvmStatic
        internal fun init(
            entitlements: EntitlementManager,
            settings: PremiumSettingsDataSource,
            tracking: TrackingDataSource,
            schedulers: SchedulersProvider
        ): PremiumRepository = instance ?: synchronized(this) {
            instance ?: PremiumRepository(
                entitlements,
                settings,
                tracking,
                schedulers
            ).also { instance = it }
        }

        @VisibleForTesting
        internal fun destroy() {
            instance?.disposables?.clear()
            instance = null
        }
    }
}
