package com.audiomack.ui.premiumdownload

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.api.MusicDataSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.PremiumDownloadMusicModel
import com.audiomack.model.PremiumDownloadStatsModel
import com.audiomack.model.PremiumLimitedDownloadInfoViewType
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class PremiumDownloadViewModelTest {

    @Mock
    lateinit var backObserver: Observer<Void>

    @Mock
    lateinit var upgradeObserver: Observer<Void>

    @Mock
    lateinit var goToDownloadsObserver: Observer<Void>

    @Mock
    lateinit var openURLObserver: Observer<String>

    @Mock
    lateinit var progressPercentageObserver: Observer<Float>

    @Mock
    lateinit var infoTextObserver: Observer<PremiumDownloadProgressInfo>

    @Mock
    lateinit var firstDownloadLayoutVisibleObserver: Observer<Boolean>

    lateinit var data: PremiumDownloadModel

    lateinit var viewModel: PremiumDownloadViewModel

    @Mock
    lateinit var musicDataSource: MusicDataSource

    lateinit var schedulersProvider: SchedulersProvider

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        schedulersProvider = TestSchedulersProvider()

        val musicModel = PremiumDownloadMusicModel("123", MusicType.Song, 1, emptyList())
        val statsModel = PremiumDownloadStatsModel("", MixpanelSource.empty, 0, 0)
        data = PremiumDownloadModel(musicModel, statsModel, PremiumLimitedDownloadInfoViewType.FirstDownload)

        viewModel = PremiumDownloadViewModel().apply {
            backEvent.observeForever(backObserver)
            upgradeEvent.observeForever(upgradeObserver)
            goToDownloadsEvent.observeForever(goToDownloadsObserver)
            openURLEvent.observeForever(openURLObserver)
            progressPercentage.observeForever(progressPercentageObserver)
            infoText.observeForever(infoTextObserver)
            firstDownloadLayoutVisible.observeForever(firstDownloadLayoutVisibleObserver)

            init(data)
        }

        verify(infoTextObserver).onChanged(any())
        verify(progressPercentageObserver).onChanged(any())
        verify(firstDownloadLayoutVisibleObserver).onChanged(true)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `back click observed`() {
        viewModel.onBackClick()
        verify(backObserver).onChanged(null)
    }

    @Test
    fun `upgrade click observed`() {
        viewModel.onUpgradeClick()
        verify(upgradeObserver).onChanged(null)
    }

    @Test
    fun `go-to-downloads click observed`() {
        viewModel.onGoToDownloadsClick()
        verify(goToDownloadsObserver).onChanged(null)
    }

    @Test
    fun `learn click observed`() {
        viewModel.onLearnClick()
        verify(openURLObserver).onChanged(any())
    }
}
