package com.audiomack.ui.splash

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.audiomack.PRIVACY_POLICY_URL
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.deeplink.DeeplinkDataSource
import com.audiomack.data.deeplink.DeeplinkRepository
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.reachability.Reachability
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.socialauth.SocialAuthManager
import com.audiomack.data.socialauth.SocialAuthManagerImpl
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.PermissionType
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.Observable
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

class SplashViewModel(
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val preferencesDataSource: PreferencesDataSource = PreferencesRepository(),
    private val reachabilityDataSource: ReachabilityDataSource = Reachability.getInstance(),
    private val socialAuthManager: SocialAuthManager = SocialAuthManagerImpl(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val remoteVariablesProvider: RemoteVariablesProvider = RemoteVariablesProviderImpl(),
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    private val deeplinkDataSource: DeeplinkDataSource = DeeplinkRepository.getInstance(),
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    val reqCodePermissions = 900
    val fakeWait = 1500L

    private var invalidateTimer: Boolean = false
    private var timer: Timer? = null

    val showLoadingUIEvent = SingleLiveEvent<Void>()
    val goToDownloadsEvent = SingleLiveEvent<Void>()
    val grantPermissionsEvent = SingleLiveEvent<Void>()
    val deleteNotificationEvent = SingleLiveEvent<Void>()
    val runAutologinEvent = SingleLiveEvent<Void>()
    val showPermissionsViewEvent = SingleLiveEvent<Void>()
    val goHomeEvent = SingleLiveEvent<Void>()
    val showRetryLoginEvent = SingleLiveEvent<Void>()
    val openURLEvent = SingleLiveEvent<String>()

    fun onTryAgainTapped() {
        showLoadingUIEvent.call()
        userDataSource.credentials?.let {
            runAutologinEvent.call()
        }
    }

    fun onGoToDownloadsTapped() {
        goToDownloadsEvent.call()
    }

    fun onGrantPermissionsTapped(activity: Activity) {
        preferencesDataSource.setPermissionsShown(activity, "yes")
        grantPermissionsEvent.call()
    }

    fun onRequestPermissionsResult(activity: Activity, requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        mixpanelDataSource.trackEnablePermissions(activity, permissions, grantResults)
        if (requestCode == reqCodePermissions) {
            goAhead(activity)
        }
    }

    fun onCreate(activity: Activity, delay: Long) {

        deleteNotificationEvent.call()

        if (adsDataSource.isFreshInstall()) {

            trackingDataSource.trackFirstSession()

            // Init remote vars and wait for completion, then go to homepage
            remoteVariablesProvider.initialise()
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    goHomeEvent.call()
                }, {
                    goHomeEvent.call()
                })
                .composite()
        } else {

            // Init remote vars here as well but don't do anything on completion
            remoteVariablesProvider.initialise()
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({}, {})
                .composite()

            userDataSource.credentials?.let {
                runAutologinEvent.call()
            } ?: run {
                timer = Timer().also {
                    it.schedule(timerTask {
                        activity.runOnUiThread {
                            if (!invalidateTimer) {
                                goAhead(activity)
                                invalidateTimer = true
                            }
                        }
                    }, delay)
                }
            }
        }

        mixpanelDataSource.trackGeneralProperties(userDataSource.oneSignalId)
    }

    fun onRequestedLocationPermission() {
        mixpanelDataSource.trackPromptPermissions(PermissionType.Location)
    }

    fun onBranchDeeplinkDetected(activity: Activity, branchDeeplink: String?) {
        deeplinkDataSource.updateBranchDeeplink(branchDeeplink)
        if (!invalidateTimer) {
            invalidateTimer = true
            goAhead(activity)
        }
    }

    fun onPermissionsAlreadyGranted() {
        goHomeEvent.call()
    }

    fun autologin(activity: Activity) {
        if (!reachabilityDataSource.networkAvailable) {
            showRetryLoginEvent.call()
            return
        }
        if (userDataSource.credentials?.isLoggedViaFacebook == true) {
            socialAuthManager.refreshFacebookToken()
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    if (it) {
                        goAhead(activity)
                    } else {
                        goAheadWithDelay(activity)
                    }
                }, {
                    goAhead(activity)
                })
                .composite()
        } else if (userDataSource.credentials?.isLoggedViaGoogle == true) {
            socialAuthManager.refreshGoogleToken()
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    if (it) {
                        goAhead(activity)
                    } else {
                        goAheadWithDelay(activity)
                    }
                }, {
                    goAhead(activity)
                })
                .composite()
        } else {
            goAheadWithDelay(activity)
        }
    }

    private fun goAhead(activity: Activity) {
        val hasLocation = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        if (!hasLocation && preferencesDataSource.needToShowPermissions(activity)) {
            showPermissionsViewEvent.call()
        } else {
            goHomeEvent.call()
        }
    }

    private fun goAheadWithDelay(activity: Activity) {
        Observable.timer(fakeWait, TimeUnit.MILLISECONDS)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                goAhead(activity)
            }, {})
            .composite()
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }

    fun onPrivacyPolicyTapped() {
        openURLEvent.postValue(PRIVACY_POLICY_URL)
    }
}
