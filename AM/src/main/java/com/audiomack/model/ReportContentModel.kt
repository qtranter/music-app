package com.audiomack.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ReportContentModel(
    var contentId: String,
    var contentType: ReportContentType,
    var reportType: ReportType,
    var reportReason: ReportContentReason?
) : Parcelable
