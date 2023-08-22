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
import com.audiomack.data.database.MusicDAOImpl
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibrarySearchOffline
import com.audiomack.model.AMArtist
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.CellType
import com.audiomack.model.MixpanelSource
import com.audiomack.model.SearchType
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.mylibrary.search.MyLibrarySearchViewModel
import com.audiomack.utils.convertDpToPixel
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import timber.log.Timber

class DataMyLibrarySearchDownloadsFragment : DataFragment(TAG) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewModelProvider(requireParentFragment()).get(MyLibrarySearchViewModel::class.java).apply {
            searchQuery.observe(viewLifecycleOwner, Observer { changedQuery(it) })
        }
    }

    override fun apiCallObservable(): APIRequestData? {
        super.apiCallObservable()
        return if (!query.isNullOrEmpty() && currentPage == 0) {
            APIRequestData(MusicDAOImpl().querySavedItems(query ?: "").flatMap { Observable.just(APIResponseData(it, null)) }, null)
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
            tvMessage.setText(R.string.library_search_downloads_placeholder)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.query = it.getString("query")
        }
    }

    override fun getMixpanelSource(): MixpanelSource =
        MixpanelSource(MainApplication.currentTab, MixpanelPageMyLibrarySearchOffline)

    companion object {
        private const val TAG = "DataMyLibrarySearchDownloadsFragment"

        @JvmStatic
        fun newInstance(query: String): DataMyLibrarySearchDownloadsFragment {
            return DataMyLibrarySearchDownloadsFragment().apply {
                arguments = Bundle().apply {
                    putString("query", query)
                }
            }
        }
    }
}
