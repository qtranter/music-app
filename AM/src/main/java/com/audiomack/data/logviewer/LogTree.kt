package com.audiomack.data.logviewer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

class LogTree(
    private val type: LogType,
    private val logDataSource: LogDataSource
) : Timber.Tree() {

    private val dateFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (type.tag == tag) {
            logDataSource.addLog(type, dateFormatter.format(Date()) + " - " + message)
        }
    }
}
