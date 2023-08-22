package com.audiomack.data.premium

import com.audiomack.model.SubscriptionNotification
import com.audiomack.model.SubscriptionStore
import io.reactivex.Observable
import io.reactivex.Single

typealias IsPremium = Boolean
typealias IsEntitlementActive = Boolean
typealias AdminOverride = Boolean

/**
 * A billing issue may arise when the user is subscribed, or unsubscribed
 */
enum class BillingIssue {
    Subscribed, UnSubscribed
}

/**
 * The premium subscription entitlement
 *
 * @param active Whether the user's premium subscription is active
 * @param billingIssue Non-null if there is a billing issue
 */
data class Entitlement(
    val active: Boolean,
    val billingIssue: BillingIssue? = null,
    val store: SubscriptionStore = SubscriptionStore.None
)

/**
 * Manage the premium subscription.
 *
 * @see Entitlement
 */
internal interface EntitlementManager {
    val entitlementObservable: Observable<Entitlement>
    val entitlement: Entitlement?
    fun restore(): Single<IsEntitlementActive>
    fun reload()
}

/**
 * Access or observe the premium status. PremiumDataSource observes [EntitlementManager] and checks
 * for active entitlements while allowing for admin overrides.
 *
 * @see PremiumSettingsDataSource
 */
interface PremiumDataSource {
    /**
     * Listen for changes to the premium status.
     *
     * **Upstream emissions may be overridden by admin settings.**
     */
    val premiumObservable: Observable<IsPremium>

    /**
     * Whether the user currently has an active premium subscription.
     *
     * **May be overridden by admin settings.**
     */
    val isPremium: Boolean

    /**
     * An issue with the subscription; typically billing related.
     */
    val subscriptionNotification: SubscriptionNotification

    /**
     * The Store where the subscription has been charged.
     */
    val subscriptionStore: SubscriptionStore

    /**
     * Invalidate the cached premium status and trigger a new load
     */
    fun refresh()
}

interface PremiumSettingsDataSource {
    /**
     * The locally persisted premium status
     */
    var savedPremium: Boolean

    /**
     * Whether [adminGrantPremium] should be allowed to override premium status
     */
    var adminOverride: Boolean

    /**
     * If [adminGrantPremium] is true, used to manually control premium status
     */
    var adminGrantPremium: Boolean

    /**
     * Listen for changes to admin premium status changes
     */
    val adminGrantPremiumObservable: Observable<AdminOverride>

    /**
     * Old entitlements were locally persisted. This is true if those records still exist
     */
    val isLegacyPremium: Boolean

    /**
     * Deletes legacy entitlement records
     *
     * @see isLegacyPremium
     */
    fun deleteLegacyPremium()
}
