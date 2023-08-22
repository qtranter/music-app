package com.audiomack.data.cache

import android.content.Context
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.util.component1
import androidx.core.util.component2
import com.audiomack.MainApplication
import com.audiomack.data.storage.Storage
import com.audiomack.data.storage.StorageProvider
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.utils.ExoPlayerUtils
import com.audiomack.utils.ExoPlayerUtils.USER_AGENT_NAME
import com.audiomack.utils.ExoPlayerUtils.inferContentType
import com.audiomack.utils.addTo
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper
import com.google.android.exoplayer2.offline.ProgressiveDownloader
import com.google.android.exoplayer2.upstream.DataSource.Factory
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheEvictor
import com.google.android.exoplayer2.upstream.cache.CacheKeyFactory
import com.google.android.exoplayer2.upstream.cache.CacheUtil
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

class CachingLayerImpl private constructor(
    storage: Storage,
    private val cacheKeyFactory: CacheKeyFactory,
    private val cacheEvictor: CacheEvictor,
    private val databaseProvider: DatabaseProvider?,
    private val upstreamFactory: Factory,
    private val schedulers: SchedulersProvider
) : CachingLayer {

    private val disposables: CompositeDisposable by lazy { CompositeDisposable() }

    override val cache: SimpleCache by lazy {
        SimpleCache(storage.cacheDir, cacheEvictor, databaseProvider)
    }

    override fun add(uri: Uri) {
        val url = uri.buildUpon().clearQuery().build().toString()
        download(uri)
            .subscribeOn(schedulers.io)
            .map { it.toInt() }
            .filter { it.rem(10) == 0 }
            .distinctUntilChanged()
            .subscribe({
                Timber.tag(TAG).d("Cached %d%% of %s", it, url)
            }, { e ->
                Timber.tag(TAG).e(e, "Unable to add Uri to cache: $url")
            })
            .addTo(disposables)
    }

    private fun download(uri: Uri): Flowable<Float> = Flowable.create({ emitter ->
        val type = uri.inferContentType()
        if (type != C.TYPE_OTHER) {
            emitter.onError(Error("Adaptive pre-caching is not currently supported"))
            return@create
        }

        val cacheFactory =
            ExoPlayerUtils.getCacheDataSourceFactory(cache, upstreamFactory, cacheKeyFactory)
        val downloaderHelper = DownloaderConstructorHelper(cache, cacheFactory)
        val downloader = ProgressiveDownloader(uri, uri.toCacheKey(), downloaderHelper)

        emitter.setCancellable { downloader.cancel() }

        downloader.download { _, _, percentDownloaded ->
            emitter.onNext(percentDownloaded)
            if (percentDownloaded == 100.0f) {
                emitter.onComplete()
            }
        }
    }, BackpressureStrategy.DROP)

    override fun isCached(uri: Uri): Boolean {
        val dataSpec = DataSpec(uri)
        val (requestLength, bytesCached) = CacheUtil.getCached(dataSpec, cache, cacheKeyFactory)
        return requestLength == bytesCached
    }

    override fun remove(uri: Uri) {
        Single.just(uri.toCacheKey())
            .subscribeOn(schedulers.io)
            .map { CacheUtil.remove(cache, it) }
            .subscribe()
            .addTo(disposables)
    }

    override fun clear() {
        clearCache()
            .subscribeOn(schedulers.io)
            .subscribe { Timber.tag(TAG).i("Finished clearing cache") }
            .addTo(disposables)
    }

    private fun clearCache(): Completable = Completable.create { emitter ->
        cache.keys.forEach { key ->
            cache.getCachedSpans(key).forEach { cache.removeSpan(it) }
        }
        emitter.onComplete()
    }

    companion object {
        private const val TAG = "CachingLayerImpl"

        private const val CACHE_SIZE = 20L * 1024L * 1024L // 20MB

        @Volatile
        private var instance: CachingLayerImpl? = null

        fun init(
            context: Context,
            storage: Storage = StorageProvider.getInstance(),
            cacheKeyFactory: CacheKeyFactory = UriKeyCacheFactory(),
            cacheEvictor: CacheEvictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE),
            upstreamFactory: Factory = ExoPlayerUtils.getBaseFactory(context),
            schedulers: SchedulersProvider = AMSchedulersProvider()
        ): CachingLayerImpl = instance ?: synchronized(this) {
            instance ?: CachingLayerImpl(
                storage,
                cacheKeyFactory,
                cacheEvictor,
                ExoDatabaseProvider(context),
                upstreamFactory,
                schedulers
            ).also { instance = it }
        }

        fun getInstance(): CachingLayerImpl = instance ?: MainApplication.context?.let { init(it) }
        ?: throw IllegalStateException("CachingLayerImpl was not initialized")

        @VisibleForTesting
        fun getInstance(
            storage: Storage = StorageProvider.getInstance(),
            cacheKeyFactory: CacheKeyFactory = UriKeyCacheFactory(),
            cacheEvictor: CacheEvictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE),
            upstreamFactory: Factory = DefaultHttpDataSourceFactory(USER_AGENT_NAME, null),
            schedulers: SchedulersProvider = AMSchedulersProvider()
        ): CachingLayerImpl = instance ?: synchronized(this) {
            CachingLayerImpl(
                storage,
                cacheKeyFactory,
                cacheEvictor,
                null,
                upstreamFactory,
                schedulers
            ).also { instance = it }
        }
    }
}
