package com.audiomack.data.cache

import android.net.Uri
import com.audiomack.model.AMResultItem
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheKeyFactory
import timber.log.Timber

interface CachingLayer {

    val cache: Cache

    /** Returns true if the entire content length of the remote resource is cached */
    fun isCached(uri: Uri): Boolean

    /** Initiates a download of the full remote resource into the cache */
    fun add(uri: Uri)

    /** Deletes cached resource for the given [Uri] if it exists */
    fun remove(uri: Uri)

    /** Deletes all cached resources */
    fun clear()
}

fun CachingLayer.remove(item: AMResultItem): Boolean {
    val uri = Uri.parse(item.url)
    if (isCached(item)) {
        remove(uri)
        return true
    }
    return false
}

fun CachingLayer.isCached(item: AMResultItem): Boolean = isCached(Uri.parse(item.url))

class UriKeyCacheFactory : CacheKeyFactory {
    override fun buildCacheKey(dataSpec: DataSpec?): String? = dataSpec?.uri?.toCacheKey()
}

fun Uri.toCacheKey(): String? {
    var url = this.toString()
    try {
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"))
        }
        url = url.replace("http:", "https:").replace("/", "").replace(":", "").replace("?", "")
            .replace("*", "").replace("<", "").replace(">", "").replace("|", "").replace("\\", "")
    } catch (e: Exception) {
        Timber.w(e)
    }

    return url
}
