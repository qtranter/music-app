package com.audiomack.utils

class AMCrashHandler(private val defaultHandler: Thread.UncaughtExceptionHandler) : Thread.UncaughtExceptionHandler {

    private val blacklist: List<String> = listOf("Results have already been set", "Package manager has died", "UiAutomation not connected!")

    override fun uncaughtException(thread: Thread, throwable: Throwable?) {

        if (throwable?.stackTrace?.isNotEmpty() == true && blacklist.contains(throwable.message ?: "")) {
            // Let's ignore this crash
            // https://issuetracker.google.com/issues/70416429
        } else defaultHandler.uncaughtException(thread, throwable)
    }
}
