package com.audiomack.usecases

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.data.authentication.AuthenticationDataSource
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.socialauth.FacebookAuthData
import com.audiomack.data.socialauth.SocialAuthManager
import com.audiomack.data.telco.TelcoDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AuthenticationType
import com.audiomack.model.Credentials
import com.audiomack.model.LoginSignupSource
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class FacebookExpressLoginUseCaseImplTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var socialAuthManager: SocialAuthManager
    @Mock private lateinit var authenticationDataSource: AuthenticationDataSource
    @Mock private lateinit var trackingDataSource: TrackingDataSource
    @Mock private lateinit var mixpanelDataSource: MixpanelDataSource
    @Mock private lateinit var userDataSource: UserDataSource
    @Mock private lateinit var premiumDataSource: PremiumDataSource
    @Mock private lateinit var telcoDataSource: TelcoDataSource
    @Mock private lateinit var eventBus: EventBus
    private lateinit var schedulers: SchedulersProvider

    private lateinit var sut: FacebookExpressLoginUseCaseImpl

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulers = TestSchedulersProvider()
        sut = FacebookExpressLoginUseCaseImpl(
            socialAuthManager,
            authenticationDataSource,
            trackingDataSource,
            mixpanelDataSource,
            userDataSource,
            premiumDataSource,
            telcoDataSource,
            eventBus,
            schedulers)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `test the complete flow`() {
        val context = mock<Context>()
        val data = FacebookAuthData("1", "xxx", false)
        val credentials = mock<Credentials>()
        whenever(socialAuthManager.runFacebookExpressLogin(context)).thenReturn(Single.just(data))
        whenever(authenticationDataSource.loginWithFacebook(data.id, data.token, null)).thenReturn(Single.just(credentials))
        sut.run(context)
            .test()
            .assertNoErrors()
        verify(socialAuthManager).runFacebookExpressLogin(context)
        verify(authenticationDataSource).loginWithFacebook(data.id, data.token, null)
        verify(userDataSource).onLoggedIn()
        verify(mixpanelDataSource).trackLogin(LoginSignupSource.AppLaunch, AuthenticationType.Facebook, userDataSource, premiumDataSource, telcoDataSource)
        verify(trackingDataSource).trackLogin()
    }
}
