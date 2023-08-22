package com.audiomack.ui.authentication.socialemail

import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.model.EventSocialEmailAdded
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import org.greenrobot.eventbus.EventBus

class AuthenticationSocialEmailViewModel : BaseViewModel() {

    val closeEvent = SingleLiveEvent<Void>()
    val showErrorEvent = SingleLiveEvent<String>()

    fun onBackgroundTapped() {
        closeEvent.call()
    }

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onCancelTapped() {
        closeEvent.call()
    }

    fun onSubmitTapped(email: String) {
        if (email.isEmpty()) {
            showErrorEvent.postValue(MainApplication.context?.getString(R.string.authentication_validation_email_empty) ?: "")
            return
        }
        EventBus.getDefault().post(EventSocialEmailAdded(email))
        closeEvent.call()
    }
}
