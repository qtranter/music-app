package com.audiomack.playback

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.RGB_565
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.Builder
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media.MediaBrowserServiceCompat
import androidx.media.VolumeProviderCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.audiomack.CHROMECAST_NAMESPACE
import com.audiomack.INTENT_CLOSE
import com.audiomack.INTENT_EXTRA_MIXPANEL_BUTTON
import com.audiomack.INTENT_OPEN_PLAYER
import com.audiomack.INTENT_TOGGLE_FAVORITE
import com.audiomack.INTENT_TOGGLE_REPOST
import com.audiomack.NOTIFICATION_CHANNEL_GENERAL_ID
import com.audiomack.NOTIFICATION_CHANNEL_PLAYBACK_ID
import com.audiomack.NOTIFICATION_REQUEST_CODE
import com.audiomack.R
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleRepostException
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.ads.AdsWizzManager
import com.audiomack.data.ads.AudioAdManager
import com.audiomack.data.ads.AudioAdState
import com.audiomack.data.device.DeviceDataSource
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.imageloader.ImageLoaderCallback
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.data.queue.QueueRepository
import com.audiomack.data.storage.StorageProvider
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonPlayerNotification
import com.audiomack.model.AMGenre
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItem.ItemImagePreset.ItemImagePresetSmall
import com.audiomack.model.EventFavoriteStatusChanged
import com.audiomack.model.EventPlayer
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.LoginSignupSource.Favorite
import com.audiomack.model.LoginSignupSource.Repost
import com.audiomack.model.PlayerCommand
import com.audiomack.network.AnalyticsHelper
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.widget.AudiomackWidget
import com.audiomack.utils.CastUtils
import com.audiomack.utils.album
import com.audiomack.utils.albumArt
import com.audiomack.utils.artist
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.id
import com.audiomack.utils.isMediaStoreUri
import com.audiomack.utils.isPlayEnabled
import com.audiomack.utils.isPlaying
import com.audiomack.utils.isWebUrl
import com.audiomack.utils.showRepostedToast
import com.audiomack.utils.title
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.audio.AudioListener
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.DefaultMediaMetadataProvider
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.QueueNavigator
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlin.math.max
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import timber.log.Timber

class MusicService : MediaBrowserServiceCompat(), SessionAvailabilityListener, AudioListener {

    private lateinit var mediaSession: MediaSessionCompat

    private val mediaController: MediaControllerCompat by lazy {
        MediaControllerCompat(this, mediaSession)
    }

    private val mediaSessionConnector: MediaSessionConnector by lazy {
        MediaSessionConnector(mediaSession)
    }

    private val queueNavigator: QueueNavigator by lazy {
        object : TimelineQueueNavigator(mediaSession) {
            override fun getSupportedQueueNavigatorActions(player: Player): Long =
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

            override fun onSkipToNext(player: Player, dispatcher: ControlDispatcher) =
                playback.next()

            override fun onSkipToPrevious(player: Player, dispatcher: ControlDispatcher) =
                playback.prev()

            override fun getMediaDescription(player: Player, windowIndex: Int) =
                buildMediaDescription()
        }
    }

    private val playerCommandsReceiver = PlayerCommandsReceiver()

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            addAudioListener(this@MusicService)
            setHandleAudioBecomingNoisy(true)
            setHandleWakeLock(true)
        }
    }

    private var castContext: CastContext? = null
    private var castPlayer: CastPlayer? = null
    private val castSession: CastSession?
        get() = castContext?.sessionManager?.currentCastSession
    private val isCastPlayer: Boolean
        get() = playback.isPlayer(castPlayer)

    private val playback: Playback by lazy { PlayerPlayback.getInstance() }
    private val currentItem: PlaybackItem?
        get() = playback.item.value

    private val queueRepository: QueueDataSource = QueueRepository.getInstance()
    private val premiumRepository: PremiumDataSource by lazy { PremiumRepository.init(this) }
    private val musicServiceUseCase: MusicServiceUseCase = MusicServiceUseCaseImpl()
    private val deviceDataSource: DeviceDataSource = DeviceRepository
    private val trackingDataSource: TrackingDataSource = TrackingRepository()
    private val audioAdManager: AudioAdManager by lazy { AdsWizzManager.getInstance() }
    private val sourceProvider: Sources by lazy { SourceProvider.init(this) }

    private var equalizer: Equalizer? = null

    private var isForegroundService = false

    private var currentArtworkBitmap: Bitmap? = null

    private val disposables = CompositeDisposable()

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(this)
    }
    private val notificationBuilder: NotificationBuilder by lazy {
        NotificationBuilder(this, audioAdManager)
    }

    private val wifiLock: WifiManager.WifiLock by lazy {
        (getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_TAG).apply {
                setReferenceCounted(false)
            }
    }

    override fun onCreate() {
        super.onCreate()

        Timber.tag(TAG).i("Music service created")
        trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - created")

        StorageProvider.init(this)

        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(getNotificationLaunchIntent())
            isActive = true
        }.also { session ->
            sessionToken = session.sessionToken
        }

        try {
            castContext = CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            Timber.tag(TAG).w("Unable to get shared cast context")
            deviceDataSource.castAvailable = false
        }

        castContext?.let { ctx ->
            castPlayer = CastPlayer(ctx).apply {
                setSessionAvailabilityListener(this@MusicService)
                addListener(playback)
            }
        }

        val player = castPlayer?.takeIf { it.isCastSessionAvailable } ?: exoPlayer

        // Since ExoPlayer will manage the MediaSession, listen for state changes.
        mediaController.registerCallback(MediaControllerCallback())

        // Let ExoPlayer manage the MediaSession
        mediaSessionConnector.apply {
            setPlayer(player)
            // We must handle queue navigation since we're not using ConcatenatingMediaSource
            setQueueNavigator(queueNavigator)
            // Supply the metadata for remote clients
            setMediaMetadataProvider { buildMediaMetadata(player) }
            setFastForwardIncrementMs(SKIP_FORWARD_DURATION.toInt())
            setRewindIncrementMs(SKIP_BACK_DURATION.toInt())
        }

        playback.apply {
            setPlayer(player)
            // Listen for changes to the current playback item
            item.observeOn(AndroidSchedulers.mainThread())
                .subscribe { onPlaybackItemChange(it) }
                .also { disposables.add(it) }
        }

        registerReceiver(
            playerCommandsReceiver,
            IntentFilter().apply {
                addAction(INTENT_TOGGLE_REPOST)
                addAction(INTENT_TOGGLE_FAVORITE)
                addAction(INTENT_CLOSE)
            }
        )

        EventBus.getDefault().register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).i("onStartCommand called with intent = $intent")

        if (intent?.getBooleanExtra(EXTRA_PLAY_WHEN_READY, false) == true) {
            playback.reload()
        }

        sessionToken?.let { token ->
            val notification = notificationBuilder.buildNotification(token)
            startForeground(NOW_PLAYING_NOTIFICATION, notification)
            isForegroundService = true
        }

        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Stop playback and service when swiping the app away from recents.
     */
    override fun onTaskRemoved(rootIntent: Intent) {
        Timber.tag(TAG).i("onTaskRemoved() called")

        trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - task removed")

        // Don't kill the service if casting
        if (isCastPlayer) return

        stopForeground(true)
        isForegroundService = false
        playback.stop(false)
        stopSelf()
    }

    override fun onDestroy() {
        trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - destroyed")

        playback.release()
        mediaSession.run {
            isActive = false
            release()
        }
        exoPlayer.release()
        castPlayer?.apply {
            setSessionAvailabilityListener(null)
            release()
        }
        unregisterReceiver(playerCommandsReceiver)
        disposables.clear()
        releaseEqualizer()
        EventBus.getDefault().unregister(this)
        notifyWidgetAppDestroyed()

        if (wifiLock.isHeld) wifiLock.release()

        Timber.tag(TAG).i("Music service destroyed")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        // Clients can connect, but this BrowserRoot is an empty hierarchy
        // so onLoadChildren returns nothing. This disables the ability to browse for content.
        return BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentMediaId: String,
        result: Result<List<MediaItem>>
    ) {
        // Browsing not allowed
        if (MY_EMPTY_MEDIA_ROOT_ID == parentMediaId) {
            result.sendResult(listOf())
            return
        }
    }

    override fun onCastSessionAvailable() {
        Timber.tag(TAG).i("onCastSessionAvailable() : Connected to cast session")
        trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - connected to cast session")
        // TODO Set MediaSession to inactive once using Cast MediaSession
        setCurrentPlayer(castPlayer)
        // Remote media callbacks don't include an "end" event, so we use a custom message.
        castSession?.setMessageReceivedCallbacks(CHROMECAST_NAMESPACE, messageReceivedCallback)
    }

    override fun onCastSessionUnavailable() {
        Timber.tag(TAG).i("onCastSessionUnavailable() : Disconnected from cast session")
        trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - disconnected from cast session")
        // TODO Set MediaSession to active once using Cast MediaSession and playlists
        setCurrentPlayer(exoPlayer)
        castSession?.removeMessageReceivedCallbacks(CHROMECAST_NAMESPACE)
    }

    override fun onAudioSessionId(audioSessionId: Int) {
        Timber.tag(TAG).d("onAudioSessionId() called with $audioSessionId")
        playback.audioSessionId = audioSessionId

        if (premiumRepository.isPremium) {
            initEqualizer()
        }
    }

    private fun setCurrentPlayer(newPlayer: Player?) {
        if (newPlayer == null || playback.isPlayer(newPlayer)) return

        var playbackPosition = C.TIME_UNSET
        var playWhenReady = false

        if (!playback.isEnded) {
            playbackPosition = playback.position
            playWhenReady = playback.isPlaying
        }
        playback.pause()

        playback.setPlayer(newPlayer)
        mediaSessionConnector.setPlayer(newPlayer)

        // Because we're not using playlists, we have to keep our own MediaSession alive while casting.
        // TODO Remove when switched to playlists and re-enable the Cast MediaSession
        castSession?.let { session ->
            try {
                CastVolumeProvider(session)
            } catch (e: Exception) {
                trackingDataSource.trackException(e)
                null
            }
        }?.let { castVolumeProvider ->
            mediaSession.setPlaybackToRemote(castVolumeProvider)
        } ?: run {
            mediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC)
        }

        play(playbackPosition, playWhenReady)
    }

    private fun onPlaybackItemChange(playbackItem: PlaybackItem) {
        Timber.tag(TAG).i("onPlaybackItemChange called with $playbackItem")
        trackingDataSource.trackBreadcrumb("${javaClass.simpleName} - playback item changed to ${playbackItem.track.itemId}")
        play(playbackItem.position, playbackItem.playWhenReady)
        loadArtwork(playbackItem)
    }

    private fun loadArtwork(playbackItem: PlaybackItem) {
        PicassoImageLoader.load(
            this,
            playbackItem.track.getImageURLWithPreset(ItemImagePresetSmall),
            config = RGB_565,
            callback = object : ImageLoaderCallback {
                override fun onBitmapLoaded(bitmap: Bitmap?) {
                    currentArtworkBitmap = bitmap
                    mediaSessionConnector.invalidateMediaSessionQueue()
                }

                override fun onBitmapFailed(errorDrawable: Drawable?) {
                    currentArtworkBitmap = null
                    mediaSessionConnector.invalidateMediaSessionQueue()
                }
            }
        )
    }

    private fun play(
        playbackPosition: Long = C.TIME_UNSET,
        whenReady: Boolean = false
    ) {
        val item = currentItem ?: return

        if (playback.isPlayer(exoPlayer)) {
            // Preparing a new media source will invalidate the current MediaSession queue
            val mediaSource = sourceProvider.buildMediaSource(item.uri)
            exoPlayer.apply {
                prepare(mediaSource)
                seekTo(0, playbackPosition)
                playWhenReady = whenReady
            }
        } else {
            if (!item.streamUrl.isWebUrl()) {
                playback.stop()
                playback.reload()
                return
            }

            val mediaQueueItem = CastUtils.buildMediaQueueItem(
                this,
                item.track,
                whenReady,
                item.streamUrl
            )
            castPlayer?.run {
                loadItem(mediaQueueItem, playbackPosition)
            }
        }
    }

    private fun buildMediaDescription(): MediaDescriptionCompat {
        val builder = MediaDescriptionCompat.Builder()

        currentItem?.let { item ->
            val song = item.track
            val artist = getArtist(song)

            builder
                .setMediaUri(Uri.parse(item.streamUrl))
                .setMediaId(song.itemId)
                .setTitle(song.title)
                .setSubtitle(artist)
                .setDescription(song.album ?: song.playlist)
                .setIconBitmap(currentArtworkBitmap)
                .setExtras(bundleOf(
                    MediaMetadataCompat.METADATA_KEY_RATING to musicServiceUseCase.isFavorite(song),
                    MediaMetadataCompat.METADATA_KEY_GENRE to song.genre
                ))
        }

        return builder.build()
    }

    private fun buildMediaMetadata(player: Player): MediaMetadataCompat {
        return Builder(
            DefaultMediaMetadataProvider(mediaController, null).getMetadata(player)
        ).apply {
            currentItem?.track?.let { song ->
                id = song.itemId
                artist = getArtist(song)
                album = song.album ?: song.playlist
                title = song.title
                albumArt = currentArtworkBitmap
            }
        }.build()
    }

    private fun getArtist(song: AMResultItem): String? {
        return if (!song.featured.isNullOrBlank()) {
            getString(R.string.feat_x, song.artist, song.featured)
        } else {
            song.artist
        }
    }

    private fun getNotificationLaunchIntent(): PendingIntent? {
        val openIntent = Intent(this, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(INTENT_OPEN_PLAYER, true)
        return PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun initEqualizer() {
        playback.audioSessionId?.let { audioSessionId ->
            try {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = true
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Error while instantiating equalizer")
            }

            val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            sendBroadcast(intent)
        }
    }

    private fun releaseEqualizer() {
        equalizer?.apply {
            enabled = false
            release()
        }
        equalizer = null

        playback.audioSessionId?.let { audioSessionId ->
            val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            sendBroadcast(intent)
        }
    }

    private fun togglePlayer() {
        playback.apply {
            if (isPlaying) pause() else play()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventPlayer: EventPlayer) {
        Timber.tag(TAG).i("onMessageEvent - EventPlayer command: ${eventPlayer.command}")
        when (eventPlayer.command) {
            PlayerCommand.TOGGLE_PLAY -> togglePlayer()
            PlayerCommand.PREV -> {
                if (currentItem.isPodcast()) playback.rewind() else playback.prev()
            }
            PlayerCommand.NEXT -> {
                if (currentItem.isPodcast()) playback.fastForward() else playback.next()
            }
            else -> return
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(eventFavoriteStatusChanged: EventFavoriteStatusChanged) {
        queueRepository.currentItem?.let { song ->
            if (song.itemId == eventFavoriteStatusChanged.itemId) {
                mediaSessionConnector.invalidateMediaSessionQueue()
            }
        }
    }

    private fun notifyWidgetAppDestroyed() {
        val intentWidgetDestroyed = Intent(this, AudiomackWidget::class.java)
        intentWidgetDestroyed.action = AudiomackWidget.DESTROYED
        sendBroadcast(intentWidgetDestroyed)
    }

    /**
     * Listens for "end" custom message.
     */
    private val messageReceivedCallback = Cast.MessageReceivedCallback { _, _, message ->
        Timber.tag(TAG).i("Cast.MessageReceivedCallback - onMessageReceived: $message")
        message?.let {
            val type = JSONObject(message).optString("type")
            if ("ended" == type) {
                playback.onPlayerStateChanged(playback.isPlaying, Player.STATE_ENDED)
            }
        }
    }

    /**
     * Class to receive callbacks about state changes to the [MediaSessionCompat]. In response
     * to those callbacks, this class:
     *
     * - Build/update the service's notification.
     * - Register/unregister a broadcast receiver for [AudioManager.ACTION_AUDIO_BECOMING_NOISY].
     * - Calls [Service.startForeground] and [Service.stopForeground].
     */
    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onRepeatModeChanged(repeatMode: Int) {
            when (repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ONE -> playback.repeat(RepeatType.ONE)
                PlaybackStateCompat.REPEAT_MODE_ALL, PlaybackStateCompat.REPEAT_MODE_GROUP ->
                    playback.repeat(RepeatType.ALL)
                else -> playback.repeat(RepeatType.OFF)
            }
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            queueRepository.setShuffle(
                shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL ||
                    shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_GROUP
            )
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
//            Timber.tag(TAG).i("onMetadataChanged() called with metadata = ${metadata?.description}")
            mediaController.playbackState?.let { updateNotification(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let { updateNotification(it) }
        }

        private fun updateNotification(state: PlaybackStateCompat) {
            val updatedState = state.state

            // Skip building a notification when state is "none" and metadata is null.
            val notification = if (mediaController.metadata != null &&
                updatedState != PlaybackStateCompat.STATE_NONE
            ) {
                notificationBuilder.buildNotification(mediaSession.sessionToken)
            } else {
                null
            }

            when (updatedState) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING -> {
                    if (!wifiLock.isHeld) wifiLock.acquire()

                    if (notification != null) {
                        try {
                            notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                        } catch (e: Exception) {
                            // TODO Figure out this "bad array lengths"/"DeadSystemException" crash
                            AnalyticsHelper.getInstance().trackException(e)
                        }

                        if (!isForegroundService) {
                            ContextCompat.startForegroundService(
                                applicationContext,
                                Intent(applicationContext, this@MusicService.javaClass)
                            )
                            startForeground(NOW_PLAYING_NOTIFICATION, notification)
                            isForegroundService = true
                        }
                    }
                }
                else -> {
                    if (wifiLock.isHeld) wifiLock.release()

                    if (isForegroundService) {
                        if (notification != null) {
                            notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                        }
                    }
                }
            }
        }
    }

    /**
     * Class to receive broadcasts when a song is favorited/reposted from a remote source,
     * like playback notification or app widget.
     */
    private inner class PlayerCommandsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.tag("PlayerCommandsReceiver").i("onReceive - intent: $intent")
            when (intent.action ?: return) {
                INTENT_TOGGLE_FAVORITE -> toggleFavorite(intent)
                INTENT_TOGGLE_REPOST -> toggleRepost(intent)
                INTENT_CLOSE -> onTaskRemoved(intent)
            }
        }

        private fun toggleFavorite(intent: Intent) {
            val mixpanelButton = intent.getStringExtra(INTENT_EXTRA_MIXPANEL_BUTTON) ?: return
            val song = currentItem?.track ?: return
            musicServiceUseCase.toggleFavorite(song, mixpanelButton)
                .subscribe({ result ->
                    if (result is ToggleFavoriteResult.Notify) {
                        Timber.tag(TAG).d("Successfully toggled favorite for $song")
                        // Invalidate the Queue so the favorite icon is updated
                        mediaSessionConnector.invalidateMediaSessionQueue()
                        notificationBuilder.buildNotification(mediaSession.sessionToken).also {
                            notificationManager.notify(NOW_PLAYING_NOTIFICATION, it)
                        }
                    }
                }, {
                    Timber.tag(TAG).w(it, "Unable to favorite $song")
                    when (it) {
                        is ToggleFavoriteException.LoggedOut -> {
                            showLoginNotification(Favorite)
                        }
                        is ToggleFavoriteException.Offline -> {
                            showOfflineNotification()
                        }
                    }
                }).also { disposables.add(it) }
        }

        private fun toggleRepost(intent: Intent) {
            val mixpanelButton = intent.getStringExtra(INTENT_EXTRA_MIXPANEL_BUTTON) ?: return
            val song = currentItem?.track ?: return
            musicServiceUseCase.toggleRepost(song, mixpanelButton)
                .subscribe({
                    if (it is ToggleRepostResult.Notify) showRepostedToast(it)
                }, {
                    Timber.tag(TAG).w(it, "Unable to repost $song")
                    when (it) {
                        is ToggleRepostException.LoggedOut -> {
                            showLoginNotification(Repost)
                        }
                        is ToggleRepostException.Offline -> {
                            showOfflineNotification()
                        }
                    }
                }).also { disposables.add(it) }
        }

        private fun showLoginNotification(source: LoginSignupSource) {
            val context = this@MusicService
            val builder = NotificationCompat.Builder(
                context,
                NOTIFICATION_CHANNEL_GENERAL_ID
            )

            val intent = Intent(context, HomeActivity::class.java).apply {
                action = HomeActivity.ACTION_LOGIN_REQUIRED
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            when (source) {
                Favorite -> {
                    intent.putExtra(HomeActivity.EXTRA_LOGIN_FAVORITE, true)
                    builder.setContentText(getString(R.string.notif_login_favorite))
                }
                Repost -> {
                    intent.putExtra(HomeActivity.EXTRA_LOGIN_REPOST, true)
                    builder.setContentText(getString(R.string.notif_login_repost))
                }
                else -> {
                    builder.setContentText(getString(R.string.login_needed_message))
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )

            builder
                .setContentIntent(pendingIntent)
                .setContentTitle(getString(R.string.notif_login_required))
                .setSmallIcon(R.drawable.notification_icon)
                .setColor(context.colorCompat(R.color.orange))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .build()
                .also {
                    notificationManager.notify(ERROR_NOTIFICATION, it)
                }
        }

        private fun showOfflineNotification() {
            val context = this@MusicService
            val intent = Intent(context, HomeActivity::class.java).apply {
                action = HomeActivity.ACTION_NOTIFY_OFFLINE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )

            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_GENERAL_ID)
                .setContentIntent(pendingIntent)
                .setContentTitle(getString(R.string.player_extra_offline_placeholder_title))
                .setContentText(getString(R.string.player_extra_offline_placeholder_subtitle))
                .setSmallIcon(R.drawable.notification_icon)
                .setColor(context.colorCompat(R.color.orange))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .build()
                .also {
                    notificationManager.notify(ERROR_NOTIFICATION, it)
                }
        }
    }

    private class NotificationBuilder(
        private val context: Context,
        private val audioAdManager: AudioAdManager
    ) {
        private val skipToPreviousAction = NotificationCompat.Action(
            R.drawable.notification_player_prev,
            context.getString(R.string.player_prev),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
        )
        private val playAction = NotificationCompat.Action(
            R.drawable.notification_player_play,
            context.getString(R.string.player_play),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_PLAY
            )
        )
        private val pauseAction = NotificationCompat.Action(
            R.drawable.notification_player_pause,
            context.getString(R.string.player_pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_PAUSE
            )
        )
        private val skipToNextAction = NotificationCompat.Action(
            R.drawable.notification_player_next,
            context.getString(R.string.player_next),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
        )
        private val favPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_REQUEST_CODE,
            Intent(INTENT_TOGGLE_FAVORITE).apply {
                putExtra(INTENT_EXTRA_MIXPANEL_BUTTON, MixpanelButtonPlayerNotification)
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        private val stopPendingIntent =
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_STOP
            )
        private val cancelAction = NotificationCompat.Action(
            R.drawable.notification_player_close,
            context.getString(R.string.player_close),
            PendingIntent.getBroadcast(
                context,
                NOTIFICATION_REQUEST_CODE,
                Intent(INTENT_CLOSE),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        private val fastForwardAction = NotificationCompat.Action(
            R.drawable.ic_skip_forward_30,
            context.getString(R.string.player_skip_forward),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_FAST_FORWARD
            )
        )
        private val rewindAction = NotificationCompat.Action(
            R.drawable.ic_skip_back_15,
            context.getString(R.string.player_skip_back),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_REWIND
            )
        )
        private val favoriteAction = NotificationCompat.Action(
            R.drawable.ic_notif_heart_empty,
            context.getString(R.string.player_fav),
            favPendingIntent
        )
        private val unFavoriteAction = NotificationCompat.Action(
            R.drawable.ic_notif_heart_filled,
            context.getString(R.string.player_unfav),
            favPendingIntent
        )

        fun buildNotification(sessionToken: MediaSessionCompat.Token): Notification {
            val builder = NotificationCompat.Builder(
                context,
                NOTIFICATION_CHANNEL_PLAYBACK_ID
            )
            val controller = MediaControllerCompat(context, sessionToken)

            (audioAdManager.adState as? AudioAdState.Playing)?.let { adState ->
                val mediaStyle = MediaStyle().setMediaSession(sessionToken)
                val adDuration = adState.ad?.duration?.times(1000L)?.toLong() ?: 0

                // https://issuetracker.google.com/issues/145770610
                // TODO remove at androidx:core-ktx 1.4.0+
                builder.extras

                // NotificationCompat is supposed to remove the needs for these checks, but lint
                // says otherwise
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    builder.setChronometerCountDown(true)
                }

                return builder.setContentIntent(controller.sessionActivity)
                    .setContentText(context.getString(R.string.audio_ad_support_artists) + "\n" + context.getString(R.string.audio_ad_upgrade_premium))
                    .setContentTitle(context.getString(R.string.audio_ad_notif_title))
                    .setOnlyAlertOnce(true)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setStyle(mediaStyle)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setShowWhen(true)
                    .setWhen(System.currentTimeMillis().plus(adDuration))
                    .setUsesChronometer(true)
                    .setOngoing(true)
                    .build()
            }

            // The metadata description doesn't copy over the full extras, so we use the queue
            val extras = controller.queue?.firstOrNull()?.description?.extras

            val description = controller.metadata.description
            val playbackState = controller.playbackState

            val genre = extras?.getString(MediaMetadataCompat.METADATA_KEY_GENRE)
            val isPodcast = AMGenre.Podcast.apiValue() == genre

            val actions = mutableListOf<NotificationCompat.Action>()

            actions.add(if (isPodcast) rewindAction else skipToPreviousAction)

            val playing = playbackState.isPlaying
            if (playing) {
                actions.add(pauseAction)
                builder.setWhen(System.currentTimeMillis() - playbackState.position)
            } else {
                if (playbackState.isPlayEnabled) {
                    actions.add(playAction)
                }
            }

            actions.add(if (isPodcast) fastForwardAction else skipToNextAction)

            val isLocal = description.mediaUri.isMediaStoreUri()

            if (!isLocal) {
                if (extras?.getBoolean(MediaMetadataCompat.METADATA_KEY_RATING, false) == true) {
                    actions.add(unFavoriteAction)
                } else {
                    actions.add(favoriteAction)
                }
            }

            actions.add(cancelAction)

            val compactActions = (0 until max(3, actions.size)).toList()

            val mediaStyle = MediaStyle()
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(*compactActions.toIntArray())

            if (!description.description.isNullOrEmpty()) {
                builder.setContentInfo(description.description)
            }

            actions.forEach { builder.addAction(it) }

            return builder.setContentIntent(controller.sessionActivity)
                .setContentText(description.subtitle)
                .setContentTitle(description.title)
                .setLargeIcon(description.iconBitmap)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.notification_icon)
                .setStyle(mediaStyle)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDeleteIntent(stopPendingIntent)
                .setShowWhen(playing)
                .setUsesChronometer(playing)
                .setOngoing(playing)
                .build()
        }
    }

    private inner class CastVolumeProvider(
        private val castSession: CastSession,
        currentVolume: Int = (castSession.volume * MAX_VOLUME).toInt()
    ) : VolumeProviderCompat(VOLUME_CONTROL_ABSOLUTE, MAX_VOLUME, currentVolume) {

        override fun onSetVolumeTo(volume: Int) {
            castSession.volume = volume / maxVolume.toDouble()
            currentVolume = volume
        }

        override fun onAdjustVolume(delta: Int) {
            val step = delta * (maxVolume / VOLUME_STEP)
            val volume = currentVolume.plus(step).coerceIn(0, maxVolume)
            currentVolume = volume
            castSession.volume = volume / maxVolume.toDouble()
        }
    }

    companion object {
        const val EXTRA_PLAY_WHEN_READY = "com.audiomack.Intent.EXTRA_PLAY_WHEN_READY"

        private const val MAX_VOLUME = 100
        private const val VOLUME_STEP = 20
    }
}

private const val TAG = "MusicService"
private const val WIFI_LOCK_TAG = "Audiomack::MusicService:WiFi"
private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"
private const val NOW_PLAYING_NOTIFICATION: Int = 0xb339
private const val ERROR_NOTIFICATION: Int = 1
