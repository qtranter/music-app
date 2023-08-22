package com.audiomack.ui.album

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.common.State
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.actions.ToggleRepostException
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.music.MusicManager
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.model.EventLoginState
import com.audiomack.model.EventPlayer
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.playback.PlaybackItem
import com.audiomack.playback.PlaybackState
import com.audiomack.playback.PlaybackState.IDLE
import com.audiomack.playback.PlayerPlayback
import com.audiomack.playback.SongAction
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.usecases.RefreshCommentCountUseCase
import com.audiomack.utils.GeneralPreferences
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.atMost
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyBoolean
import org.mockito.MockitoAnnotations

class AlbumViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var album: AMResultItem

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var actionsDataSource: ActionsDataSource

    @Mock
    private lateinit var imageLoader: ImageLoader

    @Mock
    private lateinit var adsDataSource: AdsDataSource

    @Mock
    private lateinit var queueDataSource: QueueDataSource

    @Mock
    private lateinit var playback: PlayerPlayback

    @Mock
    private lateinit var generalPreferences: GeneralPreferences

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    @Mock
    private lateinit var musicManager: MusicManager

    @Mock
    private lateinit var premiumDownloadDataSource: PremiumDownloadDataSource

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Mock
    private lateinit var eventBus: EventBus

    @Mock
    private lateinit var refreshCommentCountUseCase: RefreshCommentCountUseCase

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: AlbumViewModel

    private val mixpanelSource = MixpanelSource("", "")

    // Observers

    @Mock
    private lateinit var notifyOfflineEventObserver: Observer<Void>

    @Mock
    private lateinit var showErrorEventObserver: Observer<String>

    @Mock
    private lateinit var notifyRepostEventObserver: Observer<ToggleRepostResult.Notify>

    @Mock
    private lateinit var loginRequiredEventObserver: Observer<LoginSignupSource>

    @Mock
    private lateinit var followStatusObserver: Observer<Boolean>

    @Mock
    private lateinit var notifyFavoriteEventObserver: Observer<ToggleFavoriteResult.Notify>

    @Mock
    private lateinit var scrollEventObserver: Observer<Void>

    @Mock
    private lateinit var openCommentsEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var georestrictedMusicClickedEventObserver: Observer<Void>

    @Mock
    private lateinit var openTrackOptionsFailedDownloadEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var openTrackOptionsEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var openTrackEventObserver: Observer<Triple<AMResultItem, AMResultItem?, Int>>

    @Mock
    private lateinit var downloadTooltipEventObserver: Observer<Void>

    @Mock
    private lateinit var shareEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var openMusicInfoEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var closeEventObserver: Observer<Void>

    @Mock
    private lateinit var openUploaderEventObserver: Observer<String>

    @Mock
    private lateinit var reloadAdapterTrackEventObserver: Observer<Int>

    @Mock
    private lateinit var reloadAdapterTracksEventObserver: Observer<Void>

    @Mock
    private lateinit var shuffleEventObserver: Observer<Pair<AMResultItem, AMResultItem>>

    @Mock
    private lateinit var commentsCountObserver: Observer<Int>

    @Mock
    private lateinit var observerShowHUD: Observer<ProgressHUDMode>

    @Mock
    private lateinit var observerShowConfirmDownloadDeletionEvent: Observer<AMResultItem>

    @Mock
    private lateinit var removeTrackFromAdapterEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var observerPremiumRequiredEvent: Observer<InAppPurchaseMode>

    @Mock
    private lateinit var observerShowPremiumDownloadEvent: Observer<PremiumDownloadModel>

    @Mock
    private lateinit var downloadActionObserver: Observer<SongAction>

    @Mock
    private lateinit var showUnlockedToastEventObserver: Observer<String>

    @Mock
    private lateinit var promptNotificationPermissionEventObserver: Observer<PermissionRedirect>

    @Mock
    private lateinit var tagEventObserver: Observer<String>

    @Mock
    private lateinit var genreEventObserver: Observer<String>

    // Subjects

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>
    private lateinit var favoriteSubject: BehaviorSubject<AMResultItem.ItemAPIStatus>
    private lateinit var repostSubject: BehaviorSubject<AMResultItem.ItemAPIStatus>
    private lateinit var commentsCountSubject: BehaviorSubject<Int>
    private lateinit var playbackStateSubject: BehaviorSubject<PlaybackState>
    private lateinit var playbackItemSubject: BehaviorSubject<PlaybackItem>
    private lateinit var premiumObservable: Subject<Boolean>

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        loginStateChangeSubject = BehaviorSubject.create()
        favoriteSubject = BehaviorSubject.create()
        repostSubject = BehaviorSubject.create()
        commentsCountSubject = BehaviorSubject.create()
        playbackStateSubject = BehaviorSubject.create()
        playbackItemSubject = BehaviorSubject.create()
        premiumObservable = PublishSubject.create()

        whenever(premiumDataSource.premiumObservable).thenReturn(premiumObservable)

        val track = mock<AMResultItem> {
            on { itemId } doReturn "2"
        }
        album = mock {
            on { itemId } doReturn "1"
            on { favoriteSubject } doReturn favoriteSubject
            on { repostSubject } doReturn repostSubject
            on { commentsCountSubject } doReturn commentsCountSubject
            on { uploaderSlug } doReturn "matteinn"
            on { tracks } doReturn listOf(track)
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
            on { genre } doReturn "rap"
            on { tags } doReturn arrayOf("hip-hop")
        }
        whenever(playback.state).thenReturn(State(IDLE))
        whenever(playback.item).thenReturn(playbackItemSubject)
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)
        whenever(refreshCommentCountUseCase.refresh(any())).doReturn(Completable.complete())
        whenever(musicDataSource.getOfflineResource(any())).thenReturn(Observable.error(Exception("")))
        schedulersProvider = TestSchedulersProvider()
        viewModel = AlbumViewModel(
            album,
            mixpanelSource,
            false,
            userDataSource,
            actionsDataSource,
            imageLoader,
            adsDataSource,
            queueDataSource,
            playback,
            generalPreferences,
            musicDataSource,
            musicManager,
            eventBus,
            refreshCommentCountUseCase,
            premiumDownloadDataSource,
            premiumDataSource,
            schedulersProvider
        ).apply {
            notifyOfflineEvent.observeForever(notifyOfflineEventObserver)
            showErrorEvent.observeForever(showErrorEventObserver)
            notifyRepostEvent.observeForever(notifyRepostEventObserver)
            loginRequiredEvent.observeForever(loginRequiredEventObserver)
            followStatus.observeForever(followStatusObserver)
            notifyFavoriteEvent.observeForever(notifyFavoriteEventObserver)
            scrollEvent.observeForever(scrollEventObserver)
            openCommentsEvent.observeForever(openCommentsEventObserver)
            georestrictedMusicClickedEvent.observeForever(georestrictedMusicClickedEventObserver)
            openTrackOptionsFailedDownloadEvent.observeForever(openTrackOptionsFailedDownloadEventObserver)
            openTrackOptionsEvent.observeForever(openTrackOptionsEventObserver)
            openTrackEvent.observeForever(openTrackEventObserver)
            downloadTooltipEvent.observeForever(downloadTooltipEventObserver)
            shareEvent.observeForever(shareEventObserver)
            openMusicInfoEvent.observeForever(openMusicInfoEventObserver)
            closeEvent.observeForever(closeEventObserver)
            openUploaderEvent.observeForever(openUploaderEventObserver)
            reloadAdapterTrackEvent.observeForever(reloadAdapterTrackEventObserver)
            reloadAdapterTracksEvent.observeForever(reloadAdapterTracksEventObserver)
            shuffleEvent.observeForever(shuffleEventObserver)
            commentsCount.observeForever(commentsCountObserver)
            showHUDEvent.observeForever(observerShowHUD)
            showConfirmDownloadDeletionEvent.observeForever(observerShowConfirmDownloadDeletionEvent)
            removeTrackFromAdapterEvent.observeForever(removeTrackFromAdapterEventObserver)
            premiumRequiredEvent.observeForever(observerPremiumRequiredEvent)
            showPremiumDownloadEvent.observeForever(observerShowPremiumDownloadEvent)
            downloadAction.observeForever(downloadActionObserver)
            showUnlockedToastEvent.observeForever(showUnlockedToastEventObserver)
            promptNotificationPermissionEvent.observeForever(promptNotificationPermissionEventObserver)
            tagEvent.observeForever(tagEventObserver)
            genreEvent.observeForever(genreEventObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `on uploader tapped`() {
        viewModel.onUploaderTapped()
        verify(openUploaderEventObserver).onChanged(any())
    }

    @Test
    fun `on back tapped`() {
        viewModel.onBackTapped()
        verify(closeEventObserver).onChanged(null)
    }

    @Test
    fun `on info tapped`() {
        viewModel.onInfoTapped()
        verify(openMusicInfoEventObserver).onChanged(any())
    }

    @Test
    fun `on share tapped`() {
        viewModel.onShareTapped()
        verify(shareEventObserver).onChanged(any())
    }

    @Test
    fun `open comments`() {
        viewModel.onCommentsTapped()
        verify(openCommentsEventObserver).onChanged(any())
    }

    @Test
    fun `on scroll`() {
        viewModel.onScroll()
        verify(scrollEventObserver).onChanged(null)
    }

    // Play

    @Test
    fun `on play all tapped, currently playing album, paused`() {
        whenever(queueDataSource.isCurrentItemOrParent(any())).thenReturn(true)
        whenever(playback.isPlaying).thenReturn(false)
        viewModel.onPlayAllTapped()
        verify(playback).play()
    }

    @Test
    fun `on play all tapped, currently playing album, playing`() {
        whenever(queueDataSource.isCurrentItemOrParent(any())).thenReturn(true)
        whenever(playback.isPlaying).thenReturn(true)
        viewModel.onPlayAllTapped()
        verify(playback).pause()
    }

    @Test
    fun `on play all tapped, currently not playing album, can play`() {
        whenever(queueDataSource.isCurrentItemOrParent(any())).thenReturn(false)
        whenever(musicManager.isDownloadFailed(any())).thenReturn(Observable.error(Exception("Test")))
        viewModel.onPlayAllTapped()
        verify(playback, never()).play()
        verify(playback, never()).pause()
    }

    // Shuffle

    @Test
    fun `on shuffle tapped`() {
        whenever(queueDataSource.isCurrentItemOrParent(any())).thenReturn(false)
        whenever(queueDataSource.shuffle).thenReturn(false)
        viewModel.onShuffleTapped()
        verify(shuffleEventObserver).onChanged(any())
        verify(eventBus, never()).post(argWhere { it is EventPlayer })
    }

    // Tooltips

    @Test
    fun `on layout ready, need to show tooltip`() {
        whenever(generalPreferences.needToShowAlbumDownloadTooltip()).thenReturn(true)
        viewModel.onLayoutReady()
        verify(generalPreferences).setAlbumDownloadTooltipShown()
        verify(downloadTooltipEventObserver).onChanged(null)
    }

    @Test
    fun `on layout ready, tooltip already shown`() {
        whenever(generalPreferences.needToShowAlbumDownloadTooltip()).thenReturn(false)
        viewModel.onLayoutReady()
        verify(generalPreferences, never()).setAlbumDownloadTooltipShown()
        verifyZeroInteractions(downloadTooltipEventObserver)
    }

    // RecyclerView actions

    @Test
    fun `open track, georestricted content`() {
        val track = mock<AMResultItem> {
            on { isGeoRestricted } doReturn true
        }
        viewModel.onTrackTapped(track)
        verify(georestrictedMusicClickedEventObserver).onChanged(null)
        verifyZeroInteractions(musicManager)
    }

    @Test
    fun `open track, failed download`() {
        `when`(musicManager.isDownloadFailed(any())).thenReturn(Observable.just(true))
        viewModel.onTrackTapped(mock())
        verifyZeroInteractions(openTrackEventObserver)
        verify(openTrackOptionsFailedDownloadEventObserver).onChanged(any())
    }

    @Test
    fun `open track, not failed download`() {
        `when`(musicManager.isDownloadFailed(any())).thenReturn(Observable.just(false))
        viewModel.onTrackTapped(mock())
        verify(openTrackEventObserver).onChanged(any())
        verifyZeroInteractions(openTrackOptionsFailedDownloadEventObserver)
    }

    @Test
    fun `open track options, georestricted content`() {
        val track = mock<AMResultItem> {
            on { isGeoRestricted } doReturn true
        }
        viewModel.onTrackActionsTapped(track)
        verify(georestrictedMusicClickedEventObserver).onChanged(null)
    }

    @Test
    fun `open track options, failed download`() {
        `when`(musicManager.isDownloadFailed(any())).thenReturn(Observable.just(true))
        viewModel.onTrackActionsTapped(mock())
        verifyZeroInteractions(openTrackOptionsEventObserver)
        verify(openTrackOptionsFailedDownloadEventObserver).onChanged(any())
    }

    @Test
    fun `open track options, not failed download`() {
        `when`(musicManager.isDownloadFailed(any())).thenReturn(Observable.just(false))
        viewModel.onTrackActionsTapped(mock())
        verify(openTrackOptionsEventObserver).onChanged(any())
        verifyZeroInteractions(openTrackOptionsFailedDownloadEventObserver)
    }

    @Test
    fun `remove track from adapter`() {
        viewModel.onRemoveTrackFromAdapter(mock())
        verify(removeTrackFromAdapterEventObserver).onChanged(any())
    }

    // Download

    @Test
    fun `download, check storage permission, accept, test all results`() {
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

        viewModel.onDownloadTapped()
        verify(observerShowConfirmDownloadDeletionEvent).onChanged(any())
        verify(observerShowHUD).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD).onChanged(ProgressHUDMode.Dismiss)
        verify(showUnlockedToastEventObserver).onChanged(musicTitle)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, check storage permission, accept, logged out`() {
        val source = LoginSignupSource.Download
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.LoggedOut(source)
        ))

        viewModel.onDownloadTapped()
        verify(loginRequiredEventObserver).onChanged(source)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, check storage permission, accept, not premium`() {
        val mode = InAppPurchaseMode.PlaylistDownload
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.Unsubscribed(mode)
        ))

        viewModel.onDownloadTapped()
        verify(observerPremiumRequiredEvent).onChanged(mode)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, check storage permission, accept, show premium downloads`() {
        val model = PremiumDownloadModel(mock(), mock(), mock())
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.ShowPremiumDownload(model)
        ))

        viewModel.onDownloadTapped()
        verify(observerShowPremiumDownloadEvent).onChanged(model)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    // Follow

    @Test
    fun `toggle follow, logged in`() {
        val result = true
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.just(ToggleFollowResult.Finished(true)))
        viewModel.onFollowTapped(mixpanelSource)
        verify(followStatusObserver, atLeast(1)).onChanged(result)
    }

    @Test
    fun `toggle follow, ask for permissions`() {
        val redirect = PermissionRedirect.Settings
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.just(ToggleFollowResult.AskForPermission(redirect)))
        viewModel.onFollowTapped(mixpanelSource)
        verify(promptNotificationPermissionEventObserver).onChanged(redirect)
    }

    @Test
    fun `toggle follow, offline`() {
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.error(ToggleFollowException.Offline))
        viewModel.onFollowTapped(mixpanelSource)
        verify(followStatusObserver, times(1)).onChanged(ArgumentMatchers.anyBoolean())
        verify(notifyOfflineEventObserver).onChanged(null)
    }

    @Test
    fun `toggle follow, logged out`() {
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.error(ToggleFollowException.LoggedOut))
        viewModel.onFollowTapped(mixpanelSource)
        verify(followStatusObserver, atMost(1)).onChanged(anyBoolean())
        verify(loginRequiredEventObserver).onChanged(eq(LoginSignupSource.AccountFollow))

        val result = true
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.just(ToggleFollowResult.Finished(true)))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)
        verify(followStatusObserver, atLeast(1)).onChanged(result)
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
        verify(showErrorEventObserver).onChanged(any())
    }

    // Repost

    @Test
    fun `repost, notify repost event and triggers in app rating`() {
        `when`(actionsDataSource.toggleRepost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(
            ToggleRepostResult.Notify(true, true, "title", "artist")
        ))
        viewModel.onRepostTapped()
        verify(notifyRepostEventObserver).onChanged(any())
    }

    @Test
    fun `repost, logged out then login`() {
        `when`(actionsDataSource.toggleRepost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(
            ToggleRepostException.LoggedOut))

        viewModel.onRepostTapped()
        verifyNoMoreInteractions(notifyRepostEventObserver)
        verify(loginRequiredEventObserver).onChanged(eq(LoginSignupSource.Repost))

        `when`(actionsDataSource.toggleRepost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(
            ToggleRepostResult.Notify(true, true, "title", "artist")
        ))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)
        verify(notifyRepostEventObserver).onChanged(any())
    }

    @Test
    fun `repost, offline`() {
        `when`(actionsDataSource.toggleRepost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(
            ToggleRepostException.Offline))
        viewModel.onRepostTapped()
        verify(notifyOfflineEventObserver).onChanged(null)
    }

    @Test
    fun `repost, generic error`() {
        `when`(actionsDataSource.toggleRepost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onRepostTapped()
        verify(showErrorEventObserver).onChanged(any())
    }

    // Favorite track

    @Test
    fun `favorite track, notify favorite event and triggers in app rating and notifies adapter`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(
            ToggleFavoriteResult.Notify(true, true, true, true, true, "title", "artist")
        ))
        viewModel.onTrackFavoriteTapped(album.tracks!!.first())
        verify(notifyFavoriteEventObserver).onChanged(any())
        verify(reloadAdapterTrackEventObserver).onChanged(eq(0))
    }

    @Test
    fun `favorite track, logged out then login`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(
            ToggleFavoriteException.LoggedOut))

        viewModel.onTrackFavoriteTapped(album.tracks!!.first())
        verifyNoMoreInteractions(notifyFavoriteEventObserver)
        verify(loginRequiredEventObserver).onChanged(eq(LoginSignupSource.Favorite))

        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(
            ToggleFavoriteResult.Notify(true, true, true, true, true, "title", "artist")
        ))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)
        verify(notifyFavoriteEventObserver).onChanged(any())
    }

    @Test
    fun `favorite track, offline`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(
            ToggleFavoriteException.Offline))
        viewModel.onTrackFavoriteTapped(album.tracks!!.first())
        verify(notifyOfflineEventObserver).onChanged(null)
    }

    @Test
    fun `favorite track, generic error`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onTrackFavoriteTapped(album.tracks!!.first())
        verify(showErrorEventObserver).onChanged(any())
    }

    // Comments count

    @Test
    fun `comments count observed`() {
        val count = 3
        commentsCountSubject.onNext(count)
        Mockito.verify(commentsCountObserver).onChanged(eq(count))
    }

    // Premium status changes

    @Test
    fun `download action observed when changed`() {
        premiumObservable.onNext(true)
        verify(downloadActionObserver, times(2)).onChanged(any()) // first time set during init
    }

    // EventBus

    @Test
    fun `on downloads edited reload all tracks`() {
        viewModel.onMessageEvent(EventDownloadsEdited())
        verify(reloadAdapterTracksEventObserver).onChanged(null)
        verify(downloadActionObserver, times(2)).onChanged(any()) // first time set during init
    }

    // Tags

    @Test
    fun `genre tapped`() {
        viewModel.onTagTapped("rap")
        verify(genreEventObserver, atLeast(1)).onChanged("rap")
    }

    @Test
    fun `tag tapped`() {
        viewModel.onTagTapped("hip-hop")
        verify(tagEventObserver, atLeast(1)).onChanged("tag:hip-hop")
    }
}
