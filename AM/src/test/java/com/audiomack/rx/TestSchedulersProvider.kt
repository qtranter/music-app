package com.audiomack.rx

import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler

class TestSchedulersProvider : SchedulersProvider {

    override val computation: Scheduler
        get() = Schedulers.trampoline()

    override val io: Scheduler
        get() = Schedulers.trampoline()

    override val main: Scheduler
        get() = Schedulers.trampoline()

    override val newThread: Scheduler
        get() = Schedulers.trampoline()

    override val single: Scheduler
        get() = Schedulers.trampoline()

    override val trampoline: Scheduler
        get() = Schedulers.trampoline()

    override val interval: TestScheduler = TestScheduler()
}
