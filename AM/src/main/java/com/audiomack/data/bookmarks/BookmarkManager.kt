package com.audiomack.data.bookmarks

import android.database.sqlite.SQLiteException
import android.text.TextUtils
import com.activeandroid.query.Delete
import com.activeandroid.query.Select
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.remotevariables.datasource.FirebaseRemoteVariablesDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.model.AMBookmarkItem
import com.audiomack.model.AMBookmarkStatus
import com.audiomack.model.AMResultItem
import io.reactivex.Completable
import io.reactivex.Observable
import java.util.ArrayList
import java.util.Date
import kotlin.math.max
import timber.log.Timber

interface BookmarkDataSource {
    val statusValid: Boolean
    val bookmarkedItemsCount: Int
    val allBookmarkedItems: List<AMResultItem>
    fun getStatusAsync(): Observable<AMBookmarkStatus>
    fun deleteAll(): Completable
    fun updateStatus(currentItemId: String?, playbackPosition: Int)
}

object BookmarkManager : BookmarkDataSource {

    private var lastPlaybackData: Pair<String?, String?>? = null

    private val TAG = BookmarkManager::class.java.simpleName

    private val trackingDataSource: TrackingDataSource = TrackingRepository()

    val status: AMBookmarkStatus?
        get() = Select().from(AMBookmarkStatus::class.java).executeSingle()

    override val statusValid: Boolean
        get() = status?.isValid ?: false

    override val allBookmarkedItems: List<AMResultItem>
        get() {
            val bookmarkedItems =
                Select().from(AMBookmarkItem::class.java).execute<AMBookmarkItem>()
            val items = ArrayList<AMResultItem>(bookmarkedItems.size)
            for (bookmarkedItem in bookmarkedItems) {
                val dbItem = AMResultItem.findById(bookmarkedItem.itemId)
                if (dbItem != null) {
                    items.add(dbItem)
                } else {
                    val amResultItem = AMResultItem().copyFrom(bookmarkedItem)
                    items.add(amResultItem)
                }
            }
            return items
        }

    override val bookmarkedItemsCount: Int
        get() = Select().from(AMBookmarkItem::class.java).count()

    override fun updateStatus(currentItemId: String?, playbackPosition: Int) {
        if (RemoteVariablesProviderImpl(FirebaseRemoteVariablesDataSource).bookmarksEnabled) {
            val playbackPositionString = playbackPosition.toString()
            val needToSave = lastPlaybackData == null || !TextUtils.equals(
                currentItemId,
                lastPlaybackData!!.first
            ) || !TextUtils.equals(playbackPositionString, lastPlaybackData!!.second)
            if (needToSave) {

                lastPlaybackData = Pair(currentItemId, playbackPositionString)

                try {
                    Thread {
                        var status: AMBookmarkStatus? =
                            status
                        if (status == null) {
                            status = AMBookmarkStatus()
                        }
                        status.currentItemId = currentItemId
                        status.playbackPosition = playbackPosition
                        status.bookmarkDate = Date()

                        try {
                            status.save()
                        } catch (e: SQLiteException) {
                            trackingDataSource.trackException(e)
                        }

                        Timber.tag(TAG).d("Saved bookmark status: " + status.currentItemId + " - " + status.playbackPosition)
                    }.start()
                } catch (e: OutOfMemoryError) {
                    System.gc()
                    trackingDataSource.trackException(Exception("Run out of memory while calling updateStatus on AMBookmarkStatus, called System.gc()"))
                }
            } else {
                Timber.tag(TAG).d("Skipped saving playback position: $currentItemId - $playbackPosition")
            }
        }
    }

    // TODO consider making this private
    fun deleteStatus() {
        Delete().from(AMBookmarkStatus::class.java).execute<AMBookmarkStatus>()
    }

    // TODO consider making this private
    fun deleteAllBookmarkedItems() {
        try {
            Delete().from(AMBookmarkItem::class.java).execute<AMBookmarkItem>()
        } catch (e: SQLiteException) {
            trackingDataSource.trackException(e)
        }
    }

    override fun getStatusAsync(): Observable<AMBookmarkStatus> {
        return Observable.create { emitter ->
            status?.let {
                emitter.onNext(it)
                emitter.onComplete()
            } ?: run {
                emitter.onError(Exception("Status not found"))
            }
        }
    }

    override fun deleteAll(): Completable = Completable.fromAction {
        deleteStatus()
        deleteAllBookmarkedItems()
    }

    fun getCurrentBookmarkItemIndex(): Int {
        val currentBookmarkItem = status?.currentItemId
        val bookmarkedItems =
            allBookmarkedItems
        return max(0, bookmarkedItems.indexOfFirst { currentBookmarkItem == it.itemId })
    }
}
