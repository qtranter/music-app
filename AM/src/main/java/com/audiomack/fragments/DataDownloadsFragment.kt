package com.audiomack.fragments

import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.R.string
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.music.local.LocalMediaDataSource
import com.audiomack.data.music.local.LocalMediaRepository
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.premiumdownload.PremiumDownloadRepository
import com.audiomack.data.removedcontent.RemovedContentRepository
import com.audiomack.data.sizes.SizesRepository
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.mixpanel.MixpanelFilterSort
import com.audiomack.data.tracking.mixpanel.MixpanelFilterType
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryOffline
import com.audiomack.data.tracking.mixpanel.MixpanelPageRestoreDownloads
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.AMMusicType
import com.audiomack.model.AMMusicType.All
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItemSort
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.CellType
import com.audiomack.model.EventDeletedDownload
import com.audiomack.model.EventDownload
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.model.EventFilterSaved
import com.audiomack.model.EventRemovedDownloadFromList
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.network.AnalyticsHelper
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.filter.FilterData
import com.audiomack.ui.filter.FilterFragment
import com.audiomack.ui.filter.FilterSection
import com.audiomack.ui.filter.FilterSelection
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.home.NavigationActions
import com.audiomack.ui.home.NavigationManager
import com.audiomack.ui.mylibrary.DataTabFragment
import com.audiomack.ui.mylibrary.OfflineHeader
import com.audiomack.ui.mylibrary.offline.edit.EditDownloadsFragment
import com.audiomack.ui.mylibrary.offline.local.StoragePermissionHandler
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.audiomack.ui.removedcontent.RemovedContentActivity
import com.audiomack.utils.addTo
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.spannableString
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.Collections
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DataDownloadsFragment : DataFragment(TAG), DataTabFragment {

    private val preferencesRepository = PreferencesRepository()
    private val premiumDownloadRepository = PremiumDownloadRepository.getInstance()
    private val musicRepository: MusicDataSource = MusicRepository()
    private val localMediaDataRepository: LocalMediaDataSource = LocalMediaRepository.getInstance()
    private val userRepository: UserDataSource = UserRepository.getInstance()
    private val navigation: NavigationActions = NavigationManager.getInstance()
    private val storagePermissionHandler = StoragePermissionHandler.getInstance()
    private val trackingRepo = TrackingRepository()

    private enum class TabSelection(val value: Int) {
        Downloaded(0),
        NotOnDevice(1);
    }

    val isRestoreDownloads: Boolean
        get() = tabSelectionIndex == TabSelection.NotOnDevice.value

    private var tabSelectionIndex = TabSelection.Downloaded.value

    private var onResumeExecutedOnce: Boolean = false

    private var filtersHeader: OfflineHeader? = null
    private var layoutProgress: ViewGroup? = null
    private var layoutRemovedContent: ViewGroup? = null
    private var buttonDownloaded: RadioButton? = null
    private var buttonNotOnDevice: RadioButton? = null
    private var downloadTabsHeader: ViewGroup? = null

    // Restore downloads data
    private val restoreDownloadsMinResults = 20
    private val restoreDownloadsBuffer = ArrayList<AMResultItem>()
    private var restoreDownloadsMusicIds: List<String>? = null

    private var isTabSelected = false

    private var showLocalFilePrompt: Disposable? = null
    private val disposables = CompositeDisposable()

    private lateinit var filterData: FilterData
    private fun getFilterData(): FilterData = FilterData(
        this::class.java.simpleName,
        getString(R.string.offline_filter_title),
        listOf(FilterSection.Type, FilterSection.Sort, FilterSection.Local),
        FilterSelection(type = AMMusicType.All, sort = preferencesRepository.offlineSorting)
    )

    override fun onResume() {
        super.onResume()
        if (onResumeExecutedOnce && !isRestoreDownloads) {
            triggerPullToRefresh(false)
        }
        onResumeExecutedOnce = true

        // Premium status may have changed
        adjustInsets()
        recyclerViewAdapter.notifyDataSetChanged()
        configureDownloadHeaderView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filterData = getFilterData()
        AnalyticsHelper.getInstance().trackScreen("Offline - Downloads")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(ARG_CATEGORY)?.let {
            if (it == getString(R.string.offline_filter_notondevice)) {
                tabSelectionChanged(TabSelection.NotOnDevice)
            }
        }
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    override fun onTabSelected(fragment: Fragment) {
        isTabSelected = fragment == this
        requestPermissionsIfNecessary()
        showLocalFilePromptIfNecessary()
    }

    private fun requestPermissionsIfNecessary() {
        if (preferencesRepository.includeLocalFiles && !storagePermissionHandler.hasPermission) {
            storagePermissionHandler.checkPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        storagePermissionHandler.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun showLocalFilePromptIfNecessary() {
        showLocalFilePrompt?.let { disposables.remove(it) }

        if (!preferencesRepository.localFilePromptShown && isTabSelected && !preferencesRepository.includeLocalFiles) {
            showLocalFilePrompt = localMediaDataRepository.allTracks
                .take(1)
                .map {
                    it.sumBy { item ->
                        if (item.isAlbum) {
                            item.tracks?.size ?: 0
                        } else 1
                    }
                }
                .onErrorReturnItem(0)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { showLocalMediaPrompt(it) }
                .addTo(disposables)
        }
    }

    private fun showLocalMediaPrompt(localMediaSize: Int) {
        val context = context ?: return
        AMAlertFragment.Builder(context)
            .title(string.local_player_granted_alert_title)
            .message(
                if (localMediaSize > 0) {
                    context.getString(string.local_player_granted_alert_message, localMediaSize)
                } else {
                    context.getString(string.local_player_ungranted_alert_message)
                }
            )
            .solidButton(string.local_player_granted_alert_positive) {
                navigation.launchLocalFilesSelection()
                trackingRepo.trackBreadcrumb("Launching local media file selection")
            }
            .plain1Button(string.local_player_granted_alert_negative)
            .drawableResId(R.drawable.ic_local_file)
            .dismissOnTouchOutside(false)
            .dismissHandler({ trackingRepo.trackBreadcrumb("User declined to include local media") })
            .cancellable(false)
            .show(childFragmentManager)

        trackingRepo.trackBreadcrumb("Showed local media inclusion prompt")

        Completable.fromRunnable {
            preferencesRepository.localFilePromptShown = true
        }.subscribeOn(Schedulers.io()).subscribe()
    }

    override fun apiCallObservable(): APIRequestData? {
        super.apiCallObservable()

        if (isRestoreDownloads) {
            // Only fetch musicIds from DB on the first API call
            // On API response filter music already downloaded on this device
            //      if the union between filtered list and `buffer` is smaller than `MIN_RESULTS` then add results to `buffer` and load next page
            //      else add `buffer` to the recyclerview and clear `buffer`
            if (currentPage == 0) {
                restoreDownloadsMusicIds = null
            }
            val observable =
                (restoreDownloadsMusicIds?.let { Observable.just(it) }
                    ?: run { Observable.fromCallable { AMResultItem.getAllItemsIds() } })
                    .flatMap { excludeIDs: List<String> ->
                        restoreDownloadsMusicIds = excludeIDs
                        API.getInstance()
                            .getDownloads("all", if (currentPage == 0) null else pagingToken, true)
                            .observable
                    }
                    .flatMap { data: APIResponseData ->
                        val items = data.objects as List<AMResultItem>
                        if (items.isNotEmpty()) {
                            val freshItems = java.util.ArrayList<AMResultItem>()
                            for (item in items) {
                                if (false == restoreDownloadsMusicIds?.contains(item.itemId)) {
                                    freshItems.add(item)
                                }
                            }
                            restoreDownloadsBuffer.addAll(freshItems)
                            if (restoreDownloadsBuffer.size < restoreDownloadsMinResults) {
                                currentPage++
                                this@DataDownloadsFragment.pagingToken = data.pagingToken
                                // Setting the third param to true to tell DataFragment the result shouldn't be processed yet
                                Observable.just(APIResponseData(Collections.emptyList(), data.pagingToken, true))
                            } else {
                                val copy = java.util.ArrayList<AMResultItem>()
                                copy.addAll(restoreDownloadsBuffer)
                                restoreDownloadsBuffer.clear()
                                Observable.just(APIResponseData(copy, data.pagingToken))
                            }
                        } else {
                            val copy = java.util.ArrayList<AMResultItem>()
                            copy.addAll(restoreDownloadsBuffer)
                            restoreDownloadsBuffer.clear()
                            Observable.just(APIResponseData(copy, data.pagingToken))
                        }
                    }

            return APIRequestData(observable, null)
        }

        val sort = filterData.selection.sort ?: AMResultItemSort.NewestFirst
        val type = filterData.selection.type ?: All
        val observable: Observable<APIResponseData> = musicRepository.getOfflineItems(type, sort)
            .flatMap { Observable.just(APIResponseData(it, null)) }
        return APIRequestData(observable, null)
    }

    override fun additionalHeaderPadding() =
        if (!PremiumRepository.isPremium()) context?.convertDpToPixel(120f) ?: 0 else 0

    override fun getCellType() = CellType.MUSIC_TINY

    override fun placeholderCustomView(): View {
        return LayoutInflater.from(context).inflate(R.layout.view_placeholder, null)
    }

    override fun configurePlaceholderView(placeholderView: View) {
        val imageView = placeholderView.findViewById<ImageView>(R.id.imageView)
        val tvMessage = placeholderView.findViewById<TextView>(R.id.tvMessage)
        val cta = placeholderView.findViewById<Button>(R.id.cta)
        imageView.setImageResource(R.drawable.ic_empty_downloads)
        tvMessage.setText(if (isRestoreDownloads) R.string.restoredownlods_noresults_placeholder else R.string.downloads_noresults_placeholder)
        cta.setText(R.string.downloads_noresults_highlighted_placeholder)
        cta.visibility = if (isRestoreDownloads) View.GONE else View.VISIBLE
        val imageVisible = SizesRepository.screenHeightDp > 620
        imageView.visibility = if (imageVisible) View.VISIBLE else View.GONE
        cta.setOnClickListener { didTapOnPlaceholder() }
    }

    override fun onClickFooter() {
        didTapOnPlaceholder()
    }

    private fun didTapOnPlaceholder() {
        tabSelectionChanged(TabSelection.NotOnDevice)
        changedSettings()
    }

    override fun recyclerViewHeader(): View? {

        val headerView = LayoutInflater.from(context).inflate(R.layout.header_offline, null)

        headerView.findViewById<OfflineHeader>(R.id.layoutFilters).apply {
            filter = filterData
            onFilterClick {
                (activity as? HomeActivity)?.openFilters(FilterFragment.newInstance(filterData))
            }
            onShuffleButtonClick {
                shufflePlay()
            }
            onEditButtonClick {
                (activity as? HomeActivity)?.openEditDownloads(EditDownloadsFragment.newInstance())
            }
        }.also { filtersHeader = it }

        layoutRemovedContent = headerView.findViewById(R.id.layoutRemovedContent)
        if (RemovedContentRepository.getItems().isNotEmpty()) {
            layoutRemovedContent?.visibility = View.VISIBLE
            val tvRemovedContent = headerView.findViewById<TextView>(R.id.tvRemovedContent)
            val buttonRemovedContent = headerView.findViewById<ImageButton>(R.id.buttonRemovedContent)
            tvRemovedContent.text = tvRemovedContent.context.spannableString(
                fullString = getString(R.string.removedcontent_header),
                highlightedStrings = listOf(getString(R.string.removedcontent_header_highlighted)),
                highlightedUnderline = true
            )
            tvRemovedContent.setOnClickListener {
                RemovedContentActivity.show(activity)
                Handler().postDelayed({
                    layoutRemovedContent?.visibility = View.GONE
                    layoutRemovedContent?.tag = TAG_REMOVED_CONTENT_HIDDEN
                    headerView.post { this.adjustInsets() }
                }, 300)
            }
            buttonRemovedContent.setOnClickListener {
                RemovedContentRepository.clearItems()
                layoutRemovedContent?.visibility = View.GONE
                layoutRemovedContent?.tag = TAG_REMOVED_CONTENT_HIDDEN
                headerView.post { this.adjustInsets() }
            }
        } else {
            layoutRemovedContent?.visibility = View.GONE
            layoutRemovedContent?.tag = TAG_REMOVED_CONTENT_HIDDEN
        }

        downloadTabsHeader = headerView.findViewById(R.id.layout_header_download_tabs)

        configureDownloadHeaderView()

        return headerView
    }

    override fun footerLayoutResId() = R.layout.row_footer_downloads

    private fun configureDownloadHeaderView() {

        downloadTabsHeader?.let { headerView ->

            val totalCount = premiumDownloadRepository.premiumDownloadLimit
            val currentCount = premiumDownloadRepository.premiumLimitedUnfrozenDownloadCount
            val availableCount = premiumDownloadRepository.remainingPremiumLimitedDownloadCount

            layoutProgress = headerView.findViewById<LinearLayout>(R.id.layoutProgress).also {
                it.visibility = if (!PremiumRepository.isPremium() && currentCount > 0) View.VISIBLE else View.GONE
            }
            val viewProgress = headerView.findViewById<View>(R.id.viewProgress)
            val tvDownloadLimit = headerView.findViewById<TextView>(R.id.tvDownloadLimit)
            val viewProgressContainer = headerView.findViewById<View>(R.id.viewProgressContainer)

            tvDownloadLimit.text = if (availableCount == 0) {
                val orangeString = getString(R.string.premium_download_upgrade)
                val tmp = tvDownloadLimit.context.spannableString(
                    fullString = " ${getString(R.string.premium_download_standard_message)} $orangeString",
                    highlightedStrings = listOf(orangeString),
                    highlightedColor = tvDownloadLimit.context.colorCompat(R.color.orange)
                )
                val countString = tvDownloadLimit.context.spannableString(
                    fullString = "$availableCount",
                    fullColor = tvDownloadLimit.context.colorCompat(R.color.red_error)
                )
                TextUtils.concat(countString, tmp)
            } else {
                val orangeString = getString(R.string.premium_download_upgrade)
                val tmp = tvDownloadLimit.context.spannableString(
                    fullString = " ${getString(R.string.premium_download_standard_message)} $orangeString",
                    highlightedStrings = listOf(orangeString),
                    highlightedColor = tvDownloadLimit.context.colorCompat(R.color.orange)
                )
                val countString = tvDownloadLimit.context.spannableString(
                    fullString = "$availableCount",
                    fullColor = tvDownloadLimit.context.colorCompat(R.color.orange)
                )
                TextUtils.concat(countString, tmp)
            }

            val progress = currentCount.toFloat() / totalCount.toFloat()
            val lp = viewProgress.layoutParams as FrameLayout.LayoutParams
            lp.width = (progress * viewProgressContainer.width).toInt()
            viewProgress.layoutParams = lp
            viewProgress.background = viewProgress.context.drawableCompat(if (availableCount == 0) R.drawable.header_download_progress_full else R.drawable.header_download_progress)

            layoutProgress?.setOnClickListener {
                InAppPurchaseActivity.show(activity, InAppPurchaseMode.PremiumDownload)
            }

            val alreadySetup = buttonDownloaded != null

            if (!alreadySetup) {

                buttonDownloaded = headerView.findViewById(R.id.downloadTabDownloaded)
                buttonDownloaded?.setOnClickListener {
                    tabSelectionChanged(TabSelection.Downloaded)
                }

                buttonNotOnDevice = headerView.findViewById(R.id.downloadTabOnDevice)
                buttonNotOnDevice?.setOnClickListener {
                    tabSelectionChanged(TabSelection.NotOnDevice)
                }

                tabSelectionChanged(TabSelection.Downloaded)
            }
        }
    }

    private fun tabSelectionChanged(selection: TabSelection) {
        when (selection) {
            TabSelection.Downloaded -> {
                buttonDownloaded?.isChecked = true
                buttonNotOnDevice?.isChecked = false
                filtersHeader?.visibility = View.VISIBLE
                configureDownloadHeaderView()
                if (layoutRemovedContent?.tag != TAG_REMOVED_CONTENT_HIDDEN) {
                    layoutRemovedContent?.visibility = View.VISIBLE
                }
            }
            TabSelection.NotOnDevice -> {
                buttonDownloaded?.isChecked = false
                buttonNotOnDevice?.isChecked = true
                filtersHeader?.visibility = View.GONE
                layoutProgress?.visibility = View.GONE
                layoutRemovedContent?.visibility = View.GONE
            }
        }
        tabSelectionIndex = selection.value
        changedSettings()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onMessageEvent(eventDownload: EventDownload) {
        recyclerViewAdapter?.let {
            val index = it.indexOfItemId(eventDownload.itemId)
            if (index != -1) {
                it.notifyItemChanged(index)
            }
            if (it.realItemsCount == 0 && !isRestoreDownloads) {
                changedSettings()
            }
        }
        configureDownloadHeaderView()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventDeletedDownload: EventDeletedDownload) {
        if (!isRestoreDownloads) {
            recyclerViewAdapter?.let {
                it.removeItem(eventDeletedDownload.item)
                if (it.realItemsCount == 0) {
                    changedSettings()
                }
            }
        } else {
            recyclerViewAdapter?.let { adapter ->
                adapter.indexOfItemId(eventDeletedDownload.item.itemId).takeIf { it != -1 }?.let { index ->
                    adapter.notifyItemChanged(index)
                }
            }
        }
        configureDownloadHeaderView()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventRemovedFromDownloadList: EventRemovedDownloadFromList) {
        if (isRestoreDownloads) {
            recyclerViewAdapter.removeItem(eventRemovedFromDownloadList.item)
            hideLoader(true)
        }
        configureDownloadHeaderView()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventFilterSaved: EventFilterSaved) {
        if (eventFilterSaved.filter.fragmentClassName == this::class.java.simpleName) {
            val oldType = filterData.selection.type
            filterData = eventFilterSaved.filter
            filterData.selection.sort?.let { preferencesRepository.offlineSorting = it }
            if (eventFilterSaved.filter.selection.type == AMMusicType.Playlists) {
                val isAdmin = AMArtist.getSavedArtist()?.isAdmin ?: false
                if (!PremiumRepository.isPremium() && !isAdmin) {
                    InAppPurchaseActivity.show(activity, InAppPurchaseMode.PlaylistBrowseDownload)
                } else {
                    HomeActivity.instance?.openMyAccount("playlists", "Offline-Only")
                }
                filterData.selection.type = oldType
                filtersHeader?.filter = filterData
                return
            }
            filtersHeader?.filter = filterData
            changedSettings()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventDownloadsEdited) {
        changedSettings()
        configureDownloadHeaderView()
    }

    override fun getMixpanelSource(): MixpanelSource {
        if (isRestoreDownloads) {
            return MixpanelSource(MainApplication.currentTab, MixpanelPageRestoreDownloads)
        }
        val typeName = when (filterData.selection.type) {
            AMMusicType.Playlists -> "Playlists"
            AMMusicType.Albums -> "Albums"
            AMMusicType.Songs -> "Songs"
            else -> "All"
        }
        val sortName = when (filterData.selection.sort) {
            AMResultItemSort.AToZ -> "A-Z"
            AMResultItemSort.OldestFirst -> "Oldest"
            else -> "Newest"
        }
        return MixpanelSource(MainApplication.currentTab, MixpanelPageMyLibraryOffline, listOf(
            Pair(MixpanelFilterType, typeName),
            Pair(MixpanelFilterSort, sortName)
        ))
    }

    companion object {
        private const val TAG = "DataDownloadsFragment"
        const val TAG_REMOVED_CONTENT_HIDDEN = "hidden"
        private const val ARG_CATEGORY: String = "arg_category"

        @JvmOverloads
        fun newInstance(category: String? = null) = DataDownloadsFragment().apply {
            category?.let { arguments = bundleOf(ARG_CATEGORY to it) }
        }
    }
}
