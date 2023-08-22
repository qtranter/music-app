package com.audiomack.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.audiomack.NOTIFICATION_CHANNEL_REMOTE_ID
import com.audiomack.R
import com.audiomack.data.imageloader.ImageLoaderCallback
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.support.ZendeskRepository
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.tracking.mixpanel.MixpanelTrackerImpl
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.getJSONObjectOrNull
import com.audiomack.utils.getStringOrNull
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class AMFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        ZendeskRepository().registerForPush(token)
        MixpanelTrackerImpl.setPushToken(token)
    }

    override fun onMessageReceived(rm: RemoteMessage) {
        super.onMessageReceived(rm)

        val title = rm.data["mp_title"] ?: rm.notification?.title
        val message = rm.data["lp_message"] ?: rm.data["mp_message"] ?: rm.notification?.body

        if (message.isNullOrBlank()) {
            return
        }

        val imageUrl = rm.data["lp_imageUrl"] ?: rm.data["mp_img"] ?: rm.data["am_imageUrl"]

        val deeplink = if (rm.data.containsKey("_lpx")) {
            JSONObject(rm.data["_lpx"]).optString("URL")
        } else {
            // "mp_ontap" and "mp_cta" are both used by Mixpanel
            rm.data["am_deeplink"] ?: rm.data["mp_ontap"]?.let {
                try {
                    JSONObject(it).getStringOrNull("uri")
                } catch (e: Exception) { null }
            } ?: rm.data["mp_cta"]
        }

        val buttons = rm.data["mp_buttons"]?.let {
            buildMixpanelButtons(it)
        } ?: emptyList()

        val extras = mutableMapOf<String, String>()
        rm.data.keys.filter { it.startsWith("mp") }.forEach { extras[it] = rm.data[it] ?: "" }

        if (!imageUrl.isNullOrBlank()) {
            Handler(Looper.getMainLooper()).post {
                PicassoImageLoader.load(
                    this,
                    imageUrl,
                    config = Bitmap.Config.RGB_565,
                    callback = object : ImageLoaderCallback {
                    override fun onBitmapLoaded(bitmap: Bitmap?) {
                        sendNotification(this@AMFirebaseMessagingService, title, message, bitmap, deeplink, extras, buttons)
                    }

                    override fun onBitmapFailed(errorDrawable: Drawable?) {
                        sendNotification(this@AMFirebaseMessagingService, title, message, null, deeplink, extras, buttons)
                    }
                })
            }
        } else {
            sendNotification(this, title, message, null, deeplink, extras, buttons)
        }
    }

    private fun sendNotification(context: Context, title: String?, message: String?, bitmap: Bitmap?, deeplink: String?, extras: Map<String, String>, buttons: List<NotificationButton>) {

        val requestID = System.currentTimeMillis().toInt()
        val intent = Intent(context, HomeActivity::class.java)
        deeplink?.let {
            try {
                intent.data = Uri.parse(it)
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
        extras.forEach {
            intent.putExtra(it.key, it.value)
        }

        val pendingIntent = PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_REMOTE_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setLargeIcon(bitmap)
            .setColor(context.colorCompat(R.color.black))
            .setStyle(
                bitmap?.let {
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                } ?: run {
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                }
            )

        if (!title.isNullOrEmpty()) {
            notificationBuilder.setContentTitle(title)
        }

        buttons.forEachIndexed { index, button ->
            val actionIntent = Intent(context, HomeActivity::class.java).apply {
                try {
                    data = Uri.parse(button.link)
                } catch (e: Exception) {
                    Timber.w(e)
                }
                putExtra("notificationId", requestID)
            }
            val actionPendingIntent = PendingIntent.getActivity(context, requestID + index + 1, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            notificationBuilder.addAction(NotificationCompat.Action(-1, button.label, actionPendingIntent))
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = notificationBuilder.build()
        notificationManager.notify(requestID, notification)

        MixpanelRepository().trackPushReceived(intent)
    }

    private fun buildMixpanelButtons(string: String): List<NotificationButton> {
        return try {
            val jsonArray = JSONArray(string)
            (0 until jsonArray.length())
                .mapNotNull { jsonArray.getJSONObjectOrNull(it) }
                .mapNotNull { NotificationButton.fromJSON(it) }
        } catch (e: Exception) {
            Timber.w(e)
            emptyList()
        }
    }

    data class NotificationButton(
        val label: String,
        val link: String
    ) {
        companion object {
            fun fromJSON(json: JSONObject): NotificationButton? {
                val label = json.getStringOrNull("lbl")
                val link = json.getStringOrNull("ontap")?.let {
                    try {
                        JSONObject(it).getStringOrNull("uri")
                    } catch (e: Exception) { null }
                }
                return if (label.isNullOrBlank() || link.isNullOrBlank()) {
                    null
                } else {
                    NotificationButton(label, link)
                }
            }
        }
    }
}
