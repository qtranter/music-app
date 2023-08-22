package com.audiomack.rx

import io.reactivex.Scheduler

interface SchedulersProvider {

    val computation: Scheduler
    val io: Scheduler
    val main: Scheduler
    val newThread: Scheduler
    val single: Scheduler
    val trampoline: Scheduler
    val interval: Scheduler
}
