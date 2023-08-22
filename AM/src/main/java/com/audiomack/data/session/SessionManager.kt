package com.audiomack.data.session

/**
 * This is meant to be a place to store some data tied to an app session.
 */
interface SessionManager {
    val canShowTrendingBanner: Boolean
    fun onTrendingBannerClosed()
}

object SessionManagerImpl : SessionManager {

    private var trendingBannerClosed: Boolean = false

    override val canShowTrendingBanner: Boolean
        get() = !trendingBannerClosed

    override fun onTrendingBannerClosed() {
        trendingBannerClosed = true
    }
}
