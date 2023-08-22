package com.audiomack.data.premium

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.audiomack.BuildConfig
import com.audiomack.data.premium.BillingIssue.Subscribed
import com.audiomack.data.premium.BillingIssue.UnSubscribed
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.EventLoginState.LOGGED_IN
import com.audiomack.model.SubscriptionStore
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.getPurchaserInfoWith
import com.revenuecat.purchases.identifyWith
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener
import com.revenuecat.purchases.restorePurchasesWith
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

class PurchasesManager private constructor(
    context: Context,
    private val userDataSource: UserDataSource,
    private val trackingDataSource: TrackingDataSource,
    private val premiumSettingsDataSource: PremiumSettingsDataSource
) : EntitlementManager, UpdatedPurchaserInfoListener {

    val revenueCatUserId: String
        get() = Purchases.sharedInstance.appUserID

    private val disposables = CompositeDisposable()

    private val purchases: Purchases
        get() = Purchases.sharedInstance

    private val appUserId: String?
        get() = userDataSource.getUserId()

    override val entitlementObservable = BehaviorSubject.create<Entitlement>()

    override val entitlement: Entitlement?
        get() = entitlementObservable.value

    init {
        Purchases.debugLogsEnabled = BuildConfig.AUDIOMACK_DEBUG
        Purchases.configure(context, BuildConfig.AM_REVENUE_CAT_KEY, appUserId).apply {
            allowSharingPlayStoreAccount = false
            updatedPurchaserInfoListener = this@PurchasesManager
        }

        userDataSource.loginEvents
            .subscribe {
                when (it) {
                    LOGGED_IN -> onLogin()
                    else -> onLogout()
                }
            }
            .also { disposables.add(it) }
    }

    override fun onReceived(purchaserInfo: PurchaserInfo) {
        log("Received purchaser info update from listener")
        processPurchaserInfo(purchaserInfo)
    }

    override fun restore(): Single<IsEntitlementActive> = Single.create { emitter ->
        Purchases.sharedInstance.restorePurchases(object : ReceivePurchaserInfoListener {
            override fun onReceived(purchaserInfo: PurchaserInfo) {
                log("Restore purchases succeeded")
                processPurchaserInfo(purchaserInfo)
                emitter.onSuccess(isEntitlementActive(purchaserInfo))
            }

            override fun onError(error: PurchasesError) {
                log("Restore purchases failed")
                trackPurchasesError(error)
                emitter.onError(Exception(error.message))
            }
        })
    }

    override fun reload() {
        purchases.getPurchaserInfoWith(::trackPurchasesError) { info ->
            processPurchaserInfo(info)
        }
    }

    private fun onLogin() {
        val userId = appUserId ?: return
        purchases.identifyWith(userId, ::trackPurchasesError) { info ->
            processPurchaserInfo(info)
            restorePurchasesIfLegacy()
        }
    }

    private fun onLogout() {
        purchases.reset()
        reload()
    }

    private fun isEntitlementActive(purchaserInfo: PurchaserInfo): Boolean {
        val entitlementInfo = purchaserInfo.entitlements[ENTITLEMENT_ID]
        return entitlementInfo?.isActive == true
    }

    private fun getBillingIssue(purchaserInfo: PurchaserInfo): BillingIssue? =
        purchaserInfo.entitlements[ENTITLEMENT_ID]?.billingIssueDetectedAt?.let {
            Subscribed
        } ?: purchaserInfo.entitlements.all.filter { !it.value.isActive }
            .map { it.value }
            .firstOrNull { it.billingIssueDetectedAt != null }?.let {
                UnSubscribed
            }

    private fun getStore(purchaserInfo: PurchaserInfo): SubscriptionStore =
        purchaserInfo.entitlements[ENTITLEMENT_ID]?.store?.let {
            when (it) {
                Store.APP_STORE, Store.MAC_APP_STORE -> SubscriptionStore.AppStore
                Store.PLAY_STORE -> SubscriptionStore.PlayStore
                Store.STRIPE -> SubscriptionStore.Stripe
                Store.PROMOTIONAL, Store.UNKNOWN_STORE -> SubscriptionStore.None
            }
        } ?: SubscriptionStore.None

    private fun processPurchaserInfo(purchaserInfo: PurchaserInfo) {
        trackPurchaserInfo(purchaserInfo)

        val active = isEntitlementActive(purchaserInfo)
        val billingIssue = getBillingIssue(purchaserInfo)
        val store = getStore(purchaserInfo)
        val entitlement = Entitlement(active, billingIssue, store)
        entitlementObservable.onNext(entitlement)
    }

    private fun restorePurchasesIfLegacy() {
        if (premiumSettingsDataSource.isLegacyPremium) {
            log("Restoring legacy purchase")
            Purchases.sharedInstance.restorePurchasesWith({ error ->
                premiumSettingsDataSource.deleteLegacyPremium()
                trackPurchasesError(error)
            }, { info ->
                premiumSettingsDataSource.deleteLegacyPremium()
                processPurchaserInfo(info)
            })
        }
    }

    private fun trackPurchaserInfo(purchaserInfo: PurchaserInfo?) {
        val info = purchaserInfo ?: run {
            trackingDataSource.log("Purchaser Info set to null")
            return
        }

        val props = mutableMapOf<String, Any>()
        info.latestExpirationDate?.let { props["latestExpirationDate"] = it }
        props["activeSubscriptions"] = info.activeSubscriptions.joinToString()
        props["activeEntitlements"] = info.entitlements.active.map { it.toString() }.joinToString()
        props["entitlements"] = info.entitlements.all.map { it.toString() }.joinToString()
        props["nonConsumablePurchases"] = info.purchasedNonSubscriptionSkus
        props["requestDate"] = info.requestDate
        trackingDataSource.log("Purchaser Info", props)

        Timber.tag(TAG).d("trackPurchaserInfo : info = $purchaserInfo")
    }

    private fun trackPurchasesError(error: PurchasesError) {
        val throwable = Exception(error.message)
        Timber.tag(TAG).e(throwable)
        trackingDataSource.trackException(throwable)
    }

    private fun log(msg: String) {
        Timber.tag(TAG).d(msg)
        trackingDataSource.trackBreadcrumb(msg)
    }

    companion object {
        private const val TAG = "PurchasesManager"

        private const val ENTITLEMENT_ID = BuildConfig.AM_REVENUE_CAT_ENTITLEMENT

        @Volatile
        private var instance: PurchasesManager? = null

        @JvmStatic
        fun init(context: Context): PurchasesManager = instance ?: synchronized(this) {
            instance ?: PurchasesManager(
                context,
                UserRepository.getInstance(),
                TrackingRepository(),
                PremiumSettingsRepository.init(context)
            ).also { instance = it }
        }

        @JvmStatic
        fun getInstance(): PurchasesManager =
            instance ?: throw IllegalStateException("PurchaserInfoManager was not initiated")

        @VisibleForTesting
        fun destroy() {
            instance?.disposables?.clear()
            instance = null
        }
    }
}
