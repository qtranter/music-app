package com.audiomack.ui.onboarding.playlist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.music.MusicManager
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventLoginState
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.playback.PlaybackItem
import com.audiomack.playback.PlayerPlayback
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class PlaylistOnboardingViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val artistImage: String = ""
    private lateinit var playlist: AMResultItem
    private val mixpanelSource = MixpanelSource("", "")

    @Mock
    private lateinit var imageLoader: ImageLoader

    @Mock
    private lateinit var adsDataSource: AdsDataSource

    @Mock
    private lateinit var musicManager: MusicManager

    @Mock
    private lateinit var queueDataSource: QueueDataSource

    @Mock
    private lateinit var playback: PlayerPlayback

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var actionsDataSource: ActionsDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: PlaylistOnboardingViewModel

    // Observers

    @Mock
    private lateinit var notifyOfflineEventObserver: Observer<Void>

    @Mock
    private lateinit var loginRequiredEventObserver: Observer<LoginSignupSource>

    @Mock
    private lateinit var notifyFavoriteEventObserver: Observer<ToggleFavoriteResult.Notify>

    @Mock
    private lateinit var showHUDEventObserver: Observer<ProgressHUDMode>

    @Mock
    private lateinit var observerShowConfirmDownloadDeletionEvent: Observer<AMResultItem>

    @Mock
    private lateinit var observerPremiumRequiredEvent: Observer<InAppPurchaseMode>

    @Mock
    private lateinit var observerShowPremiumDownloadEvent: Observer<PremiumDownloadModel>

    @Mock
    private lateinit var showUnlockedToastEventObserver: Observer<String>

    // Subjects and RX observers

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>
    private lateinit var favoriteSubject: BehaviorSubject<AMResultItem.ItemAPIStatus>
    private lateinit var playbackItemSubject: BehaviorSubject<PlaybackItem>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        loginStateChangeSubject = BehaviorSubject.create()
        favoriteSubject = BehaviorSubject.create()
        playbackItemSubject = BehaviorSubject.create()
        val track = mock<AMResultItem> {
            on { itemId } doReturn "123"
        }
        playlist = mock {
            on { itemId } doReturn "111"
            on { favoriteSubject } doReturn favoriteSubject
            on { uploaderSlug } doReturn "matteinn"
            on { tracks } doReturn listOf(track)
        }
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)
        whenever(playback.item).thenReturn(playbackItemSubject)
        schedulersProvider = TestSchedulersProvider()
        viewModel = PlaylistOnboardingViewModel(
            artistImage,
            playlist,
            mixpanelSource,
            imageLoader,
            adsDataSource,
            schedulersProvider,
            musicManager,
            queueDataSource,
            playback,
            userDataSource,
            actionsDataSource
        ).apply {
            notifyOfflineEvent.observeForever(notifyOfflineEventObserver)
            loginRequiredEvent.observeForever(loginRequiredEventObserver)
            notifyFavoriteEvent.observeForever(notifyFavoriteEventObserver)
            showHUDEvent.observeForever(showHUDEventObserver)
            showConfirmDownloadDeletionEvent.observeForever(observerShowConfirmDownloadDeletionEvent)
            premiumRequiredEvent.observeForever(observerPremiumRequiredEvent)
            showPremiumDownloadEvent.observeForever(observerShowPremiumDownloadEvent)
            showUnlockedToastEvent.observeForever(showUnlockedToastEventObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `initial subscriptions`() {
        Assert.assertTrue(playback.item.hasObservers())
        Assert.assertTrue(playlist.favoriteSubject.hasObservers())
    }

    @Test
    fun `back tapped`() {
        val observerBack: Observer<Void> = mock()
        viewModel.backEvent.observeForever(observerBack)
        viewModel.onBackTapped()
        verify(observerBack).onChanged(null)
    }

    @Test
    fun `on destroy`() {
        val observerCleanup: Observer<Void> = mock()
        viewModel.cleanupEvent.observeForever(observerCleanup)
        viewModel.onDestroy()
        verify(observerCleanup).onChanged(null)
    }

    @Test
    fun `on recyclerview ready`() {
        val observerScroll: Observer<Void> = mock()
        viewModel.scrollEvent.observeForever(observerScroll)
        viewModel.onRecyclerViewConfigured()
        verify(observerScroll).onChanged(null)
    }

    @Test
    fun `on scroll`() {
        val observerScroll: Observer<Void> = mock()
        viewModel.scrollEvent.observeForever(observerScroll)
        viewModel.onScroll()
        verify(observerScroll).onChanged(null)
    }

    @Test
    fun `play all tapped, playlist already playing, paused`() {
        whenever(queueDataSource.isCurrentItemOrParent(any())).thenReturn(true)
        whenever(playback.isPlaying).thenReturn(false)
        viewModel.onPlayAllTapped()
        verify(queueDataSource).isCurrentItemOrParent(any())
        verify(playback).play()
    }

    @Test
    fun `play all tapped, playlist already playing, playing`() {
        whenever(queueDataSource.isCurrentItemOrParent(any())).thenReturn(true)
        whenever(playback.isPlaying).thenReturn(true)
        viewModel.onPlayAllTapped()
        verify(queueDataSource).isCurrentItemOrParent(any())
        verify(playback).pause()
    }

    @Test
    fun `play all tapped, playlist not already playing, georestricted item`() {
        whenever(queueDataSource.isCurrentItemOrParent(any())).thenReturn(false)
        whenever(musicManager.isDownloadFailed(any())).thenReturn(Observable.just(false))
        viewModel.onPlayAllTapped()
        verify(queueDataSource).isCurrentItemOrParent(any())
        verify(playback, never()).play()
        verify(playback, never()).pause()
    }

    @Test
    fun `shuffle tapped`() {
        val observerShuffle: Observer<Void> = mock()
        viewModel.shuffleEvent.observeForever(observerShuffle)
        viewModel.onShuffleTapped()
        verify(observerShuffle).onChanged(null)
    }

    @Test
    fun `open track, failed download`() {
        `when`(musicManager.isDownloadFailed(any())).thenReturn(Observable.just(true))
        val observerOpenTrack: Observer<Triple<AMResultItem, AMResultItem?, Int>> = mock()
        val observerOpenFailedDownload: Observer<AMResultItem> = mock()
        viewModel.openTrackEvent.observeForever(observerOpenTrack)
        viewModel.openTrackOptionsFailedDownloadEvent.observeForever(observerOpenFailedDownload)
        viewModel.onTrackTapped(mock())
        verifyZeroInteractions(observerOpenTrack)
        verify(observerOpenFailedDownload).onChanged(any())
    }

    @Test
    fun `open track, not failed download`() {
        `when`(musicManager.isDownloadFailed(any())).thenReturn(Observable.just(false))
        val track = playlist.tracks!!.first()
        val observerOpenTrack: Observer<Triple<AMResultItem, AMResultItem?, Int>> = mock()
        val observerOpenFailedDownload: Observer<AMResultItem> = mock()
        viewModel.openTrackEvent.observeForever(observerOpenTrack)
        viewModel.openTrackOptionsFailedDownloadEvent.observeForever(observerOpenFailedDownload)

        viewModel.onTrackTapped(track)

        verify(observerOpenTrack).onChanged(any())
        verifyZeroInteractions(observerOpenFailedDownload)
    }

    @Test
    fun `open track options, failed download`() {
        `when`(musicManager.isDownloadFailed(any())).thenReturn(Observable.just(true))
        val observerOpenTrackOptions: Observer<AMResultItem> = mock()
        val observerOpenFailedDownload: Observer<AMResultItem> = mock()
        viewModel.openTrackOptionsEvent.observeForever(observerOpenTrackOptions)
        viewModel.openTrackOptionsFailedDownloadEvent.observeForever(observerOpenFailedDownload)
        viewModel.onTrackActionsTapped(mock())
        verifyZeroInteractions(observerOpenTrackOptions)
        verify(observerOpenFailedDownload).onChanged(any())
    }

    @Test
    fun `open track options, not failed download`() {
        `when`(musicManager.isDownloadFailed(any())).thenReturn(Observable.just(false))
        val observerOpenTrackOptions: Observer<AMResultItem> = mock()
        val observerOpenFailedDownload: Observer<AMResultItem> = mock()
        viewModel.openTrackOptionsEvent.observeForever(observerOpenTrackOptions)
        viewModel.openTrackOptionsFailedDownloadEvent.observeForever(observerOpenFailedDownload)
        viewModel.onTrackActionsTapped(mock())
        verify(observerOpenTrackOptions).onChanged(any())
        verifyZeroInteractions(observerOpenFailedDownload)
    }

    @Test
    fun `uploader tapped`() {
        val observerOpenUploader: Observer<String> = mock()
        viewModel.openUploaderEvent.observeForever(observerOpenUploader)
        viewModel.onUploaderTapped()
        verify(observerOpenUploader).onChanged(eq(playlist.uploaderSlug))
    }

    @Test
    fun `on song changed`() {
        val observerUpdatePlayEvent: Observer<Boolean> = mock()
        val observerUpdateListEvent: Observer<Void> = mock()
        viewModel.updatePlayEvent.observeForever(observerUpdatePlayEvent)
        viewModel.updateListEvent.observeForever(observerUpdateListEvent)
        playbackItemSubject.onNext(mock())
        verify(observerUpdatePlayEvent).onChanged(anyBoolean())
        verify(observerUpdateListEvent).onChanged(null)
    }

    @Test
    fun `on play pause changed`() {
        val observerUpdatePlayEvent: Observer<Boolean> = mock()
        viewModel.updatePlayEvent.observeForever(observerUpdatePlayEvent)
        viewModel.onPlayPauseChanged()
        verify(observerUpdatePlayEvent).onChanged(anyBoolean())
    }

    @Test
    fun `on create`() {
        val observerUpdatePlayEvent: Observer<Boolean> = mock()
        val observerUpdateDetailsEvent: Observer<Void> = mock()
        viewModel.updatePlayEvent.observeForever(observerUpdatePlayEvent)
        viewModel.updateDetailsEvent.observeForever(observerUpdateDetailsEvent)
        viewModel.onCreate()
        verify(observerUpdatePlayEvent).onChanged(anyBoolean())
        verify(observerUpdateDetailsEvent).onChanged(null)
    }

    // Favorite

    @Test
    fun `favorite, notify favorite event`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(
            ToggleFavoriteResult.Notify(true, true, true, true, true, "title", "artist")
        ))
        viewModel.onFavoriteTapped()
        verify(notifyFavoriteEventObserver).onChanged(any())
    }

    @Test
    fun `favorite, logged out then login`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(
            ToggleFavoriteException.LoggedOut))

        viewModel.onFavoriteTapped()
        verifyNoMoreInteractions(notifyFavoriteEventObserver)
        verify(loginRequiredEventObserver).onChanged(eq(LoginSignupSource.Favorite))

        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(
            ToggleFavoriteResult.Notify(true, true, true, true, true, "title", "artist")
        ))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)
        verify(notifyFavoriteEventObserver).onChanged(any())
    }

    @Test
    fun `favorite, offline`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(
            ToggleFavoriteException.Offline))
        viewModel.onFavoriteTapped()
        verify(notifyOfflineEventObserver).onChanged(null)
    }

    @Test
    fun `favorite, generic error`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onFavoriteTapped()
        verify(showHUDEventObserver).onChanged(argWhere { it is ProgressHUDMode.Failure })
    }

    // Download

    @Test
    fun `on track download tapped, test all results`() {

        val tracksCount = 10
        val musicTitle = "title"
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.just(
            ToggleDownloadResult.ConfirmPlaylistDeletion,
            ToggleDownloadResult.ConfirmMusicDeletion,
            ToggleDownloadResult.StartedBlockingAPICall,
            ToggleDownloadResult.EndedBlockingAPICall,
            ToggleDownloadResult.ConfirmPlaylistDownload(tracksCount),
            ToggleDownloadResult.ShowUnlockedToast(musicTitle)
        ))

        viewModel.onTrackDownloadTapped(mock())
        verify(observerShowConfirmDownloadDeletionEvent).onChanged(any())
        verify(showHUDEventObserver).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver).onChanged(ProgressHUDMode.Dismiss)
        verify(showUnlockedToastEventObserver).onChanged(musicTitle)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `on track download tapped, logged out`() {
        val source = LoginSignupSource.Download
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.LoggedOut(source)
        ))

        viewModel.onTrackDownloadTapped(mock())
        verify(loginRequiredEventObserver).onChanged(source)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `on track download tapped, not premium`() {
        val mode = InAppPurchaseMode.PlaylistDownload
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.Unsubscribed(mode)
        ))

        viewModel.onTrackDownloadTapped(mock())
        verify(observerPremiumRequiredEvent).onChanged(mode)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, show premium downloads`() {
        val model = PremiumDownloadModel(mock(), mock(), mock())
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.ShowPremiumDownload(model)
        ))

        viewModel.onTrackDownloadTapped(mock())
        verify(observerShowPremiumDownloadEvent).onChanged(model)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }
}
