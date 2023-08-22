package com.audiomack.ui.browse.world.list

import androidx.paging.rxjava2.RxPagingSource
import com.audiomack.data.world.toArticle
import com.audiomack.model.WorldArticle
import com.audiomack.network.retrofitApi.WorldPostService
import com.audiomack.network.retrofitModel.WorldPostsResponse
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.Single

private const val ARTICLES_STARTING_PAGE_INDEX = 1

class WorldArticlesPagingSource(
    private val service: WorldPostService,
    private val slug: String,
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : RxPagingSource<Int, WorldArticle>() {
    override fun loadSingle(params: LoadParams<Int>): Single<LoadResult<Int, WorldArticle>> {
        val position = params.key ?: ARTICLES_STARTING_PAGE_INDEX
        val filter = if (slug.isBlank()) null else WorldPostService.TAG_QUALIFIER + slug
        return service.getPosts(position, params.loadSize, filter = filter)
                .subscribeOn(schedulersProvider.io)
                .map { toLoadResult(it, position) }
                .onErrorReturn { LoadResult.Error(it) }
    }

    private fun toLoadResult(response: WorldPostsResponse, position: Int): LoadResult<Int, WorldArticle> {
        return LoadResult.Page(
                data = response.posts.map { it.toArticle() },
                prevKey = if (position == ARTICLES_STARTING_PAGE_INDEX) null else position - 1,
                nextKey = if (response.posts.isEmpty()) null else position + 1
        )
    }
}
