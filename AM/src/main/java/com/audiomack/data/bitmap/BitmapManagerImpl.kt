package com.audiomack.data.bitmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.FileProvider
import com.audiomack.BuildConfig
import com.audiomack.R
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.storage.Storage
import com.audiomack.data.storage.StorageProvider
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString
import io.reactivex.Single
import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalStateException
import kotlin.math.roundToInt
import kotlinx.android.synthetic.main.fragment_screenshot.view.*

class BitmapManagerImpl(
    private val imageLoader: ImageLoader = PicassoImageLoader,
    private val storage: Storage = StorageProvider.getInstance()
) : BitmapManager {

    override fun createStickerUri(context: Context?, imageUrl: String?, songTitle: String?, artistName: String?, featArtistName: String?, format: Bitmap.CompressFormat, fileName: String, useFileProvider: Boolean): Single<Uri> =

            imageLoader.load(context, imageUrl).flatMap { bitmap ->

                context?.let {

                    // https://developers.facebook.com/docs/sharing/sharing-to-stories/android-developers
                    val density = context.resources.displayMetrics.density
                    val width = 240 * density.roundToInt()

                    val layout = LayoutInflater.from(context).inflate(R.layout.fragment_screenshot, null).viewInfo

                    layout.tvTitle.text = artistName
                    layout.tvSubtitle.text = songTitle

                    featArtistName
                        ?.takeIf { it.isNotEmpty() }
                        ?.let {
                            val fullString = String.format("%s %s", context.getString(R.string.feat), it)
                            val spannableString = layout.tvSongFeat.context.spannableString(
                                fullString = fullString,
                                highlightedStrings = listOf(it),
                                highlightedColor = layout.tvSongFeat.context.colorCompat(R.color.orange),
                                highlightedFont = R.font.opensans_semibold
                            )
                            layout.tvSongFeat.text = spannableString
                        }

                    layout.ivSong.setImageBitmap(bitmap)

                    layout.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(width * 3, View.MeasureSpec.AT_MOST) // High enough, it will be automatically sized to fit its content
                    )

                    layout.layout(0, 0, layout.measuredWidth, View.MeasureSpec.UNSPECIFIED)

                    val stickerBitmap = Bitmap.createBitmap(layout.measuredWidth, layout.measuredHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(stickerBitmap)

                    layout.draw(canvas)

                    saveBitmapFileToHiddenFolder(context, format, stickerBitmap, fileName, useFileProvider)
                }
            }

    override fun createBackgroundUri(context: Context?, imageUrl: String?, format: Bitmap.CompressFormat, fileName: String, useFileProvider: Boolean): Single<Uri> =
            imageLoader.loadAndBlur(context, imageUrl).flatMap { bitmap ->

                context?.let {

                    val width = context.resources.displayMetrics.widthPixels
                    val height = context.resources.displayMetrics.heightPixels
                    val bgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bgBitmap)
                    val layout = LayoutInflater.from(context).inflate(R.layout.fragment_screenshot, null).viewBlurBg

                    layout.measure(View.MeasureSpec.makeMeasureSpec(canvas.width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(canvas.height, View.MeasureSpec.AT_MOST))
                    layout.layout(0, 0, layout.measuredWidth, layout.measuredHeight)
                    layout.ivBlurBg.setImageBitmap(bitmap)
                    layout.draw(canvas)

                    saveBitmapFileToHiddenFolder(context, format, bgBitmap, fileName, useFileProvider)
                }
            }

    private fun saveBitmapFileToHiddenFolder(context: Context, format: Bitmap.CompressFormat, bitmap: Bitmap, fileName: String, useFileProvider: Boolean): Single<Uri> {
        return Single.create { emitter ->
            val dir = storage.shareDir ?: run {
                emitter.tryOnError(IllegalStateException("Storage volume unavailable"))
                return@create
            }
            try {
                val file = File(dir, fileName)
                val uri = if (useFileProvider) {
                    FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        file
                    )
                } else {
                    Uri.fromFile(file)
                }
                FileOutputStream(file).use { out ->
                    bitmap.compress(format, 100, out)
                    emitter.onSuccess(uri)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                emitter.onError(e)
            }
        }
    }
}
