package com.audiomack.ui.authentication.contact

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.ui.common.AMContactProvider
import com.audiomack.utils.spannableString
import com.audiomack.views.AMSnackbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_contact_support.animationView
import kotlinx.android.synthetic.main.fragment_contact_support.buttonSave
import kotlinx.android.synthetic.main.fragment_contact_support.etCanText
import kotlinx.android.synthetic.main.fragment_contact_support.etEmailLayout
import kotlinx.android.synthetic.main.fragment_contact_support.layoutHow
import kotlinx.android.synthetic.main.fragment_contact_support.layoutWhat
import kotlinx.android.synthetic.main.fragment_contact_support.layoutWhen
import kotlinx.android.synthetic.main.fragment_contact_support.tvCanTitle
import kotlinx.android.synthetic.main.fragment_contact_support.tvEmailTitle
import kotlinx.android.synthetic.main.fragment_contact_support.tvHowText
import kotlinx.android.synthetic.main.fragment_contact_support.tvHowTitle
import kotlinx.android.synthetic.main.fragment_contact_support.tvWhatText
import kotlinx.android.synthetic.main.fragment_contact_support.tvWhatTitle
import kotlinx.android.synthetic.main.fragment_contact_support.tvWhenText
import kotlinx.android.synthetic.main.fragment_contact_support.tvWhenTitle

class ContactSupportFragment : TrackedFragment(R.layout.fragment_contact_support, TAG) {

    private val viewModel: ContactSupportViewModel by activityViewModels()

    private var whatTitle: String = ""
    private var howTitle: String = ""
    private var whenTitle: String = ""
    private var emailTitle: String = ""
    private var notesTitle: String = ""
    private var requiredText: String = ""
    private var whatText: String = ""
    private var howText: String = ""
    private var whenText: String = ""

    private fun initClickListeners() {
        layoutWhat.setOnClickListener { viewModel.onWhatTapped() }
        layoutHow.setOnClickListener { viewModel.onHowTapped() }
        layoutWhen.setOnClickListener { viewModel.onWhenTapped() }
        buttonSave.setOnClickListener { viewModel.onSendTapped(whatText, howText, whenText, etEmailLayout.typingEditText.text.toString().trim(), etCanText.text.toString().trim()) }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            showLoadingEvent.observe(viewLifecycleOwner, showLoadingObserver)
            hideLoadingEvent.observe(viewLifecycleOwner, hideLoadingObserver)
            showErrorMessageEvent.observe(viewLifecycleOwner, showErrorMessageObserver)
            showSuccessMessageEvent.observe(viewLifecycleOwner, showSuccessMessageObserver)
            errorEvent.observe(viewLifecycleOwner, errorObserver)
            sendEvent.observe(viewLifecycleOwner, sendObserver)
            tooltipEvent.observe(viewLifecycleOwner, tooltipObserver)
            whatSelectEvent.observe(viewLifecycleOwner, whatSelectObserver)
            whenSelectEvent.observe(viewLifecycleOwner, whenSelectObserver)
            howSelectEvent.observe(viewLifecycleOwner, howSelectObserver)
            emailEvent.observe(viewLifecycleOwner, emailObserver)
            notesEvent.observe(viewLifecycleOwner, notesObserver)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClickListeners()
        initViewModelObservers()

        context?.let { context ->

            viewModel.init(AMContactProvider(context))

            whatTitle = getString(R.string.contact_what_title_text)
            howTitle = getString(R.string.contact_how_title_text)
            whenTitle = getString(R.string.contact_when_title_text)
            emailTitle = getString(R.string.contact_email_title_text)
            notesTitle = getString(R.string.contact_can_title_text)
            requiredText = getString(R.string.contact_selection_required)

            configureTextInputs()
            configureTexts(true)
        }
    }

    private val showLoadingObserver: Observer<Void> = Observer {
        animationView.show()
    }

    private val hideLoadingObserver: Observer<Void> = Observer {
        animationView.hide()
    }

    private val showErrorMessageObserver: Observer<Void> = Observer {
        AMSnackbar.Builder(activity)
            .withTitle(getString(R.string.generic_error_occurred))
            .withSubtitle(getString(R.string.please_try_again_later))
            .withDrawable(R.drawable.ic_snackbar_error)
            .withDuration(Snackbar.LENGTH_SHORT)
            .show()
    }

    private val showSuccessMessageObserver: Observer<Void> = Observer {
        AMSnackbar.Builder(activity)
            .withTitle(getString(R.string.contact_send_success))
            .withDrawable(R.drawable.ic_snackbar_success)
            .withDuration(Snackbar.LENGTH_SHORT)
            .show()
    }

    private val errorObserver: Observer<Void> = Observer {
        configureTexts(false)
    }

    private val sendObserver: Observer<Void> = Observer {
        viewModel.onSendTicket(whatText, howText, whenText, etEmailLayout.typingEditText.text.toString().trim(), etCanText.text.toString())
    }

    private val tooltipObserver: Observer<Void> = Observer {
        AMSnackbar.Builder(activity)
            .withTitle(getString(R.string.contact_time_limit_error))
            .withDrawable(R.drawable.ic_snackbar_error)
            .show()
    }

    private val whatSelectObserver: Observer<String> = Observer {
        whatText = it
        tvWhatText.text = it
        tvWhatTitle.text = whatTitle
    }

    private val whenSelectObserver: Observer<String> = Observer {
        whenText = it
        tvWhenText.text = it
        tvWhenTitle.text = whenTitle
    }

    private val howSelectObserver: Observer<String> = Observer {
        howText = it
        tvHowText.text = it
        tvHowTitle.text = howTitle
    }

    private val emailObserver: Observer<String> = Observer {
        if (it.isNotEmpty()) {
            tvEmailTitle.text = emailTitle
        }
    }

    private val notesObserver: Observer<String> = Observer {
        if (it.isNotEmpty()) {
            tvCanTitle.text = notesTitle
        }
    }

    private fun configureTextInputs() {
        etEmailLayout.typingEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onEmailChanged(s.toString())
            }
        })
        etCanText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onNotesChanged(s.toString())
            }
        })
    }

    private fun configureTexts(preload: Boolean) {
        tvWhatTitle.text = configureString(if (preload) false else whatText.isEmpty(), whatTitle)
        tvHowTitle.text = configureString(if (preload) false else howText.isEmpty(), howTitle)
        tvWhenTitle.text = configureString(if (preload) false else whenText.isEmpty(), whenTitle)
        tvEmailTitle.text = configureString(if (preload) false else etEmailLayout.typingEditText.text.toString().isEmpty(), emailTitle)
        tvCanTitle.text = configureString(if (preload) false else etCanText.text.toString().isEmpty(), notesTitle)
    }

    private fun configureString(error: Boolean, text: String): SpannableString {

        val highlightedString = if (error) requiredText else ""
        val fullString = text + if (error) " $highlightedString" else ""

        return tvWhatTitle.context.spannableString(
            fullString = fullString,
            highlightedStrings = listOf(highlightedString),
            highlightedColor = Color.RED,
            highlightedFont = R.font.opensans_bold
        )
    }

    companion object {
        private const val TAG = "ContactSupportFragment"
        fun newInstance(): ContactSupportFragment {
            return ContactSupportFragment()
        }
    }
}
