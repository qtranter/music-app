package com.audiomack.ui.artist

import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.SparseArray
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.viewpager.widget.ViewPager
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.tracking.mixpanel.MixpanelButtonProfile
import com.audiomack.data.tracking.mixpanel.MixpanelPageProfile
import com.audiomack.data.user.UserData
import com.audiomack.fragments.BaseTabHostFragment
import com.audiomack.fragments.DataFavoritesFragment
import com.audiomack.fragments.DataFollowersFragment
import com.audiomack.fragments.DataFollowingFragment
import com.audiomack.fragments.DataFragment
import com.audiomack.fragments.DataPlaylistsFragment
import com.audiomack.fragments.DataUploadsFragment
import com.audiomack.fragments.EmptyFragment
import com.audiomack.model.AMArtist
import com.audiomack.model.EventFollowChange
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.imagezoom.ImageZoomFragment
import com.audiomack.utils.askFollowNotificationPermissions
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.showFollowedToast
import com.audiomack.utils.showLoggedOutAlert
import com.audiomack.utils.showOfflineAlert
import com.audiomack.utils.spannableStringWithImageAtTheEnd
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min
import kotlinx.android.synthetic.main.fragment_artist.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

// TODO needs to be fully converted to MVVM.
// As of today ArtistViewModel only contains the "follow" logic.
class ArtistFragment : BaseTabHostFragment(TAG) {

    private val viewModel: ArtistViewModel by viewModels()

    protected var disposables = CompositeDisposable()

    private var tabsAdapter: TabsAdapter? = null

    private var artist: AMArtist? = null
    private var showBackButton: Boolean = false
    private var deeplinkTab: String? = null
    private var deeplinkPlaylistsCategory: String? = null
    private var openShare: Boolean = false

    private val tabs: List<String> by lazy {
        listOf(
            MainApplication.context?.getString(R.string.artist_tab_uploads) ?: "",
            MainApplication.context?.getString(R.string.artist_tab_favorites) ?: "",
            MainApplication.context?.getString(R.string.artist_tab_playlists) ?: "",
            MainApplication.context?.getString(R.string.artist_tab_followers) ?: "",
            MainApplication.context?.getString(R.string.artist_tab_following) ?: ""
        )
    }

    val expandedHeaderHeight: Int
        get() = (topLayout!!.parent as LinearLayout).height

    val collapsedHeaderHeight: Int
        get() = navigationBar!!.height + tabLayout!!.height

    val currentHeaderHeight: Int
        get() {
            val parent = topLayout!!.parent as LinearLayout
            return parent.height + (parent.layoutParams as FrameLayout.LayoutParams).topMargin
        }

    override val topLayoutHeight: Int
        get() = expandedHeaderHeight

    val mixpanelSource: MixpanelSource
        get() = MixpanelSource(MainApplication.currentTab, MixpanelPageProfile)

    fun setArtist(artist: AMArtist) {
        this.artist = artist
    }

    fun isDisplayingSameData(artist: AMArtist): Boolean {
        return this.artist?.artistId == artist.artistId
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_artist, container, false)
        topLayout = view.findViewById(R.id.topLayout)
        tabLayout = view.findViewById(R.id.tabLayout)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        artist?.let {
            viewModel.initWithArtist(it)
        }

        initViewModelObservers()
        initClickListeners()

        if (showBackButton) {
            leftButton.visibility = View.VISIBLE
            leftButton.setOnClickListener { (activity as? HomeActivity)?.popFragment() }
        } else {
            leftButton.visibility = View.INVISIBLE
        }
        rightButton.setOnClickListener {
            artist?.let { (activity as? HomeActivity)?.openArtistMore(it) }
        }
        buttonInfo.setOnClickListener {
            artist?.let { (activity as? HomeActivity)?.openArtistMore(it) }
        }
        buttonShare.setOnClickListener { artist?.openShareSheet(activity, mixpanelSource, MixpanelButtonProfile) }
        buttonNavShare.setOnClickListener { artist?.openShareSheet(activity, mixpanelSource, MixpanelButtonProfile) }

        avatarImageView.visibility = View.INVISIBLE
        heroLayout.post {
            val headerLp = headerLayout?.layoutParams as? FrameLayout.LayoutParams ?: return@post
            headerLp.topMargin = heroLayout.height
            headerLayout.layoutParams = headerLp
            val avatarLp = avatarImageView.layoutParams as FrameLayout.LayoutParams
            avatarLp.topMargin = heroLayout.height - avatarImageView.context.convertDpToPixel(26f)
            avatarImageView.layoutParams = avatarLp
            avatarImageView.visibility = View.VISIBLE
        }

        Handler().postDelayed({

            if (!isAdded) {
                return@postDelayed
            }

            tabsAdapter = TabsAdapter(childFragmentManager, tabs)
            viewPager.adapter = tabsAdapter
            tabLayout?.setupWithViewPager(viewPager)
            viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {}

                override fun onPageScrollStateChanged(state: Int) {
                    if (state == ViewPager.SCROLL_STATE_DRAGGING || state == ViewPager.SCROLL_STATE_SETTLING) {
                        tabsAdapter?.notifyPages(viewPager.currentItem)
                    }
                }
            })

            val pageIndex = when (deeplinkTab) {
                "uploads" -> 0
                "favorites" -> 1
                "playlists" -> 2
                "followers" -> 3
                "following" -> 4
                else -> 0
            }
            if (pageIndex != 0) {
                viewPager.setCurrentItem(pageIndex, false)
            }
            deeplinkTab = null
        }, 30)

        if (openShare) {
            buttonShare.callOnClick()
        }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            followStatus.observe(viewLifecycleOwner) { updateFollowButton(it) }
            notifyFollowToastEvent.observe(viewLifecycleOwner) { showFollowedToast(it) }
            offlineAlertEvent.observe(viewLifecycleOwner) { showOfflineAlert() }
            loggedOutAlertEvent.observe(viewLifecycleOwner) { showLoggedOutAlert(it) }
            promptNotificationPermissionEvent.observe(viewLifecycleOwner) { askFollowNotificationPermissions(it) }
        }
    }

    private fun initClickListeners() {
        buttonFollow.setOnClickListener { viewModel.onFollowTapped(mixpanelSource) }
        buttonFollowSmall.setOnClickListener { viewModel.onFollowTapped(mixpanelSource) }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        disposables.add(
            API.getInstance().getArtistInfo(artist!!.urlSlug)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ amArtist ->
                    this@ArtistFragment.artist = amArtist
                    showUserData()
                }, {})
        )

        showUserData()
    }

    private fun showUserData() {
        val artist = artist ?: return

        val name = when {
            artist.isVerified -> tvName.spannableStringWithImageAtTheEnd(
                artist.name,
                R.drawable.ic_verified,
                16
            )
            artist.isTastemaker -> tvName.spannableStringWithImageAtTheEnd(
                artist.name,
                R.drawable.ic_tastemaker,
                16
            )
            artist.isAuthenticated -> tvName.spannableStringWithImageAtTheEnd(
                artist.name,
                R.drawable.ic_authenticated,
                16
            )
            else -> artist.name
        }
        tvName.text = name
        tvTopTitle.text = name

        tvSlug.text = artist.urlSlugDisplay
        tvFollowers.text = artist.followersShort
        tvFollowing.text = artist.followingShort
        tvPlays.text = artist.playsShort
        tvPlays.visibility = if (artist.playsCount == 0L) View.GONE else View.VISIBLE
        tvPlaysLabel.visibility = if (artist.playsCount == 0L) View.GONE else View.VISIBLE
        PicassoImageLoader.load(context, artist.smallImage, avatarImageView)
        if (!TextUtils.isEmpty(artist.largeImage)) {
            avatarImageView.setOnClickListener {
                HomeActivity.instance?.openImageZoomFragment(
                    ImageZoomFragment.newInstance(artist.largeImage)
                )
            }
        }

        if (AMArtist.isMyAccount(artist)) {
            buttonFollow.visibility = View.GONE
            buttonFollowSmall.visibility = View.GONE
        }

        val avatarLayoutParams = avatarImageView.layoutParams as FrameLayout.LayoutParams
        if (!artist.banner.isNullOrEmpty()) {
            PicassoImageLoader.load(context, artist.banner, bannerImageView)
            bannerImageView.setOnClickListener {
                HomeActivity.instance?.openImageZoomFragment(
                    ImageZoomFragment.newInstance(artist.banner)
                )
            }
            followInfoLayoutWithBanner.visibility = View.VISIBLE
            buttonFollowSmall.visibility = View.GONE
            avatarLayoutParams.gravity = Gravity.LEFT
            spacerAboveUserDataForNoBanner.visibility = View.GONE
            bannerImageView.visibility = View.VISIBLE
            bannerOverlayImageView.visibility = View.VISIBLE
            heroNoBanner.visibility = View.GONE
        } else {
            followInfoLayoutWithBanner.visibility = View.GONE
            buttonFollowSmall.visibility = View.VISIBLE
            avatarLayoutParams.gravity = Gravity.CENTER_HORIZONTAL
            spacerAboveUserDataForNoBanner.visibility = View.VISIBLE
            bannerImageView.visibility = View.GONE
            bannerOverlayImageView.visibility = View.GONE
            heroNoBanner.visibility = View.VISIBLE
        }
        avatarImageView.layoutParams = avatarLayoutParams

        updateFollowButton(UserData.isArtistFollowed(artist.artistId))
    }

    override fun didScrollTo(verticalOffset: Int) {
        super.didScrollTo(verticalOffset)

        val topLayout = topLayout ?: return

        val verticalOffset = max(0, verticalOffset)

        val noBanner = artist?.banner.isNullOrEmpty()

        val parent = topLayout.parent as LinearLayout

        val parentLp = parent.layoutParams as FrameLayout.LayoutParams
        val heroLp = heroLayout.layoutParams as FrameLayout.LayoutParams
        val avatarLp = avatarImageView.layoutParams as FrameLayout.LayoutParams

        val dp30 = avatarImageView.context.convertDpToPixel(30f)
        val dp12 = avatarImageView.context.convertDpToPixel(12f)
        val maxValue = topLayout.height

        val newValue = max(-maxValue, -verticalOffset)
        val heroTopNewValue =
            max(-(heroLayout.height - navigationBar.height), -verticalOffset)

        val avatarSize: Int
        val avatarTopNewValue: Int

        if (noBanner) {
            avatarSize = dp30 * 2 - min(
                dp30.toFloat(),
                verticalOffset.toFloat() / heroLayout.height.toFloat() * dp30
            ).toInt()
            avatarTopNewValue = dp12
        } else {
            avatarSize = dp30 * 2 - min(
                dp30.toFloat(),
                verticalOffset.toFloat() / (heroLayout.height - navigationBar.height).toFloat() * dp30
            ).toInt()
            avatarTopNewValue = max(
                -avatarSize,
                -verticalOffset + heroLayout.height + avatarImageView.context.convertDpToPixel(34f) - avatarSize
            )
        }

        if (newValue != parentLp.topMargin) {
            parentLp.topMargin = newValue
            parent.layoutParams = parentLp
        }
        if (heroTopNewValue != heroLp.topMargin) {
            heroLp.topMargin = heroTopNewValue
            heroLayout.layoutParams = heroLp
        }
        if (avatarTopNewValue != avatarLp.topMargin || avatarSize != avatarLp.width) {
            avatarLp.topMargin = avatarTopNewValue
            avatarLp.width = avatarSize
            avatarLp.height = avatarSize
            avatarImageView.layoutParams = avatarLp
        }

        if (!noBanner) {
            if (avatarSize == dp30) {
                heroLayout.bringToFront()
                navigationBar.bringToFront()
            } else {
                avatarImageView.bringToFront()
                navigationBar.bringToFront()
            }
        }

        val opaqueNavigationBarTarget = heroLayout!!.height.toFloat()
        val opaqueNavigationBar = verticalOffset > opaqueNavigationBarTarget
        rightButton.visibility =
            if (opaqueNavigationBar || noBanner) View.VISIBLE else View.INVISIBLE
        buttonNavShare.visibility =
            if (opaqueNavigationBar || noBanner) View.VISIBLE else View.INVISIBLE
        var alpha =
            if (verticalOffset <= opaqueNavigationBarTarget) 0.toFloat() else 1 - (maxValue - min(
                maxValue,
                verticalOffset
            )).toFloat() / (min(maxValue, verticalOffset) - opaqueNavigationBarTarget)
        alpha = max(0f, min(alpha, 1f))
        tvTopTitle.alpha = alpha
        if (noBanner) {
            avatarImageView.alpha = 1 - alpha
        }
        navigationBar.setBackgroundColor(
            ColorUtils.setAlphaComponent(navigationBar.context.colorCompat(R.color.profile_bg),
                (alpha * 255).toInt())
        )
    }

    private fun updateFollowButton(followed: Boolean) {
        buttonFollow.background = buttonFollow.context.drawableCompat(if (followed) R.drawable.profile_header_following_bg else R.drawable.profile_header_follow_bg)
        buttonFollow.text = if (followed) getString(R.string.artistinfo_unfollow) else getString(R.string.artistinfo_follow)
        buttonFollowSmall.background = buttonFollow.context.drawableCompat(if (followed) R.drawable.profile_header_following_bg else R.drawable.profile_header_follow_bg)
        buttonFollowSmall.text = if (followed) getString(R.string.artistinfo_unfollow) else getString(R.string.artistinfo_follow)
    }

    fun scrollToUploads() {
        viewPager.currentItem = 0
    }

    private inner class TabsAdapter internal constructor(fm: FragmentManager, private val tabs: List<String>) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val fragmentsMap: SparseArray<WeakReference<Fragment>> = SparseArray()

        override fun getItem(position: Int): Fragment {

            val artistSlug = artist!!.urlSlug
            val artistName = artist!!.name

            val fragment: Fragment

            when (position) {
                0 -> fragment = DataUploadsFragment.newInstance(false, artistSlug, artistName)
                1 -> fragment = DataFavoritesFragment.newInstance(false, artistSlug, artistName)
                2 -> fragment =
                    DataPlaylistsFragment.newInstance(false, artistSlug, artistName, null)
                3 -> fragment = DataFollowersFragment.newInstance(false, artistSlug, artistName)
                4 -> fragment = DataFollowingFragment.newInstance(false, artistSlug, artistName)
                5 -> {
                    fragment = DataPlaylistsFragment.newInstance(
                        false,
                        artistSlug,
                        artistName,
                        deeplinkPlaylistsCategory
                    )
                    deeplinkPlaylistsCategory = null
                }
                else -> fragment = EmptyFragment()
            }

            fragmentsMap.put(position, WeakReference(fragment))
            return fragment
        }

        override fun getCount(): Int = this.tabs.size

        override fun getPageTitle(position: Int): CharSequence? = this.tabs[position]

        fun notifyPages(triggeringPosition: Int) {
            for (i in 0 until fragmentsMap.size()) {
                val key = fragmentsMap.keyAt(i)
                if (key != triggeringPosition || key != fragmentsMap.size() - 1) {
                    if (fragmentsMap.get(key) != null) {
                        val fragment = fragmentsMap.get(key).get()
                        if (fragment is DataFragment) {
                            fragment.adjustScroll()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        arguments?.let {
            this.deeplinkTab = it.getString("deeplinkTab")
            this.deeplinkPlaylistsCategory = it.getString("deeplinkPlaylistsCategory")
            this.showBackButton = it.getBoolean("showBackButton")
            this.openShare = it.getBoolean("openShare")
        }
    }

    override fun onDestroy() {
        disposables.dispose()
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventFollowChange: EventFollowChange) {
        if (artist != null && artist?.artistId == eventFollowChange.artistId) {
            loadData()
        }
    }

    companion object {
        private const val TAG = "ArtistFragment"
        @JvmStatic
        fun newInstance(deeplinkTab: String?, deeplinkPlaylistsCategory: String?, showBackButton: Boolean, openShare: Boolean): ArtistFragment {
            return ArtistFragment().apply {
                arguments = bundleOf(
                    "deeplinkTab" to deeplinkTab,
                    "deeplinkPlaylistsCategory" to deeplinkPlaylistsCategory,
                    "showBackButton" to showBackButton,
                    "openShare" to openShare
                )
            }
        }
    }
}
