package com.audiomack.ui.authentication.changepw

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.authentication.AuthenticationDataSource
import com.audiomack.data.authentication.AuthenticationRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.LogoutReason
import com.audiomack.model.ProgressHUDMode
import com.audiomack.network.APIDetailedException
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.Completable

class ChangePasswordViewModel(
    private val authenticationRepository: AuthenticationDataSource = AuthenticationRepository(),
    private val userRepository: UserDataSource = UserRepository.getInstance(),
    private val mixpanelRepository: MixpanelDataSource = MixpanelRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    data class ViewState(
        val currentPasswordSecured: Boolean = true,
        val newPasswordSecured: Boolean = true,
        val confirmPasswordSecured: Boolean = true,
        val saveButtonEnabled: Boolean = false
    )

    private val _viewState = MutableLiveData(ViewState())
    val viewState: LiveData<ViewState> get() = _viewState

    val goBackEvent = SingleLiveEvent<Void>()
    val openForgotPasswordEvent = SingleLiveEvent<Void>()
    val showHUDEvent = SingleLiveEvent<ProgressHUDMode>()
    val showSuccessAlertEvent = SingleLiveEvent<Void>()

    private var currentPassword: String = ""
    private var newPassword: String = ""
    private var confirmPassword: String = ""

    fun onBackClick() {
        goBackEvent.call()
    }

    fun onCurrentPasswordShowHideClick() {
        val currentValue = requireNotNull(_viewState.value)
        _viewState.value = currentValue.copy(
            currentPasswordSecured = currentValue.currentPasswordSecured.not()
        )
    }

    fun onNewPasswordShowHideClick() {
        val currentValue = requireNotNull(_viewState.value)
        _viewState.value = currentValue.copy(
            newPasswordSecured = currentValue.newPasswordSecured.not()
        )
    }

    fun onConfirmPasswordShowHideClick() {
        val currentValue = requireNotNull(_viewState.value)
        _viewState.value = currentValue.copy(
            confirmPasswordSecured = currentValue.confirmPasswordSecured.not()
        )
    }

    fun onForgotPasswordClick() {
        openForgotPasswordEvent.call()
    }

    fun onCurrentPasswordChanged(password: String) {
        if (currentPassword == password) return
        currentPassword = password
        validateInput()
    }

    fun onNewPasswordChanged(password: String) {
        if (newPassword == password) return
        newPassword = password
        validateInput()
    }

    fun onConfirmPasswordChanged(password: String) {
        if (confirmPassword == password) return
        confirmPassword = password
        validateInput()
    }

    fun onSaveClick() {
        mixpanelRepository.trackChangePassword()
        showHUDEvent.value = ProgressHUDMode.Loading
        authenticationRepository.changePassword(currentPassword, newPassword)
            .andThen(Completable.defer { userRepository.logout(LogoutReason.ChangePassword) })
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                showHUDEvent.value = ProgressHUDMode.Dismiss
                showSuccessAlertEvent.call()
            }, {
                if (it is APIDetailedException) {
                    showHUDEvent.value = ProgressHUDMode.Failure(it.verboseDescription)
                } else {
                    showHUDEvent.value = ProgressHUDMode.Failure("")
                }
            })
            .composite()
    }

    private fun validateInput() {
        val currentValue = requireNotNull(_viewState.value)
        _viewState.value = currentValue.copy(saveButtonEnabled =
            currentPassword.isNotEmpty() && newPassword.isNotEmpty() && currentPassword != newPassword && newPassword == confirmPassword
        )
    }
}
