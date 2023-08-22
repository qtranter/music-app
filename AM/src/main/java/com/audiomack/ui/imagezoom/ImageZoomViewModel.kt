package com.audiomack.ui.imagezoom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.ImageLoaderCallback
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.home.NavigationActions
import com.audiomack.ui.home.NavigationManager

class ImageZoomViewModel(
    private val imageLoader: ImageLoader = PicassoImageLoader,
    private val navigationActions: NavigationActions = NavigationManager.getInstance()
) : BaseViewModel() {

    private val _toggleProgressBar = MutableLiveData<Boolean>()
    val toggleProgressBar: LiveData<Boolean> get() = _toggleProgressBar

    private val _toggleImageView = MutableLiveData<Boolean>()
    val toggleImageView: LiveData<Boolean> get() = _toggleImageView

    fun onCloseTapped() {
        navigationActions.navigateBack()
    }

    fun onImageViewFling(velocityX: Float, velocityY: Float) {
        if (velocityY > velocityX * 4) {
            navigationActions.navigateBack()
        }
    }

    fun loadImage(context: Context, url: String?, imageView: ImageView) {
        if (url.isNullOrBlank()) {
            navigationActions.navigateBack()
            return
        }

        _toggleProgressBar.postValue(true)
        _toggleImageView.postValue(true)

        imageLoader.load(context, url, callback = object : ImageLoaderCallback {
            override fun onBitmapLoaded(bitmap: Bitmap?) {
                _toggleProgressBar.postValue(false)
                bitmap?.let { imageView.setImageBitmap(it) }
            }

            override fun onBitmapFailed(errorDrawable: Drawable?) {
                navigationActions.navigateBack()
            }
        })
    }
}
