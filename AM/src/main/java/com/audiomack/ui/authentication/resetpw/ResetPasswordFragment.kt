package com.audiomack.ui.authentication.resetpw

import android.os.Bundle
import android.text.SpannableString
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.ProgressHUDMode
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.views.AMProgressHUD
import kotlinx.android.synthetic.main.fragment_resetpassword.buttonClose
import kotlinx.android.synthetic.main.fragment_resetpassword.buttonReset
import kotlinx.android.synthetic.main.fragment_resetpassword.buttonShowConfirmPassword
import kotlinx.android.synthetic.main.fragment_resetpassword.buttonShowNewPassword
import kotlinx.android.synthetic.main.fragment_resetpassword.etConfirmPassword
import kotlinx.android.synthetic.main.fragment_resetpassword.etNewPassword

class ResetPasswordFragment : TrackedFragment(R.layout.fragment_resetpassword, TAG) {

    private val viewModel by viewModels<ResetPasswordViewModel>(
        factoryProducer = { ResetPasswordViewModelFactory(token) }
    )

    private val token by lazy { requireNotNull(requireArguments().getString(FRAGMENT_ARGS_TOKEN)) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModelObservers()
        initClickListeners()
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            viewState.observe(viewLifecycleOwner) {
                etNewPassword.transformationMethod =
                    if (it.newPasswordSecured) PasswordTransformationMethod() else null
                etNewPassword.setSelection(etNewPassword.length())
                buttonShowNewPassword.setImageResource(
                    if (it.newPasswordSecured) R.drawable.ic_password_show
                    else R.drawable.ic_password_hide
                )

                etConfirmPassword.transformationMethod =
                    if (it.confirmPasswordSecured) PasswordTransformationMethod() else null
                etConfirmPassword.setSelection(etConfirmPassword.length())
                buttonShowConfirmPassword.setImageResource(
                    if (it.confirmPasswordSecured) R.drawable.ic_password_show
                    else R.drawable.ic_password_hide
                )

                buttonReset.isClickable = it.resetButtonEnabled
                buttonReset.alpha = if (it.resetButtonEnabled) 1F else 0.5F
            }

            closeEvent.observe(viewLifecycleOwner) { activity?.onBackPressed() }
            showHUDEvent.observe(viewLifecycleOwner) { mode ->
                if ((mode as? ProgressHUDMode.Failure)?.message?.isBlank() == true) {
                    AMProgressHUD.show(activity, ProgressHUDMode.Failure(getString(R.string.generic_api_error)))
                } else {
                    AMProgressHUD.show(activity, mode)
                }
            }
            showSuccessAlertEvent.observe(viewLifecycleOwner) {
                AMAlertFragment.Builder(requireActivity())
                    .title(SpannableString(getString(R.string.reset_password_success)))
                    .solidButton(SpannableString(getString(R.string.ok))) {
                        activity?.onBackPressed()
                        AuthenticationActivity.show(activity, LoginSignupSource.ResetPassword)
                    }
                    .cancellable(false)
                    .show(parentFragmentManager)
            }
        }
    }

    private fun initClickListeners() {
        buttonClose.setOnClickListener { viewModel.onCloseClick() }
        buttonReset.setOnClickListener { viewModel.onResetClick() }
        buttonShowNewPassword.setOnClickListener { viewModel.onNewPasswordShowHideClick() }
        buttonShowConfirmPassword.setOnClickListener { viewModel.onConfirmPasswordShowHideClick() }
        etNewPassword.addTextChangedListener { viewModel.onNewPasswordChanged(etNewPassword.text.toString()) }
        etConfirmPassword.addTextChangedListener { viewModel.onConfirmPasswordChanged(etConfirmPassword.text.toString()) }
    }

    companion object {
        private const val TAG = "ResetPasswordFragment"
        private const val FRAGMENT_ARGS_TOKEN = "args_token"
        fun newInstance(token: String) = ResetPasswordFragment().apply {
            arguments = bundleOf(FRAGMENT_ARGS_TOKEN to token)
        }
    }
}
