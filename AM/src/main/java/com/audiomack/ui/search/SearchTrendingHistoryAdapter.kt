package com.audiomack.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.adapters.DataRecyclerViewAdapter
import com.audiomack.adapters.viewholders.AccountViewHolder
import com.audiomack.adapters.viewholders.MusicBrowseSmallViewHolder
import com.audiomack.adapters.viewholders.PlaylistViewHolder
import com.audiomack.model.AMArtist
import com.audiomack.model.AMNotification
import com.audiomack.model.AMResultItem
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.CellType
import com.audiomack.model.SearchType

class SearchTrendingHistoryAdapter(
    private val items: MutableList<SearchTrendingHistoryItem>,
    private val tapHandler: (String, SearchType) -> Unit,
    private val deleteHandler: (String) -> Unit,
    private val openMusicHandler: (AMResultItem) -> Unit,
    private val openArtistHandler: (AMArtist) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DataRecyclerViewAdapter.RecyclerViewListener {

    private val typeHeader = 0
    private val typeSearch = 1
    private val typeMusic = 2
    private val typeArtist = 3
    private val typePlaylist = 4

    fun updateItems(newItems: List<SearchTrendingHistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun removeItem(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            SearchTrendingHistoryItem.RecommendationsHeader, SearchTrendingHistoryItem.RecentHeader -> typeHeader
            is SearchTrendingHistoryItem.RecentSearch -> typeSearch
            is SearchTrendingHistoryItem.TrendingMusic -> if (item.music.isPlaylist) typePlaylist else typeMusic
            else -> typeArtist
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            typeHeader -> SearchHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_search_history_header, parent, false))
            typeSearch -> SearchTextViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_search_history_text, parent, false))
            typeMusic -> MusicBrowseSmallViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_browsemusic_small, parent, false))
            typeArtist -> AccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_account, parent, false))
            else -> PlaylistViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_playlist, parent, false))
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            SearchTrendingHistoryItem.RecommendationsHeader -> (holder as? SearchHeaderViewHolder)?.setup(holder.itemView.context.getString(R.string.search_recommendations))
            SearchTrendingHistoryItem.RecentHeader -> (holder as? SearchHeaderViewHolder)?.setup(holder.itemView.context.getString(R.string.search_recent))
            is SearchTrendingHistoryItem.RecentSearch -> (holder as? SearchTextViewHolder)?.setup(item.text, true, {
                tapHandler(item.text, SearchType.Recent)
            }, {
                deleteHandler(item.text)
                removeItem(holder.adapterPosition)
            })
            is SearchTrendingHistoryItem.TrendingMusic -> {
                (holder as? PlaylistViewHolder)?.setup(
                    item = item.music,
                    featuredText = null,
                    featured = false,
                    listener = this,
                    position = position,
                    hideActions = true
                )
                (holder as? MusicBrowseSmallViewHolder)?.setup(
                    item = item.music,
                    featuredText = null,
                    featured = false,
                    listener = this,
                    cellType = CellType.MUSIC_BROWSE_SMALL,
                    hideStats = false,
                    hideActions = true
                )
            }
            is SearchTrendingHistoryItem.TrendingArtist -> (holder as? AccountViewHolder)?.setup(
                account = item.artist,
                featuredText = null,
                featured = false,
                showDivider = true,
                listener = this,
                hideActions = true
            )
        }
    }

    // DataRecyclerViewAdapter.RecyclerViewListener

    override fun onClickItem(item: Any) {
        when (item) {
            is AMResultItem -> openMusicHandler(item)
            is AMArtist -> openArtistHandler(item)
        }
    }

    override fun onClickNotificationMusic(
        item: AMResultItem,
        comment: Boolean,
        type: AMNotification.NotificationType
    ) {}

    override fun onClickNotificationArtist(
        artistSlug: String,
        type: AMNotification.NotificationType
    ) {}

    override fun onClickNotificationBenchmark(
        item: AMResultItem,
        benchmark: BenchmarkModel,
        type: AMNotification.NotificationType
    ) {}

    override fun onClickNotificationBundledPlaylists(
        playlists: List<AMResultItem>,
        type: AMNotification.NotificationType
    ) {}

    override fun onClickNotificationCommentUpvote(
        music: AMResultItem,
        data: AMNotification.UpvoteCommentNotificationData,
        type: AMNotification.NotificationType
    ) {}

    override fun onScrollTo(verticalOffset: Int) {}

    override fun onClickTwoDots(item: AMResultItem) {}

    override fun onClickFollow(artist: AMArtist) {}

    override fun onStartLoadMore() {}

    override fun onClickFooter() {}

    override fun onClickDownload(item: AMResultItem) {}
}
