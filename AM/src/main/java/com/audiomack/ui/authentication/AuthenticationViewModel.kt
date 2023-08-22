package com.audiomack.ui.authentication

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.MainApplication
import com.audiomack.PRIVACY_POLICY_URL
import com.audiomack.R
import com.audiomack.TOS_URL
import com.audiomack.data.authentication.* // ktlint-disable no-wildcard-imports
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.socialauth.AppleAuthData
import com.audiomack.data.socialauth.FacebookAuthData
import com.audiomack.data.socialauth.GoogleAuthData
import com.audiomack.data.socialauth.SocialAuthManager
import com.audiomack.data.socialauth.SocialAuthManagerImpl
import com.audiomack.data.socialauth.TwitterAuthData
import com.audiomack.data.telco.TelcoDataSource
import com.audiomack.data.telco.TelcoRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.appsflyer.AppsFlyerDataSource
import com.audiomack.data.tracking.appsflyer.AppsFlyerRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist.Gender
import com.audiomack.model.AuthenticationType
import com.audiomack.model.Credentials
import com.audiomack.model.EventSocialEmailAdded
import com.audiomack.model.LoginSignupSource
import com.audiomack.network.API
import com.audiomack.network.APIDetailedException
import com.audiomack.network.APILoginException
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.webviewauth.WebViewAuthResult
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import io.reactivex.Single
import java.util.Date
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AuthenticationViewModel(
    private val source: LoginSignupSource,
    val profileCompletion: Boolean,
    private val api: API = API.getInstance(),
    private val authRepository: AuthenticationDataSource = AuthenticationRepository(api),
    private val trackingRepository: TrackingDataSource = TrackingRepository(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val appsFlyerDataSource: AppsFlyerDataSource = AppsFlyerRepository(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val telcoDataSource: TelcoDataSource = TelcoRepository(),
    private val socialAuthManager: SocialAuthManager = SocialAuthManagerImpl(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    var isOnline: Boolean = true

    val closeEvent = SingleLiveEvent<Void>()
    val goToChooseLoginTypeEvent = SingleLiveEvent<Void>()
    val goToSignupEvent = SingleLiveEvent<String?>()
    val goToLoginEvent = SingleLiveEvent<String?>()
    val smartlockEvent = SingleLiveEvent<Void>()
    val goToForgotPasswordEvent = SingleLiveEvent<Void>()
    val emailNotExistentEvent = SingleLiveEvent<Void>()
    val showLoaderEvent = SingleLiveEvent<Void>()
    val hideLoaderEvent = SingleLiveEvent<Void>()
    val showErrorEvent = SingleLiveEvent<String>()
    val showSupportEvent = SingleLiveEvent<Void>()
    val smartlockCredentialsEvent = SingleLiveEvent<Triple<String?, String?, Boolean>>()
    val showSocialEmailPromptEvent = SingleLiveEvent<Void>()
    val openURLEvent = SingleLiveEvent<String>()
    val focusOnEmailEvent = SingleLiveEvent<Void>()
    val showAppleWebViewEvent = SingleLiveEvent<Void>()
    val authErrorEvent = SingleLiveEvent<AuthenticationException>()
    val signupLoadingEvent = SingleLiveEvent<Boolean>()
    val credentialsEvent = SingleLiveEvent<Credentials>()
    val goToAgeGenderEvent = SingleLiveEvent<Void>()

    private var _footerVisible = MutableLiveData<Boolean>()
    val footerVisible: LiveData<Boolean> get() = _footerVisible

    private var connectedOnce = false

    // Store social auth data while asking the user to provide a valid email address
    private var pendingFacebookAuthData: FacebookAuthData? = null
    private var pendingGoogleAuthData: GoogleAuthData? = null
    private var pendingTwitterAuthData: TwitterAuthData? = null
    private var pendingAppleAuthData: AppleAuthData? = null
    private var pendingFacebookListener: AuthenticationDataSource.FacebookAuthenticationCallback? = null
    private var pendingGoogleListener: AuthenticationDataSource.GoogleAuthenticationCallback? = null
    private var pendingTwitterListener: AuthenticationDataSource.TwitterAuthenticationCallback? = null
    private var pendingAppleListener: AuthenticationDataSource.AppleAuthenticationCallback? = null

    private val _signupCredentials = MutableLiveData<SignupCredentials>()

    init {
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    fun login(email: String, password: String, listener: AuthenticationDataSource.LoginAuthenticationCallback) {

        trackingRepository.trackBreadcrumb("Email signin button tap")

        if (email.isEmpty()) {
            listener.onAuthenticationError(InvalidEmailAuthenticationException(MainApplication.context?.getString(R.string.authentication_validation_email_empty) ?: ""))
            return
        }

        if (password.isEmpty()) {
            listener.onAuthenticationError(InvalidPasswordAuthenticationException(MainApplication.context?.getString(R.string.authentication_validation_password_empty) ?: ""))
            return
        }

        trackingRepository.trackBreadcrumb("Email signin API call")

        listener.onBeforeLogin()

        compositeDisposable.add(
            authRepository.loginWithEmailPassword(email, password)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    trackingRepository.trackBreadcrumb("Email signin API success")
                    trackLogin(AuthenticationType.Email)
                    listener.onAuthenticationSuccess(it)
                }, {
                    trackingRepository.trackException(Exception("Email signin API failure", it))
                    if (!isOnline) {
                        listener.onAuthenticationError(OfflineException("Bad Connection"))
                    } else if (it is AuthenticationException) {
                        listener.onAuthenticationError(it)
                    } else if (it is APILoginException) {
                        listener.onAuthenticationError(LoginException(it.errorMessage, it.statusCode))
                    }
                })
        )
    }

    fun onSignupCredentialsSubmitted(
        username: String,
        email: String,
        password: String,
        confirmPwd: String,
        advertisingId: String?
    ) {
        trackingRepository.trackBreadcrumb("Email signup button tap")

        if (username.isEmpty()) {
            authErrorEvent.postValue(
                InvalidUsernameAuthenticationException(
                    MainApplication.context?.getString(
                        R.string.authentication_validation_username_empty
                    ) ?: ""
                )
            )
            return
        }

        if (email.isEmpty()) {
            authErrorEvent.postValue(
                InvalidEmailAuthenticationException(
                    MainApplication.context?.getString(
                        R.string.authentication_validation_email_empty
                    ) ?: ""
                )
            )
            return
        }

        if (password.isEmpty()) {
            authErrorEvent.postValue(
                InvalidPasswordAuthenticationException(
                    MainApplication.context?.getString(
                        R.string.authentication_validation_password_empty
                    ) ?: ""
                )
            )
            return
        }

        if (password != confirmPwd) {
            authErrorEvent.postValue(
                    MismatchPasswordAuthenticationException(
                            MainApplication.context?.getString(
                                    R.string.authentication_validation_password_mismatch
                            ) ?: ""
                    )
            )
            return
        }

        _signupCredentials.value = SignupCredentials(username, email, password, advertisingId)
        goToAgeGenderEvent.call()
    }

    fun onAgeGenderSubmitted(birthday: Date, gender: Gender) {
        if (profileCompletion) {
            // Profile completion (age and gender)
            completeProfile(birthday, gender)
        } else {
            // Complete signup
            _signupCredentials.value?.apply {
                this.birthday = birthday
                this.gender = gender
            }?.also {
                signup(it)
            } ?: run {
                authErrorEvent.value = SignupException(
                    MainApplication.context?.getString(R.string.generic_api_error) ?: ""
                )
            }
        }
    }

    private fun completeProfile(birthday: Date, gender: Gender) {

        trackingRepository.trackBreadcrumb("Complete profile API call")

        signupLoadingEvent.postValue(true)

        userDataSource.getUserAsync()
            .subscribeOn(schedulersProvider.io)
            .flatMapSingle { artist ->
                artist.birthday = birthday
                artist.gender = gender
                userDataSource.saveLocalArtist(artist)
            }
            .flatMapCompletable {
                userDataSource.completeProfile(it.name ?: "", birthday, gender)
            }
            .observeOn(schedulersProvider.main)
            .subscribe({
                trackingRepository.trackBreadcrumb("Complete profile API success")
                signupLoadingEvent.postValue(false)
                closeEvent.call()
            }, {
                trackingRepository.trackException(Exception("Complete profile API failure", it))
                signupLoadingEvent.postValue(false)
                if (!isOnline) {
                    val message = MainApplication.context?.getString(R.string.feature_not_available_offline_alert_message) ?: ""
                    authErrorEvent.postValue(ProfileCompletionSkippableException(message))
                } else {
                    (it as? APIDetailedException)?.message?.takeIf { it.isNotBlank() }?.let {
                        authErrorEvent.postValue(ProfileCompletionException(it))
                    } ?: run {
                        authErrorEvent.postValue(ProfileCompletionSkippableException(MainApplication.context?.getString(R.string.generic_api_error) ?: ""))
                    }
                }
            })
            .addTo(compositeDisposable)
    }

    private fun signup(signupCredentials: SignupCredentials) {

        trackingRepository.trackBreadcrumb("Email signup API call")

        signupLoadingEvent.postValue(true)

        authRepository.signup(signupCredentials) { exception, credentials ->
            exception?.let {
                trackingRepository.trackException(Exception("Email signup API failure", it))
                signupLoadingEvent.postValue(false)
                if (!isOnline) {
                    authErrorEvent.postValue(OfflineException("Bad Connection"))
                } else {
                    authErrorEvent.postValue(it)
                }
                return@signup
            }
            trackingRepository.trackBreadcrumb("Email signup API success")
            trackSignup(AuthenticationType.Email)

            signupLoadingEvent.postValue(false)
            credentialsEvent.postValue(credentials)
        }
    }

    fun loginWithFacebook(activity: Activity, listener: AuthenticationDataSource.FacebookAuthenticationCallback) {
        trackingRepository.trackBreadcrumb("Facebook signin button tap")
        compositeDisposable.add(
            socialAuthManager.authenticateWithFacebook(activity)
                .flatMapSingle {
                    if (it.missingEmail) {
                        throw FacebookMissingEmailAuthenticationException(it)
                    } else {
                        trackingRepository.trackBreadcrumb("Facebook signin API call")
                        showLoaderEvent.call()
                        loginWithFacebook(it.id, it.token, null)
                    }
                }
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    hideLoaderEvent.call()

                    trackingRepository.trackBreadcrumb("Facebook signin API success")
                    if (it.isRegisteredViaSocial) {
                        trackSignup(AuthenticationType.Facebook)
                    } else {
                        trackLogin(AuthenticationType.Facebook)
                    }

                    listener.onAuthenticationSuccess(it)
                }, {
                    hideLoaderEvent.call()
                    when (it) {
                        is FacebookMissingEmailAuthenticationException -> {
                            trackingRepository.trackException(
                                Exception("Facebook signin got no email", it)
                            )
                            pendingFacebookAuthData = it.authData
                            pendingFacebookListener = listener
                            showSocialEmailPromptEvent.call()
                        }
                        is AuthenticationException -> {
                            trackingRepository.trackException(
                                Exception("Facebook signin API failure: ${it.message}", it)
                            )
                            listener.onAuthenticationError(it)
                        }
                    }
                })
        )
    }

    private fun loginWithFacebook(userId: String, token: String, socialEmail: String?): Single<Credentials> {
        return authRepository.loginWithFacebook(userId, token, socialEmail)
    }

    fun loginWithGoogle(activity: Activity, listener: AuthenticationDataSource.GoogleAuthenticationCallback) {
        trackingRepository.trackBreadcrumb("Google signin button tap")
        compositeDisposable.add(
            socialAuthManager.authenticateWithGoogle(activity)
                .flatMapSingle {
                    if (it.missingEmail) {
                        throw GoogleMissingEmailAuthenticationException(it)
                    } else {
                        trackingRepository.trackBreadcrumb("Google signin API call")
                        showLoaderEvent.call()
                        loginWithGoogle(it.idToken, null)
                    }
                }
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    hideLoaderEvent.call()

                    trackingRepository.trackBreadcrumb("Google signin API success")
                    if (it.isRegisteredViaSocial) {
                        trackSignup(AuthenticationType.Google)
                    } else {
                        trackLogin(AuthenticationType.Google)
                    }

                    listener.onAuthenticationSuccess(it)
                }, {
                    hideLoaderEvent.call()
                    when (it) {
                        is GoogleMissingEmailAuthenticationException -> {
                            trackingRepository.trackException(
                                Exception("Google signin got no email", it)
                            )
                            pendingGoogleAuthData = it.authData
                            pendingGoogleListener = listener
                            showSocialEmailPromptEvent.call()
                        }
                        is AuthenticationException -> {
                            trackingRepository.trackException(
                                Exception("Google signin API failure: ${it.message}", it)
                            )
                            listener.onAuthenticationError(it)
                        }
                    }
                })
        )
    }

    private fun loginWithGoogle(googleToken: String, socialEmail: String?): Single<Credentials> {
        return authRepository.loginWithGoogle(googleToken, socialEmail)
    }

    fun loginWithTwitter(activity: Activity, listener: AuthenticationDataSource.TwitterAuthenticationCallback) {
        trackingRepository.trackBreadcrumb("Twitter signin button tap")
        compositeDisposable.add(
            socialAuthManager.authenticateWithTwitter(activity)
                .flatMapSingle {
                    if (it.missingEmail) {
                        throw TwitterMissingEmailAuthenticationException(it)
                    } else {
                        trackingRepository.trackBreadcrumb("Twitter signin API call")
                        showLoaderEvent.call()
                        loginWithTwitter(it.token, it.secret, null)
                    }
                }
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    hideLoaderEvent.call()

                    trackingRepository.trackBreadcrumb("Twitter signin API success")
                    if (it.isRegisteredViaSocial) {
                        trackSignup(AuthenticationType.Twitter)
                    } else {
                        trackLogin(AuthenticationType.Twitter)
                    }

                    listener.onAuthenticationSuccess(it)
                }, {
                    hideLoaderEvent.call()
                    when (it) {
                        is TwitterMissingEmailAuthenticationException -> {
                            trackingRepository.trackException(
                                Exception("Twitter signin got no email", it)
                            )
                            pendingTwitterAuthData = it.authData
                            pendingTwitterListener = listener
                            showSocialEmailPromptEvent.call()
                        }
                        is AuthenticationException -> {
                            trackingRepository.trackException(
                                Exception("Twitter signin API failure: " + it.message, it)
                            )
                            listener.onAuthenticationError(it)
                        }
                    }
                })
        )
    }

    private fun loginWithTwitter(twitterToken: String, twitterSecret: String, socialEmail: String?): Single<Credentials> {
        return authRepository.loginWithTwitter(twitterToken, twitterSecret, socialEmail)
    }

    fun onAppleButtonClicked() {
        trackingRepository.trackBreadcrumb("Apple signin button tap")
        showAppleWebViewEvent.call()
    }

    fun handleAppleSignInResult(result: WebViewAuthResult, listener: AuthenticationDataSource.AppleAuthenticationCallback) {
        when (result) {
            is WebViewAuthResult.Success -> {
                showLoaderEvent.call()
                trackingRepository.trackBreadcrumb(("Apple signin API call"))
                compositeDisposable.add(
                    loginWithAppleId(result.token, null)
                        .subscribeOn(schedulersProvider.io)
                        .observeOn(schedulersProvider.main)
                        .subscribe({
                            hideLoaderEvent.call()

                            trackingRepository.trackBreadcrumb("Apple signin API success")
                            if (it.isRegisteredViaSocial) {
                                trackSignup(AuthenticationType.Apple)
                            } else {
                                trackLogin(AuthenticationType.Apple)
                            }

                            listener.onAuthenticationSuccess(it)
                        }, {
                            hideLoaderEvent.call()
                            when (it) {
                                is AppleMissingEmailAuthenticationException -> {
                                    trackingRepository.trackException(
                                        Exception("Apple signin got no email", it)
                                    )
                                    pendingAppleAuthData = it.authData
                                    pendingAppleListener = listener
                                    showSocialEmailPromptEvent.call()
                                }
                                is AuthenticationException -> {
                                    trackingRepository.trackException(
                                        Exception("Apple signin API failure: ${it.message}", it)
                                    )
                                    listener.onAuthenticationError(it)
                                }
                            }
                        })
                )
            }
            is WebViewAuthResult.Failure -> {
                listener.onAuthenticationError(AppleAuthenticationException(result.error.localizedMessage))
            }
            is WebViewAuthResult.Cancel -> {}
        }
    }

    private fun loginWithAppleId(appleIdToken: String, socialEmail: String?): Single<Credentials> {
        return authRepository.loginWithAppleId(appleIdToken, socialEmail)
    }

    fun checkEmailExistence(email: String?) {

        if (email.isNullOrEmpty()) {
            showErrorEvent.postValue(MainApplication.context?.getString(R.string.authentication_validation_email_empty) ?: "")
            return
        }

        showLoaderEvent.call()
        compositeDisposable.add(
            authRepository.checkEmailExistence(email, null)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    hideLoaderEvent.call()
                    if (it) {
                        goToLoginEvent.postValue(email)
                    } else {
                        emailNotExistentEvent.call()
                    }
                }, {
                    hideLoaderEvent.call()
                    showErrorEvent.postValue(MainApplication.context?.getString(R.string.feature_not_available_offline_alert_message) ?: "")
                })
        )
    }

    fun trackScreen(screen: String) {
        trackingRepository.trackScreen(screen)
    }

    fun trackSignupPage() {
        mixpanelDataSource.trackViewSignupPage(source)
    }

    fun requestLoginCredentials() {
        smartlockEvent.call()
    }

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onBackTapped() {
        goToChooseLoginTypeEvent.call()
    }

    fun openChooseLoginType() {
        goToChooseLoginTypeEvent.call()
    }

    fun openSignup(email: String?) {
        goToSignupEvent.postValue(email)
    }

    fun returnToLogin() {
        goToLoginEvent.call()
    }

    fun onTOSTapped() {
        openURLEvent.postValue(TOS_URL)
    }

    fun onForgotPasswordTapped() {
        goToForgotPasswordEvent.call()
    }

    fun onEmailTitleTapped() {
        focusOnEmailEvent.call()
    }

    fun onSupportTapped() {
        showSupportEvent.call()
    }

    fun onPrivacyPolicyTapped() {
        openURLEvent.postValue(PRIVACY_POLICY_URL)
    }

    fun onCreateActivity() {
        if (profileCompletion) {
            goToAgeGenderEvent.call()
        } else {
            goToChooseLoginTypeEvent.call()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        socialAuthManager.onActivityResult(requestCode, resultCode, data)
    }

    fun onCredentialsFound(email: String?, password: String?, automatic: Boolean) {
        smartlockCredentialsEvent.postValue(Triple(email, password, automatic))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventSocialEmailAdded: EventSocialEmailAdded) {
        val socialEmail = eventSocialEmailAdded.email

        if (pendingFacebookAuthData != null && pendingFacebookListener != null) {
            compositeDisposable.add(
                loginWithFacebook(pendingFacebookAuthData!!.id, pendingFacebookAuthData!!.token, socialEmail)
                    .subscribeOn(schedulersProvider.main)
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        hideLoaderEvent.call()
                        pendingFacebookListener?.onAuthenticationSuccess(it)
                    }, {
                        hideLoaderEvent.call()
                        (it as? AuthenticationException)?.let { exception ->
                            if (exception is FacebookExistingEmailAuthenticationException) {
                                goToLoginEvent.postValue(exception.email)
                            } else {
                                pendingFacebookListener?.onAuthenticationError(exception)
                            }
                        }
                    })
            )
        } else if (pendingGoogleAuthData != null && pendingGoogleListener != null) {
            compositeDisposable.add(
                loginWithGoogle(pendingGoogleAuthData!!.idToken, socialEmail)
                    .subscribeOn(schedulersProvider.main)
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        hideLoaderEvent.call()
                        pendingGoogleListener?.onAuthenticationSuccess(it)
                    }, {
                        hideLoaderEvent.call()
                        (it as? AuthenticationException)?.let { exception ->
                            if (exception is GoogleExistingEmailAuthenticationException) {
                                goToLoginEvent.postValue(exception.email)
                            } else {
                                pendingGoogleListener?.onAuthenticationError(exception)
                            }
                        }
                    })
            )
        } else if (pendingTwitterAuthData != null && pendingTwitterListener != null) {
            compositeDisposable.add(
                loginWithTwitter(pendingTwitterAuthData!!.token, pendingTwitterAuthData!!.secret, socialEmail)
                    .subscribeOn(schedulersProvider.main)
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        hideLoaderEvent.call()
                        pendingTwitterListener?.onAuthenticationSuccess(it)
                    }, {
                        hideLoaderEvent.call()
                        (it as? AuthenticationException)?.let { exception ->
                            if (exception is TwitterExistingEmailAuthenticationException) {
                                goToLoginEvent.postValue(exception.email)
                            } else {
                                pendingTwitterListener?.onAuthenticationError(exception)
                            }
                        }
                    })
            )
        } else if (pendingAppleAuthData != null && pendingAppleListener != null) {
            compositeDisposable.add(
                loginWithAppleId(pendingAppleAuthData!!.token, socialEmail)
                    .subscribeOn(schedulersProvider.main)
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        hideLoaderEvent.call()
                        pendingAppleListener?.onAuthenticationSuccess(it)
                    }, {
                        hideLoaderEvent.call()
                        (it as? AuthenticationException)?.let { exception ->
                            if (exception is AppleExistingEmailAuthenticationException) {
                                goToLoginEvent.postValue(exception.email)
                            } else {
                                pendingAppleListener?.onAuthenticationError(exception)
                            }
                        }
                    })
            )
        }
    }

    fun onGoogleServicesConnected() {
        if (!connectedOnce) {
            requestLoginCredentials()
        }
        connectedOnce = true
    }

    fun onKeyboardVisibilityChanged(open: Boolean) {
        _footerVisible.postValue(!open)
    }

    private fun trackSignup(type: AuthenticationType) {
        mixpanelDataSource.trackCreateAccount(source, type, userDataSource, premiumDataSource)
        appsFlyerDataSource.trackUserSignup()
        trackingRepository.trackSignup()
    }

    private fun trackLogin(type: AuthenticationType) {
        mixpanelDataSource.trackLogin(
            source,
            type,
            userDataSource,
            premiumDataSource,
            telcoDataSource
        )
        trackingRepository.trackLogin()
    }
}
