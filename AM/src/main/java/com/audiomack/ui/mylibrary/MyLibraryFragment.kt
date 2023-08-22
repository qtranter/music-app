package com.audiomack.ui.mylibrary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.viewModels
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.fragments.BaseTabHostFragment
import com.audiomack.fragments.DataDownloadsFragment
import com.audiomack.fragments.DataFavoritesFragment
import com.audiomack.fragments.DataFollowersFragment
import com.audiomack.fragments.DataFollowingFragment
import com.audiomack.fragments.DataPlaylistsFragment
import com.audiomack.fragments.DataUploadsFragment
import com.audiomack.fragments.EmptyFragment
import com.audiomack.ui.common.ViewPagerTabs
import com.audiomack.utils.addOnPageSelectedListener
import com.audiomack.utils.spannableStringWithImageAtTheEnd
import kotlinx.android.synthetic.main.fragment_mylibrary.avatarSmallImageView
import kotlinx.android.synthetic.main.fragment_mylibrary.backButton
import kotlinx.android.synthetic.main.fragment_mylibrary.buttonAvatarSettings
import kotlinx.android.synthetic.main.fragment_mylibrary.buttonNotifications
import kotlinx.android.synthetic.main.fragment_mylibrary.buttonSearch
import kotlinx.android.synthetic.main.fragment_mylibrary.headerView
import kotlinx.android.synthetic.main.fragment_mylibrary.ticketsBadgeView
import kotlinx.android.synthetic.main.fragment_mylibrary.tvNotificationsBadge
import kotlinx.android.synthetic.main.fragment_mylibrary.tvTopTitle
import kotlinx.android.synthetic.main.fragment_mylibrary.viewPager

class MyLibraryFragment : BaseTabHostFragment(TAG) {

    private val viewModel by viewModels<MyLibraryViewModel>(
        factoryProducer = { MyLibraryViewModelFactory(showBackButton) }
    )

    private lateinit var tabsAdapter: TabsAdapter
    private var viewPagerTabs: ViewPagerTabs? = null

    // Inputs
    private var showBackButton: Boolean = false
    private var deeplinkTab: String? = null
    private var deeplinkPlaylistsCategory: String? = null
    private var deeplinkOfflineCategory: String? = null

    private val tabs: List<String> by lazy {
        listOf(
            context?.getString(R.string.library_tab_favorites) ?: "",
            context?.getString(R.string.library_tab_offline) ?: "",
            context?.getString(R.string.library_tab_playlists) ?: "",
            context?.getString(R.string.library_tab_followers) ?: "",
            context?.getString(R.string.library_tab_following) ?: "",
            context?.getString(R.string.library_tab_uploads) ?: ""
        )
    }

    val expandedHeaderHeight: Int
        get() = currentHeaderHeight

    val collapsedHeaderHeight: Int
        get() = currentHeaderHeight

    val currentHeaderHeight: Int
        get() = headerView.height

    override val topLayoutHeight: Int
        get() = expandedHeaderHeight

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_mylibrary, container, false)
        topLayout = view.findViewById(R.id.topLayout)
        tabLayout = view.findViewById(R.id.tabLayout)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModelObservers()
        initClickListeners()

        viewPager.addOnPageSelectedListener { position ->
            childFragmentManager.fragments.getOrNull(position)?.userVisibleHint = true
        }

        viewPagerTabs = ViewPagerTabs(viewPager)
        tabLayout?.let { viewPagerTabs?.connect(it) }

        view.doOnLayout {
            tabsAdapter = TabsAdapter(childFragmentManager, tabs)
            viewPager.adapter = tabsAdapter
            tabLayout?.setupWithViewPager(viewPager)

            val pageIndex = when (deeplinkTab) {
                "downloads" -> 1
                "playlists" -> 2
                "followers" -> 3
                "following" -> 4
                "uploads" -> 5
                else -> 0
            }
            if (pageIndex != 0) {
                viewPager.setCurrentItem(pageIndex, false)
            }
            deeplinkTab = null
        }
    }

    private fun initViewModelObservers() {
        with(viewModel) {
            viewState.observe(viewLifecycleOwner) { state ->
                tvTopTitle.text = when {
                    state.userVerified -> tvTopTitle.spannableStringWithImageAtTheEnd(state.userName, R.drawable.ic_verified, 16)
                    state.userTastemaker -> tvTopTitle.spannableStringWithImageAtTheEnd(state.userName, R.drawable.ic_tastemaker, 16)
                    state.userAuthenticated -> tvTopTitle.spannableStringWithImageAtTheEnd(state.userName, R.drawable.ic_authenticated, 16)
                    else -> state.userName
                }
                PicassoImageLoader.load(context, state.userImage, avatarSmallImageView)
                tvNotificationsBadge.isVisible = state.notificationsCount > 0
                tvNotificationsBadge.text = if (state.notificationsCount < 100) state.notificationsCount.toString() else "99+"
                ticketsBadgeView.isVisible = state.ticketsBadgeVisible
                backButton.isVisible = state.backButtonVisible
            }
        }
    }

    private fun initClickListeners() {
        buttonSearch.setOnClickListener { viewModel.onSearchClick() }
        buttonNotifications.setOnClickListener { viewModel.onNotificationsClick() }
        buttonAvatarSettings.setOnClickListener { viewModel.onSettingsClick() }
        backButton.setOnClickListener { viewModel.onBackClick() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.deeplinkTab = it.getString(ARG_DEEPLINK_TAB)
            this.deeplinkPlaylistsCategory = it.getString(ARG_PLAYLISTS_CATEGORY)
            this.deeplinkOfflineCategory = it.getString(ARG_OFFLINE_CATEGORY)
            this.showBackButton = it.getBoolean(ARG_SHOW_BACK_BUTTON)
        }
    }

    override fun onDestroyView() {
        tabLayout?.let { viewPagerTabs?.remove(it) }
        super.onDestroyView()
    }

    private inner class TabsAdapter constructor(fm: FragmentManager, private val tabs: List<String>) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {

            val artistSlug = viewModel.artistSlug

            val fragment: Fragment

            when (position) {
                0 -> fragment = DataFavoritesFragment.newInstance(true, artistSlug, null)
                1 -> {
                    fragment = DataDownloadsFragment.newInstance(deeplinkOfflineCategory)
                    deeplinkOfflineCategory = null
                }
                2 -> {
                    fragment = DataPlaylistsFragment.newInstance(
                        true,
                        artistSlug,
                        null,
                        deeplinkPlaylistsCategory
                    )
                }
                3 -> fragment = DataFollowersFragment.newInstance(true, artistSlug, null)
                4 -> fragment = DataFollowingFragment.newInstance(true, artistSlug, null)
                5 -> fragment = DataUploadsFragment.newInstance(true, artistSlug, null)
                else -> fragment = EmptyFragment()
            }

            return fragment
        }

        override fun getCount() = tabs.size

        override fun getPageTitle(position: Int) = tabs[position]
    }

    companion object {
        private const val TAG = "MyLibraryFragment"
        private const val ARG_DEEPLINK_TAB = "arg_deeplink_tab"
        private const val ARG_PLAYLISTS_CATEGORY = "arg_playlists_category"
        private const val ARG_OFFLINE_CATEGORY = "arg_offline_category"
        private const val ARG_SHOW_BACK_BUTTON = "arg_show_back_button"

        fun newInstance(
            deeplinkTab: String?,
            deeplinkPlaylistsCategory: String? = null,
            deeplinkOfflineCategory: String? = null,
            showBackButton: Boolean = false
        ): MyLibraryFragment {
            return MyLibraryFragment().apply {
                arguments = bundleOf(
                    ARG_DEEPLINK_TAB to deeplinkTab,
                    ARG_PLAYLISTS_CATEGORY to deeplinkPlaylistsCategory,
                    ARG_OFFLINE_CATEGORY to deeplinkOfflineCategory,
                    ARG_SHOW_BACK_BUTTON to showBackButton
                )
            }
        }
    }
}
