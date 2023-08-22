package com.audiomack.ui.common

/**
 * A generic class that holds a value with its loading status.
 *
 * @param <T>
</T> */
sealed class Resource<out T>(val data: T?, val error: Throwable? = null) {
    class Success<T>(data: T?) : Resource<T>(data)
    class Failure<T>(error: Throwable, data: T? = null) : Resource<T>(data, error)
    class Loading<T>(data: T? = null) : Resource<T>(data)

    override fun toString(): String {
        return "${javaClass.simpleName}(data=$data, error=$error)"
    }
}

@Suppress("unused")
class ResourceException : Exception {
    constructor(message: String?) : super(message)
    constructor(throwable: Throwable) : super(throwable)
    constructor(message: String?, throwable: Throwable) : super(message, throwable)
}
