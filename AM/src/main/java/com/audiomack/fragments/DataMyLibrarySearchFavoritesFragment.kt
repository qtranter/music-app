package com.audiomack.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibrarySearchFavorites
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.CellType
import com.audiomack.model.EventFavoriteDelete
import com.audiomack.model.MixpanelSource
import com.audiomack.model.SearchType
import com.audiomack.network.API
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.mylibrary.search.MyLibrarySearchViewModel
import com.audiomack.utils.convertDpToPixel
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class DataMyLibrarySearchFavoritesFragment : DataFragment(TAG) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewModelProvider(requireParentFragment()).get(MyLibrarySearchViewModel::class.java).apply {
            searchQuery.observe(viewLifecycleOwner, Observer { changedQuery(it) })
        }
    }

    override fun apiCallObservable(): APIRequestData? {
        super.apiCallObservable()
        return if (!query.isNullOrEmpty()) {
            API.getInstance().searchUserAccount(query, "favorites", currentPage, false)
        } else {
            APIRequestData(Observable.just(APIResponseData()), null)
        }
    }

    override fun getCellType(): CellType {
        return CellType.MUSIC_BROWSE_SMALL
    }

    override fun placeholderCustomView(): View {
        return LayoutInflater.from(context).inflate(R.layout.view_search_placeholder, null)
    }

    override fun configurePlaceholderView(placeholderView: View) {
        val imageView = placeholderView.findViewById<ImageView>(R.id.imageViewAvatar)
        val tvMessage = placeholderView.findViewById<TextView>(R.id.tvMessage)
        val cta = placeholderView.findViewById<Button>(R.id.cta)
        val artist = AMArtist.getSavedArtist()
        if (artist != null && !TextUtils.isEmpty(artist.smallImage)) {
            Picasso.get().load(artist.smallImage).into(imageView)
        } else {
            Picasso.get().load(R.drawable.profile_placeholder).into(imageView)
        }
        if (query.isNullOrEmpty()) {
            tvMessage.setText(R.string.library_search_favorites_placeholder)
            cta.visibility = View.GONE
        } else {
            tvMessage.setText(R.string.library_search_noresults_placeholder)
            cta.visibility = View.VISIBLE
        }
        cta.setOnClickListener {
            try {
                activity?.onBackPressed()
                HomeActivity.instance?.openSearch(query, SearchType.LibrarySearch)
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }

    override fun additionalTopPadding(): Int {
        return context?.convertDpToPixel(15f) ?: 0
    }

    override fun didRemoveGeorestrictedItem(item: AMResultItem) {
        viewModel.removeGeorestrictedItemFromFavorites(item, mixpanelSource, MixpanelButtonList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.query = it.getString("query")
        }
    }

    override fun getMixpanelSource(): MixpanelSource =
        MixpanelSource(MainApplication.currentTab, MixpanelPageMyLibrarySearchFavorites)

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventFavoriteDelete: EventFavoriteDelete) {
        recyclerViewAdapter?.removeItem(eventFavoriteDelete.item)
    }

    companion object {
        private const val TAG = "DataMyLibrarySearchFavoritesFragment"

        @JvmStatic
        fun newInstance(query: String): DataMyLibrarySearchFavoritesFragment {
            return DataMyLibrarySearchFavoritesFragment().apply {
                arguments = Bundle().apply {
                    putString("query", query)
                }
            }
        }
    }
}
