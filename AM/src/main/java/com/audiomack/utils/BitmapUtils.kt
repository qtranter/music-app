package com.audiomack.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import timber.log.Timber

object BitmapUtils {

    fun inputStreamToBase64(inputStream: InputStream): String {
        return inputStream.use {
            val bitmap = BitmapFactory.decodeStream(it)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
    }

    @JvmOverloads
    fun fileToBase64(file: File, targetWidth: Int, targetHeight: Int = targetWidth): String {
        resizeExactly(file, targetWidth, targetHeight)

        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        var bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
        bitmap = rotateBitmapIfRequired(bitmap, file)

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun resizeExactly(croppedImage: File, targetWidth: Int, targetHeight: Int) {
        val originalBitmap = BitmapFactory.decodeFile(croppedImage.absolutePath)
        if (originalBitmap != null && originalBitmap.width != targetWidth && originalBitmap.height != targetHeight) {
            try {
                val scaled = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
                val file = File(croppedImage.absolutePath)
                val fOut = FileOutputStream(file)
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, fOut)
                fOut.flush()
                fOut.close()
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, file: File): Bitmap {
        try {
            val exif = ExifInterface(file.absolutePath)
            val rotate = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                else -> 0
            }
            return if (rotate != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotate.toFloat())
                val rotatedImg = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                rotatedImg
            } else {
                bitmap
            }
        } catch (e: IOException) {
            Timber.w(e)
        }
        return bitmap
    }
}
