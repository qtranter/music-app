package com.audiomack.network

import java.lang.Exception

data class APIException(
    val statusCode: Int,
    override val cause: Throwable? = null
) : Throwable(cause) {

    constructor(statusCode: Int) : this(statusCode, null)

    override fun toString(): String {
        return ("{\"APIException\":" +
            ", \"statusCode\":\"" + statusCode + "\"" +
            ", " + super.toString() +
            "}")
    }
}

data class APILoginException(
    val errorMessage: String,
    val errorCode: Int?,
    val statusCode: Int,
    val timeout: Boolean
) : Exception(errorMessage) {

    override fun toString(): String {
        return ("{\"APILoginException\":" +
            ", \"errorCode\":\"" + errorCode + "\"" +
            ", \"statusCode\":\"" + statusCode + "\"" +
            ", \"timeout\":\"" + timeout + "\"" +
            ", " + super.toString() +
            "}")
    }
}

sealed class LinkSocialException(override val message: String) : Exception(message) {
    object SocialNotSupported : LinkSocialException("Social not supported for linking")
    object SocialIDAlreadyLinked : LinkSocialException("Social id already linked")
    object Timeout : LinkSocialException("Operation went timeout")
    object Generic : LinkSocialException("An error occurred")
    object Ignore : LinkSocialException("This error must be ignored")
}
