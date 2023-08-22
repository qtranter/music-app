package com.audiomack.usecases

import android.content.Context
import com.audiomack.R
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl

class LoginAlertUseCase(
    private val remoteVariablesProvider: RemoteVariablesProvider = RemoteVariablesProviderImpl()
) {

    /**
     * Fetches the login alert message copy from Firebase Remote Config, defaults to a bundled Stirng if empty.
     */
    fun getMessage(context: Context): String {
        return remoteVariablesProvider.loginAlertMessage.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.login_needed_message)
    }
}
