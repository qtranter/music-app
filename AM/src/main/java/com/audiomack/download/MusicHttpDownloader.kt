package com.audiomack.download

import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okio.buffer
import okio.sink

interface MusicHttpDownloader {
    suspend fun download(url: String, destination: File): MusicHttpDownloadResult
}

class AMMusicHttpDownloader : MusicHttpDownloader {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .addInterceptor(HttpLoggingInterceptor())
        .build()

    override suspend fun download(url: String, destination: File): MusicHttpDownloadResult {

        try {

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.code == 200) {
                response.body?.let {
                    val sink = destination.sink().buffer()
                    sink.writeAll(it.source())
                    sink.close()
                    response.close()
                    return MusicHttpDownloadResult(true)
                }
            }
            response.close()

            return MusicHttpDownloadResult(false, Exception("${response.code} ${response.message}"))
        } catch (e: Exception) {
            return MusicHttpDownloadResult(false, e)
        }
    }
}

data class MusicHttpDownloadResult(
    val success: Boolean,
    val exception: Exception? = null
)
