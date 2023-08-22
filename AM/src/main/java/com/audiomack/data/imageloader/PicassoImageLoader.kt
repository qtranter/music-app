package com.audiomack.data.imageloader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.audiomack.utils.Utils
import com.audiomack.utils.isMediaStoreUri
import com.audiomack.utils.toUri
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.steelkiwi.cropiwa.CropIwaView
import io.reactivex.Single
import java.lang.Error
import java.lang.Exception
import jp.wasabeef.picasso.transformations.BlurTransformation

object PicassoImageLoader : ImageLoader {

    val targets: MutableList<Target> = mutableListOf()
    @SuppressLint("StaticFieldLeak")
    private var picassoSingleton: Picasso? = null

    override fun load(
        context: Context?,
        path: String?,
        imageView: ImageView,
        errorResId: Int?
    ) {
        if (context == null) {
            return
        }

        val picasso = getPicasso()

        picasso.cancelRequest(imageView)
        imageView.setImageDrawable(null)

        if (path.isNullOrBlank() && errorResId == null) {
            return
        }

        val file = path?.let { Utils.remoteUrlToArtworkFile(context, it) }
        if (file != null && file.exists() && file.length() > 512) {
            val target = object : Target {
                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    imageView.setImageBitmap(bitmap)
                    targets.remove(this)
                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    targets.remove(this)
                    picasso.load(path).into(imageView)
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                }
            }
            targets.add(target)
            picasso.load(file).apply {
                errorResId?.let { error(it) }
            }.into(target)
        } else {
            val isMediaStoreUri = path.toUri().isMediaStoreUri()

            picasso.load(path).apply {
                errorResId?.let { fallbackResId ->
                    error(fallbackResId)
                    if (isMediaStoreUri) placeholder(fallbackResId)
                }
            }.into(imageView)
        }
    }

    override fun load(context: Context?, path: String?, cropIwaView: CropIwaView) {
        if (context == null || path.isNullOrBlank()) {
            return
        }

        val picasso = getPicasso()
        val target = object : Target {
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                targets.remove(this)
            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                cropIwaView.setImage(bitmap)
                targets.remove(this)
            }
        }
        targets.add(target)

        val config = Bitmap.Config.RGB_565
        val file = Utils.remoteUrlToArtworkFile(context, path)
        if (file != null && file.exists() && file.length() > 0) {
            picasso.load(file).config(config).into(target)
        } else {
            picasso.load(path).config(config).into(target)
        }
    }

    override fun load(
        context: Context?,
        path: String?,
        errorResId: Int?,
        config: Bitmap.Config,
        callback: ImageLoaderCallback?
    ) {
        if (context == null || path.isNullOrBlank()) {
            return
        }

        val picasso = getPicasso()
        val target = object : Target {
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                callback?.onBitmapFailed(errorDrawable)
                targets.remove(this)
            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                callback?.onBitmapLoaded(bitmap)
                targets.remove(this)
            }
        }
        targets.add(target)

        val file = Utils.remoteUrlToArtworkFile(context, path)
        if (file != null && file.exists() && file.length() > 0) {
            picasso.load(file).config(config).apply {
                errorResId?.let { fallbackResId ->
                    error(fallbackResId)
                }
            }.into(target)
        } else {
            val isMediaStoreUri = path.toUri().isMediaStoreUri()

            picasso.load(path).config(config).apply {
                errorResId?.let { fallbackResId ->
                    error(fallbackResId)
                    if (isMediaStoreUri) placeholder(fallbackResId)
                }
            }.into(target)
        }
    }

    override fun load(context: Context?, path: String?): Single<Bitmap> {

        return Single.create { emitter ->
            try {
                if (context != null) {
                    if (!path.isNullOrBlank()) {
                        val picasso = getPicasso()
                        val target = object : Target {
                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                            }

                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                if (e != null) {
                                    emitter.onError(e)
                                } else {
                                    emitter.onError(Error("Exception is null"))
                                }
                                targets.remove(this)
                            }

                            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                if (bitmap != null) {
                                    emitter.onSuccess(bitmap)
                                } else {
                                    emitter.onError(Error("Bitmap is null"))
                                }
                                targets.remove(this)
                            }
                        }
                        targets.add(target)

                        val config = Bitmap.Config.RGB_565
                        val file = Utils.remoteUrlToArtworkFile(context, path)
                        if (file != null && file.exists() && file.length() > 0) {
                            picasso.load(file).config(config).into(target)
                        } else {
                            picasso.load(path).config(config).into(target)
                        }
                    } else {
                        emitter.onError(Error("Path is null or empty"))
                    }
                } else {
                    emitter.onError(Error("Context is null"))
                }
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    override fun loadAndBlur(context: Context?, imageUrl: String?): Single<Bitmap> {
        return Single.create { emitter ->
            try {
                if (context != null) {
                    if (!imageUrl.isNullOrBlank()) {
                        val picasso = getPicasso()
                        val target = object : Target {
                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                            }

                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                if (e != null) {
                                    emitter.onError(e)
                                } else {
                                    emitter.onError(Error("Exception is null"))
                                }
                                targets.remove(this)
                            }

                            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                if (bitmap != null) {
                                    emitter.onSuccess(bitmap)
                                } else {
                                    emitter.onError(Error("Bitmap is null"))
                                }
                                targets.remove(this)
                            }
                        }
                        targets.add(target)

                        val config = Bitmap.Config.RGB_565
                        val file = Utils.remoteUrlToArtworkFile(context, imageUrl)
                        if (file != null && file.exists() && file.length() > 0) {
                            picasso.load(file).transform(BlurTransformation(context, 45, 2)).config(config).into(target)
                        } else {
                            picasso.load(imageUrl).transform(BlurTransformation(context, 45, 2)).config(config).into(target)
                        }
                    } else {
                        emitter.onError(Error("Path is null or empty"))
                    }
                } else {
                    emitter.onError(Error("Context is null"))
                }
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    private fun getPicasso(): Picasso {
        return picassoSingleton ?: run {
            val picasso = Picasso.get()
            picassoSingleton = picasso
            picasso
        }
    }
}
