package com.audiomack.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.data.tracking.mixpanel.MixpanelFilterType
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryFavorites
import com.audiomack.data.tracking.mixpanel.MixpanelPageProfileFavorites
import com.audiomack.model.AMMusicType
import com.audiomack.model.AMResultItem
import com.audiomack.model.APIRequestData
import com.audiomack.model.CellType
import com.audiomack.model.EventFavoriteDelete
import com.audiomack.model.EventFilterSaved
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.ui.filter.FilterData
import com.audiomack.ui.filter.FilterSection
import com.audiomack.ui.filter.FilterSelection
import com.audiomack.ui.filter.FilterViewModel
import com.audiomack.ui.filter.FilterViewModelFactory
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.mylibrary.FavoritesHeader
import com.audiomack.ui.mylibrary.MyLibraryFragment
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DataFavoritesFragment : DataFragment(TAG) {

    private val categoryCodes: List<String> = listOf("music", "song", "album", "playlist")

    private var categoryKey: String? = null

    private lateinit var filterData: FilterData
    private var header: FavoritesHeader? = null

    private lateinit var filterViewModel: FilterViewModel

    private fun getFilterData(): FilterData = FilterData(
        this::class.java.simpleName,
        getString(R.string.favorites_filter_title),
        listOf(FilterSection.Type),
        FilterSelection(type = AMMusicType.All),
        excludedTypes = if (myAccount) emptyList() else listOf(AMMusicType.Playlists)
    )

    var myAccount: Boolean = false
    private var userSlug: String? = null
    private var artistName: String? = null

    override fun apiCallObservable(): APIRequestData? {
        super.apiCallObservable()
        return if (myAccount) {
            API.getInstance().getArtistFavorites(userSlug, currentPage, category, false)
        } else {
            API.getInstance().getArtistFavorites(userSlug, currentPage, category, true)
        }
    }

    override fun getCellType(): CellType = CellType.MUSIC_BROWSE_SMALL

    override fun recyclerViewHeader(): View? {
        context?.let { ctx ->
            return FavoritesHeader(ctx).apply {
                filter = filterData
                onCheckChanged {
                    with(filterViewModel) {
                        onFilterTypeChanged(it)
                        onApplyClick()
                    }
                }
                onShuffleButtonClick {
                    shufflePlay()
                }
            }.also { header = it }
        }
        return null
    }

    override fun canShowUpsellView() = parentFragment is MyLibraryFragment

    override fun placeholderCustomView(): View {
        return LayoutInflater.from(context).inflate(R.layout.view_placeholder, null)
    }

    override fun configurePlaceholderView(placeholderView: View) {
        val imageView = placeholderView.findViewById<ImageView>(R.id.imageView)
        val tvMessage = placeholderView.findViewById<TextView>(R.id.tvMessage)
        val cta = placeholderView.findViewById<Button>(R.id.cta)
        imageView.setImageResource(R.drawable.ic_empty_favorites)
        if (myAccount) {
            tvMessage.setText(R.string.favorites_my_noresults_placeholder)
            cta.visibility = View.GONE
        } else {
            val highlightedString =
                if (TextUtils.isEmpty(artistName)) getString(R.string.user_name_placeholder) else artistName
            val fullString =
                getString(R.string.favorites_other_noresults_placeholder, highlightedString)
            tvMessage.text = tvMessage.context.spannableString(
                fullString = fullString,
                highlightedStrings = listOf(highlightedString ?: ""),
                fullColor = tvMessage.context.colorCompat(R.color.placeholder_gray),
                highlightedColor = tvMessage.context.colorCompat(android.R.color.white),
                fullFont = R.font.opensans_regular,
                highlightedFont = R.font.opensans_semibold
            )
            cta.visibility = View.GONE
        }
        val imageVisible = cta.visibility == View.GONE
        imageView.visibility = if (imageVisible) View.VISIBLE else View.GONE
        cta.setOnClickListener {
            AuthenticationActivity.show(
                context,
                LoginSignupSource.MyLibrary,
                null
            )
        }
    }

    override fun didRemoveGeorestrictedItem(item: AMResultItem) {
        viewModel.removeGeorestrictedItemFromFavorites(item, mixpanelSource, MixpanelButtonList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.category = categoryCodes[0]
            this.myAccount = it.getBoolean("myAccount")
            this.userSlug = it.getString("userSlug")
            this.artistName = it.getString("artistName")
        }
        filterData = getFilterData()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filterViewModel = ViewModelProvider(this, FilterViewModelFactory(filterData))
            .get(FilterViewModel::class.java)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventFavoriteDelete: EventFavoriteDelete) {
        if (recyclerViewAdapter != null) {
            val index = recyclerViewAdapter.indexOfItemId(eventFavoriteDelete.item.itemId)
            if (index != -1) {
                recyclerViewAdapter.removeItem(eventFavoriteDelete.item)
                hideLoader(true)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventFilterSaved: EventFilterSaved) {
        if (eventFilterSaved.filter.fragmentClassName == this::class.java.simpleName) {
            val oldType = filterData.selection.type
            filterData = eventFilterSaved.filter
            header?.filter = filterData
            categoryKey = when (filterData.selection.type) {
                AMMusicType.All -> categoryCodes[0]
                AMMusicType.Songs -> categoryCodes[1]
                AMMusicType.Albums -> categoryCodes[2]
                else -> {
                    (activity as? HomeActivity)?.openMyAccount("playlists", "Favorited Playlists")
                    filterData.selection.type = oldType
                    header?.filter = filterData
                    categoryKey
                    return
                }
            }
            changeCategory(categoryKey)
        }
    }

    override fun getMixpanelSource(): MixpanelSource {
        val page = when (parentFragment) {
            is MyLibraryFragment -> MixpanelPageMyLibraryFavorites
            else -> MixpanelPageProfileFavorites
        }
        val typeName = when (categoryKey) {
            categoryCodes[3] -> "Playlists"
            categoryCodes[2] -> "Albums"
            categoryCodes[1] -> "Songs"
            else -> "All"
        }
        return MixpanelSource(MainApplication.currentTab, page, listOf(
            Pair(MixpanelFilterType, typeName)
        ))
    }

    companion object {
        private const val TAG = "DataFavoritesFragment"

        fun newInstance(
            myAccount: Boolean,
            userSlug: String?,
            artistName: String?
        ) = DataFavoritesFragment().apply {
            arguments = Bundle().apply {
                putBoolean("myAccount", myAccount)
                putString("userSlug", userSlug)
                putString("artistName", artistName)
            }
        }
    }
}
