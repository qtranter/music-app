package com.audiomack.data.tracking

import com.audiomack.network.AnalyticsHelper
import io.reactivex.Completable

class TrackingRepository(
    private val analytics: AnalyticsHelper = AnalyticsHelper.getInstance()
) : TrackingDataSource {

    override fun trackEvent(event: String, data: HashMap<String, String>?, providers: List<TrackingProvider>): String {
        when {
            providers.contains(TrackingProvider.Facebook) ->
                analytics.trackEventOnFacebook(event, data)
            providers.contains(TrackingProvider.Firebase) ->
                analytics.trackEventOnFirebase(event, data)
        }
        return event
    }

    override fun trackScreen(event: String): String {
        // GA/Firebase only
        analytics.trackScreen(event)
        return event
    }

    override fun trackGA(event: String, action: String?, label: String?): String {
        analytics.trackEvent(event, action, label)
        return event
    }

    override fun trackEventOnAllProviders(event: String, data: HashMap<String, String>?) {
        analytics.trackEventOnAllProviders(event, data)
    }

    override fun trackException(throwable: Throwable) {
        analytics.trackException(throwable)
    }

    override fun trackIdentity() = Completable.create { emitter ->
        analytics.trackIdentity()
        emitter.onComplete()
    }

    override fun trackBreadcrumb(message: String) {
        analytics.trackBreadcrumb(message)
    }

    override fun trackLogin() {
        analytics.trackLogin()
    }

    override fun trackLogout() {
        analytics.trackLogout()
    }

    override fun trackSignup() {
        analytics.trackSignup()
    }

    override fun log(msg: String, props: Map<String, Any>?) {
        analytics.log(msg, props)
    }

    override fun trackFirstSession() {
        analytics.trackFirstSession()
    }
}
