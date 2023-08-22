package com.audiomack.ui.playlist.details

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
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.music.MusicManager
import com.audiomack.data.playlist.PlayListDataSource
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.reachability.ReachabilityDataSource
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
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class PlaylistViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var playlist: AMResultItem
    private var online: Boolean = true
    private var deleted: Boolean = false
    private val mixpanelSource = MixpanelSource("", "")

    @Mock
    private lateinit var imageLoader: ImageLoader

    @Mock
    private lateinit var adsDataSource: AdsDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    @Mock
    private lateinit var generalPreferences: GeneralPreferences

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    @Mock
    private lateinit var musicManager: MusicManager

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var actionsDataSource: ActionsDataSource

    @Mock
    private lateinit var queueDataSource: QueueDataSource

    @Mock
    private lateinit var playback: PlayerPlayback

    @Mock
    private lateinit var eventBus: EventBus

    @Mock
    private lateinit var refreshCommentCountUseCase: RefreshCommentCountUseCase

    @Mock
    private lateinit var playlistDataSource: PlayListDataSource

    @Mock
    private lateinit var premiumDownloadDataSource: PremiumDownloadDataSource

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Mock
    private lateinit var reachabilityDataSource: ReachabilityDataSource

    // SUT
    private lateinit var viewModel: PlaylistViewModel

    // Observers

    @Mock
    private lateinit var closeEventObserver: Observer<Void>

    @Mock
    private lateinit var openCommentsEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var scrollEventObserver: Observer<Void>

    @Mock
    private lateinit var closeOptionsEventObserver: Observer<Void>

    @Mock
    private lateinit var shareEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var showEditMenuEventObserver: Observer<Void>

    @Mock
    private lateinit var openInfoEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var notifyOfflineEventObserver: Observer<Void>

    @Mock
    private lateinit var loginRequiredEventObserver: Observer<LoginSignupSource>

    @Mock
    private lateinit var openUploaderEventObserver: Observer<String>

    @Mock
    private lateinit var addDeleteDownloadEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var reloadAdapterTrackEventObserver: Observer<Int>

    @Mock
    private lateinit var reloadAdapterTracksEventObserver: Observer<Void>

    @Mock
    private lateinit var shuffleEventObserver: Observer<Pair<AMResultItem, AMResultItem>>

    @Mock
    private lateinit var openTrackEventObserver: Observer<Triple<AMResultItem, AMResultItem?, Int>>

    @Mock
    private lateinit var georestrictedMusicClickedEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var openTrackOptionsFailedDownloadEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var openTrackOptionsEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var openMusicInfoEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var showFavoriteTooltipEventObserver: Observer<Void>

    @Mock
    private lateinit var showDownloadTooltipEventObserver: Observer<Void>

    @Mock
    private lateinit var followStatusObserver: Observer<Boolean>

    @Mock
    private lateinit var notifyFavoriteEventObserver: Observer<ToggleFavoriteResult.Notify>

    @Mock
    private lateinit var createPlaylistStatusEventObserver: Observer<PlaylistViewModel.CreatePlaylistStatus>

    @Mock
    private lateinit var deletePlaylistStatusEventObserver: Observer<PlaylistViewModel.DeletePlaylistStatus>

    @Mock
    private lateinit var showHUDEventObserver: Observer<ProgressHUDMode>

    @Mock
    private lateinit var commentsCountObserver: Observer<Int>

    @Mock
    private lateinit var setupTracksEventObserver: Observer<AMResultItem>

    @Mock
    private lateinit var observerShowConfirmPlaylistSyncEvent: Observer<Int>

    @Mock
    private lateinit var observerShowConfirmDownloadDeletionEvent: Observer<AMResultItem>

    @Mock
    private lateinit var observerShowConfirmPlaylistDownloadDeletionEvent: Observer<AMResultItem>

    @Mock
    private lateinit var observerPremiumRequiredEvent: Observer<InAppPurchaseMode>

    @Mock
    private lateinit var observerShowFailedPlaylistDownloadEvent: Observer<Void>

    @Mock
    private lateinit var observerShowPremiumDownloadEvent: Observer<PremiumDownloadModel>

    @Mock
    private lateinit var downloadActionObserver: Observer<SongAction>

    @Mock
    private lateinit var showUnlockedToastEventObserver: Observer<String>

    @Mock
    private lateinit var promptNotificationPermissionEventObserver: Observer<PermissionRedirect>

    // Subjects and RX observers

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>
    private lateinit var playbackItemSubject: BehaviorSubject<PlaybackItem>
    private lateinit var favoriteSubject: BehaviorSubject<AMResultItem.ItemAPIStatus>
    private lateinit var commentsCountSubject: BehaviorSubject<Int>
    private lateinit var premiumObservable: Subject<Boolean>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        loginStateChangeSubject = BehaviorSubject.create()
        favoriteSubject = BehaviorSubject.create()
        commentsCountSubject = BehaviorSubject.create()
        playbackItemSubject = BehaviorSubject.create()
        premiumObservable = PublishSubject.create()

        whenever(premiumDataSource.premiumObservable).thenReturn(premiumObservable)

        val track = mock<AMResultItem> {
            on { itemId } doReturn "2"
        }
        playlist = mock {
            on { itemId } doReturn "111"
            on { favoriteSubject } doReturn favoriteSubject
            on { commentsCountSubject } doReturn commentsCountSubject
            on { uploaderSlug } doReturn "matteinn"
            on { tracks } doReturn listOf(track)
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }

        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)
        whenever(playback.state).thenReturn(State(IDLE))
        whenever(playback.item).thenReturn(playbackItemSubject)
        whenever(refreshCommentCountUseCase.refresh(any())).doReturn(Completable.complete())

        schedulersProvider = TestSchedulersProvider()

        viewModel = PlaylistViewModel(
            playlist,
            online,
            deleted,
            mixpanelSource,
            false,
            imageLoader,
            adsDataSource,
            schedulersProvider,
            generalPreferences,
            musicDataSource,
            musicManager,
            userDataSource,
            actionsDataSource,
            queueDataSource,
            playback,
            eventBus,
            refreshCommentCountUseCase,
            playlistDataSource,
            premiumDownloadDataSource,
            premiumDataSource,
                reachabilityDataSource
        ).apply {
            closeEvent.observeForever(closeEventObserver)
            scrollEvent.observeForever(scrollEventObserver)
            closeOptionsEvent.observeForever(closeOptionsEventObserver)
            shareEvent.observeForever(shareEventObserver)
            showEditMenuEvent.observeForever(showEditMenuEventObserver)
            openMusicInfoEvent.observeForever(openInfoEventObserver)
            notifyOfflineEvent.observeForever(notifyOfflineEventObserver)
            loginRequiredEvent.observeForever(loginRequiredEventObserver)
            georestrictedMusicClickedEvent.observeForever(georestrictedMusicClickedEventObserver)
            openTrackOptionsFailedDownloadEvent.observeForever(openTrackOptionsFailedDownloadEventObserver)
            openTrackOptionsEvent.observeForever(openTrackOptionsEventObserver)
            openTrackEvent.observeForever(openTrackEventObserver)
            openUploaderEvent.observeForever(openUploaderEventObserver)
            addDeleteDownloadEvent.observeForever(addDeleteDownloadEventObserver)
            reloadAdapterTrackEvent.observeForever(reloadAdapterTrackEventObserver)
            reloadAdapterTracksEvent.observeForever(reloadAdapterTracksEventObserver)
            shuffleEvent.observeForever(shuffleEventObserver)
            openCommentsEvent.observeForever(openCommentsEventObserver)
            openMusicInfoEvent.observeForever(openMusicInfoEventObserver)
            showFavoriteTooltipEvent.observeForever(showFavoriteTooltipEventObserver)
            showDownloadTooltipEvent.observeForever(showDownloadTooltipEventObserver)
            followStatus.observeForever(followStatusObserver)
            notifyFavoriteEvent.observeForever(notifyFavoriteEventObserver)
            createPlaylistStatusEvent.observeForever(createPlaylistStatusEventObserver)
            deletePlaylistStatusEvent.observeForever(deletePlaylistStatusEventObserver)
            showHUDEvent.observeForever(showHUDEventObserver)
            commentsCount.observeForever(commentsCountObserver)
            setupTracksEvent.observeForever(setupTracksEventObserver)
            showConfirmPlaylistSyncEvent.observeForever(observerShowConfirmPlaylistSyncEvent)
            showConfirmDownloadDeletionEvent.observeForever(observerShowConfirmDownloadDeletionEvent)
            showConfirmPlaylistDownloadDeletionEvent.observeForever(observerShowConfirmPlaylistDownloadDeletionEvent)
            premiumRequiredEvent.observeForever(observerPremiumRequiredEvent)
            showFailedPlaylistDownloadEvent.observeForever(observerShowFailedPlaylistDownloadEvent)
            showPremiumDownloadEvent.observeForever(observerShowPremiumDownloadEvent)
            downloadAction.observeForever(downloadActionObserver)
            showUnlockedToastEvent.observeForever(showUnlockedToastEventObserver)
            promptNotificationPermissionEvent.observeForever(promptNotificationPermissionEventObserver)
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
    fun `on edit tapped`() {
        viewModel.onEditTapped()
        verify(showEditMenuEventObserver).onChanged(null)
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
    fun `on layout ready, need to show favorite tooltip`() {
        whenever(generalPreferences.needToShowPlaylistFavoriteTooltip()).thenReturn(true)
        viewModel.onLayoutReady()
        verify(generalPreferences).setPlaylistFavoriteTooltipShown()
        verify(generalPreferences, never()).setPlaylistDownloadTooltipShown()
        verify(showFavoriteTooltipEventObserver).onChanged(null)
    }

    @Test
    fun `on layout ready, need to show download tooltip already shown`() {
        whenever(generalPreferences.needToShowAlbumDownloadTooltip()).thenReturn(false)
        whenever(generalPreferences.needToShowPlaylistDownloadTooltip()).thenReturn(true)
        viewModel.onLayoutReady()
        verify(generalPreferences, never()).setPlaylistFavoriteTooltipShown()
        verify(generalPreferences).setPlaylistDownloadTooltipShown()
        verifyZeroInteractions(showFavoriteTooltipEventObserver)
        verify(showDownloadTooltipEventObserver).onChanged(null)
    }

    @Test
    fun `on layout ready, tooltips already shown`() {
        whenever(generalPreferences.needToShowAlbumDownloadTooltip()).thenReturn(false)
        whenever(generalPreferences.needToShowPlaylistDownloadTooltip()).thenReturn(false)
        viewModel.onLayoutReady()
        verify(generalPreferences, never()).setPlaylistFavoriteTooltipShown()
        verify(generalPreferences, never()).setPlaylistDownloadTooltipShown()
        verifyZeroInteractions(showFavoriteTooltipEventObserver)
        verifyZeroInteractions(showDownloadTooltipEventObserver)
    }

    // RecyclerView actions

    @Test
    fun `open track, georestricted content`() {
        val track = mock<AMResultItem> {
            on { isGeoRestricted } doReturn true
        }
        viewModel.onTrackTapped(track)
        verify(georestrictedMusicClickedEventObserver).onChanged(track)
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
        verify(georestrictedMusicClickedEventObserver).onChanged(track)
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

    // Edit

    @Test
    fun `options reorder track tapped`() {
        val observerReorder: Observer<AMResultItem> = mock()
        viewModel.openReorderEvent.observeForever(observerReorder)
        viewModel.onOptionReorderRemoveTracksTapped()
        verify(closeOptionsEventObserver).onChanged(null)
        verify(observerReorder).onChanged(argWhere { it.uploaderSlug == playlist.uploaderSlug })
    }

    @Test
    fun `options edit playlist tapped`() {
        val observerEdit: Observer<AMResultItem> = mock()
        viewModel.openEditEvent.observeForever(observerEdit)
        viewModel.onOptionEditPlaylistTapped()
        verify(closeOptionsEventObserver).onChanged(null)
        verify(observerEdit).onChanged(argWhere { it.uploaderSlug == playlist.uploaderSlug })
    }

    @Test
    fun `options share playlist tapped`() {
        viewModel.onOptionSharePlaylistTapped()
        verify(closeOptionsEventObserver).onChanged(null)
        verify(shareEventObserver).onChanged(argWhere { it.uploaderSlug == playlist.uploaderSlug })
    }

    @Test
    fun `options delete playlist tapped`() {
        val observerShowDeleteConfirmation: Observer<AMResultItem> = mock()
        viewModel.showDeleteConfirmationEvent.observeForever(observerShowDeleteConfirmation)
        viewModel.onOptionDeletePlaylistTapped()
        verify(closeOptionsEventObserver).onChanged(null)
        verify(observerShowDeleteConfirmation).onChanged(argWhere { it.uploaderSlug == playlist.uploaderSlug })
    }

    @Test
    fun `delete playlist succeeds`() {
        `when`(musicDataSource.deletePlaylist(any())).thenReturn(Observable.just(true))
        viewModel.onConfirmDeletePlaylistTapped()
        verify(deletePlaylistStatusEventObserver, times(1)).onChanged(eq(PlaylistViewModel.DeletePlaylistStatus.Loading))
        verify(deletePlaylistStatusEventObserver, times(1)).onChanged(argWhere { it is PlaylistViewModel.DeletePlaylistStatus.Success })
    }

    @Test
    fun `delete playlist fails`() {
        `when`(musicDataSource.deletePlaylist(any())).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onConfirmDeletePlaylistTapped()
        verify(deletePlaylistStatusEventObserver, times(1)).onChanged(eq(PlaylistViewModel.DeletePlaylistStatus.Loading))
        verify(deletePlaylistStatusEventObserver, times(1)).onChanged(argWhere { it is PlaylistViewModel.DeletePlaylistStatus.Error })
    }

    @Test
    fun `on create playlist tapped, success`() {
        `when`(
            musicDataSource.createPlaylist(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                any()
            )
        ).thenReturn(Observable.just(playlist))
        viewModel.onCreatePlaylistTapped()
        verify(createPlaylistStatusEventObserver, times(1)).onChanged(argWhere { it is PlaylistViewModel.CreatePlaylistStatus.Loading })
        verify(createPlaylistStatusEventObserver, times(1)).onChanged(argWhere { it is PlaylistViewModel.CreatePlaylistStatus.Success })
        verify(closeEventObserver).onChanged(null)
    }

    @Test
    fun `on create playlist tapped, failure`() {
        `when`(
            musicDataSource.createPlaylist(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                any()
            )
        ).thenReturn(Observable.error(Exception("unknown error for tests")))
        viewModel.onCreatePlaylistTapped()
        verify(
            createPlaylistStatusEventObserver,
            times(1)
        ).onChanged(argWhere { it is PlaylistViewModel.CreatePlaylistStatus.Loading })
        verify(
            createPlaylistStatusEventObserver,
            times(1)
        ).onChanged(argWhere { it is PlaylistViewModel.CreatePlaylistStatus.Error })
        verifyZeroInteractions(closeEventObserver)
    }

    @Test
    fun onDeleteTakendownPlaylistTapped() {
        viewModel.onDeleteTakendownPlaylistTapped()
        verify(closeEventObserver).onChanged(null)
    }

    @Test
    fun `on sync tapped, not deleted`() {
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(Exception("")))

        val observer: Observer<Void> = mock()
        viewModel.performSyncEvent.observeForever(observer)
        viewModel.onSyncTapped()
        verify(observer).onChanged(null)
    }

    // Follow

    @Test
    fun `toggle follow, logged in`() {
        val result = true
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.just(ToggleFollowResult.Finished(true)))
        viewModel.onFollowTapped()
        verify(followStatusObserver, atLeast(1)).onChanged(result)
    }

    @Test
    fun `toggle follow, ask for permissions`() {
        val redirect = PermissionRedirect.Settings
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.just(ToggleFollowResult.AskForPermission(redirect)))
        viewModel.onFollowTapped()
        verify(promptNotificationPermissionEventObserver).onChanged(redirect)
    }

    @Test
    fun `toggle follow, offline`() {
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.error(ToggleFollowException.Offline))
        viewModel.onFollowTapped()
        verify(followStatusObserver, times(1)).onChanged(anyBoolean())
        verify(notifyOfflineEventObserver).onChanged(null)
    }

    @Test
    fun `toggle follow, logged out`() {
        `when`(actionsDataSource.toggleFollow(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Observable.error(ToggleFollowException.LoggedOut))
        viewModel.onFollowTapped()
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
        verify(showHUDEventObserver).onChanged(argWhere { it is ProgressHUDMode.Failure })
    }

    // Favorite track

    @Test
    fun `favorite track, notify favorite event and notifies adapter`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(
            ToggleFavoriteResult.Notify(true, true, true, true, true, "title", "artist")
        ))
        viewModel.onTrackFavoriteTapped(playlist.tracks!!.first())
        verify(notifyFavoriteEventObserver).onChanged(any())
        verify(reloadAdapterTrackEventObserver).onChanged(eq(0))
    }

    @Test
    fun `favorite track, logged out then login`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(
            ToggleFavoriteException.LoggedOut))

        viewModel.onTrackFavoriteTapped(playlist.tracks!!.first())
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
        viewModel.onTrackFavoriteTapped(playlist.tracks!!.first())
        verify(notifyOfflineEventObserver).onChanged(null)
    }

    @Test
    fun `favorite track, generic error`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onTrackFavoriteTapped(playlist.tracks!!.first())
        verify(showHUDEventObserver).onChanged(argWhere { it is ProgressHUDMode.Failure })
    }

    // Comments count

    @Test
    fun `comments count observed`() {
        val count = 3
        commentsCountSubject.onNext(count)
        verify(commentsCountObserver).onChanged(eq(count))
    }

    // Georestricted actions

    @Test
    fun `removeGeorestrictedTrack success`() {
        val track = mock<AMResultItem>()
        whenever(playlistDataSource.editPlaylist(any(), any(), anyOrNull(), anyOrNull(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(playlist))

        viewModel.removeGeorestrictedTrack(track)

        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDEventObserver, never()).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(setupTracksEventObserver, atLeast(1)).onChanged(playlist)
    }

    @Test
    fun `removeGeorestrictedTrack failure`() {
        val track = mock<AMResultItem>()
        whenever(playlistDataSource.editPlaylist(any(), any(), anyOrNull(), anyOrNull(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(Exception("")))

        viewModel.removeGeorestrictedTrack(track)

        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(showHUDEventObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
    }

    // Download

    @Test
    fun `download playlist, ask confirmation, accept`() {
        val tracksCount = 10
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.just(
            ToggleDownloadResult.ConfirmPlaylistDownload(tracksCount)
        ))

        viewModel.onDownloadTapped()
        verify(observerShowConfirmPlaylistSyncEvent).onChanged(tracksCount)

        viewModel.onPlaylistSyncConfirmed()
        verify(actionsDataSource, times(2)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, test all results`() {
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
        verify(observerShowConfirmPlaylistDownloadDeletionEvent).onChanged(any())
        verify(observerShowConfirmDownloadDeletionEvent).onChanged(any())
        verify(observerShowConfirmPlaylistSyncEvent).onChanged(tracksCount)
        verify(showHUDEventObserver).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver).onChanged(ProgressHUDMode.Dismiss)
        verify(showUnlockedToastEventObserver).onChanged(musicTitle)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, logged out`() {
        val source = LoginSignupSource.Download
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.LoggedOut(source)
        ))

        viewModel.onDownloadTapped()
        verify(loginRequiredEventObserver).onChanged(source)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, not premium`() {
        val mode = InAppPurchaseMode.PlaylistDownload
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.Unsubscribed(mode)
        ))

        viewModel.onDownloadTapped()
        verify(observerPremiumRequiredEvent).onChanged(mode)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, failed downloading playlist`() {
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.FailedDownloadingPlaylist
        ))

        viewModel.onDownloadTapped()
        verify(observerShowFailedPlaylistDownloadEvent).onChanged(null)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `download, show premium downloads`() {
        val model = PremiumDownloadModel(mock(), mock(), mock())
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.error(
            ToggleDownloadException.ShowPremiumDownload(model)
        ))

        viewModel.onDownloadTapped()
        verify(observerShowPremiumDownloadEvent).onChanged(model)

        verify(actionsDataSource, times(1)).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
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
        com.nhaarman.mockitokotlin2.verify(reloadAdapterTracksEventObserver).onChanged(null)
        com.nhaarman.mockitokotlin2.verify(downloadActionObserver, times(2)).onChanged(any()) // first time set during init
    }
}
