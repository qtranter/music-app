package com.audiomack.ui.player.maxi.morefromartist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.player.PlayerRepository
import com.audiomack.data.sizes.SizesRepository
import com.audiomack.data.tracking.mixpanel.MixpanelPageProfileUploads
import com.audiomack.fragments.DataFragment
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.CellType
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.openUrlInAudiomack
import com.audiomack.utils.spannableString
import io.reactivex.Observable

class PlayerMoreFromArtistFragment : DataFragment(TAG) {

    private lateinit var moreFromArtistViewModel: PlayerMoreFromArtistViewModel
    private var tvHeader: TextView? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        moreFromArtistViewModel = ViewModelProvider(
            this,
            PlayerMoreFromArtistViewModelFactory(PlayerRepository.getInstance())
        ).get(PlayerMoreFromArtistViewModel::class.java)

        view?.doOnLayout {
            val lp = it.layoutParams
            lp.height = SizesRepository.screenHeight -
                (64 * it.resources.displayMetrics.density).toInt() // tabs height
            val horizontalPadding = (10 * it.resources.displayMetrics.density).toInt()
            it.setPadding(horizontalPadding, 0, horizontalPadding, 0)
            it.layoutParams = lp
        }

        initViewModelObservers()
    }

    private fun initViewModelObservers() {
        moreFromArtistViewModel.apply {
            loadDataEvent.observe(viewLifecycleOwner, observerLoadData)
            uploaderName.observe(viewLifecycleOwner, observerUploaderName)
            openInternalUrlEvent.observe(viewLifecycleOwner, observerOpenUrl)
            showLoading.observe(viewLifecycleOwner, observerShowLoading)
            showNoConnection.observe(viewLifecycleOwner, observerShowNoConnection)
        }
    }

    private val observerLoadData: Observer<Void> = Observer {
        if (recyclerViewAdapter.realItemsCount == 0 && !downloading) {
            changedSettings()
        }
    }

    private val observerUploaderName: Observer<String> = Observer { uploaderName ->
        tvHeader?.text = getString(R.string.player_extra_more_from_artist_template, uploaderName)
    }

    private val observerOpenUrl: Observer<String> = Observer { urlString ->
        context?.openUrlInAudiomack(urlString)
    }

    private val observerShowLoading: Observer<Void> = Observer {
        clearAndShowLoader()
    }

    private val observerShowNoConnection: Observer<Void> = Observer {
        hideLoader(false)
    }

    // DataFragment config

    override fun getCellType() = CellType.MUSIC_BROWSE_SMALL

    override fun apiCallObservable(): APIRequestData {
        if (moreFromArtistViewModel.uploaderSlug.isEmpty()) {
            return APIRequestData(Observable.just(APIResponseData(mutableListOf(), null)), null)
        }
        val observable: Observable<APIResponseData> = API.getInstance().getArtistUploads(moreFromArtistViewModel.uploaderSlug, 0, true).observable.map {
            it.objects = it.objects.take(5)
            it
        }
        return APIRequestData(observable, null)
    }

    override fun placeholderCustomView() = LayoutInflater.from(context).inflate(R.layout.view_placeholder, null)

    override fun configurePlaceholderView(placeholderView: View) {
        val imageView = placeholderView.findViewById<ImageView>(R.id.imageView)
        val tvMessage = placeholderView.findViewById<TextView>(R.id.tvMessage)
        val cta = placeholderView.findViewById<Button>(R.id.cta)
        imageView.setImageResource(R.drawable.ic_empty_uploads)
        val highlightedString = if (moreFromArtistViewModel.uploaderName.value.isNullOrBlank()) getString(R.string.user_name_placeholder) else moreFromArtistViewModel.uploaderName.value
        val fullString =
            getString(R.string.uploads_other_noresults_placeholder, highlightedString)
        tvMessage.text = context?.spannableString(
            fullString = fullString,
            highlightedStrings = listOf(highlightedString ?: ""),
            fullColor = tvMessage.context.colorCompat(R.color.placeholder_gray),
            highlightedColor = tvMessage.context.colorCompat(android.R.color.white),
            fullFont = R.font.opensans_regular,
            highlightedFont = R.font.opensans_semibold
        )
        cta.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        cta.setOnClickListener {
            moreFromArtistViewModel.onPlaceholderTapped()
        }
    }

    override fun recyclerViewHeader(): View {
        val headerView = LayoutInflater.from(context).inflate(R.layout.header_more_from_artist, null)
        this.tvHeader = headerView.findViewById(R.id.tvHeader)
        return headerView
    }

    override fun footerLayoutResId() = R.layout.row_footer_more_from_artist

    override fun onClickFooter() {
        moreFromArtistViewModel.onFooterTapped()
    }

    override fun getMixpanelSource(): MixpanelSource =
        MixpanelSource(MainApplication.currentTab, MixpanelPageProfileUploads)

    // Static

    companion object {
        private const val TAG = "PlayerMoreFromArtistFragment"
        fun newInstance() = PlayerMoreFromArtistFragment()
    }
}
