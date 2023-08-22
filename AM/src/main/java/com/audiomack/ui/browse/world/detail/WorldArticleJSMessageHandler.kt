package com.audiomack.ui.browse.world.detail

import com.audiomack.data.deeplink.Deeplink
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import com.audiomack.utils.getStringOrNull
import org.json.JSONObject
import timber.log.Timber

interface WorldArticleJSMessageHandler {
    /**
     * Transforms a message into a [Deeplink] if the payload is well-formed
     * @param message: the raw [String] message received from Javascript
     */
    fun parseMessage(message: String, mixpanelSource: MixpanelSource): Deeplink?
}

class WorldArticleJSMessageHandlerImpl : WorldArticleJSMessageHandler {
    override fun parseMessage(message: String, mixpanelSource: MixpanelSource): Deeplink? {
        val json = try {
            JSONObject(message)
        } catch (e: Exception) {
            Timber.w(e)
            return null
        }

        when (val action = json.optString("action")) {
            "play", "add-to-queue" -> {
                val data = json.optJSONObject("data") ?: return null
                val artistSlug = data.getStringOrNull("artistSlug") ?: return null
                val musicSlug = data.getStringOrNull("musicSlug") ?: return null
                val musicType = MusicType.values().firstOrNull { it.typeForMusicApi == data.optString("musicType") } ?: return null

                return if (action == "play") {
                    when (musicType) {
                        MusicType.Song -> Deeplink.Song("$artistSlug/$musicSlug")
                        MusicType.Album -> Deeplink.AlbumPlay("$artistSlug/$musicSlug", mixpanelSource)
                        MusicType.Playlist -> Deeplink.PlaylistPlay("$artistSlug/$musicSlug", mixpanelSource)
                    }
                } else Deeplink.AddToQueue("$artistSlug/$musicSlug", musicType, mixpanelSource)
            }
            else -> return null
        }
    }
}
