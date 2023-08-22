package com.audiomack.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.RemoteViews
import com.audiomack.INTENT_EXTRA_MIXPANEL_BUTTON
import com.audiomack.INTENT_TOGGLE_FAVORITE
import com.audiomack.INTENT_TOGGLE_REPOST
import com.audiomack.MainApplication.Companion.context
import com.audiomack.R
import com.audiomack.data.sizes.SizesRepository
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonWidget
import com.audiomack.model.AMGenre
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventPlayer
import com.audiomack.model.PlayerCommand
import com.audiomack.ui.home.HomeActivity
import com.squareup.picasso.Picasso
import java.lang.RuntimeException
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

/**
 * Implementation of App Widget functionality.
 */

class AudiomackWidget : AppWidgetProvider() {

    companion object {

        const val TAG = "AudiomackWidget"

        var widgetLayout = R.layout.audiomack_widget
        var countDownTimer: CountDownTimer? = null
        var isRunning = false
        var isAppOpen = false
        lateinit var remoteView: RemoteViews
        fun isRemoteViewInitialized() = ::remoteView.isInitialized

        private var songTitle: String? = null
        private var songImage: String? = null
        private var songArtist: String? = null
        private var songFeat: String? = null

        var displayText: String? = null // [songArtist] - [songTitle] feat [songFeat]

        var favorited: Boolean = false
        var reupped: Boolean = false
        var currentPos: Long = 0
        var duration: Long = 1
        var loggedIn: Boolean = true
        var reupEnabled: Boolean = false
        var isPodcast: Boolean = false
        var isLocal: Boolean = false

        const val INTENT_KEY_IMAGE = "image"
        const val INTENT_KEY_TITLE = "title"
        const val INTENT_KEY_ARTIST = "artist"
        const val INTENT_KEY_FEAT = "feat"
        const val INTENT_KEY_FAVORITE = "favorited"
        const val INTENT_KEY_REPOST = "reposted"
        const val INTENT_KEY_REPOST_ENABLED = "repost_enabled"
        const val INTENT_KEY_DURATION = "duration"
        const val INTENT_KEY_PLAYING = "playing"
        const val INTENT_KEY_PROGRESS = "progress"
        const val INTENT_KEY_LOGGED_IN = "logged in "
        const val INTENT_KEY_CURRENTPOS = "current pos"
        const val INTENT_KEY_LOADING_VIEW = "loading view"
        const val INTENT_KEY_GENRE = "genre"
        const val INTENT_KEY_IS_LOCAL = "is local"

        const val RIGHT_CLICKED = "android.ui.widget.AudiomackWidget.RIGHT_CLICKED"
        const val LEFT_CLICKED = "android.ui.widget.AudiomackWidget.LEFT_CLICKED"
        const val PLAY_CLICKED = "android.ui.widget.AudiomackWidget.PLAY_CLICKED"
        const val HEART_CLICKED = "android.ui.widget.AudiomackWidget.HEART_CLICKED"
        const val REUP_CLICKED = "android.ui.widget.AudiomackWidget.REUP_CLICKED"
        const val SONG_TITLE_CLICKED = "android.ui.widget.AudiomackWidget.SONG_TITLE_CLICKED"
        const val ALBUM_CLICKED = "android.ui.widget.AudiomackWidget.ALBUM_CLICKED"

        const val UPDATE_SONG = "android.ui.widget.AudiomackWidget.UPDATE_SONG"
        const val UPDATE_SEEKBAR_NEW_LAYOUT = "android.ui.widget.AudiomackWidget.UPDATE_SEEKBAR_NEW_LAYOUT"
        const val UPDATE_LOADING_CIRCLE = "android.ui.widget.AudiomackWidget.UPDATE_LOADING_CIRCLE"

        const val NOT_LOGGED_IN = "Not logged in"
        val DEFAULT_SONG_IMAGE by lazy { "https://assets.audiomack.com/_default/default-song-image.png?width=${SizesRepository.smallMusic}" }
        const val AUDIOMACK_TITLE = "Audiomack"
        const val PROGRESS_BAR_MAX = 100
        const val PROGRESS_BAR_MIN = 0
        const val DESTROYED = "Destroyed"
        const val FAVORITE_AFTER_LOGIN = "Favorite after login"
        const val WIDGET_RESIZE_3_COLUMN = 3
        const val WIDGET_RESIZE_4_COLUMN = 4
        const val START_TIMER_DURATION = "duration"
        const val PLAY_BUTTON_LEFT_PADDING = 10

        const val STOP_TIMER = "android.ui.widget.AudiomackWidget.STOP_TIMER"
        const val START_TIMER = "android.ui.widget.AudiomackWidget.START_TIMER"
        const val UPDATE_PLAYPAUSE = "android.ui.widget.AudiomackWidget.UPDATE_PLAYPAUSE"
        const val UPDATE_SEEKBAR_PROGRESS = "android.ui.widget.AudiomackWidget.UPDATE_SEEKBAR_PROGRESS"
        const val SONG_FAVORITED = "android.ui.widget.AudiomackWidget.SONG_FAVORITED"
        const val SONG_REPOSTED = "android.ui.widget.AudiomackWidget.SONG_REPOSTED"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {

            val rightIntent = Intent(RIGHT_CLICKED)
            rightIntent.setClass(context, AudiomackWidget::class.java)
            val rightPendingIntent: PendingIntent = PendingIntent.getBroadcast(context, 0, rightIntent, 0)

            val leftIntent = Intent(LEFT_CLICKED)
            leftIntent.setClass(context, AudiomackWidget::class.java)
            val leftPendingIntent: PendingIntent = PendingIntent.getBroadcast(context, 0, leftIntent, 0)

            val playIntent = Intent(PLAY_CLICKED)
            playIntent.setClass(context, AudiomackWidget::class.java)
            val playPendingIntent: PendingIntent = PendingIntent.getBroadcast(context, 0, playIntent, 0)

            val heartIntent = Intent(HEART_CLICKED)
            heartIntent.setClass(context, AudiomackWidget::class.java)
            val heartPendingIntent: PendingIntent = PendingIntent.getBroadcast(context, 0, heartIntent, 0)

            val songTitleIntent = Intent(SONG_TITLE_CLICKED)
            songTitleIntent.setClass(context, AudiomackWidget::class.java)
            val songTitlePendingIntent: PendingIntent = PendingIntent.getBroadcast(context, 0, songTitleIntent, 0)

            val albumIntent = Intent(ALBUM_CLICKED)
            albumIntent.setClass(context, AudiomackWidget::class.java)
            val albumPendingIntent: PendingIntent = PendingIntent.getBroadcast(context, 0, albumIntent, 0)

            val reupIntent = Intent(REUP_CLICKED)
            reupIntent.setClass(context, AudiomackWidget::class.java)
            val reupPendingIntent: PendingIntent = PendingIntent.getBroadcast(context, 0, reupIntent, 0)

            remoteView = RemoteViews(context.packageName, widgetLayout)
            remoteView.setProgressBar(R.id.progressView2, PROGRESS_BAR_MAX, PROGRESS_BAR_MIN, false)

            if (songTitle != null) {
                remoteView.setTextViewText(R.id.song_title, displayText)
                Picasso.get()
                    .load(songImage)
                    .config(Bitmap.Config.RGB_565)
                    .into(remoteView, R.id.song_image, appWidgetManager.getAppWidgetIds(ComponentName(context, AudiomackWidget::class.java)))
                remoteView.setProgressBar(R.id.progressView2, duration.toInt(), currentPos.toInt(), false)
                remoteView.setImageViewResource(R.id.heart, getHeartIcon(favorited, !isLocal))
                remoteView.setImageViewResource(R.id.reup, getReupIcon(reupped, !isLocal))
                setButtonColorActive()
                if (isRunning) {
                    remoteView.setImageViewResource(R.id.play, R.drawable.album_pause)
                } else {
                    remoteView.setViewPadding(R.id.play, PLAY_BUTTON_LEFT_PADDING, 0, 0, 0)
                }
            }

            remoteView.setOnClickPendingIntent(R.id.right_arrow, rightPendingIntent)
            remoteView.setOnClickPendingIntent(R.id.left_arrow, leftPendingIntent)
            remoteView.setOnClickPendingIntent(R.id.play, playPendingIntent)
            remoteView.setOnClickPendingIntent(R.id.heart, heartPendingIntent)
            remoteView.setOnClickPendingIntent(R.id.song_title, songTitlePendingIntent)
            remoteView.setOnClickPendingIntent(R.id.song_image, albumPendingIntent)
            remoteView.setOnClickPendingIntent(R.id.reup, reupPendingIntent)

            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteView)
        }

        fun getHeartIcon(songFavorited: Boolean, enabled: Boolean = true): Int = when {
            songFavorited && enabled -> R.drawable.ic_heart_filled
            !enabled -> R.drawable.ic_heart_empty_gray
            else -> R.drawable.ic_heart_empty
        }

        fun getReupIcon(songReupped: Boolean, enabled: Boolean = true): Int = when {
            songReupped && enabled -> R.drawable.ic_reup_active
            !enabled -> R.drawable.player_reup_off_gray
            else -> R.drawable.ic_reup
        }

        fun toggleHeartColor(songFavorited: Boolean, loggedIn: Boolean): Int {
            if (!loggedIn) {
                return if (songFavorited) R.drawable.ic_heart_empty else R.drawable.ic_heart_filled
            }
            return if (songFavorited) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty
        }

        fun toggleReupColor(songReupped: Boolean, loggedIn: Boolean): Int {
            if (!loggedIn) {
                return if (songReupped) R.drawable.ic_reup else R.drawable.ic_reup_active
            }
            return if (songReupped) R.drawable.ic_reup_active else R.drawable.ic_reup
        }

        fun updateWidgetFavorite(songFavorited: Boolean?) {
            val application = context?.applicationContext ?: return
            val favoritedIntent = Intent(SONG_FAVORITED)
            favoritedIntent.setClass(application, AudiomackWidget::class.java)
            favoritedIntent.putExtra(INTENT_KEY_FAVORITE, songFavorited)
            context?.sendBroadcast(favoritedIntent)
        }

        fun updateWidgetRepost(songReposted: Boolean?) {
            val application = context?.applicationContext ?: return
            val reupIntent = Intent(SONG_REPOSTED)
            reupIntent.setClass(application, AudiomackWidget::class.java)
            reupIntent.putExtra(INTENT_KEY_REPOST, songReposted)
            context?.sendBroadcast(reupIntent)
        }

        fun alertWidgetStatus(songPlaying: Boolean?) {
            val application = context?.applicationContext ?: return
            val intentUpdateStatus = Intent(UPDATE_PLAYPAUSE)
            intentUpdateStatus.setClass(application, AudiomackWidget::class.java)
            intentUpdateStatus.putExtra(INTENT_KEY_PLAYING, songPlaying)
            context?.sendBroadcast(intentUpdateStatus)
        }

        fun alertWidgetSeekBar(songProgress: Int) {
            val application = context?.applicationContext ?: return
            val intentUpdateSeekbar = Intent(UPDATE_SEEKBAR_PROGRESS)
            intentUpdateSeekbar.setClass(application, AudiomackWidget::class.java)
            intentUpdateSeekbar.putExtra(INTENT_KEY_PROGRESS, songProgress)
            context?.sendBroadcast(intentUpdateSeekbar)
        }

        fun alertWidgetNotLoggedIn(userLoggedIn: Boolean) {
            val application = context?.applicationContext ?: return
            val intentNotLoggedIn = Intent(NOT_LOGGED_IN)
            intentNotLoggedIn.setClass(application, AudiomackWidget::class.java)
            intentNotLoggedIn.putExtra(INTENT_KEY_LOGGED_IN, userLoggedIn)
            context?.sendBroadcast(intentNotLoggedIn)
        }

        fun alertWidgetStartTimer(songDuration: Long) {
            val application = context?.applicationContext ?: return
            val intentStartTimer = Intent(START_TIMER)
            intentStartTimer.setClass(application, AudiomackWidget::class.java)
            intentStartTimer.putExtra(START_TIMER_DURATION, songDuration)
            context?.sendBroadcast(intentStartTimer)
        }

        fun alertWidgetStopTimer() {
            val application = context?.applicationContext ?: return
            val intentStopTimer = Intent(STOP_TIMER)
            intentStopTimer.setClass(application, AudiomackWidget::class.java)
            context?.sendBroadcast(intentStopTimer)
        }

        fun newSong(song: AMResultItem) {
            val application = context?.applicationContext ?: return
            val intentNewSong = Intent(UPDATE_SONG)
            intentNewSong.setClass(application, AudiomackWidget::class.java)

            intentNewSong.putExtra(INTENT_KEY_TITLE, song.title)
            intentNewSong.putExtra(INTENT_KEY_ARTIST, song.artist)
            intentNewSong.putExtra(INTENT_KEY_FEAT, song.featured)
            intentNewSong.putExtra(INTENT_KEY_DURATION, song.duration)
            intentNewSong.putExtra(INTENT_KEY_FAVORITE, song.isFavorited)
            intentNewSong.putExtra(INTENT_KEY_REPOST, song.isReposted)
            intentNewSong.putExtra(INTENT_KEY_IMAGE, song.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall))
            intentNewSong.putExtra(INTENT_KEY_REPOST_ENABLED, !song.isUploadedByMyself(context) && !song.isLocal)
            intentNewSong.putExtra(INTENT_KEY_GENRE, song.genre)
            intentNewSong.putExtra(INTENT_KEY_IS_LOCAL, song.isLocal)
            context?.sendBroadcast(intentNewSong)
        }

        fun updateWidgetFavoriteAfterLogin() {
            val application = context?.applicationContext ?: return
            val intentFavAfterLogin = Intent(FAVORITE_AFTER_LOGIN)
            intentFavAfterLogin.setClass(application, AudiomackWidget::class.java)
            context?.sendBroadcast(intentFavAfterLogin)
        }

        fun updateCircularLoadingBar(visibility: Int) {
            val application = context?.applicationContext ?: return
            val intent = Intent(UPDATE_LOADING_CIRCLE).apply {
                setClass(application, AudiomackWidget::class.java)
                putExtra(INTENT_KEY_LOADING_VIEW, visibility)
            }
            context?.sendBroadcast(intent)
        }

        fun setButtonColorInActive() {
            remoteView.setImageViewResource(R.id.play, R.drawable.album_play_gray)
            remoteView.setImageViewResource(R.id.heart, R.drawable.ic_heart_empty_gray)
            remoteView.setImageViewResource(R.id.reup, R.drawable.player_reup_off_gray)
            remoteView.setImageViewResource(R.id.left_arrow, if (isPodcast) R.drawable.ic_skip_back_15_disabled else R.drawable.player_prev_gray)
            remoteView.setImageViewResource(R.id.right_arrow, if (isPodcast) R.drawable.ic_skip_forward_30_disabled else R.drawable.player_next_gray)
        }

        fun setButtonColorActive() {
            remoteView.setImageViewResource(R.id.play, R.drawable.album_play)
            remoteView.setImageViewResource(R.id.heart, R.drawable.player_favorite_off)
            remoteView.setImageViewResource(R.id.reup, R.drawable.ic_reup)
            remoteView.setImageViewResource(R.id.left_arrow, if (isPodcast) R.drawable.ic_skip_back_15 else R.drawable.player_prev)
            remoteView.setImageViewResource(R.id.right_arrow, if (isPodcast) R.drawable.ic_skip_forward_30 else R.drawable.player_next)
        }

        fun updateRemoteView() {
            if (isRemoteViewInitialized()) {
                context?.applicationContext?.let { applicationContext ->
                    try {
                        val component = ComponentName(applicationContext, AudiomackWidget::class.java)
                        val widgetIds =
                            AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(component)
                        AppWidgetManager.getInstance(applicationContext)
                            .partiallyUpdateAppWidget(widgetIds, remoteView)
                    } catch (e: IllegalStateException) {
                        // Most likely the device is locked
                        Timber.tag(TAG).e(e)
                    } catch (e: RuntimeException) {
                        // Most likely there is a huge image loaded in the RemoteViews
                        Timber.tag(TAG).e(e)
                        TrackingRepository().trackException(Exception("TransactionTooLargeException on widget, image url $songImage"))
                    }
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {}

    // Called on resizing
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager?, appWidgetId: Int, newOptions: Bundle?) {

        if (!isRemoteViewInitialized()) return

        val minWidth = newOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

        // Based on formula in https://developer.android.com/guide/practices/ui_guidelines/widget_design.html#anatomy
        when ((minWidth?.plus(30))?.div(70)) {
            WIDGET_RESIZE_3_COLUMN -> {
                remoteView.setViewVisibility(R.id.reup, View.GONE)
                remoteView.setViewPadding(R.id.song_title, 0, 0, 1, 0)
            }
            WIDGET_RESIZE_4_COLUMN -> {
                remoteView.setViewVisibility(R.id.reup, View.VISIBLE)
                remoteView.setViewPadding(R.id.song_title, 7, 0, 5, 0)
            }
            else -> {
                remoteView.setViewVisibility(R.id.reup, View.VISIBLE)
                remoteView.setViewPadding(R.id.song_title, 22, 0, 8, 0)
            }
        }

        appWidgetManager?.updateAppWidget(appWidgetId, remoteView)
    }

    private fun createTimer() {

        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(((duration - currentPos) * 1000), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                isRunning = true
                currentPos++

                if (isRemoteViewInitialized()) {
                    remoteView.setProgressBar(
                        R.id.progressView2,
                        (duration * 1000).toInt(),
                        ((duration * 1000) - millisUntilFinished).toInt(),
                        false
                    )
                }

                updateRemoteView()
            }

            override fun onFinish() {
                if (isRemoteViewInitialized()) {
                    remoteView.setProgressBar(
                        R.id.progressView2,
                        PROGRESS_BAR_MAX,
                        PROGRESS_BAR_MIN,
                        false
                    )
                }
            }
        }

        countDownTimer?.start()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        isRunning = false
    }

    private fun swapPlayPauseButton(showPlayButton: Boolean, remoteView: RemoteViews) {
        if (showPlayButton) {
            remoteView.setImageViewResource(R.id.play, R.drawable.album_pause)
            remoteView.setViewPadding(R.id.play, 0, 0, 0, 0)
        } else {
            remoteView.setImageViewResource(R.id.play, R.drawable.album_play)
            remoteView.setViewPadding(R.id.play, PLAY_BUTTON_LEFT_PADDING, 0, 0, 0)
            stopTimer()
        }
    }

    private fun createDisplayText() {
        displayText = "$songArtist - $songTitle"

        if (!songFeat.isNullOrBlank()) {
            displayText = displayText + " " + (context?.getString(R.string.feat_inline) ?: "") + " " + songFeat
        }
    }

    private fun openHome(context: Context) {
        val homeIntent = Intent(context, HomeActivity::class.java)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        homeIntent.data = Uri.parse("audiomack://nowplaying")
        context.startActivity(homeIntent)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        val intent = intent ?: return
        val context = context ?: return

        try {
            when (intent.action) {
                PLAY_CLICKED -> {
                    if (HomeActivity.instance != null && displayText != null) {
                        val playerPause = EventPlayer(PlayerCommand.TOGGLE_PLAY)
                        EventBus.getDefault().post(playerPause)
                    } else {
                        openHome(context)
                    }
                }

                RIGHT_CLICKED -> {
                    if (HomeActivity.instance != null && displayText != null) {
                        remoteView.setProgressBar(
                            R.id.progressView2,
                            PROGRESS_BAR_MAX,
                            PROGRESS_BAR_MIN,
                            false
                        )

                        updateRemoteView()

                        val playerNext = EventPlayer(PlayerCommand.NEXT)
                        EventBus.getDefault().post(playerNext)
                    } else {
                        openHome(context)
                    }
                }

                LEFT_CLICKED -> {
                    if (HomeActivity.instance != null && displayText != null) {
                        currentPos = 0

                        remoteView.setProgressBar(
                            R.id.progressView2,
                            PROGRESS_BAR_MAX,
                            PROGRESS_BAR_MIN,
                            false
                        )

                        updateRemoteView()

                        val playerPrev = EventPlayer(PlayerCommand.PREV)
                        EventBus.getDefault().post(playerPrev)
                    } else {
                        openHome(context)
                    }
                }

                HEART_CLICKED -> {
                    if (isLocal) return

                    if (HomeActivity.instance != null && displayText != null) {
                        context.sendBroadcast(Intent(INTENT_TOGGLE_FAVORITE).apply {
                            putExtra(INTENT_EXTRA_MIXPANEL_BUTTON, MixpanelButtonWidget)
                        })
                    } else {
                        openHome(context)
                    }
                }

                REUP_CLICKED -> {
                    if (!reupEnabled) {
                        return
                    }
                    if (HomeActivity.instance != null && displayText != null) {
                        context.sendBroadcast(Intent(INTENT_TOGGLE_REPOST).apply {
                            putExtra(INTENT_EXTRA_MIXPANEL_BUTTON, MixpanelButtonWidget)
                        })
                    } else {
                        openHome(context)
                    }
                }

                SONG_TITLE_CLICKED -> {
                    openHome(context)
                }

                ALBUM_CLICKED -> {
                    openHome(context)
                }

                UPDATE_SONG -> {
                    remoteView = RemoteViews(context.packageName, widgetLayout)
                    setButtonColorActive()
                    if (isRunning && countDownTimer != null) {
                        stopTimer()
                    }

                    songTitle = intent.getStringExtra(INTENT_KEY_TITLE)
                    songArtist = intent.getStringExtra(INTENT_KEY_ARTIST)
                    songFeat = intent.getStringExtra(INTENT_KEY_FEAT)
                    songImage = intent.getStringExtra(INTENT_KEY_IMAGE)
                    favorited = intent.getBooleanExtra(INTENT_KEY_FAVORITE, false)
                    reupped = intent.getBooleanExtra(INTENT_KEY_REPOST, false)
                    duration = intent.getLongExtra(INTENT_KEY_DURATION, 0)
                    reupEnabled = intent.getBooleanExtra(INTENT_KEY_REPOST_ENABLED, false)
                    isPodcast = AMGenre.Podcast.apiValue() == intent.getStringExtra(INTENT_KEY_GENRE)
                    isLocal = intent.getBooleanExtra(INTENT_KEY_IS_LOCAL, false)

                    createDisplayText()

                    remoteView.setTextViewText(R.id.song_title, displayText)
                    remoteView.setImageViewResource(R.id.heart, getHeartIcon(favorited, !isLocal))
                    remoteView.setImageViewResource(R.id.reup, getReupIcon(reupped, !isLocal))
                    Picasso.get()
                        .load(songImage)
                        .config(Bitmap.Config.RGB_565)
                        .into(
                            remoteView,
                            R.id.song_image,
                            AppWidgetManager.getInstance(context).getAppWidgetIds(
                                ComponentName(context, AudiomackWidget::class.java)
                            )
                        )
                    remoteView.setImageViewResource(
                        R.id.left_arrow,
                        if (isPodcast) R.drawable.ic_skip_back_15 else R.drawable.player_prev
                    )
                    remoteView.setImageViewResource(
                        R.id.right_arrow,
                        if (isPodcast) R.drawable.ic_skip_forward_30 else R.drawable.player_next
                    )

                    currentPos = 0

                    AppWidgetManager.getInstance(context)
                        .updateAppWidget(ComponentName(context, this.javaClass), remoteView)
                }

                START_TIMER -> {
                    duration = intent.getLongExtra(START_TIMER_DURATION, 0) / 1000
                    createTimer()
                }

                STOP_TIMER -> {
                    stopTimer()
                }

                UPDATE_PLAYPAUSE -> {

                    if (!isRemoteViewInitialized()) return

                    if (intent.hasExtra(INTENT_KEY_CURRENTPOS)) {
                        currentPos = intent.getIntExtra(INTENT_KEY_CURRENTPOS, 0).toLong() / 1000
                        duration = intent.getIntExtra(INTENT_KEY_DURATION, 0).toLong() / 1000
                    }

                    swapPlayPauseButton(
                        intent.getBooleanExtra(INTENT_KEY_PLAYING, false),
                        remoteView
                    )

                    updateRemoteView()
                }

                UPDATE_SEEKBAR_PROGRESS -> {
                    currentPos = (intent.getIntExtra(INTENT_KEY_PROGRESS, 0).toLong()) / 1000
                    if (!isRunning) {
                        remoteView.setProgressBar(
                            R.id.progressView2,
                            duration.toInt(),
                            currentPos.toInt(),
                            false
                        )

                        AppWidgetManager.getInstance(context).updateAppWidget(
                            ComponentName(context, AudiomackWidget::class.java), remoteView
                        )
                    } else {
                        createTimer()
                    }
                }

                UPDATE_SEEKBAR_NEW_LAYOUT -> createTimer()

                SONG_FAVORITED -> {
                    favorited = intent.getBooleanExtra(INTENT_KEY_FAVORITE, false)

                    if (!isRemoteViewInitialized()) return

                    remoteView.setImageViewResource(
                        R.id.heart,
                        toggleHeartColor(favorited, loggedIn)
                    )

                    updateRemoteView()
                }

                SONG_REPOSTED -> {
                    reupped = intent.getBooleanExtra(INTENT_KEY_REPOST, false)

                    if (!isRemoteViewInitialized()) return

                    remoteView.setImageViewResource(R.id.reup, toggleReupColor(reupped, loggedIn))

                    updateRemoteView()
                }

                NOT_LOGGED_IN -> {
                    loggedIn = intent.getBooleanExtra(INTENT_KEY_LOGGED_IN, false)
                }

                FAVORITE_AFTER_LOGIN -> {
                    loggedIn = true

                    if (!isRemoteViewInitialized()) return

                    remoteView.setImageViewResource(
                        R.id.heart,
                        toggleHeartColor(favorited, loggedIn)
                    )

                    updateRemoteView()
                }

                UPDATE_LOADING_CIRCLE -> {
                    if (!isRemoteViewInitialized()) return
                    val loadingStatus = intent.getIntExtra(INTENT_KEY_LOADING_VIEW, 0)

                    remoteView.setViewVisibility(R.id.indeterminateBar, loadingStatus)
                    when (loadingStatus) { // Play button visible when loading circle is not and vice versa
                        View.VISIBLE -> remoteView.setViewVisibility(R.id.play, View.INVISIBLE)
                        View.INVISIBLE -> remoteView.setViewVisibility(R.id.play, View.VISIBLE)
                        View.GONE -> remoteView.setViewVisibility(R.id.play, View.VISIBLE)
                    }

                    updateRemoteView()
                }

                DESTROYED -> { // App closed
                    stopTimer()

                    duration = 0
                    displayText = AUDIOMACK_TITLE
                    songImage = DEFAULT_SONG_IMAGE
                    favorited = false
                    reupped = false
                    isRunning = false
                    isAppOpen = false
                    reupEnabled = false

                    if (!isRemoteViewInitialized()) return

                    remoteView.setImageViewResource(R.id.play, R.drawable.album_play)
                    remoteView.setImageViewResource(R.id.heart, R.drawable.ic_heart_empty)
                    remoteView.setImageViewResource(R.id.reup, R.drawable.player_reup_off)
                    remoteView.setViewPadding(R.id.play, PLAY_BUTTON_LEFT_PADDING, 0, 0, 0)
                    remoteView.setViewVisibility(R.id.indeterminateBar, View.GONE)
                    remoteView.setTextViewText(R.id.song_title, displayText)
                    remoteView.setProgressBar(
                        R.id.progressView2,
                        PROGRESS_BAR_MAX,
                        PROGRESS_BAR_MIN,
                        false
                    )
                    Picasso.get()
                        .load(songImage)
                        .config(Bitmap.Config.RGB_565)
                        .into(
                            remoteView,
                            R.id.song_image,
                            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, AudiomackWidget::class.java)
                        )
                    )

                    setButtonColorInActive()

                    updateRemoteView()
                }
            }
        } catch (e: Exception) {
        }
    }
}
