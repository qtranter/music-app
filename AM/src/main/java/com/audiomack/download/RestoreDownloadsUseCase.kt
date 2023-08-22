package com.audiomack.download

import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.storage.StorageProvider
import com.audiomack.data.storage.isFileValid
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.download.DownloadOrigin.RESTORE_DOWNLOADS
import com.audiomack.download.RestoreDownloadsResult.Failure
import com.audiomack.download.RestoreDownloadsResult.Success
import com.audiomack.download.TrackParentCollection.Album
import com.audiomack.download.TrackParentCollection.Playlist
import com.audiomack.model.AMResultItem
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import timber.log.Timber

private const val TAG = "RestoreDownloadsUseCase"

sealed class RestoreDownloadsResult {
    object Success : RestoreDownloadsResult()
    class Failure(val throwable: Throwable) : RestoreDownloadsResult()
}

interface RestoreDownloadsUseCase {
    fun restore(scheduler: Scheduler? = null): Single<RestoreDownloadsResult>
}

class RestoreDownloadsUseCaseImpl(
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val musicDownloader: MusicDownloader = AMMusicDownloader.getInstance(),
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    private val storageProvider: StorageProvider = StorageProvider.getInstance(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : RestoreDownloadsUseCase {

    override fun restore(scheduler: Scheduler?): Single<RestoreDownloadsResult> =
        musicDataSource.getDownloads()
            .subscribeOn(scheduler ?: schedulersProvider.io)
            .map { downloads -> downloads.filter { !storageProvider.isFileValid(it) } }
            .doOnNext { Timber.tag(TAG).i("Attempting to restore ${it.size} downloads...") }
            .flatMap { musicDataSource.markDownloadIncomplete(it) }
            .flatMap(this::itemsToJobs)
            .firstOrError()
            .map<RestoreDownloadsResult> {
                Timber.tag(TAG).i("Enqueuing ${it.size} downloads jobs...")
                musicDownloader.enqueueDownloads(it)
                Success
            }
            .onErrorReturn { Failure(it) }
            .observeOn(scheduler ?: schedulersProvider.main)

    private fun itemsToJobs(items: List<AMResultItem>): Observable<List<DownloadJobData>> {
        val jobs = items.map { item ->
            DownloadJobData(
                item,
                getParentAlbum(item),
                AmDownloadAnalytics(RESTORE_DOWNLOADS, trackingDataSource),
                getParentCollection(item)
            )
        }
        return Observable.just(jobs)
    }

    private fun getParentCollection(item: AMResultItem): TrackParentCollection? =
        item.parentId?.let { parentId ->
            when {
                item.isAlbumTrack -> Album(parentId)
                item.isPlaylistTrack -> Playlist(parentId)
                else -> null
            }
        }

    private fun getParentAlbum(item: AMResultItem): AMResultItem? = if (item.isAlbumTrack) {
        item.parentId?.let { musicDataSource.getOfflineItem(it).blockingGet() }
    } else null
}
