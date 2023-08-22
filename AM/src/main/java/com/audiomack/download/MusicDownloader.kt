package com.audiomack.download

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.audiomack.MainApplication
import com.audiomack.NOTIFICATION_CHANNEL_DOWNLOAD_ID
import com.audiomack.NOTIFICATION_DOWNLOAD_COMPLETED_ID
import com.audiomack.R
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.storage.Storage
import com.audiomack.data.storage.StorageProvider
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.model.AMPlaylistTracks
import com.audiomack.model.AMResultItem
import com.audiomack.model.DownloadServiceCommand
import com.audiomack.model.DownloadServiceCommandType
import com.audiomack.model.EventDownload
import com.audiomack.model.EventShowAddedToOfflineInAppMessage
import com.audiomack.model.EventShowDownloadFailureToast
import com.audiomack.model.EventShowDownloadSuccessToast
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.network.APIInterface
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.Utils
import java.io.File
import java.lang.IllegalStateException
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

interface MusicDownloader {
    val downloadInProgressText: String
    fun enqueueDownload(downloadJobData: DownloadJobData)
    fun enqueueDownloads(downloadJobDataList: List<DownloadJobData>)
    fun prepareForDownload(musicId: String)
    fun prepareForDownload(musicIds: List<String>)
    fun cacheImages(music: AMResultItem)
    fun isMusicBeingDownloaded(music: AMResultItem): Boolean
    fun isMusicWaitingForDownload(music: AMResultItem): Boolean
    val countOfPremiumLimitedDownloadsInProgressOrQueued: Int
}

class AMMusicDownloader private constructor(
    private val httpDownloader: MusicHttpDownloader,
    private val mixpanelDataSource: MixpanelDataSource,
    private val trackingDataSource: TrackingDataSource,
    private val storage: Storage,
    private val apiDownloads: APIInterface.DownloadsInterface,
    private val musicDataSource: MusicDataSource
) : MusicDownloader {

    private val logger: (String) -> Unit = { message -> Timber.tag(AMMusicDownloader::class.java.simpleName).d(message) }
    private val preDownloadQueue: CopyOnWriteArrayList<DownloadJobData> = CopyOnWriteArrayList()
    private val downloadQueue: CopyOnWriteArrayList<DownloadJobData> = CopyOnWriteArrayList()
    private var currentDownloadData: DownloadJobData? = null
    private var running = false
    private var downloadInAppMessageTriggered = false

    override val downloadInProgressText: String
        get() {
            val allTracks =
                (listOf(currentDownloadData?.currentTrack) + preDownloadQueue.map { it.currentTrack } + downloadQueue.map { it.currentTrack }).mapNotNull { it?.title }
            return allTracks.take(5).joinToString(", ")
                .removeSuffix(", ") + if (allTracks.size > 5) " " + MainApplication.context!!.getString(
                R.string.download_notification_message_template,
                (allTracks.size - 5).toString()
            ) else ""
        }

    override fun enqueueDownload(downloadJobData: DownloadJobData) {
        preDownloadQueue.add(downloadJobData)
        DownloadService.start(
            MainApplication.context!!,
            DownloadServiceCommand(
                DownloadServiceCommandType.Download,
                listOf(downloadJobData.currentTrack.itemId)
            )
        )
    }

    override fun enqueueDownloads(downloadJobDataList: List<DownloadJobData>) {
        val context = MainApplication.context ?: return
        if (downloadJobDataList.isEmpty()) return

        preDownloadQueue.addAll(downloadJobDataList)

        DownloadService.start(
            context,
            DownloadServiceCommand(
                DownloadServiceCommandType.Download,
                downloadJobDataList.map { it.currentTrack.itemId }
            )
        )
    }

    override fun cacheImages(music: AMResultItem) {
        CoroutineScope(Dispatchers.IO).launch {
            cacheImagesSync(music)
        }
    }

    override fun isMusicBeingDownloaded(music: AMResultItem): Boolean {
        if (music.isAlbum || music.isPlaylist) {
            return currentDownloadData?.let {
                music.itemId == it.currentTrack.parentId
            } ?: false
        }
        return currentDownloadData?.let {
            music.itemId == it.currentTrack.itemId
        } ?: false
    }

    override fun isMusicWaitingForDownload(music: AMResultItem): Boolean {
        if (music.isAlbum || music.isPlaylist) {
            return downloadQueue.any { music.itemId == it.currentTrack.parentId } && !isMusicBeingDownloaded(music)
        }
        return downloadQueue.any { music.itemId == it.currentTrack.itemId }
    }

    override val countOfPremiumLimitedDownloadsInProgressOrQueued: Int
        get() {
            return downloadQueue.count { it.currentTrack.downloadType == AMResultItem.MusicDownloadType.Limited } +
                (currentDownloadData?.takeIf { it.currentTrack.downloadType == AMResultItem.MusicDownloadType.Limited }?.let { 1 } ?: 0)
        }

    override fun prepareForDownload(musicId: String) {

        val index = preDownloadQueue.indexOfFirst { it.currentTrack.itemId == musicId }
        val downloadJobData = if (index != -1 && index < preDownloadQueue.size) preDownloadQueue[index] else return
        preDownloadQueue.removeAt(index)

        val currentTrack = downloadJobData.currentTrack

        if (currentTrack.isLocal) {
            advance()
            return
        }

        val album = downloadJobData.album
        val parentCollection = downloadJobData.parentCollection
        val downloadAnalytics = downloadJobData.downloadAnalytics

        logger("--Download started--" +
            "\n${getDownloadLogString(currentTrack, album, parentCollection)}")

        downloadAnalytics.eventDownloadStart(currentTrack)

        CoroutineScope(Dispatchers.IO).launch {

            val dbCurrentTrack: AMResultItem? = AMResultItem.findById(currentTrack.itemId)
            dbCurrentTrack?.trackNumber = currentTrack.trackNumber

            val date = Date()
            currentTrack.setDownloadDate(date)
            dbCurrentTrack?.setDownloadDate(date)

            if (parentCollection is TrackParentCollection.Album) {
                dbCurrentTrack?.parentId = parentCollection.id
            }

            // Save the track off so it's viewable in the offline list even if the stream request fails
            // Also if we don't do this, downloading an album from browse results in all the tracks disappearing...
            try {
                dbCurrentTrack?.save() ?: currentTrack.save()
            } catch (saveException: Exception) {
                logger("Exception when trying to save track for download: ${saveException.message}")
                Timber.w(saveException)
                trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - error downloading $musicId")
                trackingDataSource.trackException(saveException)
            }

            if (dbCurrentTrack?.isDownloadCompletedIndependentlyFromType != true) {
                downloadQueue.add(downloadJobData)

                // If this download is part of a full collection download (album/playlist),
                // then let's notify the parent that this thing started
                if (parentCollection?.needsNotification == true) {
                    parentCollection.notifyAboutDownloads()
                }

                EventBus.getDefault().post(EventDownload(currentTrack.itemId, false))
            }

            advance()
        }
    }

    override fun prepareForDownload(musicIds: List<String>) {
        musicIds.forEach { prepareForDownload(it) }
    }

    private fun advance() {
        if (running) {
            return
        }
        downloadQueue
            .firstOrNull()
            ?.let { jobData ->
                currentDownloadData = jobData
                try {
                    downloadQueue.removeAt(0)
                } catch (e: Exception) {
                    // noop
                }
                DownloadService.start(MainApplication.context!!, DownloadServiceCommand(DownloadServiceCommandType.UpdateNotification))
                EventBus.getDefault().post(EventDownload(jobData.currentTrack.itemId, false))
                if (jobData.parentCollection?.needsNotification == true) {
                    jobData.parentCollection.notifyAboutDownloads()
                }
                CoroutineScope(Dispatchers.IO).launch {
                    downloadInternal(jobData)
                }
            } ?: run {
                currentDownloadData = null
                finishedDownloading()
            }
    }

    private fun skipAndAdvance() {
        running = false
        advance()
    }

    private suspend fun downloadInternal(downloadJobData: DownloadJobData) = coroutineScope {

        launch {

            running = true

            val dbTrack = AMResultItem.findById(downloadJobData.currentTrack.itemId)
            dbTrack?.copyFrom(downloadJobData.currentTrack)
            val currentTrack = dbTrack ?: downloadJobData.currentTrack
            val album = downloadJobData.album
            val parentCollection = downloadJobData.parentCollection
            val downloadAnalytics = downloadJobData.downloadAnalytics

            val isFullAlbumDownload: Boolean = parentCollection is TrackParentCollection.Album

            val streamUrlResult: StreamUrlResult = retryIO(
                block = { currentTrack.refreshUrl(currentTrack.parentId) },
                isSuccess = { it is StreamUrlResult.Success }
            )

            val streamUrl: String = when (streamUrlResult) {
                is StreamUrlResult.Failure -> {
                    logFailure(
                        downloadAnalytics,
                        streamUrlResult.exception,
                        streamUrlResult.streamUrl,
                        currentTrack,
                        album,
                        parentCollection
                    )

                    // This triggers the UI to show in the error state
                    EventBus.getDefault().post(EventDownload(currentTrack.itemId, false))
                    EventBus.getDefault().post(EventShowDownloadFailureToast())
                    skipAndAdvance()
                    return@launch
                }
                is StreamUrlResult.Success -> {
                    logger("Download stream request success: ${currentTrack.itemId}")

                    streamUrlResult.streamUrl
                    // Return the below URL if you want to test passing a bad url to DownloadManager
                    // "http://some-invalid-url.com/whateverscootydoot.html"
                }
            }

            logger("Downloading track: $streamUrl")
            trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - download started for ${currentTrack.itemId}")

            try {
                Uri.parse(streamUrl)
            } catch (e: Exception) {
                // Invalid URL
                logFailure(downloadAnalytics, e, streamUrl, currentTrack, album, parentCollection)
                EventBus.getDefault().post(EventShowDownloadFailureToast())
                skipAndAdvance()
                return@launch
            }

            // TODO: Put in more robust folder selection logic (issue #829)
            val baseFolderPath = storage.offlineDir?.absolutePath
            if (baseFolderPath == null) {
                logFailure(
                    downloadAnalytics,
                    IllegalStateException("Storage volume unavailable"),
                    streamUrl,
                    currentTrack,
                    album,
                    parentCollection
                )
                EventBus.getDefault().post(EventShowDownloadFailureToast())
                trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - Storage unavailable")
                skipAndAdvance()
                return@launch
            }

            val outputFolder = File(baseFolderPath)
            outputFolder.mkdirs()
            val destination = File(outputFolder.absolutePath + "/" + currentTrack.itemId)

            // Save to database
            currentTrack.isAlbumTrackDownloadedAsSingle = !(album?.isDownloaded ?: AMResultItem.findById(currentTrack.parentId)?.isDownloaded ?: false)
            currentTrack.isCached = false
            currentTrack.fullPath = null
            try {
                currentTrack.save()
            } catch (e: SQLiteException) {
                Timber.w(e)
            }

            logger("Download track saved: ${currentTrack.itemId}")
            trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - track ${currentTrack.itemId} saved")

            if (!isFullAlbumDownload) {
                // Albums call "addDownload" in their own way
                apiDownloads.addDownload(currentTrack.itemId, currentTrack.mixpanelSource?.page ?: MixpanelSource.empty.page)
            }

            // Show the in app message (if needed)
            if (!downloadInAppMessageTriggered) {
                EventBus.getDefault().post(EventShowAddedToOfflineInAppMessage(currentTrack.downloadType != AMResultItem.MusicDownloadType.Free, currentTrack.mixpanelSource ?: MixpanelSource.empty, parentCollection?.tracksCount ?: 1))
                downloadInAppMessageTriggered = true
            }

            // Start the download
            val downloadResult = httpDownloader.download(streamUrl, destination)

            cacheImagesSync(currentTrack)

            downloadResult.exception?.let {
                // Download failure
                logFailure(downloadAnalytics, it, streamUrl, currentTrack, album, parentCollection)
                EventBus.getDefault().post(EventShowDownloadFailureToast())
                EventBus.getDefault().post(EventDownload(currentTrack.itemId, true))
            } ?: run {
                // Download successful

                AMResultItem.findById(currentTrack.itemId)?.let { item ->

                    // Get list of playlists that aren't fully downloaded yet, then after saving the current
                    // item I'll check again this list and pop a toast if needed
                    val playlistsIDsNotYetFullyDownloaded = takeIf { item.isPlaylistTrack }?.run { AMPlaylistTracks.playlistsThatContain(item.itemId).filter { !AMResultItem.isPlaylistFullyDownloaded(it) } } ?: emptyList()

                    item.isDownloadCompleted = true
                    item.isCached = false
                    item.save()

                    if (item.isSong || item.isPlaylistTrack || item.isAlbumTrackDownloadedAsSingle) {

                        if (item.isPlaylistTrack) {
                            val playlistIds = AMPlaylistTracks.playlistsThatContain(item.itemId)
                            if (playlistIds.isNotEmpty()) {
                                EventBus.getDefault().post(EventDownload(item.itemId, true))
                                playlistIds
                                    .filter { AMResultItem.isPlaylistFullyDownloaded(it) }
                                    .forEach { playlistId ->
                                        if (playlistsIDsNotYetFullyDownloaded.contains(playlistId)) {
                                            AMResultItem.findById(playlistId)
                                                ?.takeIf { !it.isOfflineToastShown }
                                                ?.let {
                                                    it.isOfflineToastShown = true
                                                    it.save()
                                                    EventBus.getDefault().post(EventShowDownloadSuccessToast(it))
                                                }
                                        }
                                        EventBus.getDefault().post(EventDownload(playlistId, true))
                                    }
                                return@run
                            }
                        }

                        EventBus.getDefault().post(EventShowDownloadSuccessToast(item))
                        EventBus.getDefault().post(EventDownload(item.itemId, true))
                        parentCollection?.item?.takeIf {
                            it.isAlbum && (it.tracks?.all { track -> track.isDownloadCompleted } ?: false)
                        }?.let { album ->
                            album.setDownloadDate(Date())
                            album.isDownloadCompleted = true
                            album.save()
                            musicDataSource.bundleAlbumTracks(album.itemId).blockingGet()
                            EventBus.getDefault().post(EventShowDownloadSuccessToast(album))
                        }
                        if (!item.parentId.isNullOrEmpty()) {
                            EventBus.getDefault().post(EventDownload(item.parentId, true))
                        }
                    } else if (item.isAlbumTrack) {

                        EventBus.getDefault().post(EventDownload(item.itemId, true))

                        if (AMResultItem.isAlbumFullyDownloaded(item.parentId)) {
                            item.parentId?.let { albumId ->
                                musicDataSource.bundleAlbumTracks(albumId).blockingGet()
                            }
                            AMResultItem.findById(item.parentId)?.let { album ->
                                EventBus.getDefault().post(EventShowDownloadSuccessToast(album))
                            }
                            EventBus.getDefault().post(EventDownload(item.parentId, true))
                        }
                    }
                }

                // Only log success if we haven't already marked this download as complete
                downloadAnalytics.eventDownloadComplete(currentTrack)
            }
            skipAndAdvance()
        }
    }

    private suspend fun cacheImagesSync(music: AMResultItem) = coroutineScope {
        launch {
            val context = MainApplication.context ?: return@launch
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return@launch
            }
            val urls = listOf(
                music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
                music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetOriginal),
                music.banner
            ).mapNotNull { it }.filter { it.isNotBlank() }
            urls.forEach {
                val file = Utils.remoteUrlToArtworkFile(context, it)
                if (file != null && file.length() < 512) {
                    httpDownloader.download(it, file)
                }
            }
        }
    }

    private fun logFailure(
        downloadAnalytics: DownloadAnalytics,
        exception: Exception,
        streamUrl: String?,
        currentTrack: AMResultItem,
        album: AMResultItem?,
        parentCollection: TrackParentCollection?
    ) {

        logger("--Download failed--" +
            "Exception: ${exception.message}" +
            "\n${getDownloadLogString(currentTrack, album, parentCollection)}")

        trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - download failed for {$currentTrack.itemId}")
        trackingDataSource.trackException(exception)

        downloadAnalytics.eventDownloadFailure(
            exception = exception,
            extraInfo = "Track id: ${currentTrack.itemId} - ${currentTrack.title}. " +
                "Parent collection: ${parentCollection?.id} - ${parentCollection?.analyticsName}. " +
                "Stream url: $streamUrl."
        )

        mixpanelDataSource.trackError(
            "Download",
            exception.localizedMessage ?: ""
        )
    }

    private fun getDownloadLogString(
        track: AMResultItem,
        album: AMResultItem?,
        parentCollection: TrackParentCollection?
    ) =
        "Track: ${track.itemId} - ${track.title}" +
            "\nAlbum: ${album?.itemId} - ${album?.title}" +
            "\nparentCollection: ${parentCollection?.analyticsName} - ${parentCollection?.id}"

    private fun showCompletedNotification() {
        val context = MainApplication.context ?: return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val intent = Intent(context, HomeActivity::class.java)
        intent.data = Uri.parse("audiomack://artist_downloads")
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_DOWNLOAD_COMPLETED_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification =
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_DOWNLOAD_ID)
                .setContentTitle(context.getString(R.string.download_completed_notification_title))
                .setContentText(context.getString(R.string.download_completed_notification_message))
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        context.resources,
                        R.mipmap.ic_launcher
                    )
                )
                .setColor(Color.BLACK)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        notificationManager.notify(NOTIFICATION_DOWNLOAD_COMPLETED_ID, notification)
    }

    private fun finishedDownloading() {
        if (preDownloadQueue.isEmpty() && downloadQueue.isEmpty() && currentDownloadData == null) {
            showCompletedNotification()
            DownloadService.start(MainApplication.context!!, DownloadServiceCommand(DownloadServiceCommandType.Stop))
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MusicDownloader? = null

        @JvmOverloads
        @JvmStatic
        fun getInstance(
            httpDownloader: MusicHttpDownloader = AMMusicHttpDownloader(),
            mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
            trackingDataSource: TrackingDataSource = TrackingRepository(),
            storage: Storage = StorageProvider.getInstance(),
            apiDownloads: APIInterface.DownloadsInterface = API.getInstance(),
            musicDataSource: MusicDataSource = MusicRepository()
        ): MusicDownloader = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AMMusicDownloader(
                httpDownloader,
                mixpanelDataSource,
                trackingDataSource,
                storage,
                apiDownloads,
                musicDataSource
            ).also { INSTANCE = it }
        }
    }
}

class DownloadJobData(
    val currentTrack: AMResultItem,
    val album: AMResultItem? = null,
    val downloadAnalytics: DownloadAnalytics,
    val parentCollection: TrackParentCollection? = null
)

sealed class TrackParentCollection(val id: String, val needsNotification: Boolean, val analyticsName: String, val tracksCount: Int, val item: AMResultItem?) {
    class Playlist(id: String, tracksCount: Int = 0, needsNotification: Boolean = false, item: AMResultItem? = null) : TrackParentCollection(id, needsNotification, "playlist", tracksCount, item)
    class Album(id: String, tracksCount: Int = 0, needsNotification: Boolean = false, item: AMResultItem? = null) : TrackParentCollection(id, needsNotification, "album", tracksCount, item)

    fun notifyAboutDownloads() {
        EventBus.getDefault().post(EventDownload(id, false))
    }
}
