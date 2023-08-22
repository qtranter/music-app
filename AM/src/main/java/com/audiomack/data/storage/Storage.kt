package com.audiomack.data.storage

import android.database.sqlite.SQLiteOpenHelper
import androidx.core.content.FileProvider
import com.audiomack.BuildConfig
import com.audiomack.DOWNLOAD_FOLDER
import com.audiomack.model.AMResultItem
import java.io.File

const val DIR_SHARE = "share"
const val DB_AUDIOMACK = "Audiomack.db"
const val AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider"

interface Storage {
    /**
     * Returns the directory used for offline song files, or null if the primary storage volume is
     * unavailable.
     */
    val offlineDir: File?

    /**
     * Hidden app-specific directory, or null if the primary storage volume is unavailable.
     */
    val internalDir: File?

    /**
     * Publicly accessible "external" app-specific directory, or null if the primary storage
     * volume is unavailable.
     */
    val externalDir: File?

    /**
     * Internal cache directory, or null if the primary storage volume is
     * unavailable.
     */
    val cacheDir: File?

    /**
     * Internal directory used for internal sharing, exposed by [FileProvider] or null if the
     * primary storage volume is unavailable.
     */
    val shareDir: File?

    /**
     * Internal directory where databases opened with [SQLiteOpenHelper], or null if the
     * primary storage volume is unavailable.
     */
    val databaseDir: File?

    /**
     * Returns true if the given [file] is considered valid for playback
     */
    fun isFileValid(file: File?): Boolean
}

class StorageException(override val message: String?) : Exception(message)

/**
 * Returns the offline file for the given item, or null if the primary storage volume is
 * unavailable.
 */
fun Storage.getFile(item: AMResultItem): File? {
    // Old filenames were not simply saved as the id
    val fullPath = item.fullPath
    if (fullPath != null) {
        val split = fullPath.split("$DOWNLOAD_FOLDER/")
        if (!split.isNullOrEmpty()) {
            val relativePath = split[1]
            return offlineDir?.let { File(it, relativePath) }
        }
    }
    return offlineDir?.let { File(it, item.itemId) }
}

fun Storage.deleteFile(item: AMResultItem) = getFile(item)?.delete() ?: false

fun Storage.isFileValid(item: AMResultItem) = isFileValid(getFile(item))
