package com.audiomack.data.socialauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.Single
import java.lang.Exception

interface SocialAuthManager {

    fun runFacebookExpressLogin(context: Context): Single<FacebookAuthData>

    fun authenticateWithFacebook(activity: Activity): Observable<FacebookAuthData>

    fun authenticateWithGoogle(activity: Activity): Observable<GoogleAuthData>

    fun authenticateWithTwitter(activity: Activity): Observable<TwitterAuthData>

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    fun refreshFacebookToken(): Observable<Boolean>

    fun refreshGoogleToken(): Observable<Boolean>
}

class FacebookExpressLoginError(message: String) : Exception(message)

class FacebookAuthData(val id: String, val token: String, val missingEmail: Boolean)

class GoogleAuthData(val idToken: String, val missingEmail: Boolean)

class TwitterAuthData(val token: String, val secret: String, val missingEmail: Boolean)

class AppleAuthData(val token: String)
