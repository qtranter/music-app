package com.audiomack.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.data.accounts.AccountsDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.APIResponseData
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class FetchSuggestedAccountsUseCaseTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var userRepository: UserDataSource
    @Mock private lateinit var accountsRepository: AccountsDataSource

    private lateinit var sut: FetchSuggestedAccountsUseCaseImpl

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = FetchSuggestedAccountsUseCaseImpl(userRepository, accountsRepository)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `fetch editorial picks if user is logged out`() {
        val page = 0
        val editorial = listOf<AMArtist>(mock(), mock(), mock())
        whenever(userRepository.isLoggedInAsync()).thenReturn(Single.just(false))
        whenever(accountsRepository.getEditorialPickedArtists(page)).thenReturn(Observable.just(APIResponseData(editorial, null)))
        sut(page)
            .test()
            .assertValue(editorial)
            .assertNoErrors()
        verify(userRepository).isLoggedInAsync()
        verify(accountsRepository).getEditorialPickedArtists(page)
        verify(accountsRepository, never()).getRecsysArtists()
    }

    @Test
    fun `fetch editorial picks if there's a failure in retrieving login status`() {
        val page = 0
        val editorial = listOf<AMArtist>(mock(), mock(), mock())
        whenever(userRepository.isLoggedInAsync()).thenReturn(Single.error(Exception()))
        whenever(accountsRepository.getEditorialPickedArtists(page)).thenReturn(Observable.just(APIResponseData(editorial, null)))
        sut(page)
            .test()
            .assertValue(editorial)
            .assertNoErrors()
        verify(userRepository).isLoggedInAsync()
        verify(accountsRepository).getEditorialPickedArtists(page)
        verify(accountsRepository, never()).getRecsysArtists()
    }

    @Test
    fun `fetch recommended accounts if user is logged in and merge with editorial picks, first page`() {
        val page = 0
        val recommended = listOf<AMArtist>(mock(), mock(), mock())
        val editorial = listOf<AMArtist>(mock(), mock(), mock())
        whenever(userRepository.isLoggedInAsync()).thenReturn(Single.just(true))
        whenever(accountsRepository.getRecsysArtists()).thenReturn(Observable.just(APIResponseData(recommended, null)))
        whenever(accountsRepository.getEditorialPickedArtists(0)).thenReturn(Observable.just(APIResponseData(editorial, null)))
        sut(page)
            .test()
            .assertValue(recommended + editorial)
            .assertNoErrors()
        verify(userRepository).isLoggedInAsync()
        verify(accountsRepository).getEditorialPickedArtists(page)
        verify(accountsRepository).getRecsysArtists()
    }

    @Test
    fun `fetch editorial picks only if user is logged in and it has been requested a page greater than 0`() {
        val page = 1
        val editorial = listOf<AMArtist>(mock(), mock(), mock())
        whenever(userRepository.isLoggedInAsync()).thenReturn(Single.just(true))
        whenever(accountsRepository.getEditorialPickedArtists(page)).thenReturn(Observable.just(APIResponseData(editorial, null)))
        sut(page)
            .test()
            .assertValue(editorial)
            .assertNoErrors()
        verify(userRepository).isLoggedInAsync()
        verify(accountsRepository).getEditorialPickedArtists(page)
        verify(accountsRepository, never()).getRecsysArtists()
    }
}
