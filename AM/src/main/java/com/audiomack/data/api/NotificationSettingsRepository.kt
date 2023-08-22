package com.audiomack.data.api

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.audiomack.MainApplication
import com.audiomack.NOTIFICATION_CHANNEL_REMOTE_ID
import com.audiomack.model.NotificationPreferenceType
import com.audiomack.model.NotificationPreferenceTypeValue
import com.audiomack.network.API
import com.audiomack.network.APINotificationSettings
import io.reactivex.Single

class NotificationSettingsRepository(
    private val api: APINotificationSettings = API.getInstance().notificationSettingsAPI
) : NotificationSettingsDataSource {

    override fun areNotificationsEnabledForNewMusic() =
        areNotificationsEnabledAtOSLevelRx()
            .flatMap {
                if (it is NotificationsEnabledResult.DisabledAtOSLevel) Single.just(it)
                else areNewMusicNotificationsEnabledAtAppLevel()
            }

    private fun areNewMusicNotificationsEnabledAtAppLevel(): Single<NotificationsEnabledResult> =
        getNotificationPreferences()
            .map { response ->
                val enabled = response.any { it.type == NotificationPreferenceType.NewSongAlbum && it.value }
                if (!enabled) NotificationsEnabledResult.DisabledAtAppLevel
                else NotificationsEnabledResult.Enabled
            }

    private fun areNotificationsEnabledAtOSLevelRx() = Single.create<NotificationsEnabledResult> { emitter ->
        if (areNotificationsEnabledAtOSLevel()) emitter.onSuccess(NotificationsEnabledResult.Enabled)
        else emitter.onSuccess(NotificationsEnabledResult.DisabledAtOSLevel)
    }

    private fun areNotificationsEnabledAtOSLevel(): Boolean {
        val context = MainApplication.context ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return NotificationManagerCompat.from(context).areNotificationsEnabled()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.areNotificationsEnabled()) return false
        if (manager.notificationChannels.firstOrNull { it.id == NOTIFICATION_CHANNEL_REMOTE_ID }?.importance == NotificationManager.IMPORTANCE_NONE) return false
        return true
    }

    override fun getNotificationPreferences() = api.getNotificationPreferences()

    override fun setNotificationPreference(typeValue: NotificationPreferenceTypeValue) =
        api.setNotificationPreference(typeValue)
}
