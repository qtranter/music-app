package com.audiomack.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DownloadServiceCommand(
    val commandType: DownloadServiceCommandType,
    val ids: List<String> = listOf()
) : Parcelable

enum class DownloadServiceCommandType {
    Download, UpdateNotification, Stop
}
