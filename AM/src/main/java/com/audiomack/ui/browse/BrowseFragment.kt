package com.audiomack.ui.browse

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.tracking.mixpanel.MixpanelPageBrowseRecentlyAdded
import com.audiomack.data.tracking.mixpanel.MixpanelPageBrowseTopAlbums
import com.audiomack.data.tracking.mixpanel.MixpanelPageBrowseTopSongs
import com.audiomack.data.tracking.mixpanel.MixpanelPageBrowseTrending
import com.audiomack.fragments.BaseTabHostFragment
import com.audiomack.fragments.EmptyFragment
import com.audiomack.model.AMGenre
import com.audiomack.model.WorldPage
import com.audiomack.ui.browse.world.list.WorldFragment
import com.audiomack.ui.common.ViewPagerTabs
import com.audiomack.utils.addOnPageSelectedListener
import com.audiomack.utils.convertDpToPixel
import kotlinx.android.synthetic.main.fragment_browse.*

class BrowseFragment : BaseTabHostFragment(TAG) {

    private var deeplinkTab: String? = null
    private var genreKey: String? = null
    private var deeplinkWorldPage: WorldPage? = null

    private var viewPagerTabs: ViewPagerTabs? = null

    private val tabs: List<String> by lazy {
        listOf(
            MainApplication.context?.getString(R.string.browse_tab_world) ?: "",
            MainApplication.context?.getString(R.string.browse_tab_trending) ?: "",
            MainApplication.context?.getString(R.string.browse_tab_topsongs) ?: "",
            MainApplication.context?.getString(R.string.browse_tab_topalbums) ?: "",
            MainApplication.context?.getString(R.string.browse_tab_recentlyadded) ?: ""
        )
    }

    override val topLayoutHeight: Int
        get() = context?.convertDpToPixel(48f) ?: 0

    @SuppressLint("CutPasteId")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_browse, container, false)
        topLayout = view.findViewById(R.id.tabLayout)
        tabLayout = view.findViewById(R.id.tabLayout)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        genreKey = genreKey ?: AMGenre.All.apiValue()

        val tabsAdapter = TabsAdapter(childFragmentManager, tabs)
        viewPager.adapter = tabsAdapter
        tabLayout?.setupWithViewPager(viewPager)
        viewPagerTabs = ViewPagerTabs(viewPager)
        tabLayout?.let { viewPagerTabs?.connect(it) }
        viewPager.addOnPageSelectedListener { position ->
            childFragmentManager.fragments.getOrNull(position)?.userVisibleHint = true
        }

        when (deeplinkTab) {
            "world" -> viewPager.setCurrentItem(0, false)
            "trending" -> viewPager.setCurrentItem(1, false)
            "songs" -> viewPager.setCurrentItem(2, false)
            "albums" -> viewPager.setCurrentItem(3, false)
            "recent" -> viewPager.setCurrentItem(4, false)
        }
    }

    private inner class TabsAdapter constructor(fm: FragmentManager, private val tabs: List<String>) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> WorldFragment.newInstance(deeplinkWorldPage)
                1 -> DataTrendingFragment.newInstance(genreKey)
                2 -> DataTopSongsFragment.newInstance(genreKey)
                3 -> DataTopAlbumsFragment.newInstance(genreKey)
                4 -> DataRecentlyAddedFragment.newInstance(genreKey)
                else -> EmptyFragment()
            }
        }

        override fun getCount(): Int {
            return this.tabs.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return this.tabs[position]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.deeplinkTab = it.getString(ARGS_DEEPLINK_TAB)
            this.genreKey = it.getString(ARGS_GENRE)
            this.deeplinkWorldPage = it.getParcelable(ARGS_DEEPLINK_WORLD_PAGE)
        }
    }

    override fun onDestroyView() {
        tabLayout?.let { viewPagerTabs?.remove(it) }
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "BrowseFragment"
        const val ARGS_DEEPLINK_TAB = "args_deeplink_tab"
        const val ARGS_GENRE = "args_genre"
        const val ARGS_DEEPLINK_WORLD_PAGE = "args_world_page"

        val mixpanelPages by lazy {
            listOf(
                MixpanelPageBrowseTrending,
                MixpanelPageBrowseTopSongs,
                MixpanelPageBrowseTopAlbums,
                MixpanelPageBrowseRecentlyAdded
            )
        }

        @JvmStatic
        fun newInstance(deeplinkTab: String?, genreKey: String?, worldPage: WorldPage?): BrowseFragment {
            return BrowseFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGS_DEEPLINK_TAB, deeplinkTab)
                    putString(ARGS_GENRE, genreKey)
                    putParcelable(ARGS_DEEPLINK_WORLD_PAGE, worldPage)
                }
            }
        }
    }
}
