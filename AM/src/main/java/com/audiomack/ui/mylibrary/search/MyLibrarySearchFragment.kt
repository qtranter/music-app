package com.audiomack.ui.mylibrary.search

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.fragments.BaseTabHostFragment
import com.audiomack.fragments.DataMyLibrarySearchDownloadsFragment
import com.audiomack.fragments.DataMyLibrarySearchFavoritesFragment
import com.audiomack.fragments.DataMyLibrarySearchPlaylistsFragment
import com.audiomack.fragments.DataMyLibrarySearchUploadsFragment
import com.audiomack.fragments.EmptyFragment
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.spannableStringWithImageAtTheEnd
import kotlinx.android.synthetic.main.fragment_mylibrarysearch.*

class MyLibrarySearchFragment : BaseTabHostFragment(TAG) {

    private val viewModel: MyLibrarySearchViewModel by viewModels()
    private lateinit var tabsAdapter: TabsAdapter

    private val tabs: List<String> = listOf(
        MainApplication.context!!.getString(R.string.library_search_tab_favorites),
        MainApplication.context!!.getString(R.string.library_search_tab_offline),
        MainApplication.context!!.getString(R.string.library_search_tab_playlists),
        MainApplication.context!!.getString(R.string.library_search_tab_uploads)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_mylibrarysearch, container, false)
        topLayout = view.findViewById(R.id.topLayout)
        tabLayout = view.findViewById(R.id.tabLayout)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModelObservers()
        initListeners()
        initViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard()
        etSearch.keyListener = null
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            artistName.observe(viewLifecycleOwner, Observer { artistWithBadge ->
                tvTopTitle.text = when {
                    artistWithBadge.verified -> tvTopTitle.spannableStringWithImageAtTheEnd(artistWithBadge.name, R.drawable.ic_verified, 16)
                    artistWithBadge.tastemaker -> tvTopTitle.spannableStringWithImageAtTheEnd(artistWithBadge.name, R.drawable.ic_tastemaker, 16)
                    artistWithBadge.authenticated -> tvTopTitle.spannableStringWithImageAtTheEnd(artistWithBadge.name, R.drawable.ic_authenticated, 16)
                    else -> artistWithBadge.name
                }
            })
            clearSearchVisible.observe(viewLifecycleOwner, Observer { clearVisible ->
                buttonClearSearch.visibility = if (clearVisible) View.VISIBLE else View.GONE
            })
            closeEvent.observe(viewLifecycleOwner, Observer {
                activity?.onBackPressed()
            })
            clearSearchbarEvent.observe(viewLifecycleOwner, Observer {
                etSearch.setText("")
            })
            showKeyboardEvent.observe(viewLifecycleOwner, Observer {
                showKeyboard()
            })
            hideKeyboardEvent.observe(viewLifecycleOwner, Observer {
                hideKeyboard()
            })
        }
    }

    private fun initViews() {
        tabsAdapter = TabsAdapter(childFragmentManager, tabs)
        viewPager.adapter = tabsAdapter
        tabLayout?.setupWithViewPager(viewPager)
    }

    private fun initListeners() {
        leftButton.setOnClickListener { viewModel.onBackTapped() }
        buttonCancel.setOnClickListener { viewModel.onCancelTapped() }
        buttonClearSearch.setOnClickListener { viewModel.onClearTapped() }

        etSearch.doAfterTextChanged { viewModel.onSearchTextChanged(it.toString()) }
        etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                viewModel.onSearchClicked(etSearch.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun showKeyboard() {
        etSearch.requestFocus()
        (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.toggleSoftInputFromWindow(etSearch.windowToken, 0, 0)
    }

    private fun hideKeyboard() {
        (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    override val topLayoutHeight: Int
        get() = context?.convertDpToPixel(146f) ?: 0

    private inner class TabsAdapter internal constructor(
        fm: FragmentManager,
        private val tabs: List<String>
    ) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            val searchTerm = viewModel.query ?: ""
            return when (position) {
                0 -> DataMyLibrarySearchFavoritesFragment.newInstance(searchTerm)
                1 -> DataMyLibrarySearchDownloadsFragment.newInstance(searchTerm)
                2 -> DataMyLibrarySearchPlaylistsFragment.newInstance(searchTerm)
                3 -> DataMyLibrarySearchUploadsFragment.newInstance(searchTerm)
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

    companion object {
        const val TAG = "MyLibrarySearchFragment"
        fun newInstance() = MyLibrarySearchFragment()
    }
}
