package com.audiomack.data.tracking

import io.reactivex.Completable

interface TrackingDataSource {
    // Would love to have an enum of some sort for event
    // so all events would be in one place. More of a
    // map of sorts so We could say something like
    // TrackingEvent.LOGIN_ATTEMPT which would resolve to a string
    //
    // Also this method only returns the event for purposes of testing
    fun trackEvent(event: String, data: HashMap<String, String>? = null, providers: List<TrackingProvider>): String

    fun trackScreen(event: String): String

    fun trackGA(event: String, action: String?, label: String?): String

    fun trackEventOnAllProviders(event: String, data: HashMap<String, String>? = null)

    fun trackException(throwable: Throwable)

    fun trackIdentity(): Completable

    fun trackBreadcrumb(message: String)

    fun trackLogin()

    fun trackLogout()

    fun trackSignup()

    fun log(msg: String, props: Map<String, Any>? = null)

    fun trackFirstSession()
}
