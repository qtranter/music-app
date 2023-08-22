package com.audiomack.data.queue

import com.audiomack.model.AMBookmarkStatus
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.model.NextPageData
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.subjects.Subject

interface QueueDataSource {

    /**
     * The index of the current item in [items].
     */
    val index: Int

    /**
     * The list of songs in their natural order (track number, added, etc). Items should be
     * considered stale.
     */
    val items: List<AMResultItem>

    /**
     * List of [items] indices representing the playback order, which may be shuffled.
     */
    val order: List<Int>

    /**
     * Emits changes to [items] on the main thread, returned in order defined by [order].
     *
     * This is guaranteed to happen before emissions of [index] and [currentItem]
     */
    val orderedItems: Observable<List<AMResultItem>>

    /**
     * The item at [order] [index]. This item should be considered stale.
     */
    val currentItem: AMResultItem?

    /**
     * Is true when the [order] should be shuffled.
     */
    val shuffle: Boolean

    /**
     * Observers will be notified of the bookmark status when bookmarks are restored.
     */
    val bookmarkStatus: Subject<AMBookmarkStatus>

    /**
     * Returns true when [items] does not contain all items in the queue
     */
    val atEndOfQueue: Boolean

    /**
     * Pagination object containing the url for the next page
     */
    val nextPageData: NextPageData?

    /**
     * Returns true if the current track was restored from bookmarks
     */
    val currentTrackWasRestored: Boolean

    /**
     * The [observer] will be notified of changes to [index] on the main thread. This is guaranteed
     * to happen after emissions of [items] but before [currentItem] emissions.
     */
    fun subscribeToIndex(observer: Observer<Int>)

    /**
     * The [observer] will be notified of changes to [items] on the main thread, returned in order
     * defined by [order]. This is guaranteed to happen before emissions of [index] and [currentItem]
     */
    @Deprecated("Subscribe to orderedItems directly", ReplaceWith("orderedItems"))
    fun subscribeToOrderedList(observer: Observer<List<AMResultItem>>)

    /**
     * The [observer] will be notified of changes to [currentItem] on the main thread.
     */
    fun subscribeToCurrentItem(observer: Observer<AMResultItem>)

    /**
     * [observer] will be notified of changes to [shuffle] on the main thread.
     */
    fun subscribeToShuffle(observer: Observer<Boolean>)

    /**
     * Returns true if the [item] is the [currentItem] or it's parent.
     */
    fun isCurrentItemOrParent(item: AMResultItem): Boolean

    /**
     * Returns the index of the item in [order] or -1 if not found.
     */
    fun indexOfItem(item: AMResultItem): Int

    /**
     * Replaces [QueueDataSource.items] with [items] and sets [index] to [trackIndex].
     */
    fun set(
        items: List<AMResultItem>,
        trackIndex: Int = 0,
        nextPageData: NextPageData? = null,
        shuffle: Boolean = false,
        inOfflineScreen: Boolean = false,
        mixpanelSource: MixpanelSource? = null,
        fromBookmarks: Boolean = false,
        allowFrozenTracks: Boolean = false
    )

    /**
     * Adds [items] to [QueueDataSource.items] at [index].
     */
    fun add(
        items: List<AMResultItem>,
        index: Int? = null,
        nextPageData: NextPageData? = null,
        inOfflineScreen: Boolean = false,
        mixpanelSource: MixpanelSource? = null,
        allowFrozenTracks: Boolean = false
    )

    /**
     * Removes the item from [items] at [index].
     */
    fun removeAt(index: Int)

    /**
     * Moves the item in [items].
     */
    fun move(from: Int, to: Int)

    /**
     * Removes all queue items starting at [fromIndex]
     */
    fun clear(fromIndex: Int = 0)

    /**
     * Increase the index. If changing the index in direct response to a UI event it's better to use
     * [com.audiomack.ui.player.Playback.next].
     */
    fun next()

    /**
     * Decrease the index. If changing the index in direct response to a UI event it's better to use
     * [com.audiomack.ui.player.Playback.prev].
     */
    fun prev()

    /**
     * Set the [QueueDataSource.index] to [index]. If changing the index in direct response to a UI
     * event it's better to use [com.audiomack.ui.player.Playback.skip].
     */
    fun skip(index: Int)

    /**
     * Sets [shuffle] to [on] and notifies observers added by [subscribeToShuffle]
     */
    fun setShuffle(on: Boolean)

    fun restoreBookmarks()

    companion object {
        const val CURRENT_INDEX = -1
    }
}
