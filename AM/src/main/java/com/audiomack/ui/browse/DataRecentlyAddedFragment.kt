package com.audiomack.ui.browse

import android.os.Bundle
import com.audiomack.MainApplication
import com.audiomack.data.tracking.mixpanel.MixpanelFilterGenre
import com.audiomack.data.tracking.mixpanel.MixpanelPageBrowseRecentlyAdded
import com.audiomack.model.APIRequestData
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API

class DataRecentlyAddedFragment : BrowseTabFragment(TAG) {

    override fun apiCallObservable(): APIRequestData? {
        return API.getInstance().getRecent(genre, currentPage, true)
    }

    override fun getMixpanelSource(): MixpanelSource =
        MixpanelSource(MainApplication.currentTab, MixpanelPageBrowseRecentlyAdded, listOf(Pair(MixpanelFilterGenre, genre ?: "all")))

    companion object {
        private const val TAG = "DataRecentlyAddedFragment"

        @JvmStatic
        fun newInstance(genre: String?): DataRecentlyAddedFragment {
            return DataRecentlyAddedFragment().apply {
                arguments = Bundle().apply {
                    putString("genre", genre)
                }
            }
        }
    }
}
