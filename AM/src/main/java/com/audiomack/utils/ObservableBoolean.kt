package com.audiomack.utils

import io.reactivex.subjects.BehaviorSubject

class ObservableBoolean(defValue: Boolean? = null) {

    val observable = defValue?.let { BehaviorSubject.createDefault(it) } ?: BehaviorSubject.create()

    var value: Boolean
        get() = observable.value ?: false
        set(value) {
            observable.onNext(value)
        }
}
