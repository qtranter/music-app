package com.audiomack.data.api

import com.audiomack.model.NotificationPreferenceTypeValue
import io.reactivex.Single

interface NotificationSettingsDataSource {

    /**
     * Checks wether notifications are enabled at OS level or new song/album notifications are enabled at APP level.
     * @emits [NotificationsEnabledResult]
     */
    fun areNotificationsEnabledForNewMusic(): Single<NotificationsEnabledResult>

    /**
     * Fetches remote notification preferences.
     * @emits a list of notification preferences with a type and status.
     */
    fun getNotificationPreferences(): Single<List<NotificationPreferenceTypeValue>>

    /**
     * Updates remote notification preferences.
     * @emits true if everything went fine or false in case of errors.
     */
    fun setNotificationPreference(typeValue: NotificationPreferenceTypeValue): Single<Boolean>
}

sealed class NotificationsEnabledResult {
    object Enabled : NotificationsEnabledResult()
    object DisabledAtOSLevel : NotificationsEnabledResult()
    object DisabledAtAppLevel : NotificationsEnabledResult()
}
