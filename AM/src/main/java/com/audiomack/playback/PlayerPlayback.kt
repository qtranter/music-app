package com.audiomack.playback

import android.net.Uri
import android.view.View
import androidx.annotation.VisibleForTesting
import com.audiomack.DOWNLOAD_FOLDER
import com.audiomack.SONG_MONETIZATION_SECONDS
import com.audiomack.common.StateEditor
import com.audiomack.common.StateProvider
import com.audiomack.data.ads.AdsWizzManager
import com.audiomack.data.ads.AudioAdManager
import com.audiomack.data.ads.AudioAdState
import com.audiomack.data.bookmarks.BookmarkDataSource
import com.audiomack.data.bookmarks.BookmarkManager
import com.audiomack.data.cache.CachingLayer
import com.audiomack.data.cache.CachingLayerImpl
import com.audiomack.data.cache.isCached
import com.audiomack.data.cache.remove
import com.audiomack.data.database.MusicDAO
import com.audiomack.data.database.MusicDAOImpl
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.player.PlayerRepository
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueException
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.sleeptimer.SleepTimer
import com.audiomack.data.sleeptimer.SleepTimerEvent
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerCleared
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerSet
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerTriggered
import com.audiomack.data.sleeptimer.SleepTimerManager
import com.audiomack.data.storage.Storage
import com.audiomack.data.storage.StorageException
import com.audiomack.data.storage.StorageProvider
import com.audiomack.data.storage.deleteFile
import com.audiomack.data.storage.getFile
import com.audiomack.data.storage.isFileValid
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingProvider
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.appsflyer.AppsFlyerDataSource
import com.audiomack.data.tracking.appsflyer.AppsFlyerRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonNowPlaying
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelPageFeedTimeline
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.model.AMBookmarkStatus
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventPlayPauseChange
import com.audiomack.model.EventSongChange
import com.audiomack.model.MixpanelSource
import com.audiomack.model.SongEndType
import com.audiomack.playback.PlaybackState.ENDED
import com.audiomack.playback.PlaybackState.ERROR
import com.audiomack.playback.PlaybackState.IDLE
import com.audiomack.playback.PlaybackState.LOADING
import com.audiomack.playback.PlaybackState.PAUSED
import com.audiomack.playback.PlaybackState.PLAYING
import com.audiomack.playback.PlayerQueue.Collection
import com.audiomack.playback.PlayerQueue.Song
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.browse.BrowseFragment
import com.audiomack.ui.common.Resource
import com.audiomack.ui.common.Resource.Failure
import com.audiomack.ui.common.Resource.Success
import com.audiomack.ui.common.ResourceException
import com.audiomack.ui.widget.AudiomackWidget
import com.audiomack.utils.SimpleObserver
import com.audiomack.utils.Url
import com.audiomack.utils.addTo
import com.audiomack.utils.isFileUrl
import com.audiomack.utils.isValidUrl
import com.audiomack.utils.isWebUrl
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ParserException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.cast.CastPlayer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

class PlayerPlayback private constructor(
    private val playEventListener: PlayEventListener,
    private val queueDataSource: QueueDataSource,
    private val playerDataSource: PlayerDataSource,
    private val bookmarkManager: BookmarkDataSource,
    private val cachingLayer: CachingLayer,
    private val schedulersProvider: SchedulersProvider,
    private val appsFlyerDataSource: AppsFlyerDataSource,
    private val mixpanelDataSource: MixpanelDataSource,
    private val trackingDataSource: TrackingDataSource,
    private val eventBus: EventBus,
    private val storage: Storage,
    private val musicDAO: MusicDAO,
    private val stateManager: StateEditor<PlaybackState>,
    private val audioAdManager: AudioAdManager,
    private val preferences: PreferencesDataSource,
    sleepTimer: SleepTimer
) : Playback {

    private var player: Player? = null

    private val disposables: CompositeDisposable = CompositeDisposable()
    private val hotDisposables: CompositeDisposable = CompositeDisposable()
    private var loadSongDisposable: Disposable? = null

    override val item = BehaviorSubject.create<PlaybackItem>()
    override val state: StateProvider<PlaybackState> = stateManager
    override val error = PublishSubject.create<PlayerError>()
    override val timer = BehaviorSubject.create<Long>()
    override val downloadRequest = PublishSubject.create<AMResultItem>()
    override val adTimer = PublishSubject.create<Long>()

    override val duration: Long get() = player?.duration ?: 0L
    override val position: Long get() = player?.currentPosition ?: 0L

    override val isPlaying: Boolean
        get() = player?.playbackState == Player.STATE_READY && player?.playWhenReady == true

    override val isEnded: Boolean
        get() = player?.playbackState == Player.STATE_ENDED

    private val isPlayingAd: Boolean
        get() = audioAdManager.adState is AudioAdState.Playing

    private var _repeatType: RepeatType = RepeatType.OFF
        set(value) {
            Timber.tag(TAG).i("repeatType set to $value")
            field = value
            repeatType.onNext(value)
        }
    override var repeatType: Subject<RepeatType> = BehaviorSubject.create()

    override var audioSessionId: Int? = null

    private var _songSkippedManually = true
    override val songSkippedManually: Boolean
        get() = _songSkippedManually

    /**
     * Observes changes to [QueueDataSource.currentItem]. Items in the queue should be considered
     * stale.
     */
    private val currentQueueItemObserver = object : QueueObserver<AMResultItem>() {
        override fun onNext(item: AMResultItem) {
            Timber.tag(TAG).d("currentQueueItemObserver onNext: $item")
            loadSong(item)
        }
    }

    private val urlObserver = object : PlaybackObserver<Resource<Pair<AMResultItem, Url>>>() {
        override fun onNext(resource: Resource<Pair<AMResultItem, Url>>) {
            Timber.tag(TAG).d("urlObserver onNext: $resource, pending play = $pendingPlayWhenReady")

            when (resource) {
                is Success -> {
                    resource.data?.let { (track, url) ->
                        val position = bookmarkedPosition ?: C.TIME_UNSET
                        bookmarkedPosition = null

                        val playWhenReady = player?.playWhenReady == true || pendingPlayWhenReady

                        val uri: Uri = getUri(url) ?: run {
                            Timber.tag(TAG).w("Invalid url: $url")
                            if (url.isWebUrl()) reportUnplayable(track)
                            onResourceError(IOException("Invalid URL"))
                            return@let
                        }

                        if (!isCastPlayer && url.isWebUrl() && track.duration < MAX_DURATION_CACHED_FILE) {
                            cachingLayer.add(uri)
                        }

                        currentItem = PlaybackItem(track, url, uri, position, playWhenReady)
                        pendingPlayWhenReady = false
                        eventBus.post(EventSongChange())

                        trackingDataSource.log(
                            "Starting song playback",
                            mapOf(
                                "Song" to (track.itemId ?: ""),
                                "URL" to (url)
                            )
                        )
                    }
                }
                is Failure -> resource.error?.let { onResourceError(it) }
            }
        }

        override fun onError(e: Throwable) {
            Timber.tag(TAG).e(e, "urlObserver : onError()")
            onResourceError(e)
        }
    }

    /**
     * The position to use when a bookmark item is restored
     */
    private var bookmarkedPosition: Long? = null

    /**
     * The item id of the first bookmark item restored
     */
    private var bookmarkItemId: String? = null

    /**
     * Observes emissions when bookmarks are restored
     */
    private val bookmarkStatusObserver = object : QueueObserver<AMBookmarkStatus>() {
        override fun onNext(status: AMBookmarkStatus) {
            Timber.tag(TAG).d("bookmarkStatusObserver onNext: $status")
            bookmarkItemId = status.currentItemId
            if (status.playbackPosition > 0) {
                bookmarkedPosition = status.playbackPosition.toLong()
            }
        }

        override fun onError(e: Throwable) {
            Timber.tag(TAG).w(e, "Error while observing bookmark status")
        }
    }

    private val sleepTimerEventObserver = object : SimpleObserver<SleepTimerEvent>(hotDisposables) {
        override fun onNext(event: SleepTimerEvent) {
            when (event) {
                is TimerSet -> repeat(RepeatType.ALL)
                is TimerCleared -> repeat(RepeatType.OFF)
                is TimerTriggered -> pause()
            }
        }
    }

    private val url = BehaviorSubject.create<Resource<Pair<AMResultItem, String>>>()

    /**
     * Used to control whether [timer] will receive periodic updates on playback position
     */
    private val timerEnabled: Subject<Boolean> = BehaviorSubject.create()

    /**
     * When true player should start playback as soon as the URL is observed
     */
    private var pendingPlayWhenReady = false

    /**
     * The actual duration of time listened, used for tracking monetization
     */
    private var playbackTime = 0L

    private var currentItem: PlaybackItem? = null
        set(value) {
            field = value?.also { item.onNext(it) }
        }

    private val currentItemId: String?
        get() = currentItem?.track?.itemId

    private val isCastPlayer
        get() = player is CastPlayer

    private var monetizationTimerObserver: Disposable? = null
    private var playEventTimerObserver: Disposable? = null
    private var getAllPagesDisposable: Disposable? = null

    init {
        Timber.tag(TAG).i("init() called")
        initTimer()

        url.subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe(urlObserver)

        playerDataSource.subscribeToUrl(url)

        queueDataSource.apply {
            subscribeToCurrentItem(currentQueueItemObserver)
            bookmarkStatus.subscribe(bookmarkStatusObserver)
        }

        sleepTimer.sleepEvent.subscribe(sleepTimerEventObserver)
    }

    override fun setPlayer(player: Player?) {
        Timber.tag(TAG).i("Player set to ${player?.javaClass?.simpleName}")
        this.player = player?.apply {
            addListener(this@PlayerPlayback)
        }
    }

    override fun isPlayer(player: Player?) = this.player == player

    override fun reload() {
        Timber.tag(TAG).i("reload() called for ${queueDataSource.currentItem}")
        if (player?.playbackState == Player.STATE_IDLE) {
            queueDataSource.currentItem?.let {
                loadSong(it)
            }
        }
    }

    override fun release() {
        Timber.tag(TAG).i("release() called")
        val currentPosition = player?.currentPosition
        if (bookmarkedPosition == null && currentPosition != null && currentPosition > 0) {
            bookmarkedPosition = currentPosition
        }
        player?.removeListener(this)
        stopTimer()
        disposables.clear()
        loadSongDisposable?.dispose()
        pendingPlayWhenReady = false
        playbackTime = 0L
        player = null

        // Pre-load the current item without auto-playback enabled
        currentItem?.copy(
            position = bookmarkedPosition ?: C.TIME_UNSET,
            playWhenReady = false
        )?.let {
            item.onNext(it)
        }
    }

    override fun setQueue(playerQueue: PlayerQueue, play: Boolean) {
        Timber.tag(TAG).i("setQueue(): playerQueue = $playerQueue, play = $play")
        trackingDataSource.trackBreadcrumb("$TAG - new queue")
        reportSongPlayed(queueDataSource.currentItem, SongEndType.ChangedSong)

        _songSkippedManually = true

        val currentSongId = currentItem?.track?.itemId
        val newItem: AMResultItem? = if (playerQueue is Song) {
            playerQueue.item
        } else {
            playerQueue.items.elementAtOrNull(playerQueue.trackIndex)
        }
        val isCurrentSong = newItem != null && newItem.itemId == currentSongId

        if (!isCurrentSong) {
            clearPlayer(playWhenReady = play)
        } else {
            seekTo(0)
        }

        getAllPagesDisposable?.dispose()
        _repeatType = RepeatType.OFF
        bookmarkedPosition = null

        loadQueue(playerQueue)
        if (play) play()
    }

    private fun loadQueue(playerQueue: PlayerQueue) {
        queueDataSource.set(
            playerQueue.items,
            playerQueue.trackIndex,
            if (playerQueue is Collection) playerQueue.nextPageData else null,
            playerQueue.shuffle,
            playerQueue.inOfflineScreen,
            playerQueue.source,
            fromBookmarks = false,
            allowFrozenTracks = playerQueue.allowFrozenTracks
        )
    }

    override fun addQueue(playerQueue: PlayerQueue, index: Int?) {
        Timber.tag(TAG).i("addQueue(): playerQueue = $playerQueue, index = $index")
        trackingDataSource.trackBreadcrumb("$TAG - items added to queue")

        queueDataSource.add(
            playerQueue.items,
            index,
            if (playerQueue is Collection) playerQueue.nextPageData else null,
            playerQueue.inOfflineScreen,
            playerQueue.source,
            playerQueue.allowFrozenTracks
        )
    }

    override fun play() {
        if (isPlayingAd) return

        Timber.tag(TAG).i("play() called")
        trackingDataSource.trackBreadcrumb("$TAG - play $currentItemId")

        if (player == null) {
            pendingPlayWhenReady = true
        } else {
            player?.playWhenReady = true
        }
    }

    override fun pause() {
        if (isPlayingAd) return

        Timber.tag(TAG).i("pause() called")
        trackingDataSource.trackBreadcrumb("$TAG - pause $currentItemId")

        player?.playWhenReady = false
    }

    override fun stop(reset: Boolean) {
        if (isPlayingAd) return

        Timber.tag(TAG).i("stop() called")
        trackingDataSource.trackBreadcrumb("$TAG - stop $currentItemId")

        player?.apply {
            playWhenReady = false
            stop(reset)
        }
        pendingPlayWhenReady = false
    }

    override fun seekTo(position: Long) {
        if (isPlayingAd) return

        val seekable = player?.isCurrentWindowSeekable == true
        Timber.tag(TAG).i("seekTo() called : seekable = $seekable")

        if (!seekable) {
            error.onNext(PlayerError.Seek)
            return
        }

        trackingDataSource.trackBreadcrumb("$TAG - seek to $position for $currentItemId")

        player?.seekTo(position)
        AudiomackWidget.alertWidgetSeekBar(position.toInt())
    }

    override fun next() {
        if (isPlayingAd) return

        Timber.tag(TAG).i("next() called")
        trackingDataSource.trackBreadcrumb("$TAG - next")

        _songSkippedManually = true

        onChangeTrack(SongEndType.Skip)
        if (_repeatType == RepeatType.ONE) {
            repeat(RepeatType.OFF)
        }
        onNext()
    }

    override fun prev() {
        if (isPlayingAd) return

        Timber.tag(TAG).i("prev() called")

        if (position > MIN_SEC_FOR_PREV_SONG * 1000 ||
            queueDataSource.index == 0
        ) {
            seekTo(0)
        } else {
            trackingDataSource.trackBreadcrumb("$TAG - previous")

            _songSkippedManually = true

            onChangeTrack(SongEndType.Skip)
            if (_repeatType == RepeatType.ONE) {
                repeat(RepeatType.OFF)
            }
            queueDataSource.prev()
        }
    }

    override fun skip(index: Int) {
        if (isPlayingAd) return

        Timber.tag(TAG).i("Skip(): index = $index")
        trackingDataSource.trackBreadcrumb("$TAG - skip")

        _songSkippedManually = true

        onChangeTrack(SongEndType.Skip)
        queueDataSource.skip(index)
    }

    override fun onPlayerError(e: ExoPlaybackException) {
        Timber.tag(TAG).i("onPlayerError() called")

        onPlaybackError(e)
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        Timber.tag(TAG).i("onLoadingChanged(): isLoading = $isLoading")
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Timber.tag(TAG).i(
            "onPlayerStateChanged(): playWhenReady = $playWhenReady, ${stateString(playbackState)}"
        )
        when (playbackState) {
            Player.STATE_READY -> onReadyState(playWhenReady)
            Player.STATE_IDLE -> onIdleState()
            Player.STATE_BUFFERING -> onLoadingState(playWhenReady)
            Player.STATE_ENDED -> onEndedState(playWhenReady)
        }
    }

    override fun repeat(repeatType: RepeatType?) {
        _repeatType = repeatType ?: when (_repeatType) {
            RepeatType.ONE -> RepeatType.OFF
            RepeatType.OFF -> RepeatType.ALL
            RepeatType.ALL -> RepeatType.ONE
        }
    }

    private fun hasNext() = (!queueDataSource.atEndOfQueue || _repeatType != RepeatType.OFF)

    private fun onNext() {
        Timber.tag(TAG).i("onNext()")
        when (_repeatType) {
            RepeatType.ALL -> if (queueDataSource.atEndOfQueue) {
                queueDataSource.skip(0)
            } else {
                queueDataSource.next()
            }
            RepeatType.ONE -> onRepeatOne()
            RepeatType.OFF -> queueDataSource.next()
        }
    }

    private fun onRepeatOne() {
        restartMonetizationObserver()
        restartPlayEventObserver()

        // loadUrl is used for tracking playback on the server
        queueDataSource.currentItem?.let { currentItem ->
            playerDataSource.loadUrl(currentItem, skipSession = false, notify = false)
        }
    }

    private fun onChangeTrack(songEndType: SongEndType, playWhenReady: Boolean = true) {
        Timber.tag(TAG).i("onChangeTrack() songEndType = $songEndType, playWhenReady = $playWhenReady)")
        reportSongPlayed(queueDataSource.currentItem, songEndType)

        bookmarkedPosition = null
        pendingPlayWhenReady = playWhenReady

        val shouldRepeat = songEndType == SongEndType.Completed && _repeatType == RepeatType.ONE
        if (shouldRepeat) {
            seekTo(0)
        } else {
            clearPlayer(playWhenReady)
        }
    }

    private fun onReadyState(playWhenReady: Boolean) {
        if (playWhenReady) {
            onPlayState()
        } else {
            onPauseState()
        }

        if (player != null) {
            bookmarkedPosition?.let {
                bookmarkedPosition = null
                seekTo(it)
            } ?: timer.onNext(position)
        }
    }

    private fun onPlayState() {
        Timber.tag(TAG).i("onPlayState()")
        stateManager.value = PLAYING
        startTimer()
        AudiomackWidget.updateCircularLoadingBar(View.GONE)
        AudiomackWidget.alertWidgetStatus(true)
        eventBus.post(EventPlayPauseChange())
    }

    private fun onPauseState() {
        Timber.tag(TAG).i("onPauseState()")
        stateManager.value = PAUSED
        stopTimer()
        AudiomackWidget.updateCircularLoadingBar(View.GONE)
        AudiomackWidget.alertWidgetStatus(false)
        eventBus.post(EventPlayPauseChange())
    }

    private fun onLoadingState(playWhenReady: Boolean) {
        Timber.tag(TAG).i("onLoadingState(): playWhenReady = $playWhenReady")
        stateManager.value = LOADING
        if (playWhenReady) {
            AudiomackWidget.updateCircularLoadingBar(View.VISIBLE)
        }
    }

    private fun onIdleState() {
        Timber.tag(TAG).i("onIdleState()")
        stateManager.value = IDLE
        stopTimer()
        AudiomackWidget.updateCircularLoadingBar(View.GONE)
    }

    private fun onEndedState(playWhenReady: Boolean) {
        Timber.tag(TAG).i("onEndedState(): playWhenReady = $playWhenReady)")
        _songSkippedManually = false
        if (playWhenReady && hasNext()) {
            onChangeTrack(SongEndType.Completed)
            onNext()
        } else {
            if (queueDataSource.atEndOfQueue && !pendingPlayWhenReady && playWhenReady) {
                onQueueCompleted()
            } else {
                timer.onNext(position)
                stateManager.value = ENDED
                stopTimer()
                AudiomackWidget.updateCircularLoadingBar(View.GONE)
            }
        }
    }

    private fun onQueueCompleted() {
        Timber.tag(TAG).i("onQueueCompleted()")
        reportSongPlayed(queueDataSource.currentItem, SongEndType.Completed)
        stop()
        queueDataSource.skip(0)
    }

    private fun loadSong(item: AMResultItem) {
        loadSongDisposable?.dispose()
        Single.fromCallable { item.isDownloadFrozen || item.isLocal }
            .subscribeOn(schedulersProvider.io)
            .onErrorReturnItem(false)
            .flatMap {
                if (it) {
                    logBreadcrumb("No offline check for frozen song ${item.itemId}")
                    Single.just(LoadSongResult.SkipDBQuery(item))
                } else {
                    logBreadcrumb("Checking offline records for song ${item.itemId}")
                    playerDataSource.dbFindById(item.itemId)
                        .singleOrError()
                        .map { LoadSongResult.FoundDBItem(item, it.data) }
                }
            }
            .observeOn(schedulersProvider.io)
            .subscribe({ result ->
                when (result) {
                    is LoadSongResult.SkipDBQuery -> onSongLoaded(result.queueItem, null)
                    is LoadSongResult.FoundDBItem -> onSongLoaded(result.queueItem, result.dbItem)
                }
            }, {
                onResourceError(it)
            })
            .also { loadSongDisposable = it }
    }

    private fun clearPlayer(reset: Boolean = true, playWhenReady: Boolean = true) {
        stateManager.value = LOADING
        stop(reset)
        player?.playWhenReady = playWhenReady
        onLoadingState(playWhenReady)
    }

    private fun onSongLoaded(queueItem: AMResultItem, dbItem: AMResultItem?) {
        Timber.tag(TAG).i("onSongLoaded(): queueItem = $queueItem, dbItem = $dbItem")

        AudiomackWidget.newSong(queueItem)
        restartMonetizationObserver()
        restartPlayEventObserver()

        val skipSession = queueDataSource.currentTrackWasRestored || isCastPlayer

        if (queueItem.isLocal) {
            if (isCastPlayer) {
                next()
                return
            }

            logBreadcrumb("Playing local file ${queueItem.itemId} at ${queueItem.url}")
            url.onNext(Success(Pair(queueItem, queueItem.url)))
            return
        }

        bookmarkManager.updateStatus(queueItem.itemId, 0)

        // Check for offline song
        if (!isCastPlayer && dbItem != null && !dbItem.isCached) {
            logBreadcrumb("Loaded offline record ${dbItem.id} for song ${dbItem.itemId}")

            val file = storage.getFile(dbItem)
            if (file == null) {
                // If the file is null the storage volume is not available
                onStorageError(dbItem.itemId)
                playerDataSource.loadUrl(queueItem, skipSession, true)
                return
            }

            logOfflineFile(dbItem, file)

            if (dbItem.isDownloadCompleted(false) && storage.isFileValid(file)) {
                logBreadcrumb("Playing downloaded file at $file")

                val path = "file://${file.absolutePath}"
                url.onNext(Success(Pair(dbItem, path)))

                // Still fetch the remote URL for tracking purposes
                playerDataSource.loadUrl(dbItem, skipSession, false)
                return
            } else if (!dbItem.isDownloadInProgress && !dbItem.isDownloadQueued) {
                deleteFileAndRetryDownload(dbItem)
            }
        }

        // Check for cached file
        if (!isCastPlayer && cachingLayer.isCached(queueItem)) {
            logBreadcrumb("Playing cached file")

            url.onNext(Success(Pair(queueItem, queueItem.url)))

            // Still fetch the remote URL for tracking purposes
            playerDataSource.loadUrl(queueItem, skipSession, false)
            return
        }

        playerDataSource.loadUrl(queueItem, skipSession, true)
    }

    private fun restartMonetizationObserver() {
        playbackTime = 0
        monetizationTimerObserver?.dispose()
        monetizationTimerObserver =
            timer.skipWhile { playbackTime < SONG_MONETIZATION_SECONDS.times(1000L) }
                .take(1)
                .doOnError { Timber.tag(TAG).w(it) }
                .subscribe {
                    val item = queueDataSource.currentItem ?: return@subscribe
                    if (item.isLocal) return@subscribe
                    Timber.tag(TAG).d("monetizationTimerObserver: tracking monetized play for $it")
                    playerDataSource.trackMonetizedPlay(item)
                    preferences.incrementPlayCount()
                }
                .also { disposables.add(it) }
    }

    private fun restartPlayEventObserver() {
        playEventTimerObserver?.dispose()
        playEventTimerObserver = timer.skipWhile { it < 30000 }
            .take(1)
            .doOnError { Timber.tag(TAG).w(it) }
            .subscribe {
                queueDataSource.currentItem?.let { item ->
                    if (item.isLocal) return@subscribe
                    Timber.tag(TAG).d("playEventTimerObserver: tracking play event for $item")
                    playEventListener.trackPlayEvent(item)
                }
            }
            .also { disposables.add(it) }
    }

    private fun onErrorNext() {
        if (_repeatType == RepeatType.ONE) repeat(RepeatType.OFF)
        onNext()
    }

    private fun onPlaybackError(e: Throwable) {
        Timber.tag(TAG).e(e, "onPlaybackError() called")
        stateManager.value = ERROR
        error.onNext(PlayerError.Playback(e))

        if (e is ExoPlaybackException) {
            player?.stop(true)

            val sourceException = try {
                e.sourceException
            } catch (_: IllegalStateException) {
                Timber.tag(TAG)
                    .w("onPlaybackError caused by a FileDataSourceException and failed to get the sourceException")
                logError(e)
                onErrorNext()
                return
            }.also {
                logError(it)
            }

            val mediaUrl = url.value?.data?.second
            Timber.tag(TAG).w("onPlaybackError caused by a FileDataSourceException for $mediaUrl")
            mediaUrl?.let { logBreadcrumb("Failed to play song with URL: $it") }

            currentItem?.track?.let { track ->
                if (track.isLocal) {
                    logBreadcrumb("Error playing local file")
                    onErrorNext()
                    return@let
                }

                // Delete cached file
                cachingLayer.remove(track).also { removed ->
                    if (removed) logBreadcrumb("Deleted cached file for ${track.itemId}")
                }

                if (track.id != null) deleteFileAndRetryDownload(track)

                if (sourceException is ParserException) {
                    if (mediaUrl.isWebUrl()) reportUnplayable(track)
                    onErrorNext()
                    return@let
                }

                // Restart the song (via streaming)
                playerDataSource.loadUrl(track, skipSession = true, notify = true)
                play()
            }
        } else {
            onErrorNext()
        }
    }

    private fun reportUnplayable(item: AMResultItem) {
        playerDataSource.reportUnplayable(item)
            .observeOn(schedulersProvider.main)
            .subscribe({
                logBreadcrumb("Reported unplayable item ${item.itemId}")
            }, {
                logError(it, "Failed to report unplayable item")
            })
            .addTo(disposables)
    }

    private fun onQueueError(e: Throwable) {
        val throwable = QueueException(e)
        logError(throwable, "onQueueError() called")
        stateManager.value = ERROR
        error.onNext(PlayerError.Queue(throwable))
    }

    private fun onResourceError(e: Throwable) {
        val throwable = ResourceException(e)
        logError(throwable, "onResourceError() called")

        val wasPlaying = player?.playWhenReady == true
        player?.stop(true)
        stateManager.value = ERROR
        error.onNext(PlayerError.Resource(throwable))

        currentItem = null

        if (queueDataSource.items.size == 1) {
            pause()
        } else {
            when (e) {
                is UnknownHostException -> {
                    pause()
                }
                else -> {
                    onChangeTrack(SongEndType.Skip, wasPlaying)
                    onErrorNext()
                }
            }
        }
    }

    private fun onStorageError(itemId: String) {
        val e = StorageException("Storage unavailable or file for item $itemId is null")
        logError(e)
        error.onNext(PlayerError.Storage(e))
    }

    private fun initTimer() {
        // Update bookmark status every 5 seconds of playback
        updateBookmarkStatusPeriodically()

        val timerStep = 100L
        val adTriggerTimestamp = 10L

        timer.filter { position ->
            (isPlaying && position >= adTriggerTimestamp && position < (adTriggerTimestamp + timerStep)) ||
                (playbackTime == adTriggerTimestamp && position >= adTriggerTimestamp && position < adTriggerTimestamp + timerStep)
        }
            .debounce(timerStep, MILLISECONDS)
            .subscribe(adTimer)

        // Notify timer observers during playback
        timerEnabled.distinctUntilChanged()
            .switchMap { on ->
                if (on) Observable.interval(
                    timerStep,
                    MILLISECONDS
                ).observeOn(schedulersProvider.main)
                else Observable.never()
            }
            .map { position }
            .doOnNext { playbackTime += timerStep }
            .observeOn(schedulersProvider.main)
            .subscribe(timer)
    }

    private fun startTimer() {
        Timber.tag(TAG).d("startTimer() called")
        timerEnabled.onNext(true)

        AudiomackWidget.alertWidgetStartTimer(duration)
    }

    private fun stopTimer() {
        Timber.tag(TAG).d("stopTimer() called")
        timerEnabled.onNext(false)

        AudiomackWidget.alertWidgetStopTimer()
    }

    private fun updateBookmarkStatusPeriodically() {
        timer.subscribeOn(schedulersProvider.io)
            .throttleFirst(5, SECONDS)
            .doOnNext { position ->
                queueDataSource.currentItem?.let { item ->
                    if (item.isLocal) return@let // TODO remove when bookmarks support local items
                    bookmarkManager.updateStatus(item.itemId, position.toInt())
                }
            }.subscribe()
    }

    private fun reportSongPlayed(item: AMResultItem?, songEndType: SongEndType) {
        Timber.tag(TAG).i("trackSongPlay() : song = $item, endType = ${songEndType.stringValue()}")
        item?.let { song ->
            if (song.isLocal) return

            val durationPlayed = player?.currentPosition?.div(1000)?.toInt() ?: 0
            val source = item.mixpanelSource ?: MixpanelSource.empty
            Completable.fromAction {
                mixpanelDataSource.trackPlaySong(
                    song,
                    durationPlayed,
                    songEndType,
                    source,
                    MixpanelButtonNowPlaying
                )
                if (MixpanelPageFeedTimeline == source.page) {
                    trackingDataSource.trackEvent(
                        "play_feed",
                        null,
                        listOf(TrackingProvider.Firebase)
                    )
                } else if (BrowseFragment.mixpanelPages.contains(source.page)) {
                    trackingDataSource.trackEvent(
                        "play_browse",
                        null,
                        listOf(TrackingProvider.Firebase)
                    )
                }
                appsFlyerDataSource.trackSongPlay()
            }.subscribeOn(schedulersProvider.io).subscribe()
        }
    }

    private fun getUri(url: Url): Uri? =
        if (url.isFileUrl() && url.contains("/$DOWNLOAD_FOLDER/")) {
            // Downloaded files may have whitespaces and special characters
            try {
                Uri.fromFile(File(url.replace("file://", "")))
            } catch (e: Throwable) {
                null
            }
        } else {
            if (url.isValidUrl()) Uri.parse(url) else null
        }

    /**
     * Clears the saved path, deletes the file, and retries download
     */
    private fun deleteFileAndRetryDownload(item: AMResultItem) {
        Single.just(storage.deleteFile(item))
            .subscribeOn(schedulersProvider.io)
            .flatMap { deleted ->
                if (deleted) logBreadcrumb("Deleted offline file for song ${item.itemId}")
                item.fullPath = null
                musicDAO.save(item)
            }
            .observeOn(schedulersProvider.io)
            .subscribe({
                // Now that the saved path is cleared, first check if the track is already downloaded
                if (item.isDownloadCompleted(false) && storage.isFileValid(item)) {
                    onSongLoaded(item, item)
                    return@subscribe
                }

                retryDownload(item)
            }, {
                logError(it, "Error deleting offline file for song ${item.itemId}")
            })
            .also { disposables.add(it) }
    }

    private fun retryDownload(item: AMResultItem) {
        val props = mapOf(
            "Song id" to (item.itemId ?: ""),
            "Slug" to (item.urlSlug ?: ""),
            "Uploader" to (item.uploaderSlug ?: "")
        )
        trackingDataSource.log("Retrying download", props)
        downloadRequest.onNext(item)
    }

    abstract inner class PlaybackObserver<T> : Observer<T> {
        override fun onComplete() {}
        override fun onSubscribe(d: Disposable) {
            hotDisposables.add(d)
        }
    }

    abstract inner class QueueObserver<T> : PlaybackObserver<T>() {
        override fun onError(e: Throwable) {
            Timber.tag(TAG).e(e, "onError() called for ${this.javaClass.simpleName}")
            onQueueError(e)
        }
    }

    private fun stateString(playbackState: Int) = when (playbackState) {
        Player.STATE_IDLE -> "STATE_IDLE"
        Player.STATE_BUFFERING -> "STATE_BUFFERING"
        Player.STATE_READY -> "STATE_READY"
        Player.STATE_ENDED -> "STATE_ENDED"
        else -> ""
    }

    private fun logBreadcrumb(msg: String) {
        Timber.tag(TAG).d(msg)
        trackingDataSource.trackBreadcrumb(msg)
    }

    private fun logError(e: Throwable, msg: String? = null) {
        Timber.tag(TAG).e(e, msg)
        trackingDataSource.trackException(e)
    }

    private fun logOfflineFile(item: AMResultItem, file: File) {
        val props = mapOf(
            "itemId" to item.itemId,
            "path" to file.absolutePath,
            "exists" to file.exists(),
            "size" to file.length(),
            "readable" to file.canRead(),
            "download completed" to item.isDownloadCompleted(false),
            "downloading" to item.isDownloadInProgress,
            "download queued" to item.isDownloadQueued,
            "valid" to storage.isFileValid(file)
        )
        trackingDataSource.log("Offline file", props)
    }

    sealed class LoadSongResult {
        data class SkipDBQuery(val queueItem: AMResultItem) : LoadSongResult()
        data class FoundDBItem(val queueItem: AMResultItem, val dbItem: AMResultItem?) : LoadSongResult()
    }

    companion object {
        private const val TAG = "PlayerPlayback"

        private const val MIN_SEC_FOR_PREV_SONG = 7
        private const val MAX_DURATION_CACHED_FILE = 1200

        @Volatile
        private var INSTANCE: PlayerPlayback? = null

        fun getInstance(
            playEventListener: PlayEventListener = TrackPlayEventListener(),
            queueDataSource: QueueDataSource = QueueRepository.getInstance(),
            playerDataSource: PlayerDataSource = PlayerRepository.getInstance(),
            bookmarkManager: BookmarkDataSource = BookmarkManager,
            cachingLayer: CachingLayer = CachingLayerImpl.getInstance(),
            schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
            appsFlyerDataSource: AppsFlyerDataSource = AppsFlyerRepository(),
            mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
            trackingDataSource: TrackingDataSource = TrackingRepository(),
            eventBus: EventBus = EventBus.getDefault(),
            storage: Storage = StorageProvider.getInstance(),
            musicDAO: MusicDAO = MusicDAOImpl(),
            stateEditor: StateEditor<PlaybackState> = PlaybackStateManager,
            audioAdManager: AudioAdManager = AdsWizzManager.getInstance(),
            preferences: PreferencesDataSource = PreferencesRepository(),
            sleepTimer: SleepTimer = SleepTimerManager.getInstance()
        ): PlayerPlayback =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: PlayerPlayback(
                        playEventListener,
                        queueDataSource,
                        playerDataSource,
                        bookmarkManager,
                        cachingLayer,
                        schedulersProvider,
                        appsFlyerDataSource,
                        mixpanelDataSource,
                        trackingDataSource,
                        eventBus,
                        storage,
                        musicDAO,
                        stateEditor,
                        audioAdManager,
                        preferences,
                        sleepTimer
                    ).also { INSTANCE = it }
            }

        @VisibleForTesting
        fun destroy() {
            INSTANCE = null
        }
    }
}
