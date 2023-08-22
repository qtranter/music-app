package com.audiomack.data.screenshot

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Point
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import java.util.Date
import kotlin.math.abs

class ScreenshotContentObserver(handler: Handler, private val screenSize: Point, private val contentResolver: ContentResolver, val listener: () -> Unit) : ContentObserver(handler) {

    companion object {
        const val MAX_ALLOWED_DATE_OFFSET_MS = 30_000
    }

    private val externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString()

    private val projection = arrayOf(
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.ImageColumns.DATE_ADDED
    )

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        uri?.let { processUri(it) }
    }

    private fun processUri(uri: Uri) {
        if (!uri.toString().startsWith(externalContentUri)) return

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, null, null, null)

            if (cursor != null && cursor.moveToFirst()) {

                val width = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.WIDTH))
                val height = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT))
                val dateAdded = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)) * 1_000

                if (screenSize.x == width && screenSize.y == height && abs(Date().time - dateAdded) < MAX_ALLOWED_DATE_OFFSET_MS) {
                    listener()
                }
            }
        } finally {
            cursor?.close()
        }
    }
}
