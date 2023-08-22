package com.audiomack.startup

import android.content.Context
import androidx.startup.Initializer
import com.audiomack.BuildConfig
import com.audiomack.onesignal.OneSignalRepository

class OneSignalInitializer : Initializer<OneSignalRepository> {
    override fun create(context: Context) =
        OneSignalRepository.init(context, BuildConfig.AUDIOMACK_DEBUG)

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
