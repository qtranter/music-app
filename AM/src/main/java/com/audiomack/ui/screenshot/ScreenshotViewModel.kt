package com.audiomack.ui.screenshot

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.R
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.tracking.mixpanel.MixpanelScreenshotUserCreator
import com.audiomack.data.tracking.mixpanel.MixpanelScreenshotUserFan
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.BenchmarkType
import com.audiomack.model.ScreenshotModel
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import java.util.Timer
import kotlin.concurrent.timerTask
import kotlin.math.abs
import timber.log.Timber

class ScreenshotViewModel(
    private val imageLoader: ImageLoader = PicassoImageLoader,
    private val preferencesDataSource: PreferencesDataSource = PreferencesRepository(),
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel(), BenchmarkAdapter.BenchmarkListener {

    val closeEvent = SingleLiveEvent<Void>()
    val showToastEvent = SingleLiveEvent<Void>()
    val hideToastEvent = SingleLiveEvent<Void>()
    val swipeDownEvent = SingleLiveEvent<Void>()
    val prepareAnimationEvent = SingleLiveEvent<Void>()
    val startAnimationEvent = SingleLiveEvent<Void>()

    private var _title = MutableLiveData<String>()
    val title: LiveData<String> get() = _title

    private var _titleVisible = MutableLiveData<Boolean>()
    val titleVisible: LiveData<Boolean> get() = _titleVisible

    private var _subtitle = MutableLiveData<String>()
    val subtitle: LiveData<String> get() = _subtitle

    private var _musicFeatName = MutableLiveData<String>()
    val musicFeatName: LiveData<String> get() = _musicFeatName

    private var _musicFeatVisible = MutableLiveData<Boolean>()
    val musicFeatVisible: LiveData<Boolean> get() = _musicFeatVisible

    private var _artworkUrl = MutableLiveData<String>()
    val artworkUrl: LiveData<String> get() = _artworkUrl

    private var _artistArtworkUrl = MutableLiveData<String>()
    val artistArtworkUrl: LiveData<String> get() = _artistArtworkUrl

    private var _artworkBitmap = MutableLiveData<Bitmap>()
    val artworkBitmap: LiveData<Bitmap> get() = _artworkBitmap

    private var _artistArtworkBitmap = MutableLiveData<Bitmap>()
    val artistArtworkBitmap: LiveData<Bitmap> get() = _artistArtworkBitmap

    private var _backgroundBitmap = MutableLiveData<Bitmap>()
    val backgroundBitmap: LiveData<Bitmap> get() = _backgroundBitmap

    private var _artistBackgroundBitmap = MutableLiveData<Bitmap>()
    val artistBackgroundBitmap: LiveData<Bitmap> get() = _artistBackgroundBitmap

    private var _closeButtonVisible = MutableLiveData<Boolean>()
    val closeButtonVisible: LiveData<Boolean> get() = _closeButtonVisible

    private var _benchmarkCatalogVisible = MutableLiveData<Boolean>()
    val benchmarkCatalogVisible: LiveData<Boolean> get() = _benchmarkCatalogVisible

    private var _benchmarkViewsVisible = MutableLiveData<Boolean>()
    val benchmarkViewsVisible: LiveData<Boolean> get() = _benchmarkViewsVisible

    private var _verifiedBenchmarkVisible = MutableLiveData<Boolean>()
    val verifiedBenchmarkVisible: LiveData<Boolean> get() = _verifiedBenchmarkVisible

    private var _benchmarkList = MutableLiveData<List<BenchmarkModel>>()
    val benchmarkList: LiveData<List<BenchmarkModel>> get() = _benchmarkList

    private var _benchmarkMilestone = MutableLiveData<BenchmarkModel>()
    val benchmarkMilestone: LiveData<BenchmarkModel> get() = _benchmarkMilestone

    private var _benchmarkTitle = MutableLiveData<Int>()
    val benchmarkTitle: LiveData<Int> get() = _benchmarkTitle

    private var _benchmarkSubtitle = MutableLiveData<Int>()
    val benchmarkSubtitle: LiveData<Int> get() = _benchmarkSubtitle

    private var _benchmarkSubtitleSize = MutableLiveData<Int>()
    val benchmarkSubtitleSize: LiveData<Int> get() = _benchmarkSubtitleSize

    private var _benchmarkIcon = MutableLiveData<Int>()
    val benchmarkIcon: LiveData<Int> get() = _benchmarkIcon

    private var _benchmarkArtistIcon = MutableLiveData<Int?>()
    val benchmarkArtistIcon: LiveData<Int?> get() = _benchmarkArtistIcon

    private var entity: AMResultItem? = null

    private var closeTimer: Timer? = null

    override fun onCleared() {
        super.onCleared()
        closeTimer?.cancel()
    }

    private var model: ScreenshotModel? = null
    private var currentBenchmark: BenchmarkModel? = null

    fun init(model: ScreenshotModel) {
        this.model = model

        model.music?.let { music ->
            _artworkUrl.postValue(music.originalImageUrl)
            _title.postValue(music.artist)
            _subtitle.postValue(music.title)
            _titleVisible.postValue(true)
            _musicFeatName.postValue(music.feat)
            _musicFeatVisible.postValue(music.feat.isNotBlank())
            _artistArtworkUrl.postValue(music.uploaderLargeImage)
            loadMusicInfo(music.id, music.type.typeForMusicApi)
        } ?: model.artist?.let { artist ->
            _artworkUrl.postValue(artist.largeImage)
            _subtitle.postValue(artist.name)
            _titleVisible.postValue(false)
            _musicFeatVisible.postValue(false)
        }

        prepareAnimationEvent.call()
    }

    fun onAnimationComplete() {
        if (!preferencesDataSource.screenshotHintShown) {
            showToastEvent.call()
            preferencesDataSource.screenshotHintShown = true
        } else {
            hideToastEvent.call()
        }
    }

    fun onLoadArtwork(context: Context?, imageUrl: String) {
        imageLoader.load(context, imageUrl)
            .subscribeOn(schedulersProvider.main)
            .observeOn(schedulersProvider.main)
            .subscribe({ bitmap ->
                _artworkBitmap.postValue(bitmap)
            }, {})
            .also { compositeDisposable.add(it) }
    }

    fun onLoadArtistArtwork(context: Context?, imageUrl: String) {
        imageLoader.load(context, imageUrl)
            .subscribeOn(schedulersProvider.main)
            .observeOn(schedulersProvider.main)
            .subscribe({ bitmap ->
                _artistArtworkBitmap.postValue(bitmap)
            }, {})
            .also { compositeDisposable.add(it) }
    }

    fun onLoadBackgroundBlur(context: Context?, imageUrl: String) {
        imageLoader.loadAndBlur(context, imageUrl)
            .subscribeOn(schedulersProvider.main)
            .observeOn(schedulersProvider.main)
            .subscribe({ bitmap ->
                _backgroundBitmap.postValue(bitmap)
                startAnimationEvent.call()
            }, {})
            .also { compositeDisposable.add(it) }
    }

    fun onLoadArtistBackgroundBlur(context: Context?, imageUrl: String) {
        imageLoader.loadAndBlur(context, imageUrl)
            .subscribeOn(schedulersProvider.main)
            .observeOn(schedulersProvider.main)
            .subscribe({ bitmap ->
                _artistBackgroundBitmap.postValue(bitmap)
            }, {})
            .also { compositeDisposable.add(it) }
    }

    fun onFling(
        startY: Float,
        endY: Float,
        velocityY: Float
    ) {
        val minimumSwipeDistance = 120
        val swipeThresholdVelocity = 200
        if (endY - startY > minimumSwipeDistance && abs(velocityY) > swipeThresholdVelocity) {
            swipeDownEvent.call()
        }
    }

    fun onToastCloseClicked() {
        hideToastEvent.call()
    }

    fun onCloseClicked() {
        swipeDownEvent.call()
    }

    fun onHideBenchmarkClicked() {
        _benchmarkCatalogVisible.postValue(false)
    }

    fun onScreenTapped() {
        val benchmarkCatalogCurrentlyVisible = _benchmarkCatalogVisible.value ?: false
        if (benchmarkCatalogCurrentlyVisible || benchmarks.size > 1) {
            _benchmarkCatalogVisible.postValue(benchmarkCatalogCurrentlyVisible.not())
        }
        if (!benchmarkCatalogCurrentlyVisible) {
            _closeButtonVisible.postValue(true)
            closeTimer = Timer().also {
                it.schedule(timerTask { _closeButtonVisible.postValue(false) }, 5000L)
            }
        }
    }

    fun onDownAnimationComplete() {
        closeEvent.call()
    }

    private fun loadMusicInfo(entityId: String?, entityType: String?) {
        val id = entityId ?: return
        val type = entityType ?: return
        musicDataSource.getMusicInfo(id, type)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                entity = it
                _artistArtworkUrl.postValue(it.uploaderLargeImage ?: "")
                if (BenchmarkModel.getBenchmarkList(it).size > 1) {
                    _benchmarkCatalogVisible.postValue(true)
                }
                model?.benchmark?.let {
                    onBenchmarkTapped(it)
                }
            }, { Timber.e(it) })
            .also { compositeDisposable.add(it) }
    }

    override fun onBenchmarkTapped(benchmark: BenchmarkModel) {
        val model = model ?: return

        this.currentBenchmark = benchmark

        val artistViewVisible = (benchmark.type == BenchmarkType.VERIFIED || benchmark.type == BenchmarkType.AUTHENTICATED || benchmark.type == BenchmarkType.TASTEMAKER || benchmark.type == BenchmarkType.ON_AUDIOMACK)
        _benchmarkViewsVisible.postValue(benchmark.type != BenchmarkType.NONE)
        _verifiedBenchmarkVisible.postValue(artistViewVisible)

        val newBenchmarks = benchmarks
        newBenchmarks.forEach {
            it.selected = it.type == benchmark.type
        }
        _benchmarkList.postValue(newBenchmarks)

        if (benchmark.type != BenchmarkType.NONE && !artistViewVisible) {
            _benchmarkMilestone.postValue(benchmark)
        }
        when (benchmark.type) {
            BenchmarkType.PLAY -> {
                _title.postValue(model.music?.artist ?: "")
                _titleVisible.postValue(true)
                _subtitle.postValue(model.music?.title ?: "")
                _benchmarkIcon.postValue(R.drawable.ic_benchmark_play)
                _benchmarkSubtitle.postValue(R.string.benchmark_play)
                _benchmarkSubtitleSize.postValue(R.dimen.benchmark_subtitle_text_size_large)
                _musicFeatVisible.postValue(!_musicFeatName.value.isNullOrBlank())
            }
            BenchmarkType.FAVORITE -> {
                _title.postValue(model.music?.artist ?: "")
                _titleVisible.postValue(true)
                _subtitle.postValue(model.music?.title ?: "")
                _benchmarkIcon.postValue(R.drawable.ic_benchmark_favorite)
                _benchmarkSubtitle.postValue(R.string.benchmark_favorite)
                _benchmarkSubtitleSize.postValue(R.dimen.benchmark_subtitle_text_size_large)
                _musicFeatVisible.postValue(!_musicFeatName.value.isNullOrBlank())
            }
            BenchmarkType.PLAYLIST -> {
                _title.postValue(model.music?.artist ?: "")
                _titleVisible.postValue(true)
                _subtitle.postValue(model.music?.title ?: "")
                _benchmarkIcon.postValue(R.drawable.ic_benchmark_playlist)
                _benchmarkSubtitle.postValue(R.string.benchmark_playlist)
                _benchmarkSubtitleSize.postValue(R.dimen.benchmark_subtitle_text_size_large)
                _musicFeatVisible.postValue(!_musicFeatName.value.isNullOrBlank())
            }
            BenchmarkType.REPOST -> {
                _title.postValue(model.music?.artist ?: "")
                _titleVisible.postValue(true)
                _subtitle.postValue(model.music?.title ?: "")
                _benchmarkIcon.postValue(R.drawable.ic_benchmark_repost)
                _benchmarkSubtitle.postValue(R.string.benchmark_repost)
                _benchmarkSubtitleSize.postValue(R.dimen.benchmark_subtitle_text_size_large)
                _musicFeatVisible.postValue(!_musicFeatName.value.isNullOrBlank())
            }
            BenchmarkType.VERIFIED -> {
                _titleVisible.postValue(false)
                _subtitle.postValue(model.music?.uploaderName ?: "")
                _benchmarkTitle.postValue(R.string.benchmark_verified)
                _benchmarkSubtitle.postValue(R.string.benchmark_artist)
                _benchmarkSubtitleSize.postValue(R.dimen.benchmark_subtitle_text_size_large)
                _benchmarkArtistIcon.postValue(R.drawable.ic_verified)
                _musicFeatVisible.postValue(false)
            }
            BenchmarkType.TASTEMAKER -> {
                _titleVisible.postValue(false)
                _subtitle.postValue(model.music?.uploaderName ?: "")
                _benchmarkTitle.postValue(R.string.benchmark_tastemaker)
                _benchmarkSubtitle.postValue(R.string.benchmark_artist)
                _benchmarkSubtitleSize.postValue(R.dimen.benchmark_subtitle_text_size_large)
                _benchmarkArtistIcon.postValue(R.drawable.ic_tastemaker)
                _musicFeatVisible.postValue(false)
            }
            BenchmarkType.AUTHENTICATED -> {
                _titleVisible.postValue(false)
                _subtitle.postValue(model.music?.uploaderName ?: "")
                _benchmarkTitle.postValue(R.string.benchmark_authenticated)
                _benchmarkSubtitle.postValue(R.string.benchmark_artist)
                _benchmarkSubtitleSize.postValue(R.dimen.benchmark_subtitle_text_size_large)
                _benchmarkArtistIcon.postValue(R.drawable.ic_authenticated)
                _musicFeatVisible.postValue(false)
            }
            BenchmarkType.ON_AUDIOMACK -> {
                _titleVisible.postValue(false)
                _subtitle.postValue(model.music?.uploaderName ?: "")
                _benchmarkTitle.postValue(R.string.benchmark_now)
                _benchmarkSubtitle.postValue(R.string.benchmark_on_audiomack)
                _benchmarkSubtitleSize.postValue(R.dimen.benchmark_subtitle_text_size_small)
                _benchmarkArtistIcon.postValue(benchmark.badgeIconId)
                _musicFeatVisible.postValue(false)
            }
            BenchmarkType.NONE -> {
                _title.postValue(model.music?.artist ?: "")
                _titleVisible.postValue(true)
                _subtitle.postValue(model.music?.title ?: "")
                _musicFeatVisible.postValue(!_musicFeatName.value.isNullOrBlank())
            }
        }
    }

    fun onScreenshotDetected() {
        val model = model ?: return
        val benchmark = currentBenchmark ?: return

        val slug = model.music?.uploaderSlug ?: model.artist?.slug ?: ""

        mixpanelDataSource.trackScreenshot(
            benchmark.prettyTypeForAnalytics,
            if (userDataSource.getUserSlug() == slug) MixpanelScreenshotUserCreator else MixpanelScreenshotUserFan,
            model.artist,
            model.music,
            model.mixpanelSource,
            model.mixpanelButton)
    }

    private val benchmarks: List<BenchmarkModel>
        get() {
            return entity?.let { BenchmarkModel.getBenchmarkList(it) } ?: emptyList()
        }
}
