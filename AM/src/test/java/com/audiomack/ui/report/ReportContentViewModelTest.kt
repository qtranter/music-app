package com.audiomack.ui.report

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.report.ReportDataSource
import com.audiomack.model.EventContentReported
import com.audiomack.model.ReportContentReason
import com.audiomack.model.ReportContentType
import com.audiomack.model.ReportType
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Completable
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class ReportContentViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var reportDataSource: ReportDataSource

    @Mock
    private lateinit var schedulersProvider: SchedulersProvider

    @Mock
    private lateinit var eventBus: EventBus

    private lateinit var viewModel: ReportContentViewModel

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        viewModel = ReportContentViewModel(
            reportDataSource,
            schedulersProvider,
            eventBus
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observer: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `report user spam reason`() {
        val contentType = ReportContentType.Artist
        val reportType = ReportType.Report
        val reportReason = ReportContentReason.Spam
        val contentId = "123"
        Mockito.`when`(reportDataSource.reportBlock(reportType, contentId, contentType, reportReason)).thenReturn(Completable.complete())
        val observerShowConfirmation: Observer<Void> = mock()
        val observerReportReason: Observer<ReportContentReason> = mock()
        viewModel.showConfirmationEvent.observeForever(observerShowConfirmation)
        viewModel.reportReason.observeForever(observerReportReason)
        viewModel.onReasonSelected(reportReason)
        verify(observerReportReason).onChanged(eq(reportReason))
        viewModel.onSubmitTapped()
        verify(observerShowConfirmation).onChanged(null)
        viewModel.onSendReport(reportType, contentId, contentType, reportReason)
        verify(eventBus).post(argWhere { it is EventContentReported && it.reportType == reportType })
    }

    @Test
    fun `block user spam reason`() {
        val contentType = ReportContentType.Artist
        val reportType = ReportType.Block
        val reportReason = ReportContentReason.Spam
        val contentId = "123"
        Mockito.`when`(reportDataSource.reportBlock(reportType, contentId, contentType, reportReason)).thenReturn(Completable.complete())
        val observerShowConfirmation: Observer<Void> = mock()
        val observerReportReason: Observer<ReportContentReason> = mock()
        viewModel.showConfirmationEvent.observeForever(observerShowConfirmation)
        viewModel.reportReason.observeForever(observerReportReason)
        viewModel.onReasonSelected(reportReason)
        verify(observerReportReason).onChanged(eq(reportReason))
        viewModel.onSubmitTapped()
        verify(observerShowConfirmation).onChanged(null)
        viewModel.onSendReport(reportType, contentId, contentType, reportReason)
        verify(eventBus).post(argWhere { it is EventContentReported && it.reportType == reportType })
    }
}
