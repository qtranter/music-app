package com.audiomack.ui.authentication.forgotpw

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.showAlert
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressHUD
import kotlinx.android.synthetic.main.fragment_authentication_forgot_password.*
import timber.log.Timber
import zendesk.support.guide.ViewArticleActivity

class AuthenticationForgotPasswordAlertFragment : DialogFragment() {

    private val viewModel: AuthenticationForgotPasswordViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_authentication_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModelObservers()
        initClickListeners()
        configureViews()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AudiomackDialogFragment)
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            closeEvent.observe(viewLifecycleOwner, closeObserver)
            openSupportEvent.observe(viewLifecycleOwner, openSupportObserver)
            forgotPasswordStatusEvent.observe(viewLifecycleOwner, forgotPasswordStatusObserver)
            saveEnabled.observe(viewLifecycleOwner, saveEnabledObserver)
            hideKeyboardEvent.observe(viewLifecycleOwner, hideKeyboardEventObserver)
        }
    }

    private fun initClickListeners() {
        buttonFooter.setOnClickListener { viewModel.onContactUsTapped() }
        buttonSave.setOnClickListener { viewModel.onSaveTapped(etEmailLayout.typingEditText.text.toString()) }
        layoutContainer.setOnClickListener { viewModel.onBackgroundTapped() }
        buttonClose.setOnClickListener { viewModel.onCloseTapped() }

        etEmailLayout.typingEditText.addTextChangedListener {
            viewModel.onEmailChanged(it.toString().trim())
        }
    }

    private fun configureViews() {
        etEmailLayout.typingEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        etEmailLayout.autocompleteTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        tvTitle.text = getString(R.string.signup_troubles_alert_message)
        buttonFooter.text = buttonFooter.context.spannableString(
            fullString = getString(R.string.signup_troubles_alert_footer),
            highlightedStrings = listOf(getString(R.string.signup_troubles_alert_footer_highlighted)),
            highlightedColor = buttonFooter.context.colorCompat(R.color.orange),
            highlightedFont = R.font.opensans_semibold
        )
    }

    private val closeObserver = Observer<Void> {
        try {
            dismiss()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    private val openSupportObserver = Observer<Long> { articleId ->
        // See #2401 - ContactSupportActivity.show(context)
        activity?.let {
            ViewArticleActivity
                .builder(articleId)
                .withContactUsButtonVisible(false)
                .show(it)
        }
    }

    private val forgotPasswordStatusObserver = Observer<AuthenticationForgotPasswordViewModel.ForgotPasswordStatus> {
        when (it) {
            AuthenticationForgotPasswordViewModel.ForgotPasswordStatus.Loading -> AMProgressHUD.showWithStatus(activity)
            AuthenticationForgotPasswordViewModel.ForgotPasswordStatus.Success -> {
                AMProgressHUD.dismiss()
                context?.showAlert(getString(R.string.forgot_password_success))
            }
            is AuthenticationForgotPasswordViewModel.ForgotPasswordStatus.Error -> {
                AMProgressHUD.dismiss()
                context?.showAlert(getString(R.string.forgot_password_failure_template, it.exception.message))
            }
        }
    }

    private val saveEnabledObserver = Observer<Boolean> { enabled ->
        buttonSave.isEnabled = enabled
        ivSaveOverlay.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private val hideKeyboardEventObserver = Observer<Void> {
        (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(etEmailLayout.windowToken, 0)
    }

    companion object {
        fun show(activity: FragmentActivity) {
            val fragment = AuthenticationForgotPasswordAlertFragment()
            try {
                fragment.show(activity.supportFragmentManager, fragment.javaClass.simpleName)
            } catch (e: IllegalStateException) {
                Timber.w(e)
            }
        }
    }
}
