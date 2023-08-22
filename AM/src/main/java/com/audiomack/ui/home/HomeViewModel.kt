package com.audiomack.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy.KEEP
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.audiomack.INTENT_OPEN_PLAYER
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.common.WorkManagerProvider
import com.audiomack.common.WorkManagerProviderImpl
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.ads.ShowInterstitialResult
import com.audiomack.data.ads.ShowInterstitialResult.Dismissed
import com.audiomack.data.ads.ShowInterstitialResult.Failed
import com.audiomack.data.ads.ShowInterstitialResult.Loading
import com.audiomack.data.ads.ShowInterstitialResult.NotShown
import com.audiomack.data.api.ArtistsDataSource
import com.audiomack.data.api.ArtistsRepository
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.authentication.AuthenticationDataSource
import com.audiomack.data.authentication.AuthenticationRepository
import com.audiomack.data.authentication.LoginException
import com.audiomack.data.database.MusicDAOException
import com.audiomack.data.deeplink.Deeplink
import com.audiomack.data.deeplink.DeeplinkDataSource
import com.audiomack.data.deeplink.DeeplinkRepository
import com.audiomack.data.device.DeviceDataSource
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.featured.FeaturedSpotDataSource
import com.audiomack.data.featured.FeaturedSpotRepository
import com.audiomack.data.housekeeping.HousekeepingUseCase
import com.audiomack.data.housekeeping.MusicSyncUseCase
import com.audiomack.data.housekeeping.MusicSyncUseCaseImpl
import com.audiomack.data.inapprating.InAppRating
import com.audiomack.data.inapprating.InAppRatingManager
import com.audiomack.data.inapprating.InAppRatingResult
import com.audiomack.data.inappupdates.InAppUpdateAvailabilityResult
import com.audiomack.data.inappupdates.InAppUpdateResult
import com.audiomack.data.inappupdates.InAppUpdatesManager
import com.audiomack.data.inappupdates.InAppUpdatesManagerImpl
import com.audiomack.data.inappupdates.InAppUpdatesMode
import com.audiomack.data.music.local.OpenLocalMediaUseCase
import com.audiomack.data.music.local.OpenLocalMediaUseCaseImpl
import com.audiomack.data.music.local.isAudio
import com.audiomack.data.onboarding.OnboardingPlaylistsGenreProvider
import com.audiomack.data.onboarding.OnboardingPlaylistsGenreProviderImpl
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.player.PlayerRepository
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.premiumdownload.IUnlockPremiumDownloadUseCase
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadRepository
import com.audiomack.data.premiumdownload.UnlockPremiumDownloadUseCase
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.reachability.Reachability
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.share.ShareManager
import com.audiomack.data.share.ShareManagerImpl
import com.audiomack.data.sleeptimer.SleepTimer
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerTriggered
import com.audiomack.data.sleeptimer.SleepTimerManager
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.data.support.ZendeskRepository
import com.audiomack.data.telco.TelcoDataSource
import com.audiomack.data.telco.TelcoRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryOffline
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibrarySearchOffline
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.tracking.mixpanel.MixpanelTabBrowse
import com.audiomack.data.tracking.mixpanel.MixpanelTabFeed
import com.audiomack.data.tracking.mixpanel.MixpanelTabMyLibrary
import com.audiomack.data.tracking.mixpanel.MixpanelTabPlaylists
import com.audiomack.data.tracking.mixpanel.MixpanelTabSearch
import com.audiomack.data.tracking.mixpanel.MixpanelTrackerImpl
import com.audiomack.data.tracking.mixpanel.PremiumDownloadType
import com.audiomack.data.tracking.mixpanel.RestoreDownloadsMode
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.download.RestoreDownloadsWorker
import com.audiomack.model.AMArtist
import com.audiomack.model.AMComment
import com.audiomack.model.AMResultItem
import com.audiomack.model.ActionToBeResumed
import com.audiomack.model.AuthenticationType
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.EventLoginState
import com.audiomack.model.EventPlayer
import com.audiomack.model.EventShowAddedToOfflineInAppMessage
import com.audiomack.model.EventShowDownloadFailureToast
import com.audiomack.model.EventShowDownloadSuccessToast
import com.audiomack.model.EventShowUnreadTicketsAlert
import com.audiomack.model.EventToggleRemoveAdVisibility
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import com.audiomack.model.NextPageData
import com.audiomack.model.OpenMusicData
import com.audiomack.model.PlayerCommand
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.PremiumDownloadMusicModel
import com.audiomack.model.PremiumDownloadStatsModel
import com.audiomack.model.PremiumLimitedDownloadAlertViewType
import com.audiomack.model.PremiumLimitedDownloadInfoViewType
import com.audiomack.model.PremiumOnlyDownloadAlertViewType
import com.audiomack.model.ProgressHUDMode
import com.audiomack.model.SearchType
import com.audiomack.model.ShareMethod
import com.audiomack.network.API
import com.audiomack.network.APIException
import com.audiomack.network.APIInterface
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.common.Resource
import com.audiomack.ui.home.HomeActivity.Companion.ACTION_LOGIN_REQUIRED
import com.audiomack.ui.home.HomeActivity.Companion.ACTION_NOTIFY_OFFLINE
import com.audiomack.ui.home.HomeActivity.Companion.EXTRA_LOGIN_FAVORITE
import com.audiomack.ui.home.HomeActivity.Companion.EXTRA_LOGIN_REPOST
import com.audiomack.ui.tooltip.TooltipFragment
import com.audiomack.usecases.AddMusicToQueuePosition
import com.audiomack.usecases.AddMusicToQueueUseCase
import com.audiomack.usecases.AddMusicToQueueUseCaseImpl
import com.audiomack.usecases.AddMusicToQueueUseCaseResult
import com.audiomack.usecases.FacebookExpressLoginUseCase
import com.audiomack.usecases.FacebookExpressLoginUseCaseImpl
import com.audiomack.usecases.OpenMusicResult
import com.audiomack.usecases.OpenMusicUseCase
import com.audiomack.usecases.OpenMusicUseCaseImpl
import com.audiomack.usecases.PlayMusicFromIdResult
import com.audiomack.usecases.PlayMusicFromIdUseCase
import com.audiomack.usecases.PlayMusicFromIdUseCaseImpl
import com.audiomack.usecases.ShowSleepTimerPromptUseCase
import com.audiomack.usecases.ShowSleepTimerPromptUseCaseImpl
import com.audiomack.usecases.SleepTimerPromptMode
import com.audiomack.utils.Foreground
import com.audiomack.utils.ForegroundManager
import com.audiomack.utils.GeneralPreferences
import com.audiomack.utils.GeneralPreferencesImpl
import com.audiomack.utils.SimpleObserver
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import com.google.android.gms.auth.api.credentials.Credential
import com.mopub.mobileads.MoPubView
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class HomeViewModel(
    activityResultRegistry: ActivityResultRegistry,
    private val deeplinkDataSource: DeeplinkDataSource = DeeplinkRepository.getInstance(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(MixpanelTrackerImpl),
    private val preferencesDataSource: PreferencesDataSource = PreferencesRepository(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    private val remoteVariablesProvider: RemoteVariablesProvider = RemoteVariablesProviderImpl(),
    private val deviceDataSource: DeviceDataSource = DeviceRepository,
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    private val eventBus: EventBus = EventBus.getDefault(),
    private val generalPreferences: GeneralPreferences = GeneralPreferencesImpl(),
    private val artistsDataSource: ArtistsDataSource = ArtistsRepository(),
    private val zendeskDataSource: ZendeskDataSource = ZendeskRepository(),
    private val authenticationDataSource: AuthenticationDataSource = AuthenticationRepository(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val telcoDataSource: TelcoDataSource = TelcoRepository(),
    private val foreground: Foreground = ForegroundManager.get(),
    private val reachabilityDataSource: ReachabilityDataSource = Reachability.getInstance(),
    private val featuredSpotDataSource: FeaturedSpotDataSource = FeaturedSpotRepository.getInstance(),
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val queueDataSource: QueueDataSource = QueueRepository.getInstance(),
    private val musicSyncUseCase: MusicSyncUseCase = MusicSyncUseCaseImpl(),
    private val housekeepingUseCase: HousekeepingUseCase = (MainApplication.context as MainApplication).housekeepingUseCase,
    private val onboardingPlaylistsGenreProvider: OnboardingPlaylistsGenreProvider = OnboardingPlaylistsGenreProviderImpl(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val shareManager: ShareManager = ShareManagerImpl(),
    private val inAppUpdatesManager: InAppUpdatesManager = InAppUpdatesManagerImpl(),
    workManagerProvider: WorkManagerProvider = WorkManagerProviderImpl.getInstance(),
    private val playerDataSource: PlayerDataSource = PlayerRepository.getInstance(),
    private val premiumDownloadDataSource: PremiumDownloadDataSource = PremiumDownloadRepository.getInstance(),
    private val unlockPremiumDownloadUseCase: IUnlockPremiumDownloadUseCase = UnlockPremiumDownloadUseCase(),
    private val apiEmailVerification: APIInterface.EmailVerificationInterface = API.getInstance().emailVerificationAPI,
    private val sleepTimer: SleepTimer = SleepTimerManager.getInstance(),
    private val inAppRating: InAppRating = InAppRatingManager.getInstance(),
    private val playMusicFromIdUseCase: PlayMusicFromIdUseCase = PlayMusicFromIdUseCaseImpl(),
    private val addMusicToQueueUseCase: AddMusicToQueueUseCase = AddMusicToQueueUseCaseImpl(),
    private val openMusicUseCase: OpenMusicUseCase = OpenMusicUseCaseImpl(),
    private val showSleepTimerPromptUseCase: ShowSleepTimerPromptUseCase = ShowSleepTimerPromptUseCaseImpl(),
    private val openLocalMedia: OpenLocalMediaUseCase = OpenLocalMediaUseCaseImpl(activityResultRegistry),
    navigation: NavigationEvents = NavigationManager.getInstance(),
    private val navigationActions: NavigationActions = NavigationManager.getInstance(),
    alerts: AlertEvents = AlertManager,
    private val facebookExpressLoginUseCase: FacebookExpressLoginUseCase = FacebookExpressLoginUseCaseImpl(),
    private val delayMaxValue: Long = 100L
) : BaseViewModel(), NavigationEvents by navigation, AlertEvents by alerts {

    /** Specifies the image url to load in the My Library button, or null if not authenticated **/
    private val _myLibraryAvatar = MutableLiveData<String>()
    val myLibraryAvatar: LiveData<String> get() = _myLibraryAvatar

    /** Specifies the my library badge text **/
    private val _myLibraryNotifications = MutableLiveData<String>()
    val myLibraryNotifications: LiveData<String> get() = _myLibraryNotifications

    /** Specifies the feed badge text **/
    private val _feedNotifications = MutableLiveData<String>()
    val feedNotifications: LiveData<String> get() = _feedNotifications

    /** Specifies the ad layout visibility **/
    private val _adLayoutVisible = MutableLiveData<Boolean>()
    val adLayoutVisible: LiveData<Boolean> get() = _adLayoutVisible

    /** Specifies the currently visible/selected **/
    private val _currentTab = MutableLiveData<CurrentTab>()
    val currentTab: LiveData<CurrentTab> get() = _currentTab

    /** Emits a Deeplink to some Audiomack content. A Subject is used to debounce events. **/
    val deeplinkEvent = SingleLiveEvent<Deeplink>()
    private val deeplinkSubject = BehaviorSubject.create<Deeplink>()
    private var delayAmount = AtomicLong(0)

    val showSmartLockEvent = SingleLiveEvent<Void>()
    val deleteSmartLockCredentialsEvent = SingleLiveEvent<Credential>()
    val restoreMiniplayerEvent = SingleLiveEvent<Boolean>() // true if already restored
    val showUnreadTicketsAlert = SingleLiveEvent<Void>()
    val showDownloadSuccessToastEvent = SingleLiveEvent<EventShowDownloadSuccessToast>()
    val showDownloadFailureToastEvent = SingleLiveEvent<Void>()
    val showAddedToOfflineInAppMessageEvent = SingleLiveEvent<Void>()
    val openPlayerEvent = SingleLiveEvent<Void>()
    val openPlayerMenuEvent = SingleLiveEvent<Void>()
    val setupBackStackListenerEvent = SingleLiveEvent<Void>()
    val showMiniplayerTooltipEvent = SingleLiveEvent<TooltipFragment.TooltipLocation>()
    val toggleHUDModeEvent = SingleLiveEvent<ProgressHUDMode>()
    val showArtistEvent = SingleLiveEvent<ShowArtist>()
    val showSongEvent = SingleLiveEvent<ShowSong>()
    val showAlbumEvent = SingleLiveEvent<ShowAlbum>()
    val showPlaylistEvent = SingleLiveEvent<ShowPlaylist>()
    val closeTooltipEvent = SingleLiveEvent<Void>()
    val notifyOfflineEvent = SingleLiveEvent<Void>()
    val showGeorestrictedAlertEvent = SingleLiveEvent<Runnable?>()
    val openMusicInfoEvent = SingleLiveEvent<AMResultItem>()
    val showCommentEvent = SingleLiveEvent<ShowComment>()
    val showBenchmarkEvent = SingleLiveEvent<ShowBenchmark>()
    val triggerAppUpdateEvent = SingleLiveEvent<Void>()
    val showInAppUpdateConfirmationEvent = SingleLiveEvent<Void>()
    val showInAppUpdateDownloadStartedEvent = SingleLiveEvent<Void>()
    val showAgeGenderEvent = SingleLiveEvent<Void>()
    val showDeleteDownloadAlertEvent = SingleLiveEvent<AMResultItem>()
    val showPremiumDownloadEvent = SingleLiveEvent<PremiumDownloadModel>()
    val promptRestoreDownloadsEvent = SingleLiveEvent<List<AMResultItem>>()
    val restoreDownloadsEvent = SingleLiveEvent<WorkInfo>()
    val showEmailVerificationResultEvent = SingleLiveEvent<Boolean>()
    val showInterstitialLoaderEvent = SingleLiveEvent<Boolean>()
    val sleepTimerTriggeredEvent = SingleLiveEvent<Unit>()
    val showRatingPromptEvent = SingleLiveEvent<Void>()
    val showDeclinedRatingPromptEvent = SingleLiveEvent<Void>()
    val openAppRatingEvent = SingleLiveEvent<Void>()
    val showAddedToQueueToastEvent = SingleLiveEvent<Void>()
    val showPasswordResetScreenEvent = SingleLiveEvent<String>()
    val showPasswordResetErrorEvent = SingleLiveEvent<Void>()
    val showSleepTimerPromptEvent = SingleLiveEvent<SleepTimerPromptMode>()
    val triggerFacebookExpressLoginEvent = SingleLiveEvent<Unit>()

    private val workManager: WorkManager = workManagerProvider.workManager
    private val restoreDownloadsObserver = androidx.lifecycle.Observer<List<WorkInfo>> { workInfo ->
        restoreDownloadsEvent.postValue(workInfo.firstOrNull())
    }
    private val workInfoLive =
        workManager.getWorkInfosForUniqueWorkLiveData(RestoreDownloadsWorker.TAG_RESTORE_ALL)
            .apply {
                observeForever(restoreDownloadsObserver)
            }

    /** Used to store the next deeplink to be followed, other than the ones calculated by DeeplinkDataSource **/
    private var nextDeeplink: Deeplink? = null

    private var smartLockReady = false
    private var smartLockDisabled = false

    private var visible = false

    private var firstDeeplinkConsumed = false

    private var flexibleInAppUpdateAlertShown = false

    private var ageGenderScreenShown = false

    private val songInfoFailure by lazy {
        ProgressHUDMode.Failure(getString(R.string.song_info_failed))
    }

    private var pendingDeeplinkAfterLogin: Deeplink? = null

    @VisibleForTesting
    val premiumObserver = HomeObserver<Boolean> {
        // Only update _adLayoutVisible is ads are off in order to hide the banner, otherwise the banner is handled by the [EventToggleRemoveAdVisibility] event on each ad loading/show
        if (!adsDataSource.adsVisible) {
            _adLayoutVisible.postValue(adsDataSource.adsVisible)
        }
    }

    @VisibleForTesting
    val queueObserver = HomeObserver<AMResultItem> {
        val restored = restoreMiniplayerEvent.value == true
        if (!restored) restoreMiniplayerEvent.postValue(true)
    }

    private val interstitialObserver = HomeObserver<ShowInterstitialResult> {
        when (it) {
            is Loading -> showInterstitialLoaderEvent.postValue(true)
            is Failed, is NotShown, is Dismissed -> {
                showInterstitialLoaderEvent.postValue(false)
            }
        }
    }

    @VisibleForTesting
    val foregroundListener = object : Foreground.Listener {
        override fun onBecameForeground() {
            adsDataSource.restartAds()
            featuredSpotDataSource.pick()
            fetchUserData()
            fetchNotifications()
        }

        override fun onBecameBackground() {
            adsDataSource.stopAds()
        }
    }

    init {
        eventBus.register(this)
        observeLoginChanges()
        premiumDataSource.premiumObservable.subscribe(premiumObserver)
        foreground.addListener(foregroundListener)
        queueDataSource.subscribeToCurrentItem(queueObserver)

        housekeepingUseCase.downloadsToRestore
            .subscribe { promptRestoreDownloadsEvent.postValue(it) }
            .addTo(compositeDisposable)

        inAppRating.inAppRating
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe { handleInAppRatingResult(it) }
            .addTo(compositeDisposable)

        deeplinkSubject.apply {
            if (delayMaxValue > 0L) {
                concatMap {
                    Observable.just(it)
                        .delay(delayAmount.getAndSet(delayMaxValue), TimeUnit.MILLISECONDS)
                }
            }
            subscribeOn(schedulersProvider.io)
            observeOn(schedulersProvider.main)
            subscribe {
                Timber.tag(TAG).d("Posting deeplinkEvent $it")
                deeplinkEvent.value = it
            }.addTo(compositeDisposable)
        }

        premiumDataSource.refresh()

        adsDataSource.interstitialObservable
            .observeOn(schedulersProvider.main)
            .subscribe(interstitialObserver)

        handleSleepTimerPrompt()

        observeSleepTimer()

        if (adsDataSource.isFreshInstall()) {
            userDataSource.isLoggedInAsync()
                .filter { !it }
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    triggerFacebookExpressLoginEvent.call()
                }, {})
                .composite()
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }

    @VisibleForTesting
    fun cleanup() {
        eventBus.unregister(this)
        foreground.removeListener(foregroundListener)
        workInfoLive.removeObserver(restoreDownloadsObserver)
    }

    private fun observeLoginChanges() {
        userDataSource.loginEvents
            .observeOn(schedulersProvider.main)
            .subscribe {
                when (it) {
                    EventLoginState.LOGGED_IN -> {
                        pendingDeeplinkAfterLogin?.let { deeplink -> updateDeeplink(deeplink) }
                        fetchUserData()
                        fetchNotifications()
                    }
                    EventLoginState.LOGGED_OUT -> {
                        onBrowseTabClicked()
                        updateUI()
                        smartLockDisabled = true
                    }
                    else -> {
                        pendingDeeplinkAfterLogin = null
                        updateUI()
                    }
                }
            }
            .addTo(compositeDisposable)
    }

    fun onCreate(intent: Intent) {
        onboardingPlaylistsGenreProvider.trackAppSession()

        trackingDataSource.trackIdentity()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe()
            .addTo(compositeDisposable)

        setupBackStackListenerEvent.call()

        housekeepingUseCase.runHousekeeping()
            .subscribe { Timber.tag(TAG).d("Housekeeping completed") }
            .also { compositeDisposable.add(it) }

        musicSyncUseCase.syncMusic()
            ?.subscribe { Timber.tag(TAG).d("Music sync completed") }
            ?.also { compositeDisposable.add(it) }

        if (adsDataSource.isFreshInstall() && remoteVariablesProvider.firstOpeningDeeplink.isNotEmpty() && nextDeeplink == null) {
            deeplinkDataSource.obtainDeeplink(Intent().apply {
                data = Uri.parse(remoteVariablesProvider.firstOpeningDeeplink)
            })
                ?.let {
                    if (it.isModal) {
                        onBrowseTabClicked()
                        Completable.timer(10L, TimeUnit.MILLISECONDS)
                            .subscribeOn(schedulersProvider.interval)
                            .observeOn(schedulersProvider.main)
                            .subscribe { updateDeeplink(it, true) }
                            .composite()
                    } else {
                        updateDeeplink(it)
                    }
                }
                ?: onBrowseTabClicked()
        } else {
            onBrowseTabClicked()
        }

        adsDataSource.create()

        openUriFromIntent(intent)
    }

    fun onDestroy() {
        adsDataSource.destroy()
        mixpanelDataSource.flushEvents()
    }

    fun onResume(intent: Intent?) {
        Timber.tag(TAG).d("onResume")
        visible = true
        intent?.let { mixpanelDataSource.trackPushOpened(it) }
        updateUI()
        fetchUserData()
        fetchNotifications()
        foreground.setActivityResumed(HomeActivity::class.java.simpleName)
        adsDataSource.onBannerAppeared()
        initInAppUpdates()
    }

    fun onPause() {
        visible = false
        foreground.setActivityPaused(HomeActivity::class.java.simpleName)
    }

    fun onIntentReceived(intent: Intent?) {
        // Check if the app was launched with an openable Uri
        if (openUriFromIntent(intent)) return

        nextDeeplink?.let {
            updateDeeplink(it)
            nextDeeplink = null
        } ?: deeplinkDataSource.obtainDeeplink(intent)?.let {
            updateDeeplink(it)
        }

        // Actions specific to widget/player notification
        when {
            intent?.action == ACTION_LOGIN_REQUIRED -> {
                if (intent.hasExtra(EXTRA_LOGIN_FAVORITE)) {
                    launchLoginEvent.value = LoginSignupSource.Favorite
                } else if (intent.hasExtra(EXTRA_LOGIN_REPOST)) {
                    launchLoginEvent.value = LoginSignupSource.Repost
                }
            }
            intent?.action == ACTION_NOTIFY_OFFLINE -> {
                notifyOfflineEvent.call()
            }
            intent?.hasExtra(INTENT_OPEN_PLAYER) ?: false -> {
                launchPlayerEvent.value = MaximizePlayerData()
            }
        }
    }

    private fun openUriFromIntent(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false

        if (intent.type.isAudio()) {
            openLocalMedia.open(uri, intent.type)
            return true
        }

        return false
    }

    fun onFeedTabClicked() {
        updateDeeplink(Deeplink.Timeline)
    }

    fun onPlaylistsTabClicked() {
        updateDeeplink(Deeplink.Playlists(onboardingPlaylistsGenreProvider.categoryId))
    }

    fun onBrowseTabClicked() {
        updateDeeplink(Deeplink.Trending(preferencesDataSource.defaultGenre.genreKey()))
    }

    fun onSearchTabClicked() {
        updateDeeplink(Deeplink.Search())
    }

    fun onMyLibraryTabClicked() {
        userDataSource.isLoggedInAsync()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ loggedIn ->
                if (loggedIn) {
                    if (!userDataSource.hasFavorites) {
                        updateDeeplink(Deeplink.MyDownloads)
                    } else {
                        updateDeeplink(Deeplink.MyFavorites)
                    }
                } else {
                    navigationActions.launchLogin(LoginSignupSource.MyLibrary)
                    pendingDeeplinkAfterLogin = Deeplink.MyFavorites
                }
            }, {})
            .composite()
    }

    fun onRemoveBannerClicked() {
        updateDeeplink(Deeplink.Premium(InAppPurchaseMode.BannerAdDismissal))
    }

    fun onSmartLockReady(onboardingVisible: Boolean) {
        smartLockReady = true
        showSmartLockIfNeeded(onboardingVisible)
    }

    fun onAdLayoutReady(adViewHome: MoPubView) {
        adsDataSource.setHomeViewLoaded()
        adsDataSource.postInit(adViewHome)
        adsDataSource.initOgury()
    }

    fun onPlayerInstantiated() {
        if (queueDataSource.currentItem != null) {
            restoreMiniplayerEvent.postValue(true)
        }
    }

    fun onOfflineRedirectDetected() {
        nextDeeplink = Deeplink.MyDownloads
    }

    fun loginWithSmartLockCredentials(credentials: Credential?) {
        val credential = credentials?.takeIf {
            // Regular username/password account
            it.accountType == null
        } ?: return
        compositeDisposable.add(
            authenticationDataSource.loginWithEmailPassword(credential.id, credential.password ?: "")
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    userDataSource.onLoggedIn()
                    mixpanelDataSource.trackLogin(LoginSignupSource.AppLaunch, AuthenticationType.Email, userDataSource, premiumDataSource, telcoDataSource)
                    trackingDataSource.trackLogin()
                }, {
                    if (it is LoginException && (it.statusCode ?: 0) >= 400 && (it.statusCode ?: 0) < 500) {
                        deleteSmartLockCredentialsEvent.postValue(credential)
                    }
                })
        )
    }

    fun onPlayerMaximized() {
        closeTooltipEvent.call()
    }

    fun onPlayerPlaylistTooltipClosed() {
        generalPreferences.setPlayerPlaylistTooltipShown()
    }

    fun onPlayerQueueTooltipClosed() {
        generalPreferences.setPlayerQueueTooltipShown()
    }

    fun onPlayerEqTooltipClosed() {
        generalPreferences.setPlayerEqTooltipShown()
    }

    fun onPlayerScrollTooltipShown() {
        generalPreferences.setPlayerScrollTooltipShown()
    }

    fun showPlayerAd(showWhenReady: Boolean) {
        adsDataSource.showPlayerAd(showWhenReady)
    }

    fun onLoginRequiredAccepted(source: LoginSignupSource) {
        launchLoginEvent.postValue(source)
    }

    fun onLoginRequiredDeclined() {
        userDataSource.onLoginCanceled()
    }

    fun onBenchmarkOpened(activity: Activity?, music: AMResultItem?, artist: AMArtist?, benchmark: BenchmarkModel, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        shareManager.shareScreenshot(activity, music, artist, ShareMethod.Screenshot, benchmark, mixpanelSource, mixpanelButton)
    }

    fun onCastInitException() {
        deviceDataSource.castAvailable = false
    }

    fun setMiniplayerTooltipLocation(location: TooltipFragment.TooltipLocation) {
        if (generalPreferences.needToShowMiniplayerTooltip()) {
            showMiniplayerTooltipEvent.value = location
        }
    }

    fun onMiniplayerTooltipShown() {
        generalPreferences.setMiniplayerTooltipShown()
    }

    fun onRestoreDownloadsRejected(downloadsCount: Int) {
        mixpanelDataSource.trackRestoreDownloads(RestoreDownloadsMode.Manually, downloadsCount)
        housekeepingUseCase.clearDownloadsToRestore()
            .subscribe { Timber.tag(TAG).d("Cleared restored offline items database") }
            .addTo(compositeDisposable)
    }

    fun onRestoreDownloadsRequested(downloadsCount: Int) {
        mixpanelDataSource.trackRestoreDownloads(RestoreDownloadsMode.All, downloadsCount)
        val workRequest: OneTimeWorkRequest = OneTimeWorkRequestBuilder<RestoreDownloadsWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(
            RestoreDownloadsWorker.TAG_RESTORE_ALL,
            KEEP,
            workRequest
        )
    }

    // Entities requested

    fun onArtistScreenRequested(urlSlug: String, tab: String? = null, openShare: Boolean = false) {
        toggleHUDModeEvent.value = ProgressHUDMode.Loading
        compositeDisposable.add(
            artistsDataSource.artistData(urlSlug)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    toggleHUDModeEvent.value = ProgressHUDMode.Dismiss
                    showArtistEvent.value =
                        ShowArtist(it, tab, openShare)
                }, {
                    toggleHUDModeEvent.value = ProgressHUDMode.Failure(getString(R.string.artist_info_failed))
                })
        )
    }

    fun openMusic(data: OpenMusicData) {
        openMusicUseCase(data)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe { result ->
                when (result) {
                    is OpenMusicResult.ToggleLoader -> toggleHUDModeEvent.value = result.mode
                    is OpenMusicResult.GeoRestricted -> showGeorestrictedAlertEvent.call()
                    is OpenMusicResult.ShowPlaylist -> showPlaylistEvent.value = ShowPlaylist(
                        result.playlist,
                        result.online,
                        result.deleted,
                        result.mixpanelSource,
                        result.openShare
                    )
                    is OpenMusicResult.ShowAlbum -> showAlbumEvent.value = ShowAlbum(
                        result.album,
                        result.mixpanelSource,
                        result.openShare
                    )
                    is OpenMusicResult.ReadyToPlay -> launchPlayerEvent.value = result.data
                }
            }.composite()
    }

    fun onSongRequested(id: String, mixpanelSource: MixpanelSource, openShare: Boolean = false) {
        toggleHUDModeEvent.value = ProgressHUDMode.Loading
        compositeDisposable.add(
            musicDataSource.getSongInfo(id)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    toggleHUDModeEvent.value = ProgressHUDMode.Dismiss
                    showSongEvent.value = ShowSong(it, mixpanelSource, openShare)
                }, {
                    toggleHUDModeEvent.value = songInfoFailure
                })
        )
    }

    fun onAlbumRequested(id: String, mixpanelSource: MixpanelSource, openShare: Boolean = false) {
        toggleHUDModeEvent.value = ProgressHUDMode.Loading
        if (mixpanelSource.page == MixpanelPageMyLibraryOffline || mixpanelSource.page == MixpanelPageMyLibrarySearchOffline) {
            compositeDisposable.add(
                musicDataSource.getOfflineResource(id)
                    .subscribeOn(schedulersProvider.io)
                    .map { resource ->
                        resource.also { it.data?.loadTracks() }
                    }
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        it.data?.let { album ->
                            toggleHUDModeEvent.value = ProgressHUDMode.Dismiss
                            showAlbumEvent.value = ShowAlbum(album, mixpanelSource, openShare)
                        } ?: run {
                            toggleHUDModeEvent.value = ProgressHUDMode.Failure(getString(R.string.album_info_failed))
                        }
                    }, {
                        toggleHUDModeEvent.value = ProgressHUDMode.Failure(getString(R.string.album_info_failed))
                    })
            )
        } else {
            compositeDisposable.add(
                musicDataSource.getAlbumInfo(id)
                    .subscribeOn(schedulersProvider.io)
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        toggleHUDModeEvent.value = ProgressHUDMode.Dismiss
                        showAlbumEvent.value =
                            ShowAlbum(
                                it,
                                mixpanelSource,
                                openShare
                            )
                    }, {
                        toggleHUDModeEvent.value = ProgressHUDMode.Failure(getString(R.string.album_info_failed))
                    })
            )
        }
    }

    fun onPlaylistRequested(id: String, mixpanelSource: MixpanelSource, openShare: Boolean = false) {
        var remotePlaylist: AMResultItem? = null
        toggleHUDModeEvent.value = ProgressHUDMode.Loading
        if (!reachabilityDataSource.networkAvailable) {
            compositeDisposable.add(
                musicDataSource.getOfflineResource(id)
                    .subscribeOn(schedulersProvider.io)
                    .observeOn(schedulersProvider.main)
                    .flatMap { playlistResource ->
                        playlistResource.takeIf { it is Resource.Success }?.data?.let { playlist ->
                            Observable.create<AMResultItem> { emitter ->
                                playlist.loadTracks()
                                emitter.onNext(playlist)
                            }
                        } ?: throw Exception("No playlist found in DB")
                    }
                    .subscribe({ dbPlaylist ->
                        toggleHUDModeEvent.postValue(ProgressHUDMode.Dismiss)
                        showPlaylistEvent.postValue(ShowPlaylist(
                            dbPlaylist,
                            online = false,
                            deleted = false,
                            mixpanelSource = mixpanelSource,
                            openShare = openShare
                        ))
                    }, {
                        toggleHUDModeEvent.postValue(ProgressHUDMode.Failure(getString(R.string.playlist_info_failed)))
                    })
            )
        } else {
            compositeDisposable.add(
                musicDataSource.getPlaylistInfo(id)
                    .subscribeOn(schedulersProvider.io)
                    .observeOn(schedulersProvider.main)
                    .flatMap {
                        remotePlaylist = it
                        musicDataSource.getOfflineResource(it.itemId)
                    }
                    .flatMap { dbPlaylistResource ->
                        dbPlaylistResource.takeIf { it is Resource.Success }?.data?.let { dbPlaylist ->
                            Observable.create<AMResultItem> { emitter ->
                                dbPlaylist.updatePlaylist(remotePlaylist)
                                emitter.onNext(dbPlaylist)
                                emitter.onComplete()
                            }
                        } ?: Observable.just(remotePlaylist)
                    }
                    .subscribe({
                        toggleHUDModeEvent.postValue(ProgressHUDMode.Dismiss)
                        showPlaylistEvent.postValue(ShowPlaylist(
                            remotePlaylist!!,
                            online = true,
                            deleted = false,
                            mixpanelSource = mixpanelSource,
                            openShare = openShare
                        ))
                    }, {
                        if (it is MusicDAOException) {
                            toggleHUDModeEvent.postValue(ProgressHUDMode.Dismiss)
                            showPlaylistEvent.postValue(ShowPlaylist(
                                remotePlaylist!!,
                                online = true,
                                deleted = false,
                                mixpanelSource = mixpanelSource,
                                openShare = openShare
                            ))
                        } else if (it is APIException && (it.statusCode == 404 || it.statusCode == 403)) {
                            compositeDisposable.add(
                                musicDataSource.getOfflineResource(id)
                                    .subscribeOn(schedulersProvider.io)
                                    .observeOn(schedulersProvider.main)
                                    .flatMap { playlistResource ->
                                        playlistResource.takeIf { it is Resource.Success }?.data?.let { playlist ->
                                            Observable.create<AMResultItem> { emitter ->
                                                playlist.loadTracks()
                                                emitter.onNext(playlist)
                                            }
                                        } ?: throw Exception("No playlist found in DB")
                                    }
                                    .subscribe({
                                        showPlaylistEvent.postValue(ShowPlaylist(
                                            it,
                                            online = true,
                                            deleted = true,
                                            mixpanelSource = mixpanelSource,
                                            openShare = openShare
                                        ))
                                    }, {
                                        toggleHUDModeEvent.postValue(ProgressHUDMode.Failure(getString(R.string.playlist_info_failed)))
                                    })
                            )
                        } else {
                            toggleHUDModeEvent.postValue(ProgressHUDMode.Failure(getString(R.string.playlist_info_failed)))
                        }
                    })
            )
        }
    }

    fun onPlayRemoteMusicRequested(musicId: String, musicType: MusicType, mixpanelSource: MixpanelSource) {
        playMusicFromIdUseCase.loadAndPlay(musicId, musicType, mixpanelSource)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe { result ->
                when (result) {
                    is PlayMusicFromIdResult.ToggleLoader -> toggleHUDModeEvent.value = result.mode
                    is PlayMusicFromIdResult.Georestricted -> showGeorestrictedAlertEvent.call()
                    is PlayMusicFromIdResult.ReadyToPlay -> launchPlayerEvent.value = result.data
                }
            }
            .addTo(compositeDisposable)
    }

    fun playLater(musicId: String, musicType: MusicType, mixpanelSource: MixpanelSource) {
        addMusicToQueueUseCase.loadAndAdd(musicId, musicType, mixpanelSource, AddMusicToQueuePosition.Later)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe { result ->
                when (result) {
                    is AddMusicToQueueUseCaseResult.Success -> showAddedToQueueToastEvent.call()
                    is AddMusicToQueueUseCaseResult.ToggleLoader -> toggleHUDModeEvent.value = result.mode
                    is AddMusicToQueueUseCaseResult.Georestricted -> showGeorestrictedAlertEvent.call()
                }
            }
            .addTo(compositeDisposable)
    }

    fun onCommentsRequested(id: String, type: String, uuid: String, threadId: String?) {
        toggleHUDModeEvent.value = ProgressHUDMode.Loading
        compositeDisposable.add(
            musicDataSource.getMusicInfo(id, type)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ item ->
                    val comment = AMComment()
                    comment.uuid = uuid
                    comment.threadUuid = threadId
                    toggleHUDModeEvent.postValue(ProgressHUDMode.Dismiss)
                    showCommentEvent.postValue(ShowComment(item, comment))
                }, {
                    toggleHUDModeEvent.value = ProgressHUDMode.Failure(getString(R.string.generic_api_error))
                })
        )
    }

    fun onBenchmarkRequested(entityId: String, entityType: String, benchmark: BenchmarkModel, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        toggleHUDModeEvent.value = ProgressHUDMode.Loading
        compositeDisposable.add(
            musicDataSource.getMusicInfo(entityId, entityType)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    toggleHUDModeEvent.value = ProgressHUDMode.Dismiss
                    showBenchmarkEvent.value = ShowBenchmark(it, benchmark, mixpanelSource, mixpanelButton)
                }, {
                    toggleHUDModeEvent.value = songInfoFailure
                })
        )
    }

    fun onPlayerShowRequested() {
        if (queueDataSource.items.isNotEmpty()) {
            openPlayerEvent.call()
        }
    }

    fun onShuffleRequested(
        nextPage: NextPageData,
        firstPage: List<AMResultItem> = listOf(),
        offlineScreen: Boolean = nextPage.offlineScreen,
        source: MixpanelSource = nextPage.mixpanelSource
    ) {
        val isFavorites = nextPage.url.contains("favorites")
        val otherPages = if (isFavorites) {
            playerDataSource.getAllPages(nextPage)
        } else {
            playerDataSource.getNextPage(nextPage)
        }

        Observable.fromIterable(firstPage)
            .concatWith(otherPages)
            .toList()
            .doOnSubscribe { toggleHUDModeEvent.postValue(ProgressHUDMode.Loading) }
            .doOnError { toggleHUDModeEvent.postValue(songInfoFailure) }
            .doFinally { toggleHUDModeEvent.postValue(ProgressHUDMode.Dismiss) }
            .observeOn(schedulersProvider.main)
            .subscribe { items ->
                onMaximizePlayerRequested(
                    MaximizePlayerData(
                        item = items[0],
                        items = items,
                        inOfflineScreen = offlineScreen,
                        mixpanelSource = source,
                        shuffle = true,
                        scrollToTop = true
                    )
                )
            }
            .addTo(compositeDisposable)
    }

    fun onMiniplayerSwipedUp() {
        launchPlayerEvent.value = MaximizePlayerData()
    }

    fun onMaximizePlayerRequested(data: MaximizePlayerData) {
        if (data.inOfflineScreen && data.item?.downloadType == AMResultItem.MusicDownloadType.Limited && data.item.isDownloadFrozen) {
            val availableMusicToUnfreeze = premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount + premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(data.item) <= premiumDownloadDataSource.premiumDownloadLimit
            showPremiumDownloadEvent.postValue(PremiumDownloadModel(
                music = PremiumDownloadMusicModel(data.item, premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(data.item)),
                stats = PremiumDownloadStatsModel(MixpanelButtonList, data.mixpanelSource ?: MixpanelSource.empty, premiumDownloadDataSource.premiumDownloadLimit, premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount),
                alertTypeLimited = if (availableMusicToUnfreeze) PremiumLimitedDownloadAlertViewType.PlayFrozenOfflineWithAvailableUnfreezes else PremiumLimitedDownloadAlertViewType.PlayFrozenOffline
            ))
        } else if (data.inOfflineScreen && data.item?.downloadType == AMResultItem.MusicDownloadType.Premium && !premiumDataSource.isPremium) {
            showPremiumDownloadEvent.postValue(PremiumDownloadModel(
                music = PremiumDownloadMusicModel(data.item),
                alertTypePremium = PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline
            ))
        } else {
            launchPlayerEvent.value = data
        }
    }

    fun onDeeplinkConsumed(intent: Intent?) {
        val needToHandleIntent = !firstDeeplinkConsumed
        deeplinkDataSource.handlingDeeplink = false
        firstDeeplinkConsumed = true
        if (needToHandleIntent) {
            onIntentReceived(intent)
        }
    }

    fun onLinkRequested(link: String) {
        deeplinkDataSource.obtainDeeplink(Intent().apply {
            data = Uri.parse(link)
        })?.let { updateDeeplink(it) }
    }

    fun onGeorestrictedMusicClicked(onDelete: Runnable? = null) {
        showGeorestrictedAlertEvent.postValue(onDelete)
    }

    fun onDeleteDownloadRequested(id: String) {
        toggleHUDModeEvent.value = ProgressHUDMode.Loading
        compositeDisposable.add(
            musicDataSource.getOfflineItem(id)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    toggleHUDModeEvent.value = ProgressHUDMode.Dismiss
                    showDeleteDownloadAlertEvent.postValue(it)
                }, {
                    toggleHUDModeEvent.value = ProgressHUDMode.Failure(getString(R.string.song_info_failed))
                }))
    }

    fun onPremiumDownloadsRequested(model: PremiumDownloadModel) {
        showPremiumDownloadEvent.postValue(model)
    }

    // EventBus subscriptions

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventShowUnreadTicketsAlert) {
        if (visible) {
            showUnreadTicketsAlert.call()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventToggleRemoveAdVisibility) {
        _adLayoutVisible.postValue(event.visible)
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventShowDownloadSuccessToast) {
        showDownloadSuccessToastEvent.postValue(event)
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventShowDownloadFailureToast) {
        showDownloadFailureToastEvent.call()
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventShowAddedToOfflineInAppMessage) {
        // Adding a +N in case of limited downloads to take into account the current download
        if (generalPreferences.needToShowDownloadInAppMessage()) {
            generalPreferences.setDownloadInAppMessageShown()
            if (event.premiumLimited && !premiumDataSource.isPremium) {
                showPremiumDownloadEvent.postValue(PremiumDownloadModel(stats = PremiumDownloadStatsModel(MixpanelButtonList, MixpanelSource.empty, premiumDownloadDataSource.premiumDownloadLimit, premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount + event.downloadCount), infoTypeLimited = PremiumLimitedDownloadInfoViewType.FirstDownload))
                generalPreferences.setLimitedDownloadInAppMessageShown()
            } else {
                showAddedToOfflineInAppMessageEvent.call()
            }
        } else if (generalPreferences.needToShowLimitedDownloadInAppMessage() && event.premiumLimited && !premiumDataSource.isPremium) {
            showPremiumDownloadEvent.postValue(PremiumDownloadModel(stats = PremiumDownloadStatsModel(MixpanelButtonList, MixpanelSource.empty, premiumDownloadDataSource.premiumDownloadLimit, premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount + event.downloadCount), infoTypeLimited = PremiumLimitedDownloadInfoViewType.FirstDownload))
            generalPreferences.setLimitedDownloadInAppMessageShown()
            generalPreferences.setDownloadInAppMessageShown()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventPlayer: EventPlayer) {
        Timber.tag(TAG).i("onMessageEvent - EventPlayer command: ${eventPlayer.command}")
        when (eventPlayer.command) {
            PlayerCommand.OPEN -> {
                openPlayerEvent.call()
            }
            PlayerCommand.MENU -> {
                openPlayerMenuEvent.call()
            }
            else -> {}
        }
    }

    // Private methods

    private fun showSmartLockIfNeeded(onboardingVisible: Boolean) {
        if (smartLockReady &&
            !smartLockDisabled &&
            !onboardingVisible &&
            !deviceDataSource.runningEspressoTest &&
            !userDataSource.isLoggedIn() &&
            adsDataSource.isFreshInstall()
        ) {
            smartLockDisabled = true
            showSmartLockEvent.call()
        }
    }

    private fun fetchUserData() {
        if (userDataSource.isLoggedIn()) {
            artistsDataSource.updateUserData()
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe {
                    updateUI(it)
                    if (!ageGenderScreenShown && it.needsProfileCompletion) {
                        ageGenderScreenShown = true
                        showAgeGenderEvent.call()
                    }
                }
                .addTo(compositeDisposable)
        }
    }

    private fun fetchNotifications() {
        if (userDataSource.isLoggedIn()) {
            artistsDataSource.updateUserNotifications()
                .flatMap { artistsDataSource.findLoggedArtist() }
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe { updateUI(it) }
                .addTo(compositeDisposable)

            zendeskDataSource.getUnreadTicketsCount()
                .flatMap { artistsDataSource.findLoggedArtist() }
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe { updateUI(it) }
                .addTo(compositeDisposable)
        }
    }

    fun updateUI(artist: AMArtist? = null) {
        artist?.let {
            _myLibraryAvatar.postValue(it.tinyImage)

            val unseenNotications = it.unseenNotificationsCount + zendeskDataSource.cachedUnreadTicketsCount
            val unseenNoticationsText = when {
                unseenNotications <= 0 -> ""
                unseenNotications < 100 -> unseenNotications.toString()
                else -> "99+"
            }
            _myLibraryNotifications.postValue(unseenNoticationsText)

            val unseenFeed = it.feedCount
            val unseenFeedText = when {
                unseenFeed <= 0 -> ""
                unseenFeed < 100 -> unseenFeed.toString()
                else -> "99+"
            }
            _feedNotifications.postValue(unseenFeedText)
        } ?: run {
            _myLibraryAvatar.postValue(null)
            _myLibraryNotifications.postValue("")
            _feedNotifications.postValue("")
        }
    }

    private fun getString(stringResId: Int): String {
        return MainApplication.context?.getString(stringResId) ?: ""
    }

    private fun updateDeeplink(deeplink: Deeplink, ignoreTabSelection: Boolean = false) {
        val index = when (deeplink) {
            Deeplink.Timeline, Deeplink.SuggestedFollows -> 0
            Deeplink.ArtistSelectOnboarding, is Deeplink.Playlists -> 1
            is Deeplink.World, is Deeplink.WorldPost, is Deeplink.Trending, is Deeplink.TopSongs, is Deeplink.TopAlbums -> 2
            is Deeplink.Search -> 3
            Deeplink.MyFavorites, Deeplink.MyDownloads, Deeplink.MyPlaylists, Deeplink.MyFollowers, Deeplink.MyFollowing, Deeplink.MyUploads -> 4
            else -> null
        }
        if (index == 4 && !userDataSource.isLoggedIn()) {
            navigationActions.launchLogin(LoginSignupSource.MyLibrary)
            pendingDeeplinkAfterLogin = deeplink
            return
        }
        if (index != null && index != _currentTab.value?.index && !ignoreTabSelection) {
            val tabs = listOf(MixpanelTabFeed, MixpanelTabPlaylists, MixpanelTabBrowse, MixpanelTabSearch, MixpanelTabMyLibrary)
            MainApplication.currentTab = tabs.getOrNull(index) ?: tabs.first()
            _currentTab.postValue(
                CurrentTab(
                    index,
                    userDataSource.isLoggedIn()
                )
            )
        }
        deeplinkSubject.onNext(deeplink)
    }

    fun onSearchRequested(query: String, searchType: SearchType) {
        updateDeeplink(Deeplink.Search(query, searchType))
    }

    fun onMusicInfoTapped(music: AMResultItem) {
        openMusicInfoEvent.postValue(music)
    }

    fun onNotificationsRequested() {
        userDataSource.isLoggedInAsync()
            .subscribeOn(schedulersProvider.io)
            .filter { it }
            .observeOn(schedulersProvider.main)
            .subscribe({
                navigationActions.launchNotificationsEvent()
            }, {})
            .composite()
    }

    // In app updates

    private fun initInAppUpdates() {
        inAppUpdatesManager.checkForUpdates()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                when (result) {
                    is InAppUpdateAvailabilityResult.ReadyToDownload -> {
                        if (result.mode == InAppUpdatesMode.Flexible && !flexibleInAppUpdateAlertShown) {
                            triggerAppUpdateEvent.call()
                            flexibleInAppUpdateAlertShown = true
                        } else if (result.mode == InAppUpdatesMode.Immediate) {
                            triggerAppUpdateEvent.call()
                        }
                    }
                    InAppUpdateAvailabilityResult.NeedToResumeDownload -> {
                        triggerAppUpdateEvent.call()
                    }
                    InAppUpdateAvailabilityResult.ReadyToInstall -> {
                        showInAppUpdateConfirmationEvent.call()
                    }
                }
            }, {})
            .also { compositeDisposable.add(it) }
    }

    fun triggerUpdate(activity: Activity) {
        inAppUpdatesManager.triggerUpdate(activity)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                when (result) {
                    InAppUpdateResult.Downloaded -> showInAppUpdateConfirmationEvent.call()
                    InAppUpdateResult.FlexibleDownloadStarted -> showInAppUpdateDownloadStartedEvent.call()
                }
            }, {})
            .also { compositeDisposable.add(it) }
    }

    fun restartAfterUpdate() {
        inAppUpdatesManager.applyUpdate()
    }

    open inner class HomeObserver<T>(
        private val onNext: ((T) -> Unit)? = null
    ) : SimpleObserver<T>(compositeDisposable) {
        override fun onNext(t: T) { onNext?.invoke(t) }
        override fun onError(e: Throwable) = Timber.tag(TAG).e(e)
    }

    // Premium downloads

    fun unlockFrozenDownload(musicId: String) {
        unlockPremiumDownloadUseCase.unlockFrozenDownload(musicId).addTo(compositeDisposable)
    }

    fun streamFrozenMusic(
        activity: Activity,
        musicId: String,
        musicType: MusicType,
        mixpanelSource: MixpanelSource,
        mixpanelButton: String,
        actionToBeResumed: ActionToBeResumed
    ) {
        toggleHUDModeEvent.postValue(ProgressHUDMode.Loading)
        musicDataSource.getMusicInfo(musicId, musicType.typeForMusicApi)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ music ->
                toggleHUDModeEvent.postValue(ProgressHUDMode.Dismiss)
                when (actionToBeResumed) {
                    ActionToBeResumed.Play ->
                        launchPlayerEvent.value = MaximizePlayerData(
                            item = if (music.isAlbum || music.isPlaylist) music.tracks?.firstOrNull() else music,
                            collection = music.takeIf { it.isAlbum || it.isPlaylist },
                            inOfflineScreen = mixpanelSource.isInMyDownloads,
                            albumPlaylistIndex = if (music.isAlbum || music.isPlaylist) 0 else null,
                            mixpanelSource = mixpanelSource,
                            allowFrozenTracks = true
                        )
                    ActionToBeResumed.PlayNext -> music.playNext(activity, mixpanelSource, mixpanelButton, compositeDisposable)
                    ActionToBeResumed.PlayLater -> music.playLater(activity, mixpanelSource, mixpanelButton, compositeDisposable)
                    ActionToBeResumed.Shuffle ->
                        launchPlayerEvent.value = MaximizePlayerData(
                            item = if (music.isAlbum || music.isPlaylist) music.tracks?.firstOrNull() else music,
                            collection = music.takeIf { it.isAlbum || it.isPlaylist },
                            inOfflineScreen = mixpanelSource.isInMyDownloads,
                            albumPlaylistIndex = if (music.isAlbum || music.isPlaylist) 0 else null,
                            mixpanelSource = mixpanelSource,
                            shuffle = true,
                            allowFrozenTracks = true
                        )
                }
            }, { toggleHUDModeEvent.postValue(ProgressHUDMode.Failure("")) })
            .addTo(compositeDisposable)
    }

    fun onPremiumDownloadNotificationShown(type: PremiumDownloadType) {
        mixpanelDataSource.trackPremiumDownloadNotification(type)
    }

    // Email verification

    fun handleEmailVerification(hash: String) {
        apiEmailVerification.runEmailVerification(hash)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                showEmailVerificationResultEvent.postValue(true)
            }, {
                Timber.w(it)
                showEmailVerificationResultEvent.postValue(false)
            })
            .addTo(compositeDisposable)
    }

    // Reset password

    fun handleResetPassword(token: String) {
        authenticationDataSource.verifyForgotPasswordToken(token)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                showPasswordResetScreenEvent.postValue(token)
            }, {
                Timber.w(it)
                showPasswordResetErrorEvent.call()
            })
            .addTo(compositeDisposable)
    }

    // Sleep timer

    private fun observeSleepTimer() {
        sleepTimer.sleepEvent
            .observeOn(schedulersProvider.main)
            .filter { it is TimerTriggered }
            .subscribe { sleepTimerTriggeredEvent.call() }
            .addTo(compositeDisposable)
    }

    private fun handleSleepTimerPrompt() {
        showSleepTimerPromptUseCase.getPromptMode()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ showSleepTimerPromptEvent.postValue(it) }, {})
            .composite()
    }

    // In app rating

    private fun handleInAppRatingResult(result: InAppRatingResult) {
        when (result) {
            InAppRatingResult.ShowRatingPrompt -> showRatingPromptEvent.call()
            InAppRatingResult.ShowDeclinedRatingPrompt -> showDeclinedRatingPromptEvent.call()
            InAppRatingResult.OpenRating -> openAppRatingEvent.call()
            InAppRatingResult.OpenSupport -> deeplinkEvent.postValue(Deeplink.Support)
        }
    }

    fun onRatingPromptAccepted() { inAppRating.onRatingPromptAccepted() }
    fun onRatingPromptDeclined() { inAppRating.onRatingPromptDeclined() }
    fun onDeclinedRatingPromptAccepted() { inAppRating.onDeclinedRatingPromptAccepted() }
    fun onDeclinedRatingPromptDeclined() { inAppRating.onDeclinedRatingPromptDeclined() }
    fun onAppRatingRequested(activity: Activity) { inAppRating.show(activity) }

    // Facebook express login

    fun runFacebookExpressLogin(context: Context) {
        facebookExpressLoginUseCase.run(context)
            .doFinally { smartLockDisabled = true }
            .subscribe()
            .composite()
    }

    // Utils

    companion object {
        const val TAG = "HomeViewModel"
    }

    data class ShowArtist(
        val artist: AMArtist,
        val tab: String?,
        val openShare: Boolean = false
    )

    data class ShowSong(
        val song: AMResultItem,
        val mixpanelSource: MixpanelSource,
        val openShare: Boolean = false
    )

    data class ShowAlbum(
        val album: AMResultItem,
        val mixpanelSource: MixpanelSource,
        val openShare: Boolean = false
    )

    data class ShowPlaylist(
        val playlist: AMResultItem,
        val online: Boolean,
        val deleted: Boolean,
        val mixpanelSource: MixpanelSource,
        val openShare: Boolean = false
    )

    data class ShowComment(
        val music: AMResultItem,
        val comment: AMComment
    )

    data class ShowBenchmark(
        val entity: AMResultItem,
        val benchmark: BenchmarkModel,
        val mixpanelSource: MixpanelSource,
        val mixpanelButton: String
    )

    data class CurrentTab(
        val index: Int,
        val loggedIn: Boolean
    )
}
