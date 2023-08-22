package com.audiomack.ui.mylibrary.offline.local

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import kotlinx.android.synthetic.main.fragment_multi_select.buttonApply
import kotlinx.android.synthetic.main.fragment_multi_select.buttonClose
import kotlinx.android.synthetic.main.fragment_multi_select.emptyView
import kotlinx.android.synthetic.main.fragment_multi_select.recyclerView
import kotlinx.android.synthetic.main.fragment_multi_select.tvTopTitle

class LocalMediaSelectionFragment : TrackedFragment(R.layout.fragment_multi_select, TAG) {

    private val viewModel: LocalMediaSelectionViewModel by viewModels()

    private val permissionHandler = StoragePermissionHandler.getInstance()

    private val adapter: LocalFileSelectionAdapter by lazy {
        LocalFileSelectionAdapter().also { recyclerView.adapter = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews()
        initViewModel()
    }

    override fun onDestroyView() {
        recyclerView.adapter = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun initViews() {
        tvTopTitle.setText(R.string.offline_filter_files_select)
        recyclerView.setHasFixedSize(true)
        buttonClose.setOnClickListener { viewModel.onCloseClick() }
        buttonApply.apply {
            isEnabled = true
            setText(R.string.local_file_selection_apply)
            setOnClickListener { viewModel.onSaveExclusionsClick(adapter.exclusionIds) }
        }
    }

    private fun initViewModel() {
        viewModel.run {
            items.observe(viewLifecycleOwner, adapter::submitList)
            exclusions.observe(viewLifecycleOwner, ::onExclusionsLoaded)
            showEmptyView.observe(viewLifecycleOwner) { emptyView.isVisible = it }
        }
    }

    private fun onExclusionsLoaded(exclusionIds: List<Long>) {
        adapter.exclusionIds = exclusionIds.toMutableList()
    }

    private fun checkPermissions() {
        permissionHandler.checkPermissions(
            this,
            onAlreadyGranted = { viewModel.onRefresh() }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionHandler.onRequestPermissionsResult(
            requestCode,
            grantResults,
            onDenied = { viewModel.onStoragePermissionDenied() })
    }

    companion object {
        const val TAG = "LocalMediaSelectionFragment"

        fun newInstance() = LocalMediaSelectionFragment()
    }
}
