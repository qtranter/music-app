package com.audiomack.usecases.favorite

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.model.LoginSignupSource
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test

class FavoriteEventsManagerTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val favoriteEventsManager = FavoriteEventsManager

    @Test
    fun favoriteTest() {
        val result = mock<ToggleFavoriteResult.Notify>()
        val observer: Observer<ToggleFavoriteResult.Notify> = mock()
        favoriteEventsManager.favoriteEvent.observeForever(observer)
        favoriteEventsManager.favorite(result)
        verify(observer, atLeastOnce()).onChanged(result)
    }

    @Test
    fun loginRequiredTest() {
        val source = mock<LoginSignupSource>()
        val observer: Observer<LoginSignupSource> = mock()
        favoriteEventsManager.loginRequiredEvent.observeForever(observer)
        favoriteEventsManager.loginRequired(source)
        verify(observer, atLeastOnce()).onChanged(source)
    }

    @Test
    fun offlineTest() {
        val observer: Observer<Unit> = mock()
        favoriteEventsManager.notifyOfflineEvent.observeForever(observer)
        favoriteEventsManager.offline()
        verify(observer, atLeastOnce()).onChanged(null)
    }

    @Test
    fun errorTest() {
        val error = Throwable()
        val observer: Observer<Throwable> = mock()
        favoriteEventsManager.errorEvent.observeForever(observer)
        favoriteEventsManager.error(error)
        verify(observer, atLeastOnce()).onChanged(error)
    }
}
