package com.audiomack.ui.browse

import android.os.Bundle
import android.view.View
import com.audiomack.MainApplication
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.session.SessionManager
import com.audiomack.data.session.SessionManagerImpl
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelFilterGenre
import com.audiomack.data.tracking.mixpanel.MixpanelPageBrowseTrending
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.model.APIRequestData
import com.audiomack.model.EventFeaturedPostPulled
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.ui.home.HomeActivity
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DataTrendingFragment : BrowseTabFragment(TAG) {

    private val remoteVariables: RemoteVariablesProvider = RemoteVariablesProviderImpl()
    private val sessionManager: SessionManager = SessionManagerImpl
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository()

    override fun apiCallObservable(): APIRequestData? {
        return API.getInstance().getTrending(genre, currentPage, true)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventFeaturedPostPulled: EventFeaturedPostPulled) {
        injectFeaturedPosts()
    }

    override fun getMixpanelSource(): MixpanelSource =
        MixpanelSource(MainApplication.currentTab, MixpanelPageBrowseTrending, listOf(Pair(MixpanelFilterGenre, genre ?: "all")))

    override fun recyclerViewHeader(): View? {
        return super.recyclerViewHeader().also {
            if (remoteVariables.trendingBannerEnabled && sessionManager.canShowTrendingBanner) {
                header?.setupBanner(
                    remoteVariables.trendingBannerMessage,
                    {
                        remoteVariables.trendingBannerLink.takeIf { it.isNotBlank() }?.let { link ->
                            mixpanelDataSource.trackTrendingBannerClick(link)
                            (activity as? HomeActivity)?.homeViewModel?.onLinkRequested(link)
                        }
                    },
                    { sessionManager.onTrendingBannerClosed() }
                )
            }
        }
    }

    companion object {
        private const val TAG = "DataTrendingFragment"

        @JvmStatic
        fun newInstance(genre: String?): DataTrendingFragment {
            return DataTrendingFragment().apply {
                arguments = Bundle().apply {
                    putString("genre", genre)
                }
            }
        }
    }
}
