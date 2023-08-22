package com.audiomack.ui.sleeptimer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.sleeptimer.SleepTimer
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.SleepTimerSource
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class SleepTimerViewModelTest {

    private val source = SleepTimerSource.Settings

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Mock
    private lateinit var sleepTimer: SleepTimer

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    private lateinit var viewModel: SleepTimerViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        viewModel = SleepTimerViewModel(
            source,
            premiumDataSource,
            sleepTimer,
            mixpanelDataSource
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `close event on close tap`() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `upgrade event on alarm set when not premium`() {
        whenever(premiumDataSource.isPremium).thenReturn(false)
        val observerUpgrade: Observer<Void> = mock()
        viewModel.upgradeEvent.observeForever(observerUpgrade)

        viewModel.onSetSleepTimerTapped(123)

        verifyZeroInteractions(sleepTimer)
        verify(observerUpgrade).onChanged(null)
        verifyZeroInteractions(mixpanelDataSource)
    }

    @Test
    fun `set sleep timer`() {
        whenever(premiumDataSource.isPremium).thenReturn(true)
        val observerUpgrade: Observer<Void> = mock()
        viewModel.upgradeEvent.observeForever(observerUpgrade)

        viewModel.onSetSleepTimerTapped(123)

        verifyZeroInteractions(observerUpgrade)
        verify(sleepTimer).set(123)
        verify(mixpanelDataSource).trackSleepTimer(source)
    }
}
