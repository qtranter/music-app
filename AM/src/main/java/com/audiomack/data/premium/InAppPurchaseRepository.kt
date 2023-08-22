package com.audiomack.data.premium

import android.app.Activity
import androidx.annotation.VisibleForTesting
import com.android.billingclient.api.Purchase
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import io.reactivex.Single
import timber.log.Timber

class InAppPurchaseRepository private constructor(
    private val trackingDataSource: TrackingDataSource
) : InAppPurchaseDataSource {

    private var monthlyPackage: Package? = null

    override fun getCurrency(): String {
        return monthlyPackage?.product?.priceCurrencyCode ?: "USD"
    }

    override fun getSubscriptionPrice(): Double {
        return monthlyPackage?.let { it.product.priceAmountMicros.toDouble() / 1000000.toDouble() }
            ?: 4.99
    }

    override fun getSubscriptionPriceString(): String {
        return monthlyPackage?.product?.price ?: "$4.99"
    }

    override fun purchase(activity: Activity): Single<Boolean> {
        log("Starting purchase")
        return fetchOfferings().flatMap { purchaseRx(activity, it) }
    }

    override fun fetchOfferings(): Single<Package> {
        return Single.create { emitter ->
            monthlyPackage?.let {
                emitter.onSuccess(it)
            } ?: run {
                // TODO Use PurchasesManager
                Purchases.sharedInstance.getOfferingsWith({
                    trackPurchasesError(it)
                    emitter.onError(Exception(it.message))
                }, {
                    log("Fetched offerings")
                    it.current?.monthly?.let { monthly ->
                        monthlyPackage = monthly
                        emitter.onSuccess(monthly)
                    } ?: run {
                        emitter.onError(Exception("Unable to fetch monthly offering"))
                    }
                })
            }
        }
    }

    private fun purchaseRx(activity: Activity, packageToPurchase: Package) =
        Single.create<Boolean> { emitter ->
            // TODO Use PurchasesManager
            Purchases.sharedInstance.purchasePackage(activity, packageToPurchase, object :
                MakePurchaseListener {
                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    trackPurchasesError(error)
                    emitter.onError(Exception(error.message))
                }

                override fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo) {
                    log("Purchase completed")
                    emitter.onSuccess(true)
                }
            })
        }

    private fun log(msg: String) {
        Timber.tag(TAG).d(msg)
        trackingDataSource.trackBreadcrumb(msg)
    }

    private fun trackPurchasesError(error: PurchasesError) {
        Timber.tag(TAG).e("Purchase error: $error")
        trackingDataSource.trackException(Exception(error.message))
    }

    companion object {
        private const val TAG = "InAppPurchaseRepository"

        @Volatile
        private var INSTANCE: InAppPurchaseRepository? = null

        fun getInstance(
            trackingDataSource: TrackingDataSource = TrackingRepository()
        ): InAppPurchaseRepository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: InAppPurchaseRepository(trackingDataSource).also { INSTANCE = it }
        }

        @VisibleForTesting
        fun destroy() {
            INSTANCE = null
        }
    }
}
