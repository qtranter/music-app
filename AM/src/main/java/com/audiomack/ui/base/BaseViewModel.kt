package com.audiomack.ui.base

import androidx.lifecycle.ViewModel
import com.audiomack.utils.addTo
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BaseViewModel : ViewModel() {
    val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }

    protected fun Disposable.composite() {
        this.addTo(compositeDisposable)
    }
}
