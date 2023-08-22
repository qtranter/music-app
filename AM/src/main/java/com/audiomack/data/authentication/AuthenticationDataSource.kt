package com.audiomack.data.authentication

import com.audiomack.data.socialauth.AppleAuthData
import com.audiomack.data.socialauth.FacebookAuthData
import com.audiomack.data.socialauth.GoogleAuthData
import com.audiomack.data.socialauth.TwitterAuthData
import com.audiomack.model.Credentials
import com.audiomack.ui.authentication.SignupCredentials
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface AuthenticationDataSource {
    interface AuthenticationCallback {
        fun onAuthenticationSuccess(credentials: Credentials?)

        fun onAuthenticationError(error: AuthenticationException)
    }

    interface LoginAuthenticationCallback : AuthenticationCallback {
        fun onBeforeLogin()
    }

    interface FacebookAuthenticationCallback : AuthenticationCallback

    interface GoogleAuthenticationCallback : AuthenticationCallback

    interface TwitterAuthenticationCallback : AuthenticationCallback

    interface AppleAuthenticationCallback : AuthenticationCallback

    fun loginWithEmailPassword(email: String, password: String): Single<Credentials>

    fun signup(
        signupCredentials: SignupCredentials,
        callback: (AuthenticationException?, Credentials?) -> Unit
    )

    fun loginWithFacebook(userId: String, token: String, socialEmail: String?): Single<Credentials>

    fun loginWithGoogle(googleToken: String, socialEmail: String?): Single<Credentials>

    fun loginWithTwitter(twitterToken: String, twitterSecret: String, socialEmail: String?): Single<Credentials>

    fun loginWithAppleId(appleIdToken: String, socialEmail: String?): Single<Credentials>

    fun forgotPassword(email: String, callback: (AuthenticationException?) -> Unit)

    fun checkEmailExistence(email: String?, slug: String?): Observable<Boolean>

    fun logout(): Completable

    fun changePassword(oldPassword: String, newPassword: String): Completable

    fun verifyForgotPasswordToken(token: String): Completable

    fun resetPassword(token: String, newPassword: String): Completable
}

sealed class AuthenticationException(override var message: String) : Exception(message)
class TimeoutAuthenticationException(override var message: String) : AuthenticationException(message)
class LoginException(override var message: String, var statusCode: Int?) : AuthenticationException(message)
class SignupException(override var message: String) : AuthenticationException(message)
class InvalidEmailAuthenticationException(override var message: String) : AuthenticationException(message)
class InvalidPasswordAuthenticationException(override var message: String) : AuthenticationException(message)
class InvalidUsernameAuthenticationException(override var message: String) : AuthenticationException(message)
class MismatchPasswordAuthenticationException(override var message: String) : AuthenticationException(message)
class OfflineException(override var message: String) : AuthenticationException(message)
open class ForgotPasswordException(override var message: String) : AuthenticationException(message)
class ForgotPasswordEmailNotFoundException(override var message: String) : ForgotPasswordException(message)
class FacebookAuthenticationException(override var message: String) : AuthenticationException(message)
class FacebookTimeoutAuthenticationException(override var message: String) : AuthenticationException(message)
class FacebookMissingEmailAuthenticationException(val authData: FacebookAuthData) : AuthenticationException("")
class FacebookExistingEmailAuthenticationException(val email: String) : AuthenticationException("")
class GoogleAuthenticationException(override var message: String) : AuthenticationException(message)
class GoogleTimeoutAuthenticationException(override var message: String) : AuthenticationException(message)
class GoogleMissingEmailAuthenticationException(val authData: GoogleAuthData) : AuthenticationException("")
class GoogleExistingEmailAuthenticationException(val email: String) : AuthenticationException("")
class TwitterAuthenticationException(override var message: String) : AuthenticationException(message)
class TwitterTimeoutAuthenticationException(override var message: String) : AuthenticationException(message)
class TwitterMissingEmailAuthenticationException(val authData: TwitterAuthData) : AuthenticationException("")
class TwitterExistingEmailAuthenticationException(val email: String) : AuthenticationException("")
class AppleAuthenticationException(override var message: String) : AuthenticationException(message)
class AppleTimeoutAuthenticationException(override var message: String) : AuthenticationException(message)
class AppleMissingEmailAuthenticationException(val authData: AppleAuthData) : AuthenticationException("")
class AppleExistingEmailAuthenticationException(val email: String) : AuthenticationException("")
class ProfileCompletionException(override var message: String) : AuthenticationException("")
class ProfileCompletionSkippableException(override var message: String) : AuthenticationException("")
