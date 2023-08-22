package com.audiomack.data.storage

import android.content.Context
import com.audiomack.DOWNLOAD_FOLDER
import com.audiomack.MainApplication
import java.io.File

class StorageProvider private constructor(private val context: Context) : Storage {

    override val offlineDir: File?
        get() = getOfflineDirectory(context)?.apply { mkdirs() }

    override val internalDir: File?
        get() = context.filesDir

    override val externalDir: File?
        get() = context.getExternalFilesDir(null)

    override val cacheDir: File?
        get() = context.cacheDir

    override val shareDir: File?
        get() = File(internalDir, DIR_SHARE).apply { mkdirs() }

    override val databaseDir: File?
        get() = context.getDatabasePath(DB_AUDIOMACK).parentFile

    override fun isFileValid(file: File?): Boolean =
        file?.let { it.exists() && it.isFile && it.length() > 1024 } ?: false

    companion object {
        @Volatile
        private var instance: StorageProvider? = null

        @JvmStatic
        fun init(context: Context): StorageProvider = instance ?: synchronized(this) {
            instance ?: StorageProvider(context).also { instance = it }
        }

        @JvmStatic
        fun getInstance(): StorageProvider =
            instance ?: MainApplication.context?.let { init(it) }
            ?: throw IllegalStateException("StorageProvider was not initialized")

        /**
         * Returns the directory used for offline song files, or null if the primary storage volume is
         * unavailable.
         */
        @JvmStatic
        fun getOfflineDirectory(context: Context): File? {
            val dir = context.getExternalFilesDir(null) ?: return null
            return File(dir, DOWNLOAD_FOLDER)
        }
    }
}
