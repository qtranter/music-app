package com.audiomack.ui.player.full

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.common.State
import com.audiomack.common.StateEditor
import com.audiomack.data.ads.AudioAdManager
import com.audiomack.data.ads.AudioAdState
import com.audiomack.data.bookmarks.BookmarkDataSource
import com.audiomack.data.cache.CachingLayer
import com.audiomack.data.database.MusicDAO
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.sleeptimer.SleepTimer
import com.audiomack.data.sleeptimer.SleepTimerEvent
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerCleared
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerSet
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerTriggered
import com.audiomack.data.storage.Storage
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.appsflyer.AppsFlyerDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.playback.PlayEventListener
import com.audiomack.playback.PlaybackState
import com.audiomack.playback.PlaybackState.IDLE
import com.audiomack.playback.PlayerError
import com.audiomack.playback.PlayerPlayback
import com.audiomack.playback.PlayerQueue
import com.audiomack.playback.RepeatType
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.google.android.exoplayer2.Player
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observer
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import junit.framework.Assert.assertEquals
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class PlayerPlaybackTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var playEventListener: PlayEventListener

    @Mock
    private lateinit var queueDataSource: QueueDataSource

    @Mock
    private lateinit var playerDataSource: PlayerDataSource

    @Mock
    private lateinit var bookmarkManager: BookmarkDataSource

    @Mock
    private lateinit var cachingLayer: CachingLayer

    private lateinit var schedulersProvider: SchedulersProvider

    @Mock
    private lateinit var appsFlyerDataSource: AppsFlyerDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var trackingDataSource: TrackingDataSource

    @Mock
    private lateinit var sleepTimer: SleepTimer

    @Mock
    private lateinit var eventBus: EventBus

    @Mock
    private lateinit var storage: Storage

    @Mock
    private lateinit var _player: Player

    @Mock
    private lateinit var repeatTypeObserver: Observer<RepeatType>

    @Mock
    private lateinit var stateObserver: Observer<PlaybackState>

    @Mock
    private lateinit var errorObserver: Observer<PlayerError>

    @Mock
    private lateinit var musicDAO: MusicDAO

    @Mock
    private lateinit var audioAdManager: AudioAdManager

    @Mock
    private lateinit var preferences: PreferencesDataSource

    private val playbackStateManager: StateEditor<PlaybackState> = State(IDLE)

    private lateinit var sut: PlayerPlayback

    private val audioAdStateObservable = BehaviorSubject.create<AudioAdState>()
    private val sleepTimerEventObservable = PublishSubject.create<SleepTimerEvent>()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()

        whenever(sleepTimer.sleepEvent).thenReturn(BehaviorSubject.create())
        whenever(queueDataSource.bookmarkStatus).thenReturn(BehaviorSubject.create())
        whenever(audioAdManager.adStateObservable).thenReturn(audioAdStateObservable)
        whenever(sleepTimer.sleepEvent).thenReturn(sleepTimerEventObservable)

        sut = PlayerPlayback.getInstance(
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
            playbackStateManager,
            audioAdManager,
            preferences,
            sleepTimer
        ).apply {
            this.setPlayer(_player)
            this.repeatType.subscribe(repeatTypeObserver)
            this.state.observable.subscribe(stateObserver)
            error.subscribe(errorObserver)
        }
    }

    @After
    fun clearMocks() {
        PlayerPlayback.destroy()
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun duration() {
        assert(sut.duration == _player.duration)
    }

    @Test
    fun position() {
        assert(sut.position == _player.currentPosition)
    }

    @Test
    fun `isPlaying true`() {
        whenever(_player.playbackState).thenReturn(Player.STATE_READY)
        whenever(_player.playWhenReady).thenReturn(true)
        assert(sut.isPlaying)
    }

    @Test
    fun `isPlaying false`() {
        whenever(_player.playbackState).thenReturn(Player.STATE_READY)
        whenever(_player.playWhenReady).thenReturn(false)
        assert(!sut.isPlaying)
    }

    @Test
    fun `isEnded true`() {
        whenever(_player.playbackState).thenReturn(Player.STATE_ENDED)
        assert(sut.isEnded)
    }

    @Test
    fun `isEnded false`() {
        whenever(_player.playbackState).thenReturn(Player.STATE_BUFFERING)
        assert(!sut.isEnded)
    }

    @Test
    fun isPlayer() {
        assert(sut.isPlayer(_player))
    }

    @Test
    fun setQueueWithNoCurrentItem() {
        sut.setQueue(PlayerQueue.Song(mock()), true)
        verify(stateObserver, times(0)).onNext(PlaybackState.LOADING)
    }

    @Test
    fun play() {
        sut.play()
        verify(_player).playWhenReady = true
    }

    @Test
    fun pause() {
        sut.pause()
        verify(_player).playWhenReady = false
    }

    @Test
    fun stop() {
        sut.stop()
        verify(_player).stop(true)
    }

    @Test
    fun seek() {
        whenever(_player.isCurrentWindowSeekable).thenReturn(true)
        val position = 3500L
        sut.seekTo(position)
        verify(_player).seekTo(position)
    }

    @Test
    fun `seek error thrown when attempting to seek unseekable media`() {
        whenever(_player.isCurrentWindowSeekable).thenReturn(false)
        sut.seekTo(100L)
        verify(errorObserver, times(1)).onNext(PlayerError.Seek)
        verify(_player, never()).seekTo(any())
    }

    @Test
    fun `given repeat being off, when next is called, player is stopped, reset, and set to play when ready`() {
        whenever(_player.isCurrentWindowSeekable).thenReturn(true)
        sut.next()
        verify(_player).stop(true)
        verify(_player).playWhenReady = true
    }

    @Test
    fun `given repeat set to ONE, on next called, disable repeat ONE mode`() {
        whenever(_player.isCurrentWindowSeekable).thenReturn(true)
        sut.repeat(RepeatType.ONE)
        sut.next()
        verify(repeatTypeObserver, times(1)).onNext(RepeatType.OFF)
    }

    @Test
    fun `given repeat set to ALL, on next called, repeat is not disabled`() {
        whenever(_player.isCurrentWindowSeekable).thenReturn(true)
        sut.repeat(RepeatType.ALL)
        sut.next()
        verify(repeatTypeObserver, never()).onNext(RepeatType.OFF)
    }

    @Test
    fun `given repeat being set to ONE, when song is completed, player seeks to 0`() {
        whenever(_player.isCurrentWindowSeekable).thenReturn(true)
        sut.repeat(RepeatType.ONE)
        sut.onPlayerStateChanged(true, Player.STATE_ENDED)
        verify(_player).seekTo(0)
    }

    @Test
    fun `prev, moves to previous song`() {
        whenever(queueDataSource.index).thenReturn(1)
        sut.prev()
        verify(queueDataSource).prev()
    }

    @Test
    fun `prev, just rewinds current song`() {
        whenever(queueDataSource.index).thenReturn(0)
        whenever(_player.isCurrentWindowSeekable).thenReturn(true)
        sut.prev()
        verify(_player).seekTo(0L)
    }

    @Test
    fun `given repeat mode off, when skip is called, player is stopped and reset and queue is updated`() {
        whenever(_player.isCurrentWindowSeekable).thenReturn(true)
        val index = 0
        sut.skip(index)
        verify(_player).playWhenReady = true
        verify(_player).stop(true)
        verify(queueDataSource).skip(index)
    }

    @Test
    fun release() {
        sut.release()
        verify(_player).removeListener(any())
    }

    @Test
    fun `repeat one`() {
        val repeatType = RepeatType.ONE
        sut.repeat(repeatType)
        verify(repeatTypeObserver).onNext(repeatType)
    }

    @Test
    fun `repeat all`() {
        val repeatType = RepeatType.ALL
        sut.repeat(repeatType)
        verify(repeatTypeObserver).onNext(repeatType)
    }

    @Test
    fun `repeat off`() {
        val repeatType = RepeatType.OFF
        sut.repeat(repeatType)
        verify(repeatTypeObserver).onNext(repeatType)
    }

    @Test
    fun `repeat type turned off when new queue set`() {
        sut.setQueue(PlayerQueue.Song(mock()), true)
        verify(repeatTypeObserver, atLeast(1)).onNext(RepeatType.OFF)
    }

    @Test
    fun `repeat all set when sleep timer set`() {
        sleepTimerEventObservable.onNext(TimerSet(mock()))
        verify(repeatTypeObserver, times(1)).onNext(RepeatType.ALL)
    }

    @Test
    fun `repeat all cleared when sleep timer cleared`() {
        sleepTimerEventObservable.onNext(TimerCleared)
        verify(repeatTypeObserver, times(1)).onNext(RepeatType.OFF)
    }

    @Test
    fun `playback paused when sleep timer triggered`() {
        val player = mock<Player>()
        sut.setPlayer(player)
        sleepTimerEventObservable.onNext(TimerTriggered)
        assertEquals(player.playWhenReady, false)
    }
}
