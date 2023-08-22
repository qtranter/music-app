package com.audiomack.data.socialauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.audiomack.BuildConfig
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.authentication.FacebookAuthenticationException
import com.audiomack.data.authentication.GoogleAuthenticationException
import com.audiomack.data.authentication.TwitterAuthenticationException
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.model.Credentials
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.LoginStatusCallback
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.Twitter
import com.twitter.sdk.android.core.TwitterAuthConfig
import com.twitter.sdk.android.core.TwitterConfig
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import com.twitter.sdk.android.core.identity.TwitterAuthClient
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

class SocialAuthManagerImpl(
    private val trackingDataSource: TrackingDataSource = TrackingRepository()
) : SocialAuthManager {

    private var facebookSubject: BehaviorSubject<FacebookAuthData> = BehaviorSubject.create()
    private var facebookCallbackManager: CallbackManager = CallbackManager.Factory.create()

    private var twitterSubject: BehaviorSubject<TwitterAuthData> = BehaviorSubject.create()
    private var twitterAuthClient: TwitterAuthClient? = null

    private var googleSubject: BehaviorSubject<GoogleAuthData> = BehaviorSubject.create()
    private var googleSigninClient: GoogleSignInClient? = null
    private val reqCodeGoogleSignIn = 123

    override fun runFacebookExpressLogin(context: Context) = Single.create<FacebookAuthData> { emitter ->
        LoginManager.getInstance().retrieveLoginStatus(context, object : LoginStatusCallback {
            override fun onCompleted(accessToken: AccessToken?) {
                accessToken?.let { auth ->
                    emitter.onSuccess(FacebookAuthData(auth.userId, auth.token, false))
                } ?: emitter.onError(FacebookExpressLoginError("AccessToken is null"))
            }

            override fun onFailure() {
                emitter.onError(FacebookExpressLoginError("Generic failure"))
            }

            override fun onError(exception: java.lang.Exception?) {
                emitter.onError(FacebookExpressLoginError(exception?.message ?: "Generic error"))
            }
        })
    }

    override fun authenticateWithFacebook(activity: Activity): Observable<FacebookAuthData> {

        facebookSubject = BehaviorSubject.create()

        val permissions = listOf("public_profile", "email")
        val facebookCallback = object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                val token = loginResult.accessToken.token
                val userId = loginResult.accessToken.userId
                Timber.tag("Facebook login").d("Token: $token - UserId: $userId")
                LoginManager.getInstance().unregisterCallback(facebookCallbackManager)
                if (token.isNullOrBlank()) {
                    facebookSubject.onError(FacebookAuthenticationException(activity.getString(R.string.login_error_message_facebook)))
                    trackingDataSource.trackException(Exception("Null or empty facebook token"))
                } else {
                    facebookSubject.onNext(FacebookAuthData(userId, token, false))
                }
            }

            override fun onCancel() {
                LoginManager.getInstance().unregisterCallback(facebookCallbackManager)
            }

            override fun onError(exception: FacebookException) {
                LoginManager.getInstance().unregisterCallback(facebookCallbackManager)
                facebookSubject.onError(FacebookAuthenticationException(exception.message ?: activity.getString(R.string.login_error_message_facebook)))
                trackingDataSource.trackException(exception)
            }
        }

        try {
            LoginManager.getInstance().logOut()
            LoginManager.getInstance().registerCallback(facebookCallbackManager, facebookCallback)
            LoginManager.getInstance().logInWithReadPermissions(activity, permissions)
        } catch (e: Exception) {
            facebookSubject.onError(FacebookAuthenticationException(activity.getString(R.string.login_error_message_facebook)))
        }

        return facebookSubject
    }

    override fun authenticateWithGoogle(activity: Activity): Observable<GoogleAuthData> {

        googleSubject = BehaviorSubject.create()

        val googleSigninOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.AM_GOOGLE_ID)
            .requestEmail()
            .build()
        googleSigninClient = GoogleSignIn.getClient(activity, googleSigninOptions)
        googleSigninClient?.signOut()?.addOnCompleteListener {
            googleSigninClient?.signInIntent?.let {
                activity.startActivityForResult(it, reqCodeGoogleSignIn)
            }
        }

        return googleSubject
    }

    override fun authenticateWithTwitter(activity: Activity): Observable<TwitterAuthData> {

        twitterSubject = BehaviorSubject.create()

        Twitter.initialize(
            TwitterConfig
                .Builder(activity)
                .twitterAuthConfig(
                    TwitterAuthConfig(
                        BuildConfig.AM_TWITTER_CONSUMER_KEY,
                        BuildConfig.AM_TWITTER_CONSUMER_SECRET
                )
                ).build()
        )
        twitterAuthClient = TwitterAuthClient()
        twitterAuthClient?.cancelAuthorize()
        twitterAuthClient?.authorize(activity, object : Callback<TwitterSession>() {
            override fun success(result: Result<TwitterSession>?) {
                val data = result?.data ?: return twitterSubject.onError(TwitterAuthenticationException(activity.getString(R.string.login_twitter_error_message)))
                if (data.authToken.token.isNullOrBlank() || data.authToken.secret.isNullOrBlank()) {
                    twitterSubject.onError(TwitterAuthenticationException(activity.getString(R.string.login_twitter_error_message)))
                    trackingDataSource.trackException(Exception("Null or empty twitter token or secret"))
                } else {
                    twitterSubject.onNext(TwitterAuthData(data.authToken.token, data.authToken.secret, false))
                }
            }

            override fun failure(exception: TwitterException?) {
                Timber.w(exception)
                if (exception != null && exception.message != "Authorization failed, request was canceled.") {
                    twitterSubject.onError(TwitterAuthenticationException(activity.getString(R.string.login_twitter_error_message)))
                }
            }
        })

        return twitterSubject
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        facebookCallbackManager.onActivityResult(requestCode, resultCode, data)

        twitterAuthClient?.onActivityResult(requestCode, resultCode, data)

        if (requestCode == reqCodeGoogleSignIn) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                if (((account.exception as? ApiException)?.message ?: "").startsWith("12501")) {
                    // Operation cancelled by the user
                    return
                }
                account.exception?.let {
                    trackingDataSource.trackException(it)
                }
                val googleSignInAccount = account.getResult(ApiException::class.java)
                val idToken = googleSignInAccount?.idToken ?: return googleSubject.onError(GoogleAuthenticationException(MainApplication.context?.getString(R.string.login_google_error_message) ?: ""))
                if (idToken.isBlank()) {
                    googleSubject.onError(GoogleAuthenticationException(MainApplication.context?.getString(R.string.login_google_error_message) ?: ""))
                    trackingDataSource.trackException(Exception("Empty or null google token"))
                } else {
                    googleSubject.onNext(GoogleAuthData(idToken, false))
                }
            } catch (e: Exception) {
                Timber.w(e)
                trackingDataSource.trackException(e)
                googleSubject.onError(GoogleAuthenticationException(MainApplication.context?.getString(R.string.login_google_error_message) ?: ""))
            }
            return
        }
    }

    override fun refreshFacebookToken(): Observable<Boolean> {
        return Observable.create { emitter ->
            Timber.tag("Facebook refresh").d("Refreshing facebook token")
            if (AccessToken.getCurrentAccessToken() == null) {
                emitter.onError(Exception("No facebook token found, refresh aborted"))
                return@create
            }
            AccessToken.refreshCurrentAccessTokenAsync(object :
                AccessToken.AccessTokenRefreshCallback {
                override fun OnTokenRefreshed(accessToken: AccessToken) {
                    Timber.tag("Facebook refresh").d("Facebook token refreshed")
                    emitter.onNext(true)
                    emitter.onComplete()
                }

                override fun OnTokenRefreshFailed(exception: FacebookException) {
                    Timber.tag("Facebook refresh").d("Failed to refresh Facebook token")
                    emitter.onError(exception)
                    emitter.onComplete()
                }
            })
        }
    }

    override fun refreshGoogleToken(): Observable<Boolean> {
        return Observable.create { emitter ->
            Timber.tag("Google refresh").d("Refreshing Google token")
            val googleSigninOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.AM_GOOGLE_ID)
                .requestEmail()
                .build()
            googleSigninClient = GoogleSignIn.getClient(MainApplication.context!!, googleSigninOptions)
            googleSigninClient?.silentSignIn()?.addOnCompleteListener {
                try {
                    it.result?.idToken?.let {
                        val credentials = Credentials.load(MainApplication.context)
                        if (credentials != null) {
                            credentials.googleToken = it
                            Credentials.save(credentials, MainApplication.context!!)
                        }
                        Timber.tag("Google refresh").d("Google token refreshed")
                        emitter.onNext(true)
                    } ?: run {
                        Timber.tag("Google refresh").d("Failed to refresh Google token")
                        emitter.onNext(false)
                    }
                } catch (e: Exception) {
                    Timber.w(e)
                    emitter.onNext(false)
                }
                emitter.onComplete()
            }
        }
    }
}
