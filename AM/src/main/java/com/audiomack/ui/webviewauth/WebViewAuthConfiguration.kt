package com.audiomack.ui.webviewauth

import android.net.Uri
import android.os.Parcelable
import java.util.UUID
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WebViewAuthConfiguration(
    val allowedDomain: String,
    val interceptRedirectUri: String,
    val interceptRedirectUriQueryStringParamName: String,
    val authenticationUri: String
) : Parcelable

class WebViewAuthConfigurationFactory {

    fun createAppleConfiguration(
        clientId: String,
        redirectUri: String,
        state: String = UUID.randomUUID().toString()
    ) = WebViewAuthConfiguration(
        "appleid.apple.com",
        redirectUri,
        "id_token",
        Uri.parse("https://appleid.apple.com/auth/authorize")
            .buildUpon()
            .apply {
                appendQueryParameter("response_type", "code id_token")
                appendQueryParameter("client_id", clientId)
                appendQueryParameter("redirect_uri", redirectUri)
                appendQueryParameter("scope", "email")
                appendQueryParameter("state", state)
                appendQueryParameter("response_mode", "form_post")
            }
            .build()
            .toString()
    )

    fun createInstagramConfiguration(
        clientId: String,
        redirectUri: String
    ) = WebViewAuthConfiguration(
        "api.instagram.com",
        redirectUri,
        "code",
        Uri.parse("https://api.instagram.com/oauth/authorize")
            .buildUpon()
            .apply {
                appendQueryParameter("response_type", "code")
                appendQueryParameter("client_id", clientId)
                appendQueryParameter("redirect_uri", redirectUri)
                appendQueryParameter("scope", "user_profile")
            }
            .build()
            .toString()
    )
}
