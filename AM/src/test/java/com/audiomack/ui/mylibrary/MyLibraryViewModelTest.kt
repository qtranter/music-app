package com.audiomack.ui.mylibrary

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.data.support.ZendeskUnreadTicketsData
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.home.NavigationActions
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class MyLibraryViewModelTest {

    private val showBackButton = false

    @Mock
    private lateinit var userRepository: UserDataSource

    @Mock
    private lateinit var zendeskRepository: ZendeskDataSource

    @Mock
    private lateinit var navigation: NavigationActions

    private lateinit var schedulers: SchedulersProvider

    private lateinit var viewModel: MyLibraryViewModel

    @Mock
    private lateinit var viewStateObserver: Observer<MyLibraryViewModel.ViewState>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val artist = mock<AMArtist>()
        schedulers = TestSchedulersProvider()
        whenever(userRepository.getUserAsync()).thenReturn(Observable.error(Exception("User not found")))
        whenever(userRepository.refreshUserData()).thenReturn(Observable.just(artist))
        whenever(zendeskRepository.getUnreadTicketsCount()).thenReturn(Observable.just(ZendeskUnreadTicketsData(7, false)))
        viewModel = MyLibraryViewModel(
            showBackButton,
            userRepository,
            zendeskRepository,
            navigation,
            schedulers
        ).apply {
            viewState.observeForever(viewStateObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `view state updates`() {
        verify(viewStateObserver, times(1)).onChanged(argWhere { it.ticketsBadgeVisible })
    }

    @Test
    fun `search click observed`() {
        viewModel.onSearchClick()
        verify(navigation).launchMyLibrarySearchEvent()
    }

    @Test
    fun `notifications click observed`() {
        viewModel.onNotificationsClick()
        verify(navigation).launchNotificationsEvent()
    }

    @Test
    fun `settings click observed`() {
        viewModel.onSettingsClick()
        verify(navigation).launchSettingsEvent()
    }

    @Test
    fun `back click observed`() {
        viewModel.onBackClick()
        verify(navigation).navigateBack()
    }
}
