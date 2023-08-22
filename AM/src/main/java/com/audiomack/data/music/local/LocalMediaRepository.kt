package com.audiomack.data.music.local

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Media
import androidx.annotation.VisibleForTesting
import com.audiomack.GENERAL_PREFERENCES_INCLUDE_LOCAL_FILES
import com.audiomack.MainApplication
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.LocalMediaExclusion
import com.audiomack.model.albumFromMediaCursor
import com.audiomack.model.songFromMediaCursor
import com.audiomack.model.songFromOpenableCursor
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.common.Permission.Storage
import com.audiomack.ui.common.PermissionHandler
import com.audiomack.ui.mylibrary.offline.local.LocalMediaExclusionsDataSource
import com.audiomack.ui.mylibrary.offline.local.LocalMediaExclusionsRepository
import com.audiomack.ui.mylibrary.offline.local.StoragePermissionHandler
import com.audiomack.utils.addTo
import com.audiomack.utils.gt
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

class LocalMediaRepository private constructor(
    private val contentResolver: ContentResolver,
    private val preferences: PreferencesDataSource,
    private val exclusionsRepo: LocalMediaExclusionsDataSource,
    private val disposables: CompositeDisposable,
    private val schedulers: SchedulersProvider,
    private val storagePermissions: PermissionHandler<Storage>,
    private val tracking: TrackingDataSource
) : LocalMediaDataSource, ContentObserver(Handler()) {

    private val _allItems = BehaviorSubject.create<List<AMResultItem>>()
    override val allTracks: Observable<List<AMResultItem>> by lazy {
        _allItems.also { loadAll() }
    }

    private val _visibleItems = BehaviorSubject.create<List<AMResultItem>>()
    override val visibleItems: Observable<List<AMResultItem>> by lazy {
        _visibleItems.also { loadFiltered() }
    }

    private val hasLocalMedia = BehaviorSubject.create<Boolean>()

    private val includeLocalFiles =
        preferences.observeBoolean(GENERAL_PREFERENCES_INCLUDE_LOCAL_FILES)

    private val shouldMonitorLocalMedia: Boolean
        get() = storagePermissions.hasPermission && preferences.includeLocalFiles

    init {
        observeMediaStore()
        observeStoragePermissions()
    }

    override fun getTrack(storeId: Long) = Single.create<AMResultItem> { emitter ->
        val item = try {
            getTrackInternal(storeId)
        } catch (e: Exception) {
            emitter.tryOnError(e)
            null
        }

        if (item != null) {
            emitter.onSuccess(item)
        } else {
            emitter.tryOnError(LocalResourceException("Unable to find media with id $storeId"))
        }
    }

    private fun getTrackInternal(storeId: Long): AMResultItem? =
        getTrackCursor(storeId)?.use { cursor ->
            takeIf { cursor.moveToFirst() }.let {
                val album = cursor.getLong(Media.ALBUM_ID)?.let { albumId ->
                    getAlbumCursor(albumId)?.use { albumCursor ->
                        takeIf { albumCursor.moveToFirst() }.let {
                            AMResultItem().albumFromMediaCursor(albumCursor)
                        }
                    }
                }
                AMResultItem().songFromMediaCursor(cursor, album)
            }
        }

    override fun getAlbum(storeId: Long) = Single.create<AMResultItem> { emitter ->
        val album = try {
            getAlbumCursor(storeId)?.use { albumCursor ->
                takeIf { albumCursor.moveToFirst() }.let {
                    AMResultItem().albumFromMediaCursor(albumCursor)?.also { item ->
                        val exclusions = exclusionsRepo.exclusions.map { it.mediaId.toString() }
                        val tracks = mutableListOf<AMResultItem>()
                        getAlbumTracksCursor(storeId, exclusions)?.use { trackCursor ->
                            while (trackCursor.moveToNext()) {
                                AMResultItem().songFromMediaCursor(trackCursor, item)?.let { song ->
                                    tracks += song
                                }
                            }
                        }
                        item.tracks = tracks
                    }
                }
            }
        } catch (e: Exception) {
            emitter.tryOnError(e)
            null
        }

        if (album != null) {
            emitter.onSuccess(album)
        } else {
            emitter.tryOnError(LocalResourceException("Unable to find album with id $storeId"))
        }
    }

    override fun findIdByPath(path: String) = Maybe.create<MediaStoreId> { emitter ->
        val selection = "${Media.DATA} = ?"
        val selectionArgs = arrayOf(path)

        try {
            contentResolver.query(
                Media.EXTERNAL_CONTENT_URI,
                PROJECTION_MUSIC,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getContentId()
                    if (id != null) {
                        emitter.onSuccess(id)
                        return@create
                    }
                }
            }
        } catch (e: Exception) {
            emitter.tryOnError(e)
            return@create
        }

        emitter.onComplete()
    }

    override fun query(uri: Uri) = Maybe.create<AMResultItem> { emitter ->
        val item = try {
            contentResolver.query(uri)?.use { cursor ->
                cursor.takeIf { it.moveToFirst() }
                    ?.let { AMResultItem().songFromOpenableCursor(it, uri) }
            }
        } catch (e: Exception) {
            emitter.tryOnError(RuntimeException())
            return@create
        }

        if (item != null) {
            emitter.onSuccess(item)
        } else {
            emitter.onComplete()
        }
    }

    override fun getType(uri: Uri): MimeType? = contentResolver.getType(uri)

    override fun refresh() {
        refreshMediaCount()
    }

    private fun loadAll() {
        hasLocalMedia
            .flatMapSingle { loadMedia() }
            .subscribeOn(schedulers.io)
            .doOnError { tracking.trackException(it) }
            .onErrorReturnItem(arrayListOf())
            .observeOn(schedulers.main)
            .subscribe { allItems -> _allItems.onNext(allItems) }
            .addTo(disposables)

        refreshMediaCount()
    }

    private fun loadFiltered() {
        Observable.combineLatest(
            includeLocalFiles,
            hasLocalMedia,
            exclusionsRepo.exclusionsObservable,
            { include, hasMedia, exclusions -> Pair(include && hasMedia, exclusions) }
        ).subscribeOn(schedulers.io)
            .flatMapSingle { (itemsVisible, exclusions) ->
                if (itemsVisible) loadMedia(true, exclusions) else Single.just(listOf())
            }
            .doOnError { tracking.trackException(it) }
            .onErrorReturnItem(arrayListOf())
            .doOnNext { tracking.trackBreadcrumb("Loaded ${it.size} visible local media items") }
            .observeOn(schedulers.main)
            .subscribe(_visibleItems)

        refreshMediaCount()
    }

    fun clear() {
        contentResolver.unregisterContentObserver(this)
        disposables.clear()
    }

    private fun loadMedia(
        collapseAlbums: Boolean = false,
        exclusions: List<LocalMediaExclusion> = listOf()
    ) = Single.create<List<AMResultItem>> { emitter ->
        val exclusionIds = exclusions.map { it.mediaId.toString() }

        val albums = mutableMapOf<String, AMResultItem>()

        try {
            getAlbumsCursor(exclusionIds)?.use { cursor ->
                while (cursor.moveToNext()) {
                    AMResultItem().albumFromMediaCursor(cursor)?.let { album ->
                        albums.put(album.itemId, album)
                    }
                }
            }
        } catch (e: Exception) {
            emitter.tryOnError(e)
            return@create
        }

        // Filter for albums with multiple tracks. AMResultItem.albumFromCursor() returns an array
        // of nulls for the tracks field, so also clear those.
        val albumsToShow = albums
            .filterValues { it.tracks?.size.gt(1) }
            .mapValues { it.value.apply { tracks = mutableListOf() } }

        val items = mutableListOf<AMResultItem>()

        try {
            getMediaCursor(exclusionIds)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val albumId = cursor.getLong(Media.ALBUM_ID)?.toString()
                    val album = albumId?.let { albums[it] }

                    val song = AMResultItem().songFromMediaCursor(cursor, album) ?: continue

                    if (!collapseAlbums || albumId == null) {
                        items += song
                        continue
                    }

                    // Add songs to album tracks when there are more than one track
                    albumsToShow[albumId]?.run {
                        tracks?.add(song)
                    } ?: run {
                        items += song
                    }
                }
            }
        } catch (e: Exception) {
            emitter.tryOnError(e)
            return@create
        }

        if (collapseAlbums) {
            items += albumsToShow.values.filter { !it.tracks.isNullOrEmpty() }
        }

        emitter.onSuccess(items)
    }

    private fun observeMediaStore() {
        if (!shouldMonitorLocalMedia) {
            contentResolver.unregisterContentObserver(this)
            return
        }

        contentResolver.registerContentObserver(
            Media.EXTERNAL_CONTENT_URI,
            true,
            this
        )
    }

    private fun observeStoragePermissions() {
        storagePermissions.hasPermissionObservable
            .observeOn(schedulers.main)
            .doOnNext { tracking.trackBreadcrumb("Storage permission set to = $it") }
            .doOnError { tracking.trackException(it) }
            .subscribe {
                observeMediaStore()
                refreshMediaCount()
            }
            .addTo(disposables)
    }

    override fun onChange(selfChange: Boolean) {
        this.onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        tracking.trackBreadcrumb("Observed change event from MediaStore")
        refreshMediaCount()
    }

    /**
     * Checks the media store to see if there is local media and triggers [hasLocalMedia].
     *
     * This is only true if the user has already granted storage permissions.
     */
    private fun refreshMediaCount() {
        if (!shouldMonitorLocalMedia) {
            contentResolver.unregisterContentObserver(this)
            hasLocalMedia.onNext(false)
            return
        }

        getCount()
            .subscribeOn(schedulers.io)
            .doOnSuccess { tracking.trackBreadcrumb("Found $it Media Store records") }
            .map { it > 0 }
            .doOnError { tracking.trackException(it) }
            .onErrorReturnItem(false)
            .observeOn(schedulers.main)
            .subscribe { hasFiles ->
                Timber.tag(TAG).d("refreshMediaCount : hasFiles = $hasFiles")
                hasLocalMedia.onNext(hasFiles)
            }
            .addTo(disposables)
    }

    private fun getMediaCursor(exclusionIds: List<String> = listOf()): Cursor? {
        val exclusions = exclusionIds.joinToString()
        val selection = "${Media.IS_MUSIC} = ? AND ${Media._ID} NOT IN ($exclusions) AND ${Media.ALBUM_ID} NOT IN ($exclusions)"
        val selectionArgs = arrayOf("1")

        return contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            PROJECTION_MUSIC,
            selection,
            selectionArgs,
            ALBUM_TRACK_SORT
        )
    }

    private fun getAlbumsCursor(exclusionIds: List<String> = listOf()): Cursor? {
        val selection = "${Media._ID} NOT IN (${exclusionIds.joinToString()})"

        return contentResolver.query(
            Albums.EXTERNAL_CONTENT_URI,
            PROJECTION_ALBUM,
            selection,
            null,
            null
        )
    }

    private fun getTrackCursor(id: Long): Cursor? {
        val selection = "${Media._ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        return contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            PROJECTION_MUSIC,
            selection,
            selectionArgs,
            null
        )
    }

    private fun getAlbumCursor(id: Long): Cursor? {
        val selection = "${Albums._ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        return contentResolver.query(
            Albums.EXTERNAL_CONTENT_URI,
            PROJECTION_ALBUM,
            selection,
            selectionArgs,
            null
        )
    }

    private fun getAlbumTracksCursor(
        albumId: Long,
        exclusionIds: List<String> = listOf()
    ): Cursor? {
        val selection = "${Media.ALBUM_ID} = ? AND ${Media._ID} NOT IN (${exclusionIds.joinToString()})"
        val selectionArgs = arrayOf(albumId.toString())

        return contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            PROJECTION_MUSIC,
            selection,
            selectionArgs,
            ALBUM_TRACK_SORT
        )
    }

    private fun getCount() = Single.create<Int> { emitter ->
        val count = getMediaCursor()?.use { it.count } ?: 0
        emitter.onSuccess(count)
    }

    companion object {
        private const val TAG = "LocalMediaRepository"

        private val PROJECTION_MUSIC = mutableListOf(
            Media._ID,
            Media.TITLE,
            Media.DISPLAY_NAME,
            Media.ARTIST,
            Media.ALBUM,
            Media.ALBUM_ID,
            Media.DATE_ADDED,
            Media.TRACK
        ).run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Media.DURATION)
            }
            toTypedArray()
        }

        private val PROJECTION_ALBUM = arrayOf(
            Albums._ID,
            Albums.ALBUM,
            Albums.ARTIST,
            Albums.NUMBER_OF_SONGS,
            Albums.LAST_YEAR
        )

        private const val ALBUM_TRACK_SORT =
            "${Media.ALBUM_ID} ASC, ${Media.TRACK} ASC, ${Media.TITLE} ASC"

        @Volatile
        private var instance: LocalMediaRepository? = null

        @JvmStatic
        @JvmOverloads
        fun getInstance(
            contentResolver: ContentResolver = MainApplication.context!!.contentResolver,
            preferences: PreferencesDataSource = PreferencesRepository(),
            localMediaExclusions: LocalMediaExclusionsDataSource = LocalMediaExclusionsRepository.getInstance(),
            disposables: CompositeDisposable = CompositeDisposable(),
            schedulers: SchedulersProvider = AMSchedulersProvider(),
            permissionHandler: PermissionHandler<Storage> = StoragePermissionHandler.getInstance(),
            tracking: TrackingDataSource = TrackingRepository()
        ): LocalMediaRepository = instance ?: LocalMediaRepository(
            contentResolver,
            preferences,
            localMediaExclusions,
            disposables,
            schedulers,
            permissionHandler,
            tracking
        ).also { instance = it }

        @VisibleForTesting
        fun destroy() {
            instance?.clear()
            instance = null
        }
    }
}
