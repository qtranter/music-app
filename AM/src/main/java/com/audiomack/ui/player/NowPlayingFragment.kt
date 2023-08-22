package com.audiomack.ui.player

import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.R.drawable
import com.audiomack.playback.RepeatType
import com.audiomack.playback.RepeatType.ALL
import com.audiomack.playback.RepeatType.ONE
import com.audiomack.playback.ShuffleState
import com.audiomack.playback.ShuffleState.DISABLED
import com.audiomack.playback.ShuffleState.ON
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.player.full.PlayerFragment
import com.audiomack.ui.player.full.view.DragFragment
import com.audiomack.ui.player.maxi.bottom.PlayerBottomFragment
import com.audiomack.ui.player.maxi.uploader.PlayerUploaderTagsFragment
import com.audiomack.ui.tooltip.TooltipCorner.BOTTOMRIGHT
import com.audiomack.ui.tooltip.TooltipFragment.TooltipLocation
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.setOnScrollYListener
import com.audiomack.views.AMSnackbar
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.abs
import kotlinx.android.synthetic.main.fragment_now_playing.nowPlayingBottom
import kotlinx.android.synthetic.main.fragment_now_playing.nowPlayingBottomTabContainer
import kotlinx.android.synthetic.main.fragment_now_playing.nowPlayingBottomTabs
import kotlinx.android.synthetic.main.fragment_now_playing.nowPlayingEqBtn
import kotlinx.android.synthetic.main.fragment_now_playing.nowPlayingLayout
import kotlinx.android.synthetic.main.fragment_now_playing.nowPlayingPlayer
import kotlinx.android.synthetic.main.fragment_now_playing.nowPlayingRepeatBtn
import kotlinx.android.synthetic.main.fragment_now_playing.nowPlayingShuffleBtn
import kotlinx.android.synthetic.main.fragment_now_playing.nowPlayingUploader

class NowPlayingFragment : DragFragment() {

    private lateinit var viewModel: NowPlayingViewModel

    private val disposables = CompositeDisposable()

    /**
     * When the fragment is minimized we want to restore scroll back to the top, but we don't
     * want to trigger the event that hides the minimized player. This field is true in that case.
     */
    private var scrollResetting = false

    private val scrollObservable = Observable.create<Int> { emitter ->
        nowPlayingLayout.setOnScrollYListener {
            emitter.onNext(it)
        }
    }

    /**
     * Observers will be notified of the latest Y axis scroll position
     */
    private val scrollYSubject: Subject<Int> = PublishSubject.create()

    /**
     * The amount of scroll needed to show the "bottom" section
     */
    private val bottomVisibleScrollDistance by lazy {
        nowPlayingUploader.top - nowPlayingPlayer.height - nowPlayingShuffleBtn.height / 2
    }

    /**
     * The amount of scroll needed to show the "tabs" section
     */
    private val tabsVisibleScrollDistance by lazy {
        nowPlayingBottom.top - nowPlayingPlayer.height - nowPlayingShuffleBtn.height / 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_now_playing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = (requireActivity() as HomeActivity).nowPlayingViewModel

        initViews()
        initViewModelObservers()
        initClickListeners()
        observeTabVisibility()

        nowPlayingShuffleBtn.doOnLayout {
            nowPlayingPlayer.layoutParams?.height = view.height - it.height / 2 - it.context.convertDpToPixel(9F)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            parentFragmentManager.commit {
                add(R.id.nowPlayingPlayer, PlayerFragment.newInstance())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // The view model will still be notified of queue changes, but we don't want to fetch
        // while paused
        viewModel.onBottomVisibilityChanged(false)
    }

    override fun onResume() {
        super.onResume()
        // Reset the visibility flag that was cleared in onPause()
        view?.doOnLayout {
            viewModel.onBottomVisibilityChanged(nowPlayingLayout.scrollY > bottomVisibleScrollDistance)
            viewModel.onTabsVisibilityChanged(nowPlayingLayout.scrollY > tabsVisibleScrollDistance)
        }
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    private fun initViews() {
        scrollObservable.subscribe(scrollYSubject)
        observePlayerVisibility()
        pinBottomTabsOnScroll()
        nowPlayingLayout.dragListener = this
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            shuffle.observe(viewLifecycleOwner, shuffleStateObserver)
            repeat.observe(viewLifecycleOwner, repeatTypeObserver)
            equalizerEnabled.observe(viewLifecycleOwner, equalizerEnabledObserver)
            onMinimizeEvent.observe(viewLifecycleOwner, minimizeObserver)
            maximizeEvent.observe(viewLifecycleOwner, maximizeObserver)
            scrollToTopEvent.observe(viewLifecycleOwner, scrollToTopObserver)
            bottomPageSelectedEvent.observe(viewLifecycleOwner, bottomPageSelectedObserver)
            requestScrollTooltipEvent.observe(viewLifecycleOwner, scrollTooltipEventObserver)
            requestEqTooltipEvent.observe(viewLifecycleOwner, eqTooltipEventObserver)
            onTooltipDismissEvent.observe(viewLifecycleOwner, tooltipDismissedEventObserver)
            isLocalMedia.observe(viewLifecycleOwner, ::toggleBottomSections)
        }
    }

    private fun initClickListeners() {
        nowPlayingShuffleBtn.setOnClickListener { viewModel.onShuffleClick() }
        nowPlayingRepeatBtn.setOnClickListener { viewModel.onRepeatClick() }
        nowPlayingEqBtn.setOnClickListener { viewModel.onEqClick() }

        nowPlayingBottomTabs.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.nowPlayingTabComments -> {
                    viewModel.onBottomTabSelected(0)
                }
                R.id.nowPlayingTabInfo -> {
                    viewModel.onBottomTabSelected(1)
                }
                R.id.nowPlayingTabMore -> {
                    viewModel.onBottomTabSelected(2)
                }
            }
        }
    }

    private val shuffleStateObserver = Observer<ShuffleState> { shuffleState ->
        nowPlayingShuffleBtn.setImageResource(
            when (shuffleState) {
                ON -> drawable.ic_shuffle_on
                else -> drawable.ic_shuffle
            }
        )
        nowPlayingShuffleBtn.alpha = if (shuffleState != DISABLED) 1.0F else 0.5F
        nowPlayingShuffleBtn.isClickable = shuffleState != DISABLED
    }

    private val repeatTypeObserver = Observer<RepeatType> { repeatType ->
        nowPlayingRepeatBtn.setImageResource(
            when (repeatType) {
                ALL -> drawable.ic_repeat_all
                ONE -> drawable.ic_repeat_one
                else -> drawable.ic_repeat
            }
        )
        when (repeatType) {
            ONE -> {
                AMSnackbar.Builder(activity)
                    .withTitle(getString(R.string.player_repeat_one))
                    .withDuration(Snackbar.LENGTH_SHORT)
                    .show()
            }
            ALL -> {
                AMSnackbar.Builder(activity)
                    .withTitle(getString(R.string.player_repeat_all))
                    .withDuration(Snackbar.LENGTH_SHORT)
                    .show()
            }
            else -> {}
        }
    }

    private val equalizerEnabledObserver = Observer<Boolean> { enabled ->
        nowPlayingEqBtn.alpha = if (enabled) 1.0F else 0.5F
        nowPlayingEqBtn.isClickable = enabled
    }

    private val minimizeObserver = Observer<Void> {
        scrollResetting = true
        nowPlayingLayout.scrollY = 0
    }

    private val maximizeObserver = Observer<Boolean> {
        if (it) scrollResetting = false
    }

    private val scrollToTopObserver = Observer<Void> {
        scrollResetting = false
        nowPlayingLayout.scrollY = 0
    }

    private val bottomPageSelectedObserver = Observer<Int> {
        nowPlayingBottomTabs.check(
            when (it) {
                0 -> R.id.nowPlayingTabComments
                1 -> R.id.nowPlayingTabInfo
                2 -> R.id.nowPlayingTabMore
                else -> -1
            }
        )
    }

    private val scrollTooltipEventObserver = Observer<Void> {
        view?.let { view ->
            nowPlayingLayout.showScrollBars()

            val margin = resources.getDimensionPixelOffset(R.dimen.now_playing_scrollbar_margin)
            val rect = Rect()
            view.getGlobalVisibleRect(rect)
            val target = Point(rect.right - margin, rect.bottom / 6)
            viewModel.setScrollTooltipLocation(TooltipLocation(BOTTOMRIGHT, target))
        }
    }

    private val eqTooltipEventObserver = Observer<Void> {
        showEqTooltipWhenNeeded()

        disposables.add(
            Observable.timer(1L, SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    nowPlayingLayout.smoothScrollTo(0, nowPlayingEqBtn.top / 2)
                }
        )
    }

    private val tooltipDismissedEventObserver = Observer<Void> {
        nowPlayingLayout.hideScrollBars()
    }

    private fun observePlayerVisibility() {
        disposables.add(
            scrollYSubject
                .map { it > nowPlayingPlayer.height }
                .distinctUntilChanged()
                .debounce(250L, MILLISECONDS)
                .subscribe {
                    if (!scrollResetting) {
                        viewModel.onPlayerVisibilityChanged(it)
                    }
                    scrollResetting = false
                }
        )
    }

    private fun pinBottomTabsOnScroll() {
        disposables.add(
            scrollYSubject
                .map { nowPlayingBottomTabContainer.top - it }
                .subscribe {
                    if (it < 0) {
                        nowPlayingBottomTabContainer.translationY = abs(it).toFloat()
                    }
                }
        )
    }

    private fun showEqTooltipWhenNeeded() {
        disposables.add(
            scrollYSubject
                .skipWhile { it < (nowPlayingEqBtn.top / 2) }
                .take(1)
                .subscribe {
                    val rect = Rect()
                    nowPlayingEqBtn.getGlobalVisibleRect(rect)
                    val target = Point(rect.left + nowPlayingEqBtn.width / 2, rect.top)
                    viewModel.setEqTooltipLocation(TooltipLocation(BOTTOMRIGHT, target))
                }
        )
    }

    private fun observeTabVisibility() {
        disposables.add(
            scrollYSubject
                .map { it > bottomVisibleScrollDistance }
                .distinctUntilChanged()
                .subscribe {
                    viewModel.onBottomVisibilityChanged(it)
                }
        )
        disposables.add(
            scrollYSubject
                .map { it > tabsVisibleScrollDistance }
                .distinctUntilChanged()
                .subscribe {
                    viewModel.onTabsVisibilityChanged(it)
                }
        )
        disposables.add(
            scrollYSubject
                .map {
                    val lastChild = nowPlayingLayout.getChildAt(nowPlayingLayout.childCount - 1)
                    val diff = lastChild.bottom - nowPlayingLayout.height - it
                    diff == 0
                }
                .distinctUntilChanged()
                .subscribe {
                    viewModel.onScrollViewReachedBottomChange(it)
                }
        )
    }

    private fun toggleBottomSections(isLocal: Boolean) {
        nowPlayingBottomTabContainer.isVisible = !isLocal

        parentFragmentManager.commit {
            val uploaderFragment =
                parentFragmentManager.findFragmentByTag(PlayerUploaderTagsFragment.TAG)
            val bottomFragment =
                parentFragmentManager.findFragmentByTag(PlayerBottomFragment.TAG)

            if (isLocal) {
                uploaderFragment?.let { remove(it) }
                bottomFragment?.let { remove(it) }
                nowPlayingLayout.scrollY = 0
            } else {
                if (uploaderFragment == null) {
                    add(
                        R.id.nowPlayingUploader,
                        PlayerUploaderTagsFragment.newInstance(),
                        PlayerUploaderTagsFragment.TAG
                    )
                }
                if (bottomFragment == null) {
                    if (Build.VERSION.SDK_INT <= 27) {
                        // There must be something buggy with NestedScrollView used with a ViewPager2.
                        // Adding PlayerBottomFragment together with the other fragments makes the player
                        // scroll to the bottom section. This is an hack for API Levels up to 27 included.
                        // See https://github.com/audiomack/audiomack-android/issues/2096
                        runOnCommit {
                            nowPlayingLayout.post {
                                parentFragmentManager.commit(true) {
                                    add(
                                        R.id.nowPlayingBottom,
                                        PlayerBottomFragment.newInstance(),
                                        PlayerBottomFragment.TAG
                                    )
                                }
                            }
                        }
                    } else {
                        add(
                            R.id.nowPlayingBottom,
                            PlayerBottomFragment.newInstance(),
                            PlayerBottomFragment.TAG
                        )
                    }
                }
            }
        }
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "NowPlayingFragment"

        fun newInstance() = NowPlayingFragment()
    }
}
