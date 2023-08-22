package com.audiomack.ui.browse

import android.os.Bundle
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.tracking.mixpanel.MixpanelFilterGenre
import com.audiomack.data.tracking.mixpanel.MixpanelFilterPeriod
import com.audiomack.data.tracking.mixpanel.MixpanelFilterPeriod_Weekly
import com.audiomack.data.tracking.mixpanel.MixpanelPageBrowseTopAlbums
import com.audiomack.model.AMGenre
import com.audiomack.model.AMPeriod
import com.audiomack.model.APIRequestData
import com.audiomack.model.CellType
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.ui.filter.FilterData
import com.audiomack.ui.filter.FilterSection
import com.audiomack.ui.filter.FilterSelection

class DataTopAlbumsFragment : BrowseTabFragment(TAG) {

    override fun apiCallObservable(): APIRequestData? {
        return API.getInstance().getItems(genre, "albums", AMPeriod.Week.apiValue(), currentPage, true)
    }

    override fun getCellType(): CellType = CellType.MUSIC_BROWSE_SMALL_CHART

    override fun getFilterData(): FilterData {
        if (this.genre == AMGenre.Podcast.apiValue()) {
            this.genre = AMGenre.All.apiValue()
        }
        return FilterData(
            this::class.java.simpleName,
            getString(R.string.filters_title_chart),
            listOf(FilterSection.Genre),
            FilterSelection(
                AMGenre.fromApiValue(genre),
                AMPeriod.Week
            ),
            listOf(AMGenre.Rock, AMGenre.Podcast)
        )
    }

    override fun getMixpanelSource(): MixpanelSource {
        return MixpanelSource(
            MainApplication.currentTab, MixpanelPageBrowseTopAlbums, listOf(
                Pair(MixpanelFilterGenre, genre ?: "all"),
                Pair(MixpanelFilterPeriod, MixpanelFilterPeriod_Weekly)
            ))
    }

    companion object {
        private const val TAG = "DataTopAlbumsFragment"

        @JvmStatic
        fun newInstance(genre: String?): DataTopAlbumsFragment {
            return DataTopAlbumsFragment().apply {
                arguments = Bundle().apply {
                    putString("genre", genre)
                }
            }
        }
    }
}
