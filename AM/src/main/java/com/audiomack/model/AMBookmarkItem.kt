package com.audiomack.model

import com.activeandroid.Model
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Table
import java.util.Date

@Table(name = "bookmarks")
data class AMBookmarkItem(
    @Column(name = "item_id") val itemId: String,
    @Column(name = "type") val type: String? = null,
    @Column(name = "artist") val artist: String? = null,
    @Column(name = "title") val title: String? = null,
    @Column(name = "album") val album: String? = null,
    @Column(name = "image") val image: String? = null,
    @Column(name = "featured") val featured: String? = null,
    @Column(name = "producer") val producer: String? = null,
    @Column(name = "genre") val genre: String? = null,
    @Column(name = "desc") val desc: String? = null,
    @Column(name = "url") val url: String? = null,
    @Column(name = "uploader_name") val uploaderName: String? = null,
    @Column(name = "uploader_id") val uploaderId: String? = null,
    @Column(name = "url_slug") val urlSlug: String? = null,
    @Column(name = "uploader_slug") val uploaderSlug: String? = null,
    @Column(name = "uploaded") val released: String? = null,
    @Column(name = "buy_url") val buyURL: String? = null,
    @Column(name = "download_url") val downloadURL: String? = null,
    @Column(name = "parent_id", index = true) val parentId: String? = null,
    @Column(name = "track_number") val trackNumber: Int = 0,
    @Column(name = "disc_number") val discNumber: Int = 0,
    @Column(name = "full_path") val fullPath: String? = null,
    @Column(name = "download_manager_id") val downloadManagerId: Long = 0,
    @Column(name = "video_ad") val videoAd: Boolean = false,
    @Column(name = "private_playlist") val privatePlaylist: Boolean = false,
    @Column(name = "created") val created: String? = null,
    @Column(name = "stream_only") val streamOnly: Boolean = false,
    @Column(
        name = "album_track_downloaded_as_single",
        index = true
    ) val albumTrackDownloadedAsSingle: Boolean = false,
    @Column(name = "download_completed") val downloadCompleted: Boolean = false,
    @Column(name = "original_image") val originalImage: String? = null,
    @Column(name = "playlist_image") val playlistImage: String? = null,
    @Column(name = "small_image") val smallImage: String? = null,
    @Column(name = "song_image") val songImage: String? = null,
    @Column(name = "cached") val cached: Boolean = false,
    @Column(name = "amp") val amp: Boolean = false,
    @Column(name = "amp_duration") val ampDuration: Int = 0,
    @Column(name = "synced") val synced: Boolean = false,
    @Column(name = "uploader_followed") val uploaderFollowed: Boolean = false,
    @Column(name = "last_updated") val lastUpdated: String? = null,
    @Column(name = "repost_artist_name") val repostArtistName: String? = null,
    @Column(name = "repost_timestamp") val repostTimestamp: Long = 0,
    @Column(name = "playlist") val playlist: String? = null,
    @Column(name = "offline_toast_shown") val offlineToastShown: Boolean = false,
    @Column(name = "banner") val banner: String? = null,
    @Column(name = "mixpanel_source") val originalMixpanelSource: String? = null,
    @Column(name = "comments") val commentCount: Int? = 0,
    @Column(name = "duration") val duration: Long = 0,
    @Column(name = "uploader_verified") val uploaderVerified: Boolean = false,
    @Column(name = "uploader_tastemaker") val uploaderTastemaker: Boolean = false,
    @Column(name = "uploader_authenticated") val uploaderAuthenticated: Boolean = false,
    @Column(name = "uploader_image") val uploaderImage: String? = null,
    @Column(name = "uploader_followers") val uploaderFollowers: Long? = null,
    @Column(name = "album_release_date") val albumReleaseDate: Long = 0,
    @Column(name = "song_release_date") val songReleaseDate: Long = 0,
    @Column(name = "tags") val tags: String? = null,
    @Column(name = "extra_key") val extraKey: String? = null,
    @Column(name = "premium_download") val premiumDownload: String? = null,
    @Column(name = "download_date") val downloadDate: Date? = null,
    @Column(name = "frozen") val frozen: Boolean = false,
    @Column(name = "local_media") val isLocal: Boolean = false
) : Model()
