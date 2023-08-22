package com.audiomack.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import com.audiomack.DASHBOARD_URL
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.sizes.SizesRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonKebabMenu
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryUploads
import com.audiomack.data.tracking.mixpanel.MixpanelPageProfileUploads
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.Action
import com.audiomack.model.CellType
import com.audiomack.model.Credentials
import com.audiomack.model.EventHighlightsUpdated
import com.audiomack.model.EventUploadDeleted
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.network.AnalyticsHelper
import com.audiomack.ui.highlights.EditHighlightsActivity
import com.audiomack.ui.highlights.HighlightsHeaderView
import com.audiomack.ui.highlights.HighlightsInterface
import com.audiomack.ui.mylibrary.MyLibraryFragment
import com.audiomack.ui.settings.OptionsMenuFragment
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.openUrlExcludingAudiomack
import com.audiomack.utils.spannableString
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DataUploadsFragment : DataFragment(TAG) {

    var myAccount: Boolean = false
    private var artistUrlSlug: String? = null
    var artistName: String? = null

    private var highlightsHeaderView: HighlightsHeaderView? = null

    private var highlightsDisposable: Disposable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (myAccount) {
            val screen = "My Uploads"
            AnalyticsHelper.getInstance().trackScreen(screen)
        }
        showRepostInfo = true

        viewModel.removeHighlightAtPositionEvent.observe(viewLifecycleOwner, Observer { position ->
            highlightsHeaderView?.removeItem(position)
        })
    }

    override fun onResume() {
        super.onResume()
        highlightsHeaderView?.updateRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()
        highlightsDisposable?.dispose()
    }

    override fun apiCallObservable(): APIRequestData? {
        super.apiCallObservable()
        if (artistUrlSlug != null) {
            if (!myAccount && currentPage == 0) {
                downloadHighlights()
            }
            return API.getInstance().getArtistUploads(artistUrlSlug, currentPage, !myAccount)
        }
        return APIRequestData(Observable.just(APIResponseData()), null)
    }

    override fun getCellType(): CellType {
        return CellType.MUSIC_BROWSE_SMALL
    }

    override fun canShowUpsellView() = parentFragment is MyLibraryFragment

    override fun recyclerViewHeader(): View? {
        if (!myAccount) {
            val itsMe = TextUtils.equals(UserRepository.getInstance().getUserSlug(), artistUrlSlug)
            highlightsHeaderView = HighlightsHeaderView(requireContext())
            highlightsHeaderView?.myAccount = itsMe
            highlightsHeaderView?.highlightsInterface = object : HighlightsInterface {
                override fun didTapOnEdit() {
                    EditHighlightsActivity.show(activity)
                }

                override fun didTapOnMusic(music: AMResultItem, highlights: List<AMResultItem>) {
                    // Load highlighted music + recent music in the queue
                    openMusic(music, null, highlights)
                }

                override fun didTapOnMusicMenu(music: AMResultItem, position: Int) {
                    val actions = listOf(
                        Action(
                            getString(R.string.highlights_remove),
                            object : Action.ActionListener {
                                override fun onActionExecuted() {
                                    (activity as? BaseActivity)?.popFragment()
                                    viewModel.onHighlightRemoved(
                                        music,
                                        position,
                                        MixpanelButtonKebabMenu,
                                        mixpanelSource
                                    )
                                }
                            })
                    )
                    (activity as? BaseActivity)?.openOptionsFragment(
                        OptionsMenuFragment.newInstance(
                            actions
                        )
                    )
                }
            }
            return highlightsHeaderView
        }
        return null
    }

    override fun placeholderCustomView(): View {
        return LayoutInflater.from(context).inflate(R.layout.view_placeholder, null)
    }

    override fun configurePlaceholderView(placeholderView: View) {
        val imageView = placeholderView.findViewById<ImageView>(R.id.imageView)
        val tvMessage = placeholderView.findViewById<TextView>(R.id.tvMessage)
        val cta = placeholderView.findViewById<Button>(R.id.cta)
        imageView.setImageResource(R.drawable.ic_empty_uploads)
        if (myAccount) {
            tvMessage.setText(R.string.uploads_my_noresults_placeholder)
            cta.setText(R.string.uploads_my_noresults_highlighted_placeholder)
            cta.visibility = View.VISIBLE
        } else {
            val highlightedString =
                if (TextUtils.isEmpty(artistName)) getString(R.string.user_name_placeholder) else artistName
            val fullString =
                getString(R.string.uploads_other_noresults_placeholder, highlightedString)
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
        val imageVisible = SizesRepository.screenHeightDp > 520
        imageView.visibility = if (imageVisible) View.VISIBLE else View.GONE
        cta.setOnClickListener {
            context?.openUrlExcludingAudiomack(DASHBOARD_URL)
        }
    }

    override fun didRemoveGeorestrictedItem(item: AMResultItem) {
        artistUrlSlug?.takeIf { Credentials.load(activity)?.userUrlSlug == it && it != item.uploaderSlug }
            ?.let { slug ->
                viewModel.removeGeorestrictedItemFromUploads(
                    slug,
                    item,
                    mixpanelSource,
                    MixpanelButtonList
                )
            }
    }

    private fun downloadHighlights() {
        val itsMe = TextUtils.equals(artistUrlSlug, UserRepository.getInstance().getUserSlug())
        highlightsDisposable =
            MusicRepository().getHighlights(artistUrlSlug ?: "", itsMe)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ amResultItems ->
                    highlightsHeaderView?.updateHighlights(amResultItems)
                }, { })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventHighlightsUpdated: EventHighlightsUpdated) {
        if (!myAccount) {
            downloadHighlights()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventUploadDeleted: EventUploadDeleted) {
        if (!myAccount && Credentials.load(activity)?.userUrlSlug == artistUrlSlug) {
            recyclerViewAdapter.removeItem(eventUploadDeleted.item)
            downloadHighlights()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.myAccount = it.getBoolean("myAccount")
            this.artistUrlSlug = it.getString("artistUrlSlug")
            this.artistName = it.getString("artistName")
        }
    }

    override fun getMixpanelSource(): MixpanelSource {
        val page = when (parentFragment) {
            is MyLibraryFragment -> MixpanelPageMyLibraryUploads
            else -> MixpanelPageProfileUploads
        }
        return MixpanelSource(MainApplication.currentTab, page)
    }

    companion object {
        private const val TAG = "DataUploadsFragment"

        fun newInstance(
            myAccount: Boolean,
            artistUrlSlug: String?,
            artistName: String?
        ) = DataUploadsFragment().apply {
            arguments = Bundle().apply {
                putBoolean("myAccount", myAccount)
                putString("artistUrlSlug", artistUrlSlug)
                putString("artistName", artistName)
            }
        }
    }
}
