package com.audiomack.ui.replacedownload

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMResultItem
import com.audiomack.model.PremiumDownloadInfoModel
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.showDownloadUnlockedToast
import com.audiomack.views.AMProgressHUD
import kotlinx.android.synthetic.main.fragment_comments.recyclerView
import kotlinx.android.synthetic.main.fragment_replace_download.buttonClose
import kotlinx.android.synthetic.main.fragment_replace_download.buttonReplace
import kotlinx.android.synthetic.main.fragment_replace_download.tvSubtitle

class ReplaceDownloadFragment : TrackedFragment(R.layout.fragment_replace_download, TAG) {

    private val viewModel: ReplaceDownloadViewModel by viewModels()
    private lateinit var adapter: ReplaceDownloadAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()
        initView()
        initClickListeners()
        initViewModelObservers()
    }

    private fun initView() {
        adapter = ReplaceDownloadAdapter(viewModel)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
    }

    private fun initViewModel() {
        val model = arguments?.getParcelable(EXTRA_PREMIUM_DOWNLOADS) as? PremiumDownloadModel
            ?: throw IllegalStateException("Missing 'data' intent extra")
        viewModel.init(model)
    }

    private fun initClickListeners() {
        buttonClose.setOnClickListener { viewModel.onCloseClick() }
        buttonReplace.setOnClickListener { viewModel.onReplaceClick() }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            openDownloadsEvent.observe(viewLifecycleOwner, openDownloadsEventsObserver)
            closeEvent.observe(viewLifecycleOwner, closeEventObserver)
            showHUDEvent.observe(viewLifecycleOwner, showHUDEventObserver)
            items.observe(viewLifecycleOwner, itemsObserver)
            itemsSelected.observe(viewLifecycleOwner, itemsSelectedObserver)
            replaceTextData.observe(viewLifecycleOwner, replaceTextObserver)
            subtitleText.observe(viewLifecycleOwner, subtitleTextObserver)
            showUnlockedToastEvent.observe(viewLifecycleOwner, showUnlockedToastEventObserver)
        }
    }

    private fun getTextForNumber(number: Int): Int {
        return when (number) {
            1 -> R.string.premium_download_replace_button_text_one
            2 -> R.string.premium_download_replace_button_text_two
            3 -> R.string.premium_download_replace_button_text_three
            4 -> R.string.premium_download_replace_button_text_four
            5 -> R.string.premium_download_replace_button_text_five
            6 -> R.string.premium_download_replace_button_text_six
            7 -> R.string.premium_download_replace_button_text_seven
            8 -> R.string.premium_download_replace_button_text_eight
            9 -> R.string.premium_download_replace_button_text_nine
            10 -> R.string.premium_download_replace_button_text_ten
            11 -> R.string.premium_download_replace_button_text_eleven
            12 -> R.string.premium_download_replace_button_text_twelve
            13 -> R.string.premium_download_replace_button_text_thirteen
            14 -> R.string.premium_download_replace_button_text_fourteen
            15 -> R.string.premium_download_replace_button_text_fifteen
            16 -> R.string.premium_download_replace_button_text_sixteen
            17 -> R.string.premium_download_replace_button_text_seventeen
            18 -> R.string.premium_download_replace_button_text_eighteen
            19 -> R.string.premium_download_replace_button_text_nineteen
            20 -> R.string.premium_download_replace_button_text_twenty
            else -> R.string.empty
        }
    }

    private val openDownloadsEventsObserver = Observer<Void> {
        (activity as? HomeActivity)?.openMyAccount("downloads")
    }

    private val closeEventObserver = Observer<Void> {
        activity?.onBackPressed()
        AMProgressHUD.dismiss()
    }

    private val showHUDEventObserver = Observer<ProgressHUDMode> {
        AMProgressHUD.show(activity, it)
    }

    private val itemsObserver = Observer<List<AMResultItem>> { items ->
        if (items.isNullOrEmpty()) {
            recyclerView.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            adapter.update(items)
        }
    }

    private val itemsSelectedObserver = Observer<List<AMResultItem>> { itemsSelected ->
        adapter.updateSelectedItems(itemsSelected)
    }

    private val subtitleTextObserver = Observer<Int> {
        if (it == 1) tvSubtitle.text = getString(R.string.premium_download_replace_single_subheader) else tvSubtitle.text = getString(R.string.premium_download_replace_multiple_subheader, it)
    }

    private val showUnlockedToastEventObserver = Observer<String> { musicName ->
        showDownloadUnlockedToast(musicName)
    }

    private val replaceTextObserver = Observer<PremiumDownloadInfoModel> { info ->
        val downloadCount = info.replaceCount
        val selectedCount = info.selectedCount
        val remainingCount = downloadCount - selectedCount
        val remainingString = getString(getTextForNumber(remainingCount))
        buttonReplace.text = if (remainingCount > 0) getString(R.string.premium_download_replace_button, remainingString, remainingCount) else getString(R.string.premium_download_replace_button_selected, selectedCount)
        buttonReplace.background = buttonReplace.context.drawableCompat(if (remainingCount > 0) R.drawable.popup_rounded_button_grey else R.drawable.popup_rounded_button)
        buttonReplace.isEnabled = remainingCount <= 0
    }

    companion object {
        private const val TAG = "ReplaceDownloadFragment"
        private const val EXTRA_PREMIUM_DOWNLOADS = "data"
        fun newInstance(data: PremiumDownloadModel): ReplaceDownloadFragment =
            ReplaceDownloadFragment().apply {
                arguments = Bundle().apply { putParcelable(EXTRA_PREMIUM_DOWNLOADS, data) }
            }
    }
}
