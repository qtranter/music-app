package com.audiomack.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.audiomack.R
import kotlinx.android.synthetic.main.fragment_notifications_container.buttonClose

class NotificationsContainerFragment : Fragment(R.layout.fragment_notifications_container) {

    private val viewModel: NotificationsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModelObservers()
        initClickListeners()

        viewModel.onCreate()
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            closeEvent.observe(viewLifecycleOwner) { activity?.onBackPressed() }
            showNotificationsFragmentEvent.observe(viewLifecycleOwner) {
                childFragmentManager
                    .beginTransaction()
                    .add(R.id.container, DataNotificationsFragment.newInstance())
                    .commitAllowingStateLoss()
            }
            showPlaylistsGridEvent.observe(viewLifecycleOwner) { playlist ->
                activity?.supportFragmentManager?.also {
                    it.beginTransaction()
                        .add(R.id.container, DataNotificationUpdatedPlaylistsFragment.newInstance(playlist))
                        .addToBackStack("playlists")
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    private fun initClickListeners() {
        buttonClose.setOnClickListener { viewModel.onCloseTapped() }
    }

    companion object {
        const val TAG = "NotificationsContainerFragment"
        fun newInstance() = NotificationsContainerFragment()
    }
}
