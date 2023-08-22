package com.audiomack.data.premium

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.audiomack.MainApplication
import com.audiomack.PREMIUM_ADMIN_OVERRIDE_PREMIUM
import com.audiomack.PREMIUM_GOLD_PREFERENCES
import com.audiomack.PREMIUM_GRANT_PREMIUM_FOR_ADMINS
import com.audiomack.PREMIUM_PLATINUM_PREFERENCES
import com.audiomack.PREMIUM_PREFERENCES
import com.audiomack.PREMIUM_PREMIUM2018_PREFERENCES
import com.audiomack.PREMIUM_REVENUECAT_PREFERENCES
import com.audiomack.utils.SecureSharedPreferences
import io.reactivex.subjects.PublishSubject

class PremiumSettingsRepository private constructor(context: Context) : PremiumSettingsDataSource {

    private val preferences = SecureSharedPreferences(context, PREMIUM_PREFERENCES)

    override var savedPremium: Boolean
        get() = preferences.getString(PREMIUM_REVENUECAT_PREFERENCES)?.toBoolean() ?: false
        set(value) = preferences.put(PREMIUM_REVENUECAT_PREFERENCES, value.toString())

    override var adminOverride: Boolean
        get() = preferences.getString(PREMIUM_ADMIN_OVERRIDE_PREMIUM)?.toBoolean() ?: false
        set(value) {
            preferences.put(PREMIUM_ADMIN_OVERRIDE_PREMIUM, value.toString())
            adminGrantPremiumObservable.onNext(value && adminGrantPremium)
        }

    override var adminGrantPremium: Boolean
        get() = adminOverride && preferences.getString(PREMIUM_GRANT_PREMIUM_FOR_ADMINS)
            ?.toBoolean() == true
        set(value) {
            preferences.put(PREMIUM_GRANT_PREMIUM_FOR_ADMINS, value.toString())
            adminGrantPremiumObservable.onNext(adminOverride && value)
        }

    override val adminGrantPremiumObservable = PublishSubject.create<Boolean>()

    override val isLegacyPremium: Boolean =
        when {
            preferences.getString(PREMIUM_GOLD_PREFERENCES)?.toBoolean() == true -> true
            preferences.getString(PREMIUM_PLATINUM_PREFERENCES)?.toBoolean() == true -> true
            preferences.getString(PREMIUM_PREMIUM2018_PREFERENCES)?.toBoolean() == true -> true
            else -> false
        }

    override fun deleteLegacyPremium() {
        preferences.put(PREMIUM_GOLD_PREFERENCES, false.toString())
        preferences.put(PREMIUM_PLATINUM_PREFERENCES, false.toString())
        preferences.put(PREMIUM_PREMIUM2018_PREFERENCES, false.toString())
    }

    companion object {
        @Volatile
        private var instance: PremiumSettingsRepository? = null

        @JvmStatic
        fun init(context: Context): PremiumSettingsRepository = instance ?: synchronized(this) {
            instance ?: PremiumSettingsRepository(context).also { instance = it }
        }

        @JvmStatic
        fun getInstance(): PremiumSettingsRepository =
            instance ?: MainApplication.context?.let { init(it) }
            ?: throw IllegalStateException("PremiumSettingsRepository was not initialized")

        @VisibleForTesting
        fun destroy() {
            instance = null
        }
    }
}
