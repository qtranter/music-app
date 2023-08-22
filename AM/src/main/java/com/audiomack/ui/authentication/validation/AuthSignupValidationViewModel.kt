package com.audiomack.ui.authentication.validation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.AMArtist.Gender
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.authentication.SignupCredentials.Companion.MIN_AGE
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.DateUtils
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import java.util.Date

class AuthSignupValidationViewModel(
    userDataSource: UserDataSource = UserRepository.getInstance(),
    schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel(), AuthSignupValidationFragment.Contract {

    var user: AMArtist? = null

    val errorEvent = SingleLiveEvent<ValidationException>()
    val birthdayErrorEvent = SingleLiveEvent<BirthdayException>()
    val showTermsEvent = SingleLiveEvent<String>()
    val showPrivacyPolicyEvent = SingleLiveEvent<String>()
    val contactUsEvent = SingleLiveEvent<Void>()
    val validationEvent = SingleLiveEvent<Pair<Date, Gender>>()

    private val _birthday = MutableLiveData<Date>()
    val birthday: LiveData<Date> get() = _birthday

    private val _gender = MutableLiveData<Gender>()
    val gender: LiveData<Gender> get() = _gender

    init {
        userDataSource.getUserAsync()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe {
                user = it
                it.birthday?.let { birthdayNonNull -> _birthday.value = birthdayNonNull }
                it.gender?.let { genderNonNull -> _gender.value = genderNonNull }
            }
            .addTo(compositeDisposable)
    }

    val startDate: Date?
        get() = _birthday.value ?: user?.birthday

    fun onDateSelected(date: Date) {
        when {
            date.after(Date()) -> birthdayErrorEvent.value = BirthdayException.Invalid
            !isMinimumAge(date) -> birthdayErrorEvent.value = BirthdayException.MinAge
            else -> birthdayErrorEvent.value = BirthdayException.None
        }
        _birthday.value = date
    }

    override fun onGenderSelected(gender: Gender) {
        _gender.value = gender
    }

    fun save() {
        val birthday = birthday.value
        if (birthday == null || birthday.after(Date())) {
            errorEvent.value = ValidationException.Birthday
            return
        } else if (!isMinimumAge(birthday)) {
            errorEvent.value = ValidationException.MinAge
            return
        }

        val gender = gender.value
        if (gender == null) {
            errorEvent.value = ValidationException.Gender
            return
        }

        validationEvent.postValue(Pair(birthday, gender))
    }

    fun onTermsClick() {
        showTermsEvent.call()
    }

    fun onPrivacyClick() {
        showPrivacyPolicyEvent.call()
    }

    fun onContactUsClick() {
        contactUsEvent.call()
    }

    private fun isMinimumAge(date: Date) = DateUtils.getAge(date) >= MIN_AGE

    sealed class ValidationException(override val message: String? = null) : Exception(message) {
        object Birthday : ValidationException()
        object MinAge : ValidationException()
        object Gender : ValidationException()
    }

    sealed class BirthdayException(override val message: String? = null) : Exception(message) {
        object Invalid : BirthdayException()
        object MinAge : BirthdayException()
        object None : BirthdayException()
    }
}
