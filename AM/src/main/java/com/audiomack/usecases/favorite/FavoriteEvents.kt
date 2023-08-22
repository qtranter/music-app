package com.audiomack.usecases.favorite

import androidx.lifecycle.LiveData
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.model.LoginSignupSource
import com.audiomack.utils.SingleLiveEvent

interface FavoriteTriggers {
    fun favorite(result: ToggleFavoriteResult.Notify)
    fun loginRequired(source: LoginSignupSource)
    fun offline()
    fun error(throwable: Throwable)
}

interface FavoriteEvents {
    val favoriteEvent: LiveData<ToggleFavoriteResult.Notify>
    val loginRequiredEvent: LiveData<LoginSignupSource>
    val notifyOfflineEvent: LiveData<Unit>
    val errorEvent: LiveData<Throwable>
}

object FavoriteEventsManager : FavoriteTriggers, FavoriteEvents {
    override val favoriteEvent = SingleLiveEvent<ToggleFavoriteResult.Notify>()
    override val loginRequiredEvent = SingleLiveEvent<LoginSignupSource>()
    override val notifyOfflineEvent = SingleLiveEvent<Unit>()
    override val errorEvent = SingleLiveEvent<Throwable>()

    override fun favorite(result: ToggleFavoriteResult.Notify) = favoriteEvent.postValue(result)
    override fun loginRequired(source: LoginSignupSource) = loginRequiredEvent.postValue(source)
    override fun offline() = notifyOfflineEvent.call()
    override fun error(throwable: Throwable) = errorEvent.postValue(throwable)
}
