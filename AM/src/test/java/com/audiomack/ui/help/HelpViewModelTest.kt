package com.audiomack.ui.help

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.data.support.ZendeskUnreadTicketsData
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@Suppress("UNCHECKED_CAST")
class HelpViewModelTest {

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var zendeskDataSource: ZendeskDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: HelpViewModel

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        loginStateChangeSubject = BehaviorSubject.create()
        `when`(zendeskDataSource.getUnreadTicketsCount()).thenReturn(Observable.just(ZendeskUnreadTicketsData(0, false)))
        `when`(zendeskDataSource.getUIConfigs()).thenReturn(emptyList())
        `when`(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)
        schedulersProvider = TestSchedulersProvider()
        viewModel = HelpViewModel(zendeskDataSource, userDataSource, schedulersProvider)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observer: Observer<Void> = mock()
        viewModel.close.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `knowledge base when logged in`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        val observer: Observer<Void> = mock()
        viewModel.showKnowledgeBase.observeForever(observer)
        viewModel.onKnowledgeBaseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `knowledge base when logged out`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)
        val observer: Observer<Void> = mock()
        viewModel.showKnowledgeBase.observeForever(observer)
        viewModel.onKnowledgeBaseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `tickets when logged in`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        val observer: Observer<Void> = mock()
        viewModel.showTickets.observeForever(observer)
        viewModel.onTicketsTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `tickets when logged out`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)

        val showLoginAlertObserver: Observer<Void> = mock()
        viewModel.showLoginAlert.observeForever(showLoginAlertObserver)

        val showLoginObserver: Observer<LoginSignupSource> = mock()
        viewModel.showLogin.observeForever(showLoginObserver)

        val showTicketsObserver: Observer<Void> = mock()
        viewModel.showTickets.observeForever(showTicketsObserver)

        viewModel.onTicketsTapped()
        verify(showLoginAlertObserver).onChanged(null)

        viewModel.onStartLoginTapped()
        verify(showLoginObserver).onChanged(any())

        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)
        verify(showTicketsObserver).onChanged(null)
    }

    @Test
    fun `tickets when logged out, cancel login flow`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)

        val showLoginAlertObserver: Observer<Void> = mock()
        viewModel.showLoginAlert.observeForever(showLoginAlertObserver)

        val showLoginObserver: Observer<LoginSignupSource> = mock()
        viewModel.showLogin.observeForever(showLoginObserver)

        val showTicketsObserver: Observer<Void> = mock()
        viewModel.showTickets.observeForever(showTicketsObserver)

        viewModel.onTicketsTapped()
        verify(showLoginAlertObserver).onChanged(null)

        viewModel.onStartLoginTapped()
        verify(showLoginObserver).onChanged(any())

        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)
        verifyNoMoreInteractions(showTicketsObserver)
    }

    @Test
    fun `ticket count update`() {
        val count = 1
        `when`(zendeskDataSource.getUnreadTicketsCount()).thenReturn(Observable.just(ZendeskUnreadTicketsData(count, false)))
        `when`(zendeskDataSource.getUIConfigs()).thenReturn(emptyList())
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        val observer: Observer<Int> = mock()
        viewModel.unreadTicketsCount.observeForever(observer)
        viewModel.onUnreadTicketsCountRequested()
        verify(observer).onChanged(count)
    }

    @Test
    fun `show unread alert`() {
        `when`(zendeskDataSource.getUnreadTicketsCount()).thenReturn(Observable.just(ZendeskUnreadTicketsData(1, true)))
        `when`(zendeskDataSource.getUIConfigs()).thenReturn(emptyList())
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        val observer: Observer<Void> = mock()
        viewModel.showUnreadAlert.observeForever(observer)
        viewModel.onUnreadTicketsCountRequested()
        verify(observer).onChanged(null)
    }
}
