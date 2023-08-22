package com.audiomack.ui.browse

import android.os.Bundle
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.tracking.mixpanel.MixpanelFilterGenre
import com.audiomack.data.tracking.mixpanel.MixpanelFilterPeriod
import com.audiomack.data.tracking.mixpanel.MixpanelFilterPeriod_Weekly
import com.audiomack.data.tracking.mixpanel.MixpanelPageBrowseTopSongs
import com.audiomack.model.AMGenre
import com.audiomack.model.AMPeriod
import com.audiomack.model.APIRequestData
import com.audiomack.model.CellType
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.ui.filter.FilterData
import com.audiomack.ui.filter.FilterSection
import com.audiomack.ui.filter.FilterSelection

class DataTopSongsFragment : BrowseTabFragment(TAG) {

    override fun apiCallObservable(): APIRequestData? {
        return API.getInstance().getItems(genre, "songs", AMPeriod.Week.apiValue(), currentPage, true)
    }

    override fun getCellType(): CellType = CellType.MUSIC_BROWSE_SMALL_CHART

    override fun getFilterData() = FilterData(
        this::class.java.simpleName,
        getString(R.string.filters_title_chart),
        listOf(FilterSection.Genre),
        FilterSelection(
            AMGenre.fromApiValue(genre),
            AMPeriod.Week
        ),
        listOf(AMGenre.Rock)
    )

    override fun getMixpanelSource(): MixpanelSource {
        return MixpanelSource(
            MainApplication.currentTab, MixpanelPageBrowseTopSongs, listOf(
                Pair(MixpanelFilterGenre, genre ?: "all"),
                Pair(MixpanelFilterPeriod, MixpanelFilterPeriod_Weekly)
            ))
    }

    companion object {
        private const val TAG = "DataTopSongsFragment"

        @JvmStatic
        fun newInstance(genre: String?): DataTopSongsFragment {
            return DataTopSongsFragment().apply {
                arguments = Bundle().apply {
                    putString("genre", genre)
                }
            }
        }
    }
}
