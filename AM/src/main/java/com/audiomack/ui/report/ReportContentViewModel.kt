package com.audiomack.ui.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.report.ReportDataSource
import com.audiomack.data.report.ReportRepository
import com.audiomack.model.EventContentReported
import com.audiomack.model.ProgressHUDMode
import com.audiomack.model.ReportContentReason
import com.audiomack.model.ReportContentType
import com.audiomack.model.ReportType
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import org.greenrobot.eventbus.EventBus

class ReportContentViewModel(
    private val reportDataSource: ReportDataSource = ReportRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val eventBus: EventBus = EventBus.getDefault()
) : BaseViewModel() {

    val closeEvent = SingleLiveEvent<Void>()
    var showConfirmationEvent = SingleLiveEvent<Void>()
    val showHUDEvent = SingleLiveEvent<ProgressHUDMode>()

    private var _reportReason = MutableLiveData<ReportContentReason>()
    val reportReason: LiveData<ReportContentReason> get() = _reportReason

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onSubmitTapped() {
        showConfirmationEvent.call()
    }

    fun onReasonSelected(reportReason: ReportContentReason) {
        _reportReason.postValue(reportReason)
    }

    fun onSendReport(reportType: ReportType, contentId: String, contentType: ReportContentType, reportReason: ReportContentReason) {
        showHUDEvent.postValue(ProgressHUDMode.Loading)
        compositeDisposable.add(
            reportDataSource.reportBlock(reportType, contentId, contentType, reportReason)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    closeEvent.call()
                    eventBus.post(EventContentReported(reportType))
                }, {
                    showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    showHUDEvent.postValue(ProgressHUDMode.Failure(""))
                    closeEvent.call()
                })
        )
    }
}
