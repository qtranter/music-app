package com.audiomack.utils.extensions

import android.content.Context
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun Context.drawableCompat(@DrawableRes id: Int) = AppCompatResources.getDrawable(this, id)

fun Context.colorCompat(@ColorRes id: Int) = ContextCompat.getColor(this, id)

/**
 * Copies stream from the uri path to the destination file
 *
 * @param sourceUri the Uri path of the source
 * @param dst the destination file to copy stream to
 *
 * @return the number of bytes copied
 */
@Throws(IOException::class)
fun Context.copyInputStreamToFile(sourceUri: Uri, dst: File): Long {
    return FileOutputStream(dst).use { output ->
        contentResolver.openInputStream(sourceUri)?.use { input ->
            input.copyTo(output)
        } ?: 0L
    }
}
