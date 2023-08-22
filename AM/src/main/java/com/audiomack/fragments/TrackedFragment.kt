package com.audiomack.fragments

import androidx.fragment.app.Fragment
import com.audiomack.data.tracking.TrackingRepository

open class TrackedFragment : Fragment {

    private val trackingRepository = TrackingRepository()

    private val logTag: String

    constructor() : super() {
        this.logTag = this.javaClass.simpleName
    }

    constructor(logTag: String) : super() {
        this.logTag = logTag
    }

    constructor(layoutResId: Int, logTag: String) : super(layoutResId) {
        this.logTag = logTag
    }

    override fun onResume() {
        super.onResume()
        trackingRepository.trackBreadcrumb("$logTag - resumed")
    }
}
