package com.audiomack.ui.mylibrary.offline.local

import com.audiomack.model.AMResultItem
import com.audiomack.model.LocalMediaExclusion
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.Observable
import io.reactivex.Single

interface AddLocalMediaExclusionUseCase {

    /**
     * Adds this item to the exclusions list.
     *
     * If this is an album, all of the album tracks will be added, but not the album item, itself.
     *
     * @return List of [LocalMediaExclusion] records created.
     */
    fun addExclusionFrom(item: AMResultItem): Single<List<LocalMediaExclusion>>

    /**
     * Adds all items to the exclusions list.
     *
     * When adding an album, all of the album tracks will be added, but not the album item, itself.
     *
     * @return List of [LocalMediaExclusion] records created.
     */
    fun addExclusionsFrom(items: List<AMResultItem>): Single<List<LocalMediaExclusion>>
}

class AddLocalMediaExclusionUseCaseImpl(
    private val exclusionsRepo: LocalMediaExclusionsDataSource = LocalMediaExclusionsRepository.getInstance(),
    private val schedulers: SchedulersProvider = AMSchedulersProvider()
) : AddLocalMediaExclusionUseCase {

    override fun addExclusionFrom(item: AMResultItem): Single<List<LocalMediaExclusion>> =
        Single.just(item)
            .subscribeOn(schedulers.io)
            .flatMapObservable(::flattenToIds)
            .toList()
            .flatMap { exclusionsRepo.add(it) }
            .observeOn(schedulers.main)

    override fun addExclusionsFrom(items: List<AMResultItem>): Single<List<LocalMediaExclusion>> =
        Observable.fromIterable(items)
            .subscribeOn(schedulers.io)
            .flatMap(::flattenToIds)
            .toList()
            .flatMap { exclusionsRepo.add(it) }
            .observeOn(schedulers.main)

    private fun flattenToIds(item: AMResultItem): Observable<Long> {
        val observable = item.tracks?.let { tracks -> Observable.fromIterable(tracks) }
            ?: Observable.just(item)
        return observable.map { it.itemId.toLong() }
    }
}
