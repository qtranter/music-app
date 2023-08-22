package com.audiomack.ui.authentication.forgotpw

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.authentication.AuthenticationDataSource
import com.audiomack.data.authentication.AuthenticationException
import com.audiomack.data.authentication.AuthenticationRepository
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.data.support.ZendeskRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.network.API
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.RegexValidator
import com.audiomack.utils.RegexValidatorImpl
import com.audiomack.utils.SingleLiveEvent

class AuthenticationForgotPasswordViewModel(
    private val authenticationRepository: AuthenticationDataSource = AuthenticationRepository(
        API.getInstance()
    ),
    private val zendeskDataSource: ZendeskDataSource = ZendeskRepository(),
    private val regexValidator: RegexValidator = RegexValidatorImpl(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository()
) : BaseViewModel() {

    sealed class ForgotPasswordStatus {
        object Loading : ForgotPasswordStatus()
        object Success : ForgotPasswordStatus()
        class Error(val exception: AuthenticationException) : ForgotPasswordStatus()
    }

    private var _saveEnabled = MutableLiveData(false)
    val saveEnabled: LiveData<Boolean> get() = _saveEnabled

    val closeEvent = SingleLiveEvent<Void>()
    val openSupportEvent = SingleLiveEvent<Long>()
    val forgotPasswordStatusEvent = MutableLiveData<ForgotPasswordStatus>()
    val hideKeyboardEvent = SingleLiveEvent<Void>()

    fun onContactUsTapped() {
        openSupportEvent.postValue(zendeskDataSource.cantLoginArticleId)
        closeEvent.call()
    }

    fun onEmailChanged(email: String) {
        _saveEnabled.postValue(regexValidator.isValidEmailAddress(email))
    }

    fun onSaveTapped(email: String) {
        if (_saveEnabled.value == true) {
            hideKeyboardEvent.call()
            forgotPasswordStatusEvent.postValue(ForgotPasswordStatus.Loading)
            authenticationRepository.forgotPassword(email.trim()) { exception: AuthenticationException? ->
                exception?.let {
                    forgotPasswordStatusEvent.postValue(ForgotPasswordStatus.Error(it))
                    closeEvent.call()
                    return@forgotPassword
                }
                forgotPasswordStatusEvent.postValue(ForgotPasswordStatus.Success)
                closeEvent.call()
            }
            mixpanelDataSource.trackResetPassword(email.trim())
        }
    }

    fun onBackgroundTapped() {
        closeEvent.call()
    }

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onCancelTapped() {
        closeEvent.call()
    }
}
