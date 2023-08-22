package com.audiomack.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.adapters.DataRecyclerViewAdapter
import com.audiomack.data.tracking.mixpanel.MixpanelPageNotifications
import com.audiomack.fragments.DataFragment
import com.audiomack.model.AMResultItem
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.CellType
import com.audiomack.model.MixpanelSource
import com.audiomack.utils.extensions.colorCompat
import io.reactivex.Observable

class DataNotificationUpdatedPlaylistsFragment : DataFragment(TAG) {

    private var playlists: List<AMResultItem> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(view.context.colorCompat(R.color.background_color))
    }

    override fun apiCallObservable(): APIRequestData =
        APIRequestData(Observable.just(APIResponseData(if (currentPage == 0) playlists else emptyList(), null)), null)

    override fun getCellType(): CellType {
        return CellType.PLAYLIST_GRID
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager {
        val layoutManager = GridLayoutManager(context, 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (recyclerViewAdapter.getItemViewType(position) == CellType.HEADER.ordinal || recyclerViewAdapter.getItemViewType(
                        position
                    ) == DataRecyclerViewAdapter.TYPE_LOADING
                ) {
                    2
                } else 1
            }
        }
        return layoutManager
    }

    override fun getMixpanelSource(): MixpanelSource =
        MixpanelSource(MainApplication.currentTab, MixpanelPageNotifications)

    companion object {
        private const val TAG = "DataNotificationUpdatedPlaylistsFragment"

        fun newInstance(playlists: List<AMResultItem>) = DataNotificationUpdatedPlaylistsFragment().apply {
            this.playlists = playlists
        }
    }
}
