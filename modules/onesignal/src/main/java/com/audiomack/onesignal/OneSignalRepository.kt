package com.audiomack.onesignal

import android.content.Context
import com.onesignal.OneSignal
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

interface OneSignalDataSource {
    /**
     * Emits a [OneSignalNotificationParseResult] with the Uri to be opened and some metadata for tracking purposes
     */
    val result: Subject<OneSignalNotificationParseResult>

    /**
     * The OneSignal ID for the user, it's different from the "External" one we pass to the SDK
     */
    val playerId: String?

    /**
     * Tells the SDK to use the Audiomack user id as the external ID
     * @param userId: ID of the logged in user
     */
    fun setExternalUserId(userId: String)

    /**
     * Tells the SDK to remove any references to the logged in external user, to be used on logout
     */
    fun removeExternalUserId()
}

class OneSignalRepository private constructor(
    applicationContext: Context,
    debug: Boolean,
    openHandler: OneSignalNotificationOpenHandler
) : OneSignalDataSource {

    private var subject = BehaviorSubject.create<OneSignalNotificationParseResult>()

    init {
        OneSignal.startInit(applicationContext)
            .setNotificationOpenedHandler {
                it.notification.payload.additionalData?.let { payload ->
                    openHandler.processNotification(payload)?.let { result ->
                        subject.onNext(result)
                    }
                }
            }
            .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
            .unsubscribeWhenNotificationsAreDisabled(true)
            .autoPromptLocation(false)
            .init()
        OneSignal.setLogLevel(
            if (debug) OneSignal.LOG_LEVEL.DEBUG else OneSignal.LOG_LEVEL.NONE,
            OneSignal.LOG_LEVEL.NONE
        )
        OneSignal.setLocationShared(false)
    }

    override val result: Subject<OneSignalNotificationParseResult>
        get() = subject

    override val playerId: String?
        get() = OneSignal.getPermissionSubscriptionState()?.subscriptionStatus?.userId

    override fun setExternalUserId(userId: String) {
        OneSignal.setExternalUserId(userId)
    }

    override fun removeExternalUserId() {
        OneSignal.removeExternalUserId()
    }

    companion object {
        @Volatile
        private var INSTANCE: OneSignalRepository? = null

        fun init(applicationContext: Context, debug: Boolean): OneSignalRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: OneSignalRepository(
                    applicationContext,
                    debug,
                    OneSignalNotificationOpenHandler()
                ).also { INSTANCE = it }
            }

        @JvmStatic
        fun getInstance(): OneSignalRepository =
            INSTANCE ?: throw IllegalStateException("OneSignalRepository was not initialized")
    }
}
