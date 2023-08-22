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
import com.audiomack.data.search.filters.SearchFilters
import com.audiomack.data.tracking.mixpanel.MixpanelFilterGenre
import com.audiomack.data.tracking.mixpanel.MixpanelFilterSort
import com.audiomack.data.tracking.mixpanel.MixpanelFilterVerified
import com.audiomack.data.tracking.mixpanel.MixpanelPageSearchSongs
import com.audiomack.fragments.DataFragment
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

class DataSearchSongFragment : DataFragment(TAG) {

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
            API.getInstance().search(SearchFilters.query, "songs", SearchFilters.categoryCode, SearchFilters.verifiedOnly, SearchFilters.genreCode, currentPage, true)
        } else {
            APIRequestData(Observable.just(APIResponseData()), null)
        }
    }

    override fun getCellType(): CellType {
        return CellType.MUSIC_BROWSE_SMALL
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
            MainApplication.currentTab, MixpanelPageSearchSongs, listOf(
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
        private const val TAG = "DataSearchSongFragment"

        fun newInstance() = DataSearchSongFragment()
    }
}
