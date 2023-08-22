package com.audiomack.ui.authentication.changepw

import android.os.Bundle
import android.text.SpannableString
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.ProgressHUDMode
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.ui.authentication.forgotpw.AuthenticationForgotPasswordAlertFragment
import com.audiomack.views.AMProgressHUD
import kotlinx.android.synthetic.main.fragment_changepassword.buttonBack
import kotlinx.android.synthetic.main.fragment_changepassword.buttonForgotPassword
import kotlinx.android.synthetic.main.fragment_changepassword.buttonSave
import kotlinx.android.synthetic.main.fragment_changepassword.buttonShowConfirmPassword
import kotlinx.android.synthetic.main.fragment_changepassword.buttonShowCurrentPassword
import kotlinx.android.synthetic.main.fragment_changepassword.buttonShowNewPassword
import kotlinx.android.synthetic.main.fragment_changepassword.etConfirmPassword
import kotlinx.android.synthetic.main.fragment_changepassword.etCurrentPassword
import kotlinx.android.synthetic.main.fragment_changepassword.etNewPassword

class ChangePasswordFragment : TrackedFragment(R.layout.fragment_changepassword, TAG) {

    private val viewModel by viewModels<ChangePasswordViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModelObservers()
        initClickListeners()
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            goBackEvent.observe(viewLifecycleOwner) { activity?.onBackPressed() }
            openForgotPasswordEvent.observe(viewLifecycleOwner) {
                AuthenticationForgotPasswordAlertFragment.show(requireActivity())
            }
            viewState.observe(viewLifecycleOwner) {
                etCurrentPassword.transformationMethod =
                    if (it.currentPasswordSecured) PasswordTransformationMethod() else null
                etCurrentPassword.setSelection(etCurrentPassword.length())
                buttonShowCurrentPassword.setImageResource(
                    if (it.currentPasswordSecured) R.drawable.ic_password_show
                    else R.drawable.ic_password_hide
                )

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

                buttonSave.isClickable = it.saveButtonEnabled
                buttonSave.alpha = if (it.saveButtonEnabled) 1F else 0.5F
            }
            showHUDEvent.observe(viewLifecycleOwner) { mode ->
                if ((mode as? ProgressHUDMode.Failure)?.message?.isBlank() == true) {
                    AMProgressHUD.show(activity, ProgressHUDMode.Failure(getString(R.string.generic_api_error)))
                } else {
                    AMProgressHUD.show(activity, mode)
                }
            }
            showSuccessAlertEvent.observe(viewLifecycleOwner) {
                AMAlertFragment.Builder(requireActivity())
                    .title(SpannableString(getString(R.string.update_password_success)))
                    .solidButton(SpannableString(getString(R.string.ok))) {
                        activity?.onBackPressed()
                        AuthenticationActivity.show(activity, LoginSignupSource.ChangePassword)
                    }
                    .cancellable(false)
                    .show(parentFragmentManager)
            }
        }
    }

    private fun initClickListeners() {
        buttonBack.setOnClickListener { viewModel.onBackClick() }
        buttonShowCurrentPassword.setOnClickListener { viewModel.onCurrentPasswordShowHideClick() }
        buttonShowNewPassword.setOnClickListener { viewModel.onNewPasswordShowHideClick() }
        buttonShowConfirmPassword.setOnClickListener { viewModel.onConfirmPasswordShowHideClick() }
        buttonForgotPassword.setOnClickListener { viewModel.onForgotPasswordClick() }
        buttonSave.setOnClickListener { viewModel.onSaveClick() }
        etCurrentPassword.addTextChangedListener { viewModel.onCurrentPasswordChanged(etCurrentPassword.text.toString()) }
        etNewPassword.addTextChangedListener { viewModel.onNewPasswordChanged(etNewPassword.text.toString()) }
        etConfirmPassword.addTextChangedListener { viewModel.onConfirmPasswordChanged(etConfirmPassword.text.toString()) }
    }

    companion object {
        private const val TAG = "ChangePasswordFragment"
        fun newInstance() = ChangePasswordFragment()
    }
}
