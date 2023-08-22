package com.audiomack.ui.player.mini

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventPlayer
import com.audiomack.model.PlayerCommand
import com.audiomack.playback.PlaybackState.PAUSED
import com.audiomack.playback.PlaybackState.PLAYING
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.player.NowPlayingViewModel
import com.audiomack.ui.player.full.PlayerViewModel
import com.audiomack.ui.player.maxi.PlayerDragDirection
import com.audiomack.ui.widget.AudiomackWidget
import com.audiomack.utils.SwipeDetector
import com.audiomack.utils.addOnPageSelectedListener
import com.audiomack.utils.extensions.drawableCompat
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.android.synthetic.main.fragment_minified_player.buttonPlay
import kotlinx.android.synthetic.main.fragment_minified_player.buttonScrollToTop
import kotlinx.android.synthetic.main.fragment_minified_player.buttonTwoDots
import kotlinx.android.synthetic.main.fragment_minified_player.imageView
import kotlinx.android.synthetic.main.fragment_minified_player.progressView
import kotlinx.android.synthetic.main.fragment_minified_player.viewPager
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

class MinifiedPlayerFragment : androidx.fragment.app.Fragment(), SwipeDetector.DragListener, SwipeDetector.ClickListener {

    private lateinit var playerViewModel: PlayerViewModel
    private lateinit var nowPlayingViewModel: NowPlayingViewModel

    private var swipeDetector: SwipeDetector? = null
    private val minimumDragDistance = 10
    private var isAlreadyDraggingUp = false

    private var playing: Boolean = false
    private var playbackDuration: Long? = null

    private var changeInState: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            super.onCreateView(inflater, container, savedInstanceState)
            inflater.inflate(R.layout.fragment_minified_player, container, false)
        } catch (e: Exception) {
            Timber.w(e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerViewModel = (requireActivity() as HomeActivity).playerViewModel
        nowPlayingViewModel = (requireActivity() as HomeActivity).nowPlayingViewModel

        buttonPlay.setOnClickListener {
            playerViewModel.onPlayPauseClick()
        }

        buttonTwoDots.setOnClickListener {
            EventBus.getDefault().post(EventPlayer(PlayerCommand.MENU))
        }

        buttonScrollToTop.setOnClickListener {
            onClickDetected()
        }

        swipeDetector = SwipeDetector(viewPager.width, viewPager.height, null, null, this, this)
        viewPager.setOnTouchListener { v, event -> swipeDetector?.onTouch(v, event) ?: false }
        viewPager.addOnPageSelectedListener {
            playerViewModel.onTrackSelected(it)
            updateArtwork()
        }

        initObservers()
    }

    private fun updateArtwork() {
        val currentItem = viewPager.currentItem
        val songs = (viewPager.adapter as? MinifiedPlayerPagerAdapter)?.songs
        if (songs == null || songs.size <= currentItem) return

        songs[currentItem].let { song ->
            imageView.setImageDrawable(null)
            PicassoImageLoader.load(
                imageView.context,
                song.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
                imageView,
                R.drawable.ic_artwork
            )
        }
    }

    private fun initObservers() {
        playerViewModel.apply {
            songList.observe(viewLifecycleOwner, Observer { songs ->
                viewPager.adapter = MinifiedPlayerPagerAdapter(songs)
                updateArtwork()
            })
            currentIndex.observe(viewLifecycleOwner, Observer { index ->
                viewPager.setCurrentItem(index, false)
            })
            playbackState.observe(viewLifecycleOwner, Observer { state ->
                playing = state == PLAYING
                updateButtonState()

                val position = playerViewModel.currentPosition.value?.toInt() ?: 0
                val duration = playerViewModel.duration.value?.toInt() ?: 0
                notifyWidgetState(state == PAUSED, position, duration)
            })
            currentPosition.observe(viewLifecycleOwner, Observer { position ->
                if (playbackDuration != null) {
                    progressView.progress = position.toInt()
                }
            })
            duration.observe(viewLifecycleOwner, Observer { duration ->
                playbackDuration = duration
                progressView.max = duration.toInt()
                currentPosition.value?.let { position ->
                    progressView.progress = position.toInt()
                }
            })
        }

        nowPlayingViewModel.apply {
            maximizeEvent.observe(viewLifecycleOwner, Observer { isMaximized ->
                buttonTwoDots.visibility = if (isMaximized) View.GONE else View.VISIBLE
                buttonScrollToTop.visibility = if (isMaximized) View.VISIBLE else View.GONE
            })
        }
    }

    override fun onDragStart(view: View, startX: Float, startY: Float): Boolean {
        return false
    }

    override fun onDrag(
        view: View,
        rawX: Float,
        rawY: Float,
        startX: Float,
        startY: Float
    ): Boolean {
        if (nowPlayingViewModel.isMaximized) return false

        val deltaY = (startY - rawY).roundToInt()
        val deltaX = (startX - rawX).roundToInt()

        if ((abs(deltaY) <= minimumDragDistance || abs(deltaX) >= minimumDragDistance) && !isAlreadyDraggingUp) {
            return false
        }

        isAlreadyDraggingUp = true
        (activity as? HomeActivity)?.dragPlayer(deltaY,
            PlayerDragDirection.UP
        )

        return true
    }

    override fun onDragEnd(
        view: View,
        endX: Float,
        endY: Float,
        startX: Float,
        startY: Float
    ): Boolean {
        val draggingUp = startY > endY
        val deltaY = (startY - endY).toInt()
        val distance = abs(deltaY)
        val minDistance = view.height / 3

        if (!isAlreadyDraggingUp) {
            return false
        }

        isAlreadyDraggingUp = false

        (activity as? HomeActivity)?.let {
            if (draggingUp && distance > minDistance && !nowPlayingViewModel.isMaximized) {
                it.homeViewModel.onMiniplayerSwipedUp()
                return true
            }
            it.resetPlayerDrag(250 * distance / minDistance,
                PlayerDragDirection.UP
            )
        }

        return false
    }

    override fun onClickDetected() {
        EventBus.getDefault().post(EventPlayer(PlayerCommand.OPEN))
    }

    private fun updateButtonState() {
        buttonPlay.setImageDrawable(
            buttonPlay.context.drawableCompat(
                if (playing) R.drawable.miniplayer_pause else R.drawable.miniplayer_play
            )
        )
    }

    private fun notifyWidgetState(paused: Boolean, progress: Int, duration: Int) {
        if (changeInState != paused) {
            val intentStateChange = android.content.Intent(context, AudiomackWidget::class.java)
            intentStateChange.action = AudiomackWidget.UPDATE_PLAYPAUSE
            intentStateChange.putExtra(AudiomackWidget.INTENT_KEY_PLAYING, !paused)
            intentStateChange.putExtra(AudiomackWidget.INTENT_KEY_CURRENTPOS, progress)
            intentStateChange.putExtra(AudiomackWidget.INTENT_KEY_DURATION, duration)
            changeInState = paused
            activity?.sendBroadcast(intentStateChange)
        }
    }
}
