package com.audiomack.ui.screenshot

import android.net.Uri
import android.os.Parcelable
import com.audiomack.model.ShareMethod
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ShareStoryModel(
    var contentUrl: String,
    var shareMethod: ShareMethod,
    var stickerUri: Uri,
    var backgroundUri: Uri
) : Parcelable
