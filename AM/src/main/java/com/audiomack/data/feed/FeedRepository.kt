package com.audiomack.data.feed

import com.audiomack.network.API
import com.audiomack.network.APIInterface

class FeedRepository(
    private val api: APIInterface.FeedInterface = API.getInstance()
) : FeedDataSource {

    override fun getMyFeed(
        page: Int,
        excludeReups: Boolean,
        ignoreGeorestrictedMusic: Boolean
    ) = api.getMyFeed(page, excludeReups, ignoreGeorestrictedMusic)
}
