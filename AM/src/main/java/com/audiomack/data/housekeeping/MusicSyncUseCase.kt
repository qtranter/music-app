package com.audiomack.data.housekeeping

import com.audiomack.data.api.ArtistsDataSource
import com.audiomack.data.api.ArtistsRepository
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.Completable

interface MusicSyncUseCase {
    fun syncMusic(): Completable?
}

class MusicSyncUseCaseImpl(
    private val housekeepingDataSource: HousekeepingDataSource = HousekeepingRepository(),
    private val artistsDataSource: ArtistsDataSource = ArtistsRepository(),
    private val remoteVariablesProvider: RemoteVariablesProvider = RemoteVariablesProviderImpl(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : MusicSyncUseCase {

    override fun syncMusic(): Completable? {
        return if (remoteVariablesProvider.syncCheckEnabled) {
            artistsDataSource.findLoggedArtist()
                .subscribeOn(schedulersProvider.io)
                .concatMapCompletable { housekeepingDataSource.syncMusic }
                .onErrorComplete()
                .observeOn(schedulersProvider.main)
        } else null
    }
}
