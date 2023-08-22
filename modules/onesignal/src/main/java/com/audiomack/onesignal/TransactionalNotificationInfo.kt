package com.audiomack.onesignal

data class TransactionalNotificationInfo(
    val songName: String? = null,
    val songId: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val albumId: String? = null,
    val genre: String? = null,
    val playlistName: String? = null,
    val playlistId: String? = null,
    val campaign: String? = null
)
