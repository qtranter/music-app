package com.audiomack.data.world

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.rxjava2.flowable
import com.audiomack.model.WorldArticle
import com.audiomack.model.WorldPage
import com.audiomack.network.API
import com.audiomack.network.retrofitApi.WorldPostService
import com.audiomack.network.retrofitModel.WorldPageResponse
import com.audiomack.network.retrofitModel.WorldPostResponse
import com.audiomack.ui.browse.world.list.WorldArticlesPagingSource

private const val NETWORK_PAGE_SIZE = 5

class WorldRepository(
    private val service: WorldPostService = API.getInstance().worldPostService
) : WorldDataSource {

    override fun getPages() = service.getPages().map { it.pages.map { it.toPage() } }

    override fun getPostsStream(page: WorldPage) =
        Pager(
                config = PagingConfig(
                        pageSize = NETWORK_PAGE_SIZE,
                        enablePlaceholders = false
                ),
                pagingSourceFactory = { WorldArticlesPagingSource(service, page.slug) }
        ).flowable

    override fun getPost(slug: String) = service.getPost(slug).map { it.posts.first().toArticle() }
}

// Mappers

private fun WorldPageResponse.toPage() = WorldPage(title, "hash-$slug")

fun WorldPostResponse.toArticle() =
    WorldArticle(
        id,
        title,
        slug,
        html,
        feature_image,
        custom_excerpt?.takeIf(String::isNotBlank) ?: excerpt,
        published_at,
        tags?.firstOrNull { it.visibility != "internal" }?.name
    )
