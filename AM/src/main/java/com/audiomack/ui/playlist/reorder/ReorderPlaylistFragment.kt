package com.audiomack.ui.playlist.reorder

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.api.MusicRepository
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMResultItem
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.utils.ReorderRecyclerViewItemTouchHelper
import com.audiomack.views.AMProgressHUD
import com.audiomack.views.AMSnackbar
import kotlinx.android.synthetic.main.fragment_reorder_playlist.buttonClose
import kotlinx.android.synthetic.main.fragment_reorder_playlist.buttonSave
import kotlinx.android.synthetic.main.fragment_reorder_playlist.recyclerView

class ReorderPlaylistFragment : TrackedFragment(R.layout.fragment_reorder_playlist, TAG) {

    private lateinit var viewModel: ReorderPlaylistViewModel
    private lateinit var adapter: ReorderPlaylistAdapter
    private lateinit var playlist: AMResultItem

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!::playlist.isInitialized) {
            activity?.onBackPressed()
            return
        }

        viewModel = ViewModelProvider(this, ReorderPlaylistViewModelFactory(playlist, AMSchedulersProvider(), MusicRepository())).get(ReorderPlaylistViewModel::class.java)
        viewModel.showTracksEvent.observe(this, Observer {
            adapter = ReorderPlaylistAdapter(it)
            recyclerView.adapter = adapter
            recyclerView.setHasFixedSize(true)
            val itemTouchHelper = ReorderRecyclerViewItemTouchHelper(adapter)
            ItemTouchHelper(itemTouchHelper).attachToRecyclerView(recyclerView)
        })
        viewModel.closeEvent.observe(this, Observer {
            activity?.finish()
        })
        viewModel.loadingEvent.observe(this, Observer {
            when (it) {
                ReorderPlaylistViewModel.ReorderPlaylistLoadingStatus.Loading -> AMProgressHUD.showWithStatus(activity)
                is ReorderPlaylistViewModel.ReorderPlaylistLoadingStatus.Error -> {
                    AMProgressHUD.dismiss()
                    AMSnackbar.Builder(activity)
                        .withTitle(it.message)
                        .withSubtitle(getString(R.string.please_try_again_later))
                        .withDrawable(R.drawable.ic_snackbar_error)
                        .withSecondary(R.drawable.ic_snackbar_playlist_grey)
                        .show()
                }
                is ReorderPlaylistViewModel.ReorderPlaylistLoadingStatus.Success -> {
                    AMProgressHUD.dismiss()
                    AMSnackbar.Builder(activity)
                        .withTitle(it.message)
                        .withDrawable(R.drawable.ic_snackbar_playlist)
                        .show()
                }
            }
        })

        buttonClose.setOnClickListener { viewModel.onCloseTapped() }

        buttonSave.setOnClickListener { viewModel.onSaveTapped(adapter.getItems()) }

        viewModel.onCreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainApplication.playlist?.let {
            playlist = it
        }
    }

    companion object {
        private const val TAG = "ReorderPlaylistFragment"
        fun newInstance(): ReorderPlaylistFragment = ReorderPlaylistFragment()
    }
}
