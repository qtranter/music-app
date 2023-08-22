package com.audiomack.usecases.favorite

import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.model.AMResultItem
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.utils.addTo
import io.reactivex.disposables.CompositeDisposable

interface FavoriteUseCase {
    operator fun invoke(
        item: AMResultItem,
        mixpanelSource: MixpanelSource,
        compositeDisposable: CompositeDisposable,
        onLoginRequired: (() -> Unit)?
    )
}

class FavoriteUseCaseImpl(
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val schedulers: SchedulersProvider = AMSchedulersProvider(),
    private val favoriteEventsManager: FavoriteEventsManager = FavoriteEventsManager
) : FavoriteUseCase {
    override fun invoke(
        item: AMResultItem,
        mixpanelSource: MixpanelSource,
        compositeDisposable: CompositeDisposable,
        onLoginRequired: (() -> Unit)?
    ) {
        actionsDataSource.toggleFavorite(item, MixpanelButtonList, mixpanelSource)
            .subscribeOn(schedulers.io)
            .observeOn(schedulers.main)
            .subscribe({ result ->
                when (result) {
                    is ToggleFavoriteResult.Notify -> favoriteEventsManager.favorite(result)
                }
            }, { throwable ->
                when (throwable) {
                    is ToggleFavoriteException.LoggedOut -> {
                        onLoginRequired?.invoke()
                        favoriteEventsManager.loginRequired(LoginSignupSource.Favorite)
                    }
                    is ToggleFavoriteException.Offline -> {
                        favoriteEventsManager.offline()
                    }
                    else -> {
                        favoriteEventsManager.error(throwable)
                    }
                }
            }).apply { addTo(compositeDisposable) }
    }
}
