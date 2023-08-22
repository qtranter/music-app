package com.audiomack.ui.removedcontent

import com.audiomack.data.removedcontent.RemovedContentDataSource
import com.audiomack.data.removedcontent.RemovedContentRepository
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.network.AnalyticsKeys
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent

class RemovedContentViewModel(
    private val trackingRepository: TrackingDataSource = TrackingRepository(),
    private val removedContentRepository: RemovedContentDataSource = RemovedContentRepository
) : BaseViewModel() {

    val close = SingleLiveEvent<Void>()
    val ok = SingleLiveEvent<Void>()

    fun onCloseTapped() {
        close.call()
    }

    fun onOkTapped() {
        ok.call()
    }

    fun trackScreen() {
        trackingRepository.trackScreen(AnalyticsKeys.EVENT_REMOVED_CONTENT)
    }

    var items = removedContentRepository.getItems()

    fun clearItems() {
        removedContentRepository.clearItems()
    }
}
