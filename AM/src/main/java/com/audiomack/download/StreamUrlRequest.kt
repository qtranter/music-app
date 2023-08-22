package com.audiomack.download

import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import io.reactivex.Observable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.delay
import timber.log.Timber

sealed class StreamUrlResult {
    class Success(val streamUrl: String) : StreamUrlResult()

    class Failure(val exception: Exception, val streamUrl: String? = null) : StreamUrlResult()
}

suspend fun <T> retryIO(
    times: Int = 4,
    initialDelay: Long = 1000, // 1 second
    maxDelay: Long = 10000, // 10 seconds
    factor: Double = 2.0,
    block: suspend () -> T,
    isSuccess: (ioReturn: T) -> Boolean
): T {

    var currentDelay = initialDelay

    repeat(times - 1) {
        val ioReturn = block()
        if (isSuccess(ioReturn)) {
            return ioReturn
        } else {
            Timber.w("Failure in retryIO. Retrying...")
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }

    return block() // last attempt
}

suspend fun AMResultItem.refreshUrl(parentId: String?): StreamUrlResult = suspendCoroutine { c ->

    val middleware = object : API.GetStreamURLListener {

        override fun onSuccess(streamUrl: String?) {
            // Error cases
            if (streamUrl.isNullOrBlank()) {
                c.resume(StreamUrlResult.Failure(Exception("Null stream url in success")))
                return
            } else if (!streamUrl.startsWith("http")) {
                c.resume(StreamUrlResult.Failure(Exception("Invalid stream url returned in success"), streamUrl))
                return
            }
            c.resume(StreamUrlResult.Success(streamUrl))
        }

        override fun onFailure(exception: Exception) {
            c.resume(StreamUrlResult.Failure(exception))
        }
    }

    val getStreamUrl: Observable<String> = if (isAlbumTrack && !parentId.isNullOrEmpty()) {
        API.getInstance().getStreamURLForAlbumWithSession(parentId, itemId, true, mixpanelSource?.page ?: MixpanelSource.empty.page, extraKey)
    } else if (isPlaylistTrack && !parentId.isNullOrEmpty()) {
        API.getInstance().getStreamURLForPlaylistWithSession(parentId, itemId, true, mixpanelSource?.page ?: MixpanelSource.empty.page, extraKey)
    } else {
        API.getInstance().getStreamURLWithSession(itemId, true, mixpanelSource?.page ?: MixpanelSource.empty.page, extraKey)
    }

    getStreamUrl.subscribe({ streamUrl ->
        middleware.onSuccess(streamUrl)
    }, { throwable ->
        middleware.onFailure(Exception(throwable))
    })
}
