package com.audiomack.onesignal

import android.net.Uri
import org.json.JSONObject

class OneSignalNotificationOpenHandler{

    fun processNotification(json: JSONObject): OneSignalNotificationParseResult? {

        val info = TransactionalNotificationInfo(
            songName = json.getStringOrNull("songname"),
            songId = json.getStringOrNull("songid"),
            artistName = json.getStringOrNull("artistname"),
            albumName = json.getStringOrNull("albumname"),
            albumId = json.getStringOrNull("albumid"),
            genre = json.getStringOrNull("genre"),
            playlistName = json.getStringOrNull("playlistname"),
            playlistId = json.getStringOrNull("playlistid"),
            campaign = json.getStringOrNull("campaign")
        )

        json.getStringOrNull("am_deeplink")?.let {
            try {
                return OneSignalNotificationParseResult(Uri.parse(it), info)
            } catch (e: Exception) {}
        }

        json.getStringOrNull("ticket_id")?.let {
            return OneSignalNotificationParseResult(Uri.parse("audiomack://support/$it"), info)
        }

        return null
    }
}

private fun JSONObject.getStringOrNull(name: String): String? = if (isNull(name)) null else optString(name)