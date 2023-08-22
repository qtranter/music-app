package com.audiomack.data.report

import com.audiomack.model.ReportContentReason
import com.audiomack.model.ReportContentType
import com.audiomack.model.ReportType
import com.audiomack.network.API
import io.reactivex.Completable

class ReportRepository : ReportDataSource {
    override fun reportBlock(reportType: ReportType, contentId: String, contentType: ReportContentType, reason: ReportContentReason): Completable {
        return API.getInstance().reportBlock(reportType, contentId, contentType, reason)
    }
}
