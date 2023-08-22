package com.audiomack.ui.player.maxi.bottom

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.audiomack.ui.comments.view.CommentsFragment
import com.audiomack.ui.player.maxi.info.PlayerInfoFragment
import com.audiomack.ui.player.maxi.morefromartist.PlayerMoreFromArtistFragment

class PlayerBottomAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CommentsFragment.newInstance(CommentsFragment.Mode.Player)
            1 -> PlayerInfoFragment.newInstance()
            2 -> PlayerMoreFromArtistFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid page position")
        }
    }
}
