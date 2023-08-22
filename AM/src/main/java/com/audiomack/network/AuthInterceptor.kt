package com.audiomack.network

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.text.SpannableString
import com.audiomack.BuildConfig
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.device.DeviceDataSource
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.socialauth.SocialAuthManager
import com.audiomack.data.socialauth.SocialAuthManagerImpl
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.Credentials
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.LogoutReason
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.ui.home.HomeActivity
import com.audiomack.views.AMSnackbar
import com.facebook.AccessToken
import io.reactivex.Single
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer

class AuthInterceptor(
    private val userAgent: String,
    private val deviceDataSource: DeviceDataSource = DeviceRepository,
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    private val socialAuthManager: SocialAuthManager = SocialAuthManagerImpl(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : Interceptor {

    companion object {
        /**
         * Prevents the request from including an access token and secret
         */
        const val TAG_DO_NOT_AUTHENTICATE = "do_not_sign"
        const val TAG_DO_NOT_REFRESH_TOKEN_ON_401 = "do_not_refresh_token_on_401"
    }

    private var refreshing = false
    private var refreshTokenFailureAlertShown = false

    private val userDataSource: UserDataSource by lazy { UserRepository.getInstance() }

    private fun signRequest(request: Request): Request {
        val consumer = OkHttpOAuthConsumer(BuildConfig.AM_CONSUMER_KEY, BuildConfig.AM_CONSUMER_SECRET)

        val credentials = Credentials.load(MainApplication.context!!)
        if (credentials != null && TAG_DO_NOT_AUTHENTICATE != request.tag()) {
            consumer.setTokenWithSecret(credentials.token, credentials.tokenSecret)
        }
        try {
            return consumer.sign(request).unwrap() as Request
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return request
    }

    override fun intercept(chain: Interceptor.Chain): Response {

        // Early exit in case INTERNET permission is not granted, that may happen on some custom ROMs
        if (MainApplication.context!!.checkPermission(Manifest.permission.INTERNET, android.os.Process.myPid(), android.os.Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
            throw IOException("Missing INTERNET permission")
        }

        // Sign request and add common headers
        val request = signRequest(
            chain
                .request()
                .newBuilder()
                .header("User-Agent", userAgent)
                .header("X-Application-Version", deviceDataSource.getAppVersionName())
                .header("X-Application-Platform", "android")
                .build()
            )

        try {
            val response = chain.proceed(request)

            if (response.code == 401 && TAG_DO_NOT_AUTHENTICATE != request.tag() && TAG_DO_NOT_REFRESH_TOKEN_ON_401 != request.tag() && Credentials.isLogged(MainApplication.context) && !refreshing) {

                val responseString = try {
                    response.body?.string()
                } catch (e: Exception) {
                    null
                }

                trackingDataSource.trackException(Exception("401 on request: " + request.url.toString() + " : " + (responseString ?: "")))

                refreshing = true

                val credentials = Credentials.load(MainApplication.context)

                var refreshAMTokenSingle: Single<Credentials>? = null
                if (credentials != null) {
                    if (credentials.isLoggedViaFacebook) {
                        try {
                            val facebookRefreshBeforeTokenRefresh = socialAuthManager.refreshFacebookToken().blockingFirst()
                        } catch (e: Exception) {}
                        if (!credentials.facebookId.isNullOrBlank() && !AccessToken.getCurrentAccessToken()?.token.isNullOrBlank()) {
                            refreshAMTokenSingle = API.getInstance().login(LoginProviderData.Facebook(credentials.facebookId!!, AccessToken.getCurrentAccessToken().token), credentials.socialEmail)
                        }
                    } else if (credentials.isLoggedViaGoogle) {
                        try {
                            val googleRefreshBeforeTokenRefresh = socialAuthManager.refreshGoogleToken().blockingFirst()
                        } catch (e: Exception) {}
                        if (!credentials.googleToken.isNullOrBlank()) {
                            refreshAMTokenSingle = API.getInstance().login(LoginProviderData.Google(credentials.googleToken!!), credentials.socialEmail)
                        }
                    } else if (credentials.isLoggedViaTwitter) {
                        // NO-OP
                    } else if (credentials.isLoggedViaApple) {
                        // NO-OP
                    } else {
                        if (!credentials.email.isNullOrBlank() && !credentials.password.isNullOrBlank()) {
                            refreshAMTokenSingle = API.getInstance().login(LoginProviderData.UsernamePassword(credentials.email!!, credentials.password!!), null)
                        }
                    }
                }

                refreshAMTokenSingle?.let {
                    try {
                        val freshCredentials = refreshAMTokenSingle.blockingGet()
                        var newRequest = request.newBuilder()
                            .removeHeader("Authorization")
                            .build()
                        newRequest = signRequest(newRequest)
                        refreshing = false
                        return chain.proceed(newRequest)
                    } catch (e: Exception) {
                        refreshing = false
                        if ((400 until 500).contains((e.cause as? APILoginException)?.statusCode ?: 0)) {
                            forceLogout()
                        } else {
                            showTokenRefreshFailureAlert()
                        }
                    }
                } ?: run {
                    refreshing = false
                    forceLogout()
                    return response
                }
            }

            return response
        } catch (e: SecurityException) {
            throw IOException("Missing INTERNET permission")
        }
    }

    /** Force logout, most likely because of pw changed **/
    private fun forceLogout() {
        userDataSource.logout(LogoutReason.AMTokenRefresh)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe {
                HomeActivity.instance?.let {
                    AMSnackbar.Builder(it)
                        .withTitle(it.getString(R.string.cannot_refresh_token))
                        .withSubtitle(it.getString(R.string.please_login_again))
                        .withDrawable(R.drawable.ic_snackbar_error)
                        .withSecondary(R.drawable.ic_snackbar_user_grey)
                        .show()
                }
                AuthenticationActivity.show(MainApplication.context, LoginSignupSource.ExpiredSession, Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }

    /** Show token refresh failure alert, just once per session **/
    private fun showTokenRefreshFailureAlert() {
        if (!refreshTokenFailureAlertShown) {
            refreshTokenFailureAlertShown = true
            try {
                if (HomeActivity.instance != null) {
                    AMAlertFragment.show(
                        HomeActivity.instance!!,
                        SpannableString(MainApplication.context!!.getString(R.string.failed_refresh_token_title)),
                        MainApplication.context!!.getString(R.string.failed_refresh_token_message),
                        MainApplication.context!!.getString(R.string.failed_refresh_token_button),
                        null,
                        null,
                        null,
                        null)
                }
            } catch (e: Exception) {}
        }
    }
}
