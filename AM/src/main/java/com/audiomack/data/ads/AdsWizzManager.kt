package com.audiomack.data.ads

import android.os.Build
import androidx.annotation.VisibleForTesting
import com.ad.core.adBaseManager.AdData
import com.ad.core.adBaseManager.AdEvent
import com.ad.core.adBaseManager.AdEvent.Type.State.AllAdsCompleted
import com.ad.core.adBaseManager.AdEvent.Type.State.DidStartPlaying
import com.ad.core.adBaseManager.AdEvent.Type.State.FirstAdWillInitialize
import com.ad.core.adBaseManager.AdEvent.Type.State.Initialized
import com.ad.core.adBaseManager.AdEvent.Type.State.PreparingForPlay
import com.ad.core.adBaseManager.AdEvent.Type.State.ReadyForPlay
import com.ad.core.adFetcher.AdRequestConnection
import com.ad.core.adManager.AdManager
import com.ad.core.adManager.AdManagerListener
import com.adswizz.core.adFetcher.AdswizzAdRequest
import com.adswizz.core.adFetcher.AdswizzAdZone
import com.audiomack.BuildConfig
import com.audiomack.common.StateProvider
import com.audiomack.data.ads.AudioAdState.Done
import com.audiomack.data.ads.AudioAdState.Loading
import com.audiomack.data.ads.AudioAdState.None
import com.audiomack.data.ads.AudioAdState.Playing
import com.audiomack.data.ads.AudioAdState.Ready
import com.audiomack.data.logviewer.LogType
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.player.PlayerRepository
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.AMGenre
import com.audiomack.playback.PlaybackState
import com.audiomack.playback.PlaybackStateManager
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.utils.Foreground
import com.audiomack.utils.ForegroundManager
import com.audiomack.utils.addTo
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import timber.log.Timber

/**
 * Provides audio ads from AdsWizz with the following logic:
 *
 * - Timer starts when someone is listening to music and the app is backgrounded.
 * - Once timer reaches 20 minutes, an audio ad is requested.
 * - If user foregrounds app, reset timer to 0
 * - If user pauses playback while in background, pause timer
 * - If user resumes playback while in background, resume timer
 */
class AdsWizzManager(
    private val playbackState: StateProvider<PlaybackState>,
    private val foreground: Foreground,
    private val premiumDataSource: PremiumDataSource,
    private val schedulersProvider: SchedulersProvider,
    private val trackingDataSource: TrackingDataSource,
    private val userDataSource: UserDataSource,
    private val playerDataSource: PlayerDataSource,
    remoteVariablesProvider: RemoteVariablesProvider
) : AudioAdManager, Foreground.Listener, AdManagerListener {

    override val adStateObservable = BehaviorSubject.create<AudioAdState>()

    override val adState: AudioAdState get() = adStateObservable.value ?: None

    private var adManager: AdManager? = null

    /**
     * The amount of time that the user has played since the music last began playback
     */
    private var currentTimestamp: Long? = null

    /**
     * The total amount of time that the user has played music while the app is backgrounded
     */
    private var totalTime: Long = 0L

    private val audioAdsTiming: Long by lazy { remoteVariablesProvider.audioAdsTiming }

    /**
     * The maximum amount of background playback time before an ad should be loaded
     */
    private val maxBackgroundPlaybackTime: Long by lazy {
        MILLISECONDS.convert(audioAdsTiming, SECONDS)
    }

    private val enoughTimePassed: Boolean
        get() = totalTime >= maxBackgroundPlaybackTime

    private val isPlaybackPlaying: Boolean
        get() = playbackState.value == PlaybackState.PLAYING

    private val inBackground: Boolean
        get() = !foreground.isForeground

    private val isPremium: Boolean
        get() = premiumDataSource.isPremium

    private val alreadyLoadingAd: Boolean
        get() = adState is Loading || adState is Ready || adState is Playing

    private var playbackDisposable: Disposable? = null
    private val disposables = CompositeDisposable()

    /**
     * True when the next audio ad should be played as soon as it's done loading
     */
    private var autoPlay = false

    private var ad: AdData? = null
    private var adPlaybackStartTime: Long? = null

    init {
        logAdState()
        subscribeToPremiumStatus()
    }

    override val hasAd: Boolean
        get() = inBackground && !isPremium && adState is Ready && enoughTimePassed

    override val currentDuration: Double
        get() = ad?.duration ?: 0.0

    override val currentPlaybackTime: Double
        get() {
            if (adPlaybackStartTime == null) return 0.0
            val timePlayed = adPlaybackStartTime?.let { System.currentTimeMillis() - it } ?: 0
            if (timePlayed > 0L) {
                return timePlayed.div(1000L).toDouble()
            }
            return 0.0
        }

    override fun onBecameBackground() {
        if (isPlaybackPlaying && !alreadyLoadingAd) {
            currentTimestamp = System.currentTimeMillis()
        }
    }

    override fun onBecameForeground() {
        totalTime = 0L
        currentTimestamp = null
    }

    override fun onEventErrorReceived(adManager: AdManager, ad: AdData?, error: Error) {
        Timber.tag(TAG).e(error.cause, "onEventErrorReceived : ad = ${ad?.mediaUrlString}")
        this.adManager = adManager
        adStateObservable.onNext(AudioAdState.Error(error.cause))
    }

    override fun onEventReceived(adManager: AdManager, event: AdEvent) {
        Timber.tag(TAG).i("onEventReceived : ${event.type.value}")
        this.adManager = adManager
        this.ad = event.ad

        when (event.type) {
            FirstAdWillInitialize, Initialized, PreparingForPlay -> {
                if (adState !is Loading) {
                    adStateObservable.onNext(Loading(ad))
                }
            }
            ReadyForPlay -> {
                adStateObservable.onNext(Ready(ad))
                if (autoPlay) play().also { autoPlay = false }
            }
            DidStartPlaying -> {
                adPlaybackStartTime = System.currentTimeMillis()
                adStateObservable.onNext(Playing(ad))
            }
            AllAdsCompleted -> {
                this.adManager = null
                clearTimers()
                adStateObservable.onNext(Done)
            }
        }
    }

    override fun play(): Observable<AudioAdState> {
        if (isPremium) return Observable.just(Done)

        when (adState) {
            is Ready -> adManager?.play()
            is Playing -> Timber.tag(TAG).w("Already playing an audio ad")
            else -> loadAdIfNecessary(true)
        }
        return adStateObservable.takeUntil { it == Done || it is Error }
    }

    private fun subscribeToPremiumStatus() {
        premiumDataSource.premiumObservable
            .observeOn(schedulersProvider.main)
            .subscribe { premium ->
                foreground.removeListener(this)
                if (premium) {
                    release()
                } else {
                    foreground.addListener(this)
                    subscribeToPlaybackState()
                }
            }.addTo(disposables)
    }

    private fun onPlaybackStateChange(state: PlaybackState) = when (state) {
        PlaybackState.PLAYING -> onPlaybackPlayState()
        PlaybackState.PAUSED -> onPlaybackPauseState()
        else -> Unit
    }

    private fun subscribeToPlaybackState() {
        playbackDisposable?.let { disposables.remove(it) }

        playbackDisposable = playbackState.observable
            .debounce(500L, MILLISECONDS)
            .distinctUntilChanged()
            .observeOn(schedulersProvider.main)
            .onErrorReturnItem(PlaybackState.ERROR)
            .subscribe(::onPlaybackStateChange)
            .addTo(disposables)
    }

    private fun unsubscribeFromPlaybackState() {
        playbackDisposable?.let { disposables.remove(it) }
    }

    private fun onPlaybackPlayState() {
        if (currentTimestamp == null && inBackground) {
            currentTimestamp = System.currentTimeMillis()
            return
        }
        addCurrentToTotalTime()
        currentTimestamp = System.currentTimeMillis()
        loadAdIfNecessary()
    }

    private fun onPlaybackPauseState() {
        addCurrentToTotalTime()
        currentTimestamp = null
        loadAdIfNecessary()
    }

    private fun addCurrentToTotalTime() {
        currentTimestamp?.let { time ->
            totalTime += System.currentTimeMillis() - time
        }
    }

    private fun clearTimers() {
        totalTime = 0L
        currentTimestamp = null
        adPlaybackStartTime = null
    }

    private fun buildAdParameters(): Single<String> = userDataSource.getUserAsync()
        .subscribeOn(schedulersProvider.io)
        .map { user ->
            val params = mutableMapOf<String, String>()
            user.age?.let { age -> params[PARAM_AGE] = age.toString() }
            user.gender?.takeIf { it == AMArtist.Gender.MALE }?.let { params[PARAM_GENDER] = VALUE_GENDER_MALE }
            user.gender?.takeIf { it == AMArtist.Gender.FEMALE }?.let { params[PARAM_GENDER] = VALUE_GENDER_FEMALE }
            params[PARAM_GENRE] = user.amGenre.adsWizzKey
            if (user.amGenre == AMGenre.Podcast) {
                params[PARAM_NAME] = user.name ?: ""
            }
            params
        }
        .onErrorReturn {
            val params = mutableMapOf<String, String>()
            playerDataSource.currentSong?.let { currentSong ->
                params[PARAM_GENRE] = currentSong.amGenre.adsWizzKey
                if (currentSong.amGenre == AMGenre.Podcast) {
                    params[PARAM_NAME] = currentSong.title ?: ""
                }
            }
            params
        }
        .map { params ->
            params
                .map { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
                .joinToString("&")
        }
        .singleOrError()

    private fun buildAdRequest(params: String) = Single.create<AdswizzAdRequest> { emitter ->
        AdswizzAdRequest.Builder()
            .withServer(BuildConfig.AM_ADSWIZZ_SERVER)
            .withZones(setOf(AdswizzAdZone(BuildConfig.AM_ADSWIZZ_ZONE_ID_PREROLL)))
            .withHttpProtocol(AdswizzAdRequest.HttpProtocol.HTTPS)
            .withCompanionZones(BuildConfig.AM_ADSWIZZ_ZONE_ID_DISPLAY)
            .withCustomParameter(params)
            .build { emitter.onSuccess(it) }
    }

    private fun getAdManager(adRequest: AdswizzAdRequest) = Single.create<AdManager> { emitter ->
        Timber.tag(TAG).i("getAdManager() called for request = ${adRequest.uri}")
        val adRequestConnection = AdRequestConnection(adRequest)
        adRequestConnection.requestAds { adManager, error ->
            error?.let {
                emitter.tryOnError(it)
                return@requestAds
            }

            Timber.tag(TAG).d("Connected to AdsWizz ad manager")
            adManager?.let { emitter.onSuccess(it) }
                ?: emitter.onError(Exception("Unable to create AdManager"))
        }
    }

    private fun loadAdIfNecessary(autoPlay: Boolean = false) {
        if (!enoughTimePassed || alreadyLoadingAd || !inBackground) return
        Timber.tag(TAG).i("loadAdIfNecessary(): enough time has passed. Requesting an ad...")

        this.autoPlay = autoPlay
        adStateObservable.onNext(Loading())

        buildAdParameters()
            .flatMap { params -> buildAdRequest(params) }
            .flatMap { getAdManager(it) }
            .subscribe({
                adManager = it.apply {
                    setListener(this@AdsWizzManager)
                    prepare()
                }
            }, {
                Timber.tag(TAG).w(it)
                adStateObservable.onNext(AudioAdState.Error(it))
            }).addTo(disposables)
    }

    private fun logAdState() {
        adStateObservable
            .observeOn(schedulersProvider.main)
            .subscribe {
                when (it) {
                    is Loading -> notifyAdmins("Audio ad requested")
                    is Ready -> notifyAdmins("Audio ad loaded [${it.ad?.id}]")
                    is Playing -> notifyAdmins("Audio ad playback started [${it.ad?.id}]")
                    is Done -> notifyAdmins("Audio ad playback completed")
                    is AudioAdState.Error -> notifyAdmins("Audio ad error : ${it.throwable?.message}")
                    None -> {}
                }
            }.addTo(disposables)
    }

    private fun notifyAdmins(message: String) {
        Timber.tag(LogType.ADS.tag).d(message)
        trackingDataSource.trackBreadcrumb(message)
    }

    private fun release() {
        unsubscribeFromPlaybackState()
        foreground.removeListener(this)
        adManager?.reset()
    }

    companion object {
        private const val TAG = "AdsWizzManager"
        private const val PARAM_GENDER = "aw_0_1st.gender"
        private const val VALUE_GENDER_MALE = "male"
        private const val VALUE_GENDER_FEMALE = "female"
        private const val PARAM_AGE = "aw_0_1st.age"
        private const val PARAM_GENRE = "aw_0_azn.pgenre"
        private const val PARAM_NAME = "aw_0_azn.pname"

        @Volatile
        private var INSTANCE: AdsWizzManager? = null

        @JvmOverloads
        @JvmStatic
        fun getInstance(
            playbackState: StateProvider<PlaybackState> = PlaybackStateManager,
            premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
            foreground: Foreground = ForegroundManager.get(),
            schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
            trackingDataSource: TrackingDataSource = TrackingRepository(),
            userDataSource: UserDataSource = UserRepository.getInstance(),
            playerDataSource: PlayerDataSource = PlayerRepository.getInstance(),
            remoteVariablesProvider: RemoteVariablesProvider = RemoteVariablesProviderImpl()
        ): AudioAdManager {
            if (Build.VERSION.SDK_INT < 23 || !remoteVariablesProvider.audioAdsEnabled) return NoOpAudioAdManager
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdsWizzManager(
                    playbackState,
                    foreground,
                    premiumDataSource,
                    schedulersProvider,
                    trackingDataSource,
                    userDataSource,
                    playerDataSource,
                    remoteVariablesProvider
                ).also { INSTANCE = it }
            }
        }

        @VisibleForTesting
        fun destroy() {
            INSTANCE?.disposables?.clear()
            INSTANCE = null
        }
    }
}

val AMGenre.adsWizzKey: String
    get() = when (this) {
        AMGenre.Classical -> "Classical"
        AMGenre.Country -> "Country"
        AMGenre.Electronic -> "Electronic"
        AMGenre.Jazz -> "Jazz"
        AMGenre.Gospel -> "Religion and Spirituality"
        AMGenre.Rock -> "Rock"
        AMGenre.Podcast -> "Talk"
        AMGenre.Pop -> "Top40/Hits - Pop"
        AMGenre.Rap -> "Urban - Hip-Hop"
        else -> "Music"
    }
