package com.audiomack.data.logviewer

object LogRepository : LogDataSource {

    private val data: Map<LogType, MutableList<String>> = LogType.values().map { it to ArrayList<String>() }.toMap()

    override fun provideData(type: LogType): List<String> {
        return data[type]?.toList() ?: emptyList()
    }

    override fun addLog(type: LogType, log: String) {
        data[type]?.add(0, log)
    }
}
