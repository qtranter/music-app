package com.audiomack.data.ads

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build.VERSION
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import androidx.annotation.MainThread
import com.adcolony.sdk.AdColony
import com.adswizz.core.privacy.GDPRConsent.NOT_ASKED
import com.adswizz.sdk.AdswizzSDK
import com.amazon.device.ads.AdError
import com.amazon.device.ads.AdRegistration
import com.amazon.device.ads.DTBAdCallback
import com.amazon.device.ads.DTBAdRequest
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.DTBAdSize
import com.amazon.device.ads.MRAIDPolicy
import com.applovin.sdk.AppLovinSdk
import com.audiomack.AD_PREFERENCES
import com.audiomack.AD_PREFERENCES_INTERSTITIAL_TIMESTAMP
import com.audiomack.BuildConfig
import com.audiomack.GENERAL_PREFERENCES_PLAY_COUNT
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.ads.AudioAdState.Playing
import com.audiomack.data.ads.ShowInterstitialResult.Dismissed
import com.audiomack.data.ads.ShowInterstitialResult.Failed
import com.audiomack.data.ads.ShowInterstitialResult.Loading
import com.audiomack.data.ads.ShowInterstitialResult.NotShown
import com.audiomack.data.ads.ShowInterstitialResult.Ready
import com.audiomack.data.ads.ShowInterstitialResult.Shown
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.logviewer.LogType
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.remotevariables.datasource.FirebaseRemoteVariablesDataSource
import com.audiomack.data.tracking.appsflyer.AppsFlyerRepository
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.EventToggleRemoveAdVisibility
import com.audiomack.network.AnalyticsHelper
import com.audiomack.network.AnalyticsHelper.TrackingBannerAdListener
import com.audiomack.network.AnalyticsHelper.TrackingInterstitialAdListener
import com.audiomack.network.AnalyticsHelper.TrackingNativeNetworkListener
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.home.HomeActivity
import com.audiomack.usecases.NotifyAdsEventsUseCase
import com.audiomack.usecases.NotifyAdsEventsUseCaseImpl
import com.audiomack.utils.ForegroundManager
import com.audiomack.utils.SecureSharedPreferences
import com.audiomack.utils.addTo
import com.facebook.ads.AudienceNetworkAds
import com.fyber.mediation.mopub.FyberAdapterConfiguration
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.mopub.common.MoPub
import com.mopub.common.SdkConfiguration
import com.mopub.common.logging.MoPubLog
import com.mopub.common.privacy.ConsentDialogListener
import com.mopub.common.privacy.ConsentStatus
import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubInterstitial
import com.mopub.mobileads.MoPubView
import com.mopub.mobileads.MopubAdapterNameManager
import com.mopub.nativeads.AdapterHelper
import com.mopub.nativeads.FacebookAdRenderer
import com.mopub.nativeads.GooglePlayServicesAdRenderer
import com.mopub.nativeads.GooglePlayServicesViewBinder
import com.mopub.nativeads.MediaViewBinder
import com.mopub.nativeads.MoPubNative
import com.mopub.nativeads.MoPubStaticNativeAdRenderer
import com.mopub.nativeads.MoPubVideoNativeAdRenderer
import com.mopub.nativeads.NativeAd
import com.mopub.nativeads.NativeErrorCode
import com.mopub.nativeads.RequestParameters
import com.mopub.nativeads.ViewBinder
import com.mopub.network.Networking
import com.tapjoy.TJConnectListener
import com.tapjoy.Tapjoy
import com.verizon.ads.edition.StandardEdition
import io.presage.Presage
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.Date
import java.util.EnumSet
import java.util.Random
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.math.max
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

object AdProvidersHelper : AdsDataSource, TrackingInterstitialAdListener() {

    private val TAG = AdProvidersHelper::class.java.simpleName

    // Mopub
    @SuppressLint("StaticFieldLeak")
    private var mopubHomeBannerAdView: MoPubView? = null
    @SuppressLint("StaticFieldLeak")
    private var mopub300x250AdView: MoPubView? = null
    @SuppressLint("StaticFieldLeak")
    private var mopubInterstitial: MoPubInterstitial? = null
    @SuppressLint("StaticFieldLeak")
    private var mopubNativeAd: NativeAd? = null
    private var mopub300x250AdLoaded: Boolean = false
    private var nativeAdsAdapterHelper: AdapterHelper? = null
    private val keywords: String
    private val userDataKeywords: String

    private var shutdown: Boolean = false
    private var paused: Boolean = false
    private var homeViewLoaded: Boolean = false
    private var loadingPlayerAd: Boolean = false
    private var showPlayerAdWhenReady: Boolean = false

    private var interstitialTimerActive: Boolean = false
    private val loadIntersitialHandler = Handler()

    private var homeBannerStarted: Boolean = false

    private var currentPremiumStatus: Boolean? = null

    private val random = Random()

    private val appsFlyerDataSource = AppsFlyerRepository()
    private val remoteVariablesProvider = RemoteVariablesProviderImpl(FirebaseRemoteVariablesDataSource)
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
    private val notifyAdsEventsUseCase: NotifyAdsEventsUseCase = NotifyAdsEventsUseCaseImpl()
    private val preferences: PreferencesDataSource = PreferencesRepository()

    private val sharedPreferences = SecureSharedPreferences(MainApplication.context, AD_PREFERENCES)

    private var adSessionStartTimestamp: Long = SystemClock.elapsedRealtime()

    override val interstitialObservable = BehaviorSubject.create<ShowInterstitialResult>()
    private var interstitialAdShownTimestamp: Long? = null
    private var interstitialWasShown = false
    private var interstitialDisposable: Disposable? = null
    private val interstitialIsReady: Boolean
        get() = mopubInterstitial?.isReady == true
    private val interstitialIsLoading: Boolean
        get() = interstitialObservable.value == Loading
    private val interstitialAdSessionPeriod: Long
        get() = remoteVariablesProvider.interstitialSoundOnAdPeriod
    private val interstitialAdFirstPlayDelay: Long
        get() = remoteVariablesProvider.adFirstPlayDelay
    private val interstitialIsVisible: Boolean
        get() = interstitialObservable.value is Shown

    private var freshInstall: Boolean = false

    private val activityComposite = CompositeDisposable()
    private val applicationComposite = CompositeDisposable()
    private var bannerTAMDisposable: Disposable? = null

    private val audioAdManager: AudioAdManager by lazy {
        AdsWizzManager.getInstance()
    }
    private var adsWizzInitialized = false

    private var premiumObserver: Disposable? = null

    private var bannerAPSTimestamp: Long = 0

    private var playCountDisposable: Disposable? = null

    private val withholdAds: Boolean
        get() = preferences.playCount < remoteVariablesProvider.adWithholdPlays

    init {
        calculateKeywords().also {
            keywords = it.first
            userDataKeywords = it.second
        }

        interstitialAdShownTimestamp = 0L
        bannerAPSTimestamp = 0L
        try {
            readFreshInstall()
            sharedPreferences.put(
                AD_PREFERENCES_INTERSTITIAL_TIMESTAMP,
                interstitialAdShownTimestamp.toString()
            )
        } catch (e: Exception) {
            Timber.w(e)
            freshInstall = false
        }

        observeAudioAds()
        observePlayCount()
    }

    override val adsVisible: Boolean
        get() = !PremiumRepository.isPremium()

    private fun start(context: Context) {
        try {
            if (MoPub.isSdkInitialized()) {
                Timber.tag(TAG).d("Mopub already initialised")
                toggle()
            } else {
                Timber.tag(TAG).d("Mopub initialising...")
                // MoPub should be initialized after all adapter SDKs
                Completable.mergeDelayError(
                    listOf(
                        initAppLovin(context),
                        initTapJoy(context),
                        initFacebook(context),
                        initAdColony(context),
                        initVerizon(MainApplication.context!!)
                    )
                ).onErrorComplete() // Always complete, regardless of errors upstream
                    .observeOn(schedulersProvider.main)
                    .subscribe {
                        initMoPub(context)
                    }.addTo(activityComposite)
            }
            shutdown = false
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun initAppLovin(context: Context) = Completable.create { emitter ->
        AppLovinSdk.initializeSdk(context) {
            AppLovinSdk.getInstance(context).settings.isMuted = true
            emitter.onComplete()
        }
    }.subscribeOn(schedulersProvider.io).doOnError { Timber.tag(TAG).w(it) }

    private fun initTapJoy(context: Context) = Completable.create { emitter ->
        Tapjoy.setDebugEnabled(BuildConfig.AUDIOMACK_DEBUG)
        Tapjoy.connect(context, BuildConfig.AM_TAPJOY_SDK_KEY, null, object : TJConnectListener {
            override fun onConnectFailure() {
                emitter.onError(RuntimeException("Tapjoy connection failed"))
            }

            override fun onConnectSuccess() {
                emitter.onComplete()
            }
        })
    }.subscribeOn(schedulersProvider.io).doOnError { Timber.tag(TAG).w(it) }

    private fun initFacebook(context: Context) = Completable.create { emitter ->
        AudienceNetworkAds.buildInitSettings(context)
            .withInitListener {
                if (it.isSuccess) {
                    emitter.onComplete()
                } else {
                    emitter.onError(RuntimeException("Facebook ANA init failed: ${it.message}"))
                }
            }
            .initialize()
    }.subscribeOn(schedulersProvider.io).doOnError { Timber.tag(TAG).w(it) }

    private fun initAdColony(context: Context) = Completable.create { emitter ->
        AdColony.configure(context as Application, BuildConfig.AM_ADCOLONY_APP_ID, BuildConfig.AM_ADCOLONY_INTERSTITIAL_ZONE_ID, BuildConfig.AM_ADCOLONY_INTERSTITIAL_SS_ZONE_ID, BuildConfig.AM_ADCOLONY_BANNER_ZONE_ID)
        emitter.onComplete()
    }.subscribeOn(schedulersProvider.io).doOnError { Timber.tag(TAG).w(it) }

    private fun initVerizon(application: Application) = Completable.create { emitter ->
        StandardEdition.initialize(application, BuildConfig.AM_VERIZON_SITE_ID)
        emitter.onComplete()
    }.subscribeOn(schedulersProvider.main).doOnError { Timber.tag(TAG).w(it) }

    private fun initMoPub(context: Context) {
        // According to Mopub the line below is needed to reduce ANRs
        Networking.getRequestQueue(context)
        val sdkConfiguration = SdkConfiguration.Builder(BuildConfig.AM_MOPUB_BANNER_ID)
            .withLogLevel(if (BuildConfig.AUDIOMACK_DEBUG) MoPubLog.LogLevel.DEBUG else MoPubLog.LogLevel.NONE)
            .withAdditionalNetwork(FyberAdapterConfiguration::class.java.name)
            .withMediatedNetworkConfiguration(FyberAdapterConfiguration::class.java.name, mapOf(
                FyberAdapterConfiguration.KEY_FYBER_APP_ID to BuildConfig.AM_FYBER_APP_ID
            ))
            .build()
        MoPub.initializeSdk(context, sdkConfiguration) {
            Timber.tag(TAG).d("Mopub initialised")
            initAPS(context)
            postInit(mopubHomeBannerAdView)
        }
    }

    override fun initOgury() {
        Presage.getInstance().start("270174", MainApplication.context!!)
    }

    override fun postInit(homeBannerView: MoPubView?) {
        if (homeBannerView != null) {
            mopubHomeBannerAdView = homeBannerView
        }
        if (!homeViewLoaded || !MoPub.isSdkInitialized()) {
            return
        }

        val personalInfoManager = MoPub.getPersonalInformationManager()

        if (adsVisible && personalInfoManager != null && personalInfoManager.gdprApplies() == true) {

            if (AdRegistration.isInitialized()) {
                AdRegistration.setCMPFlavor(AdRegistration.CMPFlavor.MOPUB_CMP)
            }

            if (personalInfoManager.shouldShowConsentDialog()) {
                personalInfoManager.loadConsentDialog(object : ConsentDialogListener {
                    override fun onConsentDialogLoaded() {
                        personalInfoManager.subscribeConsentStatusChangeListener { _, newConsentStatus, _ ->
                            syncNonIABGDPRConsentStatus(newConsentStatus)
                        }
                        personalInfoManager.showConsentDialog()
                        toggle()
                    }

                    override fun onConsentDialogLoadFailed(moPubErrorCode: MoPubErrorCode) {
                        if (AdRegistration.isInitialized()) {
                            AdRegistration.setConsentStatus(AdRegistration.ConsentStatus.UNKNOWN)
                        }
                        toggle()
                    }
                })
            } else {
                toggle()
            }
        } else {
            toggle()
        }
    }

    override fun setHomeViewLoaded() {
        homeViewLoaded = true
        paused = false
    }

    private fun turnOff() {
        shutdown = true

        mopubHomeBannerAdView?.autorefreshEnabled = false
        homeBannerStarted = false

        disableAudioAds()

        EventBus.getDefault().post(EventToggleRemoveAdVisibility(false))
    }

    private fun turnOn() {
        val context = MainApplication.context ?: return

        shutdown = false
        if (!MoPub.isSdkInitialized()) {
            start(context)
            currentPremiumStatus = null
        } else {
            prepareBanner()
            setupInterstitial()
            showPlayerAd(false)
        }

        enableAudioAds(context)
    }

    private fun enableAudioAds(context: Context) {
        if (!adsWizzInitialized && VERSION.SDK_INT >= 23 && remoteVariablesProvider.audioAdsEnabled && !withholdAds) {
            AdswizzSDK.apply {
                initialize(context)
                gdprConsent = NOT_ASKED
//                ccpaConfig = CCPAConfig(YES, NO, YES)
            }
            adsWizzInitialized = true
        }
    }

    private fun disableAudioAds() {
        AdswizzSDK.cleanup()
        adsWizzInitialized = false
    }

    private fun readFreshInstall() {
        freshInstall = sharedPreferences.getString(AD_PREFERENCES_INTERSTITIAL_TIMESTAMP) == null
    }

    override fun toggle() {
        val premiumStatus = !adsVisible
        if (!premiumStatus && (currentPremiumStatus == null || currentPremiumStatus != premiumStatus)) {
            currentPremiumStatus = premiumStatus
            turnOn()
        } else if (premiumStatus) {
            currentPremiumStatus = premiumStatus
            turnOff()
        }
    }

    override fun create() {
        observePremiumChanges()
        observeInterstitialAds()
        readFreshInstall()
    }

    override fun destroy() {
        Timber.tag(TAG).d("destroy")

        currentPremiumStatus = null

        activityComposite.clear()
        homeBannerStarted = false
        mopubHomeBannerAdView?.destroy()
        mopub300x250AdView?.destroy()
        mopubInterstitial?.destroy()
        mopubInterstitial = null
        mopubNativeAd?.destroy()
        loadIntersitialHandler.removeCallbacks(setupInterstitialRunnable)
    }

    override fun stopAds() {
        Timber.tag(TAG).d("stopAds")

        paused = true
    }

    override fun restartAds() {
        Timber.tag(TAG).d("restartAds")

        paused = false
        if (!interstitialTimerActive) {
            setupInterstitial()
        }
    }

    override fun onBannerAppeared() {
        if (MoPub.isSdkInitialized() && !shutdown && homeBannerStarted && (Date().time - bannerAPSTimestamp) > 570_000) {
            Timber.tag(TAG).d("onBannerAppeared: resetting banner keywords since 9:30 minutes have passed since last TAM successful request")
            refreshTAMKeywordsForBanner()
        }
    }

    private fun prepareBanner() {
        Timber.tag(TAG).d("prepareBanner")

        // Preconditions
        val banner = mopubHomeBannerAdView ?: return

        if (MoPub.isSdkInitialized() &&
            !shutdown &&
            !homeBannerStarted &&
            remoteVariablesProvider.bannerAdEnabled &&
            !withholdAds
        ) {

            Timber.tag(TAG).d("prepareBanner - all checks OK")

            banner.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            EventBus.getDefault().post(EventToggleRemoveAdVisibility(false))

            banner.bannerAdListener = object : TrackingBannerAdListener() {
                override fun onBannerLoaded(banner: MoPubView) {
                    super.onBannerLoaded(banner)
                    val adapterName = MopubAdapterNameManager.getAdapterNameFromBanner(banner)
                    Timber.tag(TAG).d("Mopub banner ad loaded [$adapterName]")

                    if (!shutdown) {
                        EventBus.getDefault().post(EventToggleRemoveAdVisibility(true))
                        trackBannerViewed()

                        refreshTAMKeywordsForBanner()
                    }
                }

                override fun onBannerFailed(banner: MoPubView, errorCode: MoPubErrorCode) {
                    super.onBannerFailed(banner, errorCode)
                    Timber.tag(TAG).d("Mopub banner ad failed to load")
                    notifyAdsEventsUseCase.notify("320x50 failed ($errorCode")

                    refreshTAMKeywordsForBanner()
                }

                override fun onBannerClicked(banner: MoPubView) {
                    super.onBannerClicked(banner)
                    Timber.tag(TAG).d("Mopub banner ad clicked")
                }

                override fun onBannerExpanded(banner: MoPubView) {
                    super.onBannerExpanded(banner)
                    Timber.tag(TAG).d("Mopub banner ad expanded")
                }

                override fun onBannerCollapsed(banner: MoPubView) {
                    super.onBannerCollapsed(banner)
                    Timber.tag(TAG).d("Mopub banner ad collapsed")
                }
            }

            banner.setAdUnitId(BuildConfig.AM_MOPUB_BANNER_ID)
            banner.autorefreshEnabled = true
            banner.setKeywords(keywords)
            banner.setUserDataKeywords(userDataKeywords)
            banner.loadAd()
            notifyAdsEventsUseCase.notify("320x50 requested")
            logKeywords("320x50", keywords, userDataKeywords)

            homeBannerStarted = true
        }
    }

    private fun refreshTAMKeywordsForBanner() {
        mopubHomeBannerAdView?.setKeywords(keywords)

        bannerTAMDisposable?.dispose()

        Timber.tag(TAG).d("Requesting TAM keywords for banner")
        fetchAPSData(ApsAdType.Banner)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ apsKeywords ->
                Timber.tag(TAG).d("Got TAM keywords for banner: $apsKeywords")
                mopubHomeBannerAdView?.setKeywords(keywords + apsKeywords)
                logKeywords("320x50", keywords + apsKeywords, userDataKeywords)
                bannerAPSTimestamp = Date().time
            }, {
                Timber.tag(TAG).d("Failed to get TAM keywords for banner: ${it.message}")
            })
            .also { bannerTAMDisposable = it }
    }

    private val setupInterstitialRunnable = {
        Timber.tag(TAG).d("The pre-loaded interstitial is no longer valid.")
        mopubInterstitial = null
        setupInterstitial()
    }

    private fun scheduleNewInterstitial() {
        loadIntersitialHandler.removeCallbacks(setupInterstitialRunnable)

        val interstitialValidDuration: Long = MILLISECONDS.convert(1L, TimeUnit.HOURS)
        loadIntersitialHandler.postDelayed(setupInterstitialRunnable, interstitialValidDuration)

        interstitialTimerActive = true
    }

    private fun setupInterstitial() {
        Timber.tag(TAG).d("setupInterstitial")

        val activity = HomeActivity.instance ?: return

        if (!MoPub.isSdkInitialized() || shutdown || mopubInterstitial != null || !ForegroundManager[activity].isForeground) {
            interstitialTimerActive = false
            return
        }

        if (!remoteVariablesProvider.interstitialAdEnabled || withholdAds) return

        fetchAPSData(ApsAdType.Interstitial)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe { apsKeywords ->
                mopubInterstitial = createInterstitial(activity, apsKeywords)
            }.addTo(activityComposite)
    }

    private fun createInterstitial(
        activity: HomeActivity,
        apsKeywords: String
    ) = MoPubInterstitial(
        activity,
        BuildConfig.AM_MOPUB_INTERSTITIAL_ID
    ).apply {
        setKeywords(getInterstitialKeywords() + apsKeywords)
        setUserDataKeywords(userDataKeywords)
        interstitialAdListener = this@AdProvidersHelper
        load()
    }.also {
        notifyAdsEventsUseCase.notify("Interstitial requested")
        logKeywords("Interstitial", getInterstitialKeywords() + apsKeywords, userDataKeywords)
    }

    override fun onInterstitialLoaded(interstitial: MoPubInterstitial) {
        super.onInterstitialLoaded(interstitial)
        interstitialObservable.onNext(Ready(interstitial))
    }

    override fun onInterstitialFailed(
        interstitial: MoPubInterstitial,
        errorCode: MoPubErrorCode
    ) {
        super.onInterstitialFailed(interstitial, errorCode)
        interstitialObservable.onNext(Failed(errorCode.toString()))
    }

    override fun onInterstitialShown(interstitial: MoPubInterstitial) {
        super.onInterstitialShown(interstitial)
        interstitialObservable.onNext(Shown(interstitial))
    }

    override fun onInterstitialDismissed(interstitial: MoPubInterstitial) {
        super.onInterstitialDismissed(interstitial)
        interstitialObservable.onNext(Dismissed)
    }

    override fun onInterstitialClicked(moPubInterstitial: MoPubInterstitial?) {
        super.onInterstitialClicked(moPubInterstitial)
        interstitialObservable.onNext(Dismissed)
    }

    override fun preloadNativeAd() {
        showPlayerAdWhenReady = false
        resetMopubNativeAd()
        prepareMopubNativeAd(null)
    }

    override fun showPlayerAd(showPlayerAdWhenReady: Boolean) {

        this.showPlayerAdWhenReady = showPlayerAdWhenReady

        val activity = HomeActivity.instance ?: return

        if (!MoPub.isSdkInitialized() || shutdown || paused ||
            !ForegroundManager[activity].isForeground || !activity.isPlayerMaximized() ||
            loadingPlayerAd || interstitialIsVisible || audioAdManager.adState is Playing
        ) {
            return
        }

        if (remoteVariablesProvider.playerAdEnabled && !withholdAds) {
            mopubNativeAd?.takeIf { !it.isDestroyed }?.let { nativeAd ->
                if (showPlayerAdWhenReady) {
                    activity.showMopubNativeAd(
                        nativeAd,
                        nativeAdsAdapterHelper!!
                    )
                    appsFlyerDataSource.trackAdWatched()
                }
                return
            }
            mopub300x250AdView?.takeIf { mopub300x250AdLoaded }?.let { mopubView ->
                if (showPlayerAdWhenReady) {
                    activity.showMopub300x250Ad(mopubView)
                }
                return
            }

            // If none is ready then load them again and show right away when ready. Firstly native ad, then 300x250
            loadingPlayerAd = true
            preparePlayerAds()
        }
    }

    private fun preparePlayerAds() {
        val nativeAdPercentage = remoteVariablesProvider.playerNativeAdsPercentage
        if (1 + random.nextInt(100) <= nativeAdPercentage) {
            prepareMopubNativeAd(object : PlayerAdLoadingFailureListener {
                override fun onAdFailedToLoad() {
                    prepareMopub300x250Ad(null)
                }
            })
        } else {
            prepareMopub300x250Ad(object : PlayerAdLoadingFailureListener {
                override fun onAdFailedToLoad() {
                    prepareMopubNativeAd(null)
                }
            })
        }
    }

    private interface PlayerAdLoadingFailureListener {
        fun onAdFailedToLoad()
    }

    private fun resetMopubNativeAd() {
        mopubNativeAd = null
    }

    private fun prepareMopubNativeAd(listener: PlayerAdLoadingFailureListener?) {

        resetMopubNativeAd()

        val activity = HomeActivity.instance ?: return

        if (!MoPub.isSdkInitialized() || shutdown || paused || !remoteVariablesProvider.playerAdEnabled || withholdAds) {
            return
        }

        if (nativeAdsAdapterHelper == null) {
            nativeAdsAdapterHelper = AdapterHelper(
                MainApplication.context!!,
                0,
                3
            )
        }

        val moPubNative = MoPubNative(
            activity,
            BuildConfig.AM_MOPUB_NATIVEAD_ID,
            object : TrackingNativeNetworkListener() {
                override fun onNativeLoad(nativeAd: NativeAd) {
                    super.onNativeLoad(nativeAd)
                    val adapterName = MopubAdapterNameManager.getAdapterNameFromNative(nativeAd)
                    Timber.tag(TAG).d("Mopub native ad loaded [$adapterName]")
                    notifyAdsEventsUseCase.notify("Native loaded [$adapterName]")
                    mopubNativeAd = nativeAd
                    loadingPlayerAd = false
                    if (mopubNativeAd != null) {
                        showPlayerAd(showPlayerAdWhenReady)
                    } else {
                        listener?.onAdFailedToLoad()
                    }
                }

                override fun onNativeFail(errorCode: NativeErrorCode) {
                    super.onNativeFail(errorCode)
                    Timber.tag(TAG).d("Mopub native ad failed to load")
                    notifyAdsEventsUseCase.notify("Native failed ($errorCode)")
                    listener?.onAdFailedToLoad()
                }
            })

        val staticAdViewBinder = ViewBinder.Builder(R.layout.view_mopub_native_ad)
            .mainImageId(R.id.native_ad_imageView)
            .iconImageId(R.id.native_ad_icon)
            .titleId(R.id.native_ad_title)
            .textId(R.id.native_ad_body)
            .callToActionId(R.id.native_ad_call_to_action)
            .privacyInformationIconImageId(R.id.native_ad_info_icon)
            .build()

        val nativeAdViewBinder = MediaViewBinder.Builder(R.layout.view_mopub_admob_native_ad)
            .mediaLayoutId(R.id.native_ad_media)
            .iconImageId(R.id.native_ad_icon)
            .titleId(R.id.native_ad_title)
            .textId(R.id.native_ad_body)
            .callToActionId(R.id.native_ad_call_to_action)
            .privacyInformationIconImageId(R.id.native_ad_info_icon)
            .build()

        val admobViewBinder = GooglePlayServicesViewBinder.Builder(R.layout.view_mopub_admob_native_ad)
            .mediaLayoutId(R.id.native_ad_media)
            .iconImageId(R.id.native_ad_icon)
            .titleId(R.id.native_ad_title)
            .textId(R.id.native_ad_body)
            .callToActionId(R.id.native_ad_call_to_action)
            .privacyInformationIconImageId(R.id.native_ad_info_icon)
            .build()

        val facebookViewBinder =
            FacebookAdRenderer.FacebookViewBinder.Builder(R.layout.view_mopub_facebook_native_ad)
                .titleId(R.id.native_ad_title)
                .textId(R.id.native_ad_body)
                .mediaViewId(R.id.native_ad_main_image)
                .adIconViewId(R.id.native_ad_icon_image)
                .adChoicesRelativeLayoutId(R.id.native_ad_choices_relative_layout)
                .advertiserNameId(R.id.native_ad_title)
                .callToActionId(R.id.native_ad_call_to_action)
                .build()

        val admobNativeAdRenderer = GooglePlayServicesAdRenderer(admobViewBinder)
        val facebookNativeAdRenderer = FacebookAdRenderer(facebookViewBinder)
        val moPubStaticNativeAdRenderer = MoPubStaticNativeAdRenderer(staticAdViewBinder)
        val moPubVideoNativeAdRenderer = MoPubVideoNativeAdRenderer(nativeAdViewBinder)
        moPubNative.registerAdRenderer(admobNativeAdRenderer)
        moPubNative.registerAdRenderer(facebookNativeAdRenderer)
        moPubNative.registerAdRenderer(moPubStaticNativeAdRenderer)
        moPubNative.registerAdRenderer(moPubVideoNativeAdRenderer)

        val desiredAssets = EnumSet.of(
            RequestParameters.NativeAdAsset.TITLE,
            RequestParameters.NativeAdAsset.TEXT,
            RequestParameters.NativeAdAsset.ICON_IMAGE,
            RequestParameters.NativeAdAsset.MAIN_IMAGE,
            RequestParameters.NativeAdAsset.CALL_TO_ACTION_TEXT
        )

        val requestParameters = RequestParameters.Builder()
            .desiredAssets(desiredAssets)
            .keywords(keywords)
            .userDataKeywords(userDataKeywords)
            .build()

        moPubNative.makeRequest(requestParameters)
        notifyAdsEventsUseCase.notify("Native requested")
        logKeywords("Native", keywords, userDataKeywords)
    }

    override fun resetMopub300x250Ad() {
        mopub300x250AdView = null
        mopub300x250AdLoaded = false
    }

    private fun prepareMopub300x250Ad(listener: PlayerAdLoadingFailureListener?) {

        resetMopub300x250Ad()

        if (!MoPub.isSdkInitialized() || shutdown || paused || HomeActivity.instance == null || !remoteVariablesProvider.playerAdEnabled || withholdAds) {
            return
        }

        fetchAPSData(ApsAdType.Rectangle)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe { apsKeywords ->
                mopub300x250AdLoaded = false
                mopub300x250AdView = MoPubView(HomeActivity.instance).also {
                    it.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    it.setAdUnitId(BuildConfig.AM_MOPUB_300x250_ID)
                    it.setKeywords(keywords + apsKeywords)
                    it.setUserDataKeywords(userDataKeywords)
                    it.bannerAdListener = object : TrackingBannerAdListener() {
                        override fun onBannerLoaded(banner: MoPubView) {
                            super.onBannerLoaded(banner)
                            val adapterName = MopubAdapterNameManager.getAdapterNameFromBanner(banner)
                            Timber.tag(TAG).d("Mopub 300x250 ad loaded [$adapterName]")
                            notifyAdsEventsUseCase.notify("300x250 loaded [$adapterName]")
                            mopub300x250AdLoaded = true
                            loadingPlayerAd = false
                            showPlayerAd(showPlayerAdWhenReady)
                        }

                        override fun onBannerFailed(banner: MoPubView, errorCode: MoPubErrorCode) {
                            super.onBannerFailed(banner, errorCode)
                            Timber.tag(TAG).d("Mopub 300x250 ad failed to load")
                            mopub300x250AdLoaded = false
                            loadingPlayerAd = false
                            notifyAdsEventsUseCase.notify("300x250 failed ($errorCode)")
                            listener?.onAdFailedToLoad()
                        }

                        override fun onBannerClicked(banner: MoPubView) {
                            super.onBannerClicked(banner)
                            Timber.tag(TAG).d("Mopub 300x250 ad clicked")
                        }

                        override fun onBannerExpanded(banner: MoPubView) {
                            super.onBannerExpanded(banner)
                            Timber.tag(TAG).d("Mopub 300x250 ad expanded")
                        }

                        override fun onBannerCollapsed(banner: MoPubView) {
                            super.onBannerCollapsed(banner)
                            Timber.tag(TAG).d("Mopub 300x250 ad collapsed")
                        }
                    }
                    it.loadAd()
                    it.autorefreshEnabled = false
                    notifyAdsEventsUseCase.notify("300x250 requested")
                    logKeywords("300x250", keywords + apsKeywords, userDataKeywords)
                }
            }
            .addTo(activityComposite)
    }

    private fun trackInterstitialShown() {
        AnalyticsHelper.getInstance().trackEvent("ad", "load", "interstitial")
        appsFlyerDataSource.trackAdWatched()
    }

    private fun trackSessionStartInterstitialShown() {
        AnalyticsHelper.getInstance().trackEvent("ad", "load", "sessionStartInterstitial")
        appsFlyerDataSource.trackAdWatched()
    }

    private fun trackBannerViewed() {
        AnalyticsHelper.getInstance().trackEvent("ad", "refresh", "banner")
        appsFlyerDataSource.trackAdWatched()
    }

    override fun getAdvertisingIdentifier(context: Context): Observable<String> {
        return Observable.fromCallable { AdvertisingIdClient.getAdvertisingIdInfo(context).id }
    }

    private fun logKeywords(placement: String, keywords: String, userDataKeywords: String) {
        Timber.tag(LogType.ADS.tag).d("$placement keywords - $keywords${if (userDataKeywords.isBlank()) "" else " - $userDataKeywords"}")
    }

    override fun isFreshInstall(): Boolean {
        return freshInstall
    }

    override fun showInterstitial() {
        if (interstitialIsLoading || interstitialIsVisible) return

        val notShown = if (!ForegroundManager.get().isForeground) {
            NotShown("App is in background")
        } else if (!adsVisible) {
            NotShown("Ads are disabled")
        } else if (!remoteVariablesProvider.interstitialAdEnabled) {
            NotShown("Interstitials are not enabled")
        } else if (withholdAds) {
            NotShown("Withholding ads for new install")
        } else if (preferences.needToShowPlayerPlaylistTooltip || preferences.needToShowPlayerQueueTooltip) {
            NotShown("Need to show a tooltip, interstitial shall wait")
        } else if (!needToShowInterstitialAd()) {
            NotShown("Not enough time has passed")
        } else {
            null
        }

        notShown?.let {
            interstitialObservable.onNext(it)
            return
        }

        interstitialObservable.onNext(Loading)

        // Give the interstitial extra time to load if it's the first one being shown
        if (!interstitialWasShown && !interstitialIsReady) {
            Timber.tag(TAG).i("Delaying playback for interstitial loading")
            Observable.timer(interstitialAdFirstPlayDelay, MILLISECONDS)
                .observeOn(schedulersProvider.main)
                .subscribe {
                    if (interstitialIsReady) {
                        showInterstitialInternal()
                    } else if (interstitialIsLoading) {
                        interstitialObservable.onNext(NotShown("Interstitial isn't ready yet"))
                    }
                }.addTo(activityComposite)
            return
        }

        if (!interstitialIsReady) {
            interstitialObservable.onNext(NotShown("Interstitial isn't ready yet"))
            return
        }

        Handler(Looper.getMainLooper()).post {
            showInterstitialInternal()
        }
    }

    @MainThread
    private fun showInterstitialInternal() {
        val shown = mopubInterstitial?.show() ?: false
        if (!shown) {
            interstitialObservable.onNext(NotShown("Failed to show a ready interstitial"))
        }
    }

    private fun needToShowInterstitialAd(): Boolean {
        if (interstitialAdShownTimestamp == null) {
            interstitialAdShownTimestamp =
                sharedPreferences.getString(AD_PREFERENCES_INTERSTITIAL_TIMESTAMP)?.toLongOrNull()
                    ?: 0L
        }
        return Date().time - (interstitialAdShownTimestamp
            ?: 0) >= remoteVariablesProvider.interstitialTiming * 1000
    }

    private fun setInterstitialAdShown() {
        interstitialAdShownTimestamp = Date().time
        try {
            sharedPreferences.put(AD_PREFERENCES_INTERSTITIAL_TIMESTAMP, interstitialAdShownTimestamp.toString())
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun getInterstitialKeywords(): String {
        val now = SystemClock.elapsedRealtime()
        val newAdSession = (now - adSessionStartTimestamp).times(1000L) >= interstitialAdSessionPeriod
        val value = if (!interstitialWasShown || newAdSession) "yes" else "no"
        return keywords.plus(",session_start:$value").also {
            adSessionStartTimestamp = now
        }
    }

    /**
     * Creates both keywords and userDataKeywords to be passed to Mopub on each ad request. These info are only calculated once per session and then cached.
     */
    private fun calculateKeywords(): Pair<String, String> {
        val deviceRepository = DeviceRepository
        val osVersionName = deviceRepository.getOsVersion()
        val appVersionName = deviceRepository.getAppVersionName()
        val userRepository = UserRepository.getInstance()
        val user = userRepository.getUser()

        val keywords = String.format(
            "am_oslevelpatch:%s,am_oslevelminor:%s,am_oslevelmajor:%s,am_appversionpatch:%s,am_appversionminor:%s,am_appversionmajor:%s,am_buildversion:%s,am_device:%s,am_osname:android,am_admin:%s,am_uploader:%s",
            if (osVersionName.split("\\.".toRegex()).size < 3) "" else osVersionName,
            if (osVersionName.split("\\.".toRegex()).size < 3) osVersionName else osVersionName.substring(0, osVersionName.lastIndexOf(".")),
            if (osVersionName.split("\\.".toRegex()).size < 2) osVersionName else osVersionName.substring(0, osVersionName.indexOf(".")),
            appVersionName,
            appVersionName.substring(0, appVersionName.lastIndexOf(".")),
            appVersionName.substring(0, appVersionName.indexOf(".")),
            deviceRepository.getAppVersionCode(),
            deviceRepository.getModel().replace("&".toRegex(), "").replace("=".toRegex(), "").replace(" ".toRegex(), ""),
            userRepository.isAdmin().toString(),
            ((user?.uploadsCount ?: 0) > 0).toString()
        )

        val userDataKeywords = user?.let { loggedUser ->
            val gender = when (loggedUser.gender) {
                AMArtist.Gender.MALE -> "m"
                AMArtist.Gender.FEMALE -> "f"
                else -> null
            }
            val age = loggedUser.age
            val yob = loggedUser.yob
            listOfNotNull(
                gender?.let { "m_gender:$it" },
                age?.let { "m_age:$it" },
                yob?.let { "m_yob:$it" }
            ).joinToString(",")
        } ?: ""

        return keywords to userDataKeywords
    }

    // APS

    private fun initAPS(context: Context) {
        if (remoteVariablesProvider.apsEnabled) {
            AdRegistration.getInstance(BuildConfig.AM_APS_KEY, context.applicationContext)
            AdRegistration.setMRAIDPolicy(MRAIDPolicy.MOPUB)
            AdRegistration.useGeoLocation(true)
            AdRegistration.enableLogging(BuildConfig.AUDIOMACK_DEBUG)
            AdRegistration.enableTesting(BuildConfig.AUDIOMACK_DEBUG)
        }
    }

    /**
     * Runs an APS request to get keywords to be used for Mopub ad requests.
     * Throws an error if the request fails or the timeout is exceeded (controlled by Remote Config)
     * All errors are swallowed and it's guaranteed to get a string, eventually empty.
     */
    private fun fetchAPSData(adType: ApsAdType): Observable<String> {
        return Observable.create<String> { emitter ->
            if (!remoteVariablesProvider.apsEnabled) {
                emitter.onError(APSDisabledException)
            }
            val request = DTBAdRequest()
            request.setSizes(adType.dtbAdSize)
            request.loadAd(object : DTBAdCallback {
                override fun onSuccess(response: DTBAdResponse) {
                    val keywords = "," + response.moPubKeywords
                    emitter.onNext(keywords)
                    emitter.onComplete()
                }
                override fun onFailure(error: AdError) {
                    emitter.onError(APSGenericException)
                }
            })
        }
            .timeout(
                if (adType != ApsAdType.Banner) remoteVariablesProvider.apsTimeout else Long.MAX_VALUE,
                MILLISECONDS
            )
            .onErrorResumeNext(Observable.just(""))
    }

    object APSGenericException : Exception("APS request failed")
    object APSDisabledException : Exception("APS disabled")

    // Internal

    private fun handleInterstitialShown(interstitial: MoPubInterstitial) {
        if (!interstitialWasShown) {
            trackSessionStartInterstitialShown()
        } else {
            trackInterstitialShown()
        }

        val adapterName = MopubAdapterNameManager.getAdapterNameFromInterstitial(interstitial)
        Timber.tag(TAG).d("Mopub interstitial ad shown [$adapterName]")
        notifyAdsEventsUseCase.notify("Interstitial shown [$adapterName]")

        interstitialWasShown = true
    }

    private fun handleInterstitialDismissed() {
        Timber.tag(TAG).d("Mopub interstitial ad dismissed")
        mopubInterstitial = null
        setInterstitialAdShown()
        scheduleNewInterstitial()
        setupInterstitial()
    }

    private fun handleInterstitialFailed(reason: String) {
        Timber.tag(TAG).d("Mopub interstitial ad failed ($reason)")
        mopubInterstitial = null
        retryLoadInterstitial()
        notifyAdsEventsUseCase.notify("Interstitial failed ($reason)")
    }

    private fun retryLoadInterstitial() {
        val milliseconds = max(60, remoteVariablesProvider.interstitialTiming - 30) * 1000
        loadIntersitialHandler.postDelayed({ setupInterstitial() }, milliseconds)
        interstitialTimerActive = true
    }

    private fun handleInterstitialLoaded(interstitial: MoPubInterstitial) {
        val adapterName = MopubAdapterNameManager.getAdapterNameFromInterstitial(interstitial)
        Timber.tag(TAG).d("Mopub interstitial ad loaded [$adapterName]")
        notifyAdsEventsUseCase.notify("Interstitial loaded [$adapterName]")
        scheduleNewInterstitial()
    }

    private fun observePremiumChanges() {
        premiumObserver?.let { activityComposite.remove(it) }
        premiumObserver = PremiumRepository.getInstance().premiumObservable
            .distinctUntilChanged()
            .subscribe { toggle() }
            .addTo(activityComposite)
    }

    private fun syncNonIABGDPRConsentStatus(newConsentStatus: ConsentStatus) {
        if (AdRegistration.isInitialized()) {
            val tamStatus = when (newConsentStatus) {
                ConsentStatus.EXPLICIT_YES, ConsentStatus.POTENTIAL_WHITELIST -> AdRegistration.ConsentStatus.EXPLICIT_YES
                ConsentStatus.EXPLICIT_NO, ConsentStatus.DNT -> AdRegistration.ConsentStatus.EXPLICIT_NO
                else -> AdRegistration.ConsentStatus.UNKNOWN
            }
            AdRegistration.setConsentStatus(tamStatus)
        }
    }

    private fun observeAudioAds() {
        audioAdManager.adStateObservable
            .filter { it is Playing }
            .observeOn(schedulersProvider.main)
            .subscribe { setInterstitialAdShown() }
            .addTo(applicationComposite)
    }

    private fun observeInterstitialAds() {
        interstitialDisposable?.let { activityComposite.remove(it) }
        interstitialDisposable = interstitialObservable.distinctUntilChanged()
            .subscribe {
                when (it) {
                    is Shown -> handleInterstitialShown(it.interstitial)
                    is Dismissed -> handleInterstitialDismissed()
                    is Ready -> handleInterstitialLoaded(it.interstitial)
                    is Failed -> handleInterstitialFailed(it.reason)
                }
            }.addTo(activityComposite)
    }

    private fun observePlayCount() {
        playCountDisposable?.let { applicationComposite.remove(it) }

        if (!withholdAds) return

        // Listen for play count changes and toggle ads when passing the threshold
        playCountDisposable = preferences.observeLong(GENERAL_PREFERENCES_PLAY_COUNT)
            .subscribeOn(schedulersProvider.io)
            .filter { it >= remoteVariablesProvider.adWithholdPlays }
            .take(1)
            .observeOn(schedulersProvider.main)
            .subscribe {
                if (adsVisible) turnOn()
                playCountDisposable?.dispose()
            }
            .addTo(applicationComposite)
    }
}

enum class ApsAdType(val dtbAdSize: DTBAdSize) {
    Banner(DTBAdSize(320, 50, BuildConfig.AM_APS_BANNER_SLOT)),
    Interstitial(DTBAdSize.DTBInterstitialAdSize(BuildConfig.AM_APS_INTERSTITIAL_SLOT)),
    Rectangle(DTBAdSize(300, 250, BuildConfig.AM_APS_300x250_SLOT))
}
