package com.audiomack.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.adapters.DataRecyclerViewAdapter
import com.audiomack.model.APIRequestData
import com.audiomack.model.CellType
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PlaylistCategory
import com.audiomack.network.API
import com.audiomack.network.playlistsForCategory
import com.audiomack.utils.convertDpToPixel

class DataPlaylistsByCategoryFragment : DataFragment(TAG) {

    private var playlistCategory: PlaylistCategory = PlaylistCategory("", "", "")

    override fun apiCallObservable(): APIRequestData? {
        super.apiCallObservable()
        return API.getInstance().playlistsForCategory(playlistCategory.slug, currentPage, true)
    }

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

    override fun placeholderCustomView(): View {
        return LayoutInflater.from(context).inflate(R.layout.view_placeholder, null)
    }

    override fun configurePlaceholderView(placeholderView: View) {
        val imageView = placeholderView.findViewById<ImageView>(R.id.imageView)
        placeholderView.findViewById<TextView>(R.id.tvMessage).setText(R.string.playlists_noresults_placeholder)
        placeholderView.findViewById<Button>(R.id.cta).visibility = View.GONE
        imageView.setImageResource(R.drawable.ic_empty_playlists)
    }

    override fun additionalTopPadding(): Int {
        return context?.convertDpToPixel(10f) ?: 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (arguments?.getParcelable("playlistCategory") as? PlaylistCategory)?.let {
            playlistCategory = it
        }
    }

    override fun getMixpanelSource(): MixpanelSource =
        MixpanelSource(MainApplication.currentTab, "Playlists - ${playlistCategory.title}")

    companion object {
        private const val TAG = "DataPlaylistsByCategory"

        fun newInstance(playlistCategory: PlaylistCategory): DataPlaylistsByCategoryFragment {
            return DataPlaylistsByCategoryFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("playlistCategory", playlistCategory)
                }
            }
        }
    }
}
