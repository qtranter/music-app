package com.audiomack.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.SleepTimerPromptStatus
import com.audiomack.data.premium.PremiumDataSource
import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class ShowSleepTimerPromptUseCaseTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var sut: ShowSleepTimerPromptUseCaseImpl

    @Mock
    private lateinit var adsDataSource: AdsDataSource

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Mock
    private lateinit var preferencesDataSource: PreferencesDataSource

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = ShowSleepTimerPromptUseCaseImpl(adsDataSource, premiumDataSource, preferencesDataSource)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `not needed because it's a fresh install`() {
        whenever(adsDataSource.isFreshInstall()).thenReturn(true)
        whenever(preferencesDataSource.sleepTimerPromptStatus).thenReturn(SleepTimerPromptStatus.Unknown)
        sut.getPromptMode()
            .test()
            .assertNoValues()
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `not needed because it's already shown`() {
        whenever(adsDataSource.isFreshInstall()).thenReturn(false)
        whenever(preferencesDataSource.sleepTimerPromptStatus).thenReturn(SleepTimerPromptStatus.Shown)
        sut.getPromptMode()
            .test()
            .assertNoValues()
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `not needed because it's already skipped`() {
        whenever(adsDataSource.isFreshInstall()).thenReturn(false)
        whenever(preferencesDataSource.sleepTimerPromptStatus).thenReturn(SleepTimerPromptStatus.Skipped)
        sut.getPromptMode()
            .test()
            .assertNoValues()
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `needed because it's not already shown, locked mode when user is not premium`() {
        whenever(adsDataSource.isFreshInstall()).thenReturn(false)
        whenever(preferencesDataSource.sleepTimerPromptStatus).thenReturn(SleepTimerPromptStatus.NotShown)
        whenever(premiumDataSource.isPremium).thenReturn(false)
        sut.getPromptMode()
            .test()
            .assertValue(SleepTimerPromptMode.Locked)
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `needed because it's not already shown, unlocked mode when user is premium`() {
        whenever(adsDataSource.isFreshInstall()).thenReturn(false)
        whenever(preferencesDataSource.sleepTimerPromptStatus).thenReturn(SleepTimerPromptStatus.NotShown)
        whenever(premiumDataSource.isPremium).thenReturn(true)
        sut.getPromptMode()
            .test()
            .assertValue(SleepTimerPromptMode.Unlocked)
            .assertNoErrors()
            .assertComplete()
    }
}
