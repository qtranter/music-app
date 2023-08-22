package com.audiomack.data.authentication

import com.audiomack.model.AMArtist.Gender
import com.audiomack.model.Credentials
import com.audiomack.network.API.ForgotPasswordListener
import com.audiomack.network.APIInterface
import com.audiomack.network.APILoginException
import com.audiomack.ui.authentication.SignupCredentials
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import java.util.Date
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

class AuthenticationRepositoryTest {
    private lateinit var apiInstance: APIInterface.AuthenticationInterface
    private lateinit var repository: AuthenticationRepository

    @Before
    fun setUp() {
        apiInstance = mock()
        repository = AuthenticationRepository(apiInstance)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun login_onSuccess() {
        val email = "test@email.com"
        val password = "password"
        val creds = Credentials()

        creds.email = email
        creds.password = password

        whenever(
            apiInstance.loginWithEmailPassword(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.just(creds))

        repository.loginWithEmailPassword(email, password).test()
            .assertNoErrors()
            .assertValue { it.email == email && it.password == password }

        verify(apiInstance).loginWithEmailPassword(
            ArgumentMatchers.matches(email),
            ArgumentMatchers.matches(password)
        )
    }

    @Test
    fun login_onTimeout() {
        val email = "test@email.com"
        val password = "password"
        val creds = Credentials()

        creds.email = email
        creds.password = password

        whenever(
            apiInstance.loginWithEmailPassword(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.error(TimeoutAuthenticationException("")))

        repository.loginWithEmailPassword(email, password)
            .test()
            .assertError { it is TimeoutAuthenticationException }
            .assertNoValues()

        verify(apiInstance).loginWithEmailPassword(
            ArgumentMatchers.matches(email),
            ArgumentMatchers.matches(password)
        )
    }

    @Test
    fun login_onFailureWithMessage() {
        val email = "test@email.com"
        val password = "password"
        val creds = Credentials()

        creds.email = email
        creds.password = password

        val failureMessage = "Try again"

        whenever(
            apiInstance.loginWithEmailPassword(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.error(LoginException(failureMessage, null)))

        repository.loginWithEmailPassword(email, password)
            .test()
            .assertError { it is LoginException && it.message == failureMessage }
            .assertNoValues()

        verify(apiInstance).loginWithEmailPassword(
            ArgumentMatchers.matches(email),
            ArgumentMatchers.matches(password)
        )
    }

    @Test
    fun login_onFailureWithoutMessage() {
        val email = "test@email.com"
        val password = "password"
        val creds = Credentials()

        creds.email = email
        creds.password = password

        whenever(
            apiInstance.loginWithEmailPassword(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.error(LoginException("", null)))

        repository.loginWithEmailPassword(email, password)
            .test()
            .assertError { it is LoginException }
            .assertNoValues()

        verify(apiInstance).loginWithEmailPassword(
            ArgumentMatchers.matches(email),
            ArgumentMatchers.matches(password)
        )
    }

    @Test
    fun `api signup`() {
        val username = "someshit"
        val email = "test@email.com"
        val password = "password"
        val birthday = Date()
        val gender = Gender.NON_BINARY
        val advertisingId = "asdfasasdfasdf"

        val signupCredentials =
            SignupCredentials(username, email, password, advertisingId, birthday, gender)

        repository.signup(signupCredentials, mock())

        verify(apiInstance).signup(
            eq(username),
            eq(email),
            eq(password),
            eq(advertisingId),
            eq(birthday),
            eq(gender),
            any()
        )
    }

    @Test
    fun loginWithFacebook_onSuccess() {
        val userId = "userId"
        val token = "token"
        val socialEmail = "email"
        val creds = Credentials()

        creds.facebookId = userId

        whenever(
            apiInstance.loginWithFacebook(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.just(creds))

        repository.loginWithFacebook(userId, token, socialEmail)
            .test()
            .assertNoErrors()
            .assertValue { it.facebookId == userId }

        verify(apiInstance).loginWithFacebook(
            ArgumentMatchers.matches(userId),
            ArgumentMatchers.matches(token),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun loginWithFacebook_onTimeout() {
        val userId = "userId"
        val token = "token"
        val socialEmail = "email"
        val creds = Credentials()

        creds.facebookId = userId

        whenever(
            apiInstance.loginWithFacebook(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(
            Single.error(
                APILoginException("", null, 0, true)
            )
        )

        repository.loginWithFacebook(userId, token, socialEmail)
            .test()
            .assertError { it is FacebookTimeoutAuthenticationException }
            .assertNoValues()

        verify(apiInstance).loginWithFacebook(
            ArgumentMatchers.matches(userId),
            ArgumentMatchers.matches(token),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun loginWithFacebook_onFailureWithMessage() {
        val userId = "userId"
        val token = "token"
        val socialEmail = "email"
        val creds = Credentials()

        creds.facebookId = userId

        val failureMessage = "Try again"

        whenever(
            apiInstance.loginWithFacebook(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.error(APILoginException(failureMessage, null, 0, false)))

        repository.loginWithFacebook(userId, token, socialEmail)
            .test()
            .assertError { it is FacebookAuthenticationException && it.message == failureMessage }
            .assertNoValues()

        verify(apiInstance).loginWithFacebook(
            ArgumentMatchers.matches(userId),
            ArgumentMatchers.matches(token),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun loginWithGoogle_onSuccess() {
        val idToken = "idToken"
        val socialEmail = "email"
        val creds = Credentials()

        creds.googleToken = idToken

        whenever(
            apiInstance.loginWithGoogle(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.just(creds))

        repository.loginWithGoogle(idToken, socialEmail)
            .test()
            .assertNoErrors()
            .assertValue { it.googleToken == idToken }

        verify(apiInstance).loginWithGoogle(
            ArgumentMatchers.matches(idToken),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun loginWithGoogle_onTimeout() {
        val idToken = "idToken"
        val socialEmail = "email"
        val creds = Credentials()

        creds.googleToken = idToken

        whenever(
            apiInstance.loginWithGoogle(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(
            Single.error(
                APILoginException("", null, 0, true)
            )
        )

        repository.loginWithGoogle(idToken, socialEmail)
            .test()
            .assertError { it is GoogleTimeoutAuthenticationException }
            .assertNoValues()

        verify(apiInstance).loginWithGoogle(
            ArgumentMatchers.matches(idToken),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun loginWithGoogle_onFailureWithMessage() {
        val idToken = "idToken"
        val socialEmail = "email"
        val creds = Credentials()

        creds.googleToken = idToken

        val failureMessage = "Try again"

        whenever(
            apiInstance.loginWithGoogle(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.error(APILoginException(failureMessage, null, 0, false)))

        repository.loginWithGoogle(idToken, socialEmail)
            .test()
            .assertError { it is GoogleAuthenticationException && it.message == failureMessage }
            .assertNoValues()

        verify(apiInstance).loginWithGoogle(
            ArgumentMatchers.matches(idToken),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun loginWithTwitter_onSuccess() {
        val token = "token"
        val secret = "secret"
        val socialEmail = "email"
        val creds = Credentials()

        creds.twitterToken = token
        creds.twitterSecret = secret

        whenever(
            apiInstance.loginWithTwitter(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.just(creds))

        repository.loginWithTwitter(token, secret, socialEmail)
            .test()
            .assertNoErrors()
            .assertValue { it.twitterToken == token && it.twitterSecret == secret }

        verify(apiInstance).loginWithTwitter(
            ArgumentMatchers.matches(token),
            ArgumentMatchers.matches(secret),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun loginWithTwitter_onTimeout() {
        val token = "token"
        val secret = "secret"
        val socialEmail = "email"
        val creds = Credentials()

        creds.twitterToken = token
        creds.twitterSecret = secret

        whenever(
            apiInstance.loginWithTwitter(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(
            Single.error(
                APILoginException("", null, 0, true)
            )
        )

        repository.loginWithTwitter(token, secret, socialEmail)
            .test()
            .assertError { it is TwitterTimeoutAuthenticationException }
            .assertNoValues()

        verify(apiInstance).loginWithTwitter(
            ArgumentMatchers.matches(token),
            ArgumentMatchers.matches(secret),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun loginWithTwitter_onFailureWithMessage() {
        val token = "token"
        val secret = "secret"
        val socialEmail = "email"
        val creds = Credentials()

        creds.twitterToken = token
        creds.twitterSecret = secret

        val failureMessage = "Try again"

        whenever(
            apiInstance.loginWithTwitter(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.error(APILoginException(failureMessage, null, 0, false)))

        repository.loginWithTwitter(token, secret, socialEmail)
            .test()
            .assertError { it is TwitterAuthenticationException && it.message == failureMessage }
            .assertNoValues()

        verify(apiInstance).loginWithTwitter(
            ArgumentMatchers.matches(token),
            ArgumentMatchers.matches(secret),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun loginWithAppleId_onSuccess() {
        val token = "token"
        val socialEmail = "email"
        val creds = Credentials()

        creds.appleIdToken = token

        whenever(
            apiInstance.loginWithAppleId(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.just(creds))

        repository.loginWithAppleId(token, socialEmail)
            .test()
            .assertNoErrors()
            .assertValue { it.appleIdToken == token }

        verify(apiInstance).loginWithAppleId(
            ArgumentMatchers.matches(token),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun loginWithAppleId_onTimeout() {
        val token = "token"
        val socialEmail = "email"
        val creds = Credentials()

        creds.appleIdToken = token

        whenever(
            apiInstance.loginWithAppleId(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(
            Single.error(
                APILoginException("", null, 0, true)
            )
        )

        repository.loginWithAppleId(token, socialEmail)
            .test()
            .assertError { it is AppleTimeoutAuthenticationException }
            .assertNoValues()

        verify(apiInstance).loginWithAppleId(
            ArgumentMatchers.matches(token),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun loginWithAppleId_onFailureWithMessage() {
        val token = "token"
        val socialEmail = "email"
        val creds = Credentials()

        creds.appleIdToken = token

        val failureMessage = "Try again"

        whenever(
            apiInstance.loginWithAppleId(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(Single.error(APILoginException(failureMessage, null, 0, false)))

        repository.loginWithAppleId(token, socialEmail)
            .test()
            .assertError { it is AppleAuthenticationException && it.message == failureMessage }
            .assertNoValues()

        verify(apiInstance).loginWithAppleId(
            ArgumentMatchers.matches(token),
            ArgumentMatchers.matches(socialEmail)
        )
    }

    @Test
    fun forgotPassword_onSuccess() {
        val email = "test@email.com"

        doAnswer {
            val listener = it.arguments.last() as ForgotPasswordListener
            listener.onSuccess()
        }.`when`(apiInstance).forgotPassword(ArgumentMatchers.anyString(), any())

        repository.forgotPassword(email) { exception: AuthenticationException? ->
            Assert.assertEquals(null, exception)
        }

        verify(apiInstance).forgotPassword(ArgumentMatchers.matches(email), any())
    }

    @Test
    fun forgotPassword_onFailureWithMessageAndIsFound() {
        val email = "test@email.com"
        val failureMessage = "Stupid shit"

        doAnswer {
            val listener = it.arguments.last() as ForgotPasswordListener
            listener.onFailure(failureMessage, false)
        }.`when`(apiInstance).forgotPassword(ArgumentMatchers.anyString(), any())

        repository.forgotPassword(email) { exception: AuthenticationException? ->
            Assert.assertEquals(true, exception is ForgotPasswordException)
            Assert.assertEquals(true, exception!!.message.contains(failureMessage))
        }

        verify(apiInstance).forgotPassword(ArgumentMatchers.matches(email), any())
    }

    @Test
    fun forgotPassword_onFailureWithMessageAndIsNotFound() {
        val email = "test@email.com"
        val failureMessage = "Stupid shit"

        doAnswer {
            val listener = it.arguments.last() as ForgotPasswordListener
            listener.onFailure(failureMessage, true)
        }.`when`(apiInstance).forgotPassword(ArgumentMatchers.anyString(), any())

        repository.forgotPassword(email) { exception: AuthenticationException? ->
            Assert.assertEquals(true, exception is ForgotPasswordEmailNotFoundException)
            Assert.assertEquals(true, exception!!.message.contains("not found"))
        }

        verify(apiInstance).forgotPassword(ArgumentMatchers.matches(email), any())
    }

    @Test
    fun logout_onSuccess() {

        whenever(apiInstance.logout()).thenReturn(Completable.complete())

        repository.logout()
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(apiInstance).logout()
    }

    @Test
    fun logout_onError() {

        val exception = Exception("Error")
        whenever(apiInstance.logout()).thenReturn(Completable.error(exception))

        repository.logout()
            .test()
            .assertError(exception)
            .assertNotComplete()

        verify(apiInstance).logout()
    }

    @Test
    fun changePassword_onSuccess() {
        val oldPassword = "123"
        val newPassword = "456"
        whenever(apiInstance.changePassword(oldPassword, newPassword)).thenReturn(Completable.complete())

        repository.changePassword(oldPassword, newPassword)
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(apiInstance).changePassword(oldPassword, newPassword)
    }

    @Test
    fun changePassword_onerror() {
        val oldPassword = "123"
        val newPassword = "456"
        val exception = Exception("Error")
        whenever(apiInstance.changePassword(oldPassword, newPassword)).thenReturn(Completable.error(exception))

        repository.changePassword(oldPassword, newPassword)
            .test()
            .assertError(exception)
            .assertNotComplete()

        verify(apiInstance).changePassword(oldPassword, newPassword)
    }

    @Test
    fun verifyForgotPasswordToken_onSuccess() {
        val token = "xxx"
        whenever(apiInstance.verifyForgotPasswordToken(token)).thenReturn(Completable.complete())

        repository.verifyForgotPasswordToken(token)
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(apiInstance).verifyForgotPasswordToken(token)
    }

    @Test
    fun verifyForgotPasswordToken_onError() {
        val token = "xxx"
        val exception = Exception("Error")
        whenever(apiInstance.verifyForgotPasswordToken(token)).thenReturn(Completable.error(exception))

        repository.verifyForgotPasswordToken(token)
            .test()
            .assertError(exception)
            .assertNotComplete()

        verify(apiInstance).verifyForgotPasswordToken(token)
    }

    @Test
    fun resetPassword_onSuccess() {
        val token = "xxx"
        val newPassword = "pw"
        whenever(apiInstance.resetPassword(token, newPassword)).thenReturn(Completable.complete())

        repository.resetPassword(token, newPassword)
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(apiInstance).resetPassword(token, newPassword)
    }

    @Test
    fun resetPassword_onError() {
        val token = "xxx"
        val newPassword = "pw"
        val exception = Exception("Error")
        whenever(apiInstance.resetPassword(token, newPassword)).thenReturn(Completable.error(exception))

        repository.resetPassword(token, newPassword)
            .test()
            .assertError(exception)
            .assertNotComplete()

        verify(apiInstance).resetPassword(token, newPassword)
    }
}
