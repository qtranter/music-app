package com.audiomack.utils

import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber

/**
 * Convenience class for [Observer] that allows you to only override [onNext]
 *
 * @property compositeDisposable If provided the [Disposable] returned from [onSubscribe] is added.
 */
abstract class SimpleObserver<T>(
    private val compositeDisposable: CompositeDisposable? = null
) : Observer<T> {

    override fun onSubscribe(d: Disposable) {
        compositeDisposable?.add(d)
    }

    override fun onComplete() {
        // For rent
    }

    override fun onError(e: Throwable) {
        Timber.tag(javaClass.simpleName).w(e, "Uncaught exception for ${javaClass.simpleName}")
        // For rent
    }
}
