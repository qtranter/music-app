package com.audiomack.network.retrofitApi

import com.audiomack.BuildConfig
import com.audiomack.network.retrofitModel.WorldPagesResponse
import com.audiomack.network.retrofitModel.WorldPostsResponse
import io.reactivex.Single
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WorldPostService {

    @GET("content/pages/")
    fun getPages(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 999,
        @Query("order") order: String = ORDER,
        @Query("key") key: String = BuildConfig.AM_GHOST_KEY
    ): Single<WorldPagesResponse>

    @GET("content/posts/")
    fun getPosts(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("filter") filter: String? = null,
        @Query("include") include: String = "tags",
        @Query("key") key: String = BuildConfig.AM_GHOST_KEY
    ): Single<WorldPostsResponse>

    @GET("content/posts/slug/{slug}")
    fun getPost(
        @Path("slug") slug: String,
        @Query("include") include: String = INCLUDE,
        @Query("native") native: Boolean = true,
        @Query("key") key: String = BuildConfig.AM_GHOST_KEY
    ): Single<WorldPostsResponse>

    companion object {
        const val TAG_QUALIFIER = "tag:"
        const val FILTER_ALL = ""

        private const val INCLUDE = "authors,tags"
        private const val ORDER = "updated_at"

        fun create(client: OkHttpClient): WorldPostService = Retrofit.Builder()
                .baseUrl(BuildConfig.AM_GHOST_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(WorldPostService::class.java)
    }
}
