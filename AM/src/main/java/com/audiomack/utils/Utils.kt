package com.audiomack.utils

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.audiomack.MainApplication
import com.audiomack.data.storage.StorageProvider
import com.audiomack.usecases.SaveImageUseCase
import io.reactivex.Single
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.math.RoundingMode
import java.net.URLEncoder
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt
import timber.log.Timber

object Utils {

    val isTwitterAppInstalled: Boolean
        get() {
            return try {
                MainApplication.context!!.packageManager.getApplicationInfo(
                    "com.twitter.android",
                    0
                )
                true
            } catch (e: Exception) {
                false
            }
        }

    val isInstagramAppInstalled: Boolean
        get() {
            return try {
                MainApplication.context!!.packageManager.getApplicationInfo(
                    "com.instagram.android",
                    0
                )
                true
            } catch (e: Exception) {
                false
            }
        }

    fun timeFromMilliseconds(milliseconds: Long): String {
        if (milliseconds >= 86400000 || milliseconds < 0) {
            return "-:--"
        }
        val minutes = floor(milliseconds.toDouble() / (1000f * 60f)).toInt()
        val seconds =
            if (minutes == 0) floor(milliseconds.toDouble() / 1000f).toInt() else floor((milliseconds % (minutes * 1000 * 60) / 1000).toDouble()).toInt()
        val formattedTime = minutes.toString() + ":" + String.format(Locale.US, "%02d", seconds)
        return if (formattedTime.length <= 6) {
            formattedTime
        } else {
            "-:--"
        }
    }

    fun deslash(url: String): String {
        return url.replace("\\\\".toRegex(), "")
    }

    @Throws(IOException::class)
    fun copy(src: File, dst: File) {
        val input = FileInputStream(src)
        val output = FileOutputStream(dst)
        val buf = ByteArray(1024)
        var len: Int = input.read(buf)
        while (len > 0) {
            output.write(buf, 0, len)
            len = input.read(buf)
        }
        input.close()
        output.close()
    }

    fun moveFile(srcPath: String, destPath: String) {
        val src = File(srcPath)
        val dest = File(destPath)
        dest.parentFile.mkdirs()
        src.renameTo(dest)
    }

    /**
     * Creates and returns a Rx Single object to copy stream from the source uri
     * to the destination file
     *
     * @param saveImageUseCase SaveImageUseCase abstraction for context.copyInputStreamToFile()
     * @param sourceUri the Uri path of the stream source
     * @param dst the destination file to copy stream to
     *
     * @return Single<Boolean>
     */
    fun saveImageFileFromUri(
        saveImageUseCase: SaveImageUseCase,
        sourceUri: Uri?,
        dst: File?
    ): Single<Boolean> {
        return Single.create { emitter ->
            val inputUri = sourceUri ?: run {
                emitter.tryOnError(IllegalStateException("the source uri is null"))
                return@create
            }

            val destFile = dst ?: run {
                emitter.tryOnError(IllegalStateException("the destination file is null"))
                return@create
            }

            try {
                val byteCount = saveImageUseCase.copyInputStreamToFile(inputUri, destFile)
                emitter.onSuccess(byteCount > 0)
            } catch (e: Throwable) {
                e.printStackTrace()
                emitter.onError(e)
            }
        }
    }

    fun purgeDirectory(dir: File) {
        if (dir.isDirectory && dir.listFiles() != null) {
            for (file in dir.listFiles()) {
                if (file.isDirectory) {
                    purgeDirectory(file)
                } else {
                    file.delete()
                }
            }
        }
    }

    /**
     * Gives the device a user agent. Care should be taken here as the API returns http/https
     * assets based on regex parsing of the build version within this string.
     *
     * @param context
     * @return Device useragent
     */
    fun getUserAgent(context: Context): String {

        var versionName = ""
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = pInfo.versionName
        } catch (e: Exception) {
            Timber.w(e)
        }

        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val phoneName = removeNonASCIICharacters("$manufacturer $model")

        return "audiomack-android/" + versionName + " (" + phoneName + "; " + Build.VERSION.RELEASE + ")"
    }

    fun removeNonASCIICharacters(string: String?): String? {
        return if (string != null && string.isNotEmpty()) {
            string.replace("[^\\x00-\\x7F]".toRegex(), " ")
        } else string
    }

    fun remoteUrlToArtworkFile(context: Context, url: String): File? {
        var result: String? = url
        try {
            result = URLEncoder.encode(url, "UTF-8")
        } catch (e: Exception) {
            Timber.w(e)
        }

        if (result != null && result.length > 255) {
            result = result.substring(result.length - 255)
        }
        val baseFolderPath =
            StorageProvider.getOfflineDirectory(context)?.absolutePath ?: return null
        val outputFolder = File(baseFolderPath + File.separator + "artworks")
        outputFolder.mkdirs()
        return File(outputFolder.absolutePath + File.separator + result)
    }

    fun getCurrentProcessPackageName(context: Context): String {
        return (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.let { activityManager ->
            activityManager.runningAppProcesses?.firstOrNull { it.pid == android.os.Process.myPid() }?.processName ?: ""
        } ?: ""
    }

    fun openAppRating(context: Context) {
        val uri = Uri.parse("https://play.google.com/store/apps/details?id=" + context.packageName)
        var intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.android.vending")
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is ActivityNotFoundException) {
                try {
                    intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                } catch (ee: Exception) {
                    ee.printStackTrace()
                }
            }
        }
    }

    fun formatFullStatNumber(count: Long?): String {
        if (count == null || count <= 0) {
            return "0"
        }
        val formatter = DecimalFormat("###,###,###,###")
        formatter.decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
        return formatter.format(count)
    }

    /**
     * @return a string that is formatted with K/M/B suffixes and at most two decimal numbers, default is "0" for invalid inputs.
     * e.g. 1245 -> 1.24K
     */
    fun formatShortStatNumber(count: Long?): String {
        if (count == null || count <= 0) {
            return "0"
        }
        if (count < 1_000) {
            return count.toString()
        } else {
            val formatter = DecimalFormat()
            formatter.roundingMode = RoundingMode.FLOOR
            return when {
                count < 1_000_000L -> {
                    val result = count.toFloat() / 1_000f
                    formatter.maximumFractionDigits =
                        if (count < 10_000L) 2 else if (count < 100_000L) 1 else 0
                    formatter.minimumFractionDigits = formatter.maximumFractionDigits
                    formatter.format(result.toDouble()) + "K"
                }
                count < 1_000_000_000L -> {
                    val result = count.toFloat() / 1_000_000f
                    formatter.maximumFractionDigits =
                        if (count < 10_000_000L) 2 else if (count < 100_000_000L) 1 else 0
                    formatter.minimumFractionDigits = formatter.maximumFractionDigits
                    formatter.format(result.toDouble()) + "M"
                }
                else -> {
                    val result = count.toFloat() / 1_000_000_000f
                    formatter.maximumFractionDigits =
                        if (count < 10_000_000_000L) 2 else if (count < 100_000_000_000L) 1 else 0
                    formatter.minimumFractionDigits = formatter.maximumFractionDigits
                    formatter.format(result.toDouble()) + "B"
                }
            }
        }
    }

    /**
     * @return a string that is formatted with K/M/B suffixes and no decimals, rounded down.
     * e.g. 1245 -> 1K
     */
    fun formatShortStatNumberWithoutDecimals(count: Long): String {
        return when {
            count < 1_000 -> count.toString()
            count < 1_000_000 -> floor(count.toDouble() / 1_000.toDouble()).toInt().toString() + "K"
            count < 1_000_000_000 -> floor(count.toDouble() / 1_000_000.toDouble()).toInt().toString() + "M"
            else -> floor(count.toDouble() / 1_000_000_000.toDouble()).toInt().toString() + "B"
        }
    }

    /**
     * @param number
     * @param step
     * @return the nearest (wrt to 'number') value that is a multiple of step
     */
    fun roundNumber(number: Int, step: Int): Int {
        return if (step <= 0) {
            number
        } else (number.toFloat() / step.toFloat()).roundToInt() * step
    }
}

/**
 * Handler that runs on the main thread
 *
 * @param handle Invoked in [handleMessage]
 */
class MainHandler(private val handle: (Message?) -> Unit) : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message?) = handle(msg)
}
