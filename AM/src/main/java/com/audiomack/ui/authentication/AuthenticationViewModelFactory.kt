package com.audiomack.ui.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.model.LoginSignupSource

class AuthenticationViewModelFactory(
    private val source: LoginSignupSource,
    private val profileCompletion: Boolean
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AuthenticationViewModel(source, profileCompletion) as T
    }
}
