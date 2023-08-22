package com.audiomack.ui.feed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.audiomack.R
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.CellType
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.slideupmenu.music.SlideUpMenuMusicFragment.Companion.newInstance
import com.audiomack.usecases.download.setupDownloadHandler
import com.audiomack.usecases.favorite.setupFavoriteHandler
import com.audiomack.utils.askFollowNotificationPermissions
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.groupie.CarouselItem
import com.audiomack.utils.groupie.LoadingItem
import com.audiomack.utils.groupie.ViewAllHeaderItem
import com.audiomack.utils.openUrlInAudiomack
import com.audiomack.utils.showFollowedToast
import com.audiomack.utils.showLoggedOutAlert
import com.xwray.groupie.Group
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.groupiex.plusAssign
import java.util.Locale
import kotlinx.android.synthetic.main.fragment_feed.recyclerView
import kotlinx.android.synthetic.main.fragment_feed.toolbar
import kotlinx.android.synthetic.main.toolbar.view.homeAsUp
import kotlinx.android.synthetic.main.toolbar.view.tvTitle

class FeedFragment : TrackedFragment(R.layout.fragment_feed, TAG) {

    private val feedViewModel by viewModels<FeedViewModel>()
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()
    private val suggestedAccountsAdapter = GroupAdapter<GroupieViewHolder>()
    private val socialFeedSection = Section()
    private lateinit var groupLayoutManager: GridLayoutManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
    }

    override fun onDestroyView() {
        recyclerView.adapter = null
        super.onDestroyView()
    }

    private fun initViews() {
        initToolbar()
        initGroupieRecyclerView()
    }

    private fun initGroupieRecyclerView() {
        groupAdapter.spanCount = 4

        groupLayoutManager = GridLayoutManager(requireContext(), groupAdapter.spanCount).apply {
            spanSizeLookup = groupAdapter.spanSizeLookup
        }

        recyclerView.apply {
            layoutManager = groupLayoutManager
            adapter = groupAdapter
            recyclerView.setPadding(
                0, 0, 0,
                if (feedViewModel.adsVisible) resources.getDimensionPixelSize(R.dimen.ad_height) else 0
            )
        }

        val allGroups = mutableListOf<Group>()

        allGroups += Section().apply {
            setHeader(
                ViewAllHeaderItem(
                    R.string.feed_suggested_accounts,
                    onViewAllClick = { (activity as? HomeActivity)?.openSuggestedAccounts() }
                )
            )
            add(CarouselItem(suggestedAccountsAdapter))
        }

        allGroups += socialFeedSection

        groupAdapter.updateAsync(allGroups)
    }

    private fun initObservers() {
        setupFavoriteHandler(feedViewModel)
        setupDownloadHandler(feedViewModel) {
            feedViewModel.onPlaylistSyncConfirmed(it)
        }

        with(feedViewModel) {
            suggestedAccounts.observe(viewLifecycleOwner, suggestedAccountsObserver)

            loggedOutAlertEvent.observe(viewLifecycleOwner) { source ->
                showLoggedOutAlert(source)
            }

            feedItems.observe(viewLifecycleOwner, feedItemsObserver)

            reloadFeedEvent.observe(viewLifecycleOwner) {
                socialFeedSection.clear()
            }

            feedPlaceHolderVisibilityEvent.observe(viewLifecycleOwner) { visible ->
                if (visible) {
                    socialFeedSection.setPlaceholder(TimeLinePlaceHolderItem())
                } else {
                    socialFeedSection.removePlaceholder()
                }
            }

            optionsFragmentEvent.observe(viewLifecycleOwner) {
                (activity as? HomeActivity?)?.openOptionsFragment(
                    newInstance(
                        it,
                        feedViewModel.feedMixPanelSource,
                        removeFromDownloadsEnabled = false,
                        removeFromQueueEnabled = false,
                        removeFromQueueIndex = null
                    )
                )
            }

            notifyFollowToastEvent.observe(viewLifecycleOwner) {
                showFollowedToast(it)
            }

            openMusicEvent.observe(viewLifecycleOwner) { data ->
                (activity as? HomeActivity)?.homeViewModel?.openMusic(data)
            }

            songChangeEvent.observe(viewLifecycleOwner) { playingItemId ->
                socialFeedSection.groups.filterIsInstance<BrowseMusicSmallCardItem>().forEach {
                    it.currentlyPlaying = it.item.itemId == playingItemId
                }
                socialFeedSection.notifyChanged()
            }

            // Download events
            downloadItemEvent.observe(viewLifecycleOwner) { downloadItemId ->
                socialFeedSection.groups.filterIsInstance<BrowseMusicSmallCardItem>().filter {
                    it.item.itemId == downloadItemId
                }.map {
                    socialFeedSection.notifyItemChanged(socialFeedSection.getPosition(it))
                }
            }

            promptNotificationPermissionEvent.observe(
                viewLifecycleOwner,
                { redirect: PermissionRedirect ->
                    askFollowNotificationPermissions(redirect)
                })

            updateUIEvent.observe(viewLifecycleOwner) {
                (activity as? HomeActivity?)?.homeViewModel?.updateUI(it)
            }
        }
    }

    private val suggestedAccountsObserver = Observer<List<AMArtist>> { artists ->
        suggestedAccountsAdapter.clear()
        suggestedAccountsAdapter += artists.map { artist ->
            SuggestedAccountCardItem(
                artist,
                LayoutType.Horizontal,
                onFollowTapped = { feedViewModel.onFollowTapped(artist) },
                onItemTapped = {
                    artist.urlSlug?.let {
                        context?.openUrlInAudiomack("audiomack://artist/$it")
                    }
                }
            )
        }
        if (feedViewModel.hasMoreSuggestedAccounts) {
            suggestedAccountsAdapter += LoadingItem(LoadingItem.LoadingItemType.GRID) {
                feedViewModel.loadMoreSuggestedAccounts()
            }
        }
    }

    private val feedItemsObserver = Observer<List<AMResultItem>> {
        if (socialFeedSection.groups.filterIsInstance<TimeLineHeaderItem>().isEmpty() && it.isNotEmpty()) {
            socialFeedSection.setHeader(TimeLineHeaderItem(
                feedViewModel.isLoggedIn,
                feedViewModel.excludeReUps
            ) { excludeReUps ->
                feedViewModel.excludeReUps = excludeReUps
            })
        }

        if (socialFeedSection.itemCount > 0 && socialFeedSection.getItem(socialFeedSection.itemCount - 1) is LoadingItem) {
            socialFeedSection.remove(socialFeedSection.getItem(socialFeedSection.itemCount - 1))
        }

        val listener = object : BrowseMusicSmallCardItem.SocialFeedCardItemListener {
            override fun onClickTwoDots(item: AMResultItem) {
                feedViewModel.onClickTwoDots(item)
            }

            override fun onClickDownload(item: AMResultItem) {
                feedViewModel.onClickDownload(item)
            }

            override fun onClickItem(item: AMResultItem) {
                feedViewModel.onClickItem(item)
            }
        }

        val socialFeedItems = mutableListOf<Group>()
        socialFeedItems += it.map { resultItem ->
            BrowseMusicSmallCardItem(
                item = resultItem,
                featuredText = null,
                listener = listener,
                cellType = CellType.MUSIC_BROWSE_SMALL
            )
        }
        socialFeedSection.addAll(socialFeedItems)

        if (socialFeedItems.isNotEmpty()) {
            socialFeedSection.add(LoadingItem { feedViewModel.loadMoreFeedItems() })
        }
    }

    private fun initToolbar() {
        with(toolbar) {
            homeAsUp.setImageDrawable(requireContext().drawableCompat(R.drawable.ic_logo_toolbar))
            tvTitle.text = getString(R.string.home_tab_feed).toUpperCase(Locale.getDefault())
        }
    }

    companion object {
        private const val TAG = "FeedFragment"
        fun newInstance() = FeedFragment()
    }
}
