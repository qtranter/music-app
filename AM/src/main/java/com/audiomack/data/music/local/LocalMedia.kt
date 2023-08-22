package com.audiomack.data.music.local

import android.net.Uri
import androidx.media2.exoplayer.external.util.MimeTypes
import com.audiomack.model.AMResultItem
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

internal typealias MediaStoreId = Long

internal typealias MimeType = String
internal fun MimeType?.isAudio() = this?.startsWith(MimeTypes.BASE_TYPE_AUDIO) == true

interface LocalMediaDataSource {
    /**
     * Emits all local music found in the media store.
     *
     * Album tracks are returned individually and no [AMResultItem] is returned for albums.
     */
    val allTracks: Observable<List<AMResultItem>>

    /**
     * Emits local media that should be visible in the app. Three criteria must be met for a
     * local file to be included:
     *
     * 1. Local files inclusion preference is on
     * 2. File is not part of the exclusions list
     * 3. App has storage permissions
     *
     * Album tracks are returned as part of the [AMResultItem.tracks] in an album [AMResultItem]
     *
     * @see com.audiomack.model.LocalMediaExclusion
     */
    val visibleItems: Observable<List<AMResultItem>>

    /**
     * Emits an [AMResultItem] based on a media store record with the _id provided.
     */
    fun getTrack(storeId: MediaStoreId): Single<AMResultItem>

    /**
     * Emits an album [AMResultItem] with the [AMResultItem.tracks] field populated based on a
     * media store record with the _id provided.
     *
     * Excluded tracks are not returned in [AMResultItem.tracks]
     *
     * @see com.audiomack.model.LocalMediaExclusion
     */
    fun getAlbum(storeId: MediaStoreId): Single<AMResultItem>

    /**
     * Attempts to locate a MediaStore item by it's data path and emits the content ID if found.
     *
     * @see android.provider.BaseColumns._ID
     */
    fun findIdByPath(path: String): Maybe<MediaStoreId>

    /**
     * Queries Content and Document Providers for this openable Uri, returning an item if found
     */
    fun query(uri: Uri): Maybe<AMResultItem>

    /**
     * @see [android.content.ContentResolver.getType]
     */
    fun getType(uri: Uri): MimeType?

    /**
     * Reloads all active streams
     */
    fun refresh()
}

class LocalResourceException(override val message: String?) : Exception(message)
