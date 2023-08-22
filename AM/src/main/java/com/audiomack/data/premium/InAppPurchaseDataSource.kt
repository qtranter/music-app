package com.audiomack.data.premium

import android.app.Activity
import com.revenuecat.purchases.Package
import io.reactivex.Single

interface InAppPurchaseDataSource {

    fun getCurrency(): String

    fun getSubscriptionPrice(): Double

    fun getSubscriptionPriceString(): String

    fun purchase(activity: Activity): Single<Boolean>

    /** Emits a [Package], either already cached or newly fetched from RC **/
    fun fetchOfferings(): Single<Package>
}
