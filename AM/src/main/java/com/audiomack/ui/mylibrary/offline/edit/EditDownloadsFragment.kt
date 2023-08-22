package com.audiomack.ui.mylibrary.offline.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.utils.ReorderRecyclerViewItemTouchHelper
import kotlinx.android.synthetic.main.fragment_multi_select.buttonApply
import kotlinx.android.synthetic.main.fragment_multi_select.buttonClose
import kotlinx.android.synthetic.main.fragment_multi_select.recyclerView

class EditDownloadsFragment : TrackedFragment(R.layout.fragment_multi_select, TAG) {

    private val viewModel: EditDownloadsViewModel by viewModels()
    private var adapter = EditDownloadsAdapter(
        removeListener = { viewModel.onMusicRemoved(it) },
        selectionChangedListener = { viewModel.onSelectionChanged(it) },
        noMoreItemsListener = { viewModel.onDownloadsCompletelyRemoved() }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        val itemTouchHelper = ReorderRecyclerViewItemTouchHelper(adapter, reorderEnabled = false, deleteLastItemEnabled = true)
        ItemTouchHelper(itemTouchHelper).attachToRecyclerView(recyclerView)

        initViewModelObservers()
        initClickListeners()
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            closeEvent.observe(viewLifecycleOwner, { activity?.onBackPressed() })
            removeButtonEnabled.observe(viewLifecycleOwner, { enabled ->
                buttonApply.isEnabled = enabled
            })
            showMusicListEvent.observe(viewLifecycleOwner, { musicList ->
                adapter.setMusicList(musicList)
            })
            removeSelectedMusicEvent.observe(viewLifecycleOwner, {
                adapter.removeSelectedMusic()
            })
        }
    }

    private fun initClickListeners() {
        buttonClose.setOnClickListener { viewModel.onCloseButtonClick() }
        buttonApply.setOnClickListener { viewModel.onRemoveButtonClick() }
    }

    companion object {
        const val TAG = "EditDownloadsFragment"
        fun newInstance() = EditDownloadsFragment()
    }
}
