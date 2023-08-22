package com.audiomack.utils

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C.ContentType
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource.Factory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSinkFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheKeyFactory
import com.google.android.exoplayer2.util.Util

object ExoPlayerUtils {

    const val USER_AGENT_NAME = "Audiomack"

    fun getBaseFactory(context: Context): DataSource.Factory {
        val userAgent = Util.getUserAgent(context, USER_AGENT_NAME)
        return DefaultDataSourceFactory(context, userAgent)
    }

    fun getCacheDataSourceFactory(
        cache: Cache,
        upstreamFactory: DataSource.Factory,
        cacheKeyFactory: CacheKeyFactory
    ): CacheDataSourceFactory {
        return CacheDataSourceFactory(
            cache,
            upstreamFactory,
            Factory(),
            CacheDataSinkFactory(cache, CacheDataSink.DEFAULT_FRAGMENT_SIZE),
            0,
            null,
            cacheKeyFactory
        )
    }

    @ContentType
    fun Uri.inferContentType(): Int {
        val fileName = lastPathSegment ?: throw IllegalStateException("Invalid uri")
        return Util.inferContentType(fileName)
    }
}
