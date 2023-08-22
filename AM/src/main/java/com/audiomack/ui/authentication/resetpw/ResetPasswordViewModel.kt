package com.audiomack.ui.authentication.resetpw

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

class ResetPasswordViewModel(
    private val token: String,
    private val authenticationRepository: AuthenticationDataSource = AuthenticationRepository(),
    private val userRepository: UserDataSource = UserRepository.getInstance(),
    private val mixpanelRepository: MixpanelDataSource = MixpanelRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    data class ViewState(
        val newPasswordSecured: Boolean = true,
        val confirmPasswordSecured: Boolean = true,
        val resetButtonEnabled: Boolean = false
    )

    private val _viewState = MutableLiveData(ViewState())
    val viewState: LiveData<ViewState> get() = _viewState

    val closeEvent = SingleLiveEvent<Void>()
    val showHUDEvent = SingleLiveEvent<ProgressHUDMode>()
    val showSuccessAlertEvent = SingleLiveEvent<Void>()

    private var newPassword: String = ""
    private var confirmPassword: String = ""

    fun onCloseClick() {
        closeEvent.call()
    }

    fun onResetClick() {
        mixpanelRepository.trackResetPassword(userRepository.getEmail() ?: "")
        showHUDEvent.value = ProgressHUDMode.Loading
        authenticationRepository.resetPassword(token, newPassword)
            .andThen(Completable.defer { userRepository.logout(LogoutReason.ResetPassword) })
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

    private fun validateInput() {
        val currentValue = requireNotNull(_viewState.value)
        _viewState.value = currentValue.copy(
            resetButtonEnabled = newPassword.isNotEmpty() && newPassword == confirmPassword
        )
    }
}

@Suppress("UNCHECKED_CAST")
class ResetPasswordViewModelFactory(private val token: String) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = ResetPasswordViewModel(token) as T
}
