package com.audiomack.ui.authentication.socialemail

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.views.AMProgressHUD
import kotlinx.android.synthetic.main.fragment_authentication_social_email.*
import timber.log.Timber

class AuthenticationSocialEmailAlertFragment : DialogFragment() {

    private val viewModel: AuthenticationSocialEmailViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_authentication_social_email, container, false)
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
            showErrorEvent.observe(viewLifecycleOwner, showErrorObserver)
        }
    }

    private fun initClickListeners() {
        buttonSubmit.setOnClickListener { viewModel.onSubmitTapped(etEmailLayout.typingEditText.text.toString()) }
        buttonCancel.setOnClickListener { viewModel.onCancelTapped() }
        layoutContainer.setOnClickListener { viewModel.onBackgroundTapped() }
        buttonClose.setOnClickListener { viewModel.onCloseTapped() }
    }

    private fun configureViews() {
        etEmailLayout.typingEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        etEmailLayout.autocompleteTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
    }

    private val closeObserver: Observer<Void> = Observer {
        try {
            dismiss()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    private val showErrorObserver: Observer<String> = Observer {
        AMProgressHUD.showWithError(activity, it)
    }

    companion object {
        @JvmStatic
        fun show(activity: FragmentActivity) {
            val fragment = AuthenticationSocialEmailAlertFragment()
            try {
                fragment.show(activity.supportFragmentManager, fragment.javaClass.simpleName)
            } catch (e: IllegalStateException) {
                Timber.w(e)
            }
        }
    }
}
