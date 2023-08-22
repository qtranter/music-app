package com.audiomack.ui.search

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.viewModels
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.keyboard.KeyboardDetector
import com.audiomack.data.search.filters.SearchFilters
import com.audiomack.data.tracking.mixpanel.MixpanelPageSearchTrending
import com.audiomack.data.tracking.mixpanel.MixpanelTabSearch
import com.audiomack.fragments.BaseTabHostFragment
import com.audiomack.fragments.EmptyFragment
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.MixpanelSource
import com.audiomack.model.SearchType
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.search.results.DataSearchAlbumsFragment
import com.audiomack.ui.search.results.DataSearchArtistsFragment
import com.audiomack.ui.search.results.DataSearchMusicFragment
import com.audiomack.ui.search.results.DataSearchPlaylistsFragment
import com.audiomack.ui.search.results.DataSearchSongFragment
import com.audiomack.utils.addOnPageSelectedListener
import com.audiomack.utils.convertDpToPixel
import kotlinx.android.synthetic.main.fragment_search.*
import timber.log.Timber

class SearchFragment : BaseTabHostFragment(TAG) {

    private var tabsAdapter: TabsAdapter? = null
    private var suggestionsAdapter: SearchSuggestionsAdapter? = null
    private var trendingHistoryAdapter: SearchTrendingHistoryAdapter? = null

    private var categoryKey: String? = null
    private var query: String? = null
    private var searchType: SearchType = SearchType.Direct

    val viewModel: SearchViewModel by viewModels()

    private val tabs: List<String> by lazy {
        listOf(
            MainApplication.context!!.getString(R.string.search_tab_allmusic),
            MainApplication.context!!.getString(R.string.search_tab_playlists),
            MainApplication.context!!.getString(R.string.search_tab_songs),
            MainApplication.context!!.getString(R.string.search_tab_albums),
            MainApplication.context!!.getString(R.string.search_tab_accounts)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        topLayout = view.findViewById(R.id.topLayout)
        tabLayout = view.findViewById(R.id.tabLayout)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModelObservers()
        initClickListeners()

        trendingHistoryAdapter = SearchTrendingHistoryAdapter(
            items = mutableListOf(),
            tapHandler = { term, type -> viewModel.onSearchTapped(term, type) },
            deleteHandler = { viewModel.onDeleteRecentSearch(it) },
            openMusicHandler = { music -> viewModel.onMusicTapped(music) },
            openArtistHandler = { artist -> viewModel.onArtistTapped(artist) }
        )
        trendingRecentRecyclerView.adapter = trendingHistoryAdapter
        trendingRecentRecyclerView.setHasFixedSize(true)

        suggestionsAdapter = SearchSuggestionsAdapter { viewModel.onSearchTapped(it, SearchType.Suggestion) }
        autocompleteRecyclerView.adapter = suggestionsAdapter
        autocompleteRecyclerView.setHasFixedSize(true)

        categoryKey = SearchFilters.categoryCodes[0]

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable) {
                viewModel.onTextChanged(s.toString().trim())
            }
        })

        etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                viewModel.onSearchTapped(etSearch.text.toString().trim(), searchType)
                return@setOnKeyListener true
            }
            false
        }

        tabsAdapter = TabsAdapter(childFragmentManager, tabs)
        viewPager.adapter = tabsAdapter
        tabLayout?.setupWithViewPager(viewPager)
        viewPager.addOnPageSelectedListener { position ->
            childFragmentManager.fragments.getOrNull(position)?.userVisibleHint = true
        }

        etSearch.setText(query)
        viewModel.onSearchTapped(query, searchType)
        searchType = SearchType.Direct

        lifecycle.addObserver(KeyboardDetector(view) { state ->
            viewModel.onKeyboardVisibilityChanged(state)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.onDestroy()
        etSearch.keyListener = null
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            cancelEvent.observe(viewLifecycleOwner) {
                etSearch.setText("")
                SearchFilters.query = null
                trendingRecentRecyclerView.visibility = View.VISIBLE
                tabLayoutContainer.visibility = View.GONE
                viewPager.visibility = View.GONE
            }
            clearEvent.observe(viewLifecycleOwner) {
                etSearch.setText("")
                SearchFilters.query = null
            }
            clearButtonVisible.observe(viewLifecycleOwner) { visible ->
                buttonClear.visibility = if (visible) View.VISIBLE else View.GONE
            }
            showKeyboardEvent.observe(viewLifecycleOwner) {
                try {
                    etSearch.requestFocus()
                    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
                } catch (e: Exception) {
                    Timber.w(e)
                }
            }
            hideKeyboardEvent.observe(viewLifecycleOwner) {
                try {
                    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(etSearch.windowToken, 0)
                } catch (e: Exception) {
                    Timber.w(e)
                }
            }
            startSearchEvent.observe(viewLifecycleOwner) { (query, forceVerified) ->
                etSearch.setText(query)
                etSearch.setSelection(query.length)
                tabLayoutContainer.visibility = View.VISIBLE
                viewPager.visibility = View.VISIBLE
                trendingRecentRecyclerView.visibility = View.GONE
                autocompleteRecyclerView.visibility = View.GONE
                this@SearchFragment.query = query
                SearchFilters.query = query
                if (forceVerified) {
                    SearchFilters.verifiedOnly = true
                }
                viewModel.notifyTabs()
            }
            suggestionsEvent.observe(viewLifecycleOwner) {
                it?.let { (string, suggestions) ->
                    if (tabLayoutContainer.visibility != View.VISIBLE) {
                        suggestionsAdapter?.updateSuggestions(suggestions, string)
                        autocompleteRecyclerView.visibility =
                            if (suggestions.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
            hideSuggestionsEvent.observe(viewLifecycleOwner) {
                autocompleteRecyclerView.visibility = View.GONE
            }
            showPlaceholderEvent.observe(viewLifecycleOwner) {
                trendingRecentRecyclerView.visibility = View.VISIBLE
                tabLayoutContainer.visibility = View.GONE
                viewPager.visibility = View.GONE
            }
            notifyTrendingAdapterEvent.observe(viewLifecycleOwner) {
                trendingHistoryAdapter?.notifyDataSetChanged()
            }
            updateTrendingHistoryListEvent.observe(viewLifecycleOwner) {
                trendingHistoryAdapter?.updateItems(it)
                trendingRecentRecyclerView.layoutManager?.scrollToPosition(0)
            }
            openSongEvent.observe(viewLifecycleOwner) { (song, items) ->
                HomeActivity.instance?.homeViewModel?.onMaximizePlayerRequested(
                    MaximizePlayerData(
                        item = song,
                        items = items,
                        mixpanelSource = MixpanelSource(MixpanelTabSearch, MixpanelPageSearchTrending)
                    )
                )
            }
            openAlbumEvent.observe(viewLifecycleOwner) { album ->
                HomeActivity.instance?.openAlbum(
                    album = album,
                    externalSource = MixpanelSource(MixpanelTabSearch, MixpanelPageSearchTrending),
                    openShare = false
                )
            }
            openPlaylistEvent.observe(viewLifecycleOwner) { playlist ->
                HomeActivity.instance?.requestPlaylist(
                    id = playlist.itemId,
                    mixpanelSource = MixpanelSource(MixpanelTabSearch, MixpanelPageSearchTrending)
                )
            }
            openArtistEvent.observe(viewLifecycleOwner) { artist ->
                HomeActivity.instance?.openArtist(artist)
            }
            recyclerViewPadding.observe(viewLifecycleOwner) { padding ->
                val bottomPadding = padding.bottomPadding + if (padding.adsVisible) resources.getDimensionPixelSize(R.dimen.ad_height) else 0

                trendingRecentRecyclerView.setPadding(
                    trendingRecentRecyclerView.paddingLeft,
                    trendingRecentRecyclerView.paddingTop,
                    trendingRecentRecyclerView.paddingRight,
                    bottomPadding
                )
                autocompleteRecyclerView.setPadding(
                    autocompleteRecyclerView.paddingLeft,
                    autocompleteRecyclerView.paddingTop,
                    autocompleteRecyclerView.paddingRight,
                    bottomPadding
                )
            }
        }
    }

    private fun initClickListeners() {
        buttonCancel.setOnClickListener { viewModel.onCancelTapped() }
        buttonClear.setOnClickListener { viewModel.onClearTapped() }
    }

    fun onSearchCompleted(replacementSearch: Boolean) {
        viewModel.onSearchCompleted(replacementSearch)
    }

    override val topLayoutHeight: Int
        get() = context?.convertDpToPixel(102f) ?: 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        query = arguments?.getString(ARG_QUERY)
        searchType = (arguments?.get(ARG_SEARCH_TYPE) as? SearchType) ?: SearchType.Direct
    }

    companion object {
        private const val TAG = "SearchFragment"
        const val ARG_QUERY = "arg_query"
        const val ARG_SEARCH_TYPE = "arg_search_type"

        fun newInstance(query: String?, searchType: SearchType?) = SearchFragment().apply {
            arguments = Bundle().apply {
                query?.let { putString(ARG_QUERY, it) }
                searchType?.let { putSerializable(ARG_SEARCH_TYPE, it) }
            }
        }
    }

    private inner class TabsAdapter(
        fm: FragmentManager,
        private val tabs: List<String>
    ) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment =
            when (position) {
                0 -> DataSearchMusicFragment.newInstance()
                1 -> DataSearchPlaylistsFragment.newInstance()
                2 -> DataSearchSongFragment.newInstance()
                3 -> DataSearchAlbumsFragment.newInstance()
                4 -> DataSearchArtistsFragment.newInstance()
                else -> EmptyFragment()
            }

        override fun getCount() = this.tabs.size

        override fun getPageTitle(position: Int) = this.tabs[position]
    }
}
