package com.audiomack.data.queue

import com.audiomack.BuildConfig
import com.audiomack.data.ads.AdsWizzManager
import com.audiomack.data.ads.AudioAdManager
import com.audiomack.data.ads.AudioAdState
import com.audiomack.data.bookmarks.BookmarkManager
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.player.PlayerRepository
import com.audiomack.data.queue.QueueDataSource.Companion.CURRENT_INDEX
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.model.AMBookmarkStatus
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.model.NextPageData
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.home.AlertManager
import com.audiomack.ui.home.AlertTriggers
import com.audiomack.utils.ObservableBoolean
import com.audiomack.utils.ObservableInt
import com.audiomack.utils.ObservableList
import com.audiomack.utils.addTo
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.math.max
import timber.log.Timber

class QueueRepository private constructor(
    private val playerDataSource: PlayerDataSource,
    private val bookmarkManager: BookmarkManager,
    private val audioAdManager: AudioAdManager,
    private val remoteVariablesProvider: RemoteVariablesProvider,
    private val alertTriggers: AlertTriggers,
    private val schedulersProvider: SchedulersProvider
) : QueueDataSource {

    private val _index = ObservableInt()
    override val index get() = _index.value

    private val _items = ObservableList<AMResultItem>()
    override val items get() = _items.value

    private var _order = ObservableList<Int>()
    override val order: List<Int> get() = _order.value

    private val _shuffle = ObservableBoolean()
    override val shuffle: Boolean get() = _shuffle.value

    private val _currentItem: Subject<AMResultItem> = BehaviorSubject.create()
    override val currentItem: AMResultItem?
        get() = order.elementAtOrNull(index)?.let { items.elementAtOrNull(it) }

    private var _currentTrackFromBookmarks = false
    override val currentTrackWasRestored: Boolean
        get() = _currentTrackFromBookmarks

    override val atEndOfQueue: Boolean
        get() = index == items.size - 1 && nextPageData == null

    override val bookmarkStatus = BehaviorSubject.create<AMBookmarkStatus>()

    private var bookmarkDisposable: Disposable? = null
    private var updateOrderDisposable: Disposable? = null

    private val disposables: CompositeDisposable = CompositeDisposable()
    private val cancellable: CompositeDisposable = CompositeDisposable()

    private var latestItemId: String? = currentItem?.itemId

    private var _nextPageData: NextPageData? = null
    override val nextPageData: NextPageData? get() = _nextPageData

    private val _orderedItems = BehaviorSubject.create<List<AMResultItem>>()
    override val orderedItems: Observable<List<AMResultItem>>
        get() = _orderedItems.subscribeOn(schedulersProvider.trampoline)

    private val bookmarksEnabled: Boolean
        get() = BuildConfig.AUDIOMACK_DEBUG || remoteVariablesProvider.bookmarksEnabled

    init {
        _items.observable
            .observeOn(schedulersProvider.main)
            .subscribe { syncOrder(it) }
            .addTo(disposables)

        _order.observable
            .subscribeOn(schedulersProvider.trampoline)
            .map { getOrderedItems(items) }
            .filter { it.isNotEmpty() }
            .observeOn(schedulersProvider.main)
            .subscribe(_orderedItems)

        _orderedItems
            .distinctUntilChanged()
            .subscribe { saveBookmarks(it) }
            .also { disposables.add(it) }

        _orderedItems
            .subscribeOn(schedulersProvider.trampoline)
            .filter { latestItemId != null }
            .map { items -> items.indexOfFirst { it.itemId == latestItemId } }
            .filter { it >= 0 }
            .observeOn(schedulersProvider.main)
            .subscribe(_index.observable)

        _index.observable
            .debounce(500L, MILLISECONDS)
            .filter { it >= 0 && it < order.size }
            .map { order[it] }
            .filter { it >= 0 && items.isNotEmpty() && it < items.size }
            .map { items[it] }
            .doOnNext { Timber.tag(TAG).d("Setting current item to $it") }
            .subscribe(_currentItem)

        _currentItem
            .subscribeOn(schedulersProvider.trampoline)
            .observeOn(schedulersProvider.main)
            .subscribe { latestItemId = it.itemId }
            .also { disposables.add(it) }

        restoreBookmarks()
    }

    override fun subscribeToIndex(observer: Observer<Int>) {
        _index.observable
            .subscribeOn(schedulersProvider.trampoline)
            .filter { it >= 0 }
            .observeOn(schedulersProvider.main)
            .subscribe(observer)
    }

    override fun subscribeToOrderedList(observer: Observer<List<AMResultItem>>) {
        _orderedItems
            .subscribeOn(schedulersProvider.trampoline)
            .observeOn(schedulersProvider.main)
            .subscribe(observer)
    }

    override fun subscribeToCurrentItem(observer: Observer<AMResultItem>) {
        _currentItem
            .subscribeOn(schedulersProvider.trampoline)
            .distinctUntilChanged()
            .observeOn(schedulersProvider.main)
            .subscribe(observer)
    }

    override fun subscribeToShuffle(observer: Observer<Boolean>) {
        _shuffle.observable
            .observeOn(schedulersProvider.main)
            .subscribe(observer)
    }

    override fun set(
        items: List<AMResultItem>,
        trackIndex: Int,
        nextPageData: NextPageData?,
        shuffle: Boolean,
        inOfflineScreen: Boolean,
        mixpanelSource: MixpanelSource?,
        fromBookmarks: Boolean,
        allowFrozenTracks: Boolean
    ) {
        Timber.tag(TAG)
            .d("set: size = ${items.size}, index = $trackIndex, nexPageData = $nextPageData")

        if (items.isEmpty()) return

        _nextPageData = nextPageData
        _items.clear()
        _order.clear()
        _index.clear()
        _currentTrackFromBookmarks = fromBookmarks

        // Collection may include album and playlist items, so keep track of the selected id
        latestItemId = if (trackIndex >= 0 && trackIndex < items.size && !shuffle) items[trackIndex].itemId else null

        Single.just(items)
            .subscribeOn(schedulersProvider.io)
            .map { it.flatten(inOfflineScreen, mixpanelSource, allowFrozenTracks) }
            .map { tracks ->
                val i = tracks.indexOfFirst { it.itemId == latestItemId }
                Pair(tracks, i)
            }
            .observeOn(schedulersProvider.main)
            .subscribe({ (tracks, i) ->
                Timber.tag(TAG).d("set: flattened ${tracks.size} tracks; index = $i")
                _shuffle.value = shuffle
                setInternal(tracks)
                _index.value = if (i < 0) 0 else i
            }, { throwable ->
                Timber.tag(TAG).e(throwable)
                // TODO Notify of error
            })
            .also { cancellable.add(it) }
    }

    override fun add(
        items: List<AMResultItem>,
        index: Int?,
        nextPageData: NextPageData?,
        inOfflineScreen: Boolean,
        mixpanelSource: MixpanelSource?,
        allowFrozenTracks: Boolean
    ) {
        Timber.tag(TAG).d("add: ${items.size} items at $index")
        _nextPageData = nextPageData

        val playNext = index == CURRENT_INDEX
        val i = if (playNext) this.index.inc() else index

        Single.just(items)
            .subscribeOn(schedulersProvider.io)
            .map { it.flatten(inOfflineScreen, mixpanelSource, allowFrozenTracks) }
            .observeOn(schedulersProvider.main)
            .subscribe({ tracks ->
                addInternal(tracks, i, playNext)
                alertTriggers.onAddedToQueue()
            }, { throwable ->
                Timber.tag(TAG).e(throwable)
                // TODO Notify of error
            })
            .also { cancellable.add(it) }
    }

    override fun removeAt(index: Int) {
        Timber.tag(TAG).i("removeAt: $index")
        _order.removeAt(index)
    }

    override fun move(from: Int, to: Int) {
        Timber.tag(TAG).i("move: from = $from, to = $to")
        _order.move(from, to)
    }

    override fun clear(fromIndex: Int) {
        if (fromIndex == 0) {
            cancellable.clear()
            _order.clear()
            _items.keepOnly(index)
            _index.value = 0
        } else {
            val i = max(fromIndex, index.inc())
            _items.removeRange(i, _items.size)
        }
        _nextPageData = null
    }

    override fun next() {
        Timber.tag(TAG).d("next() called. current index = $index")
        playAdIfNeeded { nextInternal() }
    }

    private fun playAdIfNeeded(onDone: () -> Unit) {
        Timber.tag(TAG).i("playAdIfNeeded() called : has ad = ${audioAdManager.hasAd}")

        if (!audioAdManager.hasAd) {
            return onDone()
        }

        audioAdManager.play()
            .onErrorReturn(AudioAdState::Error)
            .doOnComplete(onDone)
            .observeOn(schedulersProvider.main)
            .subscribe { if (it is AudioAdState.Error) onDone() }
            .addTo(disposables)
    }

    private fun nextInternal() {
        _currentTrackFromBookmarks = false
        if (index == items.size - 1) {
            if (nextPageData != null) {
                loadNextPage()
            }
        } else {
            val next = index.inc()
            if (next < order.size) {
                _index.value = next
            }
        }
    }

    override fun prev() {
        Timber.tag(TAG).d("prev() called. current index = $index")
        _currentTrackFromBookmarks = false
        if (index > 0) _index.value = index.dec()
    }

    override fun skip(index: Int) {
        Timber.tag(TAG).d("skip: $index")
        _currentTrackFromBookmarks = false
        playAdIfNeeded { _index.value = index }
    }

    override fun isCurrentItemOrParent(item: AMResultItem): Boolean {
        val isSong = !item.isPlaylist && !item.isAlbum
        return currentItem?.let {
            (isSong && item.itemId == it.itemId) || (!isSong && it.parentId == item.itemId)
        } ?: false
    }

    override fun indexOfItem(item: AMResultItem): Int {
        return indexOfItemById(item.itemId)
    }

    private fun indexOfItemById(itemId: String): Int {
        val itemIndex = items.indexOfFirst { it.itemId == itemId }
        return order.indexOfFirst { it == itemIndex }
    }

    private fun setInternal(newItems: List<AMResultItem>) {
        _items.set(newItems)
        setOrder(newItems)
    }

    private fun addInternal(
        newItems: List<AMResultItem>,
        orderIndex: Int? = null,
        nextInQueue: Boolean = false,
        skipNext: Boolean = false
    ) {
        // Add new items after the current item in the unordered list
        val unOrderedIndex = index.takeIf { it >= 0 }?.let { order[it] }
        if (unOrderedIndex != null) {
            _items.addAll(unOrderedIndex.inc(), newItems)
        } else {
            _items.addAll(newItems)
        }

        addToOrder(newItems, orderIndex, nextInQueue)

        if (skipNext) next()
    }

    private fun setOrder(items: List<AMResultItem>) {
        Timber.tag(TAG).d("setOrder: ${items.size} items, shuffle = $shuffle, index = $index")
        if (items.isEmpty()) {
            _order.clear()
            return
        }

        Observable.just(items)
            .subscribeOn(schedulersProvider.computation)
            .map { if (shuffle) it.shuffled() else it }
            .flatMapIterable { it }
            .map { items.indexOf(it) }
            .toList()
            .observeOn(schedulersProvider.main)
            .subscribe { order -> _order.set(order) }
            .addTo(cancellable)
    }

    private fun syncOrder(items: List<AMResultItem>) {
        if (items.isEmpty()) {
            _order.clear()
            return
        }

        Observable.just(items)
            .subscribeOn(schedulersProvider.computation)
            .flatMapIterable { it }
            .map { items.indexOf(it) }
            .toList()
            .observeOn(schedulersProvider.main)
            .subscribe { order -> _order.set(order) }
            .addTo(cancellable)
    }

    private fun addToOrder(newItems: List<AMResultItem>, orderIndex: Int?, nextInQueue: Boolean) {
        Timber.tag(TAG)
            .d("addToOrder: items = $newItems, orderIndex = $orderIndex, nextInQueue = $nextInQueue")

        if (newItems.isEmpty()) return

        val currentOrderedItems = _orderedItems.value ?: return

        // Since items have changed, get the new indexes
        val newOrder = currentOrderedItems.map { items.indexOf(it) }.toMutableList()

        val orderedNewItems = if (shuffle && !nextInQueue) newItems.shuffled() else newItems
        val newItemIndices = orderedNewItems.map { items.indexOf(it) }

        if (orderIndex != null) {
            newOrder.addAll(orderIndex, newItemIndices)
        } else {
            newOrder.addAll(newItemIndices)
        }

        _order.set(newOrder)
    }

    private fun calculateOrder(items: List<AMResultItem>) = Single.create<List<Int>> { emitter ->
        if (items.isEmpty()) {
            emitter.tryOnError(IllegalArgumentException("Unable to calculate order of empty items"))
            return@create
        }

        val orderedIndices = try {
            val orderedItems = mutableListOf<AMResultItem>()
            if (shuffle) {
                val currentItems = items.toMutableList()
                orderedItems.apply {
                    add(currentItems.removeAt(index))
                    addAll(currentItems.shuffled())
                }
            } else {
                orderedItems.addAll(items)
            }
            orderedItems.map { items.indexOf(it) }
        } catch (e: Exception) {
            emitter.tryOnError(e)
            return@create
        }
        if (!emitter.isDisposed) emitter.onSuccess(orderedIndices)
    }

    private fun updateOrder() {
        Timber.tag(TAG).d("updateOrder: ${items.size} items, shuffle = $shuffle, index = $index")
        if (items.isEmpty()) {
            _order.clear()
            return
        }

        updateOrderDisposable?.let { disposables.remove(it) }

        updateOrderDisposable = calculateOrder(_orderedItems.value ?: items)
            .delay(500L, MILLISECONDS)
            .subscribeOn(schedulersProvider.trampoline)
            .observeOn(schedulersProvider.main)
            .subscribe({ _order.set(it) }, { _order.clear() })
            .also { cancellable.add(it) }
    }

    private fun getOrderedItems(items: List<AMResultItem>) = ArrayList<AMResultItem>().apply {
        if (items.isEmpty()) return@apply

        order.forEachIndexed { orderIndex, itemIndex ->
            add(orderIndex, items[itemIndex])
        }
    }

    override fun setShuffle(on: Boolean) {
        Timber.tag(TAG).i("setShuffle() on: $on")
        _shuffle.value = on
        updateOrder()
    }

    override fun restoreBookmarks() {
        Timber.tag(TAG).d("restoreBookmarks() called")
        if (!bookmarksEnabled) return

        Observable.just(bookmarkManager)
            .subscribeOn(schedulersProvider.io)
            .filter { it.statusValid }
            .map { it.allBookmarkedItems }
            .filter { it.isNotEmpty() }
            .observeOn(schedulersProvider.main)
            .subscribe {
                val itemIndex = bookmarkManager.getCurrentBookmarkItemIndex()
                set(it, itemIndex, fromBookmarks = true)
                Timber.tag(TAG)
                    .d("restoreBookmarks: Restored ${it.size} bookmarks; index = $itemIndex")
                restoreBookmarkStatus()
            }
            .also { cancellable.add(it) }
    }

    private fun restoreBookmarkStatus() {
        Observable.just(bookmarkManager)
            .subscribeOn(schedulersProvider.io)
            .map { it.status }
            .observeOn(schedulersProvider.main)
            .subscribe(bookmarkStatus)
    }

    private fun saveBookmarks(list: List<AMResultItem>) {
        Timber.tag(TAG).i("saveBookmarks() list size = ${list.size}")
        if (list.isNotEmpty() && bookmarksEnabled) {
            bookmarkDisposable?.let { disposables.remove(it) }
            bookmarkDisposable = bookmarkManager.deleteAll()
                .subscribeOn(schedulersProvider.io)
                .andThen(Observable.fromIterable(list))
                .map { it.toBookmark().save() }
                .onErrorResumeNext(Observable.empty())
                .doOnComplete { Timber.tag(TAG).d("saveBookmarks() completed") }
                .subscribe()
                .also { cancellable.add(it) }
        }
    }

    private fun loadNextPage() {
        Timber.tag(TAG).d("loadNextPage() called; nextPageData = $nextPageData; index = $index")
        nextPageData?.let { nextPage ->
            playerDataSource.getNextPage(nextPage)
                .toList()
                .map { it.flatten(nextPage.offlineScreen, nextPage.mixpanelSource) }
                .observeOn(schedulersProvider.main)
                .subscribe({ result ->
                    Timber.tag(TAG).d("loadNextPage got ${result.size} items")
                    if (result.isEmpty()) {
                        _nextPageData = null
                    } else {
                        latestItemId = null
                        addInternal(result)
                        next()
                    }
                }, { e ->
                    Timber.tag(TAG).e(e)
                    // TODO Notify UI of inability to download next page data
                })
                .also { cancellable.add(it) }
        }
    }

    companion object {
        private const val TAG = "QueueRepository"

        @Volatile
        private var INSTANCE: QueueRepository? = null

        fun getInstance(
            playerDataSource: PlayerDataSource = PlayerRepository.getInstance(),
            bookmarkManager: BookmarkManager = BookmarkManager,
            audioAdManager: AudioAdManager = AdsWizzManager.getInstance(),
            remoteVariablesProvider: RemoteVariablesProvider = RemoteVariablesProviderImpl(),
            alertTriggers: AlertTriggers = AlertManager,
            schedulersProvider: SchedulersProvider = AMSchedulersProvider()
        ): QueueRepository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: QueueRepository(
                playerDataSource,
                bookmarkManager,
                audioAdManager,
                remoteVariablesProvider,
                alertTriggers,
                schedulersProvider
            ).also { INSTANCE = it }
        }
    }
}
