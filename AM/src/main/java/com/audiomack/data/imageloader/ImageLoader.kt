package com.audiomack.data.imageloader

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.steelkiwi.cropiwa.CropIwaView
import io.reactivex.Single

interface ImageLoader {

    fun load(context: Context?, path: String?, imageView: ImageView, errorResId: Int? = null)

    fun load(context: Context?, path: String?, cropIwaView: CropIwaView)

    fun load(
        context: Context?,
        path: String?,
        errorResId: Int? = null,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888,
        callback: ImageLoaderCallback? = null
    )

    fun load(context: Context?, path: String?): Single<Bitmap>

    fun loadAndBlur(context: Context?, imageUrl: String?): Single<Bitmap>
}
