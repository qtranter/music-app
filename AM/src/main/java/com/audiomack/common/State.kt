package com.audiomack.common

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

interface StateProvider<T : Any> {
    val value: T
    val observable: Observable<T>
}

interface StateEditor<T : Any> : StateProvider<T> {
    override var value: T
}

open class StateManager<T : Any>(private val defaultValue: T) : StateEditor<T> {

    private val subject = BehaviorSubject.createDefault(defaultValue)

    override val observable: Observable<T>
        get() = subject

    override var value: T
        get() = subject.value ?: defaultValue
        set(value) = subject.onNext(value)
}

class State<T : Any>(initialValue: T) : StateManager<T>(initialValue)
