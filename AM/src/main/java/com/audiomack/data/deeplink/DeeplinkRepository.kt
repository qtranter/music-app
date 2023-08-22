package com.audiomack.data.deeplink

import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.model.AMGenre
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.BenchmarkType
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import com.audiomack.model.SearchType
import com.audiomack.model.WorldPage
import java.util.Locale

interface DeeplinkDataSource {

    /**
     * Used to contain info about deeplinks being processed by the app.
     */
    var handlingDeeplink: Boolean

    /**
     * Returns a Deeplink obtained from the provided (optional) Intent, looking at the [data] field or some custom extras passed [am_deeplink].
     */
    fun obtainDeeplink(intent: Intent?): Deeplink?

    /**
     * Stores the branch deeplink for later usage.
     */
    fun updateBranchDeeplink(deeplink: String?)
}

class DeeplinkRepository private constructor(
    private val remoteVariablesProvider: RemoteVariablesProvider
) : DeeplinkDataSource {

    private var branchDeeplink: String? = null
    private var lastDeeplinkUrl: String? = null
    private var lastDeeplinkTimestamp: Long = 0

    @VisibleForTesting
    var duplicateDeeplinkCheckEnabled = true

    override var handlingDeeplink: Boolean = false

    override fun obtainDeeplink(intent: Intent?): Deeplink? {
        val nonNullIntent = intent ?: return branch()
        return notification(nonNullIntent)
            ?: direct(nonNullIntent)
            ?: shortcut(nonNullIntent)
    }

    override fun updateBranchDeeplink(deeplink: String?) {
        deeplink?.let { branchDeeplink = it }
    }

    private fun notification(intent: Intent): Deeplink? {
        val fullLink = intent.getStringExtra("am_deeplink") ?: return null

        val fullUri = Uri.parse(fullLink)

        val link = fullLink
            .removePrefix("audiomack://")
            .removePrefix("https://audiomack.com/")
            .removePrefix("https://audiomack.com")
            .removePrefix("http://audiomack.com/")
            .removePrefix("http://audiomack.com")

        val uri = Uri.parse(link)

        if (duplicateDeeplinkCheckEnabled) {
            // Branch deeplinks can be hadled twice based on their content, here's a trick to prevent the same deeplink to be triggered twice
            val canHandleDeeplink =
                lastDeeplinkUrl == null || lastDeeplinkUrl != link || (System.currentTimeMillis() - lastDeeplinkTimestamp) > 1000
            if (!canHandleDeeplink) {
                return null
            }
            lastDeeplinkUrl = link
            lastDeeplinkTimestamp = System.currentTimeMillis()
        }

        handlingDeeplink = !intent.getBooleanExtra("allow_bookmarks", false)

        val firstPathSegment = uri.pathSegments.firstOrNull()

        return when {
            // Do not process links that are not related to audiomack
            !(fullUri.scheme == "audiomack" || fullUri.host?.contains("audiomack.com") ?: false) -> parseLink(fullLink)
            firstPathSegment == null && !uri.getQueryParameter("verifyHash").isNullOrBlank() -> Deeplink.EmailVerification(uri.getQueryParameter("verifyHash")!!)
            firstPathSegment == null && !uri.getQueryParameter("verifyPasswordToken").isNullOrBlank() -> Deeplink.ResetPassword(uri.getQueryParameter("verifyPasswordToken")!!)
            firstPathSegment == "reset-password" -> uri.getQueryParameter("verifyPasswordToken")?.takeIf { it.isNotBlank() }?.let { Deeplink.ResetPassword(it) } ?: parseLink(fullLink)
            link == "edit/profile/password" -> Deeplink.ChangePassword
            link == "home" -> Deeplink.Home
            link == "nowplaying" -> Deeplink.NowPlaying
            link == "premium" -> Deeplink.Premium(InAppPurchaseMode.Deeplink)
            link == "suggested_follows" || link == "artists/popular" -> Deeplink.SuggestedFollows
            firstPathSegment == "support" -> uri.pathSegments.getOrNull(1)?.let { id -> Deeplink.SupportTicket(id) } ?: Deeplink.Support
            link == "artist_downloads" -> Deeplink.MyDownloads
            firstPathSegment == "artist_favorites" -> uri.pathSegments.getOrNull(1)?.let { slug -> Deeplink.ArtistFavorites(slug) } ?: Deeplink.MyFavorites
            firstPathSegment == "artist_uploads" -> uri.pathSegments.getOrNull(1)?.let { slug -> Deeplink.ArtistUploads(slug) } ?: Deeplink.MyUploads
            firstPathSegment == "artist_playlists" -> uri.pathSegments.getOrNull(1)?.let { slug -> Deeplink.ArtistPlaylists(slug) } ?: Deeplink.MyPlaylists
            firstPathSegment == "artist_followers" -> uri.pathSegments.getOrNull(1)?.let { slug -> Deeplink.ArtistFollowers(slug) } ?: Deeplink.MyFollowers
            firstPathSegment == "artist_following" -> uri.pathSegments.getOrNull(1)?.let { slug -> Deeplink.ArtistFollowing(slug) } ?: Deeplink.MyFollowing
            firstPathSegment == "playlists" || firstPathSegment == "playlists_browse" || link.startsWith("playlists/browse") -> Deeplink.Playlists(uri.pathSegments.getOrNull(2))
            firstPathSegment == "benchmark" -> parseBenchmark(uri.path!!.removePrefix(uri.pathSegments.first() + "/"))
            firstPathSegment == "notifications" -> Deeplink.Notifications
            firstPathSegment == "world" -> parseWorld(uri) ?: parseLink(fullLink)
            firstPathSegment == "trending-now" -> Deeplink.Trending()
            firstPathSegment == "music_trending" -> Deeplink.Trending()
            firstPathSegment == "music_songs" -> Deeplink.TopSongs()
            firstPathSegment == "music_albums" -> Deeplink.TopAlbums()
            firstPathSegment == "music_recent" -> Deeplink.RecentlyAdded()
            firstPathSegment == "recent" -> Deeplink.RecentlyAdded()
            link.startsWith("music_") -> parseChartsWithGenre(link)
            (uri.pathSegments.contains("song") || uri.pathSegments.contains("album") || uri.pathSegments.contains("playlist")) && uri.queryParameterNames.contains("comment") -> parseComment(uri)
            firstPathSegment == "artist" && uri.pathSegments.size <= 2 -> parseLegacyArtistURLs(uri)
            firstPathSegment == "song" && uri.pathSegments.size == 3 -> Deeplink.Song("${uri.pathSegments[1]}/${uri.pathSegments[2]}")
            firstPathSegment == "album" && uri.pathSegments.size == 3 -> Deeplink.Album("${uri.pathSegments[1]}/${uri.pathSegments[2]}")
            firstPathSegment == "playlist" && uri.pathSegments.size == 3 -> Deeplink.Playlist("${uri.pathSegments[1]}/${uri.pathSegments[2]}")
            link.startsWith("share/artist") -> uri.pathSegments.getOrNull(2)?.let { Deeplink.ArtistShare(it) }
            link.startsWith("share/playlist") -> uri.pathSegments.drop(2).takeLast(2).takeIf { it.size == 2 }?.let { Deeplink.PlaylistShare(it.joinToString("/")) }
            link.startsWith("share/album") -> uri.pathSegments.drop(2).takeLast(2).takeIf { it.size == 2 }?.let { Deeplink.AlbumShare(it.joinToString("/")) }
            link.startsWith("share/song") -> uri.pathSegments.drop(2).takeLast(2).takeIf { it.size == 2 }?.let { Deeplink.SongShare(it.joinToString("/")) }
            firstPathSegment == "login" -> Deeplink.Login
            firstPathSegment == "onboarding-artistselect" -> Deeplink.ArtistSelectOnboarding
            // Do not process reserved audiomack links
            remoteVariablesProvider.deeplinksPathsBlacklist.contains(firstPathSegment) -> parseLink(fullLink)
            uri.pathSegments.size in 1..3 -> parseVanityURLs(uri) ?: parseLink(fullLink)
            else -> parseLink(fullLink)
        }
    }

    private fun direct(intent: Intent): Deeplink? {
        val scheme = intent.scheme ?: return null
        val uri = intent.data ?: return null
        return if (scheme == "audiomack" || scheme.contains("http")) {
            notification(Intent().apply {
                putExtra("am_deeplink", uri.toString())
            })
        } else null
    }

    private fun shortcut(intent: Intent): Deeplink? {
        handlingDeeplink = intent.hasExtra("shortcut")
        val result = when (intent.getStringExtra("shortcut") ?: return null) {
            "offline" -> Deeplink.MyDownloads
            "topSongs" -> Deeplink.TopSongs()
            "favorites" -> Deeplink.MyFavorites
            "playlists" -> Deeplink.MyPlaylists
            else -> null
        }
        intent.removeExtra("shortcut")
        return result
    }

    private fun branch(): Deeplink? {
        branchDeeplink?.let { branch ->
            val withoutScheme = branch.replace("audiomack://", "")
            val key = withoutScheme.split("/")[0]
            var value = withoutScheme.substring(key.length)
            if (value.isNotEmpty() && value.startsWith("/")) {
                value = value.substring(1)
            }
            branchDeeplink = null
            return notification(Intent().apply {
                putExtra("type", key)
                putExtra("id", value)
            })
        }
        return null
    }

    private fun parseBenchmark(link: String): Deeplink.Benchmark? {
        try {
            val params = link.split("/")
            if (params.size == 5) {
                val entityType = params[0]
                val urlSlug = params[1] + "/" + params[2]
                val benchmarkType = params[3]
                val benchmarkValue = params[4]
                val benchmark = BenchmarkModel(BenchmarkType.fromString(benchmarkType), null, benchmarkValue.toLong())
                return Deeplink.Benchmark(urlSlug, entityType, benchmark)
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseChartsWithGenre(link: String): Deeplink? {
        return link.split("_").takeIf { it.size > 2 }?.let { parts ->
            val genre = parts[1].takeIf { AMGenre.fromApiValue(it) != AMGenre.Other } ?: return null
            when (parts[2]) {
                "songs" -> Deeplink.TopSongs(genre)
                "albums" -> Deeplink.TopAlbums(genre)
                "trending" -> Deeplink.Trending(genre)
                else -> null
            }
        }
    }

    private fun parseComment(uri: Uri): Deeplink.Comments? {
        val artistSlug = uri.pathSegments.firstOrNull() ?: return null
        val musicType = uri.pathSegments.getOrNull(1) ?: return null
        val musicSlug = uri.pathSegments.getOrNull(2) ?: return null
        val commentId = uri.getQueryParameter("comment") ?: return null
        val threadId = uri.getQueryParameter("thread")
        val musicId = "$artistSlug/$musicSlug"
        return Deeplink.Comments(musicId, musicType, commentId, threadId)
    }

    private fun parseLegacyArtistURLs(uri: Uri): Deeplink? {
        val artistSlug = uri.pathSegments.getOrNull(1) ?: return null
        return when (uri.pathSegments.getOrNull(2)) {
            null -> Deeplink.Artist(artistSlug)
            "favorites" -> Deeplink.ArtistFavorites(artistSlug)
            "playlists" -> Deeplink.ArtistPlaylists(artistSlug)
            "followers" -> Deeplink.ArtistFollowers(artistSlug)
            "following" -> Deeplink.ArtistFollowing(artistSlug)
            "uploads" -> Deeplink.ArtistUploads(artistSlug)
            else -> return null
        }
    }

    /**
     * New vanity URLs have the artist slug as the first path component/
     * e.g.
     * - matteinn
     * - matteinn/favorites
     * - matteinn/song/lit
     * - matteinn/album/lit
     * - matteinn/playlist/lit
     */
    private fun parseVanityURLs(uri: Uri): Deeplink? {
        val artistSlug = uri.pathSegments.first()
        return when (uri.pathSegments.getOrNull(1)) {
            null -> Deeplink.Artist(artistSlug)
            "favorites" -> Deeplink.ArtistFavorites(artistSlug)
            "playlists" -> Deeplink.ArtistPlaylists(artistSlug)
            "followers" -> Deeplink.ArtistFollowers(artistSlug)
            "following" -> Deeplink.ArtistFollowing(artistSlug)
            "uploads" -> Deeplink.ArtistUploads(artistSlug)
            "song" -> uri.pathSegments.getOrNull(2)?.let { Deeplink.Song("$artistSlug/$it") }
            "album" -> uri.pathSegments.getOrNull(2)?.let { Deeplink.Album("$artistSlug/$it") }
            "playlist" -> uri.pathSegments.getOrNull(2)?.let { Deeplink.Playlist("$artistSlug/$it") }
            else -> null
        }
    }

    private fun parseWorld(uri: Uri): Deeplink? {
        return when {
            uri.pathSegments.size == 1 -> Deeplink.World
            // Real page, reuse the slug for the title and adjust the slug
            uri.pathSegments.size == 2 -> Deeplink.WorldPage(
                WorldPage(uri.pathSegments[1], "hash-${uri.pathSegments[1]}")
            )
            // Tag, will be mapped to a page
            uri.pathSegments.size == 3 && uri.pathSegments[1] == "tag" -> Deeplink.WorldPage(
                WorldPage(
                    title = uri.pathSegments[2].capitalize(Locale.getDefault()),
                    slug = uri.pathSegments[2]
                )
            )
            uri.pathSegments.size == 3 && uri.pathSegments[1] == "post" ->
                Deeplink.WorldPost(uri.pathSegments[2])
            else -> return null
        }
    }

    private fun parseLink(link: String): Deeplink? {
        return if (link.isNotBlank()) Deeplink.Link(Uri.parse(link)) else null
    }

    companion object {
        @Volatile
        private var instance: DeeplinkRepository? = null

        @JvmStatic
        fun getInstance(
            remoteVariablesProvider: RemoteVariablesProvider = RemoteVariablesProviderImpl()
        ): DeeplinkRepository =
            instance ?: DeeplinkRepository(remoteVariablesProvider).also { instance = it }
    }
}

sealed class Deeplink(val isModal: Boolean) {
    // Support
    object Support : Deeplink(true)
    data class SupportTicket(val ticketId: String) : Deeplink(true)
    // Generic
    data class Premium(val mode: InAppPurchaseMode) : Deeplink(true)
    object Home : Deeplink(true)
    object NowPlaying : Deeplink(true)
    data class Link(val uri: Uri) : Deeplink(true)
    data class EmailVerification(val hash: String) : Deeplink(true)
    object ChangePassword : Deeplink(true)
    data class ResetPassword(val token: String) : Deeplink(true)
    object ArtistSelectOnboarding : Deeplink(true)
    object Login : Deeplink(true)
    // My Library
    object MyDownloads : Deeplink(false)
    object MyFavorites : Deeplink(false)
    object MyUploads : Deeplink(false)
    object MyPlaylists : Deeplink(false)
    object MyFollowers : Deeplink(false)
    object MyFollowing : Deeplink(false)
    object Notifications : Deeplink(true)
    // Artist
    data class Artist(val id: String) : Deeplink(true)
    data class ArtistFavorites(val id: String) : Deeplink(true)
    data class ArtistUploads(val id: String) : Deeplink(true)
    data class ArtistPlaylists(val id: String) : Deeplink(true)
    data class ArtistFollowers(val id: String) : Deeplink(true)
    data class ArtistFollowing(val id: String) : Deeplink(true)
    data class ArtistShare(val id: String) : Deeplink(true)
    // Music
    data class Playlist(val id: String) : Deeplink(true)
    data class PlaylistShare(val id: String) : Deeplink(true)
    data class PlaylistPlay(val id: String, val mixpanelSource: MixpanelSource) : Deeplink(true) // From World articles only
    data class Album(val id: String) : Deeplink(true)
    data class AlbumShare(val id: String) : Deeplink(true)
    data class AlbumPlay(val id: String, val mixpanelSource: MixpanelSource) : Deeplink(true) // From World articles only
    data class Song(val id: String) : Deeplink(true)
    data class SongShare(val id: String) : Deeplink(true)
    data class Playlists(val tag: String? = null) : Deeplink(false)
    data class TopSongs(val genre: String? = null, val period: String? = null) : Deeplink(false)
    data class TopAlbums(val genre: String? = null, val period: String? = null) : Deeplink(false)
    data class Trending(val genre: String? = null) : Deeplink(false)
    object World : Deeplink(false)
    data class WorldPage(val page: com.audiomack.model.WorldPage) : Deeplink(false)
    data class WorldPost(val slug: String) : Deeplink(true)
    data class RecentlyAdded(val genre: String? = null) : Deeplink(false)
    data class Comments(val id: String, val type: String, val uuid: String, val threadId: String? = null) : Deeplink(true)
    data class AddToQueue(val id: String, val type: MusicType, val mixpanelSource: MixpanelSource) : Deeplink(true) // From World articles only
    // Feed
    object SuggestedFollows : Deeplink(true)
    // Internal usage only
    object Timeline : Deeplink(false)
    data class Search(val query: String? = null, val searchType: SearchType? = null) : Deeplink(false)
    data class Benchmark(val entityId: String, val entityType: String, val benchmark: BenchmarkModel) : Deeplink(true)
}
