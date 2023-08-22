package com.audiomack.data.feed

import com.audiomack.model.APIRequestData

interface FeedDataSource {

    fun getMyFeed(
        page: Int,
        excludeReups: Boolean,
        ignoreGeorestrictedMusic: Boolean
    ): APIRequestData
}
