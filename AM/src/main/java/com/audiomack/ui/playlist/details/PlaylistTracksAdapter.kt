package com.audiomack.ui.playlist.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.adapters.viewholders.EmptyViewHolder
import com.audiomack.model.AMResultItem
import timber.log.Timber

class PlaylistTracksAdapter(
    private var collection: AMResultItem,
    private val tracks: MutableList<AMResultItem>,
    private val allowInlineFavoritingAndFooter: Boolean,
    private val listener: Listener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val typePlaylist = 0
    private val typePlaylistFooter = 1

    interface Listener {
        fun onTrackTapped(track: AMResultItem)
        fun onTrackActionsTapped(track: AMResultItem)
        fun onTrackDownloadTapped(track: AMResultItem)
        fun onTrackFavoriteTapped(track: AMResultItem)
        fun onCommentsTapped()
    }

    fun removeItem(track: AMResultItem) {
        val index = indexOfItemId(track.itemId)
        this.tracks.remove(track)
        if (index != -1) {
            notifyItemRemoved(index)
        }
    }

    fun indexOfItemId(itemId: String): Int {
        return tracks.indexOfFirst { itemId == it.itemId }
    }

    fun updateCollection(collection: AMResultItem) {
        this.collection = collection
        this.notifyItemChanged(itemCount - 1)
    }

    fun updateTracks(tracks: List<AMResultItem>) {
        this.tracks.clear()
        this.tracks.addAll(tracks)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            when (getItemViewType(position)) {
                typePlaylist -> (holder as PlaylistTrackViewHolder).setup(position, tracks[position], allowInlineFavoritingAndFooter, listener)
                typePlaylistFooter -> (holder as PlaylistCollectionFooterViewHolder).setup(collection) { listener.onCommentsTapped() }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return try {
            when (viewType) {
                typePlaylist -> PlaylistTrackViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.row_playlisttrack, parent, false
                    )
                )
                else -> PlaylistCollectionFooterViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.row_collection_playlist_footer, parent, false
                    )
                )
            }
        } catch (e: Exception) {
            Timber.w(e)
            EmptyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_empty, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < itemCount - 1 -> typePlaylist
            else -> if (allowInlineFavoritingAndFooter) typePlaylistFooter else typePlaylist
        }
    }

    override fun getItemCount(): Int {
        return tracks.size + (if (allowInlineFavoritingAndFooter) 1 else 0)
    }
}
