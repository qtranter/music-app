package com.audiomack

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.StrictMode
import androidx.startup.AppInitializer
import com.activeandroid.ActiveAndroid
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.cache.CachingLayerImpl
import com.audiomack.data.database.ArtistDAOImpl
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.featured.FeaturedSpotRepository
import com.audiomack.data.housekeeping.HousekeepingUseCaseImpl
import com.audiomack.data.logviewer.LogRepository
import com.audiomack.data.logviewer.LogTree
import com.audiomack.data.logviewer.LogType
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.sizes.SizesRepository
import com.audiomack.data.storage.StorageProvider
import com.audiomack.data.support.ZendeskRepository
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.tracking.mixpanel.MixpanelTabBrowse
import com.audiomack.model.AMResultItem
import com.audiomack.network.API
import com.audiomack.network.AnalyticsHelper
import com.audiomack.onesignal.OneSignalRepository
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.startup.OneSignalInitializer
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.AMCrashHandler
import com.audiomack.utils.ForegroundManager
import com.audiomack.utils.StethoUtils
import com.audiomack.utils.Utils
import com.comscore.Analytics
import com.comscore.PublisherConfiguration.Builder
import com.comscore.util.log.LogLevel
import com.google.android.play.core.missingsplits.MissingSplitsManagerFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.branch.referral.Branch
import io.embrace.android.embracesdk.Embrace
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.internal.functions.Functions
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber

class MainApplication : Application() {

    val housekeepingUseCase by lazy {
        HousekeepingUseCaseImpl(this)
    }

    @SuppressLint("CheckResult")
    override fun onCreate() {
        if (MissingSplitsManagerFactory.create(this).disableAppIfMissingRequiredSplits()) {
            return // App was installed from an incomplete bundle APK
        }

        super.onCreate()

        context = this

        if (Utils.getCurrentProcessPackageName(applicationContext) == "com.audiomack") {

            AppInitializer.getInstance(this)
                .initializeComponent(OneSignalInitializer::class.java)

            StethoUtils.initStetho(this)

            val asyncMainThreadScheduler = AndroidSchedulers.from(Looper.getMainLooper(), true)
            RxAndroidPlugins.setInitMainThreadSchedulerHandler { asyncMainThreadScheduler }

            RxJavaPlugins.setErrorHandler(Functions.emptyConsumer())

            // OkHttp must be initialized before Embrace
            API.getInstance()

            Embrace.getInstance().start(this)
            Embrace.getInstance().startEvent("Initialization")

            FirebaseApp.initializeApp(this)

            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.AUDIOMACK_DEBUG)

            if (BuildConfig.AUDIOMACK_DEBUG) {
                StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .detectNetwork()
                        .penaltyLog()
                        .build()
                )
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()
                        .detectLeakedClosableObjects()
                        .penaltyLog()
                        .build()
                )
                Timber.plant(Timber.DebugTree())
            }

            Thread.getDefaultUncaughtExceptionHandler()?.let { defaultHandler ->
                Thread.setDefaultUncaughtExceptionHandler(AMCrashHandler(defaultHandler))
            }

            ActiveAndroid.initialize(this)

            ForegroundManager.init(this)

            ArtistDAOImpl().find()
                .subscribeOn(AMSchedulersProvider().io)
                .observeOn(AMSchedulersProvider().main)
                .map { it.isAdmin || BuildConfig.AUDIOMACK_DEBUG }
                .onErrorReturnItem(BuildConfig.AUDIOMACK_DEBUG)
                .subscribe { logsEnabled ->
                    if (logsEnabled) {
                        Timber.plant(
                            LogTree(LogType.MIXPANEL, LogRepository),
                            LogTree(LogType.ADS, LogRepository),
                            LogTree(LogType.PLAYBACK, LogRepository),
                            LogTree(LogType.QUEUE, LogRepository),
                            LogTree(LogType.PLAYER_VM, LogRepository),
                            LogTree(LogType.PLAYER_REPO, LogRepository)
                        )
                    }
                }

            SizesRepository.initialize(this)

            Branch.getAutoInstance(this)

            initComScore()

            AnalyticsHelper.getInstance().init(this)

            PremiumRepository.init(this)

            ZendeskRepository().registerForPush()

            deleteNotifications()

            StorageProvider.init(this)
            CachingLayerImpl.init(this)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.let {

                    val playbackChannel = NotificationChannel(
                        NOTIFICATION_CHANNEL_PLAYBACK_ID,
                        NOTIFICATION_CHANNEL_PLAYBACK_NAME,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = NOTIFICATION_CHANNEL_PLAYBACK_NAME
                        setShowBadge(false)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                    it.createNotificationChannel(playbackChannel)

                    val remoteChannel = NotificationChannel(
                        NOTIFICATION_CHANNEL_REMOTE_ID,
                        NOTIFICATION_CHANNEL_REMOTE_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = NOTIFICATION_CHANNEL_REMOTE_NAME
                        setShowBadge(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                    it.createNotificationChannel(remoteChannel)

                    val downloadChannel = NotificationChannel(
                        NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                        NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = NOTIFICATION_CHANNEL_DOWNLOAD_NAME
                        setShowBadge(false)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                    it.createNotificationChannel(downloadChannel)

                    NotificationChannel(
                        NOTIFICATION_CHANNEL_GENERAL_ID,
                        NOTIFICATION_CHANNEL_GENERAL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = NOTIFICATION_CHANNEL_GENERAL_NAME
                        setShowBadge(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }.also { channel ->
                        it.createNotificationChannel(channel)
                    }
                }
            }

            if (!DeviceRepository.runningEspressoTest) {
                AdProvidersHelper.toggle()
            }

            OneSignalRepository.getInstance().result
                .subscribeOn(AMSchedulersProvider().io)
                .observeOn(AMSchedulersProvider().main)
                .subscribe { result ->
                    MixpanelRepository().trackTransactionalNotificationOpened(result.info)
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        data = result.deeplinkUri
                    }
                    startActivity(intent)
                }

            Thread {
                FeaturedSpotRepository.getInstance().pick()
            }.start()

            Embrace.getInstance().endEvent("Initialization")
        }
    }

    private fun initComScore() {
        val publisher = Builder()
            .publisherId(BuildConfig.AM_COMSCORE_ID)
            .build()
        Analytics.getConfiguration().addClient(publisher)
        Analytics.start(applicationContext)

        if (BuildConfig.AUDIOMACK_DEBUG) {
            Analytics.setLogLevel(LogLevel.VERBOSE)
            Analytics.getConfiguration().disable()
        }
    }

    fun deleteNotifications() {
        try {
            (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    companion object {
        @JvmStatic
        var context: Application? = null

        @JvmStatic
        var playlist: AMResultItem? = null

        @JvmStatic
        var isFreshInstallTooltipShown: Boolean = false

        @JvmStatic
        var currentTab = MixpanelTabBrowse
    }
}
