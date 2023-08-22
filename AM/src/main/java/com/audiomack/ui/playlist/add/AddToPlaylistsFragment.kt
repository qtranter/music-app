package com.audiomack.ui.playlist.add

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import kotlinx.android.synthetic.main.fragment_addtoplaylists.animationView
import kotlinx.android.synthetic.main.fragment_addtoplaylists.buttonClose
import kotlinx.android.synthetic.main.fragment_addtoplaylists.recyclerView

class AddToPlaylistsFragment : TrackedFragment(R.layout.fragment_addtoplaylists, TAG) {

    private lateinit var adapter: SelectPlaylistsAdapter
    private val viewModel: AddToPlaylistsViewModel by activityViewModels()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initViewModelObservers()

        adapter = SelectPlaylistsAdapter(recyclerView, viewModel)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        buttonClose.setOnClickListener { viewModel.onCloseCliked() }

        viewModel.requestPlaylists()
    }

    private fun initViewModelObservers() {
        viewModel.reloadAdapterPositionEvent.observe(viewLifecycleOwner, Observer {
            adapter.notifyItemChanged(it)
        })
        viewModel.addDataToAdapterEvent.observe(viewLifecycleOwner, Observer {
            adapter.addPlaylists(it)
        })
        viewModel.progressBarVisible.observe(viewLifecycleOwner, Observer {
            if (it) animationView.show()
            else animationView.hide()
        })
        viewModel.hideLoadMoreEvent.observe(viewLifecycleOwner, Observer {
            adapter.hideLoadMore(true)
        })
        viewModel.enableLoadMoreEvent.observe(viewLifecycleOwner, Observer {
            adapter.enableLoadMore()
        })
        viewModel.disableLoadMoreEvent.observe(viewLifecycleOwner, Observer {
            adapter.disableLoadMore()
        })
    }

    companion object {
        private const val TAG = "AddToPlaylistsFragment"
    }
}
