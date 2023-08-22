package com.audiomack.data.housekeeping

import android.content.Context
import com.audiomack.model.AMResultItem
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.Completable
import io.reactivex.Observable

interface HousekeepingUseCase {
    val downloadsToRestore: Observable<List<AMResultItem>>
    fun runHousekeeping(): Completable
    fun clearDownloadsToRestore(): Completable
}

class HousekeepingUseCaseImpl(
    private val context: Context,
    private val housekeepingDataSource: HousekeepingDataSource = HousekeepingRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : HousekeepingUseCase {

    override val downloadsToRestore = housekeepingDataSource.downloadsToRestore

    override fun runHousekeeping(): Completable {
        return Completable.mergeDelayError(
            listOf(
                housekeepingDataSource.createNoMediaFiles(context),
                housekeepingDataSource.houseekping
            )
        )
            .onErrorComplete()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
    }

    override fun clearDownloadsToRestore() = housekeepingDataSource.clearRestoredDatabase()
}
