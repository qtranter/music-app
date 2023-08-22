package com.audiomack.data.screenshot

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.audiomack.ui.common.Permission
import com.audiomack.ui.common.PermissionHandler
import com.audiomack.ui.mylibrary.offline.local.StoragePermissionHandler
import com.audiomack.utils.getScreenRealSize

class ScreenshotDetector(
    private val context: Context,
    private val storagePermissions: PermissionHandler<Permission.Storage> = StoragePermissionHandler.getInstance(),
    listener: () -> Unit
) : LifecycleObserver {

    private val handlerThread = HandlerThread("ScreenshotDetector").also { it.start() }
    private val handler = Handler(handlerThread.looper)
    private val contentObserver = ScreenshotContentObserver(handler, context.getScreenRealSize(), context.contentResolver, listener)

    @OnLifecycleEvent(value = Lifecycle.Event.ON_RESUME)
    fun start() {
        if (!storagePermissions.hasPermission) {
            context.contentResolver.unregisterContentObserver(contentObserver)
            return
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_PAUSE)
    fun stop() {
        context.contentResolver.unregisterContentObserver(contentObserver)
    }
}
