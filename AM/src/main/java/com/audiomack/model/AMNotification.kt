package com.audiomack.model

import com.audiomack.data.sizes.SizesRepository
import com.audiomack.data.tracking.mixpanel.MixpanelBellTypeBenchmark
import com.audiomack.data.tracking.mixpanel.MixpanelBellTypeComment
import com.audiomack.data.tracking.mixpanel.MixpanelBellTypeFavorite
import com.audiomack.data.tracking.mixpanel.MixpanelBellTypeFavoritePlaylist
import com.audiomack.data.tracking.mixpanel.MixpanelBellTypeFollow
import com.audiomack.data.tracking.mixpanel.MixpanelBellTypePlaylist
import com.audiomack.data.tracking.mixpanel.MixpanelBellTypePlaylistUpdated
import com.audiomack.data.tracking.mixpanel.MixpanelBellTypeRepost
import com.audiomack.utils.DateUtils
import com.audiomack.utils.getJSONObjectOrNull
import com.audiomack.utils.getStringOrNull
import java.util.Date
import org.json.JSONObject

class AMNotification {

    lateinit var verb: AMNotificationVerb
        private set
    var createdAt: Date? = null
        private set
    var author: NotificationAuthor? = null
        private set
    var `object`: Any? = null
        private set
    var target: AMResultItem? = null
        private set
    var isSeen: Boolean = false
        private set
    var type: NotificationType = NotificationType.Unknown
        private set

    data class UpvoteCommentNotificationData(
        val threadId: String? = null,
        val uuid: String,
        val count: Long
    )

    data class NotificationAuthor(
        val name: String,
        val slug: String,
        val image: String,
        val verified: Boolean = false,
        val tastemaker: Boolean = false,
        val authenticated: Boolean = false
    ) {
        constructor(json: JSONObject) : this(
            json.optString("name"),
            json.optString("url_slug"),
            json.optString("image"),
            "yes" == json.optString("verified"),
            "tastemaker" == json.optString("verified"),
                listOf("authenticated", "verify-pending", "verify-declined").contains(json.optString("verified"))
        )
    }

    enum class AMNotificationVerb {
        Favorite,
        Follow,
        Repost,
        Playlist,
        FavoritePlaylist,
        PlaylistUpdated,
        Comment,
        Benchmark,
        PlaylistUpdatedBundle
    }

    // We should consider using strongly typed sealed classes for all types of notifications instead of relying on the [verb] and other fields
    sealed class NotificationType(val analyticsType: String) {
        object Favorite : NotificationType(MixpanelBellTypeFavorite)
        object Follow : NotificationType(MixpanelBellTypeFollow)
        object Repost : NotificationType(MixpanelBellTypeRepost)
        object Playlist : NotificationType(MixpanelBellTypePlaylist)
        object FavoritePlaylist : NotificationType(MixpanelBellTypeFavoritePlaylist)
        object PlaylistUpdated : NotificationType(MixpanelBellTypePlaylistUpdated)
        data class Comment(val comment: String, val commentReply: String?) : NotificationType(MixpanelBellTypeComment)
        data class Benchmark(val benchmark: BenchmarkModel) : NotificationType(MixpanelBellTypeBenchmark)
        data class UpvoteComment(val data: UpvoteCommentNotificationData) : NotificationType(MixpanelBellTypeBenchmark)
        data class PlaylistUpdatedBundle(val playlists: List<AMResultItem>, val songsImages: List<String>, val songsCount: Int) : NotificationType(MixpanelBellTypePlaylistUpdated)
        object Unknown : NotificationType("")
    }

    companion object {

        @JvmStatic
        fun fromJSON(jsonObject: JSONObject): AMNotification? {
            val notification = AMNotification()

            when (jsonObject.optString("verb")) {
                "favorite" -> {
                    notification.verb = AMNotificationVerb.Favorite
                    notification.type = NotificationType.Favorite
                }
                "follow" -> {
                    notification.verb = AMNotificationVerb.Follow
                    notification.type = NotificationType.Follow
                }
                "reup" -> {
                    notification.verb = AMNotificationVerb.Repost
                    notification.type = NotificationType.Repost
                }
                "playlisted" -> {
                    notification.verb = AMNotificationVerb.Playlist
                    notification.type = NotificationType.Playlist
                }
                "playlistfavorite" -> {
                    notification.verb = AMNotificationVerb.FavoritePlaylist
                    notification.type = NotificationType.FavoritePlaylist
                }
                "playlist_updated" -> {
                    notification.verb = AMNotificationVerb.PlaylistUpdated
                    notification.type = NotificationType.PlaylistUpdated
                }
                "comment" -> {
                    notification.verb = AMNotificationVerb.Comment

                    val extraContext = jsonObject.optJSONObject("extra_context") ?: return null
                    val isReply = !extraContext.isNull("parent")
                    val comment = extraContext.optString(if (isReply) "parent" else "comment", null) ?: return null
                    val commentReply = if (isReply) extraContext.optString("comment") else null
                    notification.type = NotificationType.Comment(comment, commentReply)
                }
                "benchmark" -> {
                    notification.verb = AMNotificationVerb.Benchmark

                    val extraContext = jsonObject.optJSONObject("extra_context") ?: return null
                    val type = extraContext.optString("type", "none")
                    val count = extraContext.optLong("count")
                    when (type) {
                        "comment-upvotes" -> {
                            val threadId = extraContext.getStringOrNull("thread")
                            val uuid = extraContext.getStringOrNull("uuid") ?: return null
                            notification.type = NotificationType.UpvoteComment(UpvoteCommentNotificationData(threadId, uuid, count))
                        }
                        "plays" -> {
                            notification.type = NotificationType.Benchmark(BenchmarkModel(type = BenchmarkType.fromString(type), milestone = count))
                        }
                        else -> return null
                    }
                }
                "playlist_updated_bundle" -> {
                    notification.verb = AMNotificationVerb.PlaylistUpdatedBundle

                    val extraContext = jsonObject.optJSONObject("extra_context") ?: return null
                    val images = extraContext.optJSONArray("image_base_added_songs")?.let { array ->
                        (0 until array.length()).mapNotNull { array.getStringOrNull(it) }.map { it + "?width=" + SizesRepository.tinyMusic }
                    }?.ifEmpty { return null } ?: return null
                    val playlists = extraContext.optJSONArray("playlists")?.let { array ->
                        (0 until array.length()).mapNotNull { array.getJSONObjectOrNull(it) }.mapNotNull { AMResultItem.fromBundledPlaylistNotificationJson(it, true) }
                    }?.ifEmpty { return null } ?: return null
                    val songsAdded = playlists.sumBy { it.newlyAddedSongs ?: 0 }
                    notification.type = NotificationType.PlaylistUpdatedBundle(playlists, images, songsAdded)
                }
                else -> return null
            }

            notification.isSeen = jsonObject.optBoolean("seen")

            notification.createdAt =
                DateUtils.getInstance().getNotificationDate(jsonObject.optString("created_at"))

            notification.author = jsonObject.optJSONObject("actor")?.let { NotificationAuthor(it) }

            if (notification.verb != AMNotificationVerb.PlaylistUpdatedBundle) {
                if (notification.verb == AMNotificationVerb.Follow) {
                    jsonObject.optJSONObject("actor")?.let { actorJson ->
                        notification.`object` = AMArtist.fromJSON(false, actorJson)
                    } ?: return null
                } else {
                    jsonObject.optJSONObject("object")?.let { objectJson ->
                        notification.`object` =
                            AMResultItem.fromJson(objectJson, true, null) ?: return null
                    } ?: return null
                }
                if (notification.verb == AMNotificationVerb.PlaylistUpdated) {
                    jsonObject.optJSONObject("target")?.let { targetJson ->
                        notification.target =
                            AMResultItem.fromJson(targetJson, true, null) ?: return null
                    } ?: return null
                }
            }

            return notification
        }
    }
}
