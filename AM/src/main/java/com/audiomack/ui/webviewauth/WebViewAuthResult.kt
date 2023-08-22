package com.audiomack.ui.webviewauth

sealed class WebViewAuthResult {
    data class Success(val token: String) : WebViewAuthResult()
    data class Failure(val error: Throwable) : WebViewAuthResult()
    object Cancel : WebViewAuthResult()
}
