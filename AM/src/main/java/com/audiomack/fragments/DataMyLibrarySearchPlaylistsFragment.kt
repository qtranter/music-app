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
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibrarySearchPlaylists
import com.audiomack.model.AMArtist
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.CellType
import com.audiomack.model.MixpanelSource
import com.audiomack.model.SearchType
import com.audiomack.network.API
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.mylibrary.search.MyLibrarySearchViewModel
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import timber.log.Timber

class DataMyLibrarySearchPlaylistsFragment : DataFragment(TAG) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewModelProvider(requireParentFragment()).get(MyLibrarySearchViewModel::class.java).apply {
            searchQuery.observe(viewLifecycleOwner, Observer { changedQuery(it) })
        }
    }

    override fun apiCallObservable(): APIRequestData? {
        super.apiCallObservable()
        return if (!query.isNullOrEmpty()) {
            API.getInstance().searchUserAccount(query, "playlists", currentPage, false)
        } else {
            APIRequestData(Observable.just(APIResponseData()), null)
        }
    }

    override fun getCellType() = CellType.MUSIC_TINY

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
        if (TextUtils.isEmpty(query)) {
            tvMessage.setText(R.string.library_search_playlists_placeholder)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.query = it.getString("query")
        }
    }

    override fun getMixpanelSource(): MixpanelSource =
        MixpanelSource(MainApplication.currentTab, MixpanelPageMyLibrarySearchPlaylists)

    companion object {
        private const val TAG = "DataMyLibrarySearchPlaylistsFragment"

        @JvmStatic
        fun newInstance(query: String): DataMyLibrarySearchPlaylistsFragment {
            return DataMyLibrarySearchPlaylistsFragment().apply {
                arguments = Bundle().apply {
                    putString("query", query)
                }
            }
        }
    }
}
