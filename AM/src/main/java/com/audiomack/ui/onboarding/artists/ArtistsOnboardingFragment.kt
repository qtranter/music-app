package com.audiomack.ui.onboarding.artists

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.views.AMProgressHUD
import kotlinx.android.synthetic.main.fragment_artists_onboarding.animationView
import kotlinx.android.synthetic.main.fragment_artists_onboarding.buttonClose
import kotlinx.android.synthetic.main.fragment_artists_onboarding.buttonListenNow
import kotlinx.android.synthetic.main.fragment_artists_onboarding.recyclerView
import kotlinx.android.synthetic.main.fragment_artists_onboarding.swipeRefreshLayout

class ArtistsOnboardingFragment : TrackedFragment(R.layout.fragment_artists_onboarding, TAG) {

    private val viewModel: ArtistsOnboardingViewModel by viewModels()
    private lateinit var adapter: ArtistsOnboardingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.closeEvent.observe(viewLifecycleOwner) {
            activity?.onBackPressed()
        }
        viewModel.showLoadingEvent.observe(viewLifecycleOwner) {
            animationView.show()
        }
        viewModel.hideLoadingEvent.observe(viewLifecycleOwner) {
            animationView.hide()
            swipeRefreshLayout.isRefreshing = false
        }
        viewModel.showHUDEvent.observe(viewLifecycleOwner) {
            AMProgressHUD.showWithStatus(activity)
        }
        viewModel.hideHUDEvent.observe(viewLifecycleOwner) {
            AMProgressHUD.dismiss()
        }
        viewModel.showHUDErrorEvent.observe(viewLifecycleOwner) {
            AMProgressHUD.showWithError(activity, it)
        }
        viewModel.enableListenButtonEvent.observe(viewLifecycleOwner) {
            buttonListenNow.background = buttonListenNow.context.drawableCompat(if (it) R.drawable.artists_onboarding_orange else R.drawable.artists_onboarding_gray)
            buttonListenNow.text = if (it) getString(R.string.artists_onboarding_button) else getString(R.string.artists_onboarding_select)
        }
        viewModel.updateListEvent.observe(viewLifecycleOwner) {
            adapter.updateData(it)
        }
        viewModel.changedSelectionEvent.observe(viewLifecycleOwner) {
            adapter.updateSelection(it)
            swipeRefreshLayout.isEnabled = false
        }
        viewModel.openTrendingEvent.observe(viewLifecycleOwner) {
            (activity as? HomeActivity)?.openBrowse(null, "trending")
        }
        viewModel.showPlaylistEvent.observe(viewLifecycleOwner) { (customImage, playlist) ->
            (activity as? HomeActivity)?.openOnboardingPlaylist(customImage, playlist)
        }

        adapter = ArtistsOnboardingAdapter({
            viewModel.onItemTapped(it)
        }, {
            viewModel.onTapFooter()
        })
        recyclerView.adapter = adapter
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 3).apply {
            spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (position == 0 || position == (recyclerView.adapter?.itemCount ?: 0) - 1) 3 else 1
                }
            }
        }
        recyclerView.setHasFixedSize(true)

        swipeRefreshLayout.setColorSchemeColors(swipeRefreshLayout.context.colorCompat(R.color.orange))
        swipeRefreshLayout.isHapticFeedbackEnabled = true
        swipeRefreshLayout.setOnRefreshListener { viewModel.onRefreshTriggered() }

        buttonClose.setOnClickListener { viewModel.onCloseTapped() }

        buttonListenNow.setOnClickListener { viewModel.onListenNowTapped() }

        viewModel.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    companion object {
        const val TAG = "ArtistsOnboardingFragment"
        @JvmStatic
        fun newInstance(): ArtistsOnboardingFragment = ArtistsOnboardingFragment()
    }
}
