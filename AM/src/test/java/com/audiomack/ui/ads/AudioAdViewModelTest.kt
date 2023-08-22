package com.audiomack.ui.ads

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.ads.AudioAdManager
import com.audiomack.data.ads.AudioAdState
import com.audiomack.data.ads.AudioAdState.Done
import com.audiomack.data.ads.AudioAdState.Playing
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class AudioAdViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var audioAdManager: AudioAdManager

    @Mock
    private lateinit var trackingDataSource: TrackingDataSource

    private lateinit var viewModel: AudioAdViewModel

    private val scheduler = TestSchedulersProvider()

    private val audioAdStateObservable = BehaviorSubject.create<AudioAdState>()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        whenever(audioAdManager.adStateObservable).thenReturn(audioAdStateObservable)

        viewModel = AudioAdViewModel(audioAdManager, trackingDataSource, scheduler)
    }

    @After
    fun tearDown() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `audio ad event is true when audio ad is playing`() {
        val observer: Observer<Boolean> = mock()
        viewModel.audioAdEvent.observeForever(observer)

        audioAdStateObservable.onNext(Playing(null))

        verify(observer, times(1)).onChanged(true)
    }

    @Test
    fun `audio ad event is false when audio ad is done playing`() {
        val observer: Observer<Boolean> = mock()
        viewModel.audioAdEvent.observeForever(observer)

        audioAdStateObservable.onNext(Done)

        verify(observer, times(1)).onChanged(false)
    }

    @Test
    fun `audio ad seconds remaining observed`() {
        whenever(audioAdManager.currentDuration).thenReturn(30.0)
        whenever(audioAdManager.currentPlaybackTime).thenReturn(10.0)

        val observer: Observer<Long> = mock()
        viewModel.secondsRemaining.observeForever(observer)

        audioAdStateObservable.onNext(Playing(null))

        scheduler.interval.advanceTimeBy(1L, SECONDS)

        verify(observer, times(1)).onChanged(20L)
    }

    @Test
    fun `upsell event when upsell link is clicked`() {
        val observer: Observer<Boolean> = mock()
        viewModel.upSellClickEvent.observeForever(observer)

        viewModel.onUpSellClick()

        verify(observer, times(1)).onChanged(false)
    }

    @Test
    fun `upsell event that starts trial directly when house ad is clicked`() {
        val observer: Observer<Boolean> = mock()
        viewModel.upSellClickEvent.observeForever(observer)

        viewModel.onStartTrialClick()

        verify(observer, times(1)).onChanged(true)
    }

    @Test
    fun `error from view is tracked`() {
        val throwable = Throwable()
        viewModel.onError(throwable)
        verify(trackingDataSource, times(1)).trackException(throwable)
    }

    @Test
    fun `house ad shown when there is no companion ad`() {
        val observer: Observer<Void> = mock()
        viewModel.showHouseAdEvent.observeForever(observer)

        viewModel.onViewVisible()

        scheduler.interval.advanceTimeBy(AudioAdViewModel.DEFAULT_COMPANION_DELAY, SECONDS)

        verify(observer, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `house ad not shown when there already is a companion ad when view became visible`() {
        val observer: Observer<Void> = mock()
        viewModel.showHouseAdEvent.observeForever(observer)

        viewModel.companionAdDisplayedEvent.postValue(true)
        viewModel.onViewVisible()

        scheduler.interval.advanceTimeBy(AudioAdViewModel.DEFAULT_COMPANION_DELAY, SECONDS)

        verify(observer, never()).onChanged(anyOrNull())
    }

    @Test
    fun `house ad not shown when there a companion ad loads after view became visible`() {
        val observer: Observer<Void> = mock()
        viewModel.showHouseAdEvent.observeForever(observer)

        viewModel.onViewVisible()
        scheduler.interval.advanceTimeBy(1, SECONDS)
        viewModel.companionAdDisplayedEvent.postValue(true)
        scheduler.interval.advanceTimeBy(AudioAdViewModel.DEFAULT_COMPANION_DELAY, SECONDS)

        verify(observer, never()).onChanged(anyOrNull())
    }

    @Test
    fun `companion ad display event fires true when ad displayed`() {
        val observer: Observer<Boolean> = mock()
        viewModel.companionAdDisplayedEvent.observeForever(observer)

        viewModel.onCompanionAdDisplayed()

        verify(observer, times(1)).onChanged(true)
    }

    @Test
    fun `companion ad display event fires false when ad display ends`() {
        val observer: Observer<Boolean> = mock()
        viewModel.companionAdDisplayedEvent.observeForever(observer)

        viewModel.onCompanionAdEnded()

        verify(observer, times(1)).onChanged(false)
    }
}
