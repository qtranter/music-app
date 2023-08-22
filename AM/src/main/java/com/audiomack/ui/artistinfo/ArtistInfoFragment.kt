package com.audiomack.ui.artistinfo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.data.tracking.mixpanel.MixpanelButtonArtistInfo
import com.audiomack.data.tracking.mixpanel.MixpanelPageArtistInfo
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMArtist
import com.audiomack.model.MixpanelSource
import com.audiomack.model.ReportContentModel
import com.audiomack.model.ReportType
import com.audiomack.ui.imagezoom.ImageZoomFragment
import com.audiomack.ui.report.ReportContentActivity
import com.audiomack.utils.askFollowNotificationPermissions
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.showFollowedToast
import com.audiomack.utils.showLoggedOutAlert
import com.audiomack.utils.showOfflineAlert
import com.audiomack.utils.spannableStringWithImageAtTheEnd
import com.audiomack.views.AMSnackbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_artistinfo.btnBlock
import kotlinx.android.synthetic.main.fragment_artistinfo.btnReport
import kotlinx.android.synthetic.main.fragment_artistinfo.buttonClose
import kotlinx.android.synthetic.main.fragment_artistinfo.buttonFacebook
import kotlinx.android.synthetic.main.fragment_artistinfo.buttonFollow
import kotlinx.android.synthetic.main.fragment_artistinfo.buttonInstagram
import kotlinx.android.synthetic.main.fragment_artistinfo.buttonShare
import kotlinx.android.synthetic.main.fragment_artistinfo.buttonTwitter
import kotlinx.android.synthetic.main.fragment_artistinfo.buttonYoutube
import kotlinx.android.synthetic.main.fragment_artistinfo.imageView
import kotlinx.android.synthetic.main.fragment_artistinfo.layoutBio
import kotlinx.android.synthetic.main.fragment_artistinfo.layoutGenre
import kotlinx.android.synthetic.main.fragment_artistinfo.layoutHometown
import kotlinx.android.synthetic.main.fragment_artistinfo.layoutLabel
import kotlinx.android.synthetic.main.fragment_artistinfo.layoutMemberSince
import kotlinx.android.synthetic.main.fragment_artistinfo.layoutPlays
import kotlinx.android.synthetic.main.fragment_artistinfo.layoutWebsite
import kotlinx.android.synthetic.main.fragment_artistinfo.tvBio
import kotlinx.android.synthetic.main.fragment_artistinfo.tvFollowers
import kotlinx.android.synthetic.main.fragment_artistinfo.tvFollowing
import kotlinx.android.synthetic.main.fragment_artistinfo.tvGenre
import kotlinx.android.synthetic.main.fragment_artistinfo.tvHometown
import kotlinx.android.synthetic.main.fragment_artistinfo.tvLabel
import kotlinx.android.synthetic.main.fragment_artistinfo.tvMemberSince
import kotlinx.android.synthetic.main.fragment_artistinfo.tvName
import kotlinx.android.synthetic.main.fragment_artistinfo.tvPlays
import kotlinx.android.synthetic.main.fragment_artistinfo.tvSlug
import kotlinx.android.synthetic.main.fragment_artistinfo.tvWebsite
import timber.log.Timber

class ArtistInfoFragment : TrackedFragment(R.layout.fragment_artistinfo, TAG) {

    lateinit var artist: AMArtist
    private val viewModel: ArtistInfoViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.initArtist(artist)
        viewModel.closeEvent.observe(viewLifecycleOwner) {
            (activity as? BaseActivity)?.popFragment() ?: activity?.onBackPressed()
        }
        viewModel.imageEvent.observe(viewLifecycleOwner) {
            (activity as? BaseActivity)?.openImageZoomFragment(ImageZoomFragment.newInstance(viewModel.image))
        }
        viewModel.openUrlEvent.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                } catch (e: Exception) {
                    Timber.w(e)
                }
            }
        }
        viewModel.shareEvent.observe(viewLifecycleOwner) {
            artist.openShareSheet(activity, mixpanelSource, MixpanelButtonArtistInfo)
        }
        viewModel.followStatus.observe(viewLifecycleOwner) {
            it?.let { followed ->
                buttonFollow.background = buttonFollow.context.drawableCompat(if (followed) R.drawable.profile_header_following_bg else R.drawable.profile_header_follow_bg)
                buttonFollow.text = if (followed) getString(R.string.artistinfo_unfollow) else getString(R.string.artistinfo_follow)
            }
        }
        viewModel.notifyFollowToast.observe(viewLifecycleOwner) { followNotify ->
            showFollowedToast(followNotify)
        }
        viewModel.offlineAlert.observe(viewLifecycleOwner) {
            showOfflineAlert()
        }
        viewModel.loggedOutAlert.observe(viewLifecycleOwner) { loginSignupSource ->
            showLoggedOutAlert(loginSignupSource)
        }
        viewModel.showReportReasonEvent.observe(viewLifecycleOwner) {
            showReportContentSelection(it)
        }
        viewModel.showReportAlertEvent.observe(viewLifecycleOwner) { reportType ->
            when (reportType) {
                ReportType.Report -> {
                    btnReport.text = getString(R.string.report_content_done)
                    btnReport.setTextColor(btnReport.context.colorCompat(R.color.red_error))
                    btnReport.isEnabled = false
                    showContentReportedSnackbar()
                }
                ReportType.Block -> {
                    btnBlock.text = getString(R.string.block_user_done)
                    btnBlock.setTextColor(btnReport.context.colorCompat(R.color.red_error))
                    btnBlock.isEnabled = false
                    showContentReportedSnackbar()
                }
            }
        }
        viewModel.promptNotificationPermissionEvent.observe(viewLifecycleOwner) {
            askFollowNotificationPermissions(it)
        }

        buttonClose.setOnClickListener { viewModel.onCloseTapped() }
        imageView.setOnClickListener { viewModel.onImageTapped() }
        buttonTwitter.setOnClickListener { viewModel.onTwitterTapped() }
        buttonFacebook.setOnClickListener { viewModel.onFacebookTapped() }
        buttonInstagram.setOnClickListener { viewModel.onInstagramTapped() }
        buttonYoutube.setOnClickListener { viewModel.onYoutubeTapped() }
        buttonShare.setOnClickListener { viewModel.onShareTapped() }
        tvWebsite.setOnClickListener { viewModel.onWebsiteTapped() }
        buttonFollow.setOnClickListener { viewModel.onFollowTapped(mixpanelSource) }
        btnReport.setOnClickListener { viewModel.onReportTapped() }
        btnBlock.setOnClickListener { viewModel.onBlockTapped() }

        viewModel.imageLoader.load(activity, viewModel.image, imageView)

        when {
            viewModel.verified -> tvName.text = tvName.spannableStringWithImageAtTheEnd(viewModel.name, R.drawable.ic_verified, 16)
            viewModel.tastemaker -> tvName.text = tvName.spannableStringWithImageAtTheEnd(viewModel.name, R.drawable.ic_tastemaker, 16)
            viewModel.authenticated -> tvName.text = tvName.spannableStringWithImageAtTheEnd(viewModel.name, R.drawable.ic_authenticated, 16)
            else -> tvName.text = viewModel.name
        }

        tvSlug.text = viewModel.slug
        tvFollowing.text = viewModel.followingExtended
        tvFollowers.text = viewModel.followersExtended
        tvPlays.text = viewModel.playsExtended
        layoutPlays.visibility = if (viewModel.playsVisible) View.VISIBLE else View.GONE

        buttonTwitter.visibility = if (viewModel.twitterVisible) View.VISIBLE else View.GONE
        buttonFacebook.visibility = if (viewModel.facebookVisible) View.VISIBLE else View.GONE
        buttonInstagram.visibility = if (viewModel.instagramVisible) View.VISIBLE else View.GONE
        buttonYoutube.visibility = if (viewModel.youtubeVisible) View.VISIBLE else View.GONE

        tvWebsite.text = viewModel.website
        layoutWebsite.visibility = if (viewModel.websiteVisible) View.VISIBLE else View.GONE

        tvGenre.text = viewModel.genre
        layoutGenre.visibility = if (viewModel.genreVisible) View.VISIBLE else View.GONE

        tvLabel.text = viewModel.label
        layoutLabel.visibility = if (viewModel.labelVisible) View.VISIBLE else View.GONE

        tvHometown.text = viewModel.hometown
        layoutHometown.visibility = if (viewModel.hometownVisible) View.VISIBLE else View.GONE

        tvMemberSince.text = viewModel.memberSince
        layoutMemberSince.visibility = if (viewModel.memberSinceVisible) View.VISIBLE else View.GONE

        tvBio.text = viewModel.bio
        layoutBio.visibility = if (viewModel.bioVisible) View.VISIBLE else View.GONE

        buttonFollow.visibility = if (viewModel.followVisible) View.VISIBLE else View.GONE

        btnReport.visibility = if (viewModel.followVisible) View.VISIBLE else View.GONE
        btnBlock.visibility = if (viewModel.followVisible) View.VISIBLE else View.GONE
    }

    private fun showReportContentSelection(model: ReportContentModel) {
        activity?.let {
            ReportContentActivity.show(
                it,
                model
            )
        }
    }

    private fun showContentReportedSnackbar() {
        activity?.let {
            AMSnackbar.Builder(it)
                .withSubtitle(it.getString(R.string.confirm_report_done))
                .withDuration(Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private val mixpanelSource: MixpanelSource
        get() = MixpanelSource(MainApplication.currentTab, MixpanelPageArtistInfo)

    companion object {
        private const val TAG = "ArtistInfoFragment"

        @JvmStatic
        fun newInstance(artist: AMArtist): ArtistInfoFragment {
            val fragment = ArtistInfoFragment()
            fragment.artist = artist
            return fragment
        }
    }
}
