package com.audiomack.ui.feed.suggested

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMArtist
import com.audiomack.model.LoginSignupSource
import com.audiomack.ui.feed.LayoutType
import com.audiomack.ui.feed.SuggestedAccountCardItem
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.groupie.InfiniteScrollListener
import com.audiomack.utils.groupie.LoadingItem
import com.audiomack.utils.lastBackStackEntry
import com.audiomack.utils.openUrlInAudiomack
import com.audiomack.utils.showFollowedToast
import com.audiomack.utils.showLoggedOutAlert
import com.audiomack.utils.showOfflineAlert
import com.xwray.groupie.Group
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.groupiex.plusAssign
import kotlinx.android.synthetic.main.fragment_suggested_accounts.recyclerViewSuggestedAccounts
import kotlinx.android.synthetic.main.fragment_suggested_accounts.toolbar
import kotlinx.android.synthetic.main.toolbar.homeAsUp
import kotlinx.android.synthetic.main.toolbar.tvTitle

class SuggestedAccountsFragment : TrackedFragment(R.layout.fragment_suggested_accounts, TAG) {

    private val suggestedAccountsViewModel by viewModels<SuggestedAccountsViewModel>()
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()
    private val accountsSection = Section()
    private lateinit var groupLayoutManager: GridLayoutManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        activity?.supportFragmentManager?.addOnBackStackChangedListener(backStackListener)
    }

    override fun onDestroyView() {
        resetStatusBar()
        activity?.supportFragmentManager?.removeOnBackStackChangedListener(backStackListener)
        super.onDestroyView()
    }

    private val backStackListener = FragmentManager.OnBackStackChangedListener {
        if (activity?.supportFragmentManager?.lastBackStackEntry()?.name == TAG) {
            customiseStatusBar()
        } else {
            resetStatusBar()
        }
    }

    private fun customiseStatusBar() {
        activity?.window?.statusBarColor = requireContext().colorCompat(R.color.toolbar_bg)
    }

    private fun resetStatusBar() {
        activity?.window?.statusBarColor = Color.TRANSPARENT
    }

    private fun initViews() {
        initToolbar()
        initGroupieRecyclerView()
    }

    private fun initGroupieRecyclerView() {
        groupLayoutManager = GridLayoutManager(context, 3).apply {
            spanSizeLookup = groupAdapter.spanSizeLookup
        }

        recyclerViewSuggestedAccounts.apply {
            layoutManager = groupLayoutManager
            adapter = groupAdapter
            addOnScrollListener(object : InfiniteScrollListener(groupLayoutManager) {
                override fun onLoadMore(currentPage: Int) {
                    if (suggestedAccountsViewModel.hasMoreItems) {
                        suggestedAccountsViewModel.loadMore()
                    }
                }
            })
        }

        val allGroups = mutableListOf<Group>()
        allGroups += accountsSection
        groupAdapter.updateAsync(allGroups)

        recyclerViewSuggestedAccounts.setPadding(
            recyclerViewSuggestedAccounts.paddingStart,
            recyclerViewSuggestedAccounts.paddingTop,
            recyclerViewSuggestedAccounts.paddingEnd,
            if (suggestedAccountsViewModel.adsVisible) resources.getDimensionPixelOffset(R.dimen.ad_height) else 0)
    }

    private fun initToolbar() {
        with(toolbar) {
            setBackgroundColor(requireContext().colorCompat(R.color.toolbar_bg))
            homeAsUp.setImageDrawable(requireContext().drawableCompat(R.drawable.ic_back_button))
            homeAsUp.setOnClickListener { requireActivity().onBackPressed() }
            tvTitle.text =
                getString(R.string.feed_suggested_accounts).toUpperCase(java.util.Locale.getDefault())
        }
    }

    private fun initObservers() {
        with(suggestedAccountsViewModel) {
            suggestedAccounts.observe(viewLifecycleOwner, suggestedAccountsObserver)

            accountFollowedEvent.observe(viewLifecycleOwner) { followedItem ->
                accountsSection.groups.filterIsInstance<SuggestedAccountCardItem>().firstOrNull {
                    followedItem.artistId == it.artist.artistId
                }?.let { accountsSection.remove(it) }
            }

            reloadEvent.observe(viewLifecycleOwner) {
                accountsSection.clear()
            }

            notifyFollowToastEvent.observe(viewLifecycleOwner) {
                showFollowedToast(it)
            }

            offlineAlertEvent.observe(viewLifecycleOwner) {
                this@SuggestedAccountsFragment.showOfflineAlert()
            }

            loggedOutAlertEvent.observe(viewLifecycleOwner) { loginSignupSource: LoginSignupSource? ->
                loginSignupSource?.let {
                    this@SuggestedAccountsFragment.showLoggedOutAlert(it)
                }
            }
        }
    }

    private val suggestedAccountsObserver = Observer<List<AMArtist>> {
        if (accountsSection.groups.isNotEmpty()) {
            if (accountsSection.getItem(accountsSection.itemCount - 1) is LoadingItem) {
                accountsSection.remove(accountsSection.getItem(accountsSection.itemCount - 1))
            }
        }

        accountsSection += it.map { artist ->
            SuggestedAccountCardItem(
                artist,
                LayoutType.Grid,
                onFollowTapped = { suggestedAccountsViewModel.onFollowTapped(artist) },
                onItemTapped = {
                    artist.urlSlug?.let {
                        context?.openUrlInAudiomack("audiomack://artist/$it")
                    }
                }
            )
        }
        if (suggestedAccountsViewModel.hasMoreItems) {
            accountsSection.add(LoadingItem(LoadingItem.LoadingItemType.GRID) {
                suggestedAccountsViewModel.loadMore()
            })
        }
    }

    companion object {
        const val TAG = "SuggestedAccountsFragment"
        fun newInstance() = SuggestedAccountsFragment()
    }
}
