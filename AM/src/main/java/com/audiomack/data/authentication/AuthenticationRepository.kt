package com.audiomack.data.authentication

import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.socialauth.AppleAuthData
import com.audiomack.data.socialauth.FacebookAuthData
import com.audiomack.data.socialauth.GoogleAuthData
import com.audiomack.data.socialauth.TwitterAuthData
import com.audiomack.model.Credentials
import com.audiomack.model.ErrorCodes
import com.audiomack.network.API
import com.audiomack.network.APIInterface
import com.audiomack.network.APILoginException
import com.audiomack.ui.authentication.SignupCredentials
import io.reactivex.Observable
import io.reactivex.Single

class AuthenticationRepository(
    private val apiInstance: APIInterface.AuthenticationInterface = API.getInstance()
) : AuthenticationDataSource {
    override fun loginWithEmailPassword(email: String, password: String): Single<Credentials> {
        return apiInstance.loginWithEmailPassword(email, password)
    }

    override fun signup(
        signupCredentials: SignupCredentials,
        callback: (AuthenticationException?, Credentials?) -> Unit
    ) {

        val birthday = signupCredentials.birthday
        val gender = signupCredentials.gender
        if (birthday == null || gender == null) {
            val message = MainApplication.context?.getString(R.string.generic_api_error) ?: ""
            callback(SignupException(message), null)
            return
        }

        apiInstance.signup(
            signupCredentials.username,
            signupCredentials.email,
            signupCredentials.password,
            signupCredentials.advertisingId,
            birthday,
            gender,
            object : API.SignupListener {
                override fun onSuccess(credentials: Credentials) {
                    callback(null, credentials)
                }

                override fun onTimeout() {
                    callback(TimeoutAuthenticationException("Bad Connection"), null)
                }

                override fun onFailure(errorMessage: String?) {
                    val message = errorMessage
                        ?: MainApplication.context?.getString(R.string.generic_api_error) ?: ""
                    callback(SignupException(message), null)
                }
            })
    }

    override fun loginWithFacebook(userId: String, token: String, socialEmail: String?): Single<Credentials> {
        return apiInstance.loginWithFacebook(userId, token, socialEmail)
            .onErrorResumeNext {
                if (it is APILoginException) {
                    if (it.timeout) {
                        Single.error(FacebookTimeoutAuthenticationException("Bad Connection"))
                    } else if (it.errorCode == ErrorCodes.MISSING_SOCIAL_EMAIL && socialEmail == null) {
                        Single.error(
                            FacebookMissingEmailAuthenticationException(
                                FacebookAuthData(
                                    userId,
                                    token,
                                    true
                                )
                            )
                        )
                    } else if (it.errorCode == ErrorCodes.SOCIAL_LOGIN_EXISTING_EMAIL && socialEmail != null) {
                        Single.error(FacebookExistingEmailAuthenticationException(socialEmail))
                    } else {
                        Single.error(FacebookAuthenticationException(it.errorMessage.takeIf { it.isNotBlank() }
                            ?: MainApplication.context!!.getString(
                                R.string.generic_api_error
                            )))
                    }
                } else {
                    Single.error(
                        FacebookAuthenticationException(
                            MainApplication.context!!.getString(
                                R.string.generic_api_error
                            )
                        )
                    )
                }
            }
    }

    override fun loginWithGoogle(googleToken: String, socialEmail: String?): Single<Credentials> {
        return apiInstance.loginWithGoogle(googleToken, socialEmail)
            .onErrorResumeNext {
                if (it is APILoginException) {
                    if (it.timeout) {
                        Single.error(GoogleTimeoutAuthenticationException("Bad Connection"))
                    } else if (it.errorCode == ErrorCodes.MISSING_SOCIAL_EMAIL && socialEmail == null) {
                        Single.error(
                            GoogleMissingEmailAuthenticationException(
                                GoogleAuthData(
                                    googleToken,
                                    true
                                )
                            )
                        )
                    } else if (it.errorCode == ErrorCodes.SOCIAL_LOGIN_EXISTING_EMAIL && socialEmail != null) {
                        Single.error(GoogleExistingEmailAuthenticationException(socialEmail))
                    } else {
                        Single.error(GoogleAuthenticationException(it.errorMessage.takeIf { it.isNotBlank() }
                            ?: MainApplication.context!!.getString(
                                R.string.generic_api_error
                            )))
                    }
                } else {
                    Single.error(GoogleAuthenticationException(MainApplication.context!!.getString(R.string.generic_api_error)))
                }
            }
    }

    override fun loginWithTwitter(twitterToken: String, twitterSecret: String, socialEmail: String?): Single<Credentials> {
        return apiInstance.loginWithTwitter(twitterToken, twitterSecret, socialEmail)
            .onErrorResumeNext {
                if (it is APILoginException) {
                    if (it.timeout) {
                        Single.error(TwitterTimeoutAuthenticationException("Bad Connection"))
                    } else if (it.errorCode == ErrorCodes.MISSING_SOCIAL_EMAIL && socialEmail == null) {
                        Single.error(
                            TwitterMissingEmailAuthenticationException(
                                TwitterAuthData(
                                    twitterToken,
                                    twitterSecret,
                                    true
                                )
                            )
                        )
                    } else if (it.errorCode == ErrorCodes.SOCIAL_LOGIN_EXISTING_EMAIL && socialEmail != null) {
                        Single.error(TwitterExistingEmailAuthenticationException(socialEmail))
                    } else {
                        Single.error(TwitterAuthenticationException(it.errorMessage.takeIf { it.isNotBlank() }
                            ?: MainApplication.context!!.getString(
                                R.string.generic_api_error
                            )))
                    }
                } else {
                    Single.error(
                        TwitterAuthenticationException(
                            MainApplication.context!!.getString(
                                R.string.generic_api_error
                            )
                        )
                    )
                }
            }
    }

    override fun loginWithAppleId(appleIdToken: String, socialEmail: String?): Single<Credentials> {
        return apiInstance.loginWithAppleId(appleIdToken, socialEmail)
            .onErrorResumeNext {
                if (it is APILoginException) {
                    if (it.timeout) {
                        Single.error(AppleTimeoutAuthenticationException("Bad Connection"))
                    } else if (it.errorCode == ErrorCodes.MISSING_SOCIAL_EMAIL && socialEmail == null) {
                        Single.error(
                            AppleMissingEmailAuthenticationException(
                                AppleAuthData(
                                    appleIdToken
                                )
                            )
                        )
                    } else if (it.errorCode == ErrorCodes.SOCIAL_LOGIN_EXISTING_EMAIL && socialEmail != null) {
                        Single.error(AppleExistingEmailAuthenticationException(socialEmail))
                    } else {
                        Single.error(AppleAuthenticationException(it.errorMessage.takeIf { it.isNotBlank() }
                            ?: MainApplication.context!!.getString(
                                R.string.generic_api_error
                            )))
                    }
                } else {
                    Single.error(AppleAuthenticationException(MainApplication.context!!.getString(R.string.generic_api_error)))
                }
            }
    }

    override fun forgotPassword(email: String, callback: (AuthenticationException?) -> Unit) {
        apiInstance.forgotPassword(email, object : API.ForgotPasswordListener {
            override fun onSuccess() {
                callback(null)
            }

            override fun onFailure(errorMessage: String?, emailNotFound: Boolean) {
                if (emailNotFound) {
                    callback(ForgotPasswordEmailNotFoundException("That email was not found\n"))
                    return
                }
                val message = if (errorMessage?.isNotEmpty() == true) {
                    "$errorMessage\n"
                } else ""
                callback(ForgotPasswordException(message))
            }
        })
    }

    override fun checkEmailExistence(email: String?, slug: String?): Observable<Boolean> {
        return apiInstance.checkEmailExistence(email, slug)
    }

    override fun logout() = apiInstance.logout()

    override fun changePassword(oldPassword: String, newPassword: String) =
        apiInstance.changePassword(oldPassword, newPassword)

    override fun verifyForgotPasswordToken(token: String) =
        apiInstance.verifyForgotPasswordToken(token)

    override fun resetPassword(token: String, newPassword: String) =
        apiInstance.resetPassword(token, newPassword)
}
