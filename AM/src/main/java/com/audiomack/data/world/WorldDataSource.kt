package com.audiomack.data.world

import androidx.paging.PagingData
import com.audiomack.model.WorldArticle
import com.audiomack.model.WorldPage
import io.reactivex.Flowable
import io.reactivex.Single

interface WorldDataSource {

    /**
     * Fetches the pages (e.g. features, videos, news)
     */
    fun getPages(): Single<List<WorldPage>>

    /**
     * Fetches a paginated list of [WorldArticle]
     * @param page: the page (or tag) to be used for filtering
     */
    fun getPostsStream(page: WorldPage): Flowable<PagingData<WorldArticle>>

    /**
     * Fetches an article details
     * @param slug: the article slug
     */
    fun getPost(slug: String): Single<WorldArticle>
}
