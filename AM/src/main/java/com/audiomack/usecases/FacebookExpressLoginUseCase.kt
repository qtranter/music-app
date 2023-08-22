package com.audiomack.usecases

import android.content.Context
import com.audiomack.data.authentication.AuthenticationDataSource
import com.audiomack.data.authentication.AuthenticationRepository
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.socialauth.SocialAuthManager
import com.audiomack.data.socialauth.SocialAuthManagerImpl
import com.audiomack.data.telco.TelcoDataSource
import com.audiomack.data.telco.TelcoRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AuthenticationType
import com.audiomack.model.LoginSignupSource
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.Completable
import org.greenrobot.eventbus.EventBus

interface FacebookExpressLoginUseCase {
    fun run(context: Context): Completable
}

class FacebookExpressLoginUseCaseImpl(
    private val socialAuthManager: SocialAuthManager = SocialAuthManagerImpl(),
    private val authenticationDataSource: AuthenticationDataSource = AuthenticationRepository(),
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val telcoDataSource: TelcoDataSource = TelcoRepository(),
    private val eventBus: EventBus = EventBus.getDefault(),
    private val schedulers: SchedulersProvider = AMSchedulersProvider()
) : FacebookExpressLoginUseCase {

    override fun run(context: Context): Completable =
        socialAuthManager.runFacebookExpressLogin(context)
            .subscribeOn(schedulers.main)
            .flatMap { credentials ->
                authenticationDataSource.loginWithFacebook(credentials.id, credentials.token, null)
                    .subscribeOn(schedulers.io)
            }
            .ignoreElement()
            .andThen {
                userDataSource.onLoggedIn()
                mixpanelDataSource.trackLogin(LoginSignupSource.AppLaunch, AuthenticationType.Facebook, userDataSource, premiumDataSource, telcoDataSource)
                trackingDataSource.trackLogin()
                Completable.complete()
            }
}
