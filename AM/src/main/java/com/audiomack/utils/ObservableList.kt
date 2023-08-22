package com.audiomack.utils

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class ObservableList<T> {

    private val list: MutableList<T> = ArrayList()
    val value: List<T> get() = _observable.value ?: list

    private val _observable: BehaviorSubject<List<T>> = BehaviorSubject.createDefault(list)
    val observable: Observable<List<T>> get() = _observable

    val size get() = list.size

    fun get(index: Int): T = list[index]

    fun isEmpty(): Boolean = list.isEmpty()

    fun set(elements: List<T>) {
        list.clear()
        list.addAll(elements)
        _observable.onNext(list)
    }

    fun add(element: T) {
        list.add(element)
        _observable.onNext(list)
    }

    fun add(index: Int, element: T) {
        list.add(index, element)
        _observable.onNext(list)
    }

    fun addAll(elements: List<T>) {
        list.addAll(elements)
        _observable.onNext(list)
    }

    fun addAll(index: Int, elements: List<T>) {
        list.addAll(index, elements)
        _observable.onNext(list)
    }

    fun remove(element: T) {
        list.remove(element)
        _observable.onNext(list)
    }

    fun removeAt(index: Int) {
        list.removeAt(index)
        _observable.onNext(list)
    }

    /**
     * Removes the portion of this list between the specified fromIndex (inclusive) and toIndex (exclusive).
     */
    fun removeRange(fromIndex: Int, toIndex: Int) {
        list.subList(fromIndex, toIndex).clear()
        _observable.onNext(list)
    }

    fun move(from: Int, to: Int) {
        list.move(from, to)
        _observable.onNext(list)
    }

    fun clear() {
        list.clear()
        _observable.onNext(list)
    }

    fun keepOnly(index: Int) {
        val item = list[index]
        list.run {
            clear()
            add(item)
        }
        _observable.onNext(list)
    }
}
