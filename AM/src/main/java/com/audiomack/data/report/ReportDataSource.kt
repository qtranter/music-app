package com.audiomack.data.report

import com.audiomack.model.ReportContentReason
import com.audiomack.model.ReportContentType
import com.audiomack.model.ReportType
import io.reactivex.Completable

interface ReportDataSource {
    fun reportBlock(reportType: ReportType, contentId: String, contentType: ReportContentType, reason: ReportContentReason): Completable
}
