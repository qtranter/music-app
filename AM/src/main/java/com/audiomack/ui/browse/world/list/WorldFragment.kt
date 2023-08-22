package com.audiomack.ui.browse.world.list

import android.os.Bundle
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.paging.LoadState
import androidx.paging.PagingData
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.WorldArticle
import com.audiomack.model.WorldPage
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.views.AMSnackbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_world.*
import kotlinx.android.synthetic.main.view_placeholder.*

class WorldFragment : TrackedFragment(R.layout.fragment_world, TAG) {

    private val viewModel: WorldViewModel by viewModels()

    private val itemClickListener: (String) -> Unit = { slugString ->
        if (slugString.isNotEmpty()) {
            viewModel.onSlugRequested(slugString)
        }
    }

    private var adapter: WorldAdapter? = null

    private val pagesAdapter = WorldHeaderPillsAdapter { page ->
        viewModel.onPageRequested(page)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initViewModelObservers()
        initAdapter()

        swipeRefreshLayout.isHapticFeedbackEnabled = true
        swipeRefreshLayout.setColorSchemeColors(swipeRefreshLayout.context.colorCompat(R.color.orange))
        swipeRefreshLayout.setOnRefreshListener {
            adapter?.retry()
            swipeRefreshLayout.isRefreshing = false
        }

        imageView.isVisible = false
        cta.isVisible = false
        tvMessage.isVisible = false

        val page = arguments?.getParcelable(FILTER_PAGE) ?: WorldPage.all
        viewModel.onPageRequested(page)
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            setupPostsEvent.observe(viewLifecycleOwner, setupPostsEventObserver)
            openPostDetailEvent.observe(viewLifecycleOwner, openPostDetailEventObserver)
            viewState.observe(viewLifecycleOwner, viewStateObserver)
            adsEnabled.observe(viewLifecycleOwner, adsEnabledObserver)
            showOfflineToastEvent.observe(viewLifecycleOwner, showOfflineToastEventObserver)
        }
    }

    private fun initAdapter() {
        adapter = WorldAdapter(onClickListener = itemClickListener)

        recyclerViewPages.setHasFixedSize(true)
        recyclerViewPages.adapter = pagesAdapter

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter?.withLoadStateFooter(
            footer = WorldArticlesLoadStateAdapter { adapter?.retry() }
        )

        addAdapterLoadStateListener()
    }

    private fun addAdapterLoadStateListener() {
        adapter?.addLoadStateListener { loadState ->
            adapter?.apply {
                if (itemCount <= 1 &&
                        !loadState.source.refresh.endOfPaginationReached &&
                        loadState.source.refresh is LoadState.NotLoading) {
                    imageView.setImageResource(R.drawable.ic_world_logo_gray)
                    tvMessage.setText(R.string.world_articles_not_found)
                }
            }

            recyclerView.isGone = loadState.source.refresh !is LoadState.NotLoading
            animationView.isVisible = loadState.source.refresh is LoadState.Loading
            val counts = adapter?.itemCount ?: 0
            imageView.isVisible = loadState.source.refresh is LoadState.Error ||
                    (counts <= 1 &&
                    !loadState.source.refresh.endOfPaginationReached &&
                    loadState.source.refresh is LoadState.NotLoading)
            tvMessage.isVisible = loadState.source.refresh is LoadState.Error ||
                    (counts <= 1 &&
                    !loadState.source.refresh.endOfPaginationReached &&
                    loadState.source.refresh is LoadState.NotLoading)
            recyclerViewPages.isGone = loadState.source.refresh !is LoadState.Error
            cta.isVisible = loadState.source.refresh is LoadState.Error

            val errorState = loadState.source.append as? LoadState.Error
                    ?: loadState.source.prepend as? LoadState.Error
                    ?: loadState.append as? LoadState.Error
                    ?: loadState.prepend as? LoadState.Error
                    ?: loadState.refresh as? LoadState.Error
            errorState?.let {
                viewModel.handleError()
            }
        }
    }

    private val viewStateObserver = Observer<WorldViewModel.ViewState> { viewState ->
        when (viewState) {
            is WorldViewModel.ViewState.Error -> {
                animationView.hide()
                imageView.setImageResource(R.drawable.ic_empty_offline)
                tvMessage.setText(R.string.noconnection_placeholder)
                cta.setText(R.string.noconnection_highlighted_placeholder)
                cta.setOnClickListener { adapter?.retry() }
            }
            is WorldViewModel.ViewState.LoadingPages -> {
                animationView.show()
            }
            is WorldViewModel.ViewState.LoadedPages -> {
                animationView.hide()
                pagesAdapter.submitList(viewState.filterItems)
                pagesAdapter.notifyDataSetChanged()
            }
        }
    }

    private val adsEnabledObserver = Observer<Boolean> { adsEnabled ->
        recyclerView.setPadding(0, 0, 0,
            if (adsEnabled) resources.getDimensionPixelOffset(R.dimen.ad_height) else 0)
    }

    private val showOfflineToastEventObserver = Observer<Void> {
        AMSnackbar.Builder(activity)
            .withTitle(getString(R.string.download_results_no_connection))
            .withSubtitle(getString(R.string.please_try_request_later))
            .withDrawable(R.drawable.ic_snackbar_connection)
            .withDuration(Snackbar.LENGTH_SHORT)
            .show()
    }

    private val setupPostsEventObserver = Observer<PagingData<WorldArticle>> { pagingData ->
        adapter?.submitData(viewLifecycleOwner.lifecycle, pagingData)
    }

    private val openPostDetailEventObserver = Observer<String> {
        (activity as? HomeActivity)?.openPostDetail(it)
    }

    companion object {
        private const val TAG = "WorldFragment"
        private const val FILTER_PAGE = "page"

        @JvmStatic
        fun newInstance(page: WorldPage? = null) =
            WorldFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(FILTER_PAGE, page)
                }
            }
    }
}
