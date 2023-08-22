package com.audiomack.ui.feed

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.api.ArtistsDataSource
import com.audiomack.data.feed.FeedDataSource
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.usecases.FetchSuggestedAccountsUseCase
import com.audiomack.usecases.download.DownloadEventsManager
import com.audiomack.usecases.download.DownloadUseCase
import com.audiomack.usecases.favorite.FavoriteEventsManager
import com.audiomack.usecases.favorite.FavoriteUseCase
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
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class FeedViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val userDataSource: UserDataSource = mock()
    private val actionsDataSource: ActionsDataSource = mock()
    private val fetchSuggestedAccountsUseCase: FetchSuggestedAccountsUseCase = mock()
    private val feedDataSource: FeedDataSource = mock()
    private val preferencesDataSource: PreferencesDataSource = mock()
    private val artistDataSource: ArtistsDataSource = mock()
    private val adsDataSource: AdsDataSource = mock()
    private val queueDataSource: QueueDataSource = mock()
    private val downloadUseCase: DownloadUseCase = mock()
    private val downloadEventsManager: DownloadEventsManager = mock()
    private val favoriteUseCase: FavoriteUseCase = mock()
    private val favoriteEventsManager: FavoriteEventsManager = mock()
    private val eventBus: EventBus = mock()

    private val suggestedAccountsObserver: Observer<List<AMArtist>> = mock()
    private val feedItemsObserver: Observer<List<AMResultItem>> = mock()
    private val loggedOutAlertEventObserver: Observer<LoginSignupSource> = mock()

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>

    private lateinit var schedulersProvider: SchedulersProvider
    private lateinit var viewModel: FeedViewModel
    private lateinit var premiumObservable: Subject<Boolean>

    @Before
    fun setUp() {
        schedulersProvider = TestSchedulersProvider()
        premiumObservable = PublishSubject.create()
        whenever(fetchSuggestedAccountsUseCase(any())).thenReturn(Single.just(emptyList()))

        whenever(feedDataSource.getMyFeed(any(), any(), any())).thenReturn(
            APIRequestData(Observable.just(APIResponseData()), null)
        )

        loginStateChangeSubject = BehaviorSubject.create()
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)

        whenever(userDataSource.artistFollowEvents).thenReturn(Observable.empty())

        viewModel = FeedViewModel(
            userDataSource,
            schedulersProvider,
            actionsDataSource,
            fetchSuggestedAccountsUseCase,
            feedDataSource,
            preferencesDataSource,
            artistDataSource,
            adsDataSource,
            queueDataSource,
            downloadUseCase,
            downloadEventsManager,
            favoriteEventsManager,
            favoriteUseCase,
            eventBus
        ).apply {
            suggestedAccounts.observeForever(suggestedAccountsObserver)
            feedItems.observeForever(feedItemsObserver)
            loggedOutAlertEvent.observeForever(loggedOutAlertEventObserver)
        }
    }

    @After
    fun tearUp() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun loadMoreSuggestedAccountsTest() {
        viewModel.loadMoreSuggestedAccounts()
        verify(suggestedAccountsObserver, atLeastOnce()).onChanged(any())
    }

    @Test
    fun hasMoreSuggestedAccountsTest() {
        whenever(fetchSuggestedAccountsUseCase(any())).thenReturn(
            Single.just(listOf(mock(), mock()))
        )
        viewModel.loadMoreSuggestedAccounts()
        assertTrue(viewModel.hasMoreSuggestedAccounts)
        verify(suggestedAccountsObserver, atLeastOnce()).onChanged(any())
    }

    @Test
    fun hasNotMoreSuggestedAccountsTest() {
        whenever(fetchSuggestedAccountsUseCase(any())).thenReturn(Single.just(emptyList()))
        viewModel.loadMoreSuggestedAccounts()
        assertFalse(viewModel.hasMoreSuggestedAccounts)
        verify(suggestedAccountsObserver, atLeastOnce()).onChanged(any())
    }

    @Test
    fun loadMoreFeedItemsTest() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        viewModel.loadMoreFeedItems()
        verify(feedItemsObserver, atLeastOnce()).onChanged(any())
    }

    @Test
    fun feedPlaceHolderEventTest() {
        val placeHolderObserver: Observer<Boolean> = mock()
        viewModel.feedPlaceHolderVisibilityEvent.observeForever(placeHolderObserver)
        whenever(userDataSource.isLoggedIn()).thenReturn(false)
        viewModel.loadMoreFeedItems()
        verify(placeHolderObserver, atLeastOnce()).onChanged(true)

        whenever(userDataSource.isLoggedIn()).thenReturn(true)

        viewModel.loadMoreFeedItems()
        verify(placeHolderObserver, atLeastOnce()).onChanged(true)
    }

    @Test
    fun `exclude ReUps off test`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(preferencesDataSource.excludeReUps).thenReturn(true)
        viewModel.excludeReUps = false
        whenever(preferencesDataSource.excludeReUps).thenReturn(false)
        assertFalse(viewModel.excludeReUps)
        verify(feedItemsObserver, atLeastOnce()).onChanged(any())
        assertEquals(1, viewModel.currentFeedPage)
    }

    @Test
    fun `exclude ReUps on test`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(preferencesDataSource.excludeReUps).thenReturn(false)
        viewModel.excludeReUps = true
        whenever(preferencesDataSource.excludeReUps).thenReturn(true)
        assertTrue(viewModel.excludeReUps)
        verify(feedItemsObserver, atLeastOnce()).onChanged(any())
        assertEquals(1, viewModel.currentFeedPage)
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

    // Download
    @Test
    fun onClickDownloadTest() {
        viewModel.onClickDownload(mock())
        verify(downloadUseCase, atLeastOnce()).invoke(any(), any(), any(), anyOrNull())
    }

    @Test
    fun onPlaylistConfirmedTest() {
        viewModel.onPlaylistSyncConfirmed(mock())
        verify(downloadUseCase, atLeastOnce()).invoke(any(), any(), any(), anyOrNull())
    }

    @Test
    fun reloadFeedTest() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        val reloadFeedEventObserver: Observer<Unit> = mock()
        viewModel.reloadFeedEvent.observeForever(reloadFeedEventObserver)
        viewModel.reloadFeed()
        verify(reloadFeedEventObserver, atLeastOnce()).onChanged(Unit)
        verify(feedItemsObserver, atLeastOnce()).onChanged(any())
    }
}
