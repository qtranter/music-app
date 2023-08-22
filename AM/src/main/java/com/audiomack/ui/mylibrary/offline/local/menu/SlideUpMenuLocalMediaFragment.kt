package com.audiomack.ui.mylibrary.offline.local.menu

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.audiomack.R.drawable
import com.audiomack.R.string
import com.audiomack.ui.common.SlideUpMenuFragment
import com.audiomack.ui.common.SlideUpMenuItem

class SlideUpMenuLocalMediaFragment : SlideUpMenuFragment(TAG) {

    private val viewModel: SlideUpMenuLocalMediaViewModel by viewModels()

    private var queueIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.run {
            viewModel.id = getLong(ARG_ID)
            queueIndex = getInt(ARG_QUEUE_INDEX)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (queueIndex >= 0) {
            loadQueueMenu()
        } else {
            initViewModelObservers()
        }
    }

    private fun initViewModelObservers() {
        viewModel.isAlbum.observe(viewLifecycleOwner, ::loadDefaultMenu)
    }

    private fun loadDefaultMenu(isAlbum: Boolean) {
        setMenuItems(
            listOf(
                SlideUpMenuItem(
                    drawable.ic_menu_play_next,
                    string.options_play_next,
                    viewModel::onPlayNextClick
                ),
                SlideUpMenuItem(
                    drawable.ic_menu_add_to_queue,
                    string.options_add_to_queue,
                    viewModel::onAddToQueueClick
                ),
                SlideUpMenuItem(
                    drawable.ic_menu_hide,
                    if (isAlbum) string.options_hide_album else string.options_hide_track,
                    viewModel::onHideClick
                )
            )
        )
    }

    private fun loadQueueMenu() {
        setMenuItems(
            listOf(
                SlideUpMenuItem(
                    drawable.ic_menu_hide,
                    string.options_remove_from_queue
                ) {
                    viewModel.onRemoveFromQueueClick(queueIndex)
                }
            )
        )
    }

    companion object {
        private const val TAG = "SlideUpMenuLocalMediaFragment"

        private const val ARG_ID = "arg_id"
        private const val ARG_QUEUE_INDEX = "arg_index_queue"

        @JvmStatic
        @JvmOverloads
        fun newInstance(id: Long, queueIndex: Int = -1): SlideUpMenuLocalMediaFragment =
            SlideUpMenuLocalMediaFragment().apply {
                arguments = bundleOf(
                    ARG_ID to id,
                    ARG_QUEUE_INDEX to queueIndex
                )
            }
    }
}
