package com.audiomack.model

import com.audiomack.SUBSCRIPTIONS_APPLE_URL
import com.audiomack.SUBSCRIPTIONS_PLAYSTORE_URL
import com.audiomack.SUBSCRIPTIONS_STRIPE_URL

enum class SubscriptionStore(val url: String?) {
    None(null),
    PlayStore(SUBSCRIPTIONS_PLAYSTORE_URL),
    AppStore(SUBSCRIPTIONS_APPLE_URL),
    Stripe(SUBSCRIPTIONS_STRIPE_URL)
}
