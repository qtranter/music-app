package com.audiomack.ui.authentication.changepw

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

class ChangePasswordViewModelTest {

    @Mock
    private lateinit var authenticationRepository: AuthenticationDataSource

    @Mock
    private lateinit var userRepository: UserDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    private lateinit var schedulers: SchedulersProvider

    private lateinit var viewModel: ChangePasswordViewModel

    @Mock
    private lateinit var goBackEventObserver: Observer<Void>

    @Mock
    private lateinit var openForgotPasswordEventObserver: Observer<Void>

    @Mock
    private lateinit var viewStateObserver: Observer<ChangePasswordViewModel.ViewState>

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
        viewModel = ChangePasswordViewModel(
            authenticationRepository,
            userRepository,
            mixpanelDataSource,
            schedulers
        ).apply {
            goBackEvent.observeForever(goBackEventObserver)
            openForgotPasswordEvent.observeForever(openForgotPasswordEventObserver)
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
    fun `on back click`() {
        viewModel.onBackClick()
        verify(goBackEventObserver).onChanged(null)
    }

    @Test
    fun `on forgot password click`() {
        viewModel.onForgotPasswordClick()
        verify(openForgotPasswordEventObserver).onChanged(null)
    }

    @Test
    fun `on current password toggle visibility click`() {
        viewModel.onCurrentPasswordShowHideClick()
        viewModel.onCurrentPasswordShowHideClick()
        verify(viewStateObserver, times(2)).onChanged(argWhere { it.currentPasswordSecured })
        verify(viewStateObserver, times(1)).onChanged(argWhere { !it.currentPasswordSecured })
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
        viewModel.onCurrentPasswordChanged("123")
        viewModel.onNewPasswordChanged("456")
        viewModel.onConfirmPasswordChanged("456")
        verify(viewStateObserver, times(1)).onChanged(argWhere { it.saveButtonEnabled })
    }

    @Test
    fun `save button enabled validations, valid input, make sure UI updates are skipped when adding the same texts over and over`() {
        viewModel.onCurrentPasswordChanged("123")
        viewModel.onCurrentPasswordChanged("123")
        viewModel.onNewPasswordChanged("456")
        viewModel.onNewPasswordChanged("456")
        viewModel.onConfirmPasswordChanged("456")
        viewModel.onConfirmPasswordChanged("456")
        verify(viewStateObserver, times(1)).onChanged(argWhere { it.saveButtonEnabled })
        verify(viewStateObserver, times(3)).onChanged(argWhere { !it.saveButtonEnabled })
    }

    @Test
    fun `save button enabled validations, missing all passwords`() {
        verify(viewStateObserver, times(1)).onChanged(argWhere { !it.saveButtonEnabled })
    }

    @Test
    fun `save button enabled validations, missing new passwords`() {
        viewModel.onCurrentPasswordChanged("123")
        verify(viewStateObserver, times(2)).onChanged(argWhere { !it.saveButtonEnabled })
    }

    @Test
    fun `save button enabled validations, missing confirm passwords`() {
        viewModel.onCurrentPasswordChanged("123")
        viewModel.onNewPasswordChanged("456")
        verify(viewStateObserver, times(3)).onChanged(argWhere { !it.saveButtonEnabled })
    }

    @Test
    fun `save button enabled validations, mismatching new passwords`() {
        viewModel.onCurrentPasswordChanged("123")
        viewModel.onNewPasswordChanged("456")
        viewModel.onConfirmPasswordChanged("789")
        verify(viewStateObserver, times(4)).onChanged(argWhere { !it.saveButtonEnabled })
    }

    @Test
    fun `save button enabled validations, current and new passwords are equal`() {
        viewModel.onCurrentPasswordChanged("123")
        viewModel.onNewPasswordChanged("123")
        viewModel.onConfirmPasswordChanged("123")
        verify(viewStateObserver, times(4)).onChanged(argWhere { !it.saveButtonEnabled })
    }

    @Test
    fun `save with success`() {
        val oldPassword = "123"
        val newPassword = "456"
        whenever(authenticationRepository.changePassword(oldPassword, newPassword)).thenReturn(Completable.complete())
        whenever(userRepository.logout(LogoutReason.ChangePassword)).thenReturn(Completable.complete())

        viewModel.onCurrentPasswordChanged(oldPassword)
        viewModel.onNewPasswordChanged(newPassword)
        viewModel.onConfirmPasswordChanged(newPassword)
        viewModel.onSaveClick()

        verify(mixpanelDataSource).trackChangePassword()
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Dismiss)
        verify(userRepository).logout(LogoutReason.ChangePassword)
        verify(showSuccessAlertEventObserver, times(1)).onChanged(null)
        verify(authenticationRepository, times(1)).changePassword(oldPassword, newPassword)
    }

    @Test
    fun `save with failure`() {
        val oldPassword = "123"
        val newPassword = "456"
        val errorTitle = "Error"
        val errorMessage = null
        val exception = APIDetailedException(errorTitle, errorMessage)
        whenever(authenticationRepository.changePassword(oldPassword, newPassword)).thenReturn(Completable.error(exception))

        viewModel.onCurrentPasswordChanged(oldPassword)
        viewModel.onNewPasswordChanged(newPassword)
        viewModel.onConfirmPasswordChanged(newPassword)
        viewModel.onSaveClick()

        verify(mixpanelDataSource).trackChangePassword()
        verify(showHUDEventObserver, times(1)).onChanged(ProgressHUDMode.Loading)
        verify(showHUDEventObserver, times(1)).onChanged(argWhere { it is ProgressHUDMode.Failure })
        verify(userRepository, times(0)).logout(LogoutReason.ChangePassword)
        verify(showSuccessAlertEventObserver, times(0)).onChanged(null)
        verify(authenticationRepository, times(1)).changePassword(oldPassword, newPassword)
    }
}
