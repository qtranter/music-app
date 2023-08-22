package com.audiomack.data.bitmap

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import io.reactivex.Single

interface BitmapManager {

    fun createStickerUri(context: Context?, imageUrl: String?, songTitle: String?, artistName: String?, featArtistName: String?, format: Bitmap.CompressFormat, fileName: String, useFileProvider: Boolean): Single<Uri>

    fun createBackgroundUri(context: Context?, imageUrl: String?, format: Bitmap.CompressFormat, fileName: String, useFileProvider: Boolean): Single<Uri>
}
