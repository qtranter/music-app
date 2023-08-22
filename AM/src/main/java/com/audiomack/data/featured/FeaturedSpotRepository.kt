package com.audiomack.data.featured

import com.audiomack.model.AMFeaturedSpot
import com.audiomack.model.EventFeaturedPostPulled
import com.audiomack.network.API
import com.audiomack.network.APIInterface
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.util.Random
import org.greenrobot.eventbus.EventBus

class FeaturedSpotRepository private constructor(
    private val api: APIInterface.FeaturedSpotsInterface,
    private val eventBus: EventBus,
    private val schedulersProvider: SchedulersProvider
) : FeaturedSpotDataSource {

    private val random: Random = Random()
    private var featuredSpots: List<AMFeaturedSpot>? = null
    var featuredSpot: AMFeaturedSpot? = null

    private val disposables = CompositeDisposable()

    override fun pick() {
        disposables.add(
            get()
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ pickRandomFeaturedSpot() }, {})
        )
    }

    override fun get(): Single<List<AMFeaturedSpot>> {
        return if (featuredSpots.isNullOrEmpty()) {
            api.getFeaturedMusic()
                .doOnSuccess {
                    featuredSpots = it
                }
        } else {
            Single.just(featuredSpots)
        }
    }

    private fun pickRandomFeaturedSpot() {
        val spots = featuredSpots ?: return
        if (spots.isEmpty()) return
        val index = random.nextInt(spots.size)
        featuredSpot = spots[index]
        eventBus.post(EventFeaturedPostPulled())
    }

    companion object {
        private var INSTANCE: FeaturedSpotRepository? = null

        @JvmOverloads
        @JvmStatic
        fun getInstance(
            api: APIInterface.FeaturedSpotsInterface = API.getInstance(),
            eventBus: EventBus = EventBus.getDefault(),
            schedulersProvider: SchedulersProvider = AMSchedulersProvider()
        ): FeaturedSpotRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FeaturedSpotRepository(api, eventBus, schedulersProvider).also { INSTANCE = it }
            }
    }
}
