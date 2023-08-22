package com.audiomack.ui.authentication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.authentication.AppleAuthenticationException
import com.audiomack.data.authentication.AppleExistingEmailAuthenticationException
import com.audiomack.data.authentication.AppleMissingEmailAuthenticationException
import com.audiomack.data.authentication.AuthenticationDataSource
import com.audiomack.data.authentication.AuthenticationException
import com.audiomack.data.authentication.FacebookAuthenticationException
import com.audiomack.data.authentication.FacebookExistingEmailAuthenticationException
import com.audiomack.data.authentication.FacebookMissingEmailAuthenticationException
import com.audiomack.data.authentication.GoogleAuthenticationException
import com.audiomack.data.authentication.GoogleExistingEmailAuthenticationException
import com.audiomack.data.authentication.GoogleMissingEmailAuthenticationException
import com.audiomack.data.authentication.InvalidEmailAuthenticationException
import com.audiomack.data.authentication.InvalidPasswordAuthenticationException
import com.audiomack.data.authentication.InvalidUsernameAuthenticationException
import com.audiomack.data.authentication.LoginException
import com.audiomack.data.authentication.MismatchPasswordAuthenticationException
import com.audiomack.data.authentication.OfflineException
import com.audiomack.data.authentication.SignupException
import com.audiomack.data.authentication.TwitterAuthenticationException
import com.audiomack.data.authentication.TwitterExistingEmailAuthenticationException
import com.audiomack.data.authentication.TwitterMissingEmailAuthenticationException
import com.audiomack.data.socialauth.AppleAuthData
import com.audiomack.data.socialauth.FacebookAuthData
import com.audiomack.data.socialauth.GoogleAuthData
import com.audiomack.data.socialauth.SocialAuthManager
import com.audiomack.data.socialauth.TwitterAuthData
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.model.AMArtist.Gender
import com.audiomack.model.Credentials
import com.audiomack.model.EventSocialEmailAdded
import com.audiomack.model.LoginSignupSource
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.webviewauth.WebViewAuthResult
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atMost
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.Single
import java.util.Date
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@Suppress("UNCHECKED_CAST")
class AuthenticationViewModelTest {

    @Mock
    private lateinit var authenticationDataSource: AuthenticationDataSource

    @Mock
    private lateinit var socialAuthManager: SocialAuthManager

    @Mock
    private lateinit var trackingRepository: TrackingDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    private lateinit var source: LoginSignupSource

    private lateinit var viewModel: AuthenticationViewModel
    private lateinit var viewModelWithProfileCompletion: AuthenticationViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        source = LoginSignupSource.MyLibrary
        viewModel = AuthenticationViewModel(
            source,
            false,
            mock(),
            authenticationDataSource,
            trackingRepository,
            mixpanelDataSource,
            mock(),
            mock(),
            mock(),
            mock(),
            socialAuthManager,
            TestSchedulersProvider()
        )
        viewModelWithProfileCompletion = AuthenticationViewModel(
            source,
            true,
            mock(),
            authenticationDataSource,
            trackingRepository,
            mixpanelDataSource,
            mock(),
            mock(),
            mock(),
            mock(),
            socialAuthManager,
            TestSchedulersProvider()
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `onCreateActivity shows login choice`() {
        val observer: Observer<Void> = mock()
        viewModel.goToChooseLoginTypeEvent.observeForever(observer)
        viewModel.onCreateActivity()
        verify(observer).onChanged(null)
    }

    @Test
    fun `onCreateActivity shows age and gender`() {
        val observer: Observer<Void> = mock()
        viewModelWithProfileCompletion.goToAgeGenderEvent.observeForever(observer)
        viewModelWithProfileCompletion.onCreateActivity()
        verify(observer).onChanged(null)
    }

    @Test
    fun back() {
        val observer: Observer<Void> = mock()
        viewModel.goToChooseLoginTypeEvent.observeForever(observer)
        viewModel.onBackTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `open signup`() {
        val observer: Observer<String?> = mock()
        viewModel.goToSignupEvent.observeForever(observer)
        viewModel.openSignup(anyOrNull())
        verify(observer).onChanged(null)
    }

    @Test
    fun `open login`() {
        val observer: Observer<String?> = mock()
        viewModel.goToLoginEvent.observeForever(observer)
        viewModel.returnToLogin()
        verify(observer).onChanged(null)
    }

    @Test
    fun smartlock() {
        val observer: Observer<Void> = mock()
        viewModel.smartlockEvent.observeForever(observer)
        viewModel.requestLoginCredentials()
        verify(observer).onChanged(null)
    }

    @Test
    fun `onGoogleServices connected`() {
        val observer: Observer<Void> = mock()
        viewModel.smartlockEvent.observeForever(observer)
        viewModel.onGoogleServicesConnected()
        verify(observer).onChanged(null)
    }

    @Test
    fun `onGoogleServices connected more than once`() {
        val observer: Observer<Void> = mock()
        viewModel.smartlockEvent.observeForever(observer)
        viewModel.onGoogleServicesConnected()
        viewModel.onGoogleServicesConnected()
        viewModel.onGoogleServicesConnected()
        verify(observer, times(1)).onChanged(null)
    }

    @Test
    fun `terms of service`() {
        val observer: Observer<String> = mock()
        viewModel.openURLEvent.observeForever(observer)
        viewModel.onTOSTapped()
        verify(observer).onChanged(any())
    }

    @Test
    fun `forgot password`() {
        val observer: Observer<Void> = mock()
        viewModel.goToForgotPasswordEvent.observeForever(observer)
        viewModel.onForgotPasswordTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `contact us`() {
        val observerSupport: Observer<Void> = mock()
        viewModel.showSupportEvent.observeForever(observerSupport)
        viewModel.onSupportTapped()
        verify(observerSupport).onChanged(null)
    }

    @Test
    fun `privacy policy`() {
        val observerOpenURL: Observer<String> = mock()
        viewModel.openURLEvent.observeForever(observerOpenURL)
        viewModel.onPrivacyPolicyTapped()
        verify(observerOpenURL).onChanged(any())
    }

    @Test
    fun `login with no email fails`() {
        val listener: AuthenticationDataSource.LoginAuthenticationCallback = mock()
        viewModel.isOnline = true
        viewModel.login("", "password", listener)
        verify(listener).onAuthenticationError(any<InvalidEmailAuthenticationException>())
        verify(listener, never()).onAuthenticationError(any<InvalidPasswordAuthenticationException>())
        verify(listener, never()).onAuthenticationError(any<OfflineException>())
        verify(listener, never()).onBeforeLogin()
        verify(
            trackingRepository,
            times(1)
        ).trackBreadcrumb(argWhere { it.contains("Email signin") })
    }

    @Test
    fun `login with no password fails`() {
        val listener: AuthenticationDataSource.LoginAuthenticationCallback = mock()
        viewModel.isOnline = true
        viewModel.login("email", "", listener)
        verify(listener).onAuthenticationError(any<InvalidPasswordAuthenticationException>())
        verify(listener, never()).onAuthenticationError(any<InvalidEmailAuthenticationException>())
        verify(listener, never()).onAuthenticationError(any<OfflineException>())
        verify(listener, never()).onBeforeLogin()
        verify(
            trackingRepository,
            times(1)
        ).trackBreadcrumb(argWhere { it.contains("Email signin") })
    }

    @Test
    fun `login in offline mode does not fail immediately`() {
        val listener: AuthenticationDataSource.LoginAuthenticationCallback = mock()
        whenever(authenticationDataSource.loginWithEmailPassword(any(), any())).thenReturn(Single.just(
            Credentials()
        ))

        viewModel.isOnline = false
        viewModel.login("email", "password", listener)
        verify(listener, never()).onAuthenticationError(any<OfflineException>())
        verify(listener).onBeforeLogin()
    }

    @Test
    fun `login succeeds`() {
        val listener: AuthenticationDataSource.LoginAuthenticationCallback = mock()
        whenever(authenticationDataSource.loginWithEmailPassword(any(), any())).thenReturn(Single.just(Credentials()))
        viewModel.isOnline = true
        viewModel.login("email", "password", listener)
        verify(listener, times(1)).onBeforeLogin()
        verify(listener, times(0)).onAuthenticationError(any())
        verify(listener, times(1)).onAuthenticationSuccess(any())
    }

    @Test
    fun `login fails`() {
        val listener: AuthenticationDataSource.LoginAuthenticationCallback = mock()
        whenever(authenticationDataSource.loginWithEmailPassword(any(), any())).thenReturn(Single.error(LoginException("", null)))
        viewModel.isOnline = true
        viewModel.login("email", "password", listener)
        verify(listener, times(1)).onBeforeLogin()
        verify(listener, times(1)).onAuthenticationError(any())
        verify(listener, times(0)).onAuthenticationSuccess(any())
    }

    @Test
    fun `signup with no username fails`() {
        val errorObserver = mock<Observer<in AuthenticationException>>()
        viewModel.authErrorEvent.observeForever(errorObserver)
        val goToAgeGenderObserver = mock<Observer<Void>>()
        viewModel.goToAgeGenderEvent.observeForever(goToAgeGenderObserver)

        viewModel.onSignupCredentialsSubmitted(
                "",
                "email",
                "password",
                "password",
                null)

        verify(errorObserver, times(1)).onChanged(any<InvalidUsernameAuthenticationException>())
        verifyZeroInteractions(goToAgeGenderObserver)
    }

    @Test
    fun `signup with no email fails`() {
        val errorObserver = mock<Observer<in AuthenticationException>>()
        viewModel.authErrorEvent.observeForever(errorObserver)
        val goToAgeGenderObserver = mock<Observer<Void>>()
        viewModel.goToAgeGenderEvent.observeForever(goToAgeGenderObserver)

        viewModel.onSignupCredentialsSubmitted(
                "username",
                "",
                "password",
                "password",
                null)

        verify(errorObserver, times(1)).onChanged(any<InvalidEmailAuthenticationException>())
        verifyZeroInteractions(goToAgeGenderObserver)
    }

    @Test
    fun `signup with no password fails`() {
        val errorObserver = mock<Observer<in AuthenticationException>>()
        viewModel.authErrorEvent.observeForever(errorObserver)
        val goToAgeGenderObserver = mock<Observer<Void>>()
        viewModel.goToAgeGenderEvent.observeForever(goToAgeGenderObserver)

        viewModel.onSignupCredentialsSubmitted(
                "username",
                "email",
                "",
                "",
                null)

        verify(errorObserver, times(1)).onChanged(any<InvalidPasswordAuthenticationException>())
        verifyZeroInteractions(goToAgeGenderObserver)
    }

    @Test
    fun `signup with mismatched passwords fails`() {
        val errorObserver = mock<Observer<in AuthenticationException>>()
        viewModel.authErrorEvent.observeForever(errorObserver)
        val goToAgeGenerObserver = mock<Observer<Void>>()
        viewModel.goToAgeGenderEvent.observeForever(goToAgeGenerObserver)

        viewModel.onSignupCredentialsSubmitted(
                "username",
                "email",
                "password",
                "password2",
                null)

        verify(errorObserver, times(1)).onChanged(any<MismatchPasswordAuthenticationException>())
        verifyZeroInteractions(goToAgeGenerObserver)
    }

    @Test
    fun `signup credentials created`() {
        val errorObserver = mock<Observer<in AuthenticationException>>()
        viewModel.authErrorEvent.observeForever(errorObserver)
        val goToAgeGenderObserver = mock<Observer<Void>>()
        viewModel.goToAgeGenderEvent.observeForever(goToAgeGenderObserver)

        viewModel.onSignupCredentialsSubmitted(
                "username",
                "email",
                "password",
                "password",
                null)

        verify(goToAgeGenderObserver, times(1)).onChanged(null)
        verifyZeroInteractions(errorObserver)
    }

    @Test
    fun `signup success on age and gender submitted`() {
        val errorObserver = mock<Observer<in AuthenticationException>>()
        viewModel.authErrorEvent.observeForever(errorObserver)

        val loadingObserver = mock<Observer<in Boolean>>()
        viewModel.signupLoadingEvent.observeForever(loadingObserver)

        val credentials = mock<Credentials>()

        val credentialsObserver = mock<Observer<in Credentials>>()
        viewModel.credentialsEvent.observeForever(credentialsObserver)

        viewModel.onSignupCredentialsSubmitted(
                "username",
                "email",
                "password",
                "password",
                null)
        viewModel.onAgeGenderSubmitted(Date(), Gender.NON_BINARY)

        val argCaptor = argumentCaptor<(AuthenticationException?, Credentials?) -> Unit>()
        verify(authenticationDataSource).signup(any(), argCaptor.capture())
        argCaptor.firstValue.invoke(null, credentials)

        verify(loadingObserver, times(1)).onChanged(true)
        verify(authenticationDataSource, times(1)).signup(any(), any())
        verify(loadingObserver, times(1)).onChanged(false)
        verify(credentialsObserver, times(1)).onChanged(credentials)
        verifyZeroInteractions(errorObserver)
    }

    @Test
    fun `signup error on age and gender submitted`() {
        val errorObserver = mock<Observer<in AuthenticationException>>()
        viewModel.authErrorEvent.observeForever(errorObserver)

        val loadingObserver = mock<Observer<in Boolean>>()
        viewModel.signupLoadingEvent.observeForever(loadingObserver)

        val credentialsObserver = mock<Observer<in Credentials>>()
        viewModel.credentialsEvent.observeForever(credentialsObserver)

        viewModel.onSignupCredentialsSubmitted(
                "username",
                "email",
                "password",
                "password",
                null)
        viewModel.onAgeGenderSubmitted(Date(), Gender.NON_BINARY)

        val signupException = SignupException("")

        val argCaptor = argumentCaptor<(AuthenticationException?, Credentials?) -> Unit>()
        verify(authenticationDataSource).signup(any(), argCaptor.capture())
        argCaptor.firstValue.invoke(signupException, null)

        verify(loadingObserver, times(1)).onChanged(true)
        verify(authenticationDataSource, times(1)).signup(any(), any())
        verify(loadingObserver, times(1)).onChanged(false)
        verify(errorObserver, times(1)).onChanged(signupException)
        verify(trackingRepository, times(1)).trackException(any())
        verifyZeroInteractions(credentialsObserver)
    }

    @Test
    fun `login with facebook succeeds`() {
        whenever(authenticationDataSource.loginWithFacebook(any(), any(), anyOrNull())).thenReturn(
            Single.just(Credentials()))
        whenever(socialAuthManager.authenticateWithFacebook(any())).thenReturn(Observable.just(
            FacebookAuthData("id", "token", false)
        ))
        val listener: AuthenticationDataSource.FacebookAuthenticationCallback = mock()
        viewModel.loginWithFacebook(mock(), listener)
        verify(listener).onAuthenticationSuccess(anyOrNull())
        verify(listener, times(0)).onAuthenticationError(any())
    }

    @Test
    fun `login with facebook fails due to social auth`() {
        whenever(socialAuthManager.authenticateWithFacebook(any())).thenReturn(
            Observable.error(FacebookAuthenticationException("Really bad error"))
        )
        val listener: AuthenticationDataSource.FacebookAuthenticationCallback = mock()
        viewModel.loginWithFacebook(mock(), listener)
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(listener, atMost(1)).onAuthenticationError(any())
        verify(trackingRepository, times(1)).trackException(argWhere { it.message!!.contains("Facebook") })
    }

    @Test
    fun `login with facebook fails due to api`() {
        whenever(authenticationDataSource.loginWithFacebook(any(), any(), anyOrNull())).thenReturn(Single.error(FacebookAuthenticationException("API error")))
        whenever(socialAuthManager.authenticateWithFacebook(any())).thenReturn(Observable.just(
            FacebookAuthData("id", "token", false)
        ))
        val listener: AuthenticationDataSource.FacebookAuthenticationCallback = mock()
        viewModel.loginWithFacebook(mock(), listener)
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(listener, atMost(1)).onAuthenticationError(any())
        verify(
            trackingRepository,
            times(1)
        ).trackException(argWhere { it.message!!.contains("Facebook") })
    }

    @Test
    fun `login with facebook, no email, type email, email already existent`() {
        val email = "matteo@audiomack.com"

        whenever(authenticationDataSource.loginWithFacebook(any(), any(), anyOrNull())).thenReturn(Single.error(FacebookMissingEmailAuthenticationException(
            FacebookAuthData("id", "token", true))))
        whenever(socialAuthManager.authenticateWithFacebook(any())).thenReturn(Observable.just(FacebookAuthData("id", "token", false)))
        val listener: AuthenticationDataSource.FacebookAuthenticationCallback = mock()

        val observerShowSocialEmailPrompt: Observer<Void> = mock()
        val observerGoToLogin: Observer<String?> = mock()
        viewModel.showSocialEmailPromptEvent.observeForever(observerShowSocialEmailPrompt)
        viewModel.goToLoginEvent.observeForever(observerGoToLogin)
        viewModel.loginWithFacebook(mock(), listener)
        verify(observerShowSocialEmailPrompt).onChanged(null)

        whenever(authenticationDataSource.loginWithFacebook(any(), any(), anyOrNull())).thenReturn(Single.error(FacebookExistingEmailAuthenticationException("matteo@audiomack.com")))
        viewModel.onMessageEvent(EventSocialEmailAdded(email))
        verify(observerGoToLogin).onChanged(eq(email))
    }

    @Test
    fun `login with facebook, no email`() {
        whenever(authenticationDataSource.loginWithFacebook(any(), any(), anyOrNull())).thenReturn(Single.error(FacebookMissingEmailAuthenticationException(
            FacebookAuthData("id", "token", true)
        )))
        whenever(socialAuthManager.authenticateWithFacebook(any())).thenReturn(Observable.just(FacebookAuthData("id", "token", false)))
        val listener: AuthenticationDataSource.FacebookAuthenticationCallback = mock()
        val observerShowSocialEmailPrompt: Observer<Void> = mock()
        viewModel.showSocialEmailPromptEvent.observeForever(observerShowSocialEmailPrompt)
        viewModel.loginWithFacebook(mock(), listener)
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(listener, times(0)).onAuthenticationError(any())
        verify(
            trackingRepository,
            times(1)
        ).trackException(argWhere { it.message!!.contains("Facebook") })
        verify(observerShowSocialEmailPrompt).onChanged(null)
    }

    @Test
    fun `login with google succeeds`() {
        whenever(authenticationDataSource.loginWithGoogle(any(), anyOrNull())).thenReturn(Single.just(Credentials()))
        whenever(socialAuthManager.authenticateWithGoogle(any())).thenReturn(Observable.just(
            GoogleAuthData("idToken", false)
        ))
        val listener: AuthenticationDataSource.GoogleAuthenticationCallback = mock()
        viewModel.loginWithGoogle(mock(), listener)
        verify(listener).onAuthenticationSuccess(anyOrNull())
        verify(listener, times(0)).onAuthenticationError(any())
    }

    @Test
    fun `login with google fails due to social auth`() {
        whenever(socialAuthManager.authenticateWithGoogle(any())).thenReturn(
            Observable.error(
                GoogleAuthenticationException("Really bad error")
            )
        )
        val listener: AuthenticationDataSource.GoogleAuthenticationCallback = mock()
        viewModel.loginWithGoogle(mock(), listener)
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(listener, atMost(1)).onAuthenticationError(any())
        verify(trackingRepository, times(1)).trackException(argWhere { it.message!!.contains("Google") })
    }

    @Test
    fun `login with google, no email`() {
        whenever(authenticationDataSource.loginWithGoogle(any(), anyOrNull())).thenReturn(Single.error(GoogleMissingEmailAuthenticationException(GoogleAuthData("idToken", true))))
        whenever(socialAuthManager.authenticateWithGoogle(any())).thenReturn(Observable.just(
            GoogleAuthData("idToken", false)
        ))
        val listener: AuthenticationDataSource.GoogleAuthenticationCallback = mock()
        val observerShowSocialEmailPrompt: Observer<Void> = mock()
        viewModel.showSocialEmailPromptEvent.observeForever(observerShowSocialEmailPrompt)
        viewModel.loginWithGoogle(mock(), listener)
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(listener, atMost(1)).onAuthenticationError(any())
        verify(
            trackingRepository,
            times(1)
        ).trackException(argWhere { it.message!!.contains("Google") })
        verify(observerShowSocialEmailPrompt).onChanged(null)
    }

    @Test
    fun `login with google fails due to api`() {
        whenever(authenticationDataSource.loginWithGoogle(any(), anyOrNull())).thenReturn(Single.error(GoogleAuthenticationException("API error")))
        whenever(socialAuthManager.authenticateWithGoogle(any())).thenReturn(Observable.just(
            GoogleAuthData("idToken", false)
        ))
        val listener: AuthenticationDataSource.GoogleAuthenticationCallback = mock()
        viewModel.loginWithGoogle(mock(), listener)
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(listener, atMost(1)).onAuthenticationError(any())
        verify(
            trackingRepository,
            times(1)
        ).trackException(argWhere { it.message!!.contains("Google") })
    }

    @Test
    fun `login with google, no email, type email, email already existent`() {
        val email = "matteo@audiomack.com"

        whenever(authenticationDataSource.loginWithGoogle(any(), anyOrNull())).thenReturn(Single.error(GoogleMissingEmailAuthenticationException(GoogleAuthData("idToken", true))))
        whenever(socialAuthManager.authenticateWithGoogle(any())).thenReturn(Observable.just(
            GoogleAuthData("idToken", false)
        ))
        val listener: AuthenticationDataSource.GoogleAuthenticationCallback = mock()

        val observerShowSocialEmailPrompt: Observer<Void> = mock()
        val observerGoToLogin: Observer<String?> = mock()
        viewModel.showSocialEmailPromptEvent.observeForever(observerShowSocialEmailPrompt)
        viewModel.goToLoginEvent.observeForever(observerGoToLogin)

        viewModel.loginWithGoogle(mock(), listener)
        verify(observerShowSocialEmailPrompt).onChanged(null)

        whenever(authenticationDataSource.loginWithGoogle(any(), anyOrNull())).thenReturn(Single.error(GoogleExistingEmailAuthenticationException(email)))
        viewModel.onMessageEvent(EventSocialEmailAdded(email))
        verify(observerGoToLogin).onChanged(eq(email))
    }

    @Test
    fun `login with twitter succeeds`() {
        whenever(authenticationDataSource.loginWithTwitter(any(), any(), anyOrNull())).thenReturn(Single.just(Credentials()))
        whenever(socialAuthManager.authenticateWithTwitter(any())).thenReturn(Observable.just(
            TwitterAuthData("token", "secret", false)
        ))
        val listener: AuthenticationDataSource.TwitterAuthenticationCallback = mock()
        viewModel.loginWithTwitter(mock(), listener)
        verify(listener).onAuthenticationSuccess(anyOrNull())
        verify(listener, times(0)).onAuthenticationError(any())
    }

    @Test
    fun `login with twitter fails due to social auth`() {
        whenever(socialAuthManager.authenticateWithTwitter(any())).thenReturn(
            Observable.error(TwitterAuthenticationException("Really bad error"))
        )
        val listener: AuthenticationDataSource.TwitterAuthenticationCallback = mock()
        viewModel.loginWithTwitter(mock(), listener)
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(listener, atMost(1)).onAuthenticationError(any())
        verify(trackingRepository, times(1)).trackException(argWhere { it.message!!.contains("Twitter") })
    }

    @Test
    fun `login with twitter, no email`() {
        whenever(authenticationDataSource.loginWithTwitter(any(), any(), anyOrNull())).thenReturn(Single.error(TwitterMissingEmailAuthenticationException(
            TwitterAuthData("token", "secret", true)
        )))
        whenever(socialAuthManager.authenticateWithTwitter(any())).thenReturn(Observable.just(
            TwitterAuthData("token", "secret", false)
        ))
        val listener: AuthenticationDataSource.TwitterAuthenticationCallback = mock()
        val observerShowSocialEmailPrompt: Observer<Void> = mock()
        viewModel.showSocialEmailPromptEvent.observeForever(observerShowSocialEmailPrompt)
        viewModel.loginWithTwitter(mock(), listener)
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(listener, atMost(1)).onAuthenticationError(any())
        verify(
            trackingRepository,
            times(1)
        ).trackException(argWhere { it.message!!.contains("Twitter") })
        verify(observerShowSocialEmailPrompt).onChanged(null)
    }

    @Test
    fun `login with twitter fails due to api`() {
        whenever(authenticationDataSource.loginWithTwitter(any(), any(), anyOrNull())).thenReturn(Single.error(TwitterAuthenticationException("API error")))
        whenever(socialAuthManager.authenticateWithTwitter(any())).thenReturn(Observable.just(
            TwitterAuthData("token", "secret", false)
        ))
        val listener: AuthenticationDataSource.TwitterAuthenticationCallback = mock()
        viewModel.loginWithTwitter(mock(), listener)
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(listener, atMost(1)).onAuthenticationError(any())
        verify(
            trackingRepository,
            times(1)
        ).trackException(argWhere { it.message!!.contains("Twitter") })
    }

    @Test
    fun `login with twitter, no email, type email, email already existent`() {
        val email = "matteo@audiomack.com"

        whenever(authenticationDataSource.loginWithTwitter(any(), any(), anyOrNull())).thenReturn(Single.error(TwitterMissingEmailAuthenticationException(
            TwitterAuthData("token", "secret", true)
        )))
        whenever(socialAuthManager.authenticateWithTwitter(any())).thenReturn(Observable.just(
            TwitterAuthData("token", "secret", false)
        ))
        val listener: AuthenticationDataSource.TwitterAuthenticationCallback = mock()

        val observerShowSocialEmailPrompt: Observer<Void> = mock()
        val observerGoToLogin: Observer<String?> = mock()
        viewModel.showSocialEmailPromptEvent.observeForever(observerShowSocialEmailPrompt)
        viewModel.goToLoginEvent.observeForever(observerGoToLogin)

        viewModel.loginWithTwitter(mock(), listener)
        verify(observerShowSocialEmailPrompt).onChanged(null)

        whenever(authenticationDataSource.loginWithTwitter(any(), any(), anyOrNull())).thenReturn(Single.error(TwitterExistingEmailAuthenticationException(email)))
        viewModel.onMessageEvent(EventSocialEmailAdded(email))
        verify(observerGoToLogin).onChanged(eq(email))
    }

    @Test
    fun `sign in with apple click observed`() {
        val observer: Observer<Void> = mock()
        viewModel.showAppleWebViewEvent.observeForever(observer)

        viewModel.onAppleButtonClicked()

        verify(observer).onChanged(null)
    }

    @Test
    fun `sign in with apple fails`() {
        val listener: AuthenticationDataSource.AppleAuthenticationCallback = mock()
        viewModel.onAppleButtonClicked()
        viewModel.handleAppleSignInResult(
            WebViewAuthResult.Failure(AppleAuthenticationException("Test")),
            listener
        )
        verify(listener).onAuthenticationError(anyOrNull())
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(trackingRepository, times(1)).trackBreadcrumb(argWhere { it.contains("Apple") })
    }

    @Test
    fun `sign in with apple succeeds`() {
        whenever(authenticationDataSource.loginWithAppleId(any(), anyOrNull())).thenReturn(Single.just(
            Credentials()
        ))
        val listener: AuthenticationDataSource.AppleAuthenticationCallback = mock()
        viewModel.onAppleButtonClicked()
        viewModel.handleAppleSignInResult(WebViewAuthResult.Success("token"), listener)
        verify(listener).onAuthenticationSuccess(anyOrNull())
        verify(listener, times(0)).onAuthenticationError(any())
    }

    @Test
    fun `sign in with apple, no email`() {
        whenever(authenticationDataSource.loginWithAppleId(any(), anyOrNull())).thenReturn(Single.error(AppleMissingEmailAuthenticationException(
            AppleAuthData("token")
        )))
        val listener: AuthenticationDataSource.AppleAuthenticationCallback = mock()
        val observerShowSocialEmailPrompt: Observer<Void> = mock()
        viewModel.showSocialEmailPromptEvent.observeForever(observerShowSocialEmailPrompt)
        viewModel.onAppleButtonClicked()
        viewModel.handleAppleSignInResult(WebViewAuthResult.Success("token"), listener)
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(listener, atMost(1)).onAuthenticationError(any())
        verify(
            trackingRepository,
            times(1)
        ).trackException(argWhere { it.message!!.contains("Apple") })
        verify(observerShowSocialEmailPrompt).onChanged(null)
    }

    @Test
    fun `sign in with apple fails due to api`() {
        whenever(authenticationDataSource.loginWithAppleId(any(), anyOrNull())).thenReturn(Single.error(AppleAuthenticationException("API error")))
        val listener: AuthenticationDataSource.AppleAuthenticationCallback = mock()
        viewModel.onAppleButtonClicked()
        viewModel.handleAppleSignInResult(WebViewAuthResult.Success("token"), listener)
        verify(listener, times(0)).onAuthenticationSuccess(anyOrNull())
        verify(listener, atMost(1)).onAuthenticationError(any())
        verify(
            trackingRepository,
            times(1)
        ).trackException(argWhere { it.message!!.contains("Apple") })
    }

    @Test
    fun `sign in with apple, no email, type email, email already existent`() {
        val email = "matteo@audiomack.com"

        whenever(authenticationDataSource.loginWithAppleId(any(), anyOrNull())).thenReturn(Single.error(AppleMissingEmailAuthenticationException(
            AppleAuthData("token")
        )))
        val listener: AuthenticationDataSource.AppleAuthenticationCallback = mock()

        val observerShowSocialEmailPrompt: Observer<Void> = mock()
        val observerGoToLogin: Observer<String?> = mock()
        viewModel.showSocialEmailPromptEvent.observeForever(observerShowSocialEmailPrompt)
        viewModel.goToLoginEvent.observeForever(observerGoToLogin)

        viewModel.onAppleButtonClicked()
        viewModel.handleAppleSignInResult(WebViewAuthResult.Success("token"), listener)
        verify(observerShowSocialEmailPrompt).onChanged(null)

        whenever(authenticationDataSource.loginWithAppleId(any(), anyOrNull())).thenReturn(Single.error(AppleExistingEmailAuthenticationException(email)))
        viewModel.onMessageEvent(EventSocialEmailAdded(email))
        verify(observerGoToLogin).onChanged(eq(email))
    }

    @Test
    fun `email existent`() {
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowError: Observer<String?> = mock()
        val observerOpenLogin: Observer<String?> = mock()

        val EMAIL = "info@audiomack.com"

        whenever(authenticationDataSource.checkEmailExistence(anyOrNull(), anyOrNull())).thenReturn(Observable.just(true))

        viewModel.showLoaderEvent.observeForever(observerShowLoading)
        viewModel.hideLoaderEvent.observeForever(observerHideLoading)
        viewModel.showErrorEvent.observeForever(observerShowError)
        viewModel.goToLoginEvent.observeForever(observerOpenLogin)
        viewModel.checkEmailExistence(EMAIL)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verify(observerOpenLogin).onChanged(EMAIL)
        verifyZeroInteractions(observerShowError)
    }

    @Test
    fun `email not existent`() {
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowError: Observer<String?> = mock()
        val observerOpenLogin: Observer<String?> = mock()
        val observerEmailNotExistent: Observer<Void> = mock()

        val EMAIL = "info@audiomack.com"

        whenever(authenticationDataSource.checkEmailExistence(anyOrNull(), anyOrNull())).thenReturn(Observable.just(false))

        viewModel.showLoaderEvent.observeForever(observerShowLoading)
        viewModel.hideLoaderEvent.observeForever(observerHideLoading)
        viewModel.showErrorEvent.observeForever(observerShowError)
        viewModel.goToLoginEvent.observeForever(observerOpenLogin)
        viewModel.emailNotExistentEvent.observeForever(observerEmailNotExistent)
        viewModel.checkEmailExistence(EMAIL)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verify(observerEmailNotExistent).onChanged(null)
        verifyZeroInteractions(observerOpenLogin)
        verifyZeroInteractions(observerShowError)
    }

    @Test
    fun `email check fails`() {
        val observerShowLoading: Observer<Void> = mock()
        val observerHideLoading: Observer<Void> = mock()
        val observerShowError: Observer<String?> = mock()
        val observerOpenLogin: Observer<String?> = mock()
        val observerEmailNotExistent: Observer<Void> = mock()

        val EMAIL = "info@audiomack.com"

        whenever(authenticationDataSource.checkEmailExistence(anyOrNull(), anyOrNull())).thenReturn(Observable.error(Exception("Test")))

        viewModel.showLoaderEvent.observeForever(observerShowLoading)
        viewModel.hideLoaderEvent.observeForever(observerHideLoading)
        viewModel.showErrorEvent.observeForever(observerShowError)
        viewModel.goToLoginEvent.observeForever(observerOpenLogin)
        viewModel.emailNotExistentEvent.observeForever(observerEmailNotExistent)
        viewModel.checkEmailExistence(EMAIL)
        verify(observerShowLoading).onChanged(null)
        verify(observerHideLoading).onChanged(null)
        verifyZeroInteractions(observerEmailNotExistent)
        verifyZeroInteractions(observerOpenLogin)
        verify(observerShowError).onChanged(anyOrNull())
    }

    @Test
    fun `smartlock finds valid credentials`() {
        val email = "email@audiomack.com"
        val password = "123"
        val autoamtic = true
        val observer: Observer<Triple<String?, String?, Boolean>> = mock()
        viewModel.onCredentialsFound(email, password, autoamtic)
        viewModel.smartlockCredentialsEvent.observeForever(observer)
        verify(observer).onChanged(argWhere { it.first == email && it.second == password && it.third == autoamtic })
    }

    @Test
    fun `on social email added, no pending actions`() {
        viewModel.onMessageEvent(EventSocialEmailAdded("aaa"))
        verifyZeroInteractions(authenticationDataSource)
        verifyZeroInteractions(authenticationDataSource)
        verifyZeroInteractions(authenticationDataSource)
    }

    @Test
    fun `keyboard visibility updates footer visibility`() {
        val keyboardVisible = true
        val observerFooterVisible: Observer<Boolean> = mock()
        viewModel.footerVisible.observeForever(observerFooterVisible)
        viewModel.onKeyboardVisibilityChanged(keyboardVisible)
        verify(observerFooterVisible).onChanged(eq(!keyboardVisible))
    }

    @Test
    fun `keyboard focus on title tap`() {
        val observer: Observer<Void> = mock()
        viewModel.focusOnEmailEvent.observeForever(observer)
        viewModel.onEmailTitleTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `track signup page`() {
        viewModel.trackSignupPage()
        verify(mixpanelDataSource, times(1)).trackViewSignupPage(source)
    }
}
