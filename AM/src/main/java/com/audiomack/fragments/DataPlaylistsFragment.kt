package com.audiomack.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.adapters.DataRecyclerViewAdapter
import com.audiomack.data.database.MusicDAOImpl
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.tracking.mixpanel.MixpanelFilterType
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryPlaylists
import com.audiomack.data.tracking.mixpanel.MixpanelPageProfilePlaylists
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMResultItemSort
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.CellType
import com.audiomack.model.EventPlaylistDeleted
import com.audiomack.model.EventPlaylistEdited
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.ui.mylibrary.MyLibraryFragment
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString
import io.reactivex.Observable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DataPlaylistsFragment : DataFragment(TAG) {

    private var myAccount: Boolean = false
    private var artistUrlSlug: String? = null
    private var artistName: String? = null
    private var section: Section = Section.Mine

    enum class Section(val key: String) {
        Mine("Mine"), Favorited("Favorited Playlists"), Offline("Offline-Only");

        companion object {
            fun valueOfOrDefault(key: String) = values().firstOrNull { it.key == key } ?: Mine
        }
    }

    override fun apiCallObservable(): APIRequestData? {
        super.apiCallObservable()

        return when (section) {
            Section.Mine -> if (myAccount) {
                API.getInstance().getMyPlaylists(currentPage, "all", null, false)
            } else {
                API.getInstance().getArtistPlaylists(artistUrlSlug, currentPage, false)
            }
            Section.Favorited -> {
                API.getInstance().getArtistFavorites(artistUrlSlug, currentPage, "playlist", false)
            }
            Section.Offline -> if (currentPage == 0) {
                APIRequestData(MusicDAOImpl().savedPlaylists(AMResultItemSort.NewestFirst).flatMap { Observable.just(APIResponseData(it, null)) }, null)
            } else {
                APIRequestData(Observable.just(APIResponseData()), null)
            }
        }
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager {
        if (myAccount) {
            return super.getLayoutManager()
        }
        val layoutManager = GridLayoutManager(context, 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (recyclerViewAdapter.getItemViewType(position) == CellType.HEADER.ordinal || recyclerViewAdapter.getItemViewType(
                        position
                    ) == DataRecyclerViewAdapter.TYPE_LOADING
                ) {
                    2
                } else 1
            }
        }
        return layoutManager
    }

    override fun getCellType() = if (myAccount) CellType.MUSIC_TINY else CellType.PLAYLIST_GRID

    override fun recyclerViewHeader(): View? {
        if (!myAccount) {
            return null
        }
        val headerView = LayoutInflater.from(context).inflate(R.layout.header_playlists, null)
        val radioGroup = headerView.findViewById<RadioGroup>(R.id.radioGroup)

        radioGroup.check(radioGroup.getChildAt(section.ordinal).id)

        radioGroup.setOnCheckedChangeListener { group, checkedId ->

            val index = (0 until group.childCount).indexOfFirst { checkedId == group.getChildAt(it).id }.takeIf { it != -1 } ?: 0
            val newSection = Section.values()[index]

            if (newSection == Section.Offline && !PremiumRepository.isPremium() && !UserRepository.getInstance().isAdmin()) {
                InAppPurchaseActivity.show(activity, InAppPurchaseMode.PlaylistBrowseDownload)
                group.check(radioGroup.getChildAt(section.ordinal).id)
            } else {
                section = newSection
                changeCategory(section.key)
            }
        }

        return headerView
    }

    override fun canShowUpsellView() = parentFragment is MyLibraryFragment

    override fun placeholderCustomView(): View {
        return LayoutInflater.from(context).inflate(R.layout.view_placeholder, null)
    }

    override fun configurePlaceholderView(placeholderView: View) {
        val imageView = placeholderView.findViewById<ImageView>(R.id.imageView)
        val tvMessage = placeholderView.findViewById<TextView>(R.id.tvMessage)
        val cta = placeholderView.findViewById<Button>(R.id.cta)

        when (section) {
            Section.Mine -> {
                imageView.setImageResource(R.drawable.ic_empty_playlists)
                if (myAccount) {
                    tvMessage.setText(R.string.playlists_my_noresults_placeholder)
                    cta.visibility = View.GONE
                } else {
                    val highlightedString =
                        if (TextUtils.isEmpty(artistName)) getString(R.string.user_name_placeholder) else artistName
                    val fullString =
                        getString(R.string.playlists_other_noresults_placeholder, highlightedString)
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
            }
            Section.Favorited -> {
                imageView.setImageResource(R.drawable.ic_empty_favorites)
                if (myAccount) {
                    tvMessage.setText(R.string.favoritedplaylists_my_noresults_placeholder)
                    cta.visibility = View.GONE
                } else {
                    val highlightedString =
                        if (TextUtils.isEmpty(artistName)) getString(R.string.user_name_placeholder) else artistName
                    val fullString = getString(
                        R.string.favoritedplaylists_other_loggedout_placeholder,
                        highlightedString
                    )
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
            }
            Section.Offline -> {
                imageView.setImageResource(R.drawable.ic_empty_playlists)
                tvMessage.setText(R.string.offlineplaylists_my_noresults_placeholder)
                cta.visibility = View.GONE
            }
        }

        imageView.visibility = View.VISIBLE

        cta.setOnClickListener {
            AuthenticationActivity.show(
                context,
                LoginSignupSource.MyLibrary,
                null
            )
        }
    }

    override fun additionalTopPadding(): Int {
        return if (myAccount) 0 else context?.convertDpToPixel(10f) ?: 0
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventPlaylistEdited: EventPlaylistEdited) {
        if (recyclerViewAdapter != null && myAccount) {
            val index = recyclerViewAdapter.indexOfItemId(eventPlaylistEdited.item.itemId)
            if (index != -1) {
                recyclerViewAdapter.updateItem(eventPlaylistEdited.item, index)
                recyclerViewAdapter.notifyItemChanged(index)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventPlaylistDeleted: EventPlaylistDeleted) {
        if (recyclerViewAdapter != null && myAccount) {
            val index = recyclerViewAdapter.indexOfItemId(eventPlaylistDeleted.item.itemId)
            if (index != -1) {
                recyclerViewAdapter.removeItem(eventPlaylistDeleted.item)
                hideLoader(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.myAccount = it.getBoolean("myAccount")
            this.artistUrlSlug = it.getString("artistUrlSlug")
            this.artistName = it.getString("artistName")
            this.section = Section.valueOfOrDefault(it.getString("categoryName", ""))
        }
    }

    override fun getMixpanelSource(): MixpanelSource =
        when (parentFragment) {
            is MyLibraryFragment -> MixpanelSource(MainApplication.currentTab, MixpanelPageMyLibraryPlaylists, listOf(
                Pair(MixpanelFilterType, if (section == Section.Mine) "My Playlists" else "Favorited Playlists")
            ))
            else -> MixpanelSource(MainApplication.currentTab, MixpanelPageProfilePlaylists)
        }

    companion object {
        private const val TAG = "DataPlaylistsFragment"

        fun newInstance(
            myAccount: Boolean,
            artistUrlSlug: String?,
            artistName: String?,
            categoryName: String?
        ) = DataPlaylistsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("myAccount", myAccount)
                    putString("artistUrlSlug", artistUrlSlug)
                    putString("artistName", artistName)
                    putString("categoryName", categoryName)
                }
            }
    }
}
