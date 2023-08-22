package com.audiomack.ui.queue

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.audiomack.R
import com.audiomack.ui.common.SlideUpMenuFragment
import com.audiomack.ui.common.SlideUpMenuItem

class SlideUpMenuQueueFragment : SlideUpMenuFragment(TAG) {

    private val viewModel: SlideUpMenuQueueViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showSaveToPlaylist.observe(viewLifecycleOwner) { showSaveToPlaylist ->
            val menuItems = mutableListOf(
                SlideUpMenuItem(
                    R.drawable.ic_menu_hide,
                    R.string.queue_clear_all,
                    viewModel::onClearAllClick
                ),
                SlideUpMenuItem(
                    R.drawable.ic_menu_remove,
                    R.string.queue_clear_upcoming,
                    viewModel::onClearUpcomingClick
                )
            )

            if (showSaveToPlaylist) {
                menuItems.add(
                    0,
                    SlideUpMenuItem(
                        R.drawable.ic_menu_add_to_queue,
                        R.string.queue_save_playlist,
                        viewModel::onSaveToPlaylistClick
                    )
                )
            }

            setMenuItems(menuItems)
        }
    }

    companion object {
        const val TAG = "SlideUpMenuQueueFragment"

        fun newInstance(): SlideUpMenuQueueFragment = SlideUpMenuQueueFragment()
    }
}
