package com.audiomack.ui.authentication.validation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist.Gender
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.authentication.SignupCredentials
import com.audiomack.ui.authentication.validation.AuthSignupValidationViewModel.BirthdayException
import com.audiomack.ui.authentication.validation.AuthSignupValidationViewModel.ValidationException
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import java.util.Calendar
import java.util.Date
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class AuthSignupValidationViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    lateinit var userDataSource: UserDataSource

    private val scheduler = TestSchedulersProvider()

    lateinit var viewModel: AuthSignupValidationViewModel

    private lateinit var validDate: Date

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        validDate = Calendar.getInstance().let {
            it.add(Calendar.YEAR, -SignupCredentials.MIN_AGE)
            it.time
        }

        whenever(userDataSource.getUserAsync()).thenReturn(Observable.never())

        viewModel = AuthSignupValidationViewModel(userDataSource, scheduler)
    }

    @After
    fun tearDown() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `start date set when user birthday is available`() {
        viewModel.user = mock {
            on { birthday } doReturn Date()
        }
        assertEquals(viewModel.startDate, viewModel.user?.birthday)
    }

    @Test
    fun `birthday event when date selected`() {
        val observer = mock<Observer<in Date>>()
        viewModel.birthday.observeForever(observer)

        viewModel.onDateSelected(validDate)

        verify(observer, times(1)).onChanged(validDate)
    }

    @Test
    fun `birthday error event when future date selected`() {
        val birthdayObserver = mock<Observer<in Date>>()
        viewModel.birthday.observeForever(birthdayObserver)

        val birthdayErrorObserver = mock<Observer<in Throwable>>()
        viewModel.birthdayErrorEvent.observeForever(birthdayErrorObserver)

        val date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
        viewModel.onDateSelected(date)

        verify(birthdayErrorObserver, times(1)).onChanged(BirthdayException.Invalid)
        verify(birthdayObserver, times(1)).onChanged(date)
    }

    @Test
    fun `gender event when gender selected`() {
        val observer = mock<Observer<in Gender>>()
        viewModel.gender.observeForever(observer)

        val gender = Gender.FEMALE
        viewModel.onGenderSelected(gender)

        verify(observer, times(1)).onChanged(gender)
    }

    @Test
    fun `show terms event on terms click`() {
        val observer = mock<Observer<String>>()
        viewModel.showTermsEvent.observeForever(observer)

        viewModel.onTermsClick()

        verify(observer, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `show privacy policy event on terms click`() {
        val observer = mock<Observer<String>>()
        viewModel.showPrivacyPolicyEvent.observeForever(observer)

        viewModel.onPrivacyClick()

        verify(observer, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `validation event when birthday and gender are valid`() {
        val validationObserver = mock<Observer<Pair<Date, Gender>>>()
        viewModel.validationEvent.observeForever(validationObserver)

        val errorObserver = mock<Observer<in Throwable>>()
        viewModel.errorEvent.observeForever(errorObserver)

        viewModel.run {
            onGenderSelected(Gender.NON_BINARY)
            onDateSelected(validDate)
            save()
        }

        verify(validationObserver, times(1)).onChanged(any())
        verifyZeroInteractions(errorObserver)
    }

    @Test
    fun `birthday error event when saving with null birthday`() {
        val validationObserver = mock<Observer<Pair<Date, Gender>>>()
        viewModel.validationEvent.observeForever(validationObserver)

        val errorObserver = mock<Observer<in Throwable>>()
        viewModel.errorEvent.observeForever(errorObserver)

        viewModel.save()

        verify(errorObserver, times(1)).onChanged(ValidationException.Birthday)
        verifyZeroInteractions(validationObserver)
    }

    @Test
    fun `min age error when age is not above 13`() {
        val validationObserver = mock<Observer<Pair<Date, Gender>>>()
        viewModel.validationEvent.observeForever(validationObserver)

        val errorObserver = mock<Observer<in Throwable>>()
        viewModel.errorEvent.observeForever(errorObserver)

        viewModel.onDateSelected(Calendar.getInstance().time)
        viewModel.save()

        verify(errorObserver, times(1)).onChanged(ValidationException.MinAge)
    }

    @Test
    fun `gender error event when saving with null gender`() {
        val validationObserver = mock<Observer<Pair<Date, Gender>>>()
        viewModel.validationEvent.observeForever(validationObserver)

        val errorObserver = mock<Observer<in Throwable>>()
        viewModel.errorEvent.observeForever(errorObserver)

        viewModel.run {
            onDateSelected(validDate)
            save()
        }

        verify(errorObserver, times(1)).onChanged(ValidationException.Gender)
        verifyZeroInteractions(validationObserver)
    }
}
