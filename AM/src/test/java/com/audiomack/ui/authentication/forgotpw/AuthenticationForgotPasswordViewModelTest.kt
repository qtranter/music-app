package com.audiomack.ui.authentication.forgotpw

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.authentication.AuthenticationDataSource
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.utils.RegexValidator
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class AuthenticationForgotPasswordViewModelTest {

    @Mock
    private lateinit var authenticationRepository: AuthenticationDataSource

    @Mock
    private lateinit var zendeskRepository: ZendeskDataSource

    @Mock
    private lateinit var regexValidator: RegexValidator

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    private lateinit var viewModel: AuthenticationForgotPasswordViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        viewModel = AuthenticationForgotPasswordViewModel(authenticationRepository, zendeskRepository, regexValidator, mixpanelDataSource)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `on close tapped`() {
        val observerClose: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onCloseTapped()
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `on background tapped`() {
        val observerClose: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onBackgroundTapped()
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `on cancel tapped`() {
        val observerClose: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onCancelTapped()
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `on contact us tapped`() {
        val articleId = 1L
        whenever(zendeskRepository.cantLoginArticleId).thenReturn(articleId)

        val observerClose: Observer<Void> = mock()
        val observerOpenSupport: Observer<Long> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.openSupportEvent.observeForever(observerOpenSupport)
        viewModel.onContactUsTapped()
        verify(observerClose).onChanged(null)
        verify(observerOpenSupport).onChanged(articleId)
    }

    @Test
    fun `on email changed`() {
        val observerSaveEnabled: Observer<Boolean> = mock()
        viewModel.saveEnabled.observeForever(observerSaveEnabled)

        // Initial value
        observerSaveEnabled.onChanged(eq(false))

        // Input something invalid
        viewModel.onEmailChanged("dave")
        whenever(regexValidator.isValidEmailAddress(any())).thenReturn(false)
        observerSaveEnabled.onChanged(eq(false))

        // Input something valid
        viewModel.onEmailChanged("dave@audiomack.com")
        whenever(regexValidator.isValidEmailAddress(any())).thenReturn(true)
        observerSaveEnabled.onChanged(eq(true))
    }

    @Test
    fun `on save, invalid email`() {
        val observerClose: Observer<Void> = mock()
        val observerForgotPasswordStatus: Observer<AuthenticationForgotPasswordViewModel.ForgotPasswordStatus> = mock()
        val observerHideKeyboard: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.forgotPasswordStatusEvent.observeForever(observerForgotPasswordStatus)
        viewModel.hideKeyboardEvent.observeForever(observerHideKeyboard)

        viewModel.onSaveTapped("dave@audiomack.com")

        verifyZeroInteractions(observerForgotPasswordStatus)
        verifyZeroInteractions(observerClose)
        verifyZeroInteractions(observerHideKeyboard)
        verifyZeroInteractions(mixpanelDataSource)
    }

    @Test
    fun `on save, valid email`() {
        val observerClose: Observer<Void> = mock()
        val observerForgotPasswordStatus: Observer<AuthenticationForgotPasswordViewModel.ForgotPasswordStatus> = mock()
        val observerHideKeyboard: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.forgotPasswordStatusEvent.observeForever(observerForgotPasswordStatus)
        viewModel.hideKeyboardEvent.observeForever(observerHideKeyboard)

        whenever(regexValidator.isValidEmailAddress(any())).thenReturn(true)
        viewModel.onEmailChanged("dave@audiomack.com")
        viewModel.onSaveTapped("dave@audiomack.com")

        verify(observerForgotPasswordStatus).onChanged(eq(AuthenticationForgotPasswordViewModel.ForgotPasswordStatus.Loading))
        verifyZeroInteractions(observerClose)
        verify(observerHideKeyboard).onChanged(null)
        verify(mixpanelDataSource).trackResetPassword(eq("dave@audiomack.com"))
    }
}
