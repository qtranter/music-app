package com.audiomack.ui.home

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultRegistry
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.work.WorkManager
import com.audiomack.common.WorkManagerProvider
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.ads.ShowInterstitialResult
import com.audiomack.data.ads.ShowInterstitialResult.Dismissed
import com.audiomack.data.ads.ShowInterstitialResult.Loading
import com.audiomack.data.api.ArtistsDataSource
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.authentication.AuthenticationDataSource
import com.audiomack.data.authentication.LoginException
import com.audiomack.data.database.MusicDAOException
import com.audiomack.data.deeplink.Deeplink
import com.audiomack.data.deeplink.DeeplinkDataSource
import com.audiomack.data.device.DeviceDataSource
import com.audiomack.data.featured.FeaturedSpotDataSource
import com.audiomack.data.housekeeping.HousekeepingUseCase
import com.audiomack.data.housekeeping.MusicSyncUseCase
import com.audiomack.data.inapprating.InAppRating
import com.audiomack.data.inapprating.InAppRatingResult
import com.audiomack.data.inappupdates.InAppUpdateAvailabilityResult
import com.audiomack.data.inappupdates.InAppUpdateResult
import com.audiomack.data.inappupdates.InAppUpdatesManager
import com.audiomack.data.inappupdates.InAppUpdatesMode
import com.audiomack.data.music.local.OpenLocalMediaUseCase
import com.audiomack.data.onboarding.OnboardingPlaylistsGenreProvider
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.preferences.DefaultGenre
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premiumdownload.IUnlockPremiumDownloadUseCase
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.share.ShareManager
import com.audiomack.data.sleeptimer.SleepTimer
import com.audiomack.data.sleeptimer.SleepTimerEvent
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerTriggered
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.data.support.ZendeskUnreadTicketsData
import com.audiomack.data.telco.TelcoDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryOffline
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibrarySearchOffline
import com.audiomack.data.tracking.mixpanel.PremiumDownloadType
import com.audiomack.data.tracking.mixpanel.RestoreDownloadsMode
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.ActionToBeResumed
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.Credentials
import com.audiomack.model.EventLoginState
import com.audiomack.model.EventPlayer
import com.audiomack.model.EventShowAddedToOfflineInAppMessage
import com.audiomack.model.EventShowDownloadFailureToast
import com.audiomack.model.EventShowDownloadSuccessToast
import com.audiomack.model.EventShowUnreadTicketsAlert
import com.audiomack.model.EventToggleRemoveAdVisibility
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.InAppPurchaseMode.AudioAd
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.LoginSignupSource.Download
import com.audiomack.model.LoginSignupSource.MyLibrary
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import com.audiomack.model.PlayerCommand
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.PremiumLimitedDownloadAlertViewType
import com.audiomack.model.PremiumOnlyDownloadAlertViewType
import com.audiomack.model.ProgressHUDMode
import com.audiomack.model.SearchType
import com.audiomack.network.APIInterface
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.common.Resource
import com.audiomack.ui.tooltip.TooltipFragment
import com.audiomack.usecases.AddMusicToQueuePosition
import com.audiomack.usecases.AddMusicToQueueUseCase
import com.audiomack.usecases.AddMusicToQueueUseCaseResult
import com.audiomack.usecases.FacebookExpressLoginUseCase
import com.audiomack.usecases.OpenMusicUseCase
import com.audiomack.usecases.PlayMusicFromIdResult
import com.audiomack.usecases.PlayMusicFromIdUseCase
import com.audiomack.usecases.ShowSleepTimerPromptUseCase
import com.audiomack.usecases.SleepTimerPromptMode
import com.audiomack.utils.ForegroundManager
import com.audiomack.utils.GeneralPreferences
import com.google.android.gms.auth.api.credentials.Credential
import com.mopub.mobileads.MoPubView
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@Suppress("UNCHECKED_CAST")
class HomeViewModelTest {

    // ViewModel DI
    @Mock private lateinit var activityResultRegistry: ActivityResultRegistry
    @Mock private lateinit var deeplinkDataSource: DeeplinkDataSource
    @Mock private lateinit var mixpanelDataSource: MixpanelDataSource
    @Mock private lateinit var preferencesDataSource: PreferencesDataSource
    @Mock private lateinit var userDataSource: UserDataSource
    @Mock private lateinit var adsDataSource: AdsDataSource
    @Mock private lateinit var remoteVariablesProvider: RemoteVariablesProvider
    @Mock private lateinit var deviceDataSource: DeviceDataSource
    @Mock private lateinit var trackingDataSource: TrackingDataSource
    @Mock private lateinit var eventBus: EventBus
    @Mock private lateinit var generalPreferences: GeneralPreferences
    @Mock private lateinit var artistsDataSource: ArtistsDataSource
    @Mock private lateinit var zendeskDataSource: ZendeskDataSource
    @Mock private lateinit var authenticationDataSource: AuthenticationDataSource
    @Mock private lateinit var premiumDataSource: PremiumDataSource
    @Mock private lateinit var telcoDataSource: TelcoDataSource
    @Mock private lateinit var foregroundManager: ForegroundManager
    @Mock private lateinit var reachabilityDataSource: ReachabilityDataSource
    @Mock private lateinit var featuredSpotDataSource: FeaturedSpotDataSource
    @Mock private lateinit var musicDataSource: MusicDataSource
    @Mock private lateinit var queueDataSource: QueueDataSource
    @Mock private lateinit var musicSyncUseCase: MusicSyncUseCase
    @Mock private lateinit var housekeepingUseCase: HousekeepingUseCase
    @Mock private lateinit var onboardingPlaylistsGenreProvider: OnboardingPlaylistsGenreProvider
    @Mock private lateinit var shareManager: ShareManager
    @Mock private lateinit var inAppUpdatesManager: InAppUpdatesManager
    @Mock private lateinit var workManagerProvider: WorkManagerProvider
    @Mock private lateinit var workManager: WorkManager
    @Mock private lateinit var playerDataSource: PlayerDataSource
    @Mock private lateinit var premiumDownloadDataSource: PremiumDownloadDataSource
    @Mock private lateinit var unlockPremiumDownloadUseCase: IUnlockPremiumDownloadUseCase
    @Mock private lateinit var apiEmailVerification: APIInterface.EmailVerificationInterface
    @Mock private lateinit var sleepTimer: SleepTimer
    @Mock private lateinit var inAppRating: InAppRating
    @Mock private lateinit var playMusicFromIdUseCase: PlayMusicFromIdUseCase
    @Mock private lateinit var addMusicToQueueUseCase: AddMusicToQueueUseCase
    @Mock private lateinit var openMusicUseCase: OpenMusicUseCase
    @Mock private lateinit var showSleepTimerPromptUseCase: ShowSleepTimerPromptUseCase
    @Mock private lateinit var facebookExpressLoginUseCase: FacebookExpressLoginUseCase
    @Mock private lateinit var openLocalMedia: OpenLocalMediaUseCase
    @Mock private lateinit var navigationActionsMock: NavigationActions

    private val navigationEvents: NavigationEvents = NavigationManager.getInstance()
    private val navigationActions: NavigationActions = NavigationManager.getInstance()
    private val alertEvents: AlertEvents = AlertManager
    private val alertTriggers: AlertTriggers = AlertManager
    private val schedulersProvider: SchedulersProvider = TestSchedulersProvider()

    private lateinit var inAppRatingSubject: Subject<InAppRatingResult>
    private lateinit var userLoginSubject: Subject<EventLoginState>

    // SUT
    private lateinit var viewModel: HomeViewModel

    // Observers
    @Mock private lateinit var deeplinkObserver: Observer<Deeplink>
    @Mock private lateinit var smartLockObserver: Observer<Void>
    @Mock private lateinit var deleteSmartLockCredentialsObserver: Observer<Credential>
    @Mock private lateinit var restoreMiniplayerObserver: Observer<Boolean>
    @Mock private lateinit var showUnreadTicketsAlertObserver: Observer<Void>
    @Mock private lateinit var eventShowDownloadSuccessToastObserver: Observer<EventShowDownloadSuccessToast>
    @Mock private lateinit var eventShowDownloadFailureToastObserver: Observer<Void>
    @Mock private lateinit var eventAddedToOfflineInAppMessageObserver: Observer<Void>
    @Mock private lateinit var eventLaunchLoginObserver: Observer<LoginSignupSource>
    @Mock private lateinit var adLayoutVisibleObserver: Observer<Boolean>
    @Mock private lateinit var avatarObserver: Observer<String>
    @Mock private lateinit var myLibraryNotificationsObserver: Observer<String>
    @Mock private lateinit var feedNotificationsObserver: Observer<String>
    @Mock private lateinit var eventOpenPlayerObserver: Observer<Void>
    @Mock private lateinit var eventOpenPlayerMenuObserver: Observer<Void>
    @Mock private lateinit var eventSetupBackStackListenerObserver: Observer<Void>
    @Mock private lateinit var eventShowMiniplayerTooltipObserver: Observer<TooltipFragment.TooltipLocation>
    @Mock private lateinit var eventToggleHUDModeObserver: Observer<ProgressHUDMode>
    @Mock private lateinit var eventShowArtistObserver: Observer<HomeViewModel.ShowArtist>
    @Mock private lateinit var eventShowSongObserver: Observer<HomeViewModel.ShowSong>
    @Mock private lateinit var eventShowAlbumObserver: Observer<HomeViewModel.ShowAlbum>
    @Mock private lateinit var eventShowPlaylistObserver: Observer<HomeViewModel.ShowPlaylist>
    @Mock private lateinit var eventMaximizePlayerObserver: Observer<MaximizePlayerData>
    @Mock private lateinit var eventCloseTooltipObserver: Observer<Void>
    @Mock private lateinit var eventShowGeorestrictedAlertObserver: Observer<Runnable?>
    @Mock private lateinit var eventShowBenchmarkObserver: Observer<HomeViewModel.ShowBenchmark>
    @Mock private lateinit var triggerAppUpdateEventObserver: Observer<Void>
    @Mock private lateinit var showInAppUpdateConfirmationEventObserver: Observer<Void>
    @Mock private lateinit var showInAppUpdateDownloadStartedEventObserver: Observer<Void>
    @Mock private lateinit var showAgeGenderEventObserver: Observer<Void>
    @Mock private lateinit var showDeleteDownloadAlertEventObserver: Observer<AMResultItem>
    @Mock private lateinit var showPremiumDownloadEventObserver: Observer<PremiumDownloadModel>
    @Mock private lateinit var showEmailVerificationResultEventObserver: Observer<Boolean>
    @Mock private lateinit var showRatingPromptEventObserver: Observer<Void>
    @Mock private lateinit var showDeclinedRatingPromptEventObserver: Observer<Void>
    @Mock private lateinit var openAppRatingEventObserver: Observer<Void>
    @Mock private lateinit var showAddedToQueueToastEventObserver: Observer<Void>
    @Mock private lateinit var showPasswordResetScreenEventObserver: Observer<String>
    @Mock private lateinit var showPasswordResetErrorEventObserver: Observer<Void>
    @Mock private lateinit var showSleepTimerPromptEventObserver: Observer<SleepTimerPromptMode>
    @Mock private lateinit var triggerFacebookExpressLoginEventObserver: Observer<Unit>

    private val interstitialObservable = PublishSubject.create<ShowInterstitialResult>()
    private val sleepTimerEventObservable = PublishSubject.create<SleepTimerEvent>()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        inAppRatingSubject = BehaviorSubject.create()
        userLoginSubject = BehaviorSubject.create<EventLoginState>()

        whenever(artistsDataSource.findLoggedArtist()).thenReturn(Observable.error(Exception("Unknown error for tests")))
        whenever(artistsDataSource.updateUserNotifications()).thenReturn(Observable.just(true))
        whenever(preferencesDataSource.defaultGenre).thenReturn(DefaultGenre.ALL)
        whenever(housekeepingUseCase.runHousekeeping()).thenReturn(Completable.complete())
        whenever(musicSyncUseCase.syncMusic()).thenReturn(Completable.complete())
        whenever(inAppUpdatesManager.checkForUpdates()).thenReturn(Single.error(Exception("")))
        whenever(premiumDataSource.premiumObservable).thenReturn(mock())
        whenever(workManager.getWorkInfosForUniqueWorkLiveData(any())).thenReturn(mock())
        whenever(workManagerProvider.workManager).thenReturn(workManager)
        whenever(housekeepingUseCase.downloadsToRestore).thenReturn(Observable.never())
        whenever(adsDataSource.interstitialObservable).thenReturn(interstitialObservable)
        whenever(sleepTimer.sleepEvent).thenReturn(sleepTimerEventObservable)
        whenever(inAppRating.inAppRating).thenReturn(inAppRatingSubject)
        whenever(trackingDataSource.trackIdentity()).thenReturn(Completable.complete())
        whenever(showSleepTimerPromptUseCase.getPromptMode()).thenReturn(Maybe.just(SleepTimerPromptMode.Locked))
        whenever(zendeskDataSource.getUnreadTicketsCount()).thenReturn(Observable.just(
            ZendeskUnreadTicketsData(0, false)
        ))
        whenever(userDataSource.loginEvents).thenReturn(userLoginSubject)

        viewModel = HomeViewModel(
            activityResultRegistry,
            deeplinkDataSource,
            mixpanelDataSource,
            preferencesDataSource,
            userDataSource,
            adsDataSource,
            remoteVariablesProvider,
            deviceDataSource,
            trackingDataSource,
            eventBus,
            generalPreferences,
            artistsDataSource,
            zendeskDataSource,
            authenticationDataSource,
            premiumDataSource,
            telcoDataSource,
            foregroundManager,
            reachabilityDataSource,
            featuredSpotDataSource,
            musicDataSource,
            queueDataSource,
            musicSyncUseCase,
            housekeepingUseCase,
            onboardingPlaylistsGenreProvider,
            schedulersProvider,
            shareManager,
            inAppUpdatesManager,
            workManagerProvider,
            playerDataSource,
            premiumDownloadDataSource,
            unlockPremiumDownloadUseCase,
            apiEmailVerification,
            sleepTimer,
            inAppRating,
            playMusicFromIdUseCase,
            addMusicToQueueUseCase,
            openMusicUseCase,
            showSleepTimerPromptUseCase,
            openLocalMedia,
            navigationEvents,
            navigationActionsMock,
            alertEvents,
            facebookExpressLoginUseCase,
            0L
        ).apply {
            deeplinkEvent.observeForever(deeplinkObserver)
            showSmartLockEvent.observeForever(smartLockObserver)
            deleteSmartLockCredentialsEvent.observeForever(deleteSmartLockCredentialsObserver)
            restoreMiniplayerEvent.observeForever(restoreMiniplayerObserver)
            showUnreadTicketsAlert.observeForever(showUnreadTicketsAlertObserver)
            showDownloadSuccessToastEvent.observeForever(eventShowDownloadSuccessToastObserver)
            showDownloadFailureToastEvent.observeForever(eventShowDownloadFailureToastObserver)
            showAddedToOfflineInAppMessageEvent.observeForever(eventAddedToOfflineInAppMessageObserver)
            launchLoginEvent.observeForever(eventLaunchLoginObserver)
            adLayoutVisible.observeForever(adLayoutVisibleObserver)
            myLibraryAvatar.observeForever(avatarObserver)
            myLibraryNotifications.observeForever(myLibraryNotificationsObserver)
            feedNotifications.observeForever(feedNotificationsObserver)
            openPlayerEvent.observeForever(eventOpenPlayerObserver)
            openPlayerMenuEvent.observeForever(eventOpenPlayerMenuObserver)
            setupBackStackListenerEvent.observeForever(eventSetupBackStackListenerObserver)
            showMiniplayerTooltipEvent.observeForever(eventShowMiniplayerTooltipObserver)
            toggleHUDModeEvent.observeForever(eventToggleHUDModeObserver)
            showArtistEvent.observeForever(eventShowArtistObserver)
            showSongEvent.observeForever(eventShowSongObserver)
            showAlbumEvent.observeForever(eventShowAlbumObserver)
            showPlaylistEvent.observeForever(eventShowPlaylistObserver)
            launchPlayerEvent.observeForever(eventMaximizePlayerObserver)
            closeTooltipEvent.observeForever(eventCloseTooltipObserver)
            showGeorestrictedAlertEvent.observeForever(eventShowGeorestrictedAlertObserver)
            showBenchmarkEvent.observeForever(eventShowBenchmarkObserver)
            triggerAppUpdateEvent.observeForever(triggerAppUpdateEventObserver)
            showInAppUpdateConfirmationEvent.observeForever(showInAppUpdateConfirmationEventObserver)
            showInAppUpdateDownloadStartedEvent.observeForever(showInAppUpdateDownloadStartedEventObserver)
            showAgeGenderEvent.observeForever(showAgeGenderEventObserver)
            showDeleteDownloadAlertEvent.observeForever(showDeleteDownloadAlertEventObserver)
            showPremiumDownloadEvent.observeForever(showPremiumDownloadEventObserver)
            showEmailVerificationResultEvent.observeForever(showEmailVerificationResultEventObserver)
            showRatingPromptEvent.observeForever(showRatingPromptEventObserver)
            showDeclinedRatingPromptEvent.observeForever(showDeclinedRatingPromptEventObserver)
            openAppRatingEvent.observeForever(openAppRatingEventObserver)
            showAddedToQueueToastEvent.observeForever(showAddedToQueueToastEventObserver)
            showPasswordResetScreenEvent.observeForever(showPasswordResetScreenEventObserver)
            showPasswordResetErrorEvent.observeForever(showPasswordResetErrorEventObserver)
            showSleepTimerPromptEvent.observeForever(showSleepTimerPromptEventObserver)
            triggerFacebookExpressLoginEvent.observeForever(triggerFacebookExpressLoginEventObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
        NavigationManager.destroy()
    }

    @Test
    fun `clear subscriptions`() {
        viewModel.cleanup()
        verify(eventBus).unregister(any())
        verify(foregroundManager).removeListener(any())
    }

    @Test
    fun `did login`() {
        userLoginSubject.onNext(EventLoginState.LOGGED_IN)
        verify(userDataSource, atLeast(1)).isLoggedIn()
    }

    @Test
    fun `did logout`() {
        userLoginSubject.onNext(EventLoginState.LOGGED_OUT)
        verify(deeplinkObserver).onChanged(argWhere { it is Deeplink.Trending })
    }

    @Test
    fun `did cancel login`() {
        userLoginSubject.onNext(EventLoginState.CANCELED_LOGIN)
        verify(userDataSource, never()).isLoggedIn()
    }

    @Test
    fun `premium status changed`() {
        viewModel.premiumObserver.onNext(false)
        verify(adLayoutVisibleObserver).onChanged(any())
    }

    @Test
    fun `became foreground`() {
        viewModel.foregroundListener.onBecameForeground()
        verify(adsDataSource).restartAds()
        verify(featuredSpotDataSource).pick()
    }

    @Test
    fun `became background`() {
        viewModel.foregroundListener.onBecameBackground()
        verify(adsDataSource).stopAds()
    }

    @Test
    fun `on create`() {
        viewModel.onCreate(mock())
        verify(trackingDataSource).trackIdentity()
        verify(eventSetupBackStackListenerObserver).onChanged(null)
        verify(housekeepingUseCase).runHousekeeping()
        verify(musicSyncUseCase).syncMusic()
        verify(onboardingPlaylistsGenreProvider).trackAppSession()
    }

    @Test
    fun `on create, redirects to fresh install deeplink that is modal, also loads Trending behind the scenes`() {
        whenever(adsDataSource.isFreshInstall()).thenReturn(true)
        whenever(remoteVariablesProvider.firstOpeningDeeplink).thenReturn("audiomack://suggested_follows")
        whenever(deeplinkDataSource.obtainDeeplink(anyOrNull())).doReturn(Deeplink.SuggestedFollows)
        viewModel.onCreate(mock())
        verify(deeplinkObserver, times(1)).onChanged(eq(Deeplink.Trending(null)))
        verify(housekeepingUseCase).runHousekeeping()
        verify(musicSyncUseCase).syncMusic()
    }

    @Test
    fun `on create, redirects to fresh install deeplink that is not modal`() {
        whenever(adsDataSource.isFreshInstall()).thenReturn(true)
        whenever(remoteVariablesProvider.firstOpeningDeeplink).thenReturn("audiomack://music_songs")
        whenever(deeplinkDataSource.obtainDeeplink(anyOrNull())).doReturn(Deeplink.TopSongs(null))
        viewModel.onCreate(mock())
        verify(deeplinkObserver, times(1)).onChanged(eq(Deeplink.TopSongs(null)))
        verify(deeplinkObserver, never()).onChanged(eq(Deeplink.Trending(null)))
        verify(housekeepingUseCase).runHousekeeping()
        verify(musicSyncUseCase).syncMusic()
    }

    @Test
    fun `on destroy`() {
        viewModel.onDestroy()
        verify(adsDataSource).destroy()
        verify(mixpanelDataSource).flushEvents()
    }

    @Test
    fun `on intent received, deeplink available`() {
        val deeplink = Deeplink.NowPlaying
        whenever(deeplinkDataSource.obtainDeeplink(anyOrNull())).thenReturn(deeplink)
        viewModel.onIntentReceived(null)
        verify(deeplinkObserver).onChanged(eq(deeplink))
    }

    @Test
    fun `on intent received, deeplink not available`() {
        whenever(deeplinkDataSource.obtainDeeplink(anyOrNull())).thenReturn(null)
        viewModel.onIntentReceived(null)
        verifyZeroInteractions(deeplinkObserver)
    }

    @Test
    fun `on resume, calls mixpanel push tracking`() {
        val intent: Intent = mock()
        viewModel.onResume(intent)
        verify(mixpanelDataSource).trackPushOpened(eq(intent))
    }

    @Test
    fun `on resume, doesn't call mixpanel push tracking because of null intent`() {
        viewModel.onResume(null)
        verify(mixpanelDataSource, times(0)).trackPushOpened(anyOrNull())
    }

    @Test
    fun `on resume, notify Foreground Manager`() {
        viewModel.onResume(any())
        verify(foregroundManager).setActivityResumed(anyString())
    }

    @Test
    fun `on resume, notify ads`() {
        viewModel.onResume(any())
        verify(adsDataSource).onBannerAppeared()
    }

    @Test
    fun `on pause, notify Foreground Manager`() {
        viewModel.onPause()
        verify(foregroundManager).setActivityPaused(anyString())
    }

    @Test
    fun `feed tab click observed`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        viewModel.onFeedTabClicked()
        verify(deeplinkObserver).onChanged(eq(Deeplink.Timeline))
    }

    @Test
    fun `playlists tab click observed`() {
        viewModel.onPlaylistsTabClicked()
        verify(deeplinkObserver).onChanged(eq(Deeplink.Playlists()))
    }

    @Test
    fun `browse tab click, no default genre`() {
        whenever(preferencesDataSource.defaultGenre).thenReturn(DefaultGenre.ALL)
        viewModel.onBrowseTabClicked()
        verify(deeplinkObserver).onChanged(eq(Deeplink.Trending(null)))
    }

    @Test
    fun `browse tab click, electronic default genre`() {
        whenever(preferencesDataSource.defaultGenre).thenReturn(DefaultGenre.ELECTRONIC)
        viewModel.onBrowseTabClicked()
    }

    @Test
    fun `search tab click`() {
        viewModel.onSearchTabClicked()
        verify(deeplinkObserver).onChanged(eq(Deeplink.Search()))
    }

    @Test
    fun `my library tab click, not logged in, redirects to login`() {
        whenever(userDataSource.isLoggedInAsync()).thenReturn(Single.just(false))
        viewModel.onMyLibraryTabClicked()
        verify(navigationActionsMock).launchLogin(MyLibrary)
    }

    @Test
    fun `my library tab click, logged in with no favorites, redirects to downloads`() {
        whenever(userDataSource.isLoggedInAsync()).thenReturn(Single.just(true))
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.hasFavorites).thenReturn(false)
        viewModel.onMyLibraryTabClicked()
        verify(deeplinkObserver).onChanged(eq(Deeplink.MyDownloads))
    }

    @Test
    fun `my library tab click, logged in with at least one favorite, redirects to favorites`() {
        whenever(userDataSource.isLoggedInAsync()).thenReturn(Single.just(true))
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.hasFavorites).thenReturn(true)
        viewModel.onMyLibraryTabClicked()
        verify(deeplinkObserver).onChanged(eq(Deeplink.MyFavorites))
    }

    @Test
    fun `remove banner button click`() {
        viewModel.onRemoveBannerClicked()
        verify(deeplinkObserver).onChanged(eq(Deeplink.Premium(InAppPurchaseMode.BannerAdDismissal)))
    }

    @Test
    fun `smartlock ready, not shown because user is already logged in`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(adsDataSource.isFreshInstall()).thenReturn(false)
        viewModel.onSmartLockReady(false)
        verifyZeroInteractions(smartLockObserver)
    }

    @Test
    fun `smartlock ready, not shown because it's not the first session`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(false)
        whenever(adsDataSource.isFreshInstall()).thenReturn(false)
        viewModel.onSmartLockReady(false)
        verifyZeroInteractions(smartLockObserver)
    }

    @Test
    fun `smartlock ready, not shown because onboarding is currently shown`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(false)
        whenever(adsDataSource.isFreshInstall()).thenReturn(true)
        viewModel.onSmartLockReady(true)
        verifyZeroInteractions(smartLockObserver)
    }

    @Test
    fun `smartlock ready, all conditions met to be shown`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(false)
        whenever(adsDataSource.isFreshInstall()).thenReturn(true)
        viewModel.onSmartLockReady(false)
        verify(smartLockObserver).onChanged(null)
    }

    @Test
    fun `ad layout ready`() {
        val adViewHome: MoPubView = mock()
        viewModel.onAdLayoutReady(adViewHome)
        verify(adsDataSource).setHomeViewLoaded()
        verify(adsDataSource).postInit(adViewHome)
        verify(adsDataSource).initOgury()
    }

    @Test
    fun `on player instantiated, bookmarks are disabled`() {
        whenever(remoteVariablesProvider.bookmarksEnabled).thenReturn(false)
        viewModel.onPlayerInstantiated()
        verifyZeroInteractions(restoreMiniplayerObserver)
    }

    @Test
    fun `on player instantiated, status is not valid`() {
        whenever(queueDataSource.currentItem).thenReturn(null)
        viewModel.onPlayerInstantiated()
        verifyZeroInteractions(restoreMiniplayerObserver)
    }

    @Test
    fun `on player instantiated, pending deeplink handling in progress`() {
        whenever(remoteVariablesProvider.bookmarksEnabled).thenReturn(false)
        viewModel.onOfflineRedirectDetected()
        viewModel.onResume(null)
        viewModel.onPlayerInstantiated()
        verifyZeroInteractions(restoreMiniplayerObserver)
    }

    @Test
    fun `on player instantiated, all conditions met to restore miniplayer`() {
        whenever(queueDataSource.currentItem).thenReturn(mock())
        viewModel.onPlayerInstantiated()
        verify(restoreMiniplayerObserver).onChanged(true)
    }

    @Test
    fun `mini player restored when queue item is set if not already restored`() {
        viewModel.restoreMiniplayerEvent.postValue(false)
        viewModel.queueObserver.onNext(mock())
        verify(restoreMiniplayerObserver, times(1)).onChanged(true)
    }

    @Test
    fun `mini player not restored when queue item is set if already restored`() {
        viewModel.restoreMiniplayerEvent.postValue(true)
        viewModel.queueObserver.onNext(mock())
        verify(restoreMiniplayerObserver, times(1)).onChanged(true)
    }

    @Test
    fun `offline mode detected, navigate deeplink then set it to null`() {
        whenever(userDataSource.isLoggedInAsync()).thenReturn(Single.just(true))
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        viewModel.onOfflineRedirectDetected()
        viewModel.onIntentReceived(null)
        viewModel.onIntentReceived(null)
        verify(deeplinkObserver, times(1)).onChanged(Deeplink.MyDownloads)
    }

    @Test
    fun `login with SmartLock credentials, null credential`() {
        viewModel.loginWithSmartLockCredentials(null)
        verifyZeroInteractions(authenticationDataSource)
        verifyZeroInteractions(deleteSmartLockCredentialsObserver)
    }

    @Test
    fun `login with SmartLock credentials, valid credential, success`() {
        val credential = mock<Credential> {
            on { id } doReturn "user@audiomack.com"
            on { password } doReturn "1234567890"
        }
        whenever(authenticationDataSource.loginWithEmailPassword(anyOrNull(), anyOrNull())).thenReturn(
            Single.just(Credentials()))
        viewModel.loginWithSmartLockCredentials(credential)
        verify(authenticationDataSource).loginWithEmailPassword(anyOrNull(), anyOrNull())
        verify(userDataSource).onLoggedIn()
        verify(mixpanelDataSource).trackLogin(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
        verify(trackingDataSource).trackLogin()
        verifyZeroInteractions(deleteSmartLockCredentialsObserver)
    }

    @Test
    fun `login with SmartLock credentials, valid credential, bad failure`() {
        val credential = mock<Credential> {
            on { id } doReturn "user@audiomack.com"
            on { password } doReturn "1234567890"
        }
        whenever(authenticationDataSource.loginWithEmailPassword(anyOrNull(), anyOrNull())).thenReturn(Single.error(LoginException("Test", 400)))
        viewModel.loginWithSmartLockCredentials(credential)
        verify(authenticationDataSource).loginWithEmailPassword(anyOrNull(), anyOrNull())
        verify(deleteSmartLockCredentialsObserver).onChanged(credential)
    }

    @Test
    fun `login with SmartLock credentials, valid credential, network failure`() {
        val credential = mock<Credential> {
            on { id } doReturn "user@audiomack.com"
            on { password } doReturn "1234567890"
        }
        whenever(authenticationDataSource.loginWithEmailPassword(anyOrNull(), anyOrNull())).thenReturn(Single.error(LoginException("Test", null)))
        viewModel.loginWithSmartLockCredentials(credential)
        verify(authenticationDataSource).loginWithEmailPassword(anyOrNull(), anyOrNull())
        verifyZeroInteractions(deleteSmartLockCredentialsObserver)
    }

    @Test
    fun `on player maximized`() {
        viewModel.onPlayerMaximized()
        verify(eventCloseTooltipObserver).onChanged(null)
    }

    @Test
    fun `on player playlist tooltip closed`() {
        viewModel.onPlayerPlaylistTooltipClosed()
        verify(generalPreferences).setPlayerPlaylistTooltipShown()
    }

    @Test
    fun `on player queue tooltip closed`() {
        viewModel.onPlayerQueueTooltipClosed()
        verify(generalPreferences).setPlayerQueueTooltipShown()
    }

    @Test
    fun `on player eq tooltip closed`() {
        viewModel.onPlayerEqTooltipClosed()
        verify(generalPreferences).setPlayerEqTooltipShown()
    }

    @Test
    fun `on player scroll tooltip closed`() {
        viewModel.onPlayerScrollTooltipShown()
        verify(generalPreferences).setPlayerScrollTooltipShown()
    }

    @Test
    fun `show player ad`() {
        val showWhenReady = false
        viewModel.showPlayerAd(showWhenReady)
        verify(adsDataSource).showPlayerAd(eq(showWhenReady))
    }

    @Test
    fun `on login required, accepted`() {
        val source = LoginSignupSource.AppLaunch
        viewModel.onLoginRequiredAccepted(source)
        verify(eventLaunchLoginObserver).onChanged(eq(source))
    }

    @Test
    fun `on login required, declined`() {
        viewModel.onLoginRequiredDeclined()
        verify(userDataSource).onLoginCanceled()
    }

    @Test
    fun `on cast init exception`() {
        viewModel.onCastInitException()
        verify(deviceDataSource).castAvailable = false
    }

    @Test
    fun `on miniplayer tooltip location set, no need to show`() {
        whenever(generalPreferences.needToShowMiniplayerTooltip()).thenReturn(false)
        viewModel.setMiniplayerTooltipLocation(mock())
        verify(generalPreferences).needToShowMiniplayerTooltip()
        verifyZeroInteractions(eventShowMiniplayerTooltipObserver)
    }

    @Test
    fun `on miniplayer tooltip location set, need to show`() {
        whenever(generalPreferences.needToShowMiniplayerTooltip()).thenReturn(true)
        viewModel.setMiniplayerTooltipLocation(mock())
        verify(generalPreferences).needToShowMiniplayerTooltip()
        verify(eventShowMiniplayerTooltipObserver).onChanged(any())
    }

    @Test
    fun `on miniplayer tooltip shown`() {
        viewModel.onMiniplayerTooltipShown()
        verify(generalPreferences).setMiniplayerTooltipShown()
    }

    @Test
    fun `on artist screen requested, failure`() {
        val slug = "matteinn"
        val tab = "favorites"
        whenever(artistsDataSource.artistData(slug)).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onArtistScreenRequested(slug, tab)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verifyZeroInteractions(eventShowArtistObserver)
    }

    @Test
    fun `on artist screen requested, success`() {
        val slug = "matteinn"
        val tab = "favorites"
        whenever(artistsDataSource.artistData(slug)).thenReturn(Observable.just(mock()))
        viewModel.onArtistScreenRequested(slug, tab)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Dismiss))
        verify(eventShowArtistObserver).onChanged(argWhere { it.tab == tab })
    }

    @Test
    fun `on song requested, failure`() {
        val id = "matteinn/burp"
        whenever(musicDataSource.getSongInfo(id)).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onSongRequested(id, MixpanelSource.empty)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verifyZeroInteractions(eventShowSongObserver)
    }

    @Test
    fun `on song requested, success`() {
        val id = "matteinn/burp"
        whenever(musicDataSource.getSongInfo(id)).thenReturn(Observable.just(mock()))
        viewModel.onSongRequested(id, MixpanelSource.empty)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Dismiss))
        verify(eventShowSongObserver).onChanged(any())
    }

    @Test
    fun `on album requested, remotely, failure`() {
        val id = "matteinn/burp"
        whenever(musicDataSource.getAlbumInfo(id)).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onAlbumRequested(id, MixpanelSource.empty)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verifyZeroInteractions(eventShowSongObserver)
    }

    @Test
    fun `on album requested, remotely, success`() {
        val id = "matteinn/burp"
        whenever(musicDataSource.getAlbumInfo(id)).thenReturn(Observable.just(mock()))
        viewModel.onAlbumRequested(id, MixpanelSource.empty)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Dismiss))
        verify(eventShowAlbumObserver).onChanged(any())
    }

    @Test
    fun `on album requested, locally, failure`() {
        val id = "matteinn/burp"
        whenever(musicDataSource.getOfflineResource(id)).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onAlbumRequested(id, MixpanelSource("", MixpanelPageMyLibraryOffline))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verifyZeroInteractions(eventShowSongObserver)
    }

    @Test
    fun `on album requested, locally, no entry found`() {
        val id = "matteinn/burp"
        whenever(musicDataSource.getOfflineResource(id)).thenReturn(
            Observable.just(Resource.Failure(mock()))
        )
        viewModel.onAlbumRequested(id, MixpanelSource("", MixpanelPageMyLibraryOffline))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(
            eventToggleHUDModeObserver,
            times(1)
        ).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verifyZeroInteractions(eventShowSongObserver)
    }

    @Test
    fun `on album requested, locally, success`() {
        val id = "matteinn/burp"
        whenever(musicDataSource.getOfflineResource(id)).thenReturn(
            Observable.just(Resource.Success(mock()))
        )
        viewModel.onAlbumRequested(id, MixpanelSource("", MixpanelPageMyLibrarySearchOffline))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Dismiss))
        verify(eventShowAlbumObserver).onChanged(any())
    }

    @Test
    fun `on playlist requested, not reachable, failure`() {
        val id = "matteinn/burp"
        whenever(reachabilityDataSource.networkAvailable).thenReturn(false)
        whenever(musicDataSource.getOfflineResource(any())).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onPlaylistRequested(id, MixpanelSource.empty)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verifyZeroInteractions(eventShowPlaylistObserver)
    }

    @Test
    fun `on playlist requested, not reachable, success`() {
        val id = "matteinn/burp"
        whenever(reachabilityDataSource.networkAvailable).thenReturn(false)
        whenever(musicDataSource.getOfflineResource(any())).thenReturn(
            Observable.just(Resource.Success(mock()))
        )
        viewModel.onPlaylistRequested(id, MixpanelSource.empty)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Dismiss))
        verify(eventShowPlaylistObserver).onChanged(any())
    }

    @Test
    fun `on playlist requested, reachable, failure`() {
        val id = "matteinn/burp"
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(musicDataSource.getPlaylistInfo(any())).thenReturn(Observable.error(Exception("Unknown error for tests")))
        whenever(musicDataSource.getOfflineResource(any())).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onPlaylistRequested(id, MixpanelSource.empty)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verifyZeroInteractions(eventShowPlaylistObserver)
    }

    @Test
    fun `on playlist requested, reachable, success, no entry in DB`() {
        val id = "matteinn/burp"
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn "123"
        }
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(musicDataSource.getPlaylistInfo(any())).thenReturn(Observable.just(playlist))
        whenever(musicDataSource.getOfflineResource(any())).thenReturn(
            Observable.error(MusicDAOException("Unknown error for tests"))
        )
        viewModel.onPlaylistRequested(id, MixpanelSource.empty)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Dismiss))
        verify(eventShowPlaylistObserver).onChanged(any())
    }

    @Test
    fun `on playlist requested, reachable, success, found entry in DB`() {
        val id = "matteinn/burp"
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn "123"
        }
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(musicDataSource.getPlaylistInfo(any())).thenReturn(Observable.just(playlist))
        whenever(musicDataSource.getOfflineResource(any())).thenReturn(
            Observable.just(Resource.Success(playlist))
        )
        viewModel.onPlaylistRequested(id, MixpanelSource.empty)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Dismiss))
        verify(eventShowPlaylistObserver).onChanged(any())
    }

    @Test
    fun `on player show requested, queue is empty`() {
        whenever(queueDataSource.items).thenReturn(emptyList())
        viewModel.onPlayerShowRequested()
        verifyZeroInteractions(eventOpenPlayerObserver)
    }

    @Test
    fun `on player show requested, queue is not empty`() {
        whenever(queueDataSource.items).thenReturn(listOf(mock(), mock()))
        viewModel.onPlayerShowRequested()
        verify(eventOpenPlayerObserver).onChanged(null)
    }

    @Test
    fun `on miniplayer swiped up`() {
        viewModel.onMiniplayerSwipedUp()
        verify(eventMaximizePlayerObserver).onChanged(eq(MaximizePlayerData()))
    }

    @Test
    fun `on maximize player requested`() {
        val data = MaximizePlayerData()
        viewModel.onMaximizePlayerRequested(data)
        verify(eventMaximizePlayerObserver).onChanged(eq(data))
    }

    @Test
    fun `on maximize player requested, frozen limited download in offline mode, available unfreezes`() {
        val data = MaximizePlayerData(
            item = mock {
                on { itemId } doReturn "1"
                on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
                on { isDownloadFrozen } doReturn true
            },
            inOfflineScreen = true
        )
        whenever(premiumDownloadDataSource.premiumDownloadLimit).thenReturn(20)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).thenReturn(0)
        whenever(premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(any())).thenReturn(1)
        viewModel.onMaximizePlayerRequested(data)
        verifyZeroInteractions(eventMaximizePlayerObserver)
        verify(showPremiumDownloadEventObserver).onChanged(argWhere { it.alertTypeLimited == PremiumLimitedDownloadAlertViewType.PlayFrozenOfflineWithAvailableUnfreezes })
    }

    @Test
    fun `on maximize player requested, frozen limited download in offline mode, no unfreezes available`() {
        val data = MaximizePlayerData(
            item = mock {
                on { itemId } doReturn "1"
                on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
                on { isDownloadFrozen } doReturn true
            },
            inOfflineScreen = true
        )
        whenever(premiumDownloadDataSource.premiumDownloadLimit).thenReturn(20)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).thenReturn(20)
        whenever(premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(any())).thenReturn(1)
        viewModel.onMaximizePlayerRequested(data)
        verifyZeroInteractions(eventMaximizePlayerObserver)
        verify(showPremiumDownloadEventObserver).onChanged(argWhere { it.alertTypeLimited == PremiumLimitedDownloadAlertViewType.PlayFrozenOffline })
    }

    @Test
    fun `on maximize player requested, frozen premium-only download in offline mode`() {
        val data = MaximizePlayerData(
            item = mock {
                on { itemId } doReturn "1"
                on { downloadType } doReturn AMResultItem.MusicDownloadType.Premium
                on { isDownloadFrozen } doReturn true
            },
            inOfflineScreen = true
        )
        viewModel.onMaximizePlayerRequested(data)
        verifyZeroInteractions(eventMaximizePlayerObserver)
        verify(showPremiumDownloadEventObserver).onChanged(argWhere { it.alertTypePremium == PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline })
    }

    @Test
    fun `on deeplink consumed`() {
        viewModel.onDeeplinkConsumed(null)
        verify(deeplinkDataSource).handlingDeeplink = false
    }

    @Test
    fun `on link requested`() {
        val link = "https://google.com"
        whenever(deeplinkDataSource.obtainDeeplink(anyOrNull())).doReturn(Deeplink.Link(mock()))
        viewModel.onLinkRequested(link)
        verify(deeplinkDataSource).obtainDeeplink(anyOrNull())
    }

    @Test
    fun `on georestricted item opened`() {
        val onDelete = mock<Runnable>()
        viewModel.onGeorestrictedMusicClicked(onDelete)
        verify(eventShowGeorestrictedAlertObserver).onChanged(onDelete)
    }

    @Test
    fun `got event EventShowUnreadTicketsAlert, show`() {
        viewModel.onResume(null)
        viewModel.onMessageEvent(EventShowUnreadTicketsAlert())
        verify(showUnreadTicketsAlertObserver).onChanged(null)
    }

    @Test
    fun `got event EventShowUnreadTicketsAlert, do not show because view is paused`() {
        viewModel.onResume(null)
        viewModel.onPause()
        viewModel.onMessageEvent(EventShowUnreadTicketsAlert())
        verifyZeroInteractions(showUnreadTicketsAlertObserver)
    }

    @Test
    fun `got event EventToggleRemoveAdVisibility`() {
        val visible = true
        viewModel.onMessageEvent(EventToggleRemoveAdVisibility(visible))
        verify(adLayoutVisibleObserver).onChanged(visible)
    }

    @Test
    fun `got event EventShowDownloadSuccessToast`() {
        val event = EventShowDownloadSuccessToast(mock())
        viewModel.onMessageEvent(event)
        verify(eventShowDownloadSuccessToastObserver).onChanged(eq(event))
    }

    @Test
    fun `got event EventShowDownloadFailureToast`() {
        viewModel.onMessageEvent(EventShowDownloadFailureToast())
        verify(eventShowDownloadFailureToastObserver).onChanged(null)
    }

    @Test
    fun `got event EventShowAddedToOfflineInAppMessage, need to show in app message`() {
        whenever(generalPreferences.needToShowDownloadInAppMessage()).thenReturn(true)
        viewModel.onMessageEvent(EventShowAddedToOfflineInAppMessage(false, MixpanelSource.empty, 0))
        verify(eventAddedToOfflineInAppMessageObserver).onChanged(null)
        verify(generalPreferences).setDownloadInAppMessageShown()
        verifyZeroInteractions(showPremiumDownloadEventObserver)
    }

    @Test
    fun `got event EventShowAddedToOfflineInAppMessage, no need to show in app message`() {
        whenever(generalPreferences.needToShowDownloadInAppMessage()).thenReturn(false)
        viewModel.onMessageEvent(EventShowAddedToOfflineInAppMessage(false, MixpanelSource.empty, 0))
        verifyZeroInteractions(eventAddedToOfflineInAppMessageObserver)
        verifyZeroInteractions(showPremiumDownloadEventObserver)
    }

    @Test
    fun `got event EventShowAddedToOfflineInAppMessage, show regular in app message (not limited download) because user is premium`() {
        whenever(generalPreferences.needToShowDownloadInAppMessage()).thenReturn(true)
        whenever(premiumDataSource.isPremium).thenReturn(true)
        viewModel.onMessageEvent(EventShowAddedToOfflineInAppMessage(true, MixpanelSource.empty, 0))
        verify(eventAddedToOfflineInAppMessageObserver).onChanged(null)
        verify(generalPreferences).setDownloadInAppMessageShown()
        verifyZeroInteractions(showPremiumDownloadEventObserver)
    }

    @Test
    fun `got event EventPlayer, OPEN command`() {
        viewModel.onMessageEvent(EventPlayer(PlayerCommand.OPEN))
        verify(eventOpenPlayerObserver).onChanged(null)
    }

    @Test
    fun `got event EventPlayer, TWO_DOTS command`() {
        viewModel.onMessageEvent(EventPlayer(PlayerCommand.MENU))
        verify(eventOpenPlayerMenuObserver).onChanged(null)
    }

    @Test
    fun `update UI with valid artist`() {
        val img = "https://"
        val artist: AMArtist = mock {
            on { tinyImage } doReturn img
            on { unseenNotificationsCount } doReturn 10
            on { feedCount } doReturn 205
        }
        viewModel.updateUI(artist)
        verify(avatarObserver).onChanged(img)
        verify(myLibraryNotificationsObserver).onChanged("10")
        verify(feedNotificationsObserver).onChanged("99+")
    }

    @Test
    fun `on search requested`() {
        val query = "Rock"
        val type = SearchType.Tag
        viewModel.onSearchRequested(query, type)
        verify(deeplinkObserver).onChanged(eq(Deeplink.Search(query, type)))
    }

    @Test
    fun `on music info tapped`() {
        val music = mock<AMResultItem>()
        val observer: Observer<AMResultItem> = mock()
        viewModel.openMusicInfoEvent.observeForever(observer)
        viewModel.onMusicInfoTapped(music)
        verify(observer).onChanged(any())
    }

    @Test
    fun `on notifications requested observed, allow navigation if user is logged in`() {
        whenever(userDataSource.isLoggedInAsync()).thenReturn(Single.just(true))
        viewModel.onNotificationsRequested()
        verify(navigationActionsMock).launchNotificationsEvent()
    }

    @Test
    fun `on notifications requested observed, deny navigation if user is not logged in`() {
        whenever(userDataSource.isLoggedInAsync()).thenReturn(Single.just(false))
        viewModel.onNotificationsRequested()
        verify(navigationActionsMock, never()).launchNotificationsEvent()
    }

    @Test
    fun `on benchmark requested, success`() {
        val id = "matteinn/burp"
        val type = "song"
        val benchmark = mock<BenchmarkModel>()
        whenever(musicDataSource.getMusicInfo(id, type)).thenReturn(Observable.just(mock()))
        viewModel.onBenchmarkRequested(id, type, benchmark, MixpanelSource.empty, "")
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Dismiss))
        verify(eventShowBenchmarkObserver).onChanged(any())
    }

    @Test
    fun `on benchmark requested, failure`() {
        val id = "matteinn/burp"
        val type = "song"
        val benchmark = mock<BenchmarkModel>()
        whenever(musicDataSource.getMusicInfo(id, type)).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onBenchmarkRequested(id, type, benchmark, MixpanelSource.empty, "")
        verify(eventToggleHUDModeObserver, times(1)).onChanged(eq(ProgressHUDMode.Loading))
        verify(eventToggleHUDModeObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verifyZeroInteractions(eventShowBenchmarkObserver)
    }

    @Test
    fun `check in-app-updates, ready to download flexible update`() {
        whenever(inAppUpdatesManager.checkForUpdates()).thenReturn(Single.just(InAppUpdateAvailabilityResult.ReadyToDownload(
            InAppUpdatesMode.Flexible)))
        viewModel.onResume(null)
        viewModel.onResume(null)
        verify(triggerAppUpdateEventObserver, times(1)).onChanged(null)
    }

    @Test
    fun `check in-app-updates, ready to download immediate update`() {
        whenever(inAppUpdatesManager.checkForUpdates()).thenReturn(Single.just(InAppUpdateAvailabilityResult.ReadyToDownload(
            InAppUpdatesMode.Immediate)))
        viewModel.onResume(null)
        viewModel.onResume(null)
        verify(triggerAppUpdateEventObserver, times(2)).onChanged(null)
    }

    @Test
    fun `check in-app-updates, ready to install`() {
        whenever(inAppUpdatesManager.checkForUpdates()).thenReturn(Single.just(InAppUpdateAvailabilityResult.ReadyToInstall))
        viewModel.onResume(null)
        verify(showInAppUpdateConfirmationEventObserver).onChanged(null)
    }

    @Test
    fun `check in-app-updates, need to resume interrupted download`() {
        whenever(inAppUpdatesManager.checkForUpdates()).thenReturn(Single.just(InAppUpdateAvailabilityResult.NeedToResumeDownload))
        viewModel.onResume(null)
        verify(triggerAppUpdateEventObserver).onChanged(null)
    }

    @Test
    fun `triggerUpdate, update downloaded`() {
        whenever(inAppUpdatesManager.triggerUpdate(any())).thenReturn(Observable.just(InAppUpdateResult.Downloaded))
        viewModel.triggerUpdate(mock())
        verify(inAppUpdatesManager).triggerUpdate(any())
        verify(showInAppUpdateConfirmationEventObserver).onChanged(null)
    }

    @Test
    fun `triggerUpdate, update started`() {
        whenever(inAppUpdatesManager.triggerUpdate(any())).thenReturn(Observable.just(InAppUpdateResult.FlexibleDownloadStarted))
        viewModel.triggerUpdate(mock())
        verify(inAppUpdatesManager).triggerUpdate(any())
        verify(showInAppUpdateDownloadStartedEventObserver).onChanged(null)
    }

    @Test
    fun `restart after flexible in-app-update`() {
        viewModel.restartAfterUpdate()
        verify(inAppUpdatesManager).applyUpdate()
    }

    @Test
    fun `premium only frozen delete`() {
        val id = "matteinn/burp"
        whenever(musicDataSource.getOfflineItem(id)).thenReturn(Single.just(mock()))
        viewModel.onDeleteDownloadRequested(id)
        verify(showDeleteDownloadAlertEventObserver).onChanged(any())
    }

    @Test
    fun `showAgeGenderEvent observed when logged in user needs to complete his profile`() {
        val artist = mock<AMArtist> {
            on { needsProfileCompletion } doReturn true
        }
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(artistsDataSource.updateUserData()).thenReturn(Observable.just(artist))

        userLoginSubject.onNext(EventLoginState.LOGGED_IN)

        verify(showAgeGenderEventObserver).onChanged(null)
    }

    @Test
    fun `showAgeGenderEvent not observed when logged in user doesn't need to complete his profile`() {
        val artist = mock<AMArtist> {
            on { needsProfileCompletion } doReturn false
        }
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(artistsDataSource.updateUserData()).thenReturn(Observable.just(artist))

        userLoginSubject.onNext(EventLoginState.LOGGED_IN)

        verifyZeroInteractions(showAgeGenderEventObserver)
    }

    @Test
    fun `showAgeGenderEvent not observed when user is not logged in`() {
        whenever(userDataSource.getUserAsync()).thenReturn(Observable.error(Exception("Not logged in")))
        viewModel.onResume(null)
        verifyZeroInteractions(showAgeGenderEventObserver)
    }

    @Test
    fun `unlock frozen download`() {
        val id = "123"
        whenever(unlockPremiumDownloadUseCase.unlockFrozenDownload(id)).thenReturn(mock())
        viewModel.unlockFrozenDownload(id)
        verify(unlockPremiumDownloadUseCase).unlockFrozenDownload(id)
    }

    @Test
    fun `stream frozen music, success, play`() {
        val music = mock<AMResultItem>()
        whenever(musicDataSource.getMusicInfo(any(), any())).thenReturn(Observable.just(music))

        viewModel.streamFrozenMusic(mock(), "123", MusicType.Song, MixpanelSource.empty, "", ActionToBeResumed.Play)

        verify(eventToggleHUDModeObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(eventMaximizePlayerObserver, times(1)).onChanged(any())
    }

    @Test
    fun `stream frozen music, success, play next`() {
        val music = mock<AMResultItem>()
        whenever(musicDataSource.getMusicInfo(any(), any())).thenReturn(Observable.just(music))

        viewModel.streamFrozenMusic(mock(), "123", MusicType.Song, MixpanelSource.empty, "", ActionToBeResumed.PlayNext)

        verify(eventToggleHUDModeObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(eventMaximizePlayerObserver, never()).onChanged(any())
        verify(music, times(1)).playNext(any(), any(), any(), any())
    }

    @Test
    fun `stream frozen music, success, play later`() {
        val music = mock<AMResultItem>()
        whenever(musicDataSource.getMusicInfo(any(), any())).thenReturn(Observable.just(music))

        viewModel.streamFrozenMusic(mock(), "123", MusicType.Song, MixpanelSource.empty, "", ActionToBeResumed.PlayLater)

        verify(eventToggleHUDModeObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(eventMaximizePlayerObserver, never()).onChanged(any())
        verify(music, times(1)).playLater(any(), any(), any(), any())
    }

    @Test
    fun `stream frozen music, failure`() {
        whenever(musicDataSource.getMusicInfo(any(), any())).thenReturn(Observable.error(Exception("")))

        viewModel.streamFrozenMusic(mock(), "123", MusicType.Song, MixpanelSource.empty, "", ActionToBeResumed.Play)

        verify(eventToggleHUDModeObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(eventToggleHUDModeObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(eventMaximizePlayerObserver, never()).onChanged(any())
    }

    @Test
    fun `email verification success`() {
        whenever(apiEmailVerification.runEmailVerification(any())).thenReturn(Completable.complete())
        viewModel.handleEmailVerification("xxx")
        verify(showEmailVerificationResultEventObserver).onChanged(true)
    }

    @Test
    fun `email verification failure`() {
        whenever(apiEmailVerification.runEmailVerification(any())).thenReturn(Completable.error(Throwable("Error")))
        viewModel.handleEmailVerification("xxx")
        verify(showEmailVerificationResultEventObserver).onChanged(false)
    }

    @Test
    fun `loader show event when interstitial is loading`() {
        val observer: Observer<Boolean> = mock()
        viewModel.showInterstitialLoaderEvent.observeForever(observer)
        interstitialObservable.onNext(Loading)
        verify(observer).onChanged(true)
    }

    @Test
    fun `loader hide event when interstitial is not loading`() {
        val observer: Observer<Boolean> = mock()
        viewModel.showInterstitialLoaderEvent.observeForever(observer)
        interstitialObservable.onNext(Dismissed)
        verify(observer).onChanged(false)
    }

    @Test
    fun `sleep timer triggered event on sleep timer trigger`() {
        val observer: Observer<Unit> = mock()
        viewModel.sleepTimerTriggeredEvent.observeForever(observer)
        sleepTimerEventObservable.onNext(TimerTriggered)
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun `app rating - observe need to show rating prompt`() {
        inAppRatingSubject.onNext(InAppRatingResult.ShowRatingPrompt)
        verify(showRatingPromptEventObserver).onChanged(null)
    }

    @Test
    fun `app rating - observe need to show declined rating prompt`() {
        inAppRatingSubject.onNext(InAppRatingResult.ShowDeclinedRatingPrompt)
        verify(showDeclinedRatingPromptEventObserver).onChanged(null)
    }

    @Test
    fun `app rating - observe need to open rating`() {
        inAppRatingSubject.onNext(InAppRatingResult.OpenRating)
        verify(openAppRatingEventObserver).onChanged(null)
    }

    @Test
    fun `app rating - observe need to open support`() {
        inAppRatingSubject.onNext(InAppRatingResult.OpenSupport)
        verify(deeplinkObserver).onChanged(argWhere { it is Deeplink.Support })
    }

    @Test
    fun `app rating - on rating prompt accepted`() {
        viewModel.onRatingPromptAccepted()
        verify(inAppRating).onRatingPromptAccepted()
    }

    @Test
    fun `app rating - on rating prompt declined`() {
        viewModel.onRatingPromptDeclined()
        verify(inAppRating).onRatingPromptDeclined()
    }

    @Test
    fun `app rating - on declined rating prompt accepted`() {
        viewModel.onDeclinedRatingPromptAccepted()
        verify(inAppRating).onDeclinedRatingPromptAccepted()
    }

    @Test
    fun `app rating - on declined rating prompt declined`() {
        viewModel.onDeclinedRatingPromptDeclined()
        verify(inAppRating).onDeclinedRatingPromptDeclined()
    }

    @Test
    fun `app rating - on app rating requested`() {
        viewModel.onAppRatingRequested(mock())
        verify(inAppRating).show(any())
    }

    @Test
    fun `play remote music - test all results`() {
        whenever(playMusicFromIdUseCase.loadAndPlay(any(), any(), any())).thenReturn(Observable.just(
            PlayMusicFromIdResult.Georestricted,
            PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Loading),
            PlayMusicFromIdResult.ReadyToPlay(MaximizePlayerData())
        ))
        viewModel.onPlayRemoteMusicRequested("", MusicType.Album, MixpanelSource.empty)
        verify(eventToggleHUDModeObserver).onChanged(any())
        verify(eventShowGeorestrictedAlertObserver).onChanged(null)
        verify(eventMaximizePlayerObserver).onChanged(any())
    }

    @Test
    fun `play later - test all results`() {
        whenever(addMusicToQueueUseCase.loadAndAdd(any(), any(), any(), eq(AddMusicToQueuePosition.Later))).thenReturn(Observable.just(
            AddMusicToQueueUseCaseResult.Georestricted,
            AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Loading),
            AddMusicToQueueUseCaseResult.Success
        ))
        viewModel.playLater("", MusicType.Album, MixpanelSource.empty)
        verify(eventToggleHUDModeObserver).onChanged(any())
        verify(eventShowGeorestrictedAlertObserver).onChanged(null)
        verify(showAddedToQueueToastEventObserver).onChanged(null)
    }

    @Test
    fun `verify forgot password token success`() {
        val token = "xxx"
        whenever(authenticationDataSource.verifyForgotPasswordToken(token)).thenReturn(Completable.complete())
        viewModel.handleResetPassword(token)
        verify(showPasswordResetScreenEventObserver).onChanged(token)
    }

    @Test
    fun `verify forgot password token failure`() {
        val token = "xxx"
        whenever(authenticationDataSource.verifyForgotPasswordToken(token)).thenReturn(Completable.error(Throwable("Error")))
        viewModel.handleResetPassword("xxx")
        verify(showPasswordResetErrorEventObserver).onChanged(null)
    }

    @Test
    fun `restore download rejected`() {
        whenever(housekeepingUseCase.clearDownloadsToRestore()).thenReturn(Completable.complete())

        val count = 100
        viewModel.onRestoreDownloadsRejected(count)
        verify(mixpanelDataSource).trackRestoreDownloads(RestoreDownloadsMode.Manually, count)
        verify(housekeepingUseCase).clearDownloadsToRestore()
    }

    @Test
    fun `restore download requested`() {
        val count = 100
        viewModel.onRestoreDownloadsRequested(count)
        verify(mixpanelDataSource).trackRestoreDownloads(RestoreDownloadsMode.All, count)
    }

    @Test
    fun `handles navigate back event`() {
        val observer = mock<Observer<Unit>>()
        viewModel.navigateBackEvent.observeForever(observer)
        navigationActions.navigateBack()
        verify(observer, times(1)).onChanged(null)
    }

    @Test
    fun `handles launch login event`() {
        val observer = mock<Observer<LoginSignupSource>>()
        viewModel.launchLoginEvent.observeForever(observer)
        navigationActions.launchLogin(Download)
        verify(observer, times(1)).onChanged(Download)
    }

    @Test
    fun `handles launch queue event`() {
        val observer = mock<Observer<Unit>>()
        viewModel.launchQueueEvent.observeForever(observer)
        navigationActions.launchQueue()
        verify(observer, times(1)).onChanged(null)
    }

    @Test
    fun `handles launch local media selection event`() {
        val observer = mock<Observer<Unit>>()
        viewModel.launchLocalFilesSelectionEvent.observeForever(observer)
        navigationActions.launchLocalFilesSelection()
        verify(observer, times(1)).onChanged(null)
    }

    @Test
    fun `handles launch app purchase event`() {
        val observer = mock<Observer<InAppPurchaseMode>>()
        viewModel.launchInAppPurchaseEvent.observeForever(observer)
        navigationActions.launchInAppPurchase(AudioAd)
        verify(observer, times(1)).onChanged(AudioAd)
    }

    @Test
    fun `handles generic error alert event`() {
        val observer = mock<Observer<Unit>>()
        viewModel.genericErrorEvent.observeForever(observer)
        alertTriggers.onGenericError()
        verify(observer, times(1)).onChanged(null)
    }

    @Test
    fun `handles item added to queue alert event`() {
        val observer = mock<Observer<Unit>>()
        viewModel.itemAddedToQueueEvent.observeForever(observer)
        alertTriggers.onAddedToQueue()
        verify(observer, times(1)).onChanged(null)
    }

    @Test
    fun `handles local file selection success alert event`() {
        val observer = mock<Observer<Unit>>()
        viewModel.localFilesSelectionSuccessEvent.observeForever(observer)
        alertTriggers.onLocalFilesSelectionSuccess()
        verify(observer, times(1)).onChanged(null)
    }

    @Test
    fun `handles storage permission denied alert event`() {
        val observer = mock<Observer<Unit>>()
        viewModel.storagePermissionDenied.observeForever(observer)
        alertTriggers.onStoragePermissionDenied()
        verify(observer, times(1)).onChanged(null)
    }

    @Test
    fun `show sleep timer prompt on init`() {
        verify(showSleepTimerPromptEventObserver).onChanged(showSleepTimerPromptUseCase.getPromptMode().blockingGet())
    }

    @Test
    fun `on premium download notification shown`() {
        val type = PremiumDownloadType.PremiumOnly
        viewModel.onPremiumDownloadNotificationShown(type)
        verify(mixpanelDataSource).trackPremiumDownloadNotification(type)
    }

    @Test
    fun `run facebook express login`() {
        val context = mock<Context>()
        whenever(facebookExpressLoginUseCase.run(context)).thenReturn(Completable.complete())
        viewModel.runFacebookExpressLogin(context)
        verify(facebookExpressLoginUseCase).run(context)
    }

    @Test
    fun `handles local file view intent in on create`() {
        val intent = mock<Intent> {
            on { action } doReturn Intent.ACTION_VIEW
            on { data } doReturn mock()
            on { type } doReturn "audio/mpeg"
        }
        viewModel.onCreate(intent)
        verify(openLocalMedia, times(1)).open(any(), any())
    }

    @Test
    fun `handles local file view intent in on new intent`() {
        val intent = mock<Intent> {
            on { action } doReturn Intent.ACTION_VIEW
            on { data } doReturn mock()
            on { type } doReturn "audio/mpeg"
        }
        viewModel.onIntentReceived(intent)
        verify(openLocalMedia, times(1)).open(any(), any())
        verify(deeplinkDataSource, never()).obtainDeeplink(anyOrNull())
    }

    @Test
    fun `doesn't attempt to open a Uri for non-audio mime types on create`() {
        viewModel.onCreate(mock())
        verify(openLocalMedia, never()).open(anyOrNull(), anyOrNull())
    }

    @Test
    fun `doesn't attempt to open a Uri for non-audio mime types on new intent`() {
        viewModel.onIntentReceived(mock())
        verify(openLocalMedia, never()).open(anyOrNull(), anyOrNull())
    }
}
