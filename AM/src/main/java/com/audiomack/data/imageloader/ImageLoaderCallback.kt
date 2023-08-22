package com.audiomack.data.imageloader

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

interface ImageLoaderCallback {

    fun onBitmapLoaded(bitmap: Bitmap?)

    fun onBitmapFailed(errorDrawable: Drawable?)
}
