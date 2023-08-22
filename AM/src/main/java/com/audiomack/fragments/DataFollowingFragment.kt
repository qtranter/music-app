package com.audiomack.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.sizes.SizesRepository
import com.audiomack.data.tracking.mixpanel.MixpanelPageFeedFollowing
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryFollowing
import com.audiomack.data.tracking.mixpanel.MixpanelPageProfileFollowing
import com.audiomack.model.APIRequestData
import com.audiomack.model.APIResponseData
import com.audiomack.model.CellType
import com.audiomack.model.Credentials
import com.audiomack.model.EventFollowChange
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.ui.feed.FeedFragment
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.mylibrary.MyLibraryFragment
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString
import io.reactivex.Observable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DataFollowingFragment : DataFragment(TAG) {

    internal var myAccount: Boolean = false
    private var artistUrlSlug: String? = null
    private var artistName: String? = null

    override fun apiCallObservable(): APIRequestData? {
        return if (artistUrlSlug != null) {
            API.getInstance()
                .getArtistFollowing(artistUrlSlug, if (currentPage == 0) null else pagingToken)
        } else {
            APIRequestData(Observable.just(APIResponseData()), null)
        }
    }

    override fun getCellType(): CellType {
        return CellType.ACCOUNT
    }

    override fun canShowUpsellView() = parentFragment is MyLibraryFragment

    override fun placeholderCustomView(): View {
        return LayoutInflater.from(context).inflate(R.layout.view_placeholder, null)
    }

    override fun configurePlaceholderView(placeholderView: View) {
        val imageView = placeholderView.findViewById<ImageView>(R.id.imageView)
        val tvMessage = placeholderView.findViewById<TextView>(R.id.tvMessage)
        val cta = placeholderView.findViewById<Button>(R.id.cta)
        imageView.setImageResource(R.drawable.ic_empty_people)
        if (myAccount) {
            tvMessage.setText(R.string.following_my_noresults_placeholder)
            cta.setText(R.string.following_my_noresults_highlighted_placeholder)
            cta.visibility = View.VISIBLE
        } else {
            val highlightedString =
                if (TextUtils.isEmpty(artistName)) getString(R.string.user_name_placeholder) else artistName
            val fullString =
                getString(R.string.following_other_noresults_placeholder, highlightedString)
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
        val imageVisible =
            parentFragment is FeedFragment || !Credentials.isLogged(context) && SizesRepository.screenHeightDp > 600 || SizesRepository.screenHeightDp > 520
        imageView.visibility = if (imageVisible) View.VISIBLE else View.GONE
        cta.setOnClickListener { (activity as? HomeActivity)?.homeViewModel?.onLinkRequested("audiomack://suggested_follows") }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventFollowChange: EventFollowChange) {
        if (myAccount) {
            recyclerViewAdapter?.notifyDataSetChanged()
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
            is FeedFragment -> MixpanelPageFeedFollowing
            is MyLibraryFragment -> MixpanelPageMyLibraryFollowing
            else -> MixpanelPageProfileFollowing
        }
        return MixpanelSource(MainApplication.currentTab, page)
    }

    companion object {
        private const val TAG = "DataFollowingFragment"

        fun newInstance(
            myAccount: Boolean,
            artistUrlSlug: String?,
            artistName: String?
        ) = DataFollowingFragment().apply {
            arguments = Bundle().apply {
                putBoolean("myAccount", myAccount)
                putString("artistUrlSlug", artistUrlSlug)
                putString("artistName", artistName)
            }
        }
    }
}
