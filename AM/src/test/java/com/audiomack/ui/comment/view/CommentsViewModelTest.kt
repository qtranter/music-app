package com.audiomack.ui.comment.view

import android.app.Activity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.comment.CommentDataSource
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.share.ShareManager
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMComment
import com.audiomack.model.AMCommentVote
import com.audiomack.model.AMCommenter
import com.audiomack.model.AMCommentsResponse
import com.audiomack.model.AMExpandComment
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMShowMoreComments
import com.audiomack.model.CommentMethod
import com.audiomack.model.CommentSort
import com.audiomack.model.EventCommentCountUpdated
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.comments.view.CommentsFragment
import com.audiomack.ui.comments.view.CommentsViewModel
import com.audiomack.ui.common.Resource
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibility
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibilityData
import com.audiomack.ui.player.maxi.bottom.playerTabCommentsIndex
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class CommentsViewModelTest {

    @Mock
    private lateinit var playerDataSource: PlayerDataSource

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var comments: ArrayList<AMComment>

    @Mock
    private lateinit var commentDataSource: CommentDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var adsDataSource: AdsDataSource

    @Mock
    private lateinit var shareManager: ShareManager

    @Mock
    private lateinit var imageLoader: ImageLoader

    @Mock
    private lateinit var eventBus: EventBus

    @Mock
    private lateinit var playerBottomVisibility: PlayerBottomVisibility

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModelStandalone: CommentsViewModel

    private lateinit var viewModelPlayer: CommentsViewModel

    private lateinit var viewModelSingle: CommentsViewModel

    private lateinit var songSubscriber: io.reactivex.Observer<Resource<AMResultItem>>

    private lateinit var visibilitySubscriber: io.reactivex.Observer<PlayerBottomVisibilityData>

    private val mixpanelSource = MixpanelSource("", "")

    private lateinit var loginStateChangeSubject: Subject<EventLoginState>

    private val fakeCommenter = mock<AMCommenter> {
        on { urlSlug } doReturn "matteinn"
    }

    private val fakeComment = mock<AMComment> {
        on { entityKind } doReturn "dunno"
        on { entityId } doReturn "123"
        on { threadUuid } doReturn "456"
        on { uuid } doReturn "789"
        on { commenter } doReturn fakeCommenter
    }

    private val fakeEntity = mock<AMResultItem> {
        on { type } doReturn "song"
        on { itemId } doReturn "123"
        on { typeForHighlightingAPI } doReturn "song"
    }

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        schedulersProvider = TestSchedulersProvider()

        `when`(commentDataSource.getComments(any(), any(), any(), any(), any())).thenReturn(Single.just(AMCommentsResponse()))

        `when`(playerDataSource.subscribeToSong(any())).then {
            songSubscriber = (it.arguments.first() as io.reactivex.Observer<Resource<AMResultItem>>)
            Unit
        }

        `when`(playerBottomVisibility.subscribe(any())).then {
            visibilitySubscriber = (it.arguments.first() as io.reactivex.Observer<PlayerBottomVisibilityData>)
            Unit
        }

        loginStateChangeSubject = BehaviorSubject.create()
        whenever(userDataSource.loginEvents).thenReturn(loginStateChangeSubject)

        viewModelStandalone = CommentsViewModel(
            CommentsFragment.Mode.Standalone,
            mixpanelSource,
            playerDataSource,
            userDataSource,
            commentDataSource,
            mixpanelDataSource,
            adsDataSource,
            imageLoader,
            schedulersProvider,
            eventBus,
            playerBottomVisibility,
            shareManager
        ).also {
            it.updateEntity(fakeEntity)
        }

        viewModelPlayer = CommentsViewModel(
            CommentsFragment.Mode.Player,
            mixpanelSource,
            playerDataSource,
            userDataSource,
            commentDataSource,
            mixpanelDataSource,
            adsDataSource,
            imageLoader,
            schedulersProvider,
            eventBus,
            playerBottomVisibility,
            shareManager
        ).also {
            it.updateEntity(fakeEntity)
        }

        viewModelSingle = CommentsViewModel(
            CommentsFragment.Mode.Single,
            mixpanelSource,
            playerDataSource,
            userDataSource,
            commentDataSource,
            mixpanelDataSource,
            adsDataSource,
            imageLoader,
            schedulersProvider,
            eventBus,
            playerBottomVisibility,
            shareManager
        ).also {
            it.updateEntity(fakeEntity)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observer: Observer<Void> = mock()
        viewModelStandalone.closeEvent.observeForever(observer)
        viewModelStandalone.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `update comment count`() {
        val count = 0
        val observer: Observer<Int> = mock()
        viewModelStandalone.commentCount.observeForever(observer)
        viewModelStandalone.updateCommentCount(count)
        verify(observer).onChanged(count)
        verify(eventBus).post(argWhere { it is EventCommentCountUpdated && it.count == count })
    }

    @Test
    fun `upadte comment list`() {
        val observer: Observer<Pair<ArrayList<AMComment>, String?>> = mock()
        viewModelStandalone.updateCommentListEvent.observeForever(observer)
        viewModelStandalone.updateCommentList(comments)
        verify(observer).onChanged(argWhere { it.first == comments })
    }

    @Test
    fun `start login`() {
        val observer: Observer<LoginSignupSource> = mock()
        viewModelStandalone.showLoggedInEvent.observeForever(observer)
        viewModelStandalone.onStartLoginTapped()
        verify(observer).onChanged(eq(LoginSignupSource.Comment))
    }

    @Test
    fun `show sort view`() {
        val observer: Observer<CommentSort> = mock()
        viewModelStandalone.showSortViewEvent.observeForever(observer)
        viewModelStandalone.onSortButtonTapped()
        verify(observer).onChanged(any())
    }

    @Test
    fun `comment expand tapped`() {
        val comment = AMComment()
        val expandComment = AMExpandComment(mock(), mock(), comment)
        val observer: Observer<AMExpandComment> = mock()
        viewModelStandalone.expandCommentEvent.observeForever(observer)
        viewModelStandalone.onCommentExpandTapped(expandComment)
        verify(observer).onChanged(any())
    }

    @Test
    fun `comment action tapped`() {
        val comment = AMComment()
        val observer: Observer<AMComment> = mock()
        viewModelStandalone.showOptionsEvent.observeForever(observer)
        viewModelStandalone.onCommentActionTapped(comment)
        verify(observer).onChanged(any())
    }

    @Test
    fun `reply action tapped`() {
        val comment = AMComment()
        val observer: Observer<AMComment> = mock()
        viewModelStandalone.showOptionsEvent.observeForever(observer)
        viewModelStandalone.onReplyActionTapped(comment)
        verify(observer).onChanged(any())
    }

    @Test
    fun `change sorting`() {
        val observerCloseOptions: Observer<Void> = mock()
        val observerShowLoading: Observer<Void> = mock()
        viewModelStandalone.closeOptionsEvent.observeForever(observerCloseOptions)
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.onChangedSorting(CommentSort.Top)
        verify(observerCloseOptions).onChanged(null)
        verify(observerShowLoading).onChanged(null)
    }

    @Test
    fun `show add comment view, logged out`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)
        val observerAddCommentView: Observer<AMResultItem> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showAddCommentEvent.observeForever(observerAddCommentView)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.onWriteCommentTapped()
        verifyZeroInteractions(observerAddCommentView)
        verify(observerShowLoginAlertEvent).onChanged(null)
    }

    @Test
    fun `show add comment view, logged in`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        val observerAddCommentView: Observer<AMResultItem> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showAddCommentEvent.observeForever(observerAddCommentView)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.onWriteCommentTapped()
        verify(observerAddCommentView).onChanged(anyOrNull())
        verifyZeroInteractions(observerShowLoginAlertEvent)
    }

    @Test
    fun `show delete alert view, logged out`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)
        val observerCloseOptions: Observer<Void> = mock()
        val observeShowDeleteAlertView: Observer<AMComment> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.closeOptionsEvent.observeForever(observerCloseOptions)
        viewModelStandalone.showDeleteAlertViewEvent.observeForever(observeShowDeleteAlertView)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.showDeleteAlertView(fakeComment)
        verify(observerCloseOptions).onChanged(null)
        verifyZeroInteractions(observeShowDeleteAlertView)
        verify(observerShowLoginAlertEvent).onChanged(null)
    }

    @Test
    fun `show delete alert view, logged in`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        val observerCloseOptions: Observer<Void> = mock()
        val observeShowDeleteAlertView: Observer<AMComment> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.closeOptionsEvent.observeForever(observerCloseOptions)
        viewModelStandalone.showDeleteAlertViewEvent.observeForever(observeShowDeleteAlertView)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.showDeleteAlertView(fakeComment)
        verify(observerCloseOptions).onChanged(null)
        verify(observeShowDeleteAlertView).onChanged(eq(fakeComment))
        verifyZeroInteractions(observerShowLoginAlertEvent)
    }

    @Test
    fun `show report alert view, logged out`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)
        val observerCloseOptions: Observer<Void> = mock()
        val observeShowReportAlertView: Observer<AMComment> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.closeOptionsEvent.observeForever(observerCloseOptions)
        viewModelStandalone.showReportAlertViewEvent.observeForever(observeShowReportAlertView)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.showReportAlertView(fakeComment)
        verify(observerCloseOptions).onChanged(null)
        verifyZeroInteractions(observeShowReportAlertView)
        verify(observerShowLoginAlertEvent).onChanged(null)
    }

    @Test
    fun `show report alert view, logged in`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        val observerCloseOptions: Observer<Void> = mock()
        val observeShowReportAlertView: Observer<AMComment> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.closeOptionsEvent.observeForever(observerCloseOptions)
        viewModelStandalone.showReportAlertViewEvent.observeForever(observeShowReportAlertView)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.showReportAlertView(fakeComment)
        verify(observerCloseOptions).onChanged(null)
        verify(observeShowReportAlertView).onChanged(eq(fakeComment))
        verifyZeroInteractions(observerShowLoginAlertEvent)
    }

    @Test
    fun `reply upvote tapped, not upvoted, logged in`() {
        val reply = mock<AMComment> {
            on { upVoted } doReturn false
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "456"
            on { threadUuid } doReturn "123"
        }
        val comment = mock<AMComment> {
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "123"
            on { children } doReturn arrayListOf(reply)
        }
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(commentDataSource.voteComment(any(), any(), any(), any())).thenReturn(Single.just(AMCommentVote()))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.updateCommentList(arrayListOf(comment))
        viewModelStandalone.onReplyUpVoteTapped(comment, reply)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verifyZeroInteractions(observerShowLoginAlertEvent)
        verify(mixpanelDataSource).trackCommentDetail(eq(CommentMethod.UpVote), any(), any())
        verify(reply).upVoted = true
    }

    @Test
    fun `reply upvote tapped, upvoted, logged in`() {
        val reply = mock<AMComment> {
            on { upVoted } doReturn true
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "456"
            on { threadUuid } doReturn "123"
        }
        val comment = mock<AMComment> {
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "123"
            on { children } doReturn arrayListOf(reply)
        }
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(commentDataSource.voteComment(any(), any(), any(), any())).thenReturn(Single.just(AMCommentVote()))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.updateCommentList(arrayListOf(comment))
        viewModelStandalone.onReplyUpVoteTapped(comment, reply)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verifyZeroInteractions(observerShowLoginAlertEvent)
        verify(mixpanelDataSource).trackCommentDetail(eq(CommentMethod.UpVote), any(), any())
        verify(reply).upVoted = false
    }

    @Test
    fun `reply upvote tapped, logged out`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.updateCommentList(arrayListOf(fakeComment))
        viewModelStandalone.onReplyUpVoteTapped(fakeComment, fakeComment)
        verifyZeroInteractions(observerShowLoading)
        verifyZeroInteractions(observerHideLoading)
        verify(observerShowLoginAlertEvent).onChanged(null)
    }

    @Test
    fun `reply downvote tapped, not downvoted, logged in`() {
        val reply = mock<AMComment> {
            on { downVoted } doReturn false
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "456"
            on { threadUuid } doReturn "123"
        }
        val comment = mock<AMComment> {
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "123"
            on { children } doReturn arrayListOf(reply)
        }
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(commentDataSource.voteComment(any(), any(), any(), any())).thenReturn(Single.just(AMCommentVote()))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.updateCommentList(arrayListOf(comment))
        viewModelStandalone.onReplyDownVoteTapped(comment, reply)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verifyZeroInteractions(observerShowLoginAlertEvent)
        verify(mixpanelDataSource).trackCommentDetail(eq(CommentMethod.DownVote), any(), any())
        verify(reply).downVoted = true
    }

    @Test
    fun `reply downvote tapped, downvoted, logged in`() {
        val reply = mock<AMComment> {
            on { downVoted } doReturn true
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "456"
            on { threadUuid } doReturn "123"
        }
        val comment = mock<AMComment> {
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "123"
            on { children } doReturn arrayListOf(reply)
        }
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(commentDataSource.voteComment(any(), any(), any(), any())).thenReturn(Single.just(AMCommentVote()))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.updateCommentList(arrayListOf(comment))
        viewModelStandalone.onReplyDownVoteTapped(comment, reply)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verifyZeroInteractions(observerShowLoginAlertEvent)
        verify(mixpanelDataSource).trackCommentDetail(eq(CommentMethod.DownVote), any(), any())
        verify(reply).downVoted = false
    }

    @Test
    fun `reply downvote tapped, logged out`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.updateCommentList(arrayListOf(fakeComment))
        viewModelStandalone.onReplyDownVoteTapped(fakeComment, fakeComment)
        verifyZeroInteractions(observerShowLoading)
        verifyZeroInteractions(observerHideLoading)
        verify(observerShowLoginAlertEvent).onChanged(null)
    }

    @Test
    fun `comment upvote tapped, logged out`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.updateCommentList(arrayListOf(fakeComment))
        viewModelStandalone.onCommentUpVoteTapped(fakeComment)
        verifyZeroInteractions(observerShowLoading)
        verifyZeroInteractions(observerHideLoading)
        verify(observerShowLoginAlertEvent).onChanged(null)
    }

    @Test
    fun `comment upvote tapped, not upvoted, logged in`() {
        val comment = mock<AMComment> {
            on { upVoted } doReturn false
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "123"
        }
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(commentDataSource.voteComment(any(), any(), any(), any())).thenReturn(Single.just(AMCommentVote()))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.updateCommentList(arrayListOf(comment))
        viewModelStandalone.onCommentUpVoteTapped(comment)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verifyZeroInteractions(observerShowLoginAlertEvent)
        verify(mixpanelDataSource).trackCommentDetail(eq(CommentMethod.UpVote), any(), any())
        verify(comment).upVoted = true
    }

    @Test
    fun `comment upvote tapped, upvoted, logged in`() {
        val comment = mock<AMComment> {
            on { upVoted } doReturn true
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "123"
        }
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(commentDataSource.voteComment(any(), any(), any(), any())).thenReturn(Single.just(AMCommentVote()))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.updateCommentList(arrayListOf(comment))
        viewModelStandalone.onCommentUpVoteTapped(comment)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verifyZeroInteractions(observerShowLoginAlertEvent)
        verify(mixpanelDataSource).trackCommentDetail(eq(CommentMethod.UpVote), any(), any())
        verify(comment).upVoted = false
    }

    @Test
    fun `comment downvote tapped, not downvoted, logged in`() {
        val comment = mock<AMComment> {
            on { downVoted } doReturn false
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "123"
        }
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(commentDataSource.voteComment(any(), any(), any(), any())).thenReturn(Single.just(AMCommentVote()))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.updateCommentList(arrayListOf(comment))
        viewModelStandalone.onCommentDownVoteTapped(comment)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verifyZeroInteractions(observerShowLoginAlertEvent)
        verify(mixpanelDataSource).trackCommentDetail(eq(CommentMethod.DownVote), any(), any())
        verify(comment).downVoted = true
    }

    @Test
    fun `comment downvote tapped, downvoted, logged in`() {
        val comment = mock<AMComment> {
            on { downVoted } doReturn true
            on { entityKind } doReturn "dunno"
            on { entityId } doReturn "123"
        }
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        `when`(commentDataSource.voteComment(any(), any(), any(), any())).thenReturn(Single.just(AMCommentVote()))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowLoginAlertEvent: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlertEvent)
        viewModelStandalone.updateCommentList(arrayListOf(comment))
        viewModelStandalone.onCommentDownVoteTapped(comment)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verifyZeroInteractions(observerShowLoginAlertEvent)
        verify(mixpanelDataSource).trackCommentDetail(eq(CommentMethod.DownVote), any(), any())
        verify(comment).downVoted = false
    }

    @Test
    fun `comment delete tapped, sucess`() {
        `when`(commentDataSource.deleteComment(any(), any(), any(), any())).thenReturn(Single.just(true))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.updateCommentList(arrayListOf(fakeComment))
        viewModelStandalone.onCommentDeleteTapped(fakeComment)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
    }

    @Test
    fun `comment delete tapped, failure`() {
        `when`(commentDataSource.deleteComment(any(), any(), any(), any())).thenReturn(Single.error(Exception("Unknown error for tests")))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowErrorMessage: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showErrorToastEvent.observeForever(observerShowErrorMessage)
        viewModelStandalone.updateCommentList(arrayListOf(fakeComment))
        viewModelStandalone.onCommentDeleteTapped(fakeComment)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verify(observerShowErrorMessage).onChanged(null)
    }

    @Test
    fun `comment report tapped, sucess`() {
        `when`(commentDataSource.reportComment(any(), any(), any(), any())).thenReturn(Single.just(true))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.updateCommentList(arrayListOf(fakeComment))
        viewModelStandalone.onCommentReportTapped(fakeComment)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verify(mixpanelDataSource).trackCommentDetail(eq(CommentMethod.Report), any(), any())
    }

    @Test
    fun `comment report tapped, failure`() {
        `when`(commentDataSource.reportComment(any(), any(), any(), any())).thenReturn(Single.error(Exception("Unknown error for tests")))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowErrorMessage: Observer<Void> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showErrorToastEvent.observeForever(observerShowErrorMessage)
        viewModelStandalone.updateCommentList(arrayListOf(fakeComment))
        viewModelStandalone.onCommentReportTapped(fakeComment)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verify(observerShowErrorMessage).onChanged(null)
    }

    @Test
    fun `on view more comments tapped`() {
        val observerShowMoreComments: Observer<AMShowMoreComments> = mock()
        viewModelStandalone.showMoreCommentsEvent.observeForever(observerShowMoreComments)
        viewModelStandalone.onCommentViewMoreTapped(AMShowMoreComments(mock(), "matteinn", mock(), mock(), mock()))
        verify(observerShowMoreComments).onChanged(any())
    }

    @Test
    fun `on commenter tapped`() {
        val observerShowCommenter: Observer<String> = mock()
        viewModelStandalone.showCommenterEvent.observeForever(observerShowCommenter)
        viewModelStandalone.onCommenterTapped(fakeComment)
        verify(observerShowCommenter).onChanged(eq("matteinn"))
    }

    @Test
    fun `on comment reply tapped, logged in`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        val observerShowAddReply: Observer<Pair<AMResultItem, String>> = mock()
        viewModelStandalone.showAddReplyEvent.observeForever(observerShowAddReply)
        viewModelStandalone.onCommentReplyTapped(fakeComment)
        verify(observerShowAddReply).onChanged(any())
    }

    @Test
    fun `on comment reply tapped, logged out`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(false)
        val observerShowAddReply: Observer<Pair<AMResultItem, String>> = mock()
        val observerShowLoginAlert: Observer<Void> = mock()
        viewModelStandalone.showAddReplyEvent.observeForever(observerShowAddReply)
        viewModelStandalone.showLoginAlertEvent.observeForever(observerShowLoginAlert)
        viewModelStandalone.onCommentReplyTapped(fakeComment)
        verifyZeroInteractions(observerShowAddReply)
        verify(observerShowLoginAlert).onChanged(null)
    }

    @Test
    fun `on create`() {
        val observerUpdateAvatar: Observer<String?> = mock()
        viewModelStandalone.avatar.observeForever(observerUpdateAvatar)
        viewModelStandalone.onCreate()
        verify(observerUpdateAvatar, atLeast(1)).onChanged(anyOrNull())
    }

    @Test
    fun `failure on get comments`() {
        `when`(commentDataSource.getComments(any(), any(), any(), any(), any())).thenReturn(Single.error(Exception("Error")))
        viewModelStandalone.onCreate()
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerLoadErrorToast: Observer<Void> = mock()
        val observerNoConnectionVisible: Observer<Boolean> = mock()
        val observerNoDataVisible: Observer<Boolean> = mock()
        val observerStopInfiniteScroll: Observer<Void> = mock()
        val observerPlayerHeader: Observer<Boolean> = mock()
        val observerStandaloneHeader: Observer<Boolean> = mock()
        viewModelStandalone.showLoadingEvent.observeForever(observerShowLoading)
        viewModelStandalone.hideLoadingEvent.observeForever(observerHideLoading)
        viewModelStandalone.showLoadErrorToastEvent.observeForever(observerLoadErrorToast)
        viewModelStandalone.noConnectionPlaceholderVisible.observeForever(observerNoConnectionVisible)
        viewModelStandalone.noDataPlaceholderVisible.observeForever(observerNoDataVisible)
        viewModelStandalone.stopInfiniteScrollEvent.observeForever(observerStopInfiniteScroll)
        viewModelStandalone.playerHeaderVisible.observeForever(observerPlayerHeader)
        viewModelStandalone.standaloneHeaderVisible.observeForever(observerStandaloneHeader)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verify(observerLoadErrorToast).onChanged(anyOrNull())
        verify(observerNoConnectionVisible).onChanged(eq(false))
        verify(observerNoDataVisible).onChanged(eq(true))
        verify(observerStopInfiniteScroll).onChanged(null)
        verify(observerPlayerHeader).onChanged(false)
        verify(observerStandaloneHeader, times(0)).onChanged(false)
    }

    @Test
    fun `player mode - player datasource subscription`() {
        verify(playerDataSource).subscribeToSong(any())
    }

    @Test
    fun `player mode - toggle nested scroll`() {
        val reachedBottom = true
        visibilitySubscriber.onNext(PlayerBottomVisibilityData(0, reachedBottom))

        val observer: Observer<Boolean> = mock()
        viewModelPlayer.scrollViewNestedScrollEnabled.observeForever(observer)
        verify(observer).onChanged(eq(reachedBottom))
    }

    @Test
    fun `player mode - visible, player datasource success`() {
        `when`(playerBottomVisibility.tabIndex).thenReturn(playerTabCommentsIndex)
        `when`(playerBottomVisibility.tabsVisible).thenReturn(true)
        songSubscriber.onNext(Resource.Success(mock {
            on { typeForHighlightingAPI } doReturn "song"
            on { itemId } doReturn "1"
        }))
        val observerShowLoading: Observer<Void> = mock()
        val observerNoConnectionVisible: Observer<Boolean> = mock()
        val observerNoDataVisible: Observer<Boolean> = mock()
        viewModelPlayer.showLoadingEvent.observeForever(observerShowLoading)
        viewModelPlayer.noConnectionPlaceholderVisible.observeForever(observerNoConnectionVisible)
        viewModelPlayer.noDataPlaceholderVisible.observeForever(observerNoDataVisible)
        verify(commentDataSource).getComments(any(), any(), any(), any(), any())
        verify(observerShowLoading).onChanged(null)
        verify(observerNoConnectionVisible).onChanged(eq(false))
        verify(observerNoDataVisible).onChanged(eq(true))
    }

    @Test
    fun `player mode - visible, player datasource failure`() {
        `when`(playerBottomVisibility.tabIndex).thenReturn(playerTabCommentsIndex)
        `when`(playerBottomVisibility.tabsVisible).thenReturn(true)
        songSubscriber.onNext(Resource.Failure(mock()))
        val observerNoConnectionVisible: Observer<Boolean> = mock()
        viewModelPlayer.noConnectionPlaceholderVisible.observeForever(observerNoConnectionVisible)
        verify(observerNoConnectionVisible, times(1)).onChanged(eq(true))
    }

    @Test
    fun `player mode - visible, player datasource loading`() {
        `when`(playerBottomVisibility.tabIndex).thenReturn(playerTabCommentsIndex)
        `when`(playerBottomVisibility.tabsVisible).thenReturn(true)
        songSubscriber.onNext(Resource.Loading(mock()))
        val observerShowLoading: Observer<Void> = mock()
        viewModelPlayer.showLoadingEvent.observeForever(observerShowLoading)
        verify(observerShowLoading, times(1)).onChanged(null)
    }

    @Test
    fun `player mode - not visible`() {
        `when`(playerBottomVisibility.tabIndex).thenReturn(playerTabCommentsIndex)
        `when`(playerBottomVisibility.tabsVisible).thenReturn(false)
        songSubscriber.onNext(Resource.Success(mock()))
        val observerShowLoading: Observer<Void> = mock()
        viewModelPlayer.showLoadingEvent.observeForever(observerShowLoading)
        verifyZeroInteractions(observerShowLoading)
    }

    @Test
    fun `on cancel login tapped`() {
        viewModelStandalone.onCancelLoginTapped()
        verify(userDataSource).onLoginCanceled()
    }

    @Test
    fun `on share comment tapped`() {
        val activity: Activity = mock()
        viewModelStandalone.onShareCommentTapped(activity, fakeComment)
        verify(shareManager).shareCommentLink(eq(activity), eq(fakeComment), eq(fakeEntity), eq(mixpanelSource), any())
    }

    @Test
    fun `update title for single view`() {
        val pairData = Pair(first = fakeEntity.artist, second = fakeEntity.title)
        val observer: Observer<Pair<String?, String?>> = mock()
        viewModelSingle.updateTitleEvent.observeForever(observer)
        viewModelSingle.updateSingleComment(fakeComment)
        verify(observer).onChanged(pairData)
    }

    @Test
    fun `show all comments for single view`() {
        val observer: Observer<Void> = mock()
        viewModelSingle.showViewAllEvent.observeForever(observer)
        viewModelSingle.onViewAllTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `single mode - visible`() {
        val observerSingleCommentVisible: Observer<Boolean> = mock()
        viewModelSingle.singleCommentModeVisible.observeForever(observerSingleCommentVisible)
        verify(observerSingleCommentVisible).onChanged(true)
    }

    @Test
    fun `single mode - not visible`() {
        val observerSingleCommentVisible: Observer<Boolean> = mock()
        viewModelStandalone.singleCommentModeVisible.observeForever(observerSingleCommentVisible)
        verify(observerSingleCommentVisible).onChanged(false)
    }

    @Test
    fun `title click observed, view all in single mode`() {
        val observer: Observer<Void> = mock()
        viewModelSingle.showViewAllEvent.observeForever(observer)
        viewModelSingle.onTitleClicked()
        verify(observer).onChanged(null)
    }

    @Test
    fun `title click observed, view all not called in player mode`() {
        val observer: Observer<Void> = mock()
        viewModelPlayer.showViewAllEvent.observeForever(observer)
        viewModelPlayer.onTitleClicked()
        verifyZeroInteractions(observer)
    }

    @Test
    fun `title click observed, view all not called in standalone mode`() {
        val observer: Observer<Void> = mock()
        viewModelStandalone.showViewAllEvent.observeForever(observer)
        viewModelStandalone.onTitleClicked()
        verifyZeroInteractions(observer)
    }
}
