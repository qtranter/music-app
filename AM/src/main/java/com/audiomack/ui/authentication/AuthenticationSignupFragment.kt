package com.audiomack.ui.authentication

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.reachability.Reachability
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.LoginSignupSource
import com.audiomack.utils.AMClickableSpan
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.setHorizontalPadding
import com.audiomack.utils.spannableString
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_authentication_signup.buttonShowConfirmPwd
import kotlinx.android.synthetic.main.fragment_authentication_signup.buttonShowPassword
import kotlinx.android.synthetic.main.fragment_authentication_signup.buttonSignup
import kotlinx.android.synthetic.main.fragment_authentication_signup.buttonTOS
import kotlinx.android.synthetic.main.fragment_authentication_signup.etConfirmPassword
import kotlinx.android.synthetic.main.fragment_authentication_signup.etEmailLayout
import kotlinx.android.synthetic.main.fragment_authentication_signup.etPassword
import kotlinx.android.synthetic.main.fragment_authentication_signup.etUsername
import kotlinx.android.synthetic.main.fragment_authentication_signup.tvCantLogin
import timber.log.Timber

class AuthenticationSignupFragment : TrackedFragment(R.layout.fragment_authentication_signup, TAG) {

    private val viewModel: AuthenticationViewModel by activityViewModels()

    private var advertisingId: String? = null

    private var subscription: Disposable? = null

    private val signupHandler: (View) -> Unit = handler@{ v: View ->
        etEmailLayout.clearFocus()

        viewModel.isOnline = Reachability.getInstance().networkAvailable

        val username = etUsername.text.toString().trim()
        val email = etEmailLayout.typingEditText.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPwd = etConfirmPassword.text.toString().trim()
        viewModel.onSignupCredentialsSubmitted(username, email, password, confirmPwd, advertisingId)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as? AuthenticationActivity)?.let { activity ->
            activity.source = arguments?.getSerializable("source") as LoginSignupSource
            viewModel.footerVisible.observe(viewLifecycleOwner, Observer { visible ->
                tvCantLogin.visibility = if (visible) View.VISIBLE else View.GONE
            })
            viewModel.trackScreen("Signup")
        }

        etPassword.setHorizontalPadding(8)

        buttonTOS.text = buttonTOS.context.spannableString(
            fullString = getString(R.string.signup_tos),
            highlightedStrings = listOf(
                getString(R.string.signup_tos_highlighted_tos),
                getString(R.string.signup_tos_highlighted_privacy)
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

        buttonSignup.setOnClickListener(signupHandler)
        buttonTOS.setOnClickListener { viewModel.onTOSTapped() }

        tvCantLogin.text = tvCantLogin.context.spannableString(
            fullString = getString(R.string.signup_cant_login),
            highlightedStrings = listOf(getString(R.string.signup_cant_login_highlighted)),
            highlightedColor = buttonTOS.context.colorCompat(R.color.orange)
        )
        tvCantLogin.setOnClickListener { viewModel.onSupportTapped() }

        buttonShowPassword.setOnClickListener { togglePassword(buttonShowPassword, etPassword) }
        buttonShowConfirmPwd.setOnClickListener { togglePassword(buttonShowConfirmPwd, etConfirmPassword) }

        subscription = AdProvidersHelper.getAdvertisingIdentifier(requireActivity())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { s -> advertisingId = s },
                {
                    Timber.tag(AuthenticationSignupFragment::class.java.simpleName)
                        .w("Failed to get advertising identifier")
                })

        arguments?.getString("email")?.let {
            etEmailLayout.typingEditText.setText(it)
            etEmailLayout.typingEditText.setSelection(it.length)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.dispose()
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

    companion object {
        private const val TAG = "AuthenticationSignupFragment"
        fun newInstance(source: LoginSignupSource, email: String?): AuthenticationSignupFragment {
            return AuthenticationSignupFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("source", source)
                    putString("email", email)
                }
            }
        }
    }
}
