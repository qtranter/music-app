package com.audiomack.ui.authentication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.audiomack.PRIVACY_POLICY_URL
import com.audiomack.R
import com.audiomack.TOS_URL
import com.audiomack.activities.BaseActivity
import com.audiomack.data.authentication.AuthenticationDataSource
import com.audiomack.data.authentication.AuthenticationException
import com.audiomack.data.authentication.FacebookTimeoutAuthenticationException
import com.audiomack.data.authentication.InvalidEmailAuthenticationException
import com.audiomack.data.authentication.InvalidPasswordAuthenticationException
import com.audiomack.data.authentication.InvalidUsernameAuthenticationException
import com.audiomack.data.authentication.MismatchPasswordAuthenticationException
import com.audiomack.data.authentication.OfflineException
import com.audiomack.data.authentication.ProfileCompletionException
import com.audiomack.data.authentication.ProfileCompletionSkippableException
import com.audiomack.data.authentication.SignupException
import com.audiomack.data.authentication.TimeoutAuthenticationException
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.keyboard.KeyboardDetector
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist.Gender
import com.audiomack.model.Credentials
import com.audiomack.model.LoginSignupSource
import com.audiomack.ui.authentication.forgotpw.AuthenticationForgotPasswordAlertFragment
import com.audiomack.ui.authentication.validation.AuthSignupValidationFragment
import com.audiomack.ui.authentication.validation.AuthSignupValidationViewModel
import com.audiomack.ui.authentication.validation.AuthSignupValidationViewModel.ValidationException
import com.audiomack.ui.widget.AudiomackWidget
import com.audiomack.utils.isReady
import com.audiomack.utils.openUrlExcludingAudiomack
import com.audiomack.views.AMProgressHUD
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.common.api.CommonStatusCodes
import java.util.Date
import kotlinx.android.synthetic.main.activity_authentication.buttonBack
import kotlinx.android.synthetic.main.activity_authentication.buttonClose
import kotlinx.android.synthetic.main.activity_authentication.layout
import timber.log.Timber

class AuthenticationActivity : BaseActivity(), AuthenticationDataSource.FacebookAuthenticationCallback, AuthenticationDataSource.GoogleAuthenticationCallback, AuthenticationDataSource.TwitterAuthenticationCallback, AuthenticationDataSource.AppleAuthenticationCallback {

    companion object {
        const val REQ_CODE_SAVE_CREDENTIALS = 200
        private const val REQ_CODE_CREDENTIALS_RESOLUTION = 201
        private const val EXTRA_SOURCE = "source"
        private const val EXTRA_PROFILE_COMPLETION = "profile_completion"

        @JvmStatic
        fun show(context: Context?, source: LoginSignupSource, flags: Int? = null, profileCompletion: Boolean = false) {
            context?.let {
                val intent = Intent(it, AuthenticationActivity::class.java)
                intent.putExtra(EXTRA_SOURCE, source)
                flags?.let { f ->
                    intent.setFlags(f)
                }
                intent.putExtra(EXTRA_PROFILE_COMPLETION, profileCompletion)
                it.startActivity(intent)
            }
        }
    }

    lateinit var source: LoginSignupSource
    lateinit var viewModel: AuthenticationViewModel

    private val validationViewModel: AuthSignupValidationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        source = if (intent.hasExtra(EXTRA_SOURCE)) {
            intent.getSerializableExtra(EXTRA_SOURCE) as LoginSignupSource
        } else {
            LoginSignupSource.AppLaunch
        }

        viewModel = ViewModelProvider(this, AuthenticationViewModelFactory(source, intent.getBooleanExtra(EXTRA_PROFILE_COMPLETION, false)))
            .get(AuthenticationViewModel::class.java)

        initViewModelObservers()
        initClickListeners()

        viewModel.onCreateActivity()

        lifecycle.addObserver(KeyboardDetector(layout) { state ->
            viewModel.onKeyboardVisibilityChanged(state.open)
        })
    }

    override fun onBackPressed() {
        if (supportFragmentManager.fragments.lastOrNull() is AuthSignupValidationFragment && viewModel.profileCompletion) {
            // Prevent closing mandatory age/gender step
            return
        }
        if (supportFragmentManager.fragments.lastOrNull() is AuthenticationLoginFragment || supportFragmentManager.fragments.lastOrNull() is AuthenticationSignupFragment) {
            viewModel.openChooseLoginType()
            return
        }
        super.onBackPressed()
    }

    override fun finish() {
        if (!Credentials.isLogged(this)) {
            UserRepository.getInstance().onLoginCanceled()
        }

        super.finish()
    }

    override fun onAuthenticationError(error: AuthenticationException) {
        Timber.tag("Social login").w(error)

        when (error) {
            is FacebookTimeoutAuthenticationException -> {
                AMProgressHUD.dismiss()
                try {
                    AlertDialog.Builder(this, R.style.AudiomackAlertDialog)
                        .setTitle(getString(R.string.login_error_title))
                        .setMessage(getString(R.string.feature_not_available_offline_alert_message))
                        .setPositiveButton(getString(R.string.ok), null)
                        .create()
                        .show()
                } catch (e: Exception) {
                    Timber.w(e)
                }
            }
            else -> {
                AMProgressHUD.dismiss()
                try {
                    AlertDialog.Builder(this, R.style.AudiomackAlertDialog)
                        .setTitle(getString(R.string.login_error_title))
                        .setMessage(error.message)
                        .setPositiveButton(getString(R.string.ok), null)
                        .create()
                        .show()
                } catch (e: Exception) {
                    Timber.w(e)
                }
            }
        }
    }

    override fun onAuthenticationSuccess(credentials: Credentials?) {
        onLoggedIn()
        goHome()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_SAVE_CREDENTIALS) {
            val message = if (resultCode == RESULT_OK) {
                "Credentials saved"
            } else {
                "Credentials not saved, action canceled by user"
            }
            Timber.tag("SmartLock").d(message)
            onAuthenticationSuccess(null)
        } else if (requestCode == REQ_CODE_CREDENTIALS_RESOLUTION) {
            if (resultCode == RESULT_OK) {
                Timber.tag("SmartLock").d("Credentials resolution: credentials picked")
                data?.getParcelableExtra<Credential>(Credential.EXTRA_KEY)?.let {
                    viewModel.onCredentialsFound(it.id, it.password, false)
                }
            } else {
                Timber.tag("SmartLock").d("Credentials resolution: aborted")
            }
        }
    }

    override fun onConnected(bundle: Bundle?) {
        super.onConnected(bundle)
        viewModel.onGoogleServicesConnected()
    }

    private fun initViewModelObservers() {
        val activity = this
        viewModel.apply {
            closeEvent.observe(activity) { goHome() }
            goToChooseLoginTypeEvent.observe(activity, goToChooseLoginTypeObserver)
            goToSignupEvent.observe(activity, goToSignupObserver)
            goToLoginEvent.observe(activity, goToLoginObserver)
            smartlockEvent.observe(activity, smartlockObserver)
            showSupportEvent.observe(activity) { launchSupport() }
            openURLEvent.observe(activity) { launchUrl(it) }
            goToAgeGenderEvent.observe(activity, goToAgeGenderEventObserver)
            credentialsEvent.observe(activity, signupSuccessObserver)
            signupLoadingEvent.observe(activity, signupLoadingObserver)
            authErrorEvent.observe(activity, signupErrorObserver)
        }

        validationViewModel.apply {
            showTermsEvent.observe(activity) { launchUrl(TOS_URL) }
            showPrivacyPolicyEvent.observe(activity) { launchUrl(PRIVACY_POLICY_URL) }
            contactUsEvent.observe(activity) { launchSupport() }
            errorEvent.observe(activity, ageGenderErrorObserver)
            validationEvent.observe(activity, ageGenderObserver)
        }
    }

    private val goToAgeGenderEventObserver = Observer<Void> {
        buttonBack.visibility = if (viewModel.profileCompletion) View.GONE else View.VISIBLE
        buttonClose.visibility = View.GONE
        if (supportFragmentManager.isReady()) {
            val fragment = AuthSignupValidationFragment()
            supportFragmentManager.commit {
                if (!viewModel.profileCompletion) {
                    addToBackStack(AuthSignupValidationFragment.TAG)
                }
                replace(R.id.container, fragment, AuthSignupValidationFragment.TAG)
            }
        }
    }

    private val signupLoadingObserver = Observer<Boolean> { loading ->
        if (loading) {
            AMProgressHUD.showWithStatus(this, null)
        } else {
            AMProgressHUD.dismiss()
        }
    }

    private val ageGenderObserver = Observer<Pair<Date, Gender>> { (birthday, gender) ->
        viewModel.onAgeGenderSubmitted(birthday, gender)
    }

    private val ageGenderErrorObserver = Observer<Throwable> {
        val message = when (it) {
            ValidationException.Birthday -> getString(R.string.signup_error_birthday)
            ValidationException.MinAge -> getString(R.string.signup_error_min_age)
            ValidationException.Gender -> getString(R.string.signup_error_gender)
            else -> getString(R.string.generic_api_error)
        }
        AMProgressHUD.showWithError(this, message)
    }

    private fun initClickListeners() {
        buttonClose.setOnClickListener { viewModel.onCloseTapped() }
        buttonBack.setOnClickListener { viewModel.onBackTapped() }
    }

    private val goToChooseLoginTypeObserver: Observer<Void> = Observer {
        val fragment = AuthenticationChooseLoginTypeFragment.newInstance(source)
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment, fragment.javaClass.simpleName).commitAllowingStateLoss()
        buttonBack.visibility = View.GONE
    }

    private val goToSignupObserver: Observer<String?> = Observer {
        val fragment = AuthenticationSignupFragment.newInstance(source, it)
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment, fragment.javaClass.simpleName).commitAllowingStateLoss()
        buttonBack.visibility = View.VISIBLE
    }

    private val goToLoginObserver: Observer<String?> = Observer {
        val fragment = AuthenticationLoginFragment.newInstance(source, it)
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment, fragment.javaClass.simpleName).commitAllowingStateLoss()
        buttonBack.visibility = View.VISIBLE
    }

    private val smartlockObserver: Observer<Void> = Observer {
        if (DeviceRepository.runningEspressoTest) {
            return@Observer
        }

        if (credentialsApiClient == null || credentialsApiClient?.isConnected == false) {
            return@Observer
        }

        val credentialRequest = CredentialRequest.Builder()
            .setPasswordLoginSupported(true)
            .build()

        Auth.CredentialsApi.request(credentialsApiClient, credentialRequest)
            .setResultCallback { credentialRequestResult ->
                if (credentialRequestResult.status.isSuccess) {
                    Timber.tag("SmartLock").d("Found credentials")
                    val credential = credentialRequestResult.credential
                    viewModel.onCredentialsFound(credential.id, credential.password, true)
                    return@setResultCallback
                }

                if (credentialRequestResult.status.statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                    Timber.tag("SmartLock").d("Need to choose a saved credential")
                    try {
                        credentialRequestResult.status.startResolutionForResult(this@AuthenticationActivity, REQ_CODE_CREDENTIALS_RESOLUTION)
                    } catch (e: Exception) {
                        Timber.w(e)
                    }
                    return@setResultCallback
                }

                Timber.tag("SmartLock").d("Didn't find credentials")
            }
    }

    private val signupSuccessObserver = Observer<Credentials?> { credentials ->
        credentials?.let {
            val smartLockCredentials = Credential.Builder(it.email).setPassword(it.password).build()

            try {
                Auth.CredentialsApi.save(credentialsApiClient, smartLockCredentials)
                    .setResultCallback { status ->
                        if (status.isSuccess) {
                            Timber.tag("SmartLock").d("Credentials saved on SmartLock")
                            return@setResultCallback
                        }

                        Timber.tag("SmartLock").d("Credentials not saved on SmartLock")

                        if (status.hasResolution()) {
                            Timber.tag("SmartLock").d("Try to resolve the save request")
                            try {
                                status.startResolutionForResult(this, REQ_CODE_SAVE_CREDENTIALS)
                            } catch (e: Exception) {
                                Timber.tag("SmartLock").d("Failed to resolve the save request")
                            }

                            return@setResultCallback
                        }

                        Timber.tag("SmartLock").d("No resolution")
                    }
            } catch (e: Exception) {
                Timber.w(e)
            } finally {
                onAuthenticationSuccess(credentials)
            }
        }

        Timber.tag("SmartLock").d("Credential was null in onAuthenticationSuccess callback")
    }

    private val signupErrorObserver = Observer<AuthenticationException> { error ->
        when (error) {
            // Im sure there's a clever way to consolidate these HUD calls but i dont know
            // what that way is yet...
            is InvalidEmailAuthenticationException -> {
                AMProgressHUD.showWithError(this, error.message)
            }
            is InvalidUsernameAuthenticationException -> {
                AMProgressHUD.showWithError(this, error.message)
            }
            is InvalidPasswordAuthenticationException -> {
                AMProgressHUD.showWithError(this, error.message)
            }
            is MismatchPasswordAuthenticationException -> {
                AMProgressHUD.showWithError(this, error.message)
            }
            is OfflineException -> {
                AlertDialog.Builder(this, R.style.AudiomackAlertDialog)
                    .setTitle(getString(R.string.signup_error_title))
                    .setMessage(getString(R.string.feature_not_available_offline_alert_message))
                    .setPositiveButton(
                        getString(R.string.feature_not_available_offline_alert_button),
                        null
                    )
                    .create()
                    .show()
            }
            is TimeoutAuthenticationException -> {
                AlertDialog.Builder(this, R.style.AudiomackAlertDialog)
                    .setTitle(getString(R.string.signup_error_title))
                    .setMessage(getString(R.string.feature_not_available_offline_alert_message))
                    .setPositiveButton(getString(R.string.ok), null)
                    .create()
                    .show()
            }
            is SignupException -> {
                AlertDialog.Builder(this, R.style.AudiomackAlertDialog)
                    .setTitle(getString(R.string.signup_error_title))
                    .setMessage(error.message)
                    .setPositiveButton(getString(R.string.ok), null)
                    .create()
                    .show()
            }
            is ProfileCompletionException -> {
                AlertDialog.Builder(this, R.style.AudiomackAlertDialog)
                    .setMessage(error.message)
                    .setPositiveButton(getString(R.string.ok), null)
                    .setCancelable(false)
                    .create()
                    .show()
            }
            is ProfileCompletionSkippableException -> {
                AlertDialog.Builder(this, R.style.AudiomackAlertDialog)
                    .setMessage(error.message)
                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                        if (viewModel.profileCompletion) {
                            finish()
                        }
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            }
        }
    }

    private fun launchSupport() {
        AuthenticationForgotPasswordAlertFragment.show(this@AuthenticationActivity)
    }

    private fun launchUrl(url: String?) {
        url?.let { openUrlExcludingAudiomack(it) }
    }

    private fun goHome() {
        try {
            finish()
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun onLoggedIn() {
        AudiomackWidget.updateWidgetFavoriteAfterLogin()
        AMProgressHUD.dismiss()
        UserRepository.getInstance().onLoggedIn()
    }
}
