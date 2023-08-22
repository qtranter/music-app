package com.audiomack.ui.authentication.resetpw

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.authentication.AuthenticationDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.LogoutReason
import com.audiomack.model.ProgressHUDMode
import com.audiomack.network.APIDetailedException
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class ResetPasswordViewModelTest {

    @Mock
    private lateinit var authenticationRepository: AuthenticationDataSource

    @Mock
    private lateinit var userRepository: UserDataSource

    @Mock
    private lateinit var mixpanelRepository: MixpanelDataSource

    private lateinit var schedulers: SchedulersProvider

    private lateinit var token: String

    private lateinit var viewModel: ResetPasswordViewModel

    @Mock
    private lateinit var closeEventObserver: Observer<Void>

    @Mock
    private lateinit var viewStateObserver: Observer<ResetPasswordViewModel.ViewState>

    @Mock
    private lateinit var showHUDEventObserver: Observer<ProgressHUDMode>

    @Mock
    private lateinit var showSuccessAlertEventObserver: Observer<Void>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulers = TestSchedulersProvider()
        token = "xxx"
        viewModel = ResetPasswordViewModel(
            token,
            authenticationRepository,
            userRepository,
            mixpanelRepository,
            schedulers
        ).apply {
            closeEvent.observeForever(closeEventObserver)
            viewState.observeForever(viewStateObserver)
            showHUDEvent.observeForever(showHUDEventObserver)
            showSuccessAlertEvent.observeForever(showSuccessAlertEventObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `on close click`() {
        viewModel.onCloseClick()
        verify(closeEventObserver).onChanged(null)
    }

    @Test
    fun `on new password toggle visibility click`() {
        viewModel.onNewPasswordShowHideClick()
        viewModel.onNewPasswordShowHideClick()
        verify(viewStateObserver, times(2)).onChanged(argWhere { it.newPasswordSecured })
        verify(viewStateObserver, times(1)).onChanged(argWhere { !it.newPasswordSecured })
    }

    @Test
    fun `on confirm password toggle visibility click`() {
        viewModel.onConfirmPasswordShowHideClick()
        viewModel.onConfirmPasswordShowHideClick()
        verify(viewStateObserver, times(2)).onChanged(argWhere { it.confirmPasswordSecured })
        verify(viewStateObserver, times(1)).onChanged(argWhere { !it.confirmPasswordSecured })
    }

    @Test
    fun `save button enabled validations, valid input`() {
        viewModel.onNewPasswordChanged("123")
        viewModel.onConfirmPasswordChanged("123")
        verify(viewStateObserver, times(1)).onChanged(argWhere { it.resetButtonEnabled })
    }

    @Test
    fun `save button enabled validations, valid input, make sure UI updates are skipped when adding the same texts over and over`() {
        viewModel.onNewPasswordChanged("123")
        viewModel.onNewPasswordChanged("123")
        viewModel.onConfirmPasswordChanged("123")
        viewModel.onConfirmPasswordChanged("123")
        verify(viewStateObserver, times(1)).onChanged(argWhere { it.resetButtonEnabled })
        verify(viewStateObserver, times(2)).onChanged(argWhere { !it.resetButtonEnabled })
    }

    @Test
    fun `save button enabled validations, missing all passwords`() {
        verify(viewStateObserver, times(1)).onChanged(argWhere { !it.resetButtonEnabled })
    }

    @Test
    fun `save button enabled validations, missing confirm passwords`() {
        viewModel.onNewPasswordChanged("123")
        verify(viewStateObserver, times(2)).onChanged(argWhere { !it.resetButtonEnabled })
    }

    @Test
    fun `save button enabled validations, mismatching passwords`() {
        viewModel.onNewPasswordChanged("123")
        viewModel.onConfirmPasswordChanged("456")
        verify(viewStateObserver, times(3)).onChanged(argWhere { !it.resetButtonEnabled })
    }

    @Test
    fun `reset with success`() {
        val newPassword = "123"
        whenever(authenticationRepository.resetPassword(token, newPassword)).thenReturn(Completable.complete())
        whenever(userRepository.logout(LogoutReason.ResetPassword)).thenReturn(Completable.complete())

        viewModel.onNewPasswordChanged(newPassword)
        viewModel.onConfirmPasswordChanged(newPassword)
        viewModel.onResetClick()

        verify(mixpanelRepository).trackResetPassword(userRepository.getEmail() ?: "")
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(userRepository).logout(LogoutReason.ResetPassword)
        verify(showSuccessAlertEventObserver, times(1)).onChanged(null)
        verify(authenticationRepository, times(1)).resetPassword(token, newPassword)
    }

    @Test
    fun `reset with failure`() {
        val newPassword = "123"
        val errorTitle = "Error"
        val errorMessage = null
        val exception = APIDetailedException(errorTitle, errorMessage)
        whenever(authenticationRepository.resetPassword(token, newPassword)).thenReturn(Completable.error(exception))

        viewModel.onNewPasswordChanged(newPassword)
        viewModel.onConfirmPasswordChanged(newPassword)
        viewModel.onResetClick()

        verify(mixpanelRepository).trackResetPassword(userRepository.getEmail() ?: "")
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(userRepository, times(0)).logout(LogoutReason.ResetPassword)
        verify(showSuccessAlertEventObserver, times(0)).onChanged(null)
        verify(authenticationRepository, times(1)).resetPassword(token, newPassword)
    }
}
