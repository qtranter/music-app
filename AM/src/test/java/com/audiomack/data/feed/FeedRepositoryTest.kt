package com.audiomack.data.feed

import com.audiomack.model.AMResultItem
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.network.APIInterface
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class FeedRepositoryTest {

    @Mock
    private lateinit var api: APIInterface.FeedInterface

    private lateinit var repository: FeedRepository

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        repository = FeedRepository(api)
    }

    @After
    fun tearDown() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun getSuggestedAccounts() {
        val page = 1
        val items = listOf<AMResultItem>(mock(), mock())
        whenever(api.getMyFeed(page, excludeReups = false, ignoreGeorestrictedMusic = false)).thenReturn(
            APIRequestData(Observable.just(APIResponseData(items, null)), null)
        )
        repository.getMyFeed(page, excludeReups = false, ignoreGeorestrictedMusic = false)
        verify(api, times(1)).getMyFeed(eq(page), eq(false), eq(false))
    }
}
