package com.audiomack.ui.slideupmenu.music

import android.app.Activity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.AddToPlaylistException
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleHighlightException
import com.audiomack.data.actions.ToggleHighlightResult
import com.audiomack.data.actions.ToggleRepostException
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.share.ShareManager
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryOffline
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventDownload
import com.audiomack.model.EventHighlightsUpdated
import com.audiomack.model.EventLoginState
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.PremiumLimitedDownloadAlertViewType
import com.audiomack.model.PremiumOnlyDownloadAlertViewType
import com.audiomack.model.ProgressHUDMode
import com.audiomack.model.ShareMethod
import com.audiomack.playback.SongAction
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.usecases.RefreshCommentCountUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
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
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import junit.framework.Assert.assertTrue
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class SlideUpMenuMusicViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var shareManager: ShareManager

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    @Mock
    private lateinit var queueDataSource: QueueDataSource

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var actionsDataSource: ActionsDataSource

    @Mock
    private lateinit var trackingDataSource: TrackingDataSource

    @Mock
    private lateinit var refreshCommentCountUseCase: RefreshCommentCountUseCase

    @Mock
    private lateinit var premiumDownloadDataSource: PremiumDownloadDataSource

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Mock
    private lateinit var eventBus: EventBus

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>
    private lateinit var favoriteSubject: BehaviorSubject<AMResultItem.ItemAPIStatus>
    private lateinit var repostSubject: BehaviorSubject<AMResultItem.ItemAPIStatus>
    private lateinit var commentsCountSubject: BehaviorSubject<Int>
    private lateinit var premiumObservable: Subject<Boolean>

    private lateinit var music: AMResultItem

    private val mixpanelSource = MixpanelSource("", "")

    private val removeFromQueueIndex = 0

    private lateinit var viewModel: SlideUpMenuMusicViewModel

    @Mock
    private lateinit var observerDismiss: Observer<Void>

    @Mock
    private lateinit var observerClose: Observer<Void>

    @Mock
    private lateinit var observerLoginRequired: Observer<LoginSignupSource>

    @Mock
    private lateinit var observerNotifyOffline: Observer<Void>

    @Mock
    private lateinit var observerCommentsCount: Observer<Int>

    @Mock
    private lateinit var observerShowHUD: Observer<ProgressHUDMode>

    @Mock
    private lateinit var observerReachedHighlightsLimit: Observer<Void>

    @Mock
    private lateinit var observerHighlightError: Observer<Void>

    @Mock
    private lateinit var observerHighlightSuccessEvent: Observer<String>

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
    private lateinit var observerViewState: Observer<SlideUpMenuMusicViewModel.ViewState>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        loginStateChangeSubject = BehaviorSubject.create()
        favoriteSubject = BehaviorSubject.create()
        repostSubject = BehaviorSubject.create()
        commentsCountSubject = BehaviorSubject.create()
        premiumObservable = PublishSubject.create()

        whenever(premiumDataSource.premiumObservable).thenReturn(premiumObservable)

        music = mock {
            on { isDownloadCompleted } doReturn false
            on { isDownloadInProgress } doReturn false
            on { itemId } doReturn "123"
            on { favoriteSubject } doReturn favoriteSubject
            on { repostSubject } doReturn repostSubject
            on { commentsCountSubject } doReturn commentsCountSubject
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Free
        }

        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)
        whenever(refreshCommentCountUseCase.refresh(any())).thenReturn(Completable.complete())

        viewModel = SlideUpMenuMusicViewModel(
            music,
            mixpanelSource,
            true,
            true,
            removeFromQueueIndex,
            shareManager,
            musicDataSource,
            queueDataSource,
            userDataSource,
            actionsDataSource,
            trackingDataSource,
            refreshCommentCountUseCase,
            TestSchedulersProvider(),
            premiumDownloadDataSource,
            premiumDataSource,
            eventBus
        ).apply {
            dismissEvent.observeForever(observerDismiss)
            closeEvent.observeForever(observerClose)
            loginRequiredEvent.observeForever(observerLoginRequired)
            notifyOfflineEvent.observeForever(observerNotifyOffline)
            commentsCount.observeForever(observerCommentsCount)
            showHUDEvent.observeForever(observerShowHUD)
            reachedHighlightsLimitEvent.observeForever(observerReachedHighlightsLimit)
            highlightErrorEvent.observeForever(observerHighlightError)
            highlightSuccessEvent.observeForever(observerHighlightSuccessEvent)
            showConfirmPlaylistSyncEvent.observeForever(observerShowConfirmPlaylistSyncEvent)
            showConfirmDownloadDeletionEvent.observeForever(observerShowConfirmDownloadDeletionEvent)
            showConfirmPlaylistDownloadDeletionEvent.observeForever(observerShowConfirmPlaylistDownloadDeletionEvent)
            premiumRequiredEvent.observeForever(observerPremiumRequiredEvent)
            showFailedPlaylistDownloadEvent.observeForever(observerShowFailedPlaylistDownloadEvent)
            showPremiumDownloadEvent.observeForever(observerShowPremiumDownloadEvent)
            downloadAction.observeForever(downloadActionObserver)
            showUnlockedToastEvent.observeForever(showUnlockedToastEventObserver)
            viewState.observeForever(observerViewState)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun initialSubscriptions() {
        verify(refreshCommentCountUseCase).refresh(any())
        assertTrue(music.commentsCountSubject.hasObservers())
        verify(observerViewState, times(1)).onChanged(any())
    }

    @Test
    fun cancel() {
        viewModel.onCancelTapped()
        verify(observerDismiss).onChanged(null)
    }

    @Test
    fun tapBackground() {
        viewModel.onBackgroundTapped()
        verify(observerDismiss).onChanged(null)
    }

    @Test
    fun musicInfo() {
        val observerMusicInfo: Observer<Void> = mock()
        viewModel.musicInfoEvent.observeForever(observerMusicInfo)
        viewModel.onMusicInfoTapped()
        verify(observerMusicInfo).onChanged(null)
        verify(observerClose).onChanged(null)
    }

    // Highlight

    @Test
    fun `add highlight, success`() {
        val title = "My song"
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.just(
            ToggleHighlightResult.Added(title)))

        viewModel.onHighlightTapped()

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD).onChanged(ProgressHUDMode.Dismiss)
        verify(observerViewState, times(2)).onChanged(any()) // Also happens on init
        verify(eventBus).post(argWhere { it is EventHighlightsUpdated })
        verify(observerHighlightSuccessEvent).onChanged(title)
    }

    @Test
    fun `remove highlight, success`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.just(
            ToggleHighlightResult.Removed))

        viewModel.onHighlightTapped()

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD).onChanged(ProgressHUDMode.Dismiss)
        verify(observerViewState, times(2)).onChanged(any()) // Also happens on init
        verify(eventBus).post(argWhere { it is EventHighlightsUpdated })
        verifyZeroInteractions(observerHighlightSuccessEvent)
    }

    @Test
    fun `toggle highlight, offline`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(
            ToggleHighlightException.Offline))

        viewModel.onHighlightTapped()

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD).onChanged(ProgressHUDMode.Dismiss)
        verify(observerNotifyOffline).onChanged(null)
    }

    @Test
    fun `toggle highlight, logged out`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(
            ToggleHighlightException.LoggedOut))

        viewModel.onHighlightTapped()

        verify(observerShowHUD).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD).onChanged(ProgressHUDMode.Dismiss)
        verify(observerLoginRequired).onChanged(LoginSignupSource.Highlight)

        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)

        verify(actionsDataSource, times(2)).toggleHighlight(any(), any(), any())
    }

    @Test
    fun `add highlight, reached limit`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(
            ToggleHighlightException.ReachedLimit))

        viewModel.onHighlightTapped()

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD).onChanged(ProgressHUDMode.Dismiss)
        verify(observerReachedHighlightsLimit).onChanged(null)
    }

    @Test
    fun `add highlight, error`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(
            ToggleHighlightException.Failure(true)))

        viewModel.onHighlightTapped()

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD).onChanged(ProgressHUDMode.Dismiss)
        verify(observerHighlightError).onChanged(null)
    }

    @Test
    fun `remove highlight, error`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(
            ToggleHighlightException.Failure(false)))

        viewModel.onHighlightTapped()

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD).onChanged(ProgressHUDMode.Dismiss)
        verify(observerHighlightError, never()).onChanged(null)
    }

    @Test
    fun `toggle highlight, generic error`() {
        whenever(actionsDataSource.toggleHighlight(any(), any(), any())).thenReturn(Observable.error(Exception("")))

        viewModel.onHighlightTapped()

        verify(actionsDataSource).toggleHighlight(any(), any(), any())
        verify(observerShowHUD).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD).onChanged(ProgressHUDMode.Dismiss)
        verify(observerShowHUD).onChanged(argWhere { it is ProgressHUDMode.Failure })
    }

    @Test
    fun artistInfo() {
        val observerArtistInfo: Observer<String?> = mock()
        viewModel.artistInfoEvent.observeForever(observerArtistInfo)
        viewModel.onArtistInfoTapped()
        verify(observerArtistInfo).onChanged(anyOrNull())
        verify(observerClose).onChanged(null)
    }

    @Test
    fun visible() {
        val observerStartAnimation: Observer<Void> = mock()
        viewModel.startAnimationEvent.observeForever(observerStartAnimation)
        viewModel.onVisible()
        verify(observerStartAnimation).onChanged(null)
    }

    @Test
    fun `add to playlist, logged in`() {
        whenever(actionsDataSource.addToPlaylist(anyOrNull())).thenReturn(Observable.just(true))
        val observerAddToPlaylist: Observer<Triple<List<AMResultItem>, MixpanelSource, String>> = mock()
        viewModel.addToPlaylistEvent.observeForever(observerAddToPlaylist)
        viewModel.onAddToPlaylistTapped()
        verify(observerAddToPlaylist).onChanged(any())
    }

    @Test
    fun `add to playlist, logged out then login`() {
        whenever(actionsDataSource.addToPlaylist(anyOrNull())).thenReturn(Observable.error(AddToPlaylistException.LoggedOut))
        val observerAddToPlaylist: Observer<Triple<List<AMResultItem>, MixpanelSource, String>> = mock()
        viewModel.addToPlaylistEvent.observeForever(observerAddToPlaylist)
        viewModel.onAddToPlaylistTapped()
        verifyNoMoreInteractions(observerAddToPlaylist)
        verify(observerLoginRequired).onChanged(eq(LoginSignupSource.AddToPlaylist))

        `when`(actionsDataSource.addToPlaylist(anyOrNull())).thenReturn(Observable.just(true))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)
        verify(observerAddToPlaylist).onChanged(any())
    }

    @Test
    fun `favorite, notify favorite event`() {
        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(
            ToggleFavoriteResult.Notify(true, true, true, true, true, "title", "artist")
        ))
        val observerNotifyFavorite: Observer<ToggleFavoriteResult.Notify> = mock()
        viewModel.notifyFavoriteEvent.observeForever(observerNotifyFavorite)
        viewModel.onFavoriteTapped()
        verify(observerNotifyFavorite).onChanged(any())
    }

    @Test
    fun `favorite, logged out then login`() {
        whenever(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(ToggleFavoriteException.LoggedOut))

        val observerNotifyFavorite: Observer<ToggleFavoriteResult.Notify> = mock()
        val observerLoggedOutAlert: Observer<LoginSignupSource> = mock()
        viewModel.notifyFavoriteEvent.observeForever(observerNotifyFavorite)
        viewModel.loginRequiredEvent.observeForever(observerLoggedOutAlert)

        viewModel.onFavoriteTapped()
        verifyNoMoreInteractions(observerNotifyFavorite)
        verify(observerLoggedOutAlert).onChanged(eq(LoginSignupSource.Favorite))

        `when`(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(
            ToggleFavoriteResult.Notify(true, true, true, true, true, "title", "artist")
        ))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)
        verify(observerNotifyFavorite).onChanged(any())
    }

    @Test
    fun `favorite, offline`() {
        whenever(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(ToggleFavoriteException.Offline))
        viewModel.onFavoriteTapped()
        verify(observerNotifyOffline).onChanged(null)
    }

    @Test
    fun `favorite, generic error`() {
        whenever(actionsDataSource.toggleFavorite(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onFavoriteTapped()
        verify(observerShowHUD, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
    }

    @Test
    fun `repost, notify repost event and triggers in app rating`() {
        `when`(actionsDataSource.toggleRepost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(
            ToggleRepostResult.Notify(true, true, "title", "artist")
        ))
        val observerNotifyRepost: Observer<ToggleRepostResult.Notify> = mock()
        viewModel.notifyRepostEvent.observeForever(observerNotifyRepost)
        viewModel.onRepostTapped()
        verify(observerNotifyRepost).onChanged(any())
    }

    @Test
    fun `repost, logged out then login`() {
        whenever(actionsDataSource.toggleRepost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(ToggleRepostException.LoggedOut))

        val observerNotifyRepost: Observer<ToggleRepostResult.Notify> = mock()
        val observerLoggedOutAlert: Observer<LoginSignupSource> = mock()
        viewModel.notifyRepostEvent.observeForever(observerNotifyRepost)
        viewModel.loginRequiredEvent.observeForever(observerLoggedOutAlert)

        viewModel.onRepostTapped()
        verifyNoMoreInteractions(observerNotifyRepost)
        verify(observerLoggedOutAlert).onChanged(eq(LoginSignupSource.Repost))

        `when`(actionsDataSource.toggleRepost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.just(
            ToggleRepostResult.Notify(true, true, "title", "artist")
        ))
        loginStateChangeSubject.onNext(EventLoginState.LOGGED_IN)
        verify(observerNotifyRepost).onChanged(any())
    }

    @Test
    fun `repost, offline`() {
        whenever(actionsDataSource.toggleRepost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(ToggleRepostException.Offline))
        viewModel.onRepostTapped()
        verify(observerNotifyOffline).onChanged(null)
    }

    @Test
    fun `repost, generic error`() {
        whenever(actionsDataSource.toggleRepost(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Observable.error(Exception("Unknown error for tests")))
        viewModel.onRepostTapped()
        verify(observerShowHUD, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
    }

    @Test
    fun copyLink() {
        val activity: Activity = mock()
        viewModel.onCopyLinkTapped(activity)
        verify(observerDismiss).onChanged(null)
        verify(shareManager).copyMusicLink(eq(activity), eq(music), eq(mixpanelSource), any())
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Copy Link") })
    }

    @Test
    fun twitter() {
        val activity: Activity = mock()
        viewModel.onShareViaTwitterTapped(activity, mock())
        verify(observerDismiss).onChanged(null)
        verify(shareManager).shareMusic(
            eq(activity),
            eq(music),
            eq(ShareMethod.Twitter),
            eq(mixpanelSource),
            any(),
            any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Twitter") })
    }

    @Test
    fun facebook() {
        val activity: Activity = mock()
        viewModel.onShareViaFacebookTapped(activity, mock())
        verify(observerDismiss).onChanged(null)
        verify(shareManager).shareStory(
                eq(activity),
                eq(music),
                anyOrNull(),
                eq(ShareMethod.Facebook),
                eq(mixpanelSource),
                any(),
                any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Facebook") })
    }

    @Test
    fun instagram() {
        val activity: Activity = mock()
        viewModel.onShareViaInstagramTapped(activity, mock())
        verify(observerDismiss).onChanged(null)
        verify(shareManager).shareStory(
                eq(activity),
                eq(music),
                anyOrNull(),
                eq(ShareMethod.Instagram),
                eq(mixpanelSource),
                any(),
                any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Instagram") })
    }

    @Test
    fun snapchat() {
        val activity: Activity = mock()
        viewModel.onShareViaSnapchatTapped(activity, mock())
        verify(observerDismiss).onChanged(null)
        verify(shareManager).shareStory(
                eq(activity),
                eq(music),
                anyOrNull(),
                eq(ShareMethod.Snapchat),
                eq(mixpanelSource),
                any(),
                any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Snapchat") })
    }

    @Test
    fun text() {
        val activity: Activity = mock()
        viewModel.onShareViaContactsTapped(activity, mock())
        verify(observerDismiss).onChanged(null)
        verify(shareManager).shareMusic(
            eq(activity),
            eq(music),
            eq(ShareMethod.SMS),
            eq(mixpanelSource),
            any(),
            any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Text") })
    }

    @Test
    fun more() {
        val activity: Activity = mock()
        viewModel.onShareViaOtherTapped(activity, mock())
        verify(observerDismiss).onChanged(null)
        verify(shareManager).shareMusic(
            eq(activity),
            eq(music),
            eq(ShareMethod.Standard),
            eq(mixpanelSource),
            any(),
            any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("App") })
    }

    @Test
    fun screenshot() {
        val activity: Activity = mock()
        viewModel.onShareScreenshotTapped(activity)
        verify(observerDismiss).onChanged(null)
        verify(shareManager).shareScreenshot(
                eq(activity),
                eq(music),
                anyOrNull(),
                eq(ShareMethod.Screenshot),
                anyOrNull(),
                eq(mixpanelSource),
                any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains("Screenshot") })
    }

    @Test
    fun `remove from queue`() {
        whenever(queueDataSource.index).thenReturn(1)
        viewModel.onRemoveFromQueueTapped()
        verify(queueDataSource).removeAt(eq(removeFromQueueIndex))
        verify(queueDataSource, never()).skip(any())
        verify(observerDismiss).onChanged(null)
    }

    @Test
    fun `remove from queue, currently playing song`() {
        whenever(queueDataSource.index).thenReturn(removeFromQueueIndex)
        viewModel.onRemoveFromQueueTapped()
        verify(queueDataSource).removeAt(eq(removeFromQueueIndex))
        verify(queueDataSource).skip(removeFromQueueIndex)
        verify(observerDismiss).onChanged(null)
    }

    @Test
    fun removeFromDownloadsSuccess() {
        `when`(musicDataSource.removeFromDownloads(anyOrNull())).thenReturn(Observable.just(true))
        viewModel.onRemoveFromDownloadsTapped()
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(observerDismiss).onChanged(null)
        verify(observerShowHUD, times(0)).onChanged(argWhere { it is ProgressHUDMode.Failure })
    }

    @Test
    fun removeFromDownloadsFailure() {
        `when`(musicDataSource.removeFromDownloads(anyOrNull())).thenReturn(Observable.error(Exception()))
        viewModel.onRemoveFromDownloadsTapped()
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verifyZeroInteractions(observerDismiss)
        verify(observerShowHUD, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
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
        verify(observerShowHUD).onChanged(ProgressHUDMode.Loading)
        verify(observerShowHUD).onChanged(ProgressHUDMode.Dismiss)
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
        verify(observerLoginRequired).onChanged(source)

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

    @Test
    fun `play next`() {
        val activity: Activity = mock()
        val disposables: CompositeDisposable = mock()

        viewModel.onPlayNextTapped(activity, disposables)
        verify(music).playNext(eq(activity), any(), any(), eq(disposables))
        verify(observerDismiss).onChanged(null)
    }

    @Test
    fun `play next brings up frozen premium-limited download modal`() {
        val activity: Activity = mock()
        val disposables: CompositeDisposable = mock()

        val frozenMusic = mock<AMResultItem> {
            on { isDownloadCompleted } doReturn false
            on { isDownloadInProgress } doReturn false
            on { itemId } doReturn "123"
            on { favoriteSubject } doReturn favoriteSubject
            on { repostSubject } doReturn repostSubject
            on { commentsCountSubject } doReturn commentsCountSubject
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
        }

        whenever(premiumDownloadDataSource.getFrozenCount(frozenMusic)).thenReturn(1)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).thenReturn(20)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).thenReturn(20)

        val viewModel = SlideUpMenuMusicViewModel(
            frozenMusic,
            MixpanelSource("", MixpanelPageMyLibraryOffline),
            true,
            true,
            removeFromQueueIndex,
            shareManager,
            musicDataSource,
            queueDataSource,
            userDataSource,
            actionsDataSource,
            trackingDataSource,
            refreshCommentCountUseCase,
            TestSchedulersProvider(),
            premiumDownloadDataSource,
            premiumDataSource,
            eventBus
        ).apply {
            dismissEvent.observeForever(observerDismiss)
            showPremiumDownloadEvent.observeForever(observerShowPremiumDownloadEvent)
        }

        viewModel.onPlayNextTapped(activity, disposables)
        verify(frozenMusic, times(0)).playNext(eq(activity), any(), any(), eq(disposables))
        verify(observerShowPremiumDownloadEvent).onChanged(argWhere { it.alertTypeLimited == PremiumLimitedDownloadAlertViewType.PlayFrozenOfflineWithAvailableUnfreezes })
        verifyZeroInteractions(observerDismiss)
    }

    @Test
    fun `play next brings up frozen premium-only download modal`() {
        val activity: Activity = mock()
        val disposables: CompositeDisposable = mock()

        val frozenMusic = mock<AMResultItem> {
            on { isDownloadCompleted } doReturn false
            on { isDownloadInProgress } doReturn false
            on { itemId } doReturn "123"
            on { favoriteSubject } doReturn favoriteSubject
            on { repostSubject } doReturn repostSubject
            on { commentsCountSubject } doReturn commentsCountSubject
            on { isDownloadFrozen } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Premium
        }

        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).thenReturn(5)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).thenReturn(20)

        val viewModel = SlideUpMenuMusicViewModel(
            frozenMusic,
            MixpanelSource("", MixpanelPageMyLibraryOffline),
            true,
            true,
            removeFromQueueIndex,
            shareManager,
            musicDataSource,
            queueDataSource,
            userDataSource,
            actionsDataSource,
            trackingDataSource,
            refreshCommentCountUseCase,
            TestSchedulersProvider(),
            premiumDownloadDataSource,
            premiumDataSource,
            eventBus
        ).apply {
            dismissEvent.observeForever(observerDismiss)
            showPremiumDownloadEvent.observeForever(observerShowPremiumDownloadEvent)
        }

        viewModel.onPlayNextTapped(activity, disposables)
        verify(frozenMusic, times(0)).playNext(eq(activity), any(), any(), eq(disposables))
        verify(observerShowPremiumDownloadEvent).onChanged(argWhere { it.alertTypePremium == PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline })
        verifyZeroInteractions(observerDismiss)
    }

    @Test
    fun `play later`() {
        val activity: Activity = mock()
        val disposables: CompositeDisposable = mock()

        viewModel.onPlayLaterTapped(activity, disposables)
        verify(music).playLater(eq(activity), any(), any(), eq(disposables))
        verify(observerDismiss).onChanged(null)
    }

    @Test
    fun `play later brings up frozen premium-limited download modal`() {
        val activity: Activity = mock()
        val disposables: CompositeDisposable = mock()

        val frozenMusic = mock<AMResultItem> {
            on { isDownloadCompleted } doReturn false
            on { isDownloadInProgress } doReturn false
            on { itemId } doReturn "123"
            on { favoriteSubject } doReturn favoriteSubject
            on { repostSubject } doReturn repostSubject
            on { commentsCountSubject } doReturn commentsCountSubject
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
        }

        whenever(premiumDownloadDataSource.getFrozenCount(frozenMusic)).thenReturn(1)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).thenReturn(25)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).thenReturn(20)

        val viewModel = SlideUpMenuMusicViewModel(
            frozenMusic,
            MixpanelSource("", MixpanelPageMyLibraryOffline),
            true,
            true,
            removeFromQueueIndex,
            shareManager,
            musicDataSource,
            queueDataSource,
            userDataSource,
            actionsDataSource,
            trackingDataSource,
            refreshCommentCountUseCase,
            TestSchedulersProvider(),
            premiumDownloadDataSource,
            premiumDataSource,
            eventBus
        ).apply {
            dismissEvent.observeForever(observerDismiss)
            showPremiumDownloadEvent.observeForever(observerShowPremiumDownloadEvent)
        }

        viewModel.onPlayLaterTapped(activity, disposables)
        verify(frozenMusic, times(0)).playLater(eq(activity), any(), any(), eq(disposables))
        verify(observerShowPremiumDownloadEvent).onChanged(argWhere { it.alertTypeLimited == PremiumLimitedDownloadAlertViewType.PlayFrozenOffline })
        verifyZeroInteractions(observerDismiss)
    }

    @Test
    fun `play later brings up frozen premium-only download modal`() {
        val activity: Activity = mock()
        val disposables: CompositeDisposable = mock()

        val frozenMusic = mock<AMResultItem> {
            on { isDownloadCompleted } doReturn false
            on { isDownloadInProgress } doReturn false
            on { itemId } doReturn "123"
            on { favoriteSubject } doReturn favoriteSubject
            on { repostSubject } doReturn repostSubject
            on { commentsCountSubject } doReturn commentsCountSubject
            on { isDownloadFrozen } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Premium
        }

        val viewModel = SlideUpMenuMusicViewModel(
            frozenMusic,
            MixpanelSource("", MixpanelPageMyLibraryOffline),
            true,
            true,
            removeFromQueueIndex,
            shareManager,
            musicDataSource,
            queueDataSource,
            userDataSource,
            actionsDataSource,
            trackingDataSource,
            refreshCommentCountUseCase,
            TestSchedulersProvider(),
            premiumDownloadDataSource,
            premiumDataSource,
            eventBus
        ).apply {
            dismissEvent.observeForever(observerDismiss)
            showPremiumDownloadEvent.observeForever(observerShowPremiumDownloadEvent)
        }

        viewModel.onPlayLaterTapped(activity, disposables)
        verify(frozenMusic, times(0)).playLater(eq(activity), any(), any(), eq(disposables))
        verify(observerShowPremiumDownloadEvent).onChanged(argWhere { it.alertTypePremium == PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline })
        verifyZeroInteractions(observerDismiss)
    }

    @Test
    fun `on eventbus EventDownload event`() {
        whenever(musicDataSource.getOfflineResource(music.itemId)).thenReturn(
            Observable.error(Exception("Test"))
        )
        viewModel.onMessageEvent(EventDownload(music.itemId, true))
        verify(observerViewState, times(2)).onChanged(any()) // Also happens on init
    }

    @Test
    fun `on comments click`() {
        val observer: Observer<AMResultItem> = mock()
        viewModel.openCommentsEvent.observeForever(observer)
        viewModel.onCommentsClick()
        verify(observer).onChanged(argWhere { it.itemId == music.itemId })
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `on comments count changed`() {
        val count = 3
        commentsCountSubject.onNext(count)
        verify(observerCommentsCount).onChanged(eq(count))
    }

    @Test
    fun whatsapp() {
        val method = ShareMethod.WhatsApp
        val activity: Activity = mock()
        viewModel.onShareViaWhatsAppTapped(activity)
        verify(observerDismiss).onChanged(null)
        verify(shareManager).shareLink(
            eq(activity),
            eq(music),
            anyOrNull(),
            eq(method),
            eq(mixpanelSource),
            any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains(method.stringValue()) })
    }

    @Test
    fun messenger() {
        val method = ShareMethod.Messenger
        val activity: Activity = mock()
        viewModel.onShareViaMessengerTapped(activity)
        verify(observerDismiss).onChanged(null)
        verify(shareManager).shareLink(
            eq(activity),
            eq(music),
            anyOrNull(),
            eq(method),
            eq(mixpanelSource),
            any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains(method.stringValue()) })
    }

    @Test
    fun wechat() {
        val method = ShareMethod.WeChat
        val activity: Activity = mock()
        viewModel.onShareViaWeChatTapped(activity)
        verify(observerDismiss).onChanged(null)
        verify(shareManager).shareLink(
            eq(activity),
            eq(music),
            anyOrNull(),
            eq(method),
            eq(mixpanelSource),
            any()
        )
        verify(trackingDataSource).trackScreen(argWhere { it.contains(method.stringValue()) })
    }

    // Premium status changes

    @Test
    fun `download action observed when changed`() {
        premiumObservable.onNext(true)
        verify(downloadActionObserver, times(2)).onChanged(any()) // first time set during init
    }
}
