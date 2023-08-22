package com.audiomack.data.world

import com.audiomack.network.retrofitApi.WorldPostService
import com.audiomack.network.retrofitModel.WorldPageResponse
import com.audiomack.network.retrofitModel.WorldPagesResponse
import com.audiomack.network.retrofitModel.WorldPostResponse
import com.audiomack.network.retrofitModel.WorldPostsResponse
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class WorldRepositoryTest {

    @Mock private lateinit var api: WorldPostService

    private lateinit var sut: WorldRepository

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        sut = WorldRepository(api)
    }

    @After
    fun tearDown() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `get pages`() {
        val pages = listOf(WorldPageResponse("Videos", "videos"))
        whenever(api.getPages()).thenReturn(Single.just(WorldPagesResponse(pages)))

        sut.getPages()
            .test()
            .assertValue { it.size == 1 && it[0].slug == "hash-${pages[0].slug}" && it[0].title == pages[0].title }
            .assertNoErrors()

        verify(api).getPages()
    }

    @Test
    fun `get article`() {
        val articleSlug = "article-slug"
        val post = mock<WorldPostResponse> {
            on { slug } doReturn articleSlug
        }
        whenever(api.getPost(articleSlug)).thenReturn(Single.just(WorldPostsResponse(listOf(post))))

        sut.getPost(articleSlug)
            .test()
            .assertValue { it.slug == articleSlug }
            .assertNoErrors()

        verify(api).getPost(eq(articleSlug), any(), any(), any())
    }
}
