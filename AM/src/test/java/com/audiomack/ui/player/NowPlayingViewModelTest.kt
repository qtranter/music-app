package com.audiomack.ui.player

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.device.DeviceDataSource
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.playback.NowPlayingVisibility
import com.audiomack.playback.Playback
import com.audiomack.playback.RepeatType
import com.audiomack.playback.ShuffleState
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibility
import com.audiomack.ui.tooltip.TooltipFragment.TooltipLocation
import com.audiomack.utils.GeneralPreferences
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class NowPlayingViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    lateinit var playback: Playback

    @Mock
    lateinit var generalPreferences: GeneralPreferences

    @Mock
    lateinit var queue: QueueDataSource

    @Mock
    lateinit var dataSource: PlayerDataSource

    @Mock
    lateinit var nowPlayingVisibility: NowPlayingVisibility

    @Mock
    lateinit var playerBottomVisibility: PlayerBottomVisibility

    @Mock
    lateinit var deviceDataSource: DeviceDataSource

    @Mock
    lateinit var premiumDataSource: PremiumDataSource

    private val testShuffleObserver: Observer<ShuffleState> = mock()
    private val testRepeatObserver: Observer<RepeatType> = mock()

    lateinit var viewModel: NowPlayingViewModel

    private val repeatType = PublishSubject.create<RepeatType>()

    private lateinit var premiumObserver: Subject<Boolean>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        whenever(playback.repeatType).thenReturn(repeatType)

        premiumObserver = PublishSubject.create()
        whenever(premiumDataSource.premiumObservable).thenReturn(premiumObserver)

        viewModel = NowPlayingViewModel(
            playback,
            generalPreferences,
            queue,
            dataSource,
            nowPlayingVisibility,
            playerBottomVisibility,
            deviceDataSource,
            premiumDataSource
        ).apply {
            shuffle.observeForever(testShuffleObserver)
            repeat.observeForever(testRepeatObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `eq button visibility set on init`() {
        val observer: Observer<Boolean> = mock()
        viewModel.equalizerEnabled.observeForever(observer)
        verify(observer).onChanged(any())
    }

    @Test
    fun `shuffle changed on shuffle button tap`() {
        whenever(queue.shuffle).thenReturn(false)

        viewModel.onShuffleClick()
        verify(queue).setShuffle(true)
    }

    @Test
    fun `shuffle on state observed when shuffle changes`() {
        viewModel.shuffleObserver.onNext(true)
        verify(testShuffleObserver, times(1)).onChanged(ShuffleState.ON)
    }

    @Test
    fun `shuffle off state observed when shuffle changes`() {
        viewModel.shuffleObserver.onNext(false)
        verify(testShuffleObserver, times(1)).onChanged(ShuffleState.OFF)
    }

    @Test
    fun `playback repeat change on repeat button tapped`() {
        viewModel.onRepeatClick()
        verify(playback, times(1)).repeat()
    }

    @Test
    fun `playback repeat change observed`() {
        viewModel.repeatObserver.onNext(RepeatType.ONE)
        verify(testRepeatObserver).onChanged(RepeatType.ONE)
    }

    @Test
    fun `eq click, premium`() {
        `when`(premiumDataSource.isPremium).thenReturn(true)
        whenever(playback.audioSessionId).thenReturn(1)
        val eqObserver: Observer<Int> = mock()
        viewModel.launchEqEvent.observeForever(eqObserver)
        viewModel.onEqClick()
        verify(eqObserver, times(1)).onChanged(1)
    }

    @Test
    fun `eq click, not premium, show upgrade and complete purchase`() {
        `when`(premiumDataSource.isPremium).thenReturn(false)
        val eqObserver: Observer<Int> = mock()
        val upgradeObserver: Observer<InAppPurchaseMode> = mock()
        viewModel.launchEqEvent.observeForever(eqObserver)
        viewModel.launchUpgradeEvent.observeForever(upgradeObserver)
        viewModel.onEqClick()
        verify(upgradeObserver).onChanged(eq(InAppPurchaseMode.Equalizer))
        `when`(premiumDataSource.isPremium).thenReturn(true)
        premiumObserver.onNext(true)
        verify(eqObserver, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `eq click, not premium, show upgrade and do not complete purchase`() {
        `when`(premiumDataSource.isPremium).thenReturn(false)
        val eqObserver: Observer<Void> = mock()
        val upgradeObserver: Observer<InAppPurchaseMode> = mock()
        viewModel.launchUpgradeEvent.observeForever(upgradeObserver)
        viewModel.onEqClick()
        verify(upgradeObserver).onChanged(eq(InAppPurchaseMode.Equalizer))
        `when`(premiumDataSource.isPremium).thenReturn(false)
        premiumObserver.onNext(false)
        verifyZeroInteractions(eqObserver)
    }

    @Test
    fun `on player visibility change - true, resume banners and pauses home banners`() {
        val value = true
        val playerVisibilityChangedObserver: Observer<Boolean> = mock()
        viewModel.playerVisibilityChangeEvent.observeForever(playerVisibilityChangedObserver)
        viewModel.onPlayerVisibilityChanged(value)
        verify(playerVisibilityChangedObserver).onChanged(eq(value))
    }

    @Test
    fun `on player visibility change - false, resumes banners and pauses player banners`() {
        val value = false
        val playerVisibilityChangedObserver: Observer<Boolean> = mock()
        viewModel.playerVisibilityChangeEvent.observeForever(playerVisibilityChangedObserver)
        viewModel.onPlayerVisibilityChanged(value)
        verify(playerVisibilityChangedObserver).onChanged(eq(value))
    }

    @Test
    fun `on bottom tab selected`() {
        val value = 1
        val bottomTabClickObserver: Observer<Int> = mock()
        viewModel.bottomTabClickEvent.observeForever(bottomTabClickObserver)
        viewModel.onBottomTabSelected(value)
        verify(bottomTabClickObserver).onChanged(eq(value))
    }

    @Test
    fun `bottom visibility change broadcasted`() {
        val changeObserver = mock<Observer<Boolean>>()
        viewModel.bottomVisibilityChangeEvent.observeForever(changeObserver)
        viewModel.onBottomVisibilityChanged(true)
        verify(changeObserver, times(1)).onChanged(true)
    }

    @Test
    fun `song info loaded when bottom is visible`() {
        val item = mock<AMResultItem>()
        whenever(queue.currentItem).thenReturn(item)

        viewModel.onBottomVisibilityChanged(true)

        assertEquals(viewModel.itemLoaded, item)
        verify(dataSource).loadSong(item)
    }

    @Test
    fun `song info not loaded when bottom is not visible`() {
        val item = mock<AMResultItem>()
        whenever(queue.currentItem).thenReturn(item)

        viewModel.onBottomVisibilityChanged(false)

        assertNotEquals(viewModel.itemLoaded, item)
        verifyZeroInteractions(dataSource)
    }

    @Test
    fun `song unloaded on queue item change observed`() {
        viewModel.queueItemObserver.onNext(mock())
        verify(dataSource, times(1)).unloadSong(any())
    }

    @Test
    fun `song loaded when queue item changes and bottom is visible`() {
        val item = mock<AMResultItem>()
        viewModel.bottomVisibilityChangeEvent.value = true

        viewModel.queueItemObserver.onNext(item)

        assertEquals(viewModel.itemLoaded, item)
        verify(dataSource, times(1)).loadSong(item)
    }

    @Test
    fun `song not loaded when queue item changes and bottom is not visible`() {
        val item = mock<AMResultItem>()
        viewModel.bottomVisibilityChangeEvent.value = false

        viewModel.queueItemObserver.onNext(item)

        assertNotEquals(viewModel.itemLoaded, item)
        verify(dataSource, never()).loadSong(any())
    }

    @Test
    fun `on minimized`() {
        val minimizeObserver: Observer<Void> = mock()
        viewModel.onMinimizeEvent.observeForever(minimizeObserver)
        viewModel.onMinimized()
        verify(minimizeObserver).onChanged(null)
    }

    @Test
    fun `scroll to top`() {
        val scrollToTopObserver: Observer<Void> = mock()
        viewModel.scrollToTopEvent.observeForever(scrollToTopObserver)
        viewModel.scrollToTop()
        verify(scrollToTopObserver).onChanged(null)
    }

    @Test
    fun `set maximised`() {
        val value = true
        val maximizeObserver: Observer<Boolean> = mock()
        viewModel.maximizeEvent.observeForever(maximizeObserver)
        viewModel.isMaximized = value
        verify(maximizeObserver).onChanged(eq(value))
    }

    @Test
    fun `tabs visibility changed, visible`() {
        val visibility = true
        viewModel.onTabsVisibilityChanged(visibility)
        verify(playerBottomVisibility).tabsVisible = visibility
    }

    @Test
    fun `scroll tooltip location requested when not previously shown`() {
        whenever(generalPreferences.needToShowPlayerScrollTooltip()).thenReturn(true)
        val observer = mock<Observer<in Void>>()
        viewModel.requestScrollTooltipEvent.observeForever(observer)

        val shown = viewModel.showTooltip()
        verify(observer, times(1)).onChanged(anyOrNull())
        assertTrue(shown)
    }

    @Test
    fun `scroll tooltip location not requested when previously shown`() {
        whenever(generalPreferences.needToShowPlayerScrollTooltip()).thenReturn(false)
        val observer = mock<Observer<in Void>>()
        viewModel.requestScrollTooltipEvent.observeForever(observer)

        val shown = viewModel.showTooltip()
        verify(observer, never()).onChanged(anyOrNull())
        assertFalse(shown)
    }

    @Test
    fun `scroll tooltip location observed`() {
        val observer = mock<Observer<in TooltipLocation>>()
        viewModel.showScrollTooltipEvent.observeForever(observer)
        val location = mock<TooltipLocation>()
        viewModel.setScrollTooltipLocation(location)
        verify(observer, times(1)).onChanged(location)
    }

    @Test
    fun `ads blocked when scroll tooltip shown`() {
        val location = mock<TooltipLocation>()
        viewModel.setScrollTooltipLocation(location)

        val observer = mock<Observer<in Void>>()
        viewModel.blockAdsEvent.observeForever(observer)
        verify(observer, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `eq tooltip location requested when not previously shown and scroll tooltip was shown`() {
        whenever(generalPreferences.needToShowPlayerScrollTooltip()).thenReturn(false)
        whenever(generalPreferences.needToShowPlayerEqTooltip()).thenReturn(true)
        val observer = mock<Observer<in Void>>()
        viewModel.requestEqTooltipEvent.observeForever(observer)

        val shown = viewModel.showTooltip()
        verify(observer, times(1)).onChanged(anyOrNull())
        assertTrue(shown)
    }

    @Test
    fun `eq tooltip location not requested when previously shown and scroll tooltip was shown`() {
        whenever(generalPreferences.needToShowPlayerScrollTooltip()).thenReturn(false)
        whenever(generalPreferences.needToShowPlayerEqTooltip()).thenReturn(false)
        val observer = mock<Observer<in Void>>()
        viewModel.requestEqTooltipEvent.observeForever(observer)

        val shown = viewModel.showTooltip()
        verify(observer, never()).onChanged(anyOrNull())
        assertFalse(shown)
    }

    @Test
    fun `eq tooltip location not requested when scroll tooltip was not shown`() {
        whenever(generalPreferences.needToShowPlayerScrollTooltip()).thenReturn(true)
        val observer = mock<Observer<in Void>>()
        viewModel.requestEqTooltipEvent.observeForever(observer)

        val shown = viewModel.showTooltip()
        verify(observer, never()).onChanged(anyOrNull())
        assertTrue(shown)
    }

    @Test
    fun `eq tooltip location observed`() {
        val observer = mock<Observer<in TooltipLocation>>()
        viewModel.showEqTooltipEvent.observeForever(observer)
        val location = mock<TooltipLocation>()
        viewModel.setEqTooltipLocation(location)
        verify(observer, times(1)).onChanged(location)
    }

    @Test
    fun `ads blocked when eq tooltip shown`() {
        val location = mock<TooltipLocation>()
        viewModel.setEqTooltipLocation(location)

        val observer = mock<Observer<in Void>>()
        viewModel.blockAdsEvent.observeForever(observer)
        verify(observer, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `tooltip dismissal broadcasted`() {
        val observer = mock<Observer<in Void>>()
        viewModel.onTooltipDismissEvent.observeForever(observer)
        viewModel.onTooltipDismissed()
        verify(observer, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `on bottom page selected`() {
        val index = 1
        val observer: Observer<Int> = mock()
        viewModel.bottomPageSelectedEvent.observeForever(observer)
        viewModel.onBottomPageSelected(index)
        verify(observer).onChanged(eq(index))
        verify(playerBottomVisibility).tabIndex = index
    }

    @Test
    fun `on scrollview reached bottom change`() {
        val reachedBottom = true
        viewModel.onScrollViewReachedBottomChange(reachedBottom)
        verify(playerBottomVisibility).reachedBottom = reachedBottom
    }
}
