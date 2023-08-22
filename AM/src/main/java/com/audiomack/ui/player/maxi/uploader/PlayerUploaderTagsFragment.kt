package com.audiomack.ui.player.maxi.uploader

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.audiomack.R
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.ArtistWithBadge
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.SearchType
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.askFollowNotificationPermissions
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.openUrlInAudiomack
import com.audiomack.utils.showFollowedToast
import com.audiomack.utils.showLoggedOutAlert
import com.audiomack.utils.showOfflineAlert
import com.audiomack.utils.spannableStringWithImageAtTheEnd
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.buttonFollow
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.imageViewAvatar
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.recyclerViewTags
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.tagsSeparator
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.tvFollowers
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.tvTags
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.tvUploader

class PlayerUploaderTagsFragment : TrackedFragment(R.layout.fragment_player_uploader_tags, TAG) {

    private val viewModel by viewModels<PlayerUploaderViewModel>()
    private var tagsAdapter: TagsAdapter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initTagsAdapter()
        initClickListeners()
        initViewModelObservers()
    }

    private fun initTagsAdapter() {
        tagsAdapter = TagsAdapter { viewModel.onTagClicked(it) }
        with(recyclerViewTags) {
            adapter = tagsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun initClickListeners() {
        buttonFollow.setOnClickListener { viewModel.onFollowTapped(mixpanelSource) }
        imageViewAvatar.setOnClickListener { viewModel.onUploaderTapped() }
        tvUploader.setOnClickListener { viewModel.onUploaderTapped() }
        tvFollowers.setOnClickListener { viewModel.onUploaderTapped() }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            name.observe(viewLifecycleOwner, nameObserver)
            followers.observe(viewLifecycleOwner, folloersObserver)
            avatar.observe(viewLifecycleOwner, avatarObserver)
            followStatus.observe(viewLifecycleOwner, followStatusObserver)
            followVisible.observe(viewLifecycleOwner, followVisibleObserver)
            notifyFollowToast.observe(viewLifecycleOwner, notifyFollowToastObserver)
            offlineAlert.observe(viewLifecycleOwner, offlineAlertObserver)
            loggedOutAlert.observe(viewLifecycleOwner, loggedOutAlertObserver)
            openInternalUrlEvent.observe(viewLifecycleOwner, openUrlObserver)
            promptNotificationPermissionEvent.observe(
                viewLifecycleOwner,
                promptNotificationPermissionObserver
            )
            tagsWithGenre.observe(viewLifecycleOwner, tagsWithGenreObserver)
            genreEvent.observe(viewLifecycleOwner, genreEventObserver)
            tagEvent.observe(viewLifecycleOwner, tagEventObserver)
        }
    }

    private val nameObserver = Observer<ArtistWithBadge> { artistWithBadge ->
        when {
            artistWithBadge.verified -> tvUploader.text =
                tvUploader.spannableStringWithImageAtTheEnd(
                    artistWithBadge.name,
                    R.drawable.ic_verified,
                    12
                )
            artistWithBadge.tastemaker -> tvUploader.text =
                tvUploader.spannableStringWithImageAtTheEnd(
                    artistWithBadge.name,
                    R.drawable.ic_tastemaker,
                    12
                )
            artistWithBadge.authenticated -> tvUploader.text =
                tvUploader.spannableStringWithImageAtTheEnd(
                    artistWithBadge.name,
                    R.drawable.ic_authenticated,
                    12
                )
            else -> tvUploader.text = artistWithBadge.name
        }
    }

    private val folloersObserver = Observer<String> { followers ->
        tvFollowers.text = followers
    }

    private val avatarObserver = Observer<String?> { avatar ->
        if (avatar.isNullOrBlank()) {
            imageViewAvatar.setImageResource(R.drawable.profile_placeholder)
        } else {
            PicassoImageLoader.load(context, avatar, imageViewAvatar)
        }
    }

    private val followStatusObserver = Observer<Boolean> { followed ->
        context?.let {
            buttonFollow.background = buttonFollow.context.drawableCompat(
                if (followed) R.drawable.profile_header_following_bg else R.drawable.profile_header_follow_bg
            )
            buttonFollow.text =
                if (followed) getString(R.string.artistinfo_unfollow) else getString(R.string.artistinfo_follow)
        }
    }

    private val followVisibleObserver = Observer<Boolean> { visible ->
        buttonFollow.isVisible = visible
    }

    private val notifyFollowToastObserver = Observer<ToggleFollowResult.Notify> { followNotify ->
        showFollowedToast(followNotify)
    }

    private val offlineAlertObserver = Observer<Void> {
        showOfflineAlert()
    }

    private val loggedOutAlertObserver = Observer<LoginSignupSource> { loginSignupSource ->
        showLoggedOutAlert(loginSignupSource)
    }

    private val openUrlObserver = Observer<String> { urlString ->
        context?.openUrlInAudiomack(urlString)
    }

    private val promptNotificationPermissionObserver = Observer<PermissionRedirect> {
        askFollowNotificationPermissions(it)
    }

    private val tagsWithGenreObserver = Observer<List<String>> { tags ->
        tagsAdapter?.submitList(tags)
        recyclerViewTags.isVisible = tags.isNotEmpty()
        tvTags.isVisible = tags.isNotEmpty()
        tagsSeparator.isVisible = tags.isNotEmpty()
    }

    private val genreEventObserver = Observer<String> {
        context?.openUrlInAudiomack("audiomack://music_${it}_trending")
        (activity as? HomeActivity)?.playerViewModel?.onMinimizeClick()
    }

    private val tagEventObserver = Observer<String> {
        (activity as? HomeActivity)?.openSearch(it, SearchType.Tag)
    }

    private val mixpanelSource: MixpanelSource
        get() = viewModel.currentSong?.mixpanelSource ?: MixpanelSource.empty

    companion object {
        const val TAG = "PlayerUploaderTagsFrag"

        fun newInstance() = PlayerUploaderTagsFragment()
    }
}
