package com.audiomack.usecases

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.R
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class LoginAlertUseCaseTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var remoteVariables: RemoteVariablesProvider

    private lateinit var sut: LoginAlertUseCase

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = LoginAlertUseCase(remoteVariables)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `valid remote variable`() {
        val context = mock<Context>()
        val message = "Hello"
        whenever(remoteVariables.loginAlertMessage).thenReturn(message)
        assert(message == sut.getMessage(context))
        verify(remoteVariables).loginAlertMessage
        verifyZeroInteractions(context)
    }

    @Test
    fun `defaults to bundled string`() {
        val context = mock<Context>()
        val message = "Hello"
        whenever(remoteVariables.loginAlertMessage).thenReturn(" ")
        whenever(context.getString(R.string.login_needed_message)).thenReturn(message)
        assert(message == sut.getMessage(context))
        verify(remoteVariables).loginAlertMessage
        verify(context).getString(R.string.login_needed_message)
    }
}
