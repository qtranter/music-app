package com.audiomack.playback

import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.user.UserData
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.Observable

interface MusicServiceUseCase {
    fun isFavorite(item: AMResultItem): Boolean

    fun toggleFavorite(
        music: AMResultItem,
        mixpanelButton: String
    ): Observable<ToggleFavoriteResult>

    fun toggleRepost(
        music: AMResultItem,
        mixpanelButton: String
    ): Observable<ToggleRepostResult>
}

class MusicServiceUseCaseImpl(
    private val userData: UserData = UserData,
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : MusicServiceUseCase {

    override fun isFavorite(item: AMResultItem) = userData.isItemFavorited(item)

    override fun toggleFavorite(
        music: AMResultItem,
        mixpanelButton: String
    ): Observable<ToggleFavoriteResult> {
        val mixpanelSource = music.mixpanelSource ?: MixpanelSource.empty
        return actionsDataSource.toggleFavorite(music, mixpanelButton, mixpanelSource)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
    }

    override fun toggleRepost(
        music: AMResultItem,
        mixpanelButton: String
    ): Observable<ToggleRepostResult> {
        val mixpanelSource = music.mixpanelSource ?: MixpanelSource.empty
        return actionsDataSource.toggleRepost(music, mixpanelButton, mixpanelSource)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
    }
}
