package com.audiomack.ui.search

import com.audiomack.data.featured.FeaturedSpotDataSource
import com.audiomack.data.featured.FeaturedSpotRepository
import com.audiomack.data.search.SearchDataSource
import com.audiomack.data.search.SearchRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMFeaturedSpot
import io.reactivex.Single

interface SearchTrendingUseCase {
    fun getTrendingSearches(): Single<List<AMFeaturedSpot>>
}

class SearchTrendingUseCaseImpl(
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val searchDataSource: SearchDataSource = SearchRepository(),
    private val featuredSpotDataSource: FeaturedSpotDataSource = FeaturedSpotRepository.getInstance()
) : SearchTrendingUseCase {

    override fun getTrendingSearches(): Single<List<AMFeaturedSpot>> {
        return userDataSource.isLoggedInAsync()
            .onErrorReturnItem(false)
            .flatMap { loggedIn ->
                if (loggedIn) {
                    searchDataSource.getRecommendations()
                        .flatMap {
                            if (it.isEmpty()) {
                                featuredSpotDataSource.get()
                            } else {
                                Single.just(it.map { music -> AMFeaturedSpot.fromMusic(music) })
                            }
                        }
                } else {
                    featuredSpotDataSource.get()
                }
            }
    }
}
