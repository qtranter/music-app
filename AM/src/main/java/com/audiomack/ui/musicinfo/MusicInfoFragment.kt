package com.audiomack.ui.musicinfo

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.data.tracking.mixpanel.MixpanelPageMusicInfo
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.model.ReportContentModel
import com.audiomack.model.SearchType
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.imagezoom.ImageZoomFragment
import com.audiomack.ui.report.ReportContentActivity
import com.audiomack.utils.AMClickableSpan
import com.audiomack.utils.askFollowNotificationPermissions
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.featArtists
import com.audiomack.utils.showFollowedToast
import com.audiomack.utils.showLoggedOutAlert
import com.audiomack.utils.showOfflineAlert
import com.audiomack.utils.spannableString
import com.audiomack.views.AMSnackbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_musicinfo.btnReport
import kotlinx.android.synthetic.main.fragment_musicinfo.buttonClose
import kotlinx.android.synthetic.main.fragment_musicinfo.buttonFollow
import kotlinx.android.synthetic.main.fragment_musicinfo.dividerRanks
import kotlinx.android.synthetic.main.fragment_musicinfo.imageView
import kotlinx.android.synthetic.main.fragment_musicinfo.imageViewVerified
import kotlinx.android.synthetic.main.fragment_musicinfo.layoutAddedOn
import kotlinx.android.synthetic.main.fragment_musicinfo.layoutAlbum
import kotlinx.android.synthetic.main.fragment_musicinfo.layoutGenre
import kotlinx.android.synthetic.main.fragment_musicinfo.layoutLastUpdated
import kotlinx.android.synthetic.main.fragment_musicinfo.layoutNumberOfSongs
import kotlinx.android.synthetic.main.fragment_musicinfo.layoutPlaylistCreator
import kotlinx.android.synthetic.main.fragment_musicinfo.layoutProducer
import kotlinx.android.synthetic.main.fragment_musicinfo.layoutRanks
import kotlinx.android.synthetic.main.fragment_musicinfo.tvAdded
import kotlinx.android.synthetic.main.fragment_musicinfo.tvAddedOn
import kotlinx.android.synthetic.main.fragment_musicinfo.tvAlbum
import kotlinx.android.synthetic.main.fragment_musicinfo.tvAllTimeNumber
import kotlinx.android.synthetic.main.fragment_musicinfo.tvArtist
import kotlinx.android.synthetic.main.fragment_musicinfo.tvDescription
import kotlinx.android.synthetic.main.fragment_musicinfo.tvFavs
import kotlinx.android.synthetic.main.fragment_musicinfo.tvFeat
import kotlinx.android.synthetic.main.fragment_musicinfo.tvGenre
import kotlinx.android.synthetic.main.fragment_musicinfo.tvLastUpdated
import kotlinx.android.synthetic.main.fragment_musicinfo.tvMonthNumber
import kotlinx.android.synthetic.main.fragment_musicinfo.tvNumberOfSongs
import kotlinx.android.synthetic.main.fragment_musicinfo.tvPlaylistCreator
import kotlinx.android.synthetic.main.fragment_musicinfo.tvPlaylists
import kotlinx.android.synthetic.main.fragment_musicinfo.tvPlays
import kotlinx.android.synthetic.main.fragment_musicinfo.tvProducer
import kotlinx.android.synthetic.main.fragment_musicinfo.tvReups
import kotlinx.android.synthetic.main.fragment_musicinfo.tvTitle
import kotlinx.android.synthetic.main.fragment_musicinfo.tvTodayNumber
import kotlinx.android.synthetic.main.fragment_musicinfo.tvWeekNumber
import timber.log.Timber

class MusicInfoFragment : TrackedFragment(R.layout.fragment_musicinfo, TAG) {

    private lateinit var item: AMResultItem
    private val viewModel: MusicInfoViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!::item.isInitialized) {
            activity?.onBackPressed()
            return
        }

        viewModel.initItem(item)
        viewModel.closeEvent.observe(viewLifecycleOwner, Observer {
            (activity as? BaseActivity)?.popFragment() ?: activity?.onBackPressed()
        })
        viewModel.imageEvent.observe(viewLifecycleOwner, Observer {
            (activity as? BaseActivity)?.openImageZoomFragment(ImageZoomFragment.newInstance(viewModel.getImage()))
        })
        viewModel.artistEvent.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrBlank()) {
                activity?.onBackPressed()
                HomeActivity.instance?.openSearch(it.trim(), SearchType.MusicInfo)
            }
        })
        viewModel.uploaderEvent.observe(viewLifecycleOwner, Observer {
            it?.let { uploaderSlug ->
                activity?.onBackPressed()
                HomeActivity.instance?.homeViewModel?.onArtistScreenRequested(uploaderSlug)
            }
        })
        viewModel.followStatus.observe(viewLifecycleOwner, Observer {
            it?.let { artistFollowed ->
                buttonFollow.setImageDrawable(buttonFollow.context.drawableCompat(if (artistFollowed) R.drawable.player_unfollow else R.drawable.player_follow))
            }
        })
        viewModel.notifyFollowToast.observe(viewLifecycleOwner, Observer {
            showFollowedToast(it)
        })
        viewModel.offlineAlert.observe(viewLifecycleOwner, Observer {
            showOfflineAlert()
        })
        viewModel.loggedOutAlert.observe(viewLifecycleOwner, Observer { loginSignupSource ->
            showLoggedOutAlert(loginSignupSource)
        })
        viewModel.updatedItemEvent.observe(viewLifecycleOwner, Observer {

            viewModel.imageLoader.load(
                activity,
                viewModel.getImage(),
                imageView,
                R.drawable.ic_artwork
            )
            tvArtist.text = viewModel.getArtist()
            tvArtist.visibility = if (viewModel.isArtistVisible()) View.VISIBLE else View.GONE
            tvTitle.text = viewModel.getTitle()

            when {
                viewModel.isUploaderVerified() -> {
                    imageViewVerified.setImageResource(R.drawable.ic_verified)
                    imageViewVerified.visibility = View.VISIBLE
                }
                viewModel.isUploaderTastemaker() -> {
                    imageViewVerified.setImageResource(R.drawable.ic_tastemaker)
                    imageViewVerified.visibility = View.VISIBLE
                }
                viewModel.isUploaderAuthenticated() -> {
                    imageViewVerified.setImageResource(R.drawable.ic_authenticated)
                    imageViewVerified.visibility = View.VISIBLE
                }
                else -> imageViewVerified.visibility = View.GONE
            }

            viewModel.getFeat()?.let {
                val artists = it.featArtists
                val clickHandlers = artists.map { artist ->
                    AMClickableSpan(tvFeat.context) { viewModel.onFeatNameTapped(artist) }
                }
                val featString = "${getString(R.string.feat)} ${viewModel.getFeat()}"
                val featSpannableString = tvFeat.context.spannableString(
                    fullString = featString,
                    highlightedStrings = artists,
                    highlightedColor = tvFeat.context.colorCompat(R.color.orange),
                    clickableSpans = clickHandlers
                )
                tvFeat.text = featSpannableString
                try {
                    tvFeat.movementMethod = LinkMovementMethod.getInstance()
                } catch (e: NoSuchMethodError) {
                    Timber.w(e)
                }
            }
            tvFeat.visibility = if (viewModel.isFeatVisible()) View.VISIBLE else View.GONE

            val addedString = "${getString(R.string.by)} ${viewModel.getAddedBy()}"
            val addedSpannableString = tvAdded.context.spannableString(
                fullString = addedString,
                highlightedStrings = listOf(viewModel.getAddedBy() ?: ""),
                highlightedColor = tvAdded.context.colorCompat(R.color.orange)
            )
            tvAdded.text = addedSpannableString

            buttonFollow.visibility =
                if (viewModel.isFollowButtonVisible()) View.VISIBLE else View.GONE
            viewModel.updateFollowButton()

            tvAlbum.text = viewModel.getAlbum()
            layoutAlbum.visibility = if (viewModel.isAlbumVisible()) View.VISIBLE else View.GONE

            tvProducer.text = viewModel.getProducer()
            layoutProducer.visibility =
                if (viewModel.isProducerVisible()) View.VISIBLE else View.GONE

            tvAddedOn.text = viewModel.getAddedOn()
            layoutAddedOn.visibility = if (viewModel.isAddedOnVisible()) View.VISIBLE else View.GONE

            tvGenre.text = viewModel.getGenre(tvGenre.context)
            layoutGenre.visibility =
                if (viewModel.isGenreVisible(tvGenre.context)) View.VISIBLE else View.GONE

            tvDescription.text = viewModel.getDescription()
            tvDescription.visibility =
                if (viewModel.isDescriptionVisible()) View.VISIBLE else View.GONE

            tvPlaylistCreator.text = viewModel.getPlaylistCreator()
            layoutPlaylistCreator.visibility =
                if (viewModel.isPlaylistCreatorVisible()) View.VISIBLE else View.GONE

            tvNumberOfSongs.text = viewModel.getPlaylistTracksCount()
            layoutNumberOfSongs.visibility =
                if (viewModel.isPlaylistTracksCountVisible()) View.VISIBLE else View.GONE

            tvLastUpdated.text = viewModel.getLastUpdated()
            layoutLastUpdated.visibility =
                if (viewModel.isLastUpdatedVisible()) View.VISIBLE else View.GONE

            tvPlays.text = viewModel.getPlays()
            tvFavs.text = viewModel.getFavorites()
            tvReups.text = viewModel.getReposts()
            tvPlaylists.text = viewModel.getPlaylists()

            tvTodayNumber.text = viewModel.getRankDaily()
            tvWeekNumber.text = viewModel.getRankWeekly()
            tvMonthNumber.text = viewModel.getRankMonthly()
            tvAllTimeNumber.text = viewModel.getRankAllTime()

            tvReups.visibility = if (viewModel.isReupsVisible()) View.VISIBLE else View.GONE
            tvPlaylists.visibility = if (viewModel.isPlaylistsVisible()) View.VISIBLE else View.GONE
            dividerRanks.visibility = if (viewModel.isDividerRanks()) View.VISIBLE else View.GONE
            layoutRanks.visibility = if (viewModel.isRankingVisible()) View.VISIBLE else View.GONE

            btnReport.visibility =
                if (viewModel.isFollowButtonVisible()) View.VISIBLE else View.GONE
        })
        viewModel.showReportReasonEvent.observe(viewLifecycleOwner, Observer {
            showReportContentSelection(it)
        })
        viewModel.showReportAlertEvent.observe(viewLifecycleOwner, Observer {
            btnReport.text = getString(R.string.report_content_done)
            btnReport.setTextColor(btnReport.context.colorCompat(R.color.red_error))
            btnReport.isEnabled = false
            (activity as? BaseActivity)?.let {
                AMSnackbar.Builder(activity)
                    .withSubtitle(it.getString(R.string.confirm_report_done))
                    .withDuration(Snackbar.LENGTH_SHORT)
                    .show()
            }
        })
        viewModel.promptNotificationPermissionEvent.observe(viewLifecycleOwner) {
            askFollowNotificationPermissions(it)
        }

        buttonClose.setOnClickListener { viewModel.onCloseTapped() }
        imageView.setOnClickListener { viewModel.onImageTapped() }
        tvAdded.setOnClickListener { viewModel.onUploaderTapped() }
        buttonFollow.setOnClickListener { viewModel.onFollowTapped(mixpanelSource) }
        tvArtist.setOnClickListener { viewModel.onArtistNameTapped() }
        tvPlaylistCreator.setOnClickListener { viewModel.onPlaylistCreatorTapped() }
        btnReport.setOnClickListener { viewModel.onReportTapped() }

        viewModel.updateMusicInfo()
    }

    private fun showReportContentSelection(model: ReportContentModel) {
        activity?.let {
            ReportContentActivity.show(
                it,
                model
            )
        }
    }

    private val mixpanelSource: MixpanelSource
        get() = MixpanelSource(MainApplication.currentTab, MixpanelPageMusicInfo)

    companion object {
        private const val TAG = "MusicInfoFragment"
        @JvmStatic
        fun newInstance(item: AMResultItem): MusicInfoFragment {
            val fragment = MusicInfoFragment()
            fragment.item = item
            return fragment
        }
    }
}
