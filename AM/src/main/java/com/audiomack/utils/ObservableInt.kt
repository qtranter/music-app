package com.audiomack.utils

import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

private const val TAG = "ObservableInt"

class ObservableInt {

    var value: Int
        get() = observable.value ?: Int.MIN_VALUE
        set(value) {
            Timber.tag(TAG).d("Value set to $value")
            if (value > Int.MIN_VALUE) observable.onNext(value)
        }

    val observable = BehaviorSubject.create<Int>()

    fun clear() {
        value = -1
    }
}
