package com.audiomack.ui.playlist.details

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.model.AMGenre
import com.audiomack.model.AMResultItem

class PlaylistCollectionFooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val layoutComments = itemView.findViewById<ViewGroup>(R.id.layoutComments)
    private val layoutDate = itemView.findViewById<ViewGroup>(R.id.layoutDate)
    private val layoutGenre = itemView.findViewById<ViewGroup>(R.id.layoutGenre)
    private val layoutTotalPlays = itemView.findViewById<ViewGroup>(R.id.layoutTotalPlays)
    private val tvComments = itemView.findViewById<TextView>(R.id.tvComments)
    private val tvDatePrefix = itemView.findViewById<TextView>(R.id.tvDatePrefix)
    private val tvDate = itemView.findViewById<TextView>(R.id.tvDate)
    private val tvGenre = itemView.findViewById<TextView>(R.id.tvGenre)
    private val tvTotalPlays = itemView.findViewById<TextView>(R.id.tvTotalPlays)
    private val tvRuntime = itemView.findViewById<TextView>(R.id.tvRuntime)

    fun setup(collection: AMResultItem, commentsListener: () -> Unit) {
        listOf(layoutComments, layoutGenre, layoutTotalPlays).forEach {
            it.isVisible = !collection.isLocal
        }
        layoutDate.isVisible = collection.released != null

        tvComments.text = tvComments.context.getString(R.string.comments_count_template, collection.commentsShort)
        tvDatePrefix.text = tvDatePrefix.context.getString(R.string.musicinfo_lastupdated)
        tvDate.text = collection.lastUpdated
        tvGenre.text = AMGenre.fromApiValue(collection.genre).humanValue(itemView.context)
        tvTotalPlays.text = collection.playsExtended
        tvRuntime.text = getRuntimeDataString(collection)

        layoutComments.setOnClickListener { commentsListener() }
    }

    private fun getRuntimeDataString(collection: AMResultItem): String {
        val runtimeResId = R.string.musicinfo_runtime_playlist_value
        val totalInMinutes = buildString {
            append(totalDurationInMinutes(collection.tracks))
            if (hasPotentialExcessDuration(collection.tracks)) append("+")
        }
        return itemView.context.getString(runtimeResId, totalInMinutes, collection.tracks?.size ?: 0)
    }

    private fun hasPotentialExcessDuration(tracks: List<AMResultItem>?): Boolean {
        if (tracks == null) {
            return false
        }
        return tracks.any { it.duration == 0L }
    }

    private fun totalDurationInMinutes(tracks: List<AMResultItem>?): Long {
        if (tracks == null || tracks.isEmpty()) {
            return 0
        }
        return tracks.map { it.duration }.sum() / 60L // convert seconds to minutes
    }
}
