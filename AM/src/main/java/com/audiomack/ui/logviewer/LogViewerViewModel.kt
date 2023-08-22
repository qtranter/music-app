package com.audiomack.ui.logviewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.logviewer.LogDataSource
import com.audiomack.data.logviewer.LogRepository
import com.audiomack.data.logviewer.LogType
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent

class LogViewerViewModel(
    private val logDataSource: LogDataSource = LogRepository
) : BaseViewModel() {

    private var type: LogType? = null

    val closeEvent = SingleLiveEvent<Void>()
    val shareEvent = SingleLiveEvent<String>()

    private val _logs = MutableLiveData<List<String>>()
    val logs: LiveData<List<String>> get() = _logs

    fun onBackTapped() {
        closeEvent.call()
    }

    fun onRefreshTriggered() {
        reloadData()
    }

    fun onShareTapped() {
        val text = LogType.values().map {
            "### ${it.name}:\n${logDataSource.provideData(it).joinToString("\n")}\n\n"
        }.joinToString("\n")
        shareEvent.postValue(text)
    }

    private fun reloadData() {
        type?.let {
            _logs.postValue(logDataSource.provideData(it))
        }
    }

    fun onTypeChanged(type: LogType) {
        this.type = type
        reloadData()
    }
}
