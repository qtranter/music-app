package com.audiomack.usecases.favorite

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.model.AMResultItem
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class FavoriteUseCaseTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val actionDataSource: ActionsDataSource = mock()
    private val favoriteEventsManager: FavoriteEventsManager = mock()
    private val mixpanelSource: MixpanelSource = mock()
    private val compositeDisposable: CompositeDisposable = mock()

    private lateinit var schedulers: SchedulersProvider
    private lateinit var favoriteUseCase: FavoriteUseCaseImpl

    @Before
    fun setup() {
        schedulers = TestSchedulersProvider()

        favoriteUseCase = FavoriteUseCaseImpl(
            actionDataSource,
            schedulers,
            favoriteEventsManager
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun favoriteTest() {
        val item = mock<AMResultItem>()
        val result = mock<ToggleFavoriteResult.Notify>()
        whenever(actionDataSource.toggleFavorite(item, MixpanelButtonList, mixpanelSource))
            .thenReturn(Observable.just(result))

        favoriteUseCase(item, mixpanelSource, compositeDisposable, null)
        verify(favoriteEventsManager, times(1)).favorite(result)
    }

    @Test
    fun loggedOutExceptionTest() {
        val item = mock<AMResultItem>()
        whenever(actionDataSource.toggleFavorite(item, MixpanelButtonList, mixpanelSource))
            .thenReturn(Observable.error(ToggleFavoriteException.LoggedOut))

        favoriteUseCase(item, mixpanelSource, compositeDisposable, null)
        verify(favoriteEventsManager, times(1)).loginRequired(LoginSignupSource.Favorite)
    }

    @Test
    fun offlineExceptionTest() {
        val item = mock<AMResultItem>()
        whenever(actionDataSource.toggleFavorite(item, MixpanelButtonList, mixpanelSource))
            .thenReturn(Observable.error(ToggleFavoriteException.Offline))

        favoriteUseCase(item, mixpanelSource, compositeDisposable, null)
        verify(favoriteEventsManager, times(1)).offline()
    }

    @Test
    fun errorExceptionTest() {
        val item = mock<AMResultItem>()
        val throwable = mock<Throwable>()
        whenever(actionDataSource.toggleFavorite(item, MixpanelButtonList, mixpanelSource))
            .thenReturn(Observable.error(throwable))

        favoriteUseCase(item, mixpanelSource, compositeDisposable, null)
        verify(favoriteEventsManager, times(1)).error(any())
    }
}
