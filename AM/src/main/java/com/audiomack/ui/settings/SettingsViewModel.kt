package com.audiomack.ui.settings

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.BuildConfig
import com.audiomack.data.bitmap.BitmapManagerImpl
import com.audiomack.data.device.DeviceDataSource
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.premium.InAppPurchaseDataSource
import com.audiomack.data.premium.InAppPurchaseRepository
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.premium.PremiumSettingsDataSource
import com.audiomack.data.premium.PremiumSettingsRepository
import com.audiomack.data.share.ShareManager
import com.audiomack.data.share.ShareManagerImpl
import com.audiomack.data.sleeptimer.SleepTimer
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerSet
import com.audiomack.data.sleeptimer.SleepTimerManager
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.data.support.ZendeskRepository
import com.audiomack.data.tracking.appsflyer.AppsFlyerRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.ArtistWithBadge
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LogoutReason
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SimpleObserver
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import java.util.Date
import kotlin.math.abs
import zendesk.configurations.Configuration

class SettingsViewModel(
    private val userRepository: UserDataSource = UserRepository.getInstance(),
    private val inAppPurchaseDataSource: InAppPurchaseDataSource = InAppPurchaseRepository.getInstance(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val premiumSettingsDataSource: PremiumSettingsDataSource = PremiumSettingsRepository.getInstance(),
    private val zendeskRepository: ZendeskDataSource = ZendeskRepository(),
    private val preferencesRepository: PreferencesDataSource = PreferencesRepository(),
    private val deviceRepository: DeviceDataSource = DeviceRepository,
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val shareManager: ShareManager = ShareManagerImpl(BitmapManagerImpl(), MixpanelRepository(), AppsFlyerRepository()),
    private val sleepTimer: SleepTimer = SleepTimerManager.getInstance(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    val unreadTicketsCount = MutableLiveData<Int>()
    val close = SingleLiveEvent<Void>()
    val upgrade = SingleLiveEvent<InAppPurchaseMode>()
    val openExternalURLEvent = SingleLiveEvent<String>()
    val viewProfile = SingleLiveEvent<String?>()
    val notificationsEvent = SingleLiveEvent<Void>()
    val editAccount = SingleLiveEvent<Void>()
    val shareAccount = SingleLiveEvent<AMArtist>()
    val defaultGenre = SingleLiveEvent<Void>()
    val launchSleepTimerEvent = SingleLiveEvent<Void>()
    val onSleepTimerSetEvent = SingleLiveEvent<Date>()
    val rate = SingleLiveEvent<Void>()
    val share = SingleLiveEvent<Void>()
    val permissions = SingleLiveEvent<Void>()
    val equalizer = SingleLiveEvent<Void>()
    val privacy = SingleLiveEvent<Void>()
    val support = SingleLiveEvent<Void>()
    val liveEnvironment = MutableLiveData<Boolean>()
    val logViewer = SingleLiveEvent<Void>()
    val openSource = SingleLiveEvent<Void>()
    val showLogoutAlert = SingleLiveEvent<Void>()
    val logout = SingleLiveEvent<Void>()
    val showUnreadAlert = SingleLiveEvent<Void>()
    val showTickets = SingleLiveEvent<List<Configuration>>()
    val openChangePasswordEvent = SingleLiveEvent<Void>()

    private val _artistWithBadge = MutableLiveData<ArtistWithBadge>()
    val artistWithBadge: LiveData<ArtistWithBadge> get() = _artistWithBadge

    private val _avatar = MutableLiveData<String>()
    val avatar: LiveData<String> get() = _avatar

    private val _profileHeaderVisible = MutableLiveData<Boolean>()
    val profileHeaderVisible: LiveData<Boolean> get() = _profileHeaderVisible

    private val _premiumVisible = MutableLiveData<Boolean>()
    val premiumVisible: LiveData<Boolean> get() = _premiumVisible

    private val _cancelSubscriptionVisible = MutableLiveData<Boolean>()
    val cancelSubscriptionVisible: LiveData<Boolean> get() = _cancelSubscriptionVisible

    private val _viewProfileVisible = MutableLiveData<Boolean>()
    val viewProfileVisible: LiveData<Boolean> get() = _viewProfileVisible

    private val _notificationsVisible = MutableLiveData<Boolean>()
    val notificationsVisible: LiveData<Boolean> get() = _notificationsVisible

    private val _shareProfileVisible = MutableLiveData<Boolean>()
    val shareProfileVisible: LiveData<Boolean> get() = _shareProfileVisible

    private val _permissionsVisible = MutableLiveData<Boolean>()
    val permissionsVisible: LiveData<Boolean> get() = _permissionsVisible

    private val _trackAdsVisibility = MutableLiveData<Boolean>()
    val trackAdsVisibility: LiveData<Boolean> get() = _trackAdsVisibility

    private val _trackAdsChecked = MutableLiveData<Boolean>()
    val trackAdsChecked: LiveData<Boolean> get() = _trackAdsChecked

    private val _overridePremiumVisibility = MutableLiveData<Boolean>()
    val overridePremiumVisibility: LiveData<Boolean> get() = _overridePremiumVisibility

    private val _overridePremiumChecked = MutableLiveData<Boolean>()
    val overridePremiumChecked: LiveData<Boolean> get() = _overridePremiumChecked

    private val _grantPremiumVisibility = MutableLiveData<Boolean>()
    val grantPremiumVisibility: LiveData<Boolean> get() = _grantPremiumVisibility

    private val _grantPremiumChecked = MutableLiveData<Boolean>()
    val grantPremiumChecked: LiveData<Boolean> get() = _grantPremiumChecked

    private val _switchEnvVisibility = MutableLiveData<Boolean>()
    val switchEnvVisibility: LiveData<Boolean> get() = _switchEnvVisibility

    private val _switchEnvChecked = MutableLiveData<Boolean>()
    val switchEnvChecked: LiveData<Boolean> get() = _switchEnvChecked

    private val _logViewerVisible = MutableLiveData<Boolean>()
    val logViewerVisible: LiveData<Boolean> get() = _logViewerVisible

    private val _logoutVisible = MutableLiveData<Boolean>()
    val logoutVisible: LiveData<Boolean> get() = _logoutVisible

    private val _equalizerVisible = MutableLiveData<Boolean>()
    val equalizerVisible: LiveData<Boolean> get() = _equalizerVisible

    private val _versionNameAndCode = MutableLiveData<VersionNameAndCode>()
    val versionNameAndCode: LiveData<VersionNameAndCode> get() = _versionNameAndCode

    private val _changePasswordVisible = MutableLiveData<Boolean>()
    val changePasswordVisible: LiveData<Boolean> get() = _changePasswordVisible

    private var versionTaps = 0
    private var versionTapTimestamp = 0L
    private val versionTapsNeeded = 5
    var versionTapsTimeout = 3000L // Not private just for unit tests

    private var pendingEqualizer = false
    private var pendingSleepTimer = false

    @VisibleForTesting
    val premiumObserver = object : SimpleObserver<Boolean>(compositeDisposable) {
        override fun onNext(premium: Boolean) {
            if (premium) {
                resumePendingActions()
            } else {
                clearPendingActions()
            }
        }
    }

    init {
        premiumDataSource.premiumObservable.subscribe(premiumObserver)
        observeSleepTimer()
    }

    fun onCloseTapped() {
        close.call()
    }

    fun reloadData() {

        val isLoggedIn: Boolean = userRepository.isLoggedIn()
        val isAdmin: Boolean = userRepository.isAdmin()
        val loggedUser: AMArtist? = userRepository.getUser()
        val isPremium: Boolean = premiumDataSource.isPremium

        loggedUser?.let {
            _artistWithBadge.postValue(ArtistWithBadge(it.name ?: "", it.isVerified, it.isTastemaker, it.isAuthenticated))
            _avatar.postValue(it.smallImage ?: "")
        }
        _profileHeaderVisible.postValue(isLoggedIn)
        _premiumVisible.postValue(!isPremium)
        _cancelSubscriptionVisible.postValue(isPremium)
        _viewProfileVisible.postValue(isLoggedIn)
        _shareProfileVisible.postValue(isLoggedIn)
        _permissionsVisible.postValue(deviceRepository.hasRuntimePermissions())
        _trackAdsVisibility.postValue(isAdmin)
        _trackAdsChecked.postValue(preferencesRepository.trackingAds)
        _overridePremiumVisibility.postValue(isAdmin)
        _overridePremiumChecked.postValue(premiumSettingsDataSource.adminOverride)
        _grantPremiumVisibility.postValue(premiumSettingsDataSource.adminOverride)
        _grantPremiumChecked.postValue(premiumSettingsDataSource.adminGrantPremium)
        _switchEnvVisibility.postValue(isAdmin && BuildConfig.AM_WS_URL_LIVE != BuildConfig.AM_WS_URL_DEV)
        _switchEnvChecked.postValue(!preferencesRepository.liveEnvironment)
        _logViewerVisible.postValue(isAdmin)
        _logoutVisible.postValue(isLoggedIn)
        _equalizerVisible.postValue(deviceRepository.hasEqualizer())
        _versionNameAndCode.postValue(VersionNameAndCode(deviceRepository.getAppVersionName(), deviceRepository.getAppVersionCode()))
        _changePasswordVisible.postValue(isLoggedIn)

        compositeDisposable.add(
            zendeskRepository.getUnreadTicketsCount()
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    unreadTicketsCount.postValue(it.unreadCount)
                    if (it.needToShowAlert == true) {
                        showUnreadAlert.call()
                    }
                }, {
                    unreadTicketsCount.postValue(0)
                })
        )
    }

    fun onUpgradeTapped() {
        upgrade.postValue(InAppPurchaseMode.Settings)
    }

    fun onCancelSubscriptionTapped() {
        premiumDataSource.subscriptionStore.url?.let { url ->
            openExternalURLEvent.postValue(url)
            mixpanelDataSource.trackCancelSubscription(inAppPurchaseDataSource)
        }
    }

    fun onViewProfileTapped() {
        viewProfile.postValue(userRepository.getUser()?.urlSlug)
    }

    fun onNotificationsTapped() {
        notificationsEvent.call()
    }

    fun onEditAccountTapped() {
        editAccount.call()
    }

    fun onShareAccountTapped() {
        userRepository.getUser()?.let {
            shareAccount.postValue(it)
        }
    }

    fun onSleepTimerTapped() {
        if (premiumDataSource.isPremium) {
            launchSleepTimerEvent.call()
        } else {
            pendingSleepTimer = true
            upgrade.postValue(InAppPurchaseMode.SleepTimer)
        }
    }

    fun onDefaultGenreTapped() {
        defaultGenre.call()
    }

    fun onRateTapped() {
        rate.call()
    }

    fun onShareTapped() {
        share.call()
    }

    fun onPermissionsTapped() {
        permissions.call()
    }

    fun onPrivacyTapped() {
        privacy.call()
    }

    fun onVersionTapped(context: Context) {
        if (versionTaps == 0 || abs(System.currentTimeMillis() - versionTapTimestamp) > versionTapsTimeout) {
            versionTaps = 0
            versionTapTimestamp = System.currentTimeMillis()
        }
        if (++versionTaps == versionTapsNeeded) {
            shareManager.openSupport(context)
        }
    }

    fun onSupportTapped() {
        support.call()
    }

    fun onTrackAdsChanged(tracking: Boolean) {
        preferencesRepository.trackingAds = tracking
    }

    fun onAdminOverrideChanged(enable: Boolean) {
        premiumSettingsDataSource.adminOverride = enable
        _grantPremiumVisibility.postValue(enable)
        _grantPremiumChecked.postValue(premiumSettingsDataSource.adminGrantPremium)
    }

    fun onGrantPremiumChanged(enable: Boolean) {
        premiumSettingsDataSource.adminGrantPremium = enable
    }

    fun onEnvironmentChanged(live: Boolean) {
        preferencesRepository.liveEnvironment = live
        userRepository.logout(LogoutReason.SwitchEnvironment)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe { liveEnvironment.postValue(live) }
            .addTo(compositeDisposable)
    }

    fun onLogViewerTapped() {
        logViewer.call()
    }

    fun onOpenSourceTapped() {
        openSource.call()
    }

    fun onLogoutTapped() {
        showLogoutAlert.call()
    }

    fun onLogoutConfirmed() {
        userRepository.logout(LogoutReason.Manual)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe { logout.call() }
            .addTo(compositeDisposable)
    }

    fun onTicketsTapped() {
        showTickets.postValue(zendeskRepository.getUIConfigs())
    }

    fun onEqualizerTapped() {
        if (premiumDataSource.isPremium) {
            equalizer.call()
        } else {
            pendingEqualizer = true
            upgrade.postValue(InAppPurchaseMode.Equalizer)
        }
    }

    fun onChangePasswordTapped() {
        openChangePasswordEvent.call()
    }

    private fun resumePendingActions() {
        when {
            pendingEqualizer -> onEqualizerTapped()
            pendingSleepTimer -> onSleepTimerTapped()
        }
    }

    private fun clearPendingActions() {
        pendingEqualizer = false
        pendingSleepTimer = false
    }

    private fun observeSleepTimer() {
        sleepTimer.sleepEvent
            .observeOn(schedulersProvider.main)
            .filter { it is TimerSet }
            .cast(TimerSet::class.java)
            .subscribe { onSleepTimerSetEvent.value = it.date }
            .addTo(compositeDisposable)
    }

    override fun onCleared() {
        super.onCleared()
        clearPendingActions()
    }

    data class VersionNameAndCode(
        val name: String,
        val code: String
    )
}
