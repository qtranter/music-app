package com.audiomack.ui.feed

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.EventLoginState
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.feed.suggested.SuggestedAccountsViewModel
import com.audiomack.usecases.FetchSuggestedAccountsUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.atMost
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class SuggestedAccountsViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val userDataSource: UserDataSource = mock()
    private val actionsDataSource: ActionsDataSource = mock()
    private val fetchSuggestedAccountsUseCase: FetchSuggestedAccountsUseCase = mock()
    private val adsDataSource: AdsDataSource = mock()

    private val suggestedAccountsObserver: Observer<List<AMArtist>> = mock()
    private lateinit var loginStateChangeSubject: Subject<EventLoginState>

    private lateinit var schedulersProvider: SchedulersProvider
    private lateinit var viewModel: SuggestedAccountsViewModel
    private lateinit var premiumObservable: Subject<Boolean>

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        schedulersProvider = TestSchedulersProvider()
        premiumObservable = PublishSubject.create()
        whenever(fetchSuggestedAccountsUseCase(any())).thenReturn(Single.just(emptyList()))

        loginStateChangeSubject = BehaviorSubject.create()
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)

        viewModel = SuggestedAccountsViewModel(
            userDataSource,
            schedulersProvider,
            actionsDataSource,
            fetchSuggestedAccountsUseCase,
            adsDataSource
        ).apply {
            suggestedAccounts.observeForever(suggestedAccountsObserver)
        }
    }

    @After
    fun tearUp() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun loadMoreTest() {
        viewModel.loadMore()
        val suggestedAccountsObserver: Observer<List<AMArtist>> = mock()
        viewModel.suggestedAccounts.observeForever(suggestedAccountsObserver)
        verify(suggestedAccountsObserver, atLeastOnce()).onChanged(any())
    }

    @Test
    fun hasMoreTest() {
        val suggestedAccountsObserver: Observer<List<AMArtist>> = mock()
        viewModel.suggestedAccounts.observeForever(suggestedAccountsObserver)
        whenever(fetchSuggestedAccountsUseCase(any())).thenReturn(
            Single.just(listOf(mock(), mock()))
        )
        viewModel.loadMore()
        Assert.assertTrue(viewModel.hasMoreItems)
        verify(suggestedAccountsObserver, atLeastOnce()).onChanged(any())
    }

    @Test
    fun hasNotMoreTest() {
        val suggestedAccountsObserver: Observer<List<AMArtist>> = mock()
        viewModel.suggestedAccounts.observeForever(suggestedAccountsObserver)
        whenever(fetchSuggestedAccountsUseCase(any())).thenReturn(Single.just(emptyList()))
        viewModel.loadMore()
        Assert.assertFalse(viewModel.hasMoreItems)
        verify(suggestedAccountsObserver, atLeastOnce()).onChanged(any())
    }

    @Test
    fun `toggle follow, logged in`() {
        val suggestedAccountsObserver: Observer<List<AMArtist>> = mock()
        whenever(actionsDataSource.toggleFollow(anyOrNull(), any(), any(), any()))
            .thenReturn(Observable.just(ToggleFollowResult.Finished(true)))
        viewModel.suggestedAccounts.observeForever(suggestedAccountsObserver)
        viewModel.onFollowTapped(mock())
        verify(suggestedAccountsObserver, atLeastOnce()).onChanged(any())
    }

    @Test
    fun `toggle follow, ask for permissions`() {
        val redirect = PermissionRedirect.Settings
        val suggestedAccountsObserver: Observer<List<AMArtist>> = mock()
        whenever(actionsDataSource.toggleFollow(anyOrNull(), any(), any(), any())).thenReturn(
            Observable.just(ToggleFollowResult.AskForPermission(redirect))
        )
        val promptNotificationPermissionEventObserver: Observer<PermissionRedirect> = mock()
        viewModel.promptNotificationPermissionEvent.observeForever(
            promptNotificationPermissionEventObserver
        )
        viewModel.suggestedAccounts.observeForever(suggestedAccountsObserver)
        viewModel.onFollowTapped(mock())
        verify(promptNotificationPermissionEventObserver).onChanged(redirect)
        verify(suggestedAccountsObserver, atMost(1)).onChanged(any())
    }

    @Test
    fun `toggle follow, offline`() {
        whenever(actionsDataSource.toggleFollow(anyOrNull(), any(), any(), any()))
            .thenReturn(Observable.error(ToggleFollowException.Offline))
        val suggestedAccountsObserver: Observer<List<AMArtist>> = mock()
        val observerOfflineAlert: Observer<Unit> = mock()
        viewModel.suggestedAccounts.observeForever(suggestedAccountsObserver)
        viewModel.offlineAlertEvent.observeForever(observerOfflineAlert)
        viewModel.onFollowTapped(mock())
        verify(suggestedAccountsObserver, atMost(1)).onChanged(any())
        verify(observerOfflineAlert).onChanged(null)
    }
}
