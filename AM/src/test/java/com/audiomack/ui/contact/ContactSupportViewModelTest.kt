package com.audiomack.ui.contact

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.model.Action
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.authentication.contact.ContactSupportViewModel
import com.audiomack.ui.common.AMContactProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ContactSupportViewModelTest {

    @Mock
    private lateinit var zendeskDataSource: ZendeskDataSource

    @Mock
    private lateinit var preferencesDataSource: PreferencesDataSource

    @Mock
    private lateinit var contactProvider: AMContactProvider

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: ContactSupportViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        contactProvider = mock()
        viewModel = ContactSupportViewModel(zendeskDataSource, schedulersProvider, preferencesDataSource)
        viewModel.init(contactProvider)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `on close tapped`() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `on what tapped`() {
        val observerShowOptions: Observer<List<Action>> = mock()
        viewModel.showOptionsEvent.observeForever(observerShowOptions)
        viewModel.onWhatTapped()
        verify(observerShowOptions).onChanged(any())
    }

    @Test
    fun `on how tapped`() {
        val observerShowOptions: Observer<List<Action>> = mock()
        viewModel.showOptionsEvent.observeForever(observerShowOptions)
        viewModel.onHowTapped()
        verify(observerShowOptions).onChanged(any())
    }

    @Test
    fun `on when tapped`() {
        val observerShowOptions: Observer<List<Action>> = mock()
        viewModel.showOptionsEvent.observeForever(observerShowOptions)
        viewModel.onWhenTapped()
        verify(observerShowOptions).onChanged(any())
    }

    @Test
    fun `on email changed`() {
        val emailText = "email sample text"
        val observerEmail: Observer<String> = mock()
        viewModel.emailEvent.observeForever(observerEmail)
        viewModel.onEmailChanged(emailText)
        verify(observerEmail).onChanged(any())
    }

    @Test
    fun `on notes changed`() {
        val notesText = "notes sample text"
        val observerNotes: Observer<String> = mock()
        viewModel.notesEvent.observeForever(observerNotes)
        viewModel.onNotesChanged(notesText)
        verify(observerNotes).onChanged(any())
    }

    @Test
    fun `on send tapped show error`() {
        val observerSend: Observer<Void> = mock()
        val observerError: Observer<Void> = mock()
        viewModel.sendEvent.observeForever(observerSend)
        viewModel.errorEvent.observeForever(observerError)
        viewModel.onSendTapped("", "", "", "", "")
        verifyZeroInteractions(observerSend)
        verify(observerError).onChanged(null)
    }

    @Test
    fun `on send tapped show tooltip`() {
        val whatText = "what sample text"
        val howText = "how sample text"
        val whenText = "when sample text"
        val emailText = "email sample text"
        val notesText = "notes sample text"
        val observerClose: Observer<Void> = mock()
        val observerSend: Observer<Void> = mock()
        val observerError: Observer<Void> = mock()
        val observerTooltip: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.errorEvent.observeForever(observerError)
        viewModel.sendEvent.observeForever(observerSend)
        viewModel.tooltipEvent.observeForever(observerTooltip)
        `when`(preferencesDataSource.needToShowContactTooltip).thenReturn(true)
        viewModel.onSendTapped(whatText, howText, whenText, emailText, notesText)
        verify(observerTooltip).onChanged(null)
        verifyZeroInteractions(observerSend)
    }

    @Test
    fun `on send tapped`() {
        val whatText = "what sample text"
        val howText = "how sample text"
        val whenText = "when sample text"
        val emailText = "email sample text"
        val notesText = "notes sample text"
        val observerClose: Observer<Void> = mock()
        val observerSend: Observer<Void> = mock()
        val observerError: Observer<Void> = mock()
        val observerTooltip: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.errorEvent.observeForever(observerError)
        viewModel.sendEvent.observeForever(observerSend)
        viewModel.tooltipEvent.observeForever(observerTooltip)
        `when`(preferencesDataSource.needToShowContactTooltip).thenReturn(false)
        viewModel.onSendTapped(whatText, howText, whenText, emailText, notesText)
        verify(observerSend).onChanged(null)
        verifyZeroInteractions(observerTooltip)
    }

    @Test
    fun `on send support ticket success`() {
        val whatText = "what sample text"
        val howText = "how sample text"
        val whenText = "when sample text"
        val emailText = "email sample text"
        val notesText = "notes sample text"
        `when`(zendeskDataSource.sendSupportTicket(whatText, howText, whenText, emailText, notesText)).thenReturn(Observable.just(true))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowSuccessMessage: Observer<Void> = mock()
        val observerClose: Observer<Void> = mock()
        viewModel.showLoadingEvent.observeForever(observerShowLoading)
        viewModel.hideLoadingEvent.observeForever(observerHideLoading)
        viewModel.showSuccessMessageEvent.observeForever(observerShowSuccessMessage)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onSendTicket(whatText, howText, whenText, emailText, notesText)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verify(observerShowSuccessMessage).onChanged(null)
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `on send support ticket failure`() {
        val whatText = "what sample text"
        val howText = "how sample text"
        val whenText = "when sample text"
        val emailText = "email sample text"
        val notesText = "notes sample text"
        `when`(zendeskDataSource.sendSupportTicket(whatText, howText, whenText, emailText, notesText)).thenReturn(Observable.error(Exception("Error for test")))
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowErrorMessage: Observer<Void> = mock()
        val observerClose: Observer<Void> = mock()
        viewModel.showLoadingEvent.observeForever(observerShowLoading)
        viewModel.hideLoadingEvent.observeForever(observerHideLoading)
        viewModel.showErrorMessageEvent.observeForever(observerShowErrorMessage)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onSendTicket(whatText, howText, whenText, emailText, notesText)
        verifyZeroInteractions(preferencesDataSource)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verify(observerShowErrorMessage).onChanged(null)
        verify(observerClose).onChanged(null)
    }
}
