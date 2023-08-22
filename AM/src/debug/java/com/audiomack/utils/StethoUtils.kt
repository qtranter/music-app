package com.audiomack.utils

import android.content.Context
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import okhttp3.OkHttpClient

object StethoUtils {
    fun initStetho(context: Context) {
        Stetho.initializeWithDefaults(context)
    }
    fun addNetworkInterceptor(builder: OkHttpClient.Builder) {
        builder.addNetworkInterceptor(StethoInterceptor())
    }
}
