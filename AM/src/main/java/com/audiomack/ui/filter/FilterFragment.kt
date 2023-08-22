package com.audiomack.ui.filter

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.onCheckChanged
import kotlinx.android.synthetic.main.fragment_filter.btnLocalSelectFiles
import kotlinx.android.synthetic.main.fragment_filter.buttonApply
import kotlinx.android.synthetic.main.fragment_filter.buttonClose
import kotlinx.android.synthetic.main.fragment_filter.layoutLocalFiles
import kotlinx.android.synthetic.main.fragment_filter.layoutSort
import kotlinx.android.synthetic.main.fragment_filter.layoutType
import kotlinx.android.synthetic.main.fragment_filter.switchLocalFiles
import kotlinx.android.synthetic.main.fragment_filter.tvSortAZ
import kotlinx.android.synthetic.main.fragment_filter.tvSortNewest
import kotlinx.android.synthetic.main.fragment_filter.tvSortOldest
import kotlinx.android.synthetic.main.fragment_filter.tvTopTitle
import kotlinx.android.synthetic.main.fragment_filter.tvTypeAlbums
import kotlinx.android.synthetic.main.fragment_filter.tvTypeAll
import kotlinx.android.synthetic.main.fragment_filter.tvTypePlaylists
import kotlinx.android.synthetic.main.fragment_filter.tvTypeSongs

class FilterFragment : TrackedFragment(R.layout.fragment_filter, TAG) {

    private lateinit var viewModel: FilterViewModel
    private var filter: FilterData? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val filterData = filter ?: run {
            activity?.onBackPressed()
            return
        }

        viewModel = ViewModelProvider(this, FilterViewModelFactory(filterData))
            .get(FilterViewModel::class.java)

        viewModel.closeEvent.observe(viewLifecycleOwner) {
            activity?.onBackPressed()
        }

        viewModel.updateUIEvent.observe(viewLifecycleOwner) {
            tvTopTitle.text = viewModel.screenTitle
            layoutType.isVisible = viewModel.typeVisible
            layoutLocalFiles.isVisible = viewModel.localVisible
            tvTypeAll.setCompoundDrawablesWithIntrinsicBounds(
                tvTypeAll.context.drawableCompat(if (viewModel.typeAllSelected) R.drawable.ic_check_on else R.drawable.ic_check_off),
                null, null, null
            )
            tvTypeSongs.setCompoundDrawablesWithIntrinsicBounds(
                tvTypeSongs.context.drawableCompat(if (viewModel.typeSongsSelected) R.drawable.ic_check_on else R.drawable.ic_check_off),
                null, null, null
            )
            tvTypeAlbums.setCompoundDrawablesWithIntrinsicBounds(
                tvTypeAlbums.context.drawableCompat(if (viewModel.typeAlbumsSelected) R.drawable.ic_check_on else R.drawable.ic_check_off),
                null, null, null
            )
            tvTypePlaylists.visibility =
                if (viewModel.typePlaylistsVisible) View.VISIBLE else View.GONE
            tvTypePlaylists.setCompoundDrawablesWithIntrinsicBounds(
                tvTypePlaylists.context.drawableCompat(if (viewModel.typePlaylistsSelected) R.drawable.ic_check_on else R.drawable.ic_check_off),
                null, null, null
            )

            layoutSort.visibility = if (viewModel.sortVisible) View.VISIBLE else View.GONE
            tvSortNewest.setCompoundDrawablesWithIntrinsicBounds(
                tvSortNewest.context.drawableCompat(if (viewModel.sortNewestSelected) R.drawable.ic_check_on else R.drawable.ic_check_off),
                null, null, null
            )
            tvSortOldest.setCompoundDrawablesWithIntrinsicBounds(
                tvSortOldest.context.drawableCompat(if (viewModel.sortOldestSelected) R.drawable.ic_check_on else R.drawable.ic_check_off),
                null, null, null
            )
            tvSortAZ.setCompoundDrawablesWithIntrinsicBounds(
                tvSortAZ.context.drawableCompat(if (viewModel.sortAZSelected) R.drawable.ic_check_on else R.drawable.ic_check_off),
                null, null, null
            )
        }

        viewModel.includeLocalFiles.observe(viewLifecycleOwner) { switchLocalFiles.isChecked = it }

        tvTypeAll.setOnClickListener { viewModel.onTypeAllClick() }
        tvTypeSongs.setOnClickListener { viewModel.onTypeSongsClick() }
        tvTypeAlbums.setOnClickListener { viewModel.onTypeAlbumsClick() }
        tvTypePlaylists.setOnClickListener { viewModel.onTypePlaylistsClick() }

        tvSortNewest.setOnClickListener { viewModel.onSortNewestClick() }
        tvSortOldest.setOnClickListener { viewModel.onSortOldestClick() }
        tvSortAZ.setOnClickListener { viewModel.onSortAZClick() }

        btnLocalSelectFiles.setOnClickListener { viewModel.onSelectLocalFilesClick() }
        switchLocalFiles.onCheckChanged { viewModel.onIncludeLocalFilesToggle(it) }

        buttonClose.setOnClickListener { viewModel.onCloseClick() }
        buttonApply.setOnClickListener { viewModel.onApplyClick() }

        viewModel.onCreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filter = arguments?.getParcelable("filter")
    }

    companion object {
        private const val TAG = "FilterFragment"
        fun newInstance(filter: FilterData): FilterFragment =
            FilterFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("filter", filter)
                }
            }
    }
}
