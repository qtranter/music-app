package com.audiomack.model

enum class NotificationPreferenceType(val apiCode: String) {
    NewSongAlbum("push.new_music_for_follower"),
    WeeklyArtistReport("email.weekly_summary"),
    PlayMilestones("push.benchmark_play_count"),
    CommentReplies("push.comment_new_reply"),
    UpvoteMilestones("push.benchmark_comment_vote_count"),
    VerifiedPlaylistAdds("email.added_to_verified_playlist");

    companion object {
        fun fromApiCode(apiCode: String): NotificationPreferenceType? {
            return values().firstOrNull { it.apiCode == apiCode }
        }
    }
}

data class NotificationPreferenceTypeValue(
    val type: NotificationPreferenceType,
    val value: Boolean
)
