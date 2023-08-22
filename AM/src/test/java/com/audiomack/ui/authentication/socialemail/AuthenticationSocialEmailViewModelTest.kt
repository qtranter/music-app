package com.audiomack.ui.authentication.socialemail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class AuthenticationSocialEmailViewModelTest {

    private lateinit var viewModel: AuthenticationSocialEmailViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        viewModel = AuthenticationSocialEmailViewModel()
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `on close tapped`() {
        val observerClose: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onCloseTapped()
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `on background tapped`() {
        val observerClose: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onBackgroundTapped()
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `on cancel tapped`() {
        val observerClose: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onCancelTapped()
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `add social email, empty`() {
        val observerShowError: Observer<String> = mock()
        val observerClose: Observer<Void> = mock()
        viewModel.showErrorEvent.observeForever(observerShowError)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onSubmitTapped("")
        verify(observerShowError).onChanged(any())
        verifyZeroInteractions(observerClose)
    }

    @Test
    fun `add social email, valid`() {
        val observerShowError: Observer<String> = mock()
        val observerClose: Observer<Void> = mock()
        viewModel.showErrorEvent.observeForever(observerShowError)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onSubmitTapped("matteo@biz.com")
        verifyZeroInteractions(observerShowError)
        verify(observerClose).onChanged(null)
    }
}
