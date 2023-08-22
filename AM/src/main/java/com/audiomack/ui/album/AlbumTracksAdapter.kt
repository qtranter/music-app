package com.audiomack.ui.album

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.adapters.viewholders.EmptyViewHolder
import com.audiomack.model.AMResultItem
import timber.log.Timber

class AlbumTracksAdapter(
    private var collection: AMResultItem,
    private var tracks: MutableList<AMResultItem>,
    private var followVisible: Boolean?,
    private var isFollowed: Boolean?,
    private val myDownloadsMode: Boolean,
    private val listener: Listener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val typeAlbum = 0
    private val typeAlbumFooter = 1

    interface Listener {
        fun onTrackTapped(track: AMResultItem)
        fun onTrackActionsTapped(track: AMResultItem)
        fun onTrackDownloadTapped(track: AMResultItem)
        fun onTrackFavoriteTapped(track: AMResultItem)
        fun onCommentsTapped()
        fun onFollowTapped()
        fun onUploaderTapped()
        fun onTagTapped(tag: String)
    }

    fun removeItem(track: AMResultItem): Boolean {
        val index = indexOfItemId(track.itemId)
        tracks.remove(track)
        return if (index != -1) {
            notifyDataSetChanged()
            true
        } else {
            false
        }
    }

    private fun indexOfItemId(itemId: String): Int {
        return tracks.indexOfFirst { itemId == it.itemId }
    }

    fun updateCollection(collection: AMResultItem) {
        this.collection = collection
        notifyItemChanged(itemCount - 1)
    }

    fun updateTracks(items: List<AMResultItem>) {
        tracks = items.toMutableList()
        notifyDataSetChanged()
    }

    fun updateFollowStatus(followed: Boolean?) {
        isFollowed = followed
        notifyItemChanged(itemCount - 1)
    }

    fun updateFollowVisibility(followVisible: Boolean?) {
        this.followVisible = followVisible
        notifyItemChanged(itemCount - 1)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            when (getItemViewType(position)) {
                typeAlbum -> (holder as AlbumTrackViewHolder).setup(
                    tracks[position],
                    myDownloadsMode,
                    listener
                )
                typeAlbumFooter -> (holder as AlbumCollectionFooterViewHolder).setup(
                    collection,
                    isFollowed ?: false,
                    followVisible ?: false,
                    object : AlbumCollectionFooterViewHolder.Listener {
                        override fun onCommentsClickListener() {
                            listener.onCommentsTapped()
                        }

                        override fun onFollowClickListener() {
                            listener.onFollowTapped()
                        }

                        override fun onUploaderClickListener() {
                            listener.onUploaderTapped()
                        }

                        override fun onTagClickListener(tag: String) {
                            listener.onTagTapped(tag)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return try {
            when (viewType) {
                typeAlbum -> AlbumTrackViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.row_albumtrack, parent, false
                    )
                )
                else -> AlbumCollectionFooterViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.row_collection_album_footer, parent, false
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
            position < itemCount - 1 -> typeAlbum
            else -> typeAlbumFooter
        }
    }

    override fun getItemCount() = tracks.size + 1
}
