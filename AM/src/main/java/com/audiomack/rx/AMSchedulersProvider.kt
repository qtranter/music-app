package com.audiomack.rx

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AMSchedulersProvider : SchedulersProvider {

    override val computation: Scheduler
        get() = Schedulers.computation()

    override val io: Scheduler
        get() = Schedulers.io()

    override val main: Scheduler
        get() = AndroidSchedulers.mainThread()

    override val newThread: Scheduler
        get() = Schedulers.newThread()

    override val single: Scheduler
        get() = Schedulers.single()

    override val trampoline: Scheduler
        get() = Schedulers.trampoline()

    override val interval: Scheduler
        get() = computation
}
