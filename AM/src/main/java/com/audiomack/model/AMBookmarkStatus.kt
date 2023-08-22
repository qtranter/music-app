package com.audiomack.model

import com.activeandroid.Model
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Table
import com.audiomack.data.bookmarks.BookmarkManager
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.remotevariables.datasource.FirebaseRemoteVariablesDataSource
import java.util.Date

@Table(name = "bookmark_status")
class AMBookmarkStatus : Model() {

    @Column(name = "bookmark_date")
    var bookmarkDate: Date? = null

    @Column(name = "current_item_id")
    var currentItemId: String? = null

    @Column(name = "playback_position")
    var playbackPosition: Int = 0 // ms

    @Column(name = "screen_type")
    var screenType: String? = null

    @Column(name = "entity_id")
    var entityId: String? = null

    val isValid: Boolean
        get() {
            val valid =
                bookmarkDate != null && Date().time < bookmarkDate!!.time + 1000 * 60 * 60 * RemoteVariablesProviderImpl(
                    FirebaseRemoteVariablesDataSource
                ).bookmarksExpirationHours
            if (!valid) {
                BookmarkManager.deleteStatus()
                BookmarkManager.deleteAllBookmarkedItems()
            }
            return valid
        }

    override fun toString(): String {
        return ("{" +
            " \"bookmarkDate\":" + bookmarkDate +
            ", \"currentItemId\":\"" + currentItemId + "\"" +
            ", \"playbackPosition\":\"" + playbackPosition + "\"" +
            ", \"screenType\":\"" + screenType + "\"" +
            ", \"entityId\":\"" + entityId + "\"" +
            ", \"valid\":\"" + isValid + "\"" +
            "}")
    }
}
