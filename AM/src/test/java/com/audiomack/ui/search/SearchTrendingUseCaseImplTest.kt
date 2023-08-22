package com.audiomack.ui.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.data.featured.FeaturedSpotDataSource
import com.audiomack.data.search.SearchDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMFeaturedSpot
import com.audiomack.model.AMResultItem
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class SearchTrendingUseCaseImplTest {

    private lateinit var useCase: SearchTrendingUseCaseImpl

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var searchDataSource: SearchDataSource

    @Mock
    private lateinit var featuredSpotDataSource: FeaturedSpotDataSource

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase = SearchTrendingUseCaseImpl(
            userDataSource,
            searchDataSource,
            featuredSpotDataSource
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `return featured spots if not logged in`() {
        val mockMusic = mock<AMResultItem> {
            on { itemId } doReturn "1"
        }
        val featuredSpots = listOf(AMFeaturedSpot.fromMusic(mockMusic))
        whenever(userDataSource.isLoggedInAsync()).thenReturn(Single.just(false))
        whenever(featuredSpotDataSource.get()).thenReturn(Single.just(featuredSpots))
        useCase.getTrendingSearches()
            .test()
            .assertValue(featuredSpots)
            .assertNoErrors()
        verify(userDataSource, times(1)).isLoggedInAsync()
        verify(featuredSpotDataSource, times(1)).get()
        verify(searchDataSource, times(0)).getRecommendations()
    }

    @Test
    fun `return recommended spots if logged in`() {
        val mockMusic = mock<AMResultItem> {
            on { itemId } doReturn "1"
        }
        val recommendedMusic = listOf(mockMusic)
        whenever(userDataSource.isLoggedInAsync()).thenReturn(Single.just(true))
        whenever(searchDataSource.getRecommendations()).thenReturn(Single.just(recommendedMusic))
        useCase.getTrendingSearches()
            .test()
            .assertValue(recommendedMusic.map { AMFeaturedSpot.fromMusic(it) })
            .assertNoErrors()
        verify(userDataSource, times(1)).isLoggedInAsync()
        verify(featuredSpotDataSource, times(0)).get()
        verify(searchDataSource, times(1)).getRecommendations()
    }

    @Test
    fun `return featured spots if logged in and there are no recommended songs`() {
        val mockMusic = mock<AMResultItem> {
            on { itemId } doReturn "1"
        }
        val recommendedMusic = emptyList<AMResultItem>()
        val featuredSpots = listOf(AMFeaturedSpot.fromMusic(mockMusic))
        whenever(userDataSource.isLoggedInAsync()).thenReturn(Single.just(true))
        whenever(searchDataSource.getRecommendations()).thenReturn(Single.just(recommendedMusic))
        whenever(featuredSpotDataSource.get()).thenReturn(Single.just(featuredSpots))
        useCase.getTrendingSearches()
            .test()
            .assertValue(featuredSpots)
            .assertNoErrors()
        verify(userDataSource, times(1)).isLoggedInAsync()
        verify(featuredSpotDataSource, times(1)).get()
        verify(searchDataSource, times(1)).getRecommendations()
    }
}
