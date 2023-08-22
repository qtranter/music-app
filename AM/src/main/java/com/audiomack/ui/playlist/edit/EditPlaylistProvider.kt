package com.audiomack.ui.playlist.edit

import androidx.annotation.VisibleForTesting
import com.audiomack.MainApplication
import com.audiomack.data.storage.Storage
import com.audiomack.data.storage.StorageProvider
import com.audiomack.model.AMResultItem
import com.audiomack.utils.BitmapUtils
import java.io.File
import java.io.InputStream

class EditPlaylistProvider private constructor(
    storage: Storage
) : EditPlaylistItemProvider, EditPlaylistImageProvider {

    override val playlist: AMResultItem?
        get() { return MainApplication.playlist }

    private val dir = File(storage.cacheDir, "playlist").apply { mkdirs() }

    override val imageFile = File(dir, "playlist.jpg")

    override val bannerFile = File(dir, "playlist-banner.jpg")

    override fun inputStreamToBase64(inputStream: InputStream): String {
        return BitmapUtils.inputStreamToBase64(inputStream)
    }

    override fun fileToBase64(file: File): String {
        return BitmapUtils.fileToBase64(file, AMResultItem.PLAYLIST_IMAGE_MAX_SIZE_PX)
    }

    companion object {
        @Volatile
        private var INSTANCE: EditPlaylistProvider? = null

        fun getInstance(
            storage: Storage = StorageProvider.getInstance()
        ): EditPlaylistProvider = INSTANCE ?: synchronized(this) {
            INSTANCE ?: EditPlaylistProvider(storage).also { INSTANCE = it }
        }

        @VisibleForTesting
        fun destroy() {
            INSTANCE = null
        }
    }
}
