package com.audiomack.data.housekeeping

import android.content.Context
import android.text.TextUtils
import com.audiomack.BuildConfig
import com.audiomack.data.cache.CachingLayer
import com.audiomack.data.cache.CachingLayerImpl
import com.audiomack.data.cache.isCached
import com.audiomack.data.database.MusicDAO
import com.audiomack.data.database.MusicDAOImpl
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.remotevariables.datasource.FirebaseRemoteVariablesDataSource
import com.audiomack.data.removedcontent.RemovedContentRepository
import com.audiomack.data.storage.Storage
import com.audiomack.data.storage.StorageProvider
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItemSort
import com.audiomack.model.MixpanelSource
import com.audiomack.model.RemovedContentItem
import com.audiomack.network.API
import com.audiomack.network.APIInterface
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.File
import timber.log.Timber

class HousekeepingRepository(
    private val storage: Storage = StorageProvider.getInstance(),
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    private val apiDownloads: APIInterface.DownloadsInterface = API.getInstance(),
    private val cachingLayer: CachingLayer = CachingLayerImpl.getInstance(),
    private val schedulers: SchedulersProvider = AMSchedulersProvider()
) : HousekeepingDataSource {

    private val TAG = HousekeepingRepository::class.java.simpleName

    private val musicDAO: MusicDAO = MusicDAOImpl()

    override val syncMusic: Completable
        get() {
            return musicDAO.unsyncedSavedItemsIds()
                .flatMap {
                    if (it.isNotEmpty()) {
                        apiDownloads.addDownload(it.joinToString(","), MixpanelSource.empty.page)
                    }
                    Observable.just(true)
                }
                .ignoreElements()
        }

    override val downloadsToRestore = PublishSubject.create<List<AMResultItem>>()

    override val houseekping: Completable
        get() {
            return Completable.create { emitter ->

                // It's possible that the database was restored with auto-backup. In this case
                // there will be no files in the offline directory, but we don't want to run
                // the sanity check
                val offlineFiles = storage.offlineDir?.listFiles { file ->
                    !file.isDirectory && !file.isHidden
                }
                val itemsToRestore = musicDAO.getAllTracks().blockingFirst()
                if (itemsToRestore.isNotEmpty() && offlineFiles.isNullOrEmpty()) {
                    val items = musicDAO.markDownloadIncomplete(itemsToRestore).blockingFirst()
                    Timber.tag(TAG).i("Found a restored database pointing to missing files")
                    downloadsToRestore.apply {
                        onNext(items)
                        onComplete()
                    }
                } else {
                    musicDAO.savedItems(
                        AMResultItemSort.NewestFirst,
                        "ID",
                        "item_id",
                        "type",
                        "download_completed",
                        "full_path"
                    ).blockingFirst().forEach { it.sanityCheck() }
                }

                // Clean up cached items
                musicDAO.cachedItems()
                    .flatMapIterable { it }
                    .filter { !cachingLayer.isCached(it) }
                    .forEach { it.delete() }

                val itemsIDs =
                    if (RemoteVariablesProviderImpl(FirebaseRemoteVariablesDataSource).downloadCheckEnabled) {
                        AMResultItem.getAllItemsIds()
                    } else {
                        null
                    }

                if (itemsIDs.isNullOrEmpty()) {
                    emitter.onComplete()
                    return@create
                }

                // Check music availability
                API.getInstance()
                    .checkMusicAvailability(itemsIDs, object : API.ArrayListener<String> {
                        override fun onSuccess(results: List<String>) {
                            Timber.tag("Housekeeping")
                                .d("Check music availability succeeded, found " + results.size + " items to be deleted")

                            RemovedContentRepository.clearItems()

                            if (BuildConfig.AUDIOMACK_DEBUG) {
                                // Add all saved songs in order to see the in app message
                                musicDAO.savedItems(AMResultItemSort.AToZ).blockingFirst().forEach {
                                    RemovedContentRepository.addItem(
                                        RemovedContentItem(
                                            it.artist ?: "",
                                            it.title ?: ""
                                        )
                                    )
                                }
                            }

                            results.forEach { itemIDToBeDeleted ->
                                AMResultItem.findById(itemIDToBeDeleted)?.let {
                                    RemovedContentRepository.addItem(
                                        RemovedContentItem(
                                            it.artist ?: "",
                                            it.title ?: ""
                                        )
                                    )
                                    it.deepDelete()
                                }
                            }

                            if (results.isNotEmpty() && results.size == itemsIDs.size) {
                                trackingDataSource.trackException(
                                    Exception(
                                        "Full wipeout: " + results.size + " / " + itemsIDs.size + " - " + TextUtils.join(
                                            ",",
                                            results
                                        )
                                    )
                                )
                            } else if (results.size > 5) {
                                trackingDataSource.trackException(
                                    Exception(
                                        "Partial wipeout: " + results.size + " / " + itemsIDs.size + " - " + TextUtils.join(
                                            ",",
                                            results
                                        )
                                    )
                                )
                            }

                            emitter.onComplete()
                        }

                        override fun onFailure() {
                            Timber.tag("Housekeeping").d("Check music availability failed")
                            emitter.onComplete()
                        }
                    })
            }.doOnError { Timber.tag(TAG).w(it) }
        }

    override fun createNoMediaFiles(context: Context): Completable {
        return Completable.create { emitter ->
            try {
                val baseFolderPath = storage.offlineDir?.absolutePath
                baseFolderPath?.let { dir ->
                    val baseFolderDir = File(dir)
                    baseFolderDir.mkdirs()
                    File(dir + File.separator + ".nomedia").createNewFile()
                    emitter.onComplete()
                } ?: emitter.onError(IllegalStateException("Storage volume unavailable"))
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }.doOnError { Timber.tag(TAG).w(it) }
    }

    override fun clearRestoredDatabase() = musicDAO.deleteAllItems()
}
