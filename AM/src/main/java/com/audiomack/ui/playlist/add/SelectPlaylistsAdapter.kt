package com.audiomack.ui.playlist.add

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.adapters.viewholders.EmptyViewHolder
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMMusicButtonModel
import com.audiomack.model.AMResultItem
import com.audiomack.views.AMAddToPlaylistButton
import java.util.ArrayList
import java.util.Locale

class SelectPlaylistsAdapter(recyclerView: RecyclerView, private val listener: SelectPlaylistsAdapterListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val typeNew = 0
    private val typePlaylist = 1
    private val typeLoading = 2

    private val items: MutableList<AMResultItem?> = ArrayList()

    private var loadingMore: Boolean = false
    private var loadingMoreEnabled: Boolean = false

    private val visibleThreshold = 1
    private var lastVisibleItem = 0
    private var totalItemCount = 0

    interface SelectPlaylistsAdapterListener {
        fun didTapNew()
        fun didStartLoadMore()
        fun didTogglePlaylist(playlist: AMResultItem, index: Int)
    }

    init {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (loadingMoreEnabled) {
                    (recyclerView.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                        totalItemCount = layoutManager.itemCount
                        lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                        if (!loadingMore && totalItemCount <= lastVisibleItem + visibleThreshold) {
                            showLoadmore()
                            loadingMore = true
                            this@SelectPlaylistsAdapter.listener.didStartLoadMore()
                        }
                    }
                }
            }
        })
    }

    fun enableLoadMore() {
        this.loadingMoreEnabled = true
    }

    fun disableLoadMore() {
        this.loadingMoreEnabled = false
    }

    private fun showLoadmore() {
        items.add(null)
        notifyItemInserted(items.size + 1)
        loadingMore = true
    }

    fun hideLoadMore(notify: Boolean) {
        if (items.size > 0 && items[items.size - 1] == null) {
            items.removeAt(items.size - 1)
            if (notify) {
                notifyItemRemoved(items.size + 1)
            }
        }
        loadingMore = false
    }

    fun addPlaylists(playlists: List<AMResultItem>) {
        hideLoadMore(false)
        this.items.addAll(playlists)
        this.notifyDataSetChanged()
    }

    fun getItem(position: Int): AMResultItem? {
        return items.getOrNull(position - 1)
    }

    override fun getItemCount(): Int {
        return items.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return typeNew
        }
        return if (position == itemCount - 1 && items[position - 1] == null) typeLoading else typePlaylist
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            typeNew -> CreatePlaylistViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_select_playlists_create, parent, false))
            typePlaylist -> PlaylistViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_select_playlists, parent, false))
            else -> EmptyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_loadingmore, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PlaylistViewHolder) {
            getItem(position)?.let { item ->
                holder.setup(item, position)
            }
        } else if (position == 0) {
            holder.itemView.setOnClickListener { listener.didTapNew() }
        }
    }

    private inner class PlaylistViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {

        private val imageView: ImageView = view.findViewById(R.id.imageView)
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvSongs: TextView = view.findViewById(R.id.tvSongs)
        private val addToPlaylistButton: AMAddToPlaylistButton = view.findViewById(R.id.musicButton)

        fun setup(playlist: AMResultItem, position: Int) {
            PicassoImageLoader.load(
                imageView.context,
                playlist.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
                imageView
            )

            tvTitle.text = playlist.title
            tvSongs.text = String.format(Locale.US, "%d %s", playlist.playlistTracksCount, if (playlist.playlistTracksCount != 1) tvSongs.resources.getString(R.string.playlist_song_plural) else tvSongs.resources.getString(
                    R.string.playlist_song_singular
                ))
            addToPlaylistButton.set(
                AMMusicButtonModel(
                    playlist,
                    View.OnClickListener { listener.didTogglePlaylist(playlist, position) }
                )
            )

            itemView.setOnClickListener { notifyItemChanged(position) }
        }
    }

    private inner class CreatePlaylistViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view)
}
