package com.audiomack.model

import android.content.Context
import com.audiomack.R
import org.json.JSONObject

class AMFeaturedSpot {

    private var type: String = ""
    var item: AMResultItem? = null
        private set
    var artist: AMArtist? = null
        private set

    fun getPrettyType(context: Context): String? {
        if (artist != null) {
            return (type + " " + context.getString(R.string.artist)).toUpperCase()
        } else {
            item?.let {
                return when {
                    it.isPlaylist -> (type + " " + context.getString(R.string.playlist)).toUpperCase()
                    it.isAlbum -> (type + " " + context.getString(R.string.album)).toUpperCase()
                    it.isPodcast -> (type + " " + context.getString(R.string.podcast)).toUpperCase()
                    else -> (type + " " + context.getString(R.string.song)).toUpperCase()
                }
            }
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        val spot = other as? AMFeaturedSpot ?: return false
        return if (item != null && spot.item != null) {
            item?.itemId == spot.item?.itemId
        } else if (artist != null && spot.artist != null) {
            artist?.artistId == spot.artist?.artistId
        } else {
            true
        }
    }

    override fun hashCode(): Int {
        return if (item != null) item!!.getItemId().hashCode() else if (artist != null) artist!!.artistId.hashCode() else 0
    }

    companion object {
        @JvmStatic
        fun fromJSON(json: JSONObject): AMFeaturedSpot? {
            val featuredSpot = AMFeaturedSpot()
            featuredSpot.type = json.optString("type")
            json.optJSONObject("ref")?.let { refJson ->
                val type = if (refJson.isNull("type")) null else refJson.optString("type")
                if (type != null && type != "artist") {
                    featuredSpot.item = AMResultItem.fromJson(refJson, true, null)
                } else {
                    featuredSpot.artist = AMArtist.fromJSON(false, refJson)
                }
            }
            return if (featuredSpot.item != null || featuredSpot.artist != null) featuredSpot else null
        }

        fun fromMusic(music: AMResultItem): AMFeaturedSpot {
            return AMFeaturedSpot().apply {
                item = music
            }
        }
    }
}
