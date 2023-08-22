package com.audiomack.ui.authentication

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.audiomack.BuildConfig
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.data.authentication.AuthenticationDataSource
import com.audiomack.data.authentication.AuthenticationException
import com.audiomack.data.authentication.InvalidEmailAuthenticationException
import com.audiomack.data.authentication.InvalidPasswordAuthenticationException
import com.audiomack.data.authentication.LoginException
import com.audiomack.data.authentication.OfflineException
import com.audiomack.data.authentication.TimeoutAuthenticationException
import com.audiomack.data.reachability.Reachability
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.Credentials
import com.audiomack.model.LoginSignupSource
import com.audiomack.ui.authentication.socialemail.AuthenticationSocialEmailAlertFragment
import com.audiomack.ui.webviewauth.WebViewAuthConfigurationFactory
import com.audiomack.ui.webviewauth.WebViewAuthManager
import com.audiomack.utils.AMClickableSpan
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressHUD
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.Credential
import kotlinx.android.synthetic.main.fragment_authentication_choose_login_type.buttonApple
import kotlinx.android.synthetic.main.fragment_authentication_choose_login_type.buttonContinue
import kotlinx.android.synthetic.main.fragment_authentication_choose_login_type.buttonFacebook
import kotlinx.android.synthetic.main.fragment_authentication_choose_login_type.buttonGoogle
import kotlinx.android.synthetic.main.fragment_authentication_choose_login_type.buttonTOS
import kotlinx.android.synthetic.main.fragment_authentication_choose_login_type.buttonTwitter
import kotlinx.android.synthetic.main.fragment_authentication_choose_login_type.etEmailLayout
import kotlinx.android.synthetic.main.fragment_authentication_choose_login_type.tvCantLogin
import kotlinx.android.synthetic.main.fragment_authentication_choose_login_type.tvEmailNotFound
import kotlinx.android.synthetic.main.fragment_authentication_choose_login_type.tvEmailTitle
import timber.log.Timber

class AuthenticationChooseLoginTypeFragment :
    TrackedFragment(R.layout.fragment_authentication_choose_login_type, TAG),
    AuthenticationDataSource.LoginAuthenticationCallback {

    private lateinit var viewModel: AuthenticationViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as? AuthenticationActivity)?.let { activity ->
            val viewModel = activity.viewModel
            activity.source = arguments?.getSerializable("source") as LoginSignupSource
            viewModel.trackScreen("Choose Login Type")
            viewModel.trackSignupPage()
            this@AuthenticationChooseLoginTypeFragment.viewModel = viewModel
            initViewModelObservers()
            initClickListeners(activity)
            configureViews()
        }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            showLoaderEvent.observe(viewLifecycleOwner, showLoaderObserver)
            hideLoaderEvent.observe(viewLifecycleOwner, hideLoaderObserver)
            showErrorEvent.observe(viewLifecycleOwner, showErrorObserver)
            emailNotExistentEvent.observe(viewLifecycleOwner, emailNotExistentObserver)
            smartlockCredentialsEvent.observe(viewLifecycleOwner, smartLockCredentialsObserver)
            showSocialEmailPromptEvent.observe(viewLifecycleOwner, showSocialEmailPromptObserver)
            footerVisible.observe(viewLifecycleOwner, footerVisibleObserver)
            focusOnEmailEvent.observe(viewLifecycleOwner, focusOnEmailEventObserver)
            showAppleWebViewEvent.observe(viewLifecycleOwner, showAppleWebViewEventObserver)
        }
    }

    private fun initClickListeners(activity: AuthenticationActivity) {
        tvEmailTitle.setOnClickListener {
            viewModel.onEmailTitleTapped()
        }
        tvCantLogin.setOnClickListener {
            viewModel.onSupportTapped()
        }
        tvEmailNotFound.setOnClickListener {
            viewModel.openSignup(etEmailLayout.typingEditText.text.toString())
        }
        buttonContinue.setOnClickListener {
            etEmailLayout.clearFocus()
            val email = etEmailLayout.typingEditText.text.toString().trim()
            viewModel.isOnline = Reachability.getInstance().networkAvailable
            viewModel.checkEmailExistence(email)
        }
        buttonGoogle.setOnClickListener {
            onBeforeLogin()
            viewModel.loginWithGoogle(activity, activity)
        }
        buttonTwitter.setOnClickListener {
            onBeforeLogin()
            viewModel.loginWithTwitter(activity, activity)
        }
        buttonFacebook.setOnClickListener {
            onBeforeLogin()
            viewModel.loginWithFacebook(activity, activity)
        }
        buttonApple.setOnClickListener {
            viewModel.onAppleButtonClicked()
        }
    }

    private fun configureViews() {
        tvEmailNotFound.text = tvEmailNotFound.context.spannableString(
                fullString = getString(R.string.signup_create_account_prompt),
                highlightedStrings = listOf(getString(R.string.signup_create_account_prompt_highlighted)),
                highlightedColor = tvEmailNotFound.context.colorCompat(R.color.orange),
                highlightedFont = R.font.opensans_semibold
        )

        buttonTOS.text = buttonTOS.context.spannableString(
            fullString = getString(R.string.login_tos),
            highlightedStrings = listOf(
                getString(R.string.login_tos_highlighted_tos),
                getString(R.string.login_tos_highlighted_privacy)
            ),
            highlightedColor = buttonTOS.context.colorCompat(R.color.orange),
            clickableSpans = listOf(
                AMClickableSpan(buttonTOS.context) { viewModel.onTOSTapped() },
                AMClickableSpan(buttonTOS.context) { viewModel.onPrivacyPolicyTapped() }
            )
        )
        try {
            buttonTOS.movementMethod = LinkMovementMethod()
        } catch (e: NoSuchMethodError) {
            Timber.w(e)
        }

        tvCantLogin.text = tvCantLogin.context.spannableString(
            fullString = getString(R.string.signup_cant_login),
            highlightedStrings = listOf(getString(R.string.signup_cant_login_highlighted)),
            highlightedColor = buttonTOS.context.colorCompat(R.color.orange)
        )
    }

    private val showLoaderObserver = Observer<Void> {
        AMProgressHUD.showWithStatus(activity)
    }

    private val hideLoaderObserver = Observer<Void> {
        AMProgressHUD.dismiss()
    }

    private val showErrorObserver = Observer<String> {
        AMProgressHUD.showWithError(activity, it)
    }

    private val emailNotExistentObserver = Observer<Void> {
        tvEmailNotFound.visibility = View.VISIBLE
    }

    private val smartLockCredentialsObserver = Observer<Triple<String?, String?, Boolean>> { (email, _, _) ->
        email?.let {
            etEmailLayout.typingEditText.setText(it)
            etEmailLayout.typingEditText.setSelection(it.length)
        }
    }

    private val showSocialEmailPromptObserver = Observer<Void> {
        (activity as? AuthenticationActivity)?.let {
            AuthenticationSocialEmailAlertFragment.show(it)
        }
    }

    private val footerVisibleObserver = Observer<Boolean> {
        if (!it) {
            tvCantLogin.visibility = View.GONE
            tvEmailNotFound.visibility = View.GONE
        } else {
            tvCantLogin.visibility = View.VISIBLE
        }
    }

    private val focusOnEmailEventObserver = Observer<Void> {
        (activity as? AuthenticationActivity)?.let {
            try {
                etEmailLayout.typingEditText.requestFocus()
                (it.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(etEmailLayout.typingEditText, InputMethodManager.SHOW_IMPLICIT)
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }

    private val showAppleWebViewEventObserver = Observer<Void> {
        WebViewAuthManager(
            childFragmentManager,
            "Apple",
            WebViewAuthConfigurationFactory().createAppleConfiguration(
                BuildConfig.AM_APPLE_SIGNIN_CLIENT_ID,
                BuildConfig.AM_APPLE_SIGNIN_REDIRECT_URL
            )
        ) { result ->
            viewModel.handleAppleSignInResult(result, activity as AuthenticationActivity)
        }.show()
    }

    override fun onBeforeLogin() {}

    override fun onAuthenticationSuccess(credentials: Credentials?) {
        AMProgressHUD.dismiss()

        credentials?.let {
            val smartLockCredentials = Credential.Builder(it.email).setPassword(it.password).build()

            try {
                Auth.CredentialsApi.save(
                        (activity as BaseActivity).credentialsApiClient,
                        smartLockCredentials
                ).setResultCallback { status ->
                    if (status.isSuccess) {
                        Timber.tag("SmartLock").d("Credentials saved on SmartLock")
                        return@setResultCallback
                    }

                    Timber.tag("SmartLock").d("Credentials not saved on SmartLock")

                    if (status.hasResolution()) {
                        Timber.tag("SmartLock").d("Try to resolve the save request")
                        try {
                            status.startResolutionForResult(
                                    activity,
                                    AuthenticationActivity.REQ_CODE_SAVE_CREDENTIALS
                            )
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
                (activity as? AuthenticationActivity)?.onAuthenticationSuccess(null)
            }

            return@onAuthenticationSuccess
        }

        Timber.tag("SmartLock").d("Credential was null in onAuthenticationSuccess callback")
    }

    override fun onAuthenticationError(error: AuthenticationException) {
        activity?.let {
            when (error) {
                // Im sure there's a clever way to consolidate these HUD calls but i dont know
                // what that way is yet...
                is InvalidEmailAuthenticationException -> {
                    AMProgressHUD.showWithError(it, error.message)
                }
                is InvalidPasswordAuthenticationException -> {
                    AMProgressHUD.showWithError(it, error.message)
                }
                is TimeoutAuthenticationException -> {
                    AMProgressHUD.dismiss()
                    AlertDialog.Builder(it, R.style.AudiomackAlertDialog)
                            .setTitle(R.string.login_error_title)
                            .setMessage(getString(R.string.feature_not_available_offline_alert_message))
                            .setPositiveButton(R.string.ok, null)
                            .create()
                            .show()
                }
                is OfflineException -> {
                    AMProgressHUD.dismiss()
                    AlertDialog.Builder(it, R.style.AudiomackAlertDialog)
                            .setTitle(R.string.login_error_title)
                            .setMessage(getString(R.string.feature_not_available_offline_alert_message))
                            .setPositiveButton(
                                    getString(R.string.feature_not_available_offline_alert_button),
                                    null
                            )
                            .create()
                            .show()
                }
                is LoginException -> {
                    AMProgressHUD.dismiss()
                    AlertDialog.Builder(it, R.style.AudiomackAlertDialog)
                            .setTitle(R.string.login_error_title)
                            .setMessage(error.message)
                            .setPositiveButton(R.string.ok, null)
                            .create()
                            .show()
                }
                else -> {
                    // Do nothing
                }
            }
        }
    }

    companion object {
        private const val TAG = "AuthenticationChooseLoginTypeFragment"
        fun newInstance(source: LoginSignupSource): AuthenticationChooseLoginTypeFragment {
            return AuthenticationChooseLoginTypeFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("source", source)
                }
            }
        }
    }
}
