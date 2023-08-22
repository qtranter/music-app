package com.audiomack.ui.authentication.validation

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMArtist.Gender
import com.audiomack.model.AMArtist.Gender.FEMALE
import com.audiomack.model.AMArtist.Gender.MALE
import com.audiomack.model.AMArtist.Gender.NON_BINARY
import com.audiomack.ui.authentication.AuthenticationViewModel
import com.audiomack.ui.authentication.SignupCredentials.Companion.MAX_AGE
import com.audiomack.ui.authentication.validation.AuthSignupValidationViewModel.BirthdayException.MinAge
import com.audiomack.ui.authentication.validation.AuthSignupValidationViewModel.BirthdayException.None
import com.audiomack.utils.AMClickableSpan
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.hideKeyboard
import com.audiomack.utils.isReady
import com.audiomack.utils.setOnItemSelectedListener
import com.audiomack.utils.spannableString
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlinx.android.synthetic.main.fragment_auth_signup_validation.birthdayError
import kotlinx.android.synthetic.main.fragment_auth_signup_validation.birthdaySpinner
import kotlinx.android.synthetic.main.fragment_auth_signup_validation.buttonFinish
import kotlinx.android.synthetic.main.fragment_auth_signup_validation.contactView
import kotlinx.android.synthetic.main.fragment_auth_signup_validation.genderLayout
import kotlinx.android.synthetic.main.fragment_auth_signup_validation.genderSpinner
import kotlinx.android.synthetic.main.fragment_auth_signup_validation.termsView
import kotlinx.android.synthetic.main.fragment_auth_signup_validation.title

class AuthSignupValidationFragment : TrackedFragment(R.layout.fragment_auth_signup_validation, TAG) {

    interface Contract {
        fun onGenderSelected(gender: Gender)
    }

    private val parentViewModel: AuthenticationViewModel by activityViewModels()
    private val viewModel: AuthSignupValidationViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        initViewModelObservers()
    }

    private fun initViews(view: View) {
        val profileCompletion = parentViewModel.profileCompletion
        initTitle(profileCompletion)
        initSubmitButton(profileCompletion)
        initGenderSpinner(view)
        birthdaySpinner.setOnClickListener { onBirthdaySpinnerClick() }
        buttonFinish.setOnClickListener { viewModel.save() }
        initTermsView(profileCompletion)
        initContactView()
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            gender.observe(viewLifecycleOwner, {
                genderSpinner.setSelection(it.ordinal)
            })

            birthday.observe(viewLifecycleOwner, {
                val formatter = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG)
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                birthdaySpinner.text = formatter.format(it)
            })

            birthdayErrorEvent.observe(viewLifecycleOwner, {
                birthdayError.apply {
                    visibility = if (it is None) View.GONE else View.VISIBLE
                    text = if (it is MinAge) {
                        getString(R.string.signup_error_min_age)
                    } else {
                        getString(R.string.signup_error_birthday)
                    }
                }
            })
        }
    }

    private fun initTitle(profileCompletion: Boolean) {
        title.setText(if (profileCompletion) R.string.signup_gender_birthday_information_header else R.string.signup_header)
    }

    private fun initSubmitButton(profileCompletion: Boolean) {
        buttonFinish.setText(if (profileCompletion) R.string.confirm_report_alert_submit else R.string.signup_finish)
    }

    private fun initTermsView(profileCompletion: Boolean) {
        termsView.apply {
            movementMethod = LinkMovementMethod()
            text = context.spannableString(
                fullString = getString(if (profileCompletion) R.string.signup_gender_birthday_complete_information_tos else R.string.signup_tos),
                highlightedStrings = listOf(
                    getString(R.string.signup_tos_highlighted_tos),
                    getString(R.string.signup_tos_highlighted_privacy)
                ),
                highlightedColor = context.colorCompat(R.color.orange),
                clickableSpans = listOf(
                    AMClickableSpan(context) { viewModel.onTermsClick() },
                    AMClickableSpan(context) { viewModel.onPrivacyClick() }
                )
            )
        }
    }

    private fun initGenderSpinner(view: View) {
        ArrayAdapter.createFromResource(
            view.context,
            R.array.gender,
            R.layout.item_auth_spinner
        ).apply {
            genderSpinner.adapter = this
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        genderLayout.setOnClickListener {
            // Spinner is hidden until first clicked on
            genderSpinner.run {
                performClick()
                visibility = View.VISIBLE
                viewModel.onGenderSelected(MALE)
            }
        }

        genderSpinner.apply {
            setSelection(0, false) // prevents initial 0 selection
            setOnItemSelectedListener { (pos, _) ->
                val gender = when (pos) {
                    MALE.ordinal -> MALE
                    FEMALE.ordinal -> FEMALE
                    else -> NON_BINARY
                }
                viewModel.onGenderSelected(gender)
            }
        }
    }

    private fun initContactView() {
        contactView.apply {
            movementMethod = LinkMovementMethod()
            text = context.spannableString(
                fullString = getString(R.string.signup_cant_login),
                highlightedStrings = listOf(getString(R.string.signup_cant_login_highlighted)),
                highlightedColor = context.colorCompat(R.color.orange)
            )
            setOnClickListener { viewModel.onContactUsClick() }
        }
    }

    private fun onBirthdaySpinnerClick() {
        childFragmentManager.findFragmentByTag(TAG_DATE_PICKER)?.let { fragment ->
            childFragmentManager.commit { show(fragment) }
            return
        }

        if (childFragmentManager.isReady()) {
            buildMaterialDatePicker().apply {
                addOnPositiveButtonClickListener {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.timeInMillis = it
                    viewModel.onDateSelected(cal.time)
                }
                addOnDismissListener {
                    // MaterialDatePicker doesn't dismiss the keyboard, and if called directly here
                    // the input manager doesn't register as active, so we give it some time.
                    Single.timer(250L, MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { _ ->
                            this@AuthSignupValidationFragment.context?.hideKeyboard()
                        }
                }
            }.also { it.show(childFragmentManager, TAG_DATE_PICKER) }
        }
    }

    private fun buildMaterialDatePicker() = MaterialDatePicker.Builder.datePicker().apply {
        setTitleText(resources.getString(R.string.signup_birthday_label))
        setInputMode(MaterialDatePicker.INPUT_MODE_TEXT)

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val constraintEndInMillis = calendar.timeInMillis
        calendar.add(Calendar.YEAR, -MAX_AGE)
        val constraintStartInMills = calendar.timeInMillis

        val constraintOpenAtInMillis = viewModel.startDate?.time ?: constraintEndInMillis

        val constraints = CalendarConstraints.Builder().apply {
            setStart(constraintStartInMills)
            setEnd(constraintEndInMillis)
            setOpenAt(constraintOpenAtInMillis)
        }.build()
        setCalendarConstraints(constraints)
        viewModel.startDate?.time?.let { setSelection(it) }
    }.build()

    companion object {
        const val TAG = "AuthSignupValidationFragment"
        const val TAG_DATE_PICKER = "tag_date_picker"
    }
}
