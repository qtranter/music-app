package com.audiomack.ui.authentication

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
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
import com.audiomack.ui.authentication.forgotpw.AuthenticationForgotPasswordAlertFragment
import com.audiomack.utils.AMClickableSpan
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.setHorizontalPadding
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressHUD
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.Credential
import kotlinx.android.synthetic.main.fragment_authentication_login.buttonForgotPassword
import kotlinx.android.synthetic.main.fragment_authentication_login.buttonLogin
import kotlinx.android.synthetic.main.fragment_authentication_login.buttonShowPassword
import kotlinx.android.synthetic.main.fragment_authentication_login.buttonTOS
import kotlinx.android.synthetic.main.fragment_authentication_login.etEmailLayout
import kotlinx.android.synthetic.main.fragment_authentication_login.etPassword
import kotlinx.android.synthetic.main.fragment_authentication_login.tvCantLogin
import timber.log.Timber

class AuthenticationLoginFragment : TrackedFragment(R.layout.fragment_authentication_login, TAG),
    AuthenticationDataSource.LoginAuthenticationCallback {

    private var viewModel: AuthenticationViewModel? = null

    private val loginHandler: (View) -> Unit = { v: View ->
        etEmailLayout.clearFocus()
        val email = etEmailLayout.typingEditText.text.toString().trim()
        val password = etPassword.text.toString().trim()

        activity?.let {
            val viewModel = (it as AuthenticationActivity).viewModel
            viewModel.isOnline = Reachability.getInstance().networkAvailable
            viewModel.login(email, password, this)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as? AuthenticationActivity)?.let { activity ->
            val viewModel = activity.viewModel
            viewModel.goToForgotPasswordEvent.observe(viewLifecycleOwner, Observer {
                AuthenticationForgotPasswordAlertFragment.show(activity)
            })
            viewModel.smartlockCredentialsEvent.observe(viewLifecycleOwner, Observer { (email, password, automatic) ->
                if (!automatic || email == etEmailLayout.typingEditText.text.toString()) {
                    email?.let {
                        etEmailLayout.typingEditText.setText(it)
                        etEmailLayout.typingEditText.setSelection(it.length)
                    }
                    password?.let {
                        etPassword.setText(it)
                        etPassword.setSelection(it.length)
                    }
                }
            })
            viewModel.footerVisible.observe(viewLifecycleOwner, Observer { visible ->
                tvCantLogin.visibility = if (visible) View.VISIBLE else View.GONE
            })
            viewModel.trackScreen("Login")
            viewModel.requestLoginCredentials()
            this@AuthenticationLoginFragment.viewModel = viewModel
        }

        etPassword.setHorizontalPadding(8)

        buttonTOS.text = buttonTOS.context.spannableString(
            fullString = getString(R.string.login_tos),
            highlightedStrings = listOf(
                getString(R.string.login_tos_highlighted_tos),
                getString(R.string.login_tos_highlighted_privacy)
            ),
            highlightedColor = buttonTOS.context.colorCompat(R.color.orange),
            clickableSpans = listOf(
                AMClickableSpan(buttonTOS.context) { viewModel?.onTOSTapped() },
                AMClickableSpan(buttonTOS.context) { viewModel?.onPrivacyPolicyTapped() }
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
        tvCantLogin.setOnClickListener { viewModel?.onSupportTapped() }

        activity?.let {
            val authActivity = it as AuthenticationActivity
            authActivity.source = arguments?.getSerializable("source") as LoginSignupSource
        }

        buttonLogin.setOnClickListener(loginHandler)
        buttonForgotPassword.setOnClickListener { (activity as? AuthenticationActivity)?.viewModel?.onForgotPasswordTapped() }

        buttonShowPassword.setOnClickListener { togglePassword(buttonShowPassword, etPassword) }

        etPassword.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                loginHandler(buttonLogin)
                return@setOnKeyListener true
            }
            false
        }

        arguments?.getString("email")?.let {
            etEmailLayout.typingEditText.setText(it)
            etEmailLayout.typingEditText.setSelection(it.length)
        }
    }

    override fun onStop() {
        super.onStop()
        activity?.let { notNullActivity ->
            notNullActivity.currentFocus?.let { notNullFocus ->
                notNullFocus.clearFocus()
                (notNullActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(notNullFocus.windowToken, 0)
            }
        }
    }

    private fun togglePassword(button: ImageButton, editText: EditText) {
        button.isSelected = !button.isSelected
        if (button.isSelected) {
            button.setImageResource(R.drawable.ic_password_hide)
            editText.transformationMethod = null
        } else {
            button.setImageResource(R.drawable.ic_password_show)
            editText.transformationMethod = PasswordTransformationMethod()
        }
        editText.setSelection(editText.length())
    }

    override fun onBeforeLogin() {
        activity?.let {
            AMProgressHUD.showWithStatus(it, null)
        }
    }

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
        when (error) {
            // Im sure there's a clever way to consolidate these HUD calls but i dont know
            // what that way is yet...
            is InvalidEmailAuthenticationException -> {
                activity?.let {
                    AMProgressHUD.showWithError(it, error.message)
                }
            }
            is InvalidPasswordAuthenticationException -> {
                activity?.let {
                    AMProgressHUD.showWithError(it, error.message)
                }
            }
            is TimeoutAuthenticationException -> {
                AMProgressHUD.dismiss()
                activity?.let {
                    AlertDialog.Builder(it, R.style.AudiomackAlertDialog)
                        .setTitle(R.string.login_error_title)
                        .setMessage(getString(R.string.feature_not_available_offline_alert_message))
                        .setPositiveButton(R.string.ok, null)
                        .create()
                        .show()
                }
            }
            is OfflineException -> {
                AMProgressHUD.dismiss()
                activity?.let {
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
            }
            is LoginException -> {
                AMProgressHUD.dismiss()
                activity?.let {
                    AlertDialog.Builder(it, R.style.AudiomackAlertDialog)
                        .setTitle(R.string.login_error_title)
                        .setMessage(error.message)
                        .setPositiveButton(R.string.ok, null)
                        .create()
                        .show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "AuthenticationLoginFragment"
        fun newInstance(source: LoginSignupSource, email: String?): AuthenticationLoginFragment {
            return AuthenticationLoginFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("source", source)
                    putString("email", email)
                }
            }
        }
    }
}
