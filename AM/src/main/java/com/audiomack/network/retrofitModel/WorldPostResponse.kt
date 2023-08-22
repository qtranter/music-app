package com.audiomack.network.retrofitModel

data class WorldPostResponse(
    val id: String?,
    val title: String?,
    val slug: String?,
    val html: String?,
    val feature_image: String?,
    val custom_excerpt: String?,
    val excerpt: String?,
    val published_at: String?,
    val tags: List<WorldTagResponse>?
)
