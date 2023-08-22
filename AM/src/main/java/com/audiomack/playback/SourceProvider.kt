package com.audiomack.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.audiomack.MainApplication
import com.audiomack.data.cache.CachingLayerImpl
import com.audiomack.data.cache.UriKeyCacheFactory
import com.audiomack.utils.ExoPlayerUtils
import com.audiomack.utils.ExoPlayerUtils.inferContentType
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.DataSource.Factory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheKeyFactory

class SourceProvider private constructor(
    private val cache: Cache,
    private val cacheKeyFactory: CacheKeyFactory,
    override val baseDataSourceFactory: Factory
) : Sources {

    override fun buildMediaSource(uri: Uri): MediaSource {
        val cacheFactory = ExoPlayerUtils.getCacheDataSourceFactory(
            cache,
            baseDataSourceFactory,
            cacheKeyFactory
        )
        return when (val type = uri.inferContentType()) {
            C.TYPE_SS -> SsMediaSource.Factory(cacheFactory).createMediaSource(uri)
            C.TYPE_DASH -> DashMediaSource.Factory(cacheFactory).createMediaSource(uri)
            C.TYPE_HLS -> HlsMediaSource.Factory(cacheFactory).createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(cacheFactory).createMediaSource(uri)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    companion object {
        private const val USER_AGENT_NAME = "Audiomack"

        @Volatile
        private var instance: SourceProvider? = null

        fun init(
            context: Context,
            cacheKeyFactory: CacheKeyFactory = UriKeyCacheFactory(),
            baseDataSourceFactory: Factory = ExoPlayerUtils.getBaseFactory(context)
        ): SourceProvider = instance ?: synchronized(this) {
            instance ?: SourceProvider(
                CachingLayerImpl.init(context).cache,
                cacheKeyFactory,
                baseDataSourceFactory
            ).also { instance = it }
        }

        fun getInstance(): SourceProvider =
            instance ?: MainApplication.context?.let { init(it) }
            ?: throw IllegalStateException("SourceProvider was not initialized")

        @VisibleForTesting
        fun getInstance(
            cache: Cache = CachingLayerImpl.getInstance().cache,
            cacheKeyFactory: CacheKeyFactory = UriKeyCacheFactory(),
            baseDataSourceFactory: Factory = DefaultHttpDataSourceFactory(USER_AGENT_NAME)
        ): SourceProvider = instance ?: synchronized(this) {
            instance ?: SourceProvider(
                cache,
                cacheKeyFactory,
                baseDataSourceFactory
            ).also { instance = it }
        }

        @VisibleForTesting
        fun destroy() {
            instance = null
        }
    }
}
