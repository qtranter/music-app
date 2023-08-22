package com.audiomack.data.music.local

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.WorkerThread
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.songFromOpenableFile
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.common.Permission
import com.audiomack.ui.common.PermissionHandler
import com.audiomack.ui.home.AlertManager
import com.audiomack.ui.home.AlertTriggers
import com.audiomack.ui.home.NavigationActions
import com.audiomack.ui.home.NavigationManager
import com.audiomack.ui.mylibrary.offline.local.StoragePermissionHandler
import com.audiomack.utils.addTo
import com.audiomack.utils.isMediaStoreUri
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

interface OpenLocalMediaUseCase {
    /**
     * Attempts to play the MediaStore file represented by this [uri]. If the track is part of an
     * album, the entire album will be added to the queue.
     */
    fun open(uri: Uri, mimeType: MimeType? = null)
}

class UnsupportedFileException : Exception()

sealed class OpenType {
    data class Content(val id: MediaStoreId) : OpenType()
    data class File(val uri: Uri) : OpenType()
    data class Unknown(val uri: Uri) : OpenType()
}

class OpenLocalMediaUseCaseImpl(
    activityResultRegistry: ActivityResultRegistry,
    private val localMediaRepo: LocalMediaDataSource = LocalMediaRepository.getInstance(),
    private val schedulers: SchedulersProvider = AMSchedulersProvider(),
    private val storagePermissions: PermissionHandler<Permission.Storage> = StoragePermissionHandler.getInstance(),
    private val mixPanelRepo: MixpanelDataSource = MixpanelRepository(),
    private val mimeTypeHelper: MimeTypeHelper = MimeTypeHelperImpl(),
    private val navigation: NavigationActions = NavigationManager.getInstance(),
    private val alertTriggers: AlertTriggers = AlertManager
) : OpenLocalMediaUseCase, ActivityResultCallback<Boolean> {

    private val permissionLauncher: ActivityResultLauncher<String>

    private val disposables = CompositeDisposable()

    private var pendingOpen: OpenType? = null

    init {
        permissionLauncher = activityResultRegistry.register(
            REGISTRY_KEY_LOCAL_MEDIA,
            RequestPermission(),
            this
        )
    }

    override fun open(uri: Uri, mimeType: MimeType?) {
        Timber.tag(TAG).i("open() called with uri = $uri, mimeType = $mimeType")

        Single.just(isSupported(uri, mimeType))
            .subscribeOn(schedulers.io)
            .flatMap { isSupported ->
                if (isSupported) {
                    buildOpenType(uri)
                } else {
                    Single.error(UnsupportedFileException())
                }
            }
            .observeOn(schedulers.main)
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe { open, e ->
                pendingOpen = open

                if (e != null) {
                    if (e is UnsupportedFileException) {
                        alertTriggers.onPlayUnsupportedFileAttempt(uri)
                    } else {
                        alertTriggers.onGenericError()
                    }
                    return@subscribe
                }

                if (!storagePermissions.hasPermission) {
                    permissionLauncher.launch(Permission.Storage.key)
                    return@subscribe
                }

                load()
            }
            .addTo(disposables)
    }

    override fun onActivityResult(isGranted: Boolean) {
        if (isGranted) {
            load()
        } else {
            alertTriggers.onStoragePermissionDenied()
            pendingOpen = null
        }
    }

    private fun load() {
        Timber.tag(TAG).i("load() called with pendingOpen = $pendingOpen")

        val open = pendingOpen ?: run {
            alertTriggers.onGenericError()
            return
        }

        Single.just(open)
            .subscribeOn(schedulers.io)
            .flatMap {
                when (it) {
                    is OpenType.Content -> buildContentPlayerData(it.id)
                    is OpenType.File -> buildFilePlayerData(it.uri)
                    is OpenType.Unknown -> buildOpenablePlayerData(it.uri)
                }
            }
            .observeOn(schedulers.main)
            .doAfterSuccess {
                Timber.tag(TAG).d("Launching player with $it")
                mixPanelRepo.trackLocalFileOpened(it.item?.title ?: "", it.item?.artist ?: "")
            }
            .doOnError { Timber.tag(TAG).e(it, "Error while building player data") }
            .doFinally { pendingOpen = null }
            .subscribe { data, e ->
                e?.let { alertTriggers.onGenericError() }
                data?.let { navigation.launchPlayer(it) }
            }
            .addTo(disposables)
    }

    private fun buildOpenType(uri: Uri): Single<OpenType> = when (uri.scheme) {
        // Attempt to find a MediaStore id, falling back to simply querying the openable Uri
        ContentResolver.SCHEME_CONTENT -> getMediaIdFromContent(uri)
            .map<OpenType> { OpenType.Content(it) }
            .onErrorReturnItem(OpenType.Unknown(uri))

        // Attempt to find a MediaStore id, falling back to simply opening the file
        ContentResolver.SCHEME_FILE -> getMediaIdFromFile(uri)
            .map<OpenType> { OpenType.Content(it) }
            .onErrorReturnItem(OpenType.File(uri))

        else -> Single.just(OpenType.Unknown(uri))
    }

    private fun buildContentPlayerData(id: MediaStoreId): Single<MaximizePlayerData> =
        localMediaRepo.getTrack(id)
            .subscribeOn(schedulers.io)
            .flatMap { track ->
                val parentId = track.parentId?.toLong()
                if (parentId != null && track.isAlbumTrack) {
                    buildAlbumPlayerData(track, parentId)
                } else {
                    Single.just(MaximizePlayerData(track))
                }
            }

    private fun buildAlbumPlayerData(
        track: AMResultItem,
        parentId: Long
    ) = localMediaRepo.getAlbum(parentId)
        .map { album ->
            val index = album.tracks?.indexOfFirst { it.itemId == track.itemId }
            MaximizePlayerData(
                item = track,
                collection = album,
                items = album.tracks,
                albumPlaylistIndex = index
            )
        }
        .onErrorReturnItem(MaximizePlayerData(track))

    private fun buildFilePlayerData(uri: Uri): Single<MaximizePlayerData> {
        val item = AMResultItem().songFromOpenableFile(uri)
        return Single.just(MaximizePlayerData(item))
    }

    private fun buildOpenablePlayerData(uri: Uri) =
        localMediaRepo.query(uri)
            .map { MaximizePlayerData(it) }
            .toSingle()

    private fun getMediaIdFromContent(uri: Uri) =
        Single.create<MediaStoreId> { emitter ->
            val id = uri.takeIf { it.isMediaStoreUri() }?.let { uri.lastPathSegment?.toLong() }
            if (id != null) {
                emitter.onSuccess(id)
            } else {
                emitter.tryOnError(RuntimeException("Unable to get media id from content Uri $uri"))
            }
        }

    private fun getMediaIdFromFile(uri: Uri): Single<MediaStoreId> {
        return uri.path?.let { path ->
            localMediaRepo.findIdByPath(path).flatMapSingle { Single.just(it) }
        } ?: Single.error(RuntimeException("Unable to find media id for file Uri $uri"))
    }

    @WorkerThread
    private fun isSupported(uri: Uri, mimeType: MimeType?): Boolean {
        if (mimeType != null) {
            return mimeType.isAudio()
        }

        return try {
            getMimeType(uri)
        } catch (e: Throwable) {
            null
        }?.let {
            it.isAudio()
        } ?: false
    }

    @WorkerThread
    private fun getMimeType(uri: Uri): MimeType? = when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> localMediaRepo.getType(uri)
        ContentResolver.SCHEME_FILE -> mimeTypeHelper.getMimeTypeFromUrl(uri.encodedPath)
        else -> null
    }

    companion object {
        private const val TAG = "LocalFileOpenerUseCase"
        private const val REGISTRY_KEY_LOCAL_MEDIA = "com.audiomack.data.music.local.LOCAL_MEDIA"
    }
}
