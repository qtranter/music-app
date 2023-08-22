package com.audiomack.playback.cast

import android.content.Context
import com.audiomack.BuildConfig
import com.audiomack.ui.home.HomeActivity
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.ImageHints
import com.google.android.gms.cast.framework.media.ImagePicker
import com.google.android.gms.cast.framework.media.NotificationOptions

/**
 * Set in the Manifest for [com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME]
 */
@Suppress("unused")
class CastOptionsProvider : OptionsProvider {

    private val notificationOptions = NotificationOptions.Builder()
        .setTargetActivityClassName(HomeActivity::class.java.name)
        .build()

    private val castMediaOptions = CastMediaOptions.Builder()
        .setImagePicker(AMImagePicker())
        .setExpandedControllerActivityClassName(HomeActivity::class.java.name)
        .setNotificationOptions(notificationOptions)
        .setMediaSessionEnabled(false) // TODO Remove once we're using playlists
        .build()

    override fun getCastOptions(context: Context?): CastOptions = CastOptions.Builder()
        .setReceiverApplicationId(BuildConfig.AM_CHROMECAST_RECEIVER_APP_ID)
        .setCastMediaOptions(castMediaOptions)
        .build()

    override fun getAdditionalSessionProviders(context: Context?) = null

    class AMImagePicker : ImagePicker() {
        override fun onPickImage(metadata: MediaMetadata, hints: ImageHints) =
            metadata.images.firstOrNull()
    }
}
