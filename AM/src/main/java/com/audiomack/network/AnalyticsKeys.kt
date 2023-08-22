package com.audiomack.network

class AnalyticsKeys {

    companion object {
        const val EVENT_PREMIUM_VIEW = "PremiumView"
        const val EVENT_PREMIUM_STARTED = "PremiumCheckoutStarted"
        const val EVENT_PREMIUM_SUCCEEDED = "PurchasedPremiumTrial"
        const val EVENT_PREMIUM_FAILED = "FailedPremiumPurchase"

        const val EVENT_DEFAULTGENRE_SELECTION = "Genre Selection"
        const val PARAM_DEFAULTGENRE_SELECTION = "Selection"

        const val EVENT_REMOVED_CONTENT = "Removed Content"

        const val PARAM_SOURCE = "Source"
        const val PARAM_ENV = "Env"
        const val PARAM_ENV_VALUE = "Android"
    }
}
