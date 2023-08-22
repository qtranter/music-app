package com.audiomack.ui.player.maxi.bottom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.audiomack.R
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.player.NowPlayingViewModel
import kotlinx.android.synthetic.main.fragment_player_bottom.viewPager

class PlayerBottomFragment : Fragment() {

    private lateinit var viewModel: NowPlayingViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_player_bottom, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = (requireActivity() as HomeActivity).nowPlayingViewModel

        initViews()
        initViewModelObservers()
    }

    private fun initViews() {
        PlayerBottomAdapter(childFragmentManager, lifecycle).let { viewPager.adapter = it }
        viewPager.apply {
            offscreenPageLimit = 2
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    requestLayout()
                    viewModel.onBottomPageSelected(position)
                }
            })
        }
    }

    private fun initViewModelObservers() {
        viewModel.bottomTabClickEvent.observe(viewLifecycleOwner, Observer { currentIndex ->
            if (currentIndex != viewPager.currentItem) {
                viewPager.currentItem = currentIndex
            }
        })
    }

    companion object {
        const val TAG = "PlayerBottomFragment"

        fun newInstance() = PlayerBottomFragment()
    }
}
