package com.audiomack.data.remotevariables.datasource

import com.audiomack.BuildConfig
import com.audiomack.data.remotevariables.BooleanRemoteVariable
import com.audiomack.data.remotevariables.LongRemoteVariable
import com.audiomack.data.remotevariables.RemoteVariable
import com.audiomack.data.remotevariables.StringRemoteVariable
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import io.reactivex.Observable
import java.lang.Exception
import timber.log.Timber

object FirebaseRemoteVariablesDataSource : RemoteVariablesDataSource {

    override fun init(list: List<RemoteVariable>): Observable<Boolean> {
        return Observable.create { emitter ->
            try {
                val initStartTime = System.currentTimeMillis()
                FirebaseRemoteConfig.getInstance().setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder().apply {
                        if (BuildConfig.AUDIOMACK_DEBUG) {
                            minimumFetchIntervalInSeconds = 0L
                        }
                    }.build()
                )
                FirebaseRemoteConfig.getInstance().setDefaultsAsync(
                    list.map { it.key to it.default }.toMap()
                )
                FirebaseRemoteConfig.getInstance().fetchAndActivate().addOnCompleteListener {
                    emitter.onNext(true)
                    emitter.onComplete()
                }
            } catch (e: Exception) {
                Timber.w(e)
                emitter.onNext(false)
            }
        }
    }

    override fun getBoolean(remoteVariable: BooleanRemoteVariable): Boolean {
        return try {
            FirebaseRemoteConfig.getInstance().getBoolean(remoteVariable.key)
        } catch (e: Exception) {
            Timber.w(e)
            remoteVariable.booleanDefault
        }
    }

    override fun getLong(remoteVariable: LongRemoteVariable): Long {
        return try {
            FirebaseRemoteConfig.getInstance().getLong(remoteVariable.key)
        } catch (e: Exception) {
            Timber.w(e)
            remoteVariable.longDefault
        }
    }

    override fun getString(remoteVariable: StringRemoteVariable): String {
        return try {
            FirebaseRemoteConfig.getInstance().getString(remoteVariable.key)
        } catch (e: Exception) {
            Timber.w(e)
            remoteVariable.stringDefault
        }
    }
}
