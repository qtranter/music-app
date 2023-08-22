package com.audiomack.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.adapters.viewholders.AccountViewHolder
import com.audiomack.adapters.viewholders.EmptyViewHolder
import com.audiomack.adapters.viewholders.FooterViewHolder
import com.audiomack.adapters.viewholders.HeaderViewHolder
import com.audiomack.adapters.viewholders.LoadMoreViewHolder
import com.audiomack.adapters.viewholders.MusicBrowseSmallViewHolder
import com.audiomack.adapters.viewholders.MusicTinyViewHolder
import com.audiomack.adapters.viewholders.PlaylistGridViewHolder
import com.audiomack.adapters.viewholders.PlaylistViewHolder
import com.audiomack.model.AMArtist
import com.audiomack.model.AMEmptyRow
import com.audiomack.model.AMFeaturedSpot
import com.audiomack.model.AMFooterRow
import com.audiomack.model.AMHeaderRow
import com.audiomack.model.AMLoadingRow
import com.audiomack.model.AMNotification
import com.audiomack.model.AMResultItem
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.CellType
import com.audiomack.ui.notifications.NotificationBundledPlaylistViewHolder
import com.audiomack.ui.notifications.NotificationViewHolder
import com.audiomack.utils.convertDpToPixel
import java.util.Random
import kotlin.math.max
import timber.log.Timber

class DataRecyclerViewAdapter(
    recyclerView: RecyclerView,
    var cellType: CellType,
    val listener: RecyclerViewListener,
    private val myDownloadsMode: Boolean,
    private val searchMode: Boolean,
    var showRepostInfo: Boolean,
    private val removePaddingFromFirstPosition: Boolean,
    val headerView: View?,
    @LayoutRes val footerLayoutResId: Int?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: MutableList<Any> = mutableListOf(AMHeaderRow, AMEmptyRow)
    private var loadingMore = false
    private var loadingMoreEnabled = false
    private var showFollowBtn = false
    private var visibleThreshold = 1
    private var lastVisibleItem = 0
    private var totalItemCount = 0
    private var bottomSectionHeight = 0
    private var loadMoreCellHeight = recyclerView.context.convertDpToPixel(if (cellType === CellType.PLAYLIST_GRID) 150f else 60f)
    var offsetCounter = 0
        private set

    interface RecyclerViewListener {
        fun onClickTwoDots(item: AMResultItem)
        fun onClickDownload(item: AMResultItem)
        fun onClickFollow(artist: AMArtist)
        fun onStartLoadMore()
        fun onScrollTo(verticalOffset: Int)
        fun onClickItem(item: Any)
        fun onClickNotificationArtist(artistSlug: String, type: AMNotification.NotificationType)
        fun onClickNotificationMusic(item: AMResultItem, comment: Boolean, type: AMNotification.NotificationType)
        fun onClickNotificationBenchmark(item: AMResultItem, benchmark: BenchmarkModel, type: AMNotification.NotificationType)
        fun onClickNotificationCommentUpvote(music: AMResultItem, data: AMNotification.UpvoteCommentNotificationData, type: AMNotification.NotificationType)
        fun onClickNotificationBundledPlaylists(playlists: List<AMResultItem>, type: AMNotification.NotificationType)
        fun onClickFooter()
    }

    init {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                offsetCounter += dy
                if (recyclerView.computeVerticalScrollOffset() == 0) {
                    offsetCounter = 0
                }
                listener.onScrollTo(offsetCounter)
                if (loadingMoreEnabled) {
                    (recyclerView.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                        totalItemCount = layoutManager.itemCount
                        lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                        if (!loadingMore && totalItemCount <= lastVisibleItem + visibleThreshold) {
                            showLoadMore()
                            loadingMore = true
                            listener.onStartLoadMore()
                        }
                    }
                }
            }
        })
    }

    fun enableLoadMore() {
        loadingMoreEnabled = true
    }

    fun disableLoadMore() {
        loadingMoreEnabled = false
    }

    fun disableLoadMoreAfterReachingLastPage(page: Int) {
        if (page < 2 && offsetCounter > loadMoreCellHeight) {
            offsetCounter = max(0, offsetCounter - loadMoreCellHeight)
        }
        loadingMoreEnabled = false
    }

    private fun showLoadMore() {
        items.add(AMLoadingRow)
        notifyItemInserted(items.size)
        loadingMore = true
    }

    fun hideLoadMore(notify: Boolean) {
        if (items.lastOrNull() == AMLoadingRow) {
            items.removeAt(items.lastIndex)
            if (notify) {
                notifyItemRemoved(items.size)
            }
        }
        loadingMore = false
    }

    fun isLoadingMore(): Boolean = loadingMore

    fun addBottom(bottomItems: List<Any?>) {
        hideLoadMore(false)
        val emptyRow = AMEmptyRow
        if (items.contains(emptyRow)) items.remove(emptyRow)
        items.addAll(bottomItems.filter { it != null && !items.contains(it) }.map { it as Any })
        items.add(emptyRow as Any)
        notifyDataSetChanged()
    }

    fun getItems(): List<Any> {
        if (items.lastOrNull() == AMLoadingRow) {
            return arrayListOf(items).apply {
                removeAt(0)
                lastIndex.takeIf { it != -1 }?.let {
                    removeAt(it)
                }
            }
        }
        return items
    }

    fun replaceItem(position: Int, item: Any?) {
        items[position] = item as Any
        notifyItemChanged(position)
    }

    fun insertItem(position: Int, item: Any?) {
        items.add(position, item as Any)
        notifyItemInserted(position)
    }

    /**
     * @param itemId music id
     * @return index of the items or -1 if not found
     */
    fun indexOfItemId(itemId: String?): Int {
        if (itemId == null) return -1
        return items.indexOfFirst { (it as? AMResultItem)?.itemId == itemId || (it as? AMFeaturedSpot)?.item?.itemId == itemId }
    }

    /**
     * @param itemId music id
     * @return list of indices, empty if the item has not been found
     */
    fun indicesOfItemId(itemId: String?): List<Int> {
        val indices: MutableList<Int> = mutableListOf()
        if (itemId != null) {
            for (i in items.indices) {
                (items[i] as? AMResultItem)?.takeIf { it.itemId == itemId }?.let {
                    indices.add(i)
                }
                (items[i] as? AMFeaturedSpot)?.item?.takeIf { it.itemId == itemId }?.let {
                    indices.add(i)
                }
            }
        }
        return indices
    }

    fun removeItem(item: AMResultItem) {
        items.indexOf(item as Any).takeIf { it != -1 }?.let { index ->
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateItem(item: Any, index: Int) {
        items[index] = item
    }

    fun clear(notify: Boolean) {
        items.clear()
        items.add(AMHeaderRow as Any)
        items.add(AMEmptyRow as Any)
        loadingMore = false
        if (notify) {
            notifyDataSetChanged()
        }
    }

    fun pickRandomMusic(): AMResultItem? {
        val musicOnly = items.mapNotNull { it as? AMResultItem }.filter { !it.isGeoRestricted }.ifEmpty { return null }
        val music = musicOnly[Random().nextInt(musicOnly.size)]
        return if (music.isAlbum) {
            music.loadTracks()
            music.tracks?.firstOrNull()
        } else music
    }

    override fun getItemViewType(position: Int): Int {
        if (position >= items.size) {
            return CellType.EMPTY.ordinal
        }
        if (position == items.lastIndex && items[position] == AMLoadingRow) {
            return TYPE_LOADING
        }
        if (items.isNotEmpty() && items[position] is AMHeaderRow) {
            return CellType.HEADER.ordinal
        }
        if (items[position] is AMEmptyRow) {
            return CellType.EMPTY.ordinal
        }
        if (items[position] is AMFooterRow) {
            return CellType.FOOTER.ordinal
        }
        if ((items[position] as? AMFeaturedSpot)?.artist != null) {
            return CellType.ACCOUNT.ordinal
        }
        if (cellType !== CellType.PLAYLIST_GRID && (items[position] as? AMFeaturedSpot)?.item?.isPlaylist == true) {
            return CellType.PLAYLIST.ordinal
        }
        if (cellType !== CellType.PLAYLIST_GRID && cellType !== CellType.MUSIC_TINY && (items[position] as? AMResultItem)?.isPlaylist == true) {
            return CellType.PLAYLIST.ordinal
        }
        if (items[position] is AMArtist) {
            return CellType.ACCOUNT.ordinal
        }
        if (items[position] is AMNotification) {
            return if ((items[position] as AMNotification).type is AMNotification.NotificationType.PlaylistUpdatedBundle) CellType.NOTIFICATION_BUNDLED_PLAYLIST.ordinal else CellType.NOTIFICATION.ordinal
        }
        return cellType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return try {
            when (viewType) {
                TYPE_LOADING -> {
                    LoadMoreViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_loadingmore, parent, false))
                }
                CellType.ACCOUNT.ordinal -> {
                    AccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_account, parent, false))
                }
                CellType.MUSIC_BROWSE_SMALL.ordinal, CellType.MUSIC_BROWSE_SMALL_CHART.ordinal -> {
                    MusicBrowseSmallViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_browsemusic_small, parent, false))
                }
                CellType.MUSIC_TINY.ordinal -> {
                    MusicTinyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_music_tiny, parent, false))
                }
                CellType.PLAYLIST.ordinal -> {
                    PlaylistViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_playlist, parent, false))
                }
                CellType.PLAYLIST_GRID.ordinal -> {
                    PlaylistGridViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_playlist_grid, parent, false))
                }
                CellType.NOTIFICATION.ordinal -> {
                    NotificationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_notification, parent, false))
                }
                CellType.NOTIFICATION_BUNDLED_PLAYLIST.ordinal -> {
                    NotificationBundledPlaylistViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_notification_playlistupdate, parent, false))
                }
                CellType.FOOTER.ordinal -> {
                    FooterViewHolder(LayoutInflater.from(parent.context).inflate(footerLayoutResId!!, parent, false))
                }
                CellType.EMPTY.ordinal -> {
                    EmptyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_empty, parent, false))
                }
                CellType.HEADER.ordinal -> {
                    HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_header, parent, false))
                }
                else -> {
                    EmptyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_empty, parent, false))
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
            EmptyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_empty, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val obj = items[position]
        try {
            when (holder) {
                is AccountViewHolder -> {
                    val account = if (obj is AMFeaturedSpot) obj.artist else obj as AMArtist
                    holder.setup(
                        account = account!!,
                        featuredText = when {
                            obj is AMFeaturedSpot -> obj.getPrettyType(holder.itemView.context)
                            account.isHighlightedSearchResult && account.isVerified -> holder.itemView.context.getString(R.string.search_verified_account)
                            account.isHighlightedSearchResult && account.isTastemaker -> holder.itemView.context.getString(R.string.search_tastemaker_account)
                            account.isHighlightedSearchResult && account.isAuthenticated -> holder.itemView.context.getString(R.string.search_authenticated_account)
                            else -> null
                        },
                        featured = obj is AMFeaturedSpot,
                        showDivider = cellType !== CellType.ACCOUNT,
                        listener = listener,
                        hideActions = searchMode && showFollowBtn
                    )
                }
                is MusicBrowseSmallViewHolder -> {
                    val item = if (obj is AMFeaturedSpot) obj.item else obj as AMResultItem
                    holder.setup(
                        item!!,
                        if (obj is AMFeaturedSpot) obj.getPrettyType(holder.itemView.context) else null,
                        obj is AMFeaturedSpot,
                        showRepostInfo,
                        listener,
                        position,
                        removePaddingFromFirstPosition,
                        cellType,
                        hideStats = false,
                        hideActions = false
                    )
                }
                is MusicTinyViewHolder -> {
                    val item = items[position] as AMResultItem
                    holder.setup(item, myDownloadsMode, listener)
                }
                is PlaylistViewHolder -> {
                    val item = if (items[position] is AMResultItem) items[position] as AMResultItem else (items[position] as AMFeaturedSpot).item!!
                    holder.setup(
                        item,
                        if (obj is AMFeaturedSpot) obj.getPrettyType(holder.itemView.context) else if (item.isPlaylist && item.isVerifiedSearchResult) holder.itemView.context.getString(R.string.search_verified_playlist) else null,
                        obj is AMFeaturedSpot,
                        listener,
                        position,
                        removePaddingFromFirstPosition
                    )
                }
                is PlaylistGridViewHolder -> {
                    val item = items[position] as AMResultItem
                    holder.setup(item, position, listener)
                }
                is NotificationViewHolder -> {
                    val notification = items[position] as AMNotification
                    holder.setup(notification, position, listener)
                }
                is NotificationBundledPlaylistViewHolder -> {
                    val notification = items[position] as AMNotification
                    val bundle = notification.type as AMNotification.NotificationType.PlaylistUpdatedBundle
                    holder.setup(bundle)
                    holder.itemView.setOnClickListener { listener.onClickNotificationBundledPlaylists(bundle.playlists, notification.type) }
                }
                is FooterViewHolder -> {
                    holder.setup(listener)
                }
                is EmptyViewHolder -> {
                    holder.setup(bottomSectionHeight)
                }
                is HeaderViewHolder -> {
                    holder.setup(headerView)
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    override fun getItemCount(): Int = items.size

    val realItemsCount: Int
        get() {
            var count = items.size
            if (items.contains(AMHeaderRow)) {
                count--
            }
            if (items.contains(AMFooterRow)) {
                count--
            }
            if (items.contains(AMEmptyRow)) {
                count--
            }
            return count
        }

    fun setBottomSectionHeight(bottomSectionHeight: Int) {
        val valueChanged = this.bottomSectionHeight != bottomSectionHeight
        this.bottomSectionHeight = bottomSectionHeight
        if (valueChanged) {
            val emptyRow = AMEmptyRow
            items.indexOf(emptyRow).takeIf { it != -1 }?.let { index ->
                notifyItemChanged(index)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is MusicBrowseSmallViewHolder -> holder.cleanup()
            is PlaylistViewHolder -> holder.cleanup()
            is PlaylistGridViewHolder -> holder.cleanup()
            is MusicTinyViewHolder -> holder.cleanup()
        }
    }

    fun showFollowBtn(show: Boolean) {
        showFollowBtn = show
        notifyDataSetChanged()
    }

    companion object {
        const val TYPE_LOADING: Int = Int.MAX_VALUE
    }
}
