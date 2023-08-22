package com.audiomack.model

data class ArtistWithBadge(
    val name: String,
    val verified: Boolean,
    val tastemaker: Boolean,
    val authenticated: Boolean
)
