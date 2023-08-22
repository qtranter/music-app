package com.audiomack.ui.comment.add

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.comment.CommentDataSource
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventCommentIntroDismissed
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.comments.add.AddCommentViewModel
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import io.reactivex.Single
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class AddCommentViewModelTest {

    private val fakeEntity = mock<AMResultItem> {
        on { itemId } doReturn "123"
        on { type } doReturn "song"
        on { typeForHighlightingAPI } doReturn "song"
    }

    private val threadId: String = ""

    @Mock
    private lateinit var commentDataSource: CommentDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var preferencesDataSource: PreferencesDataSource

    @Mock
    private lateinit var eventBus: EventBus

    @Mock
    private val imageLoader: ImageLoader = mock()

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: AddCommentViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        `when`(preferencesDataSource.needToShowCommentTooltip).thenReturn(true)
        viewModel = AddCommentViewModel(
            fakeEntity,
            threadId,
            commentDataSource,
            mixpanelDataSource,
            userDataSource,
            imageLoader,
            preferencesDataSource,
            eventBus,
            schedulersProvider
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun onSendTapped() {
        val observer: Observer<Void> = mock()
        viewModel.buttonSendEvent.observeForever(observer)
        viewModel.onSendTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `send with null string`() {
        val query = null
        val observerHideKeyboard: Observer<Void> = mock()
        viewModel.hideKeyboardEvent.observeForever(observerHideKeyboard)
        viewModel.buttonSendTapped(query)
        verify(observerHideKeyboard).onChanged(null)
    }

    @Test
    fun `send with empty string`() {
        val query = ""
        val observerHideKeyboard: Observer<Void> = mock()
        viewModel.hideKeyboardEvent.observeForever(observerHideKeyboard)
        viewModel.buttonSendTapped(query)
        verify(observerHideKeyboard).onChanged(null)
    }

    @Test
    fun `send with valid string, user cannot comment`() {
        `when`(userDataSource.canComment()).thenReturn(false)
        val query = "send help"
        val observerHideKeyboard: Observer<Void> = mock()
        viewModel.hideKeyboardEvent.observeForever(observerHideKeyboard)
        viewModel.buttonSendTapped(query)
        verify(observerHideKeyboard).onChanged(null)
    }

    @Test
    fun `send with valid string, user can comment, successful api call`() {
        `when`(commentDataSource.postComment(any(), any(), any(), any())).thenReturn(Single.just(mock()))
        `when`(userDataSource.canComment()).thenReturn(true)
        val query = "send help"
        val observerClose: Observer<Void> = mock()
        val observerHideKeyboard: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowErrorMessage: Observer<Void> = mock()
        viewModel.close.observeForever(observerClose)
        viewModel.hideKeyboardEvent.observeForever(observerHideKeyboard)
        viewModel.hideLoadingEvent.observeForever(observerHideLoading)
        viewModel.showErrorMessageEvent.observeForever(observerShowErrorMessage)
        viewModel.buttonSendTapped(query)
        verify(observerClose).onChanged(null)
        verify(observerHideKeyboard).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verify(mixpanelDataSource).trackAddComment(anyOrNull(), anyOrNull())
        verifyZeroInteractions(observerShowErrorMessage)
    }

    @Test
    fun `send with valid string, user can comment, failed api call`() {
        `when`(commentDataSource.postComment(any(), any(), any(), any())).thenReturn(Single.error(Exception("unknown error for tests")))
        `when`(userDataSource.canComment()).thenReturn(true)
        val query = "send help"
        val observerClose: Observer<Void> = mock()
        val observerHideKeyboard: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowErrorMessage: Observer<Void> = mock()
        viewModel.close.observeForever(observerClose)
        viewModel.hideKeyboardEvent.observeForever(observerHideKeyboard)
        viewModel.hideLoadingEvent.observeForever(observerHideLoading)
        viewModel.showErrorMessageEvent.observeForever(observerShowErrorMessage)
        viewModel.buttonSendTapped(query)
        verifyZeroInteractions(observerClose)
        verifyZeroInteractions(observerHideKeyboard)
        verify(observerHideLoading).onChanged(null)
        verify(observerShowErrorMessage).onChanged(null)
    }

    @Test
    fun `on init, need to show intro`() {
        val observerShowCommentIntro: Observer<Void> = mock()
        val observerShowKeyboard: Observer<Void> = mock()
        val observerUpdateAvatar: Observer<String?> = mock()
        val observerSongname: Observer<String> = mock()
        viewModel.showCommentIntroEvent.observeForever(observerShowCommentIntro)
        viewModel.showKeyboardEvent.observeForever(observerShowKeyboard)
        viewModel.avatar.observeForever(observerUpdateAvatar)
        viewModel.songName.observeForever(observerSongname)
        verify(observerShowCommentIntro).onChanged(null)
        verifyZeroInteractions(observerShowKeyboard)
        verify(observerUpdateAvatar).onChanged(anyOrNull())
        verify(observerSongname).onChanged(anyOrNull())
        verify(eventBus, times(1)).register(any())
    }

    @Test
    fun `on init, no need to show intro`() {
        `when`(preferencesDataSource.needToShowCommentTooltip).thenReturn(false)
        val viewModel = AddCommentViewModel(
            fakeEntity,
            threadId,
            commentDataSource,
            mixpanelDataSource,
            userDataSource,
            imageLoader,
            preferencesDataSource,
            eventBus,
            schedulersProvider
        )

        val observerShowCommentIntro: Observer<Void> = mock()
        val observerShowKeyboard: Observer<Void> = mock()
        val observerUpdateAvatar: Observer<String?> = mock()
        val observerSongname: Observer<String> = mock()
        viewModel.showCommentIntroEvent.observeForever(observerShowCommentIntro)
        viewModel.showKeyboardEvent.observeForever(observerShowKeyboard)
        viewModel.avatar.observeForever(observerUpdateAvatar)
        viewModel.songName.observeForever(observerSongname)
        verifyZeroInteractions(observerShowCommentIntro)
        verify(observerShowKeyboard).onChanged(null)
        verify(observerUpdateAvatar).onChanged(anyOrNull())
        verify(observerSongname).onChanged(anyOrNull())
    }

    @Test
    fun `on cleared, unsubscribe from EventBus`() {
        viewModel.onCleared()
        verify(eventBus, times(1)).unregister(any())
    }

    @Test
    fun `show keyboard observed on comment intro dismiss through EventBus`() {
        viewModel.onMessageEvent(EventCommentIntroDismissed())
        val observerShowKeyboard: Observer<Void> = mock()
        viewModel.showKeyboardEvent.observeForever(observerShowKeyboard)
        verify(observerShowKeyboard).onChanged(null)
    }

    @Test
    fun `on background tapped`() {
        val observerHideKeyboard: Observer<Void> = mock()
        val observerClose: Observer<Void> = mock()
        viewModel.hideKeyboardEvent.observeForever(observerHideKeyboard)
        viewModel.close.observeForever(observerClose)
        viewModel.onBackgroundTapped()
        verify(observerHideKeyboard).onChanged(null)
        verify(observerClose).onChanged(null)
    }
}
