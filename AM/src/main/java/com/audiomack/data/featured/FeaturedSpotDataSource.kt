package com.audiomack.data.featured

import com.audiomack.model.AMFeaturedSpot
import io.reactivex.Single

interface FeaturedSpotDataSource {

    /** Picks a different featured item, eventually triggers the featured items fetch if not downloaded yet **/
    fun pick()

    /** Emits the featured items that are retrieved from in memory cache or from the API **/
    fun get(): Single<List<AMFeaturedSpot>>
}
