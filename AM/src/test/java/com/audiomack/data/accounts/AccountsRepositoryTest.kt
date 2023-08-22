package com.audiomack.data.accounts

import com.audiomack.model.AMArtist
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

class AccountsRepositoryTest {

    @Mock
    private lateinit var api: APIInterface.AccountsInterface

    private lateinit var repository: AccountsRepository

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        repository = AccountsRepository(api)
    }

    @After
    fun tearDown() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun getSuggestedAccountsTest() {
        val page = 1
        val artists = listOf<AMArtist>(mock(), mock())
        whenever(api.getSuggestedFollows(page)).thenReturn(Observable.just(APIResponseData(artists, null)))
        repository.getEditorialPickedArtists(page)
        verify(api, times(1)).getSuggestedFollows(eq(page))
    }

    @Test
    fun getArtistsRecommendationsTest() {
        val artists = listOf<AMArtist>(mock(), mock())
        whenever(api.getArtistsRecommendations()).thenReturn(Observable.just(APIResponseData(artists, null)))
        repository.getRecsysArtists()
        verify(api, times(1)).getArtistsRecommendations()
    }
}
