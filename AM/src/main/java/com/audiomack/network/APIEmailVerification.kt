package com.audiomack.network

import io.reactivex.Completable
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request

class APIEmailVerification(
    private val client: OkHttpClient,
    private val baseUrl: String
) : APIInterface.EmailVerificationInterface {

    override fun runEmailVerification(hash: String): Completable = Completable.create { emitter ->

        val bodyBuilder = FormBody.Builder()
            .add("hash", hash)

        val request = Request.Builder()
            .url("${baseUrl}user/email-verify")
            .post(bodyBuilder.build())
            .build()

        val okHttpClient = client.newBuilder().apply {
            interceptors().apply {
                (filterIsInstance<AuthInterceptor>() as? Interceptor)?.let { remove(it) }
            }
        }.build()

        val call = okHttpClient.newCall(request)
        emitter.setCancellable(call::cancel)
        call.enqueue(CompletableCallback(emitter))
    }
}
