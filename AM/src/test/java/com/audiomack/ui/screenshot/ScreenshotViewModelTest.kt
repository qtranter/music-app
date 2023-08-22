package com.audiomack.ui.screenshot

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.Artist
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.BenchmarkType
import com.audiomack.model.MixpanelSource
import com.audiomack.model.Music
import com.audiomack.model.ScreenshotModel
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class ScreenshotViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var viewModel: ScreenshotViewModel

    @Mock
    private lateinit var imageLoader: ImageLoader

    @Mock
    private lateinit var preferencesDataSource: PreferencesDataSource

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var userDataSource: UserDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    private val motionEventStartNormalY = 50f

    private val motionEventStartCloseY = 150f

    private val motionEventEndY = 200f

    private var velocityNormal: Float = 300.0f

    private var velocitySlow: Float = 100.0f

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        whenever(musicDataSource.getMusicInfo(any(), any())).thenReturn(Observable.error(Exception("")))
        viewModel = ScreenshotViewModel(
            imageLoader,
            preferencesDataSource,
            musicDataSource,
            mixpanelDataSource,
            userDataSource,
            schedulersProvider
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun showToastIfNeeded() {
        whenever(preferencesDataSource.screenshotHintShown).thenReturn(false)
        val observerShowToast: Observer<Void> = mock()
        val observerHideToast: Observer<Void> = mock()
        viewModel.showToastEvent.observeForever(observerShowToast)
        viewModel.hideToastEvent.observeForever(observerHideToast)
        viewModel.onAnimationComplete()
        verify(observerShowToast).onChanged(null)
        verifyZeroInteractions(observerHideToast)
        verify(preferencesDataSource).screenshotHintShown = true
    }

    @Test
    fun showToasNotfNeeded() {
        whenever(preferencesDataSource.screenshotHintShown).thenReturn(true)
        val observerShowToast: Observer<Void> = mock()
        val observerHideToast: Observer<Void> = mock()
        viewModel.showToastEvent.observeForever(observerShowToast)
        viewModel.hideToastEvent.observeForever(observerHideToast)
        viewModel.onAnimationComplete()
        verifyZeroInteractions(observerShowToast)
        verify(observerHideToast).onChanged(null)
    }

    @Test
    fun swipeDownSuccess() {
        val observer: Observer<Void> = mock()
        viewModel.swipeDownEvent.observeForever(observer)
        viewModel.onFling(
            motionEventStartNormalY,
            200f,
            velocityNormal
        )
        verify(observer).onChanged(null)
    }

    @Test
    fun swipeDownFailureSlow() {
        val observer: Observer<Void> = mock()
        viewModel.swipeDownEvent.observeForever(observer)
        viewModel.onFling(
            motionEventStartNormalY,
            motionEventEndY,
            velocitySlow
        )
        verifyZeroInteractions(observer)
    }

    @Test
    fun swipeDownFailureShort() {
        val observer: Observer<Void> = mock()
        viewModel.swipeDownEvent.observeForever(observer)
        viewModel.onFling(
            motionEventStartCloseY,
            motionEventEndY,
            velocityNormal
        )
        verifyZeroInteractions(observer)
    }

    @Test
    fun `load artist name for music`() {
        val artist = "Matteo"
        val observer: Observer<String> = mock()
        viewModel.title.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(artist = artist), artist = null))
        verify(observer).onChanged(eq(artist))
    }

    @Test
    fun `load artwork for music`() {
        val url = "https://bla"
        val observer: Observer<String> = mock()
        viewModel.artistArtworkUrl.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(uploaderLargeImage = url), artist = null))
        verify(observer).onChanged(eq(url))
    }

    @Test
    fun `load music title`() {
        val title = "Noise"
        val observerSongName: Observer<String> = mock()
        val observerSongNameVisible: Observer<Boolean> = mock()
        viewModel.subtitle.observeForever(observerSongName)
        viewModel.titleVisible.observeForever(observerSongNameVisible)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(title = title), artist = null))
        verify(observerSongName).onChanged(eq(title))
        verify(observerSongNameVisible).onChanged(eq(true))
    }

    @Test
    fun `no title for artist`() {
        val observerSongName: Observer<String> = mock()
        val observerSongNameVisible: Observer<Boolean> = mock()
        viewModel.title.observeForever(observerSongName)
        viewModel.titleVisible.observeForever(observerSongNameVisible)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = null, artist = Artist()))
        verifyZeroInteractions(observerSongName)
        verify(observerSongNameVisible).onChanged(eq(false))
    }

    @Test
    fun `load feat for music`() {
        val feat = "bla"
        val observerSongFeatName: Observer<String> = mock()
        val observerSongFeatNameVisible: Observer<Boolean> = mock()
        viewModel.musicFeatName.observeForever(observerSongFeatName)
        viewModel.musicFeatVisible.observeForever(observerSongFeatNameVisible)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(feat = feat), artist = null))
        verify(observerSongFeatName).onChanged(eq(feat))
        verify(observerSongFeatNameVisible).onChanged(eq(true))
    }

    @Test
    fun `load empty feat for music`() {
        val feat = ""
        val observerSongFeatName: Observer<String> = mock()
        val observerSongFeatNameVisible: Observer<Boolean> = mock()
        viewModel.musicFeatName.observeForever(observerSongFeatName)
        viewModel.musicFeatVisible.observeForever(observerSongFeatNameVisible)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(feat = feat), artist = null))
        verify(observerSongFeatName).onChanged(eq(feat))
        verify(observerSongFeatNameVisible).onChanged(eq(false))
    }

    @Test
    fun loadArtworkWithImageViewAndUrl() {
        whenever(imageLoader.load(any(), any())).thenReturn(Single.just(mock()))
        val observer: Observer<Bitmap> = mock()
        viewModel.artworkBitmap.observeForever(observer)
        viewModel.onLoadArtwork(mock(), "https")
        verify(observer).onChanged(any())
    }

    @Test
    fun loadBlurredArtworkWithImageViewAndUrl() {
        whenever(imageLoader.loadAndBlur(any(), any())).thenReturn(Single.just(mock()))
        val observer: Observer<Bitmap> = mock()
        viewModel.backgroundBitmap.observeForever(observer)
        viewModel.onLoadBackgroundBlur(mock(), "https")
        verify(observer).onChanged(any())
    }

    @Test
    fun onToastClicked() {
        val observer: Observer<Void> = mock()
        viewModel.hideToastEvent.observeForever(observer)
        viewModel.onToastCloseClicked()
        verify(observer).onChanged(null)
    }

    @Test
    fun onScreenTappedToShowClose() {
        val observer: Observer<Boolean> = mock()
        viewModel.closeButtonVisible.observeForever(observer)
        viewModel.onScreenTapped()
        verify(observer).onChanged(true)
    }

    @Test
    fun loadArtistArtworkWithImageViewAndUrl() {
        whenever(imageLoader.load(any(), any())).thenReturn(Single.just(mock()))
        val observer: Observer<Bitmap> = mock()
        viewModel.artistArtworkBitmap.observeForever(observer)
        viewModel.onLoadArtistArtwork(mock(), "https")
        verify(observer).onChanged(any())
    }

    @Test
    fun loadBlurredArtistArtworkWithImageViewAndUrl() {
        whenever(imageLoader.loadAndBlur(any(), any())).thenReturn(Single.just(mock()))
        val observer: Observer<Bitmap> = mock()
        viewModel.artistBackgroundBitmap.observeForever(observer)
        viewModel.onLoadArtistBackgroundBlur(mock(), "https")
        verify(observer).onChanged(any())
    }

    @Test
    fun loadBenchmarkMilestone() {
        val benchmark = BenchmarkModel(BenchmarkType.PLAY)
        val observer: Observer<BenchmarkModel> = mock()
        viewModel.benchmarkMilestone.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(benchmark)
    }

    @Test
    fun loadBenchmarkTitle() {
        val benchmark = BenchmarkModel(BenchmarkType.VERIFIED)
        val observer: Observer<Int> = mock()
        viewModel.benchmarkTitle.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(R.string.benchmark_verified)
    }

    @Test
    fun loadBenchmarkSubtitle() {
        val benchmark = BenchmarkModel(BenchmarkType.PLAY)
        val observer: Observer<Int> = mock()
        viewModel.benchmarkSubtitle.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(R.string.benchmark_play)
    }

    @Test
    fun `large subtitle size`() {
        val benchmark = BenchmarkModel(BenchmarkType.PLAY)
        val observer: Observer<Int> = mock()
        viewModel.benchmarkSubtitleSize.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(R.dimen.benchmark_subtitle_text_size_large)
    }

    @Test
    fun `small subtitle size`() {
        val benchmark = BenchmarkModel(BenchmarkType.ON_AUDIOMACK)
        val observer: Observer<Int> = mock()
        viewModel.benchmarkSubtitleSize.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(R.dimen.benchmark_subtitle_text_size_small)
    }

    @Test
    fun loadBenchmarkIcon() {
        val benchmark = BenchmarkModel(BenchmarkType.PLAY)
        val observer: Observer<Int> = mock()
        viewModel.benchmarkIcon.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(R.drawable.ic_benchmark_play)
    }

    @Test
    fun loadBenchmarkVisibleForPlay() {
        val benchmark = BenchmarkModel(BenchmarkType.PLAY)
        val observer: Observer<Boolean> = mock()
        viewModel.benchmarkViewsVisible.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(true)
    }

    @Test
    fun loadBenchmarkVisibleForFavorite() {
        val benchmark = BenchmarkModel(BenchmarkType.FAVORITE)
        val observer: Observer<Boolean> = mock()
        viewModel.benchmarkViewsVisible.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(true)
    }

    @Test
    fun loadBenchmarkVisibleForPlaylist() {
        val benchmark = BenchmarkModel(BenchmarkType.PLAYLIST)
        val observer: Observer<Boolean> = mock()
        viewModel.benchmarkViewsVisible.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(true)
    }

    @Test
    fun loadBenchmarkVisibleForRepost() {
        val benchmark = BenchmarkModel(BenchmarkType.REPOST)
        val observer: Observer<Boolean> = mock()
        viewModel.benchmarkViewsVisible.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(true)
    }

    @Test
    fun loadBenchmarkVisibleForNone() {
        val benchmark = BenchmarkModel(BenchmarkType.NONE)
        val observer: Observer<Boolean> = mock()
        viewModel.benchmarkViewsVisible.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(false)
    }

    @Test
    fun loadBenchmarkVerifiedVisibleForVerified() {
        val benchmark = BenchmarkModel(BenchmarkType.VERIFIED)
        val observer: Observer<Boolean> = mock()
        viewModel.verifiedBenchmarkVisible.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(true)
    }

    @Test
    fun loadBenchmarkVerifiedVisibleForTastemaker() {
        val benchmark = BenchmarkModel(BenchmarkType.TASTEMAKER)
        val observer: Observer<Boolean> = mock()
        viewModel.verifiedBenchmarkVisible.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(true)
    }

    @Test
    fun loadBenchmarkVerifiedVisibleForAuthenticated() {
        val benchmark = BenchmarkModel(BenchmarkType.AUTHENTICATED)
        val observer: Observer<Boolean> = mock()
        viewModel.verifiedBenchmarkVisible.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(true)
    }

    @Test
    fun loadBenchmarkVerifiedVisibleForNone() {
        val benchmark = BenchmarkModel(BenchmarkType.NONE)
        val observer: Observer<Boolean> = mock()
        viewModel.verifiedBenchmarkVisible.observeForever(observer)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)
        verify(observer).onChanged(false)
    }

    @Test
    fun `load music from API updates benchmarks list`() {
        whenever(musicDataSource.getMusicInfo(any(), any())).thenReturn(Observable.just(mock()))
        val observerBenchmarkList: Observer<List<BenchmarkModel>> = mock()
        viewModel.benchmarkList.observeForever(observerBenchmarkList)
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        verify(observerBenchmarkList).onChanged(any())
    }

    @Test
    fun onBenchmarkTapped() {
        val benchmark = BenchmarkModel()
        val observerBenchmark: Observer<List<BenchmarkModel>> = mock()
        val observerBenchmarkVisible: Observer<Boolean> = mock()
        val observerVerifiedBenchmarkVisible: Observer<Boolean> = mock()
        viewModel.benchmarkList.observeForever(observerBenchmark)
        viewModel.benchmarkViewsVisible.observeForever(observerBenchmarkVisible)
        viewModel.verifiedBenchmarkVisible.observeForever(observerVerifiedBenchmarkVisible)

        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark)

        verify(observerBenchmark).onChanged(any())
        verify(observerBenchmarkVisible).onChanged(false)
        verify(observerVerifiedBenchmarkVisible).onChanged(false)
    }

    @Test
    fun onCloseClicked() {
        val observer: Observer<Void> = mock()
        viewModel.swipeDownEvent.observeForever(observer)
        viewModel.onCloseClicked()
        verify(observer).onChanged(null)
    }

    @Test
    fun onHideBenchmarkClicked() {
        val observer: Observer<Boolean> = mock()
        viewModel.benchmarkCatalogVisible.observeForever(observer)
        viewModel.onHideBenchmarkClicked()
        verify(observer).onChanged(false)
    }

    @Test
    fun onDownAnimationComplete() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onDownAnimationComplete()
        verify(observer).onChanged(null)
    }

    @Test
    fun `screenshot now playing`() {
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark = BenchmarkModel(BenchmarkType.NONE))
        viewModel.onScreenshotDetected()
        verify(mixpanelDataSource).trackScreenshot(eq("NowPlaying"), any(), anyOrNull(), anyOrNull(), any(), any())
    }

    @Test
    fun `screenshot adds`() {
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark = BenchmarkModel(BenchmarkType.PLAYLIST, milestone = 666L))
        viewModel.onScreenshotDetected()
        verify(mixpanelDataSource).trackScreenshot(eq("666 Adds"), any(), anyOrNull(), anyOrNull(), any(), any())
    }

    @Test
    fun `screenshot plays`() {
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark = BenchmarkModel(BenchmarkType.PLAY, milestone = 777L))
        viewModel.onScreenshotDetected()
        verify(mixpanelDataSource).trackScreenshot(eq("777 Plays"), any(), anyOrNull(), anyOrNull(), any(), any())
    }

    @Test
    fun `screenshot favs`() {
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark = BenchmarkModel(BenchmarkType.FAVORITE, milestone = 888L))
        viewModel.onScreenshotDetected()
        verify(mixpanelDataSource).trackScreenshot(eq("888 Favorites"), any(), anyOrNull(), anyOrNull(), any(), any())
    }

    @Test
    fun `screenshot reups`() {
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark = BenchmarkModel(BenchmarkType.REPOST, milestone = 999L))
        viewModel.onScreenshotDetected()
        verify(mixpanelDataSource).trackScreenshot(eq("999 Reups"), any(), anyOrNull(), anyOrNull(), any(), any())
    }

    @Test
    fun `screenshot verified artist`() {
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = null, artist = Artist()))
        viewModel.onBenchmarkTapped(benchmark = BenchmarkModel(BenchmarkType.VERIFIED))
        viewModel.onScreenshotDetected()
        verify(mixpanelDataSource).trackScreenshot(eq("Verified Artist"), any(), anyOrNull(), anyOrNull(), any(), any())
    }

    @Test
    fun `screenshot tastemaker artist`() {
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = null, artist = Artist()))
        viewModel.onBenchmarkTapped(benchmark = BenchmarkModel(BenchmarkType.TASTEMAKER))
        viewModel.onScreenshotDetected()
        verify(mixpanelDataSource).trackScreenshot(eq("Tastemaker Artist"), any(), anyOrNull(), anyOrNull(), any(), any())
    }

    @Test
    fun `screenshot authenticated artist`() {
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = null, artist = Artist()))
        viewModel.onBenchmarkTapped(benchmark = BenchmarkModel(BenchmarkType.AUTHENTICATED))
        viewModel.onScreenshotDetected()
        verify(mixpanelDataSource).trackScreenshot(eq("Authenticated Artist"), any(), anyOrNull(), anyOrNull(), any(), any())
    }

    @Test
    fun `screenshot now on audiomack`() {
        viewModel.init(ScreenshotModel(mixpanelSource = MixpanelSource.empty, mixpanelButton = "", music = Music(), artist = null))
        viewModel.onBenchmarkTapped(benchmark = BenchmarkModel(BenchmarkType.ON_AUDIOMACK))
        viewModel.onScreenshotDetected()
        verify(mixpanelDataSource).trackScreenshot(eq("On Audiomack"), any(), anyOrNull(), anyOrNull(), any(), any())
    }
}
