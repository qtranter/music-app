package com.audiomack.ui.player.maxi.uploader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.model.ArtistWithBadge
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.common.Resource
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.atMost
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyBoolean
import org.mockito.MockitoAnnotations

class PlayerUploaderViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var playerDataSource: PlayerDataSource

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var actionsDataSource: ActionsDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: PlayerUploaderViewModel

    private val mixpanelSource = MixpanelSource("", "")

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() {
        val aMResultItem = mock<AMResultItem> {
            on { itemId } doReturn "2"
            on { tags } doReturn arrayOf("rap", "hip-hop")
            on { genre } doReturn "rap"
        }

        MockitoAnnotations.initMocks(this)
        whenever(playerDataSource.subscribeToSong(any())).then {
            (it.arguments.first() as io.reactivex.Observer<Resource<AMResultItem>>).onNext(
                Resource.Success(aMResultItem)
            )
        }
        loginStateChangeSubject = BehaviorSubject.create()
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)
        schedulersProvider = TestSchedulersProvider()
        viewModel = PlayerUploaderViewModel(
            playerDataSource,
            userDataSource,
            actionsDataSource,
            schedulersProvider
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `on song changed`() {
        val observerName: Observer<ArtistWithBadge> = mock()
        val observerFollowers: Observer<String> = mock()
        val observerAvatar: Observer<String?> = mock()
        val observerFollowStatus: Observer<Boolean> = mock()
        val observerFollowVisible: Observer<Boolean> = mock()

        viewModel.name.observeForever(observerName)
        viewModel.followers.observeForever(observerFollowers)
        viewModel.avatar.observeForever(observerAvatar)
        viewModel.followStatus.observeForever(observerFollowStatus)
        viewModel.followVisible.observeForever(observerFollowVisible)

        verify(observerName).onChanged(anyOrNull())
        verify(observerFollowers).onChanged(anyOrNull())
        verify(observerAvatar).onChanged(anyOrNull())
        verify(observerFollowStatus).onChanged(any())
        verify(observerFollowVisible).onChanged(any())
    }

    @Test
    fun `toggle follow, logged in`() {
        val result = true
        val observerFollowStatus: Observer<Boolean> = mock()
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.just(ToggleFollowResult.Finished(true)))
        viewModel.followStatus.observeForever(observerFollowStatus)
        viewModel.onFollowTapped(mixpanelSource)
        verify(observerFollowStatus, atLeast(1)).onChanged(result)
    }

    @Test
    fun `toggle follow, ask for permissions`() {
        val redirect = PermissionRedirect.Settings
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.just(ToggleFollowResult.AskForPermission(redirect)))
        val promptNotificationPermissionEventObserver: Observer<PermissionRedirect> = mock()
        viewModel.promptNotificationPermissionEvent.observeForever(promptNotificationPermissionEventObserver)
        viewModel.onFollowTapped(mixpanelSource)
        verify(promptNotificationPermissionEventObserver).onChanged(redirect)
    }

    @Test
    fun `toggle follow, offline`() {
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.error(ToggleFollowException.Offline))
        val observerFollowStatus: Observer<Boolean> = mock()
        val observerOfflineAlert: Observer<Void> = mock()
        viewModel.followStatus.observeForever(observerFollowStatus)
        viewModel.offlineAlert.observeForever(observerOfflineAlert)
        viewModel.onFollowTapped(mixpanelSource)
        verify(observerFollowStatus, times(1)).onChanged(ArgumentMatchers.anyBoolean())
        verify(observerOfflineAlert).onChanged(null)
    }

    @Test
    fun `toggle follow, logged out`() {
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.error(ToggleFollowException.LoggedOut))
        val observerFollowStatus: Observer<Boolean> = mock()
        val observerLoggedOutAlert: Observer<LoginSignupSource> = mock()
        viewModel.followStatus.observeForever(observerFollowStatus)
        viewModel.loggedOutAlert.observeForever(observerLoggedOutAlert)
        viewModel.onFollowTapped(mixpanelSource)
        verify(observerFollowStatus, atMost(1)).onChanged(anyBoolean())
        verify(observerLoggedOutAlert).onChanged(eq(LoginSignupSource.AccountFollow))

        val result = true
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.just(ToggleFollowResult.Finished(true)))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)
        verify(observerFollowStatus, atLeast(1)).onChanged(result)
    }

    @Test
    fun `on uploader tapped`() {
        val observerUploaderTapped: Observer<String> = mock()
        viewModel.openInternalUrlEvent.observeForever(observerUploaderTapped)
        viewModel.onUploaderTapped()
        verify(observerUploaderTapped).onChanged(anyOrNull())
    }

    @Test
    fun `player datasource subscription`() {
        verify(playerDataSource).subscribeToSong(any())
    }

    // Tags
    @Test
    fun `genre clicked`() {
        val genreEventObserver: Observer<String> = mock()
        viewModel.genreEvent.observeForever(genreEventObserver)
        viewModel.onTagClicked("rap")
        verify(genreEventObserver, atLeast(1)).onChanged("rap")
    }

    @Test
    fun `tag clicked`() {
        val tagEventObserver: Observer<String> = mock()
        viewModel.tagEvent.observeForever(tagEventObserver)
        viewModel.onTagClicked("hip-hop")
        verify(tagEventObserver, atLeast(1)).onChanged("tag:hip-hop")
    }
}
