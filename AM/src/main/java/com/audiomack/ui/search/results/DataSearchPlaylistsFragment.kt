package com.audiomack.ui.search.results

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.adapters.DataRecyclerViewAdapter
import com.audiomack.data.search.filters.SearchFilters
import com.audiomack.data.tracking.mixpanel.MixpanelFilterGenre
import com.audiomack.data.tracking.mixpanel.MixpanelFilterSort
import com.audiomack.data.tracking.mixpanel.MixpanelFilterVerified
import com.audiomack.data.tracking.mixpanel.MixpanelPageSearchPlaylists
import com.audiomack.fragments.DataFragment
import com.audiomack.model.AMArtist
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.CellType
import com.audiomack.model.EventSearchFiltersChanged
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.ui.search.SearchViewModel
import com.audiomack.ui.search.filters.SearchFiltersActivity
import io.reactivex.Observable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DataSearchPlaylistsFragment : DataFragment(TAG) {

    private var tvCategory: TextView? = null

    private lateinit var searchViewModel: SearchViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchViewModel = ViewModelProvider(requireParentFragment()).get(SearchViewModel::class.java).apply {
            notifyTabsEvent.observe(viewLifecycleOwner) { changedSettings() }
        }
    }

    override fun apiCallObservable(): APIRequestData? {
        super.apiCallObservable()
        updateHeaderLabel()
        return if (!SearchFilters.query.isNullOrEmpty()) {
            // Remove any verified artists from the response since we have a grid UI on the app and can't display artists properly here.
            val requestData = API.getInstance().search(SearchFilters.query, "playlists", SearchFilters.categoryCode, SearchFilters.verifiedOnly, SearchFilters.genreCode, currentPage, true)
            val observable = requestData.observable.flatMap { apiResponseData: APIResponseData ->
                apiResponseData.objects = apiResponseData.objects.filter { it !is AMArtist }
                Observable.just(apiResponseData)
            }
            requestData.observable = observable
            requestData
        } else {
            APIRequestData(Observable.just(APIResponseData()), null)
        }
    }

    override fun getCellType(): CellType {
        return CellType.PLAYLIST_GRID
    }

    override fun getLayoutManager(): androidx.recyclerview.widget.RecyclerView.LayoutManager {
        return androidx.recyclerview.widget.GridLayoutManager(context, 2).apply {
            spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (recyclerViewAdapter.getItemViewType(position) == CellType.HEADER.ordinal ||
                        recyclerViewAdapter.getItemViewType(position) == DataRecyclerViewAdapter.TYPE_LOADING
                    ) {
                        2
                    } else 1
                }
            }
        }
    }

    override fun placeholderCustomView(): View {
        return LayoutInflater.from(context).inflate(R.layout.view_placeholder, null)
    }

    override fun configurePlaceholderView(placeholderView: View) {
        val imageView = placeholderView.findViewById<ImageView>(R.id.imageView)
        val tvMessage = placeholderView.findViewById<TextView>(R.id.tvMessage)
        val cta = placeholderView.findViewById<Button>(R.id.cta)
        imageView.visibility = View.GONE
        tvMessage.setText(R.string.search_noresults_placeholder)
        cta.visibility = View.GONE
    }

    override fun recyclerViewHeader(): View? {
        val headerView = LayoutInflater.from(context).inflate(R.layout.header_search, null)
        tvCategory = headerView.findViewById(R.id.tvTitle)
        updateHeaderLabel()
        headerView.findViewById<ImageButton>(R.id.buttonFilters).setOnClickListener {
            SearchFiltersActivity.show(activity)
        }
        return headerView
    }

    override fun getMixpanelSource(): MixpanelSource =
        MixpanelSource(
            MainApplication.currentTab, MixpanelPageSearchPlaylists, listOf(
                Pair(MixpanelFilterGenre, SearchFilters.mixpanelGenreName()),
                Pair(MixpanelFilterSort, SearchFilters.mixpanelSortName()),
                Pair(MixpanelFilterVerified, SearchFilters.mixpanelVerifiedName())
            ))

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventSearchFiltersChanged) {
        changedSettings()
    }

    private fun updateHeaderLabel() {
        tvCategory?.text = SearchFilters.humanDescription()
    }

    companion object {
        private const val TAG = "DataSearchPlaylistsFragment"

        fun newInstance() = DataSearchPlaylistsFragment()
    }
}
