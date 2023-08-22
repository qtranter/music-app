package com.audiomack.ui.mylibrary.offline.local

import androidx.annotation.VisibleForTesting
import com.activeandroid.ActiveAndroid
import com.activeandroid.query.Delete
import com.activeandroid.query.Select
import com.audiomack.data.music.local.MediaStoreId
import com.audiomack.model.LocalMediaExclusion
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.utils.addTo
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

interface LocalMediaExclusionsDataSource {
    /**
     * Emits all [LocalMediaExclusion] records, automatically updating on changes to the table
     */
    val exclusionsObservable: Observable<List<LocalMediaExclusion>>

    /**
     * The latest emission by [exclusionsObservable] or an empty list
     */
    val exclusions: List<LocalMediaExclusion>

    /**
     * Replaces all records with [LocalMediaExclusion] records from [exclusions].
     *
     * @return All records
     */
    fun save(exclusions: List<MediaStoreId>): Single<List<LocalMediaExclusion>>

    /**
     * Creates and saves [LocalMediaExclusion] records from [exclusions]
     */
    fun add(exclusions: List<MediaStoreId>): Single<List<LocalMediaExclusion>>

    fun add(mediaStoreId: MediaStoreId): Single<LocalMediaExclusion>
}

class LocalMediaExclusionsRepository private constructor(
    private val schedulers: SchedulersProvider = AMSchedulersProvider(),
    private val disposables: CompositeDisposable = CompositeDisposable()
) : LocalMediaExclusionsDataSource {

    private val exclusionsSubject = BehaviorSubject.create<List<LocalMediaExclusion>>()
    override val exclusionsObservable: Observable<List<LocalMediaExclusion>> by lazy {
        exclusionsSubject.also { load() }
    }

    override val exclusions: List<LocalMediaExclusion>
        get() = exclusionsSubject.value ?: listOf()

    private fun getAll() =
        Select().from(LocalMediaExclusion::class.java).execute<LocalMediaExclusion>()

    private fun deleteAll() =
        Delete().from(LocalMediaExclusion::class.java).execute<LocalMediaExclusion>()

    private fun load() {
        Single.just(getAll())
            .subscribeOn(schedulers.io)
            .subscribe { items -> exclusionsSubject.onNext(items) }
            .addTo(disposables)
    }

    override fun save(exclusions: List<MediaStoreId>) =
        Single.just(exclusions)
            .subscribeOn(schedulers.io)
            .map { it.map(::LocalMediaExclusion).also { deleteAll() } }
            .flatMap(::batchSave)
            .doAfterSuccess { load() }

    override fun add(exclusions: List<MediaStoreId>) = Single.just(exclusions)
        .subscribeOn(schedulers.io)
        .map { it.map(::LocalMediaExclusion) }
        .flatMap(::batchSave)
        .doAfterSuccess { load() }

    override fun add(mediaStoreId: MediaStoreId): Single<LocalMediaExclusion> =
        Single.just(LocalMediaExclusion(mediaStoreId))
            .subscribeOn(schedulers.io)
            .map { it.apply { save() } }
            .doAfterSuccess { load() }

    private fun batchSave(media: List<LocalMediaExclusion>) =
        Single.create<List<LocalMediaExclusion>> { emitter ->
            ActiveAndroid.beginTransaction()
            try {
                media.forEach { it.save() }
                ActiveAndroid.setTransactionSuccessful()
                emitter.onSuccess(media)
            } catch (e: Exception) {
                emitter.tryOnError(e)
            } finally {
                ActiveAndroid.endTransaction()
            }
        }

    companion object {
        @Volatile
        private var instance: LocalMediaExclusionsRepository? = null

        fun getInstance(): LocalMediaExclusionsRepository = instance ?: synchronized(this) {
            instance ?: LocalMediaExclusionsRepository().also { instance = it }
        }

        @VisibleForTesting
        fun destroy() {
            instance = null
        }
    }
}
