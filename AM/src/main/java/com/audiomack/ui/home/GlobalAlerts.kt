package com.audiomack.ui.home

import android.net.Uri
import androidx.lifecycle.LiveData
import com.audiomack.utils.SingleLiveEvent

interface AlertTriggers {
    fun onGenericError()

    fun onAddedToQueue()
    fun onLocalFilesSelectionSuccess()
    fun onStoragePermissionDenied()
    fun onAdEvent(title: String)
    fun onPlayUnsupportedFileAttempt(uri: Uri)
}

interface AlertEvents {
    val genericErrorEvent: LiveData<Unit>

    val itemAddedToQueueEvent: LiveData<Unit>
    val localFilesSelectionSuccessEvent: LiveData<Unit>
    val storagePermissionDenied: LiveData<Unit>
    val adEvent: LiveData<String>
    val playUnsupportedFileAttempt: LiveData<Uri>
}

object AlertManager : AlertTriggers, AlertEvents {
    override val genericErrorEvent = SingleLiveEvent<Unit>()
    override val itemAddedToQueueEvent = SingleLiveEvent<Unit>()
    override val localFilesSelectionSuccessEvent = SingleLiveEvent<Unit>()
    override val storagePermissionDenied = SingleLiveEvent<Unit>()
    override val adEvent = SingleLiveEvent<String>()
    override val playUnsupportedFileAttempt = SingleLiveEvent<Uri>()

    override fun onGenericError() = genericErrorEvent.call()
    override fun onAddedToQueue() = itemAddedToQueueEvent.call()
    override fun onLocalFilesSelectionSuccess() = localFilesSelectionSuccessEvent.call()
    override fun onStoragePermissionDenied() = storagePermissionDenied.call()
    override fun onAdEvent(title: String) { adEvent.value = title }
    override fun onPlayUnsupportedFileAttempt(uri: Uri) { playUnsupportedFileAttempt.postValue(uri) }
}
