package com.audiomack.playback

import android.app.Application
import android.content.ComponentName
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.ConnectionCallback
import androidx.lifecycle.AndroidViewModel

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private var mediaBrowser: MediaBrowserCompat? = null

    fun connect() {
        val application = getApplication<Application>()
        mediaBrowser = MediaBrowserCompat(
            application,
            ComponentName(application, MusicService::class.java),
            ConnectionCallback(),
            null
        ).apply {
            try {
                if (!isConnected) connect()
            } catch (e: Exception) {
                // See https://github.com/android/uamp/issues/251
            }
        }
    }

    override fun onCleared() {
        try {
            mediaBrowser?.disconnect()
        } catch (e: Exception) {
            // See https://github.com/android/uamp/issues/251
        }
    }
}
