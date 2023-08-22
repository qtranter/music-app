package com.audiomack.ui.player.full

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.common.State
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.AddToPlaylistException
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleRepostException
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.ads.ShowInterstitialResult
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelButtonNowPlaying
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataInterface
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.playback.ActionState.ACTIVE
import com.audiomack.playback.ActionState.DEFAULT
import com.audiomack.playback.ActionState.LOADING
import com.audiomack.playback.NowPlayingVisibility
import com.audiomack.playback.Playback
import com.audiomack.playback.PlaybackState
import com.audiomack.playback.PlaybackState.IDLE
import com.audiomack.playback.PlayerError
import com.audiomack.playback.RepeatType
import com.audiomack.playback.SKIP_BACK_DURATION
import com.audiomack.playback.SKIP_FORWARD_DURATION
import com.audiomack.playback.SongAction
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.common.Resource
import com.audiomack.ui.tooltip.TooltipFragment.TooltipLocation
import com.audiomack.utils.Foreground
import com.audiomack.utils.GeneralPreferences
import com.mopub.mobileads.MoPubView
import com.mopub.nativeads.AdapterHelper
import com.mopub.nativeads.NativeAd
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class PlayerViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    lateinit var playback: Playback
    @Mock
    lateinit var generalPreferences: GeneralPreferences
    @Mock
    lateinit var foreground: Foreground
    @Mock
    lateinit var playerDataSource: PlayerDataSource
    @Mock
    lateinit var queue: QueueDataSource
    @Mock
    lateinit var premiumDataSource: PremiumDataSource
    @Mock
    lateinit var adsDataSource: AdsDataSource
    @Mock
    lateinit var eventBus: EventBus
    @Mock
    lateinit var actionsDataSource: ActionsDataSource
    @Mock
    lateinit var userData: UserDataInterface
    @Mock
    lateinit var userDataSource: UserDataSource
    @Mock
    lateinit var nowPlayingVisibility: NowPlayingVisibility
    @Mock
    lateinit var trackingDataSource: TrackingDataSource
    @Mock
    lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    lateinit var premiumDownloadDataSource: PremiumDownloadDataSource

    private val screenTitleObserver: Observer<String> = mock()
    private val premiumStatusObserver: Observer<Boolean> = mock()
    private val favoriteActionObserver: Observer<SongAction> = mock()
    private val addToPlaylistActionObserver: Observer<SongAction> = mock()
    private val rePostActionObserver: Observer<SongAction> = mock()
    private val downloadActionObserver: Observer<SongAction> = mock()
    private val songQueueObserver: Observer<List<AMResultItem>> = mock()
    private val songQueueIndexObserver: Observer<Int> = mock()
    private val playbackObserver: Observer<PlaybackState> = mock()
    private val currentPositionObserver: Observer<Long> = mock()
    private val durationObserver: Observer<Long> = mock()
    private val volumeDataObserver: Observer<IntArray> = mock()
    private val requestPlaylistTooltipEventObserver: Observer<Void> = mock()
    private val requstQueueTooltipEventObserver: Observer<Void> = mock()
    private val adClosedEventObserver: Observer<Boolean> = mock()
    private val showNativeAdObserver: Observer<Pair<NativeAd, AdapterHelper>> = mock()
    private val showAdObserver: Observer<MoPubView> = mock()
    private val searchArtistEventObserver: Observer<String> = mock()
    private val showArtworkEventObserver: Observer<String> = mock()
    private val showInAppPurchseEventObserver: Observer<Unit> = mock()
    private val minimizeEventObserver: Observer<Void> = mock()
    private val showQueueEventObserver: Observer<Void> = mock()
    private val removeAdsEventObserver: Observer<Void> = mock()
    private val showPlaylistTooltipEventObserver: Observer<TooltipLocation> = mock()
    private val showQueueTooltipEventObserver: Observer<TooltipLocation> = mock()
    private val adRefreshEventObserver: Observer<Boolean> = mock()
    private val errorEventObserver: Observer<PlayerError> = mock()
    private val openAlbumObserver: Observer<Pair<String, MixpanelSource?>> = mock()
    private val openPlaylistObserver: Observer<Pair<String, MixpanelSource?>> = mock()
    private val nexButtonObserver: Observer<Boolean> = mock()
    private val downloadClickObserver: Observer<AMResultItem> = mock()
    private val retryDownloadObserver: Observer<AMResultItem> = mock()
    private val loginRequiredObserver: Observer<LoginSignupSource> = mock()
    private val notifyOfflineObserver: Observer<Void> = mock()
    private val addToPlaylistObserver: Observer<Triple<List<AMResultItem>, MixpanelSource, String>> = mock()
    private val notifyRepostObserver: Observer<ToggleRepostResult.Notify> = mock()
    private val notifyFavoriteObserver: Observer<ToggleFavoriteResult.Notify> = mock()
    private val playEventObserver: Observer<Void> = mock()
    private val repeatTypeObserver: Observer<RepeatType> = mock()
    private val shareEventObserver: Observer<AMResultItem> = mock()
    private val showConfirmDownloadDeletionEventObserver: Observer<AMResultItem> = mock()
    private val showPremiumDownloadEventObserver: Observer<PremiumDownloadModel> = mock()
    private val showUnlockedToastEventObserver: Observer<String> = mock()

    lateinit var viewModel: PlayerViewModel

    private lateinit var premiumObservable: Subject<Boolean>
    private lateinit var interstitialObservable: Subject<ShowInterstitialResult>

    private lateinit var mockItem: AMResultItem

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        whenever(playback.state).thenReturn(State(IDLE))
        whenever(playback.timer).thenReturn(PublishSubject.create())
        whenever(playback.error).thenReturn(BehaviorSubject.create())
        whenever(playback.adTimer).thenReturn(PublishSubject.create())
        whenever(playback.downloadRequest).thenReturn(PublishSubject.create())
        whenever(playback.repeatType).thenReturn(BehaviorSubject.create())

        interstitialObservable = PublishSubject.create()
        whenever(adsDataSource.interstitialObservable).thenReturn(interstitialObservable)

        premiumObservable = PublishSubject.create()
        whenever(premiumDataSource.premiumObservable).thenReturn(premiumObservable)

        loginStateChangeSubject = BehaviorSubject.create()
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)

        viewModel = PlayerViewModel(
            playback,
            generalPreferences,
            foreground,
            playerDataSource,
            queue,
            premiumDataSource,
            adsDataSource,
            eventBus,
            TestSchedulersProvider(),
            actionsDataSource,
            userData,
            userDataSource,
            nowPlayingVisibility,
            MixpanelButtonNowPlaying,
            trackingDataSource,
            premiumDownloadDataSource
        ).apply {
            parentTitle.observeForever(screenTitleObserver)
            isHiFi.observeForever(premiumStatusObserver)
            favoriteAction.observeForever(favoriteActionObserver)
            addToPlaylistAction.observeForever(addToPlaylistActionObserver)
            rePostAction.observeForever(rePostActionObserver)
            downloadAction.observeForever(downloadActionObserver)
            songList.observeForever(songQueueObserver)
            currentIndex.observeForever(songQueueIndexObserver)
            playbackState.observeForever(playbackObserver)
            currentPosition.observeForever(currentPositionObserver)
            duration.observeForever(durationObserver)
            volumeData.observeForever(volumeDataObserver)
            requestPlaylistTooltipEvent.observeForever(requestPlaylistTooltipEventObserver)
            requestQueueTooltipEvent.observeForever(requstQueueTooltipEventObserver)
            adClosedEvent.observeForever(adClosedEventObserver)
            showAdEvent.observeForever(showAdObserver)
            showNativeAdEvent.observeForever(showNativeAdObserver)
            removeAdsEvent.observeForever(removeAdsEventObserver)
            adRefreshEvent.observeForever(adRefreshEventObserver)
            showPlaylistTooltipEvent.observeForever(showPlaylistTooltipEventObserver)
            showQueueTooltipEvent.observeForever(showQueueTooltipEventObserver)
            errorEvent.observeForever(errorEventObserver)
            searchArtistEvent.observeForever(searchArtistEventObserver)
            showArtworkEvent.observeForever(showArtworkEventObserver)
            showInAppPurchaseEvent.observeForever(showInAppPurchseEventObserver)
            minimizeEvent.observeForever(minimizeEventObserver)
            showQueueEvent.observeForever(showQueueEventObserver)
            openParentAlbumEvent.observeForever(openAlbumObserver)
            openParentPlaylistEvent.observeForever(openPlaylistObserver)
            nextButtonEnabled.observeForever(nexButtonObserver)
            downloadClickEvent.observeForever(downloadClickObserver)
            retryDownloadEvent.observeForever(retryDownloadObserver)
            loginRequiredEvent.observeForever(loginRequiredObserver)
            notifyOfflineEvent.observeForever(notifyOfflineObserver)
            addToPlaylistEvent.observeForever(addToPlaylistObserver)
            notifyRepostEvent.observeForever(notifyRepostObserver)
            notifyFavoriteEvent.observeForever(notifyFavoriteObserver)
            playEvent.observeForever(playEventObserver)
            repeat.observeForever(repeatTypeObserver)
            shareEvent.observeForever(shareEventObserver)
            showConfirmDownloadDeletionEvent.observeForever(showConfirmDownloadDeletionEventObserver)
            showPremiumDownloadEvent.observeForever(showPremiumDownloadEventObserver)
            showUnlockedToastEvent.observeForever(showUnlockedToastEventObserver)
        }

        mockItem = mock {
            on { favoritesShort } doReturn "0"
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "0"
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `song list observed`() {
        val songList = mock<List<AMResultItem>>()
        viewModel.queueListObserver.onNext(songList)
        verify(songQueueObserver, times(1)).onChanged(songList)
    }

    @Test
    fun `playback state change observed`() {
        viewModel.playbackStateObserver.onNext(PlaybackState.LOADING)
        verify(playbackObserver, times(1)).onChanged(PlaybackState.LOADING)
    }

    @Test
    fun `duration set when playback state changes`() {
        whenever(playback.duration).thenReturn(300L)

        viewModel.playbackStateObserver.onNext(PlaybackState.PLAYING)
        verify(durationObserver, times(1)).onChanged(300L)
    }

    @Test
    fun `playback position change observed`() {
        viewModel.playbackTimerObserver.onNext(0L)
        verify(currentPositionObserver, times(1)).onChanged(0L)
    }

    @Test
    fun `playback error change observed`() {
        val exception = PlayerError.Playback(Exception())

        viewModel.playbackErrorObserver.onNext(exception)
        verify(errorEventObserver, times(1)).onChanged(exception)
    }

    @Test
    fun `ad refreshed when ad timer changes`() {
        val item: AMResultItem = mock()
        whenever(queue.currentItem).thenReturn(item)
        whenever(adsDataSource.adsVisible).thenReturn(true)
        whenever(foreground.isForeground).thenReturn(true)
        whenever(nowPlayingVisibility.isMaximized).thenReturn(true)

        viewModel.adTimerObserver.onNext(0L)
        verify(adRefreshEventObserver, times(1)).onChanged(true)
    }

    @Test
    fun `ad refreshed when ad timer changes and song was skipped manually`() {
        val item: AMResultItem = mock()
        whenever(queue.currentItem).thenReturn(item)
        whenever(adsDataSource.adsVisible).thenReturn(true)
        whenever(foreground.isForeground).thenReturn(true)
        whenever(nowPlayingVisibility.isMaximized).thenReturn(true)
        whenever(playback.songSkippedManually).thenReturn(true)

        viewModel.adTimerObserver.onNext(0L)
        verify(adRefreshEventObserver, times(0)).onChanged(true)
        verify(adsDataSource).showInterstitial()
    }

    @Test
    fun `restart playback after interstitial is dismissed`() {
        interstitialObservable.onNext(ShowInterstitialResult.Dismissed)
        verify(playback).play()
    }

    @Test
    fun `ad refreshed when interstitial is not shown`() {
        val item: AMResultItem = mock()
        whenever(queue.currentItem).thenReturn(item)
        whenever(adsDataSource.adsVisible).thenReturn(true)
        whenever(foreground.isForeground).thenReturn(true)
        whenever(nowPlayingVisibility.isMaximized).thenReturn(true)

        interstitialObservable.onNext(ShowInterstitialResult.NotShown(""))
        verify(adRefreshEventObserver, times(1)).onChanged(true)
    }

    @Test
    fun `queue index change observed`() {
        viewModel.queueIndexObserver.onNext(2)
        verify(songQueueIndexObserver, times(1)).onChanged(2)
    }

    @Test
    fun `playback position reset when queue index changes`() {
        viewModel.queueIndexObserver.onNext(1)
        verify(currentPositionObserver, times(1)).onChanged(0)
    }

    @Test
    fun `screen title set when album track changes`() {
        val title = "Album Title"
        val item = mock<AMResultItem> {
            on { album } doReturn title
            on { favoritesShort } doReturn "0"
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "0"
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }

        viewModel.queueCurrentItemObserver.onNext(item)
        verify(screenTitleObserver, times(1)).onChanged(title)
    }

    @Test
    fun `screen title set when playlist track changes`() {
        val title = "Playlist Title"
        val item = mock<AMResultItem> {
            on { playlist } doReturn title
            on { favoritesShort } doReturn "0"
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "0"
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }

        viewModel.queueCurrentItemObserver.onNext(item)
        verify(screenTitleObserver, times(1)).onChanged(title)
    }

    @Test
    fun `volume data set when track changes`() {
        val data = intArrayOf(1, 2, 3)
        val item = mock<AMResultItem> {
            on { volumeData } doReturn data
            on { favoritesShort } doReturn "0"
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "0"
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }

        viewModel.queueCurrentItemObserver.onNext(item)
        verify(volumeDataObserver, times(1)).onChanged(data)
    }

    @Test
    fun `favorite stats shown when favorited`() {
        val text = "2k"
        val item = mock<AMResultItem> {
            on { favoritesShort } doReturn text
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "0"
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }
        whenever(userData.isItemFavorited(item)).thenReturn(true)

        val resource = Resource.Success(item)
        viewModel.dataCurrentFullItemObserver.onNext(resource)

        ArgumentCaptor.forClass(SongAction.Favorite::class.java).run {
            verify(favoriteActionObserver, times(1)).onChanged(capture())
            assertTrue(value is SongAction.Favorite)
            assertEquals(ACTIVE, value.state)
        }
    }

    @Test
    fun `favorite stats shown when not favorited`() {
        val text = "2k"
        val item = mock<AMResultItem> {
            on { isFavorited } doReturn false
            on { favoritesShort } doReturn text
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "0"
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }
        val resource = Resource.Success(item)
        viewModel.dataCurrentFullItemObserver.onNext(resource)

        ArgumentCaptor.forClass(SongAction.Favorite::class.java).run {
            verify(favoriteActionObserver, times(1)).onChanged(capture())
            assertTrue(value is SongAction.Favorite)
            assertEquals(DEFAULT, value.state)
        }
    }

    @Test
    fun `playlist stats shown`() {
        val text = "1k"
        val item = mock<AMResultItem> {
            on { playlistsShort } doReturn text
            on { favoritesShort } doReturn "0"
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "0"
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }
        val resource = Resource.Success(item)
        viewModel.dataCurrentFullItemObserver.onNext(resource)

        ArgumentCaptor.forClass(SongAction.AddToPlaylist::class.java).run {
            verify(addToPlaylistActionObserver, times(1)).onChanged(capture())
            assertTrue(value is SongAction.AddToPlaylist)
            assertEquals(DEFAULT, value.state)
        }
    }

    @Test
    fun `repost stats shown when have not reposted`() {
        val text = "500"
        val item = mock<AMResultItem> {
            on { isReposted } doReturn false
            on { repostsShort } doReturn text
            on { favoritesShort } doReturn "0"
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "500"
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }
        val resource = Resource.Success(item)
        viewModel.dataCurrentFullItemObserver.onNext(resource)

        ArgumentCaptor.forClass(SongAction.RePost::class.java).run {
            verify(rePostActionObserver, times(1)).onChanged(capture())
            assertTrue(value is SongAction.RePost)
            assertEquals(DEFAULT, value.state)
        }
    }

    @Test
    fun `repost stats shown when has reposted`() {
        val text = "500"
        val item = mock<AMResultItem> {
            on { isReposted } doReturn true
            on { repostsShort } doReturn text
            on { favoritesShort } doReturn "0"
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }
        val resource = Resource.Success(item)
        viewModel.dataCurrentFullItemObserver.onNext(resource)

        ArgumentCaptor.forClass(SongAction.RePost::class.java).run {
            verify(rePostActionObserver, times(1)).onChanged(capture())
            assertTrue(value is SongAction.RePost)
        }
    }

    @Test
    fun `download state shown when song is not downloaded or downloading`() {
        val item = mock<AMResultItem> {
            on { isDownloaded } doReturn false
            on { isDownloadInProgress } doReturn false
            on { favoritesShort } doReturn "0"
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "0"
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }
        val resource = Resource.Success(item)
        viewModel.dataCurrentFullItemObserver.onNext(resource)

        ArgumentCaptor.forClass(SongAction.Download::class.java).run {
            verify(downloadActionObserver, times(1)).onChanged(capture())
            assertTrue(value is SongAction.Download)
            assertEquals(DEFAULT, value.state)
        }
    }

    @Test
    fun `download state shown when song is downloaded`() {
        val item = mock<AMResultItem> {
            on { isDownloadCompletedIndependentlyFromType } doReturn true
            on { isDownloadInProgress } doReturn false
            on { favoritesShort } doReturn "0"
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "0"
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }
        val resource = Resource.Success(item)
        viewModel.dataCurrentFullItemObserver.onNext(resource)

        ArgumentCaptor.forClass(SongAction.Download::class.java).run {
            verify(downloadActionObserver, times(1)).onChanged(capture())
            assertTrue(value is SongAction.Download)
            assertEquals(ACTIVE, value.state)
        }
    }

    @Test
    fun `download state shown when song is downloading`() {
        val item = mock<AMResultItem> {
            on { isDownloaded } doReturn false
            on { isDownloadInProgress } doReturn true
            on { favoritesShort } doReturn "0"
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "0"
            on { repostSubject } doReturn mock()
        }
        val resource = Resource.Success(item)
        viewModel.dataCurrentFullItemObserver.onNext(resource)

        ArgumentCaptor.forClass(SongAction.Download::class.java).run {
            verify(downloadActionObserver, times(1)).onChanged(capture())
            assertTrue(value is SongAction.Download)
            assertEquals(LOADING, value.state)
        }
    }

    @Test
    fun `ad refreshed when view came into foreground`() {
        val item: AMResultItem = mock()
        whenever(queue.currentItem).thenReturn(item)
        whenever(adsDataSource.adsVisible).thenReturn(true)
        whenever(foreground.isForeground).thenReturn(true)
        whenever(nowPlayingVisibility.isMaximized).thenReturn(true)

        viewModel.foregroundListener.onBecameForeground()
        verify(adRefreshEventObserver, times(1)).onChanged(true)
    }

    @Test
    fun `ad not refreshed when not fullscreen`() {
        whenever(queue.currentItem).thenReturn(mock())
        whenever(adsDataSource.adsVisible).thenReturn(true)
        whenever(foreground.isForeground).thenReturn(false)
        whenever(nowPlayingVisibility.isMaximized).thenReturn(true)

        viewModel.refreshPlayerAd(true)
        verify(adRefreshEventObserver, never()).onChanged(any())
    }

    @Test
    fun `ad not refreshed when not ad view is not visible`() {
        whenever(queue.currentItem).thenReturn(mock())
        whenever(adsDataSource.adsVisible).thenReturn(false)
        whenever(foreground.isForeground).thenReturn(true)
        whenever(nowPlayingVisibility.isMaximized).thenReturn(true)

        viewModel.refreshPlayerAd(true)
        verify(adRefreshEventObserver, never()).onChanged(any())
    }

    @Test
    fun `ad not refreshed when not maximized`() {
        whenever(queue.currentItem).thenReturn(mock())
        whenever(adsDataSource.adsVisible).thenReturn(true)
        whenever(foreground.isForeground).thenReturn(true)
        whenever(nowPlayingVisibility.isMaximized).thenReturn(false)

        viewModel.refreshPlayerAd(true)
        verify(adRefreshEventObserver, never()).onChanged(any())
    }

    @Test
    fun `visible ad closed when view went into background`() {
        whenever(queue.currentItem).thenReturn(mock())
        whenever(adsDataSource.adsVisible).thenReturn(true)
        whenever(foreground.isForeground).thenReturn(true)

        viewModel.foregroundListener.onBecameBackground()
        verify(adClosedEventObserver, times(1)).onChanged(true)
    }

    @Test
    fun `playback started when play button tapped`() {
        viewModel.playbackStateObserver.onNext(PlaybackState.PAUSED)

        viewModel.onPlayPauseClick()
        verify(playEventObserver, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `playback paused when pause button tapped`() {
        viewModel.playbackStateObserver.onNext(PlaybackState.PLAYING)

        viewModel.onPlayPauseClick()
        verify(playback, times(1)).pause()
    }

    @Test
    fun `interstitial ad requested when play button is tapped`() {
        whenever(foreground.isForeground).thenReturn(true)
        viewModel.playbackStateObserver.onNext(PlaybackState.PAUSED)

        viewModel.onPlayPauseClick()
        verify(adsDataSource, times(1)).showInterstitial()
    }

    @Test
    fun `ad refreshed when play button is tapped`() {
        whenever(queue.currentItem).thenReturn(mock())
        whenever(adsDataSource.adsVisible).thenReturn(true)
        whenever(foreground.isForeground).thenReturn(true)
        whenever(nowPlayingVisibility.isMaximized).thenReturn(true)

        viewModel.onPlayPauseClick()
        verify(adRefreshEventObserver, times(1)).onChanged(true)
    }

    @Test
    fun `playback seeks to touch seek event position`() {
        viewModel.onTouchSeek(63)
        verify(playback, times(1)).seekTo(63)
    }

    @Test
    fun `playback position change observed on touch seek event`() {
        viewModel.onTouchSeek(63)
        verify(currentPositionObserver, times(1)).onChanged(63)
    }

    @Test
    fun `playback skip when new track selected`() {
        viewModel.queueIndexObserver.onNext(2)

        viewModel.onTrackSelected(3)
        verify(playback, times(1)).skip(3)
    }

    @Test
    fun `playback doesn't skip when same track selected`() {
        viewModel.queueIndexObserver.onNext(2)

        viewModel.onTrackSelected(2)
        verify(playback, never()).skip(2)
    }

    @Test
    fun `interstitial ad not requested anymore when new track selected`() {
        whenever(foreground.isForeground).thenReturn(true)
        viewModel.playbackStateObserver.onNext(PlaybackState.PAUSED)
        viewModel.queueIndexObserver.onNext(2)

        viewModel.queueCurrentItemObserver.onNext(mockItem)
        verify(adsDataSource, times(0)).showInterstitial()
    }

    @Test
    fun `previous track requested when prev button tapped`() {
        whenever(queue.currentItem).thenReturn(mockItem)
        viewModel.onSkipBackClick()
        verify(playback, times(1)).prev()
    }

    @Test
    fun `next track requested when next button tapped`() {
        whenever(queue.currentItem).thenReturn(mockItem)
        viewModel.onSkipForwardClick()
        verify(playback, times(1)).next()
    }

    @Test
    fun `playback fast forwards when skip forward button tapped`() {
        whenever(playback.duration).thenReturn(MILLISECONDS.convert(3L, MINUTES))
        val position = MILLISECONDS.convert(1L, MINUTES)
        whenever(playback.position).thenReturn(position)

        val item = mock<AMResultItem> {
            on { favoriteSubject } doReturn mock()
            on { repostSubject } doReturn mock()
            on { isPodcast } doReturn true
        }
        whenever(queue.currentItem).thenReturn(item)

        viewModel.onSkipForwardClick()

        verify(playback, times(1)).seekTo(position + SKIP_FORWARD_DURATION)
    }

    @Test
    fun `playback rewinds when skip back button tapped`() {
        whenever(playback.duration).thenReturn(MILLISECONDS.convert(3L, MINUTES))
        val position = MILLISECONDS.convert(1L, MINUTES)
        whenever(playback.position).thenReturn(position)

        val item = mock<AMResultItem> {
            on { favoriteSubject } doReturn mock()
            on { repostSubject } doReturn mock()
            on { isPodcast } doReturn true
        }
        whenever(queue.currentItem).thenReturn(item)

        viewModel.onSkipBackClick()

        verify(playback, times(1)).seekTo(position - SKIP_BACK_DURATION)
    }

    @Test
    fun `playback seeks to 0 when skip back button tapped and position is less than 15 sec`() {
        whenever(playback.duration).thenReturn(MILLISECONDS.convert(3L, MINUTES))
        val position = MILLISECONDS.convert(14L, SECONDS)
        whenever(playback.position).thenReturn(position)

        val item = mock<AMResultItem> {
            on { favoriteSubject } doReturn mock()
            on { repostSubject } doReturn mock()
            on { isPodcast } doReturn true
        }
        whenever(queue.currentItem).thenReturn(item)

        viewModel.onSkipBackClick()

        verify(playback, times(1)).seekTo(0)
    }

    @Test
    fun `skips to next track when skip forward button tapped and playback position is less than 30 sec away from end`() {
        whenever(playback.duration).thenReturn(MILLISECONDS.convert(30L, SECONDS))
        val position = MILLISECONDS.convert(10L, SECONDS)
        whenever(playback.position).thenReturn(position)

        val item = mock<AMResultItem> {
            on { favoriteSubject } doReturn mock()
            on { repostSubject } doReturn mock()
            on { isPodcast } doReturn true
        }
        whenever(queue.currentItem).thenReturn(item)

        viewModel.onSkipForwardClick()

        verify(playback, times(1)).next()
    }

    @Test
    fun `playback seeks to 0 when track restart requested`() {
        viewModel.restart()
        verify(playback, times(1)).seekTo(0)
    }

    @Test
    fun `minimize event observed`() {
        viewModel.onMinimizeClick()
        verify(minimizeEventObserver, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `show queue event observed`() {
        viewModel.onQueueClick()
        verify(showQueueEventObserver, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `show in app purchase event fired when hifi tapped and user is not premium`() {
        whenever(premiumDataSource.isPremium).thenReturn(false)

        viewModel.onHiFiClick()
        verify(showInAppPurchseEventObserver, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `don't show in app purchase event fired when hifi tapped and user is premium`() {
        whenever(queue.currentItem).thenReturn(mockItem)
        premiumObservable.onNext(true)

        viewModel.onHiFiClick()
        verify(showInAppPurchseEventObserver, never()).onChanged(any())
    }

    @Test
    fun `hifi button disabled when playing local files`() {
        whenever(premiumDataSource.isPremium).thenReturn(true)
        val item = mock<AMResultItem> {
            on { isLocal } doReturn true
        }
        val resource = Resource.Success(item)

        val observer = mock<Observer<Boolean>>()
        viewModel.isHiFi.observeForever(observer)

        viewModel.dataCurrentFullItemObserver.onNext(resource)

        verify(observer, times(1)).onChanged(false)
    }

    @Test
    fun `premium status and download action observed when changed`() {
        val song = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Premium
        }
        whenever(playerDataSource.currentSong).thenReturn(song)
        whenever(queue.currentItem).thenReturn(song)

        premiumObservable.onNext(true)

        verify(premiumStatusObserver, times(1)).onChanged(any())
        verify(downloadActionObserver).onChanged(any())
    }

    @Test
    fun `artist click event observed`() {
        val artist = "Artist"
        viewModel.onArtistClick(artist)
        verify(searchArtistEventObserver, times(1)).onChanged(artist)
    }

    @Test
    fun `artwork click event observed`() {
        val url = "https://bla"
        viewModel.onArtworkClick(url)
        verify(showArtworkEventObserver, times(1)).onChanged(url)
    }

    @Test
    fun `playlist tooltip shown when necessary`() {
        whenever(generalPreferences.needToShowPlayerPlaylistTooltip()).thenReturn(true)

        viewModel.showTooltip()
        verify(requestPlaylistTooltipEventObserver, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `playlist tooltip not shown when unnecessary`() {
        whenever(generalPreferences.needToShowPlayerPlaylistTooltip()).thenReturn(false)

        viewModel.showTooltip()
        verify(requestPlaylistTooltipEventObserver, never()).onChanged(anyOrNull())
    }

    @Test
    fun `playlist tooltip location emitted`() {
        val location = mock<TooltipLocation>()
        viewModel.setPlaylistTooltipLocation(location)
        verify(showPlaylistTooltipEventObserver, times(1)).onChanged(location)
        assertTrue(viewModel.skipNextPlayerAd)
    }

    @Test
    fun `queue tooltip shown when necessary`() {
        whenever(generalPreferences.needToShowPlayerQueueTooltip()).thenReturn(true)

        viewModel.showTooltip()
        verify(requstQueueTooltipEventObserver, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `queue tooltip not shown when unnecessary`() {
        whenever(generalPreferences.needToShowPlayerQueueTooltip()).thenReturn(false)

        viewModel.showTooltip()
        verify(requstQueueTooltipEventObserver, never()).onChanged(anyOrNull())
    }

    @Test
    fun `queue tooltip location emitted`() {
        val location = mock<TooltipLocation>()
        viewModel.setQueueTooltipLocation(location)
        verify(showQueueTooltipEventObserver, times(1)).onChanged(location)
        assertTrue(viewModel.skipNextPlayerAd)
    }

    @Test
    fun `ad event observed`() {
        viewModel.showAd(mock())
        verify(showAdObserver, times(1)).onChanged(any())
    }

    @Test
    fun `native ad event observed`() {
        val nativeAd = mock<NativeAd>()
        val adapterHelper = mock<AdapterHelper>()
        viewModel.showAd(nativeAd, adapterHelper)
        verify(showNativeAdObserver, times(1)).onChanged(Pair(nativeAd, adapterHelper))
        verify(adsDataSource, times(1)).preloadNativeAd()
    }

    @Test
    fun `remove ads event when remove ads tapped`() {
        viewModel.onRemoveAdsClick()
        verify(removeAdsEventObserver, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `ad closed on close ad tapped`() {
        whenever(foreground.isForeground).thenReturn(true)

        viewModel.onCloseAdClick()
        verify(adClosedEventObserver, times(1)).onChanged(true)
    }

    @Test
    fun `mopub ads reset on ad closed`() {
        whenever(foreground.isForeground).thenReturn(true)

        viewModel.onCloseAdClick()
        verify(adClosedEventObserver, times(1)).onChanged(true)
        verify(adsDataSource, times(1)).resetMopub300x250Ad()
    }

    @Test
    fun `parent playlist click`() {
        val parentIdentifier = "456"
        val item = mock<AMResultItem> {
            on { parentId } doReturn parentIdentifier
        }
        whenever(queue.currentItem).thenReturn(item)

        viewModel.onParentClick()
        verify(openPlaylistObserver).onChanged(argWhere { it.first == parentIdentifier })
        verifyZeroInteractions(openAlbumObserver)
    }

    @Test
    fun `parent album click`() {
        val parentIdentifier = "456"
        val item = mock<AMResultItem> {
            on { parentId } doReturn parentIdentifier
            on { isAlbumTrack } doReturn true
        }
        whenever(queue.currentItem).thenReturn(item)

        viewModel.onParentClick()
        verify(openAlbumObserver).onChanged(argWhere { it.first == parentIdentifier })
        verifyZeroInteractions(openPlaylistObserver)
    }

    @Test
    fun `parent click local`() {
        val parentIdentifier = "456"
        val item = mock<AMResultItem> {
            on { parentId } doReturn parentIdentifier
            on { isAlbumTrack } doReturn true
            on { isLocal } doReturn true
        }
        whenever(queue.currentItem).thenReturn(item)

        viewModel.onParentClick()
        verifyZeroInteractions(openAlbumObserver)
        verifyZeroInteractions(openPlaylistObserver)
    }

    @Test
    fun `parent click, empty id`() {
        val parentIdentifier = ""
        val item = mock<AMResultItem> {
            on { parentId } doReturn parentIdentifier
            on { favoritesShort } doReturn "0"
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn mock()
            on { repostsShort } doReturn "0"
            on { repostSubject } doReturn mock()
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }
        val resource = Resource.Success(item)
        viewModel.dataCurrentFullItemObserver.onNext(resource)

        viewModel.onParentClick()
        verifyZeroInteractions(openAlbumObserver)
        verifyZeroInteractions(openPlaylistObserver)
    }

    @Test
    fun `next button enabled when not at end of queue`() {
        whenever(queue.atEndOfQueue).thenReturn(false)
        viewModel.queueIndexObserver.onNext(2)
        verify(nexButtonObserver, times(1)).onChanged(true)
    }

    @Test
    fun `next button disabled when at end of queue`() {
        whenever(queue.atEndOfQueue).thenReturn(true)
        viewModel.queueIndexObserver.onNext(2)
        verify(nexButtonObserver, times(1)).onChanged(false)
    }

    @Test
    fun `next button enabled when repeat all and at end of queue`() {
        whenever(queue.atEndOfQueue).thenReturn(true)

        viewModel.repeatObserver.onNext(RepeatType.ALL)

        verify(nexButtonObserver).onChanged(true)
    }

    @Test
    fun `next button disabled when not repeat all and at end of queue`() {
        whenever(queue.atEndOfQueue).thenReturn(true)
        viewModel.repeatObserver.onNext(RepeatType.OFF)
        verify(nexButtonObserver).onChanged(false)
    }

    @Test
    fun `download request observed`() {
        val item = mock<AMResultItem>()
        viewModel.downloadRequestObserver.onNext(item)
        verify(retryDownloadObserver, times(1)).onChanged(item)
    }

    // Add to playlist

    @Test
    fun `add to playlist - success`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.addToPlaylist(any())).thenReturn(Observable.just(true))
        viewModel.onAddToPlaylistClick()
        verify(addToPlaylistObserver).onChanged(any())
    }

    @Test
    fun `add to playlist - need to login - perform login`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.addToPlaylist(any())).thenReturn(Observable.error(AddToPlaylistException.LoggedOut))
        viewModel.onAddToPlaylistClick()
        verify(loginRequiredObserver).onChanged(eq(LoginSignupSource.AddToPlaylist))

        `when`(actionsDataSource.addToPlaylist(any())).thenReturn(Observable.just(true))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)

        verify(addToPlaylistObserver).onChanged(any())
    }

    @Test
    fun `add to playlist - need to login - do not login`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.addToPlaylist(any())).thenReturn(Observable.error(AddToPlaylistException.LoggedOut))
        viewModel.onAddToPlaylistClick()
        verify(loginRequiredObserver).onChanged(eq(LoginSignupSource.AddToPlaylist))

        loginStateChangeSubject.onNext(EventLoginState.CANCELED_LOGIN)

        verifyZeroInteractions(addToPlaylistObserver)
    }

    @Test
    fun `add to playlist - unknown error`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.addToPlaylist(any())).thenReturn(Observable.error(Exception("Unknown error")))
        viewModel.onAddToPlaylistClick()
        verifyZeroInteractions(loginRequiredObserver)
        verifyZeroInteractions(addToPlaylistObserver)
        verify(errorEventObserver).onChanged(any())
    }

    // Repost

    @Test
    fun `repost - success`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.toggleRepost(any(), any(), any())).thenReturn(Observable.create {
            it.onNext(ToggleRepostResult.Notify(true, true, "title", "artist"))
            it.onComplete()
        })
        viewModel.onRePostClick()

        verify(notifyRepostObserver).onChanged(any())
        verifyZeroInteractions(loginRequiredObserver)
    }

    @Test
    fun `repost - need to login - perform login`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.toggleRepost(any(), any(), any())).thenReturn(Observable.error(ToggleRepostException.LoggedOut))
        viewModel.onRePostClick()
        verify(loginRequiredObserver).onChanged(eq(LoginSignupSource.Repost))

        `when`(actionsDataSource.toggleRepost(any(), any(), any())).thenReturn(Observable.just(ToggleRepostResult.Notify(true, true, "title", "artist")))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)

        verify(notifyRepostObserver).onChanged(any())
    }

    @Test
    fun `repost - need to login - do not login`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.toggleRepost(any(), any(), any())).thenReturn(Observable.error(ToggleRepostException.LoggedOut))
        viewModel.onRePostClick()
        verify(loginRequiredObserver).onChanged(eq(LoginSignupSource.Repost))

        loginStateChangeSubject.onNext(EventLoginState.CANCELED_LOGIN)

        verifyZeroInteractions(notifyRepostObserver)
    }

    @Test
    fun `repost - offline`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.toggleRepost(any(), any(), any())).thenReturn(Observable.error(ToggleRepostException.Offline))
        viewModel.onRePostClick()

        verify(notifyOfflineObserver).onChanged(null)
        verifyZeroInteractions(notifyRepostObserver)
        verifyZeroInteractions(loginRequiredObserver)
    }

    @Test
    fun `repost - unknown error`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.toggleRepost(any(), any(), any())).thenReturn(Observable.error(Exception("Unknown error")))
        viewModel.onRePostClick()

        verifyZeroInteractions(notifyOfflineObserver)
        verifyZeroInteractions(notifyRepostObserver)
        verifyZeroInteractions(loginRequiredObserver)
        verify(errorEventObserver).onChanged(any())
    }

    // Favorite

    @Test
    fun `favorite - success`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.toggleFavorite(any(), any(), any())).thenReturn(Observable.create {
            it.onNext(ToggleFavoriteResult.Notify(true, true, true, true, true, "title", "artist"))
            it.onComplete()
        })
        viewModel.onFavoriteClick()

        verify(notifyFavoriteObserver).onChanged(any())
        verifyZeroInteractions(loginRequiredObserver)
    }

    @Test
    fun `favorite - need to login - perform login`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.toggleFavorite(any(), any(), any())).thenReturn(Observable.error(ToggleFavoriteException.LoggedOut))
        viewModel.onFavoriteClick()
        verify(loginRequiredObserver).onChanged(eq(LoginSignupSource.Favorite))

        `when`(actionsDataSource.toggleFavorite(any(), any(), any())).thenReturn(Observable.just(ToggleFavoriteResult.Notify(true, true, true, true, true, "title", "artist")))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)

        verify(notifyFavoriteObserver).onChanged(any())
    }

    @Test
    fun `favorite - need to login - do not login`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.toggleFavorite(any(), any(), any())).thenReturn(Observable.error(ToggleFavoriteException.LoggedOut))
        viewModel.onFavoriteClick()
        verify(loginRequiredObserver).onChanged(eq(LoginSignupSource.Favorite))

        loginStateChangeSubject.onNext(EventLoginState.CANCELED_LOGIN)

        verifyZeroInteractions(notifyFavoriteObserver)
    }

    @Test
    fun `favorite - offline`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.toggleFavorite(any(), any(), any())).thenReturn(Observable.error(ToggleFavoriteException.Offline))
        viewModel.onFavoriteClick()

        verify(notifyOfflineObserver).onChanged(null)
        verifyZeroInteractions(notifyFavoriteObserver)
        verifyZeroInteractions(loginRequiredObserver)
    }

    @Test
    fun `favorite - unknown error`() {
        whenever(queue.currentItem).thenReturn(mockItem)

        `when`(actionsDataSource.toggleFavorite(any(), any(), any())).thenReturn(Observable.error(Exception("Unknown error")))
        viewModel.onFavoriteClick()

        verifyZeroInteractions(notifyOfflineObserver)
        verifyZeroInteractions(notifyFavoriteObserver)
        verifyZeroInteractions(loginRequiredObserver)
        verify(errorEventObserver).onChanged(any())
    }

    @Test
    fun `repeat type change observed`() {
        viewModel.repeatObserver.onNext(RepeatType.ALL)
        verify(repeatTypeObserver).onChanged(RepeatType.ALL)
    }

    @Test
    fun `on download click`() {
        whenever(queue.currentItem).thenReturn(mockItem)
        viewModel.onDownloadClick()
        verify(downloadClickObserver).onChanged(any())
    }

    @Test
    fun `on share click`() {
        whenever(queue.currentItem).thenReturn(mockItem)
        viewModel.onShareClick()
        verify(shareEventObserver).onChanged(any())
    }

    @Test
    fun `is favorited, item is set`() {
        whenever(queue.currentItem).thenReturn(mockItem)
        whenever(userData.isItemFavorited(any())).thenReturn(true)
        assertTrue(viewModel.isFavorited())
    }

    @Test
    fun `is favorited, item is not set`() {
        assertFalse(viewModel.isFavorited())
    }

    @Test
    fun `onMinimized - closes ads`() {
        viewModel.onMinimized()
        verify(adClosedEventObserver).onChanged(eq(foreground.isForeground))
    }

    // Download

    @Test
    fun `download, test unlock`() {
        val musicTitle = "title"
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.just(
            ToggleDownloadResult.ShowUnlockedToast(musicTitle)
        ))

        viewModel.startDownload(mock(), "", false)
        verify(showUnlockedToastEventObserver).onChanged(musicTitle)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, logged out`() {
        val source = LoginSignupSource.Download
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.LoggedOut(source)
        ))

        viewModel.startDownload(mock(), "", false)
        verify(loginRequiredObserver).onChanged(source)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, show confirm deletion`() {
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.just(
            ToggleDownloadResult.ConfirmMusicDeletion
        ))

        viewModel.startDownload(mock(), "", false)
        verify(showConfirmDownloadDeletionEventObserver).onChanged(any())

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, show premium downloads`() {
        val model = PremiumDownloadModel(mock(), mock(), mock())
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.ShowPremiumDownload(model)
        ))

        viewModel.startDownload(mock(), "", false)
        verify(showPremiumDownloadEventObserver).onChanged(model)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `cast button disabled for local files`() {
        val item = mock<AMResultItem> {
            on { isLocal } doReturn true
        }
        val resource = Resource.Success(item)

        val observer = mock<Observer<Boolean>>()
        viewModel.castEnabled.observeForever(observer)

        viewModel.dataCurrentFullItemObserver.onNext(resource)

        verify(observer, times(2)).onChanged(false) // default value is false
    }
}
