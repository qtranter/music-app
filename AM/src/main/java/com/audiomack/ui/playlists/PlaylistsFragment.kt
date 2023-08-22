package com.audiomack.ui.playlists

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.audiomack.R
import com.audiomack.fragments.BaseTabHostFragment
import com.audiomack.fragments.DataPlaylistsByCategoryFragment
import com.audiomack.model.Action
import com.audiomack.model.PlaylistCategory
import com.audiomack.ui.common.ViewPagerTabs
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.settings.OptionsMenuFragment
import com.audiomack.utils.addOnPageSelectedListener
import com.audiomack.utils.convertDpToPixel
import java.util.Locale
import kotlinx.android.synthetic.main.fragment_playlists.*
import kotlinx.android.synthetic.main.view_placeholder.*
import timber.log.Timber

class PlaylistsFragment : BaseTabHostFragment(TAG) {

    private lateinit var viewModel: PlaylistsViewModel
    private var deeplinkTag: String? = null
    private var viewPagerTabs: ViewPagerTabs? = null

    @SuppressLint("CutPasteId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_playlists, container, false)
        topLayout = view.findViewById(R.id.tabLayout)
        tabLayout = view.findViewById(R.id.tabLayout)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, PlaylistsViewModelFactory(deeplinkTag)).get(PlaylistsViewModel::class.java)

        initViews()
        initClickListeners()
        initViewModelObservers()

        viewModel.downloadCategories()
    }

    private fun initViews() {
        imageView.visibility = View.GONE
        tvMessage.text = getString(R.string.noconnection_placeholder)
        cta.text = getString(R.string.noconnection_highlighted_placeholder)
    }

    private fun initClickListeners() {
        buttonAllCategories.setOnClickListener { viewModel.onAllCategoriesTapped() }
        cta.setOnClickListener { viewModel.onPlaceholderTapped() }
        viewPager.addOnPageSelectedListener { position ->
            childFragmentManager.fragments.getOrNull(position)?.userVisibleHint = true
        }
        viewPagerTabs = ViewPagerTabs(viewPager)
        tabLayout?.let { viewPagerTabs?.connect(it) }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            openMenuEvent.observe(viewLifecycleOwner, openMenuObserver)
            setupPagerEvent.observe(viewLifecycleOwner, setupPagerObserver)
            loaderVisible.observe(viewLifecycleOwner, loaderVisibleObserver)
            contentVisible.observe(viewLifecycleOwner, contentVisibleObserver)
            placeholderVisible.observe(viewLifecycleOwner, placeholderVisibleObserver)
        }
    }

    private val openMenuObserver: Observer<List<PlaylistCategory>> = Observer { categories ->
        val actions = mutableListOf<Action>()
        categories.indices.forEach { i ->
            actions.add(
                Action(
                    categories[i].title,
                    i == viewPager.currentItem,
                    object : Action.ActionListener {
                        override fun onActionExecuted() {
                            try {
                                (activity as? HomeActivity)?.popFragment()
                                viewPager.currentItem = i
                            } catch (e: Exception) {
                                Timber.w(e)
                            }
                        }
                    })
            )
        }
        try {
            (activity as? HomeActivity)?.openOptionsFragment(OptionsMenuFragment.newInstance(actions))
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private val setupPagerObserver: Observer<List<PlaylistCategory>> = Observer { categories ->
        val tabsAdapter = TabsAdapter(childFragmentManager, categories)
        viewPager.adapter = tabsAdapter
        tabLayout?.setupWithViewPager(viewPager)
    }

    private val loaderVisibleObserver: Observer<Boolean> = Observer {
        if (it) animationView.show()
        else animationView.hide()
    }

    private val contentVisibleObserver: Observer<Boolean> = Observer { visible ->
        tabLayoutContainer.visibility = if (visible) View.VISIBLE else View.GONE
        viewPager.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val placeholderVisibleObserver: Observer<Boolean> = Observer { visible ->
        viewPlaceholder.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override val topLayoutHeight: Int
        get() = context?.convertDpToPixel(48f) ?: 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deeplinkTag = arguments?.getString("deeplinkTag")
    }

    override fun onDestroyView() {
        tabLayout?.let { viewPagerTabs?.remove(it) }
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "PlaylistsFragment"

        @JvmStatic
        fun newInstance(deeplinkTag: String?): PlaylistsFragment {
            return PlaylistsFragment().apply {
                arguments = Bundle().apply {
                    putString("deeplinkTag", deeplinkTag)
                }
            }
        }
    }

    private inner class TabsAdapter internal constructor(fm: FragmentManager, var tabs: List<PlaylistCategory>) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return DataPlaylistsByCategoryFragment.newInstance(tabs[position])
        }

        override fun getCount(): Int {
            return tabs.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return tabs[position].title.toUpperCase(Locale.US)
        }
    }
}
