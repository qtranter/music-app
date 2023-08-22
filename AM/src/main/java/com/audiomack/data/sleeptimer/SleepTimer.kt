package com.audiomack.data.sleeptimer

import com.audiomack.utils.Second
import io.reactivex.Observable
import java.util.Date

interface SleepTimer {
    val sleepEvent: Observable<SleepTimerEvent>

    fun set(seconds: Second)
    fun clear()
}

sealed class SleepTimerEvent {
    data class TimerSet(val date: Date) : SleepTimerEvent()
    object TimerTriggered : SleepTimerEvent()
    object TimerCleared : SleepTimerEvent()
}
