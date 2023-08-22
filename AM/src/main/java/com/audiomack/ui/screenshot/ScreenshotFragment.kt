package com.audiomack.ui.screenshot

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.data.screenshot.ScreenshotDetector
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.ScreenshotModel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.spannableString
import kotlinx.android.synthetic.main.fragment_screenshot.btnBenchmarkHide
import kotlinx.android.synthetic.main.fragment_screenshot.btnClose
import kotlinx.android.synthetic.main.fragment_screenshot.ivArrow
import kotlinx.android.synthetic.main.fragment_screenshot.ivArtist
import kotlinx.android.synthetic.main.fragment_screenshot.ivArtistBlurBg
import kotlinx.android.synthetic.main.fragment_screenshot.ivArtistIcon
import kotlinx.android.synthetic.main.fragment_screenshot.ivBenchmark
import kotlinx.android.synthetic.main.fragment_screenshot.ivBlurBg
import kotlinx.android.synthetic.main.fragment_screenshot.ivSong
import kotlinx.android.synthetic.main.fragment_screenshot.recyclerView
import kotlinx.android.synthetic.main.fragment_screenshot.tvMilestoneSubtitle
import kotlinx.android.synthetic.main.fragment_screenshot.tvMilestoneTitle
import kotlinx.android.synthetic.main.fragment_screenshot.tvNowPlaying
import kotlinx.android.synthetic.main.fragment_screenshot.tvSongFeat
import kotlinx.android.synthetic.main.fragment_screenshot.tvSubtitle
import kotlinx.android.synthetic.main.fragment_screenshot.tvTitle
import kotlinx.android.synthetic.main.fragment_screenshot.viewBenchmark
import kotlinx.android.synthetic.main.fragment_screenshot.viewBenchmarkArtist
import kotlinx.android.synthetic.main.fragment_screenshot.viewBenchmarkImage
import kotlinx.android.synthetic.main.fragment_screenshot.viewBenchmarkTitle
import kotlinx.android.synthetic.main.fragment_screenshot.viewGesture
import kotlinx.android.synthetic.main.fragment_screenshot.viewMain
import kotlinx.android.synthetic.main.fragment_screenshot.viewParent
import kotlinx.android.synthetic.main.fragment_screenshot.viewToast

class ScreenshotFragment : TrackedFragment(R.layout.fragment_screenshot, TAG) {

    private val viewModel: ScreenshotViewModel by activityViewModels()
    private lateinit var benchmarkAdapter: BenchmarkAdapter
    private lateinit var screenshot: ScreenshotModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        screenshot = arguments?.getParcelable(EXTRA_SHARE_SCREENSHOT) as? ScreenshotModel
            ?: throw IllegalStateException("No screenshot specified in arguments")

        initGesturesListeners()
        initViewModelObservers()

        benchmarkAdapter = BenchmarkAdapter(viewModel)
        recyclerView.adapter = benchmarkAdapter

        viewModel.init(screenshot)

        lifecycle.addObserver(ScreenshotDetector(requireContext()) {
            viewModel.onScreenshotDetected()
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initGesturesListeners() {

        btnClose.setOnClickListener { viewModel.onCloseClicked() }
        viewToast.setOnClickListener { viewModel.onToastCloseClicked() }
        btnBenchmarkHide.setOnClickListener { viewModel.onHideBenchmarkClicked() }

        val gesture = GestureDetector(activity,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onSingleTapUp(e: MotionEvent?): Boolean {
                    viewModel.onScreenTapped()
                    return super.onSingleTapUp(e)
                }

                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

                override fun onFling(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    viewModel.onFling(e1.y, e2.y, velocityY)
                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            }
        )

        viewGesture.setOnTouchListener { _, event ->
            gesture.onTouchEvent(event)
        }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            title.observe(viewLifecycleOwner, titleObserver)
            titleVisible.observe(viewLifecycleOwner, titleVisibleObserver)
            subtitle.observe(viewLifecycleOwner, subtitleObserver)
            musicFeatName.observe(viewLifecycleOwner, musicFeatNameObserver)
            musicFeatVisible.observe(viewLifecycleOwner, musicFeatVisibleObserver)
            artworkUrl.observe(viewLifecycleOwner, artworkUrlObserver)
            artworkBitmap.observe(viewLifecycleOwner, artworkBitmapObserver)
            backgroundBitmap.observe(viewLifecycleOwner, backgroundBitmapObserver)
            showToastEvent.observe(viewLifecycleOwner, showToastObserver)
            hideToastEvent.observe(viewLifecycleOwner, hideToastObserver)
            startAnimationEvent.observe(viewLifecycleOwner, startAnimationEventObserver)
            prepareAnimationEvent.observe(viewLifecycleOwner, prepareAnimationEventObserver)
            closeButtonVisible.observe(viewLifecycleOwner, closeButtonVisibleObserver)
            swipeDownEvent.observe(viewLifecycleOwner, swipeDownEventObserver)
            artistArtworkUrl.observe(viewLifecycleOwner, artistArtworkUrlObserver)
            artistArtworkBitmap.observe(viewLifecycleOwner, artistArtworkBitmapObserver)
            artistBackgroundBitmap.observe(viewLifecycleOwner, artistBackgroundBitmapObserver)
            benchmarkCatalogVisible.observe(viewLifecycleOwner, benchmarkCatalogVisibleObserver)
            benchmarkList.observe(viewLifecycleOwner, benchmarkListObserver)
            benchmarkViewsVisible.observe(viewLifecycleOwner, benchmarkViewsVisibleObserver)
            verifiedBenchmarkVisible.observe(viewLifecycleOwner, verifiedBenchmarkVisibleObserver)
            benchmarkMilestone.observe(viewLifecycleOwner, benchmarkMilestoneObserver)
            benchmarkTitle.observe(viewLifecycleOwner, benchmarkTitleObserver)
            benchmarkSubtitle.observe(viewLifecycleOwner, benchmarkSubtitleObserver)
            benchmarkSubtitleSize.observe(viewLifecycleOwner, benchmarkSubtitleSizeObserver)
            benchmarkIcon.observe(viewLifecycleOwner, benchmarkIconObserver)
            benchmarkArtistIcon.observe(viewLifecycleOwner, benchmarkArtistIconObserver)
        }
    }

    private val titleObserver = Observer<String> {
        tvTitle.text = it
    }

    private val titleVisibleObserver = Observer<Boolean> { visible ->
        tvTitle.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val subtitleObserver = Observer<String> {
        tvSubtitle.text = it
    }

    private val musicFeatNameObserver = Observer<String> {
        val fullString = String.format("%s %s", getString(R.string.feat), it)
        val spannableString = tvSongFeat.context.spannableString(
            fullString = fullString,
            highlightedStrings = listOf(it),
            highlightedColor = tvSongFeat.context.colorCompat(R.color.orange),
            highlightedFont = R.font.opensans_semibold
        )
        tvSongFeat.text = spannableString
    }

    private val musicFeatVisibleObserver = Observer<Boolean> { visible ->
        tvSongFeat.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val artworkUrlObserver = Observer<String> {
        viewModel.onLoadArtwork(context, it)
        viewModel.onLoadBackgroundBlur(context, it)
    }

    private val showToastObserver = Observer<Void> {
        viewToast.visibility = View.VISIBLE
        ivArrow.visibility = View.VISIBLE
    }

    private val hideToastObserver = Observer<Void> {
        viewToast.visibility = View.GONE
        ivArrow.visibility = View.GONE
    }

    private val artworkBitmapObserver = Observer<Bitmap> {
        ivSong.setImageBitmap(it)
    }

    private val backgroundBitmapObserver = Observer<Bitmap> {
        ivBlurBg.setImageBitmap(it)
    }

    private val prepareAnimationEventObserver = Observer<Void> {
        viewParent.alpha = 0f
        viewMain.alpha = 0f
    }

    private val startAnimationEventObserver = Observer<Void> {
        view?.let {
            viewMain.animate().translationY(it.height.toFloat()).withEndAction {
                val viewParent = viewParent ?: return@withEndAction
                val viewMain = viewMain ?: return@withEndAction
                viewParent.animate().alpha(1F).duration = 50
                viewMain.animate().translationY(0F).alpha(1F).withEndAction {
                    viewModel.onAnimationComplete()
                }.duration = 300
            }.duration = 50
        }
    }

    private val closeButtonVisibleObserver = Observer<Boolean> { visible ->
        btnClose.animate().alpha(if (visible) 1F else 0F).duration = 50
    }

    private val swipeDownEventObserver = Observer<Void> {
        view?.let {
            viewMain.animate().translationY(it.height.toFloat()).withEndAction {
                viewModel.onDownAnimationComplete()
            }.duration = 300
        }
    }

    private val artistArtworkUrlObserver = Observer<String> {
        viewModel.onLoadArtistArtwork(context, it)
        viewModel.onLoadArtistBackgroundBlur(context, it)
    }

    private val artistArtworkBitmapObserver = Observer<Bitmap> {
        ivArtist.setImageBitmap(it)
    }

    private val artistBackgroundBitmapObserver = Observer<Bitmap> {
        ivArtistBlurBg.setImageBitmap(it)
    }

    private val benchmarkCatalogVisibleObserver = Observer<Boolean> { visible ->
        viewBenchmark
            .animate()
            .translationY(viewBenchmark.height.toFloat() * (if (visible) -1 else 1))
            .duration = 100
    }

    private val benchmarkViewsVisibleObserver = Observer<Boolean> { visible ->
        tvNowPlaying.visibility = if (visible) View.GONE else View.VISIBLE
        viewBenchmarkImage.visibility = if (visible) View.VISIBLE else View.GONE
        viewBenchmarkTitle.visibility = if (visible) View.VISIBLE else View.GONE
        ivBlurBg.visibility = View.VISIBLE
        ivSong.visibility = View.VISIBLE
        ivArtistBlurBg.visibility = View.GONE
        viewBenchmarkArtist.visibility = View.GONE
    }

    private val verifiedBenchmarkVisibleObserver = Observer<Boolean> { visible ->
        if (visible) {
            viewBenchmarkImage.visibility = View.GONE
            ivBlurBg.visibility = View.GONE
            ivSong.visibility = View.GONE
            ivArtistBlurBg.visibility = View.VISIBLE
            viewBenchmarkArtist.visibility = View.VISIBLE
        }
    }

    private val benchmarkListObserver = Observer<List<BenchmarkModel>> { benchmarks ->
        benchmarkAdapter.update(benchmarks)
    }

    private val benchmarkTitleObserver = Observer<Int> {
        tvMilestoneTitle.text = tvMilestoneTitle.context.resources.getString(it)
        tvMilestoneTitle.applyGradient()
    }

    private val benchmarkSubtitleObserver = Observer<Int> {
        tvMilestoneSubtitle.text = tvMilestoneSubtitle.context.resources.getString(it)
    }

    private val benchmarkSubtitleSizeObserver = Observer<Int> {
        tvMilestoneSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, tvMilestoneSubtitle.resources.getDimension(it))
    }

    private val benchmarkMilestoneObserver = Observer<BenchmarkModel> {
        tvMilestoneTitle.text = it.getPrettyMilestone(tvMilestoneTitle.context)
        tvMilestoneTitle.applyGradient()
    }

    private val benchmarkIconObserver = Observer<Int> {
        ivBenchmark.setImageDrawable(ivBenchmark.context.drawableCompat(it))
    }

    private val benchmarkArtistIconObserver = Observer<Int?> {
        it?.let { resId ->
            ivArtistIcon.setImageDrawable(ivArtistIcon.context.drawableCompat(resId))
        } ?: ivArtistIcon.setImageDrawable(null)
    }

    companion object {
        private const val TAG = "ScreenshotFragment"
        private const val EXTRA_SHARE_SCREENSHOT = "EXTRA_SHARE_SCREENSHOT"
        fun newInstance(model: ScreenshotModel): ScreenshotFragment =
            ScreenshotFragment().apply {
                arguments = Bundle().apply { putParcelable(EXTRA_SHARE_SCREENSHOT, model) }
            }
    }
}
