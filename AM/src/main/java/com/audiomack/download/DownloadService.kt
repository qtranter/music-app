package com.audiomack.download

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.audiomack.NOTIFICATION_CHANNEL_DOWNLOAD_ID
import com.audiomack.NOTIFICATION_DOWNLOAD_PROGRESS_ID
import com.audiomack.R
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.model.DownloadServiceCommand
import com.audiomack.model.DownloadServiceCommandType
import com.audiomack.ui.home.HomeActivity

class DownloadService : Service() {

    private val trackingDataSource: TrackingDataSource = TrackingRepository()
    private val musicDownloader = AMMusicDownloader.getInstance()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_DOWNLOAD_PROGRESS_ID,
            Intent(this, HomeActivity::class.java).apply {
                data = Uri.parse("audiomack://artist_downloads")
                setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_DOWNLOAD_ID)
            .setProgress(0, 0, true)
            .setContentTitle(getString(R.string.download_notification_title))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(musicDownloader.downloadInProgressText)
            )
            .setSmallIcon(R.drawable.notification_icon)
            .setColor(Color.BLACK)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_DOWNLOAD_PROGRESS_ID, notification)

        intent?.getParcelableExtra<DownloadServiceCommand>("command")?.let {
            when (it.commandType) {
                DownloadServiceCommandType.Download -> {
                    when {
                        it.ids.size > 1 -> {
                            trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - prepare to restore ${it.ids.size} download")
                        }
                        it.ids.size == 1 -> {
                            trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - prepare download for ${it.ids.first()}")
                        }
                        else -> return@let
                    }
                    musicDownloader.prepareForDownload(it.ids)
                }
                DownloadServiceCommandType.Stop -> {
                    trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - stop service")
                    stopSelf(startId)
                }
                DownloadServiceCommandType.UpdateNotification -> Unit
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        fun start(context: Context, command: DownloadServiceCommand) {
            ContextCompat.startForegroundService(context,
                Intent(context, DownloadService::class.java).apply {
                    putExtra("command", command)
                }
            )
        }
    }
}
