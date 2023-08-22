package com.audiomack.ui.settings

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.device.DeviceDataSource
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.premium.InAppPurchaseDataSource
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumSettingsDataSource
import com.audiomack.data.share.ShareManager
import com.audiomack.data.sleeptimer.SleepTimer
import com.audiomack.data.sleeptimer.SleepTimerEvent
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerSet
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.data.support.ZendeskUnreadTicketsData
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.ArtistWithBadge
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LogoutReason
import com.audiomack.model.SubscriptionStore
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.Date
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import zendesk.configurations.Configuration

class SettingsViewModelTest {

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var inAppPurchaseDataSource: InAppPurchaseDataSource

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Mock
    private lateinit var premiumSettingsDataSource: PremiumSettingsDataSource

    @Mock
    private lateinit var zendeskDataSource: ZendeskDataSource

    @Mock
    private lateinit var preferencesDataSource: PreferencesDataSource

    @Mock
    private lateinit var deviceDataSource: DeviceDataSource

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var shareManager: ShareManager

    @Mock
    private lateinit var sleepTimer: SleepTimer

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: SettingsViewModel

    private val sleepTimerEvent = PublishSubject.create<SleepTimerEvent>()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        `when`(deviceDataSource.getAppVersionName()).thenReturn("1.0.0")
        `when`(deviceDataSource.getAppVersionCode()).thenReturn("1")

        whenever(premiumDataSource.premiumObservable).thenReturn(mock())
        whenever(sleepTimer.sleepEvent).thenReturn(sleepTimerEvent)

        viewModel = SettingsViewModel(
            userDataSource,
            inAppPurchaseDataSource,
            premiumDataSource,
            premiumSettingsDataSource,
            zendeskDataSource,
            preferencesDataSource,
            deviceDataSource,
            mixpanelDataSource,
            shareManager,
            sleepTimer,
            schedulersProvider
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observer: Observer<Void> = mock()
        viewModel.close.observeForever(observer)
        viewModel.onCloseTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun upgrade() {
        val observer: Observer<InAppPurchaseMode> = mock()
        viewModel.upgrade.observeForever(observer)
        viewModel.onUpgradeTapped()
        verify(observer).onChanged(eq(InAppPurchaseMode.Settings))
    }

    @Test
    fun `cancel subscription, play store, valid url`() {
        val observer: Observer<String> = mock()
        viewModel.openExternalURLEvent.observeForever(observer)

        whenever(premiumDataSource.subscriptionStore).thenReturn(SubscriptionStore.PlayStore)
        viewModel.onCancelSubscriptionTapped()

        verify(observer).onChanged(SubscriptionStore.PlayStore.url)
        verify(mixpanelDataSource).trackCancelSubscription(inAppPurchaseDataSource)
    }

    @Test
    fun `cancel subscription, unknown or promotion, null url`() {
        val observer: Observer<String> = mock()
        viewModel.openExternalURLEvent.observeForever(observer)

        whenever(premiumDataSource.subscriptionStore).thenReturn(SubscriptionStore.None)
        viewModel.onCancelSubscriptionTapped()

        verifyZeroInteractions(observer)
        verify(mixpanelDataSource, times(0)).trackCancelSubscription(inAppPurchaseDataSource)
    }

    @Test
    fun viewProfile() {
        val observer: Observer<String?> = mock()
        viewModel.viewProfile.observeForever(observer)
        viewModel.onViewProfileTapped()
        verify(observer).onChanged(anyOrNull())
    }

    @Test
    fun notificationsClick() {
        val observer: Observer<Void> = mock()
        viewModel.notificationsEvent.observeForever(observer)
        viewModel.onNotificationsTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun editAccount() {
        val observer: Observer<Void> = mock()
        viewModel.editAccount.observeForever(observer)
        viewModel.onEditAccountTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun shareAccount() {
        `when`(userDataSource.getUser()).thenReturn(mock())
        val observer: Observer<AMArtist> = mock()
        viewModel.shareAccount.observeForever(observer)
        viewModel.onShareAccountTapped()
        verify(observer).onChanged(any())
    }

    @Test
    fun defaultGenre() {
        val observer: Observer<Void> = mock()
        viewModel.defaultGenre.observeForever(observer)
        viewModel.onDefaultGenreTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun rate() {
        val observer: Observer<Void> = mock()
        viewModel.rate.observeForever(observer)
        viewModel.onRateTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun share() {
        val observer: Observer<Void> = mock()
        viewModel.share.observeForever(observer)
        viewModel.onShareTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun permissions() {
        val observer: Observer<Void> = mock()
        viewModel.permissions.observeForever(observer)
        viewModel.onPermissionsTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun privacy() {
        val observer: Observer<Void> = mock()
        viewModel.privacy.observeForever(observer)
        viewModel.onPrivacyTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `tap on version 5 times`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        val context: Context = mock()
        viewModel.onVersionTapped(context)
        viewModel.onVersionTapped(context)
        viewModel.onVersionTapped(context)
        viewModel.onVersionTapped(context)
        viewModel.onVersionTapped(context)
        verify(shareManager).openSupport(context)
    }

    @Test
    fun `tap on version 5 times, too slow`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        val context: Context = mock()
        viewModel.versionTapsTimeout = 0L
        viewModel.onVersionTapped(context)
        viewModel.onVersionTapped(context)
        viewModel.onVersionTapped(context)
        viewModel.onVersionTapped(context)
        Thread.sleep(1L)
        viewModel.onVersionTapped(context)
        verifyZeroInteractions(shareManager)
    }

    @Test
    fun `tap on version less than 5 times`() {
        `when`(userDataSource.isLoggedIn()).thenReturn(true)
        val context: Context = mock()
        viewModel.onVersionTapped(context)
        viewModel.onVersionTapped(context)
        viewModel.onVersionTapped(context)
        viewModel.onVersionTapped(context)
        verifyZeroInteractions(shareManager)
    }

    @Test
    fun support() {
        val observer: Observer<Void> = mock()
        viewModel.support.observeForever(observer)
        viewModel.onSupportTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun environmentChange() {
        val observer: Observer<Boolean> = mock()
        viewModel.liveEnvironment.observeForever(observer)

        whenever(userDataSource.logout(LogoutReason.SwitchEnvironment)).thenReturn(Completable.complete())
        viewModel.onEnvironmentChanged(true)

        verify(observer).onChanged(true)
        verify(preferencesDataSource).liveEnvironment = true

        viewModel.onEnvironmentChanged(false)

        verify(observer).onChanged(false)
        verify(preferencesDataSource).liveEnvironment = false
        verify(userDataSource, times(2)).logout(eq(LogoutReason.SwitchEnvironment))
    }

    @Test
    fun logout() {
        val showLogoutAlertObserver: Observer<Void> = mock()
        val confirmLogoutObserver: Observer<Void> = mock()

        viewModel.showLogoutAlert.observeForever(showLogoutAlertObserver)
        viewModel.logout.observeForever(confirmLogoutObserver)

        whenever(userDataSource.logout(LogoutReason.Manual)).thenReturn(Completable.complete())

        viewModel.onLogoutTapped()

        verify(showLogoutAlertObserver).onChanged(null)

        viewModel.onLogoutConfirmed()
        verify(confirmLogoutObserver).onChanged(null)

        verify(userDataSource).logout(eq(LogoutReason.Manual))
    }

    @Test
    fun `show unread alert, success and need to show alert`() {
        `when`(zendeskDataSource.getUnreadTicketsCount()).thenReturn(Observable.just(ZendeskUnreadTicketsData(7, true)))
        val observerShowUnreadAlert: Observer<Void> = mock()
        val observerTicketCount: Observer<Int> = mock()
        viewModel.showUnreadAlert.observeForever(observerShowUnreadAlert)
        viewModel.unreadTicketsCount.observeForever(observerTicketCount)
        viewModel.reloadData()
        verify(observerShowUnreadAlert).onChanged(null)
        verify(observerTicketCount).onChanged(eq(7))
    }

    @Test
    fun `show unread alert, success and no need to show alert`() {
        `when`(zendeskDataSource.getUnreadTicketsCount()).thenReturn(Observable.just(ZendeskUnreadTicketsData(7, false)))
        val observerShowUnreadAlert: Observer<Void> = mock()
        val observerTicketCount: Observer<Int> = mock()
        viewModel.showUnreadAlert.observeForever(observerShowUnreadAlert)
        viewModel.unreadTicketsCount.observeForever(observerTicketCount)
        viewModel.reloadData()
        verifyZeroInteractions(observerShowUnreadAlert)
        verify(observerTicketCount).onChanged(eq(7))
    }

    @Test
    fun `show unread alert, failure`() {
        `when`(zendeskDataSource.getUnreadTicketsCount()).thenReturn(Observable.error(Exception("")))
        val observerShowUnreadAlert: Observer<Void> = mock()
        val observerTicketCount: Observer<Int> = mock()
        viewModel.showUnreadAlert.observeForever(observerShowUnreadAlert)
        viewModel.unreadTicketsCount.observeForever(observerTicketCount)
        viewModel.reloadData()
        verifyZeroInteractions(observerShowUnreadAlert)
        verify(observerTicketCount).onChanged(eq(0))
    }

    @Test
    fun logViewer() {
        val observer: Observer<Void> = mock()
        viewModel.logViewer.observeForever(observer)
        viewModel.onLogViewerTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun openSource() {
        val observer: Observer<Void> = mock()
        viewModel.openSource.observeForever(observer)
        viewModel.onOpenSourceTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `equalizer premium`() {
        `when`(premiumDataSource.isPremium).thenReturn(true)

        val observer: Observer<Void> = mock()
        viewModel.equalizer.observeForever(observer)
        viewModel.onEqualizerTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `equalizer not premium, then upgrade`() {
        `when`(premiumDataSource.isPremium).thenReturn(false)
        `when`(premiumDataSource.premiumObservable).thenReturn(Observable.just(false))

        val observerEqualizer: Observer<Void> = mock()
        val observerUpgrade: Observer<InAppPurchaseMode> = mock()
        viewModel.equalizer.observeForever(observerEqualizer)
        viewModel.upgrade.observeForever(observerUpgrade)
        viewModel.onEqualizerTapped()

        verify(observerUpgrade).onChanged(eq(InAppPurchaseMode.Equalizer))

        `when`(premiumDataSource.isPremium).thenReturn(true)
        viewModel.premiumObserver.onNext(true)

        verify(observerEqualizer).onChanged(null)
    }

    @Test
    fun `equalizer not premium, then not upgrade`() {
        `when`(premiumDataSource.isPremium).thenReturn(false)
        `when`(premiumDataSource.premiumObservable).thenReturn(Observable.just(false))

        val observerEqualizer: Observer<Void> = mock()
        val observerUpgrade: Observer<InAppPurchaseMode> = mock()
        viewModel.equalizer.observeForever(observerEqualizer)
        viewModel.upgrade.observeForever(observerUpgrade)
        viewModel.onEqualizerTapped()

        verify(observerUpgrade).onChanged(eq(InAppPurchaseMode.Equalizer))

        `when`(premiumDataSource.isPremium).thenReturn(false)
        viewModel.premiumObserver.onNext(false)

        verifyZeroInteractions(observerEqualizer)
    }

    @Test
    fun `on tickets tapped`() {
        val observer: Observer<List<Configuration>> = mock()
        viewModel.showTickets.observeForever(observer)
        viewModel.onTicketsTapped()
        verify(observer).onChanged(any())
    }

    @Test
    fun `on track ads changed`() {
        viewModel.onTrackAdsChanged(true)
        verify(preferencesDataSource).trackingAds = true
    }

    @Test
    fun `on enable ads changed`() {
        viewModel.onGrantPremiumChanged(true)
        verify(premiumSettingsDataSource).adminGrantPremium = true
    }

    @Test
    fun `reload data`() {
        `when`(zendeskDataSource.getUnreadTicketsCount()).thenReturn(Observable.just(ZendeskUnreadTicketsData(1, true)))

        val observerProfileHeaderVisible: Observer<Boolean> = mock()
        val observerPremiumVisible: Observer<Boolean> = mock()
        val observerCancelSubscriptionVisible: Observer<Boolean> = mock()
        val observerViewProfileVisible: Observer<Boolean> = mock()
        val observerShareProfileVisible: Observer<Boolean> = mock()
        val observerPermissionsVisible: Observer<Boolean> = mock()
        val observerTrackAdsVisible: Observer<Boolean> = mock()
        val observerTrackAdsChecked: Observer<Boolean> = mock()
        val observerEnableAdsVisible: Observer<Boolean> = mock()
        val observerEnableAdsChecked: Observer<Boolean> = mock()
        val observerSwitchEnvVisible: Observer<Boolean> = mock()
        val observerSwitchEnvChecked: Observer<Boolean> = mock()
        val observerLogViewerVisible: Observer<Boolean> = mock()
        val observerLogoutVisible: Observer<Boolean> = mock()
        val observerEqualizerVisible: Observer<Boolean> = mock()
        val observerVersionNameAndCode: Observer<SettingsViewModel.VersionNameAndCode> = mock()
        val observerChangePasswordVisible: Observer<Boolean> = mock()

        viewModel.profileHeaderVisible.observeForever(observerProfileHeaderVisible)
        viewModel.premiumVisible.observeForever(observerPremiumVisible)
        viewModel.cancelSubscriptionVisible.observeForever(observerCancelSubscriptionVisible)
        viewModel.viewProfileVisible.observeForever(observerViewProfileVisible)
        viewModel.shareProfileVisible.observeForever(observerShareProfileVisible)
        viewModel.permissionsVisible.observeForever(observerPermissionsVisible)
        viewModel.trackAdsVisibility.observeForever(observerTrackAdsVisible)
        viewModel.trackAdsChecked.observeForever(observerTrackAdsChecked)
        viewModel.grantPremiumVisibility.observeForever(observerEnableAdsVisible)
        viewModel.grantPremiumChecked.observeForever(observerEnableAdsChecked)
        viewModel.switchEnvVisibility.observeForever(observerSwitchEnvVisible)
        viewModel.switchEnvChecked.observeForever(observerSwitchEnvChecked)
        viewModel.logViewerVisible.observeForever(observerLogViewerVisible)
        viewModel.logoutVisible.observeForever(observerLogoutVisible)
        viewModel.equalizerVisible.observeForever(observerEqualizerVisible)
        viewModel.versionNameAndCode.observeForever(observerVersionNameAndCode)
        viewModel.changePasswordVisible.observeForever(observerChangePasswordVisible)

        viewModel.reloadData()

        verify(observerProfileHeaderVisible).onChanged(any())
        verify(observerPremiumVisible).onChanged(any())
        verify(observerCancelSubscriptionVisible).onChanged(any())
        verify(observerViewProfileVisible).onChanged(any())
        verify(observerShareProfileVisible).onChanged(any())
        verify(observerPermissionsVisible).onChanged(any())
        verify(observerTrackAdsVisible).onChanged(any())
        verify(observerTrackAdsChecked).onChanged(any())
        verify(observerEnableAdsVisible).onChanged(any())
        verify(observerEnableAdsChecked).onChanged(any())
        verify(observerSwitchEnvVisible).onChanged(any())
        verify(observerSwitchEnvChecked).onChanged(any())
        verify(observerLogViewerVisible).onChanged(any())
        verify(observerLogoutVisible).onChanged(any())
        verify(observerEqualizerVisible).onChanged(any())
        verify(observerVersionNameAndCode).onChanged(any())
        verify(observerChangePasswordVisible).onChanged(any())
    }

    @Test
    fun `reload data, user data, logged in`() {
        `when`(zendeskDataSource.getUnreadTicketsCount()).thenReturn(Observable.just(ZendeskUnreadTicketsData(1, true)))
        `when`(userDataSource.getUser()).thenReturn(mock())

        val observerArtistWithBadge: Observer<ArtistWithBadge> = mock()
        val observerAvatar: Observer<String> = mock()

        viewModel.artistWithBadge.observeForever(observerArtistWithBadge)
        viewModel.avatar.observeForever(observerAvatar)

        viewModel.reloadData()

        verify(observerArtistWithBadge).onChanged(any())
        verify(observerAvatar).onChanged(any())
    }

    @Test
    fun `reload data, user data, logged out`() {
        `when`(zendeskDataSource.getUnreadTicketsCount()).thenReturn(Observable.just(ZendeskUnreadTicketsData(1, true)))
        `when`(userDataSource.getUser()).thenReturn(null)

        val observerArtistWithBadge: Observer<ArtistWithBadge> = mock()
        val observerAvatar: Observer<String> = mock()

        viewModel.artistWithBadge.observeForever(observerArtistWithBadge)
        viewModel.avatar.observeForever(observerAvatar)

        viewModel.reloadData()

        verifyZeroInteractions(observerArtistWithBadge)
        verifyZeroInteractions(observerAvatar)
    }

    @Test
    fun `sleep timer premium`() {
        whenever(premiumDataSource.isPremium).thenReturn(true)

        val observer: Observer<Void> = mock()
        viewModel.launchSleepTimerEvent.observeForever(observer)
        viewModel.onSleepTimerTapped()
        verify(observer).onChanged(null)
    }

    @Test
    fun `sleep timer not premium, then upgrade`() {
        whenever(premiumDataSource.isPremium).thenReturn(false)
        whenever(premiumDataSource.premiumObservable).thenReturn(Observable.just(false))

        val observerSleepTimer: Observer<Void> = mock()
        val observerUpgrade: Observer<InAppPurchaseMode> = mock()
        viewModel.launchSleepTimerEvent.observeForever(observerSleepTimer)
        viewModel.upgrade.observeForever(observerUpgrade)
        viewModel.onSleepTimerTapped()

        verify(observerUpgrade).onChanged(eq(InAppPurchaseMode.SleepTimer))

        whenever(premiumDataSource.isPremium).thenReturn(true)
        viewModel.premiumObserver.onNext(true)

        verify(observerSleepTimer).onChanged(null)
    }

    @Test
    fun `sleep timer not premium, then not upgrade`() {
        whenever(premiumDataSource.isPremium).thenReturn(false)
        whenever(premiumDataSource.premiumObservable).thenReturn(Observable.just(false))

        val observerSleepTimer: Observer<Void> = mock()
        val observerUpgrade: Observer<InAppPurchaseMode> = mock()
        viewModel.equalizer.observeForever(observerSleepTimer)
        viewModel.upgrade.observeForever(observerUpgrade)
        viewModel.onSleepTimerTapped()

        verify(observerUpgrade).onChanged(eq(InAppPurchaseMode.SleepTimer))

        whenever(premiumDataSource.isPremium).thenReturn(false)
        viewModel.premiumObserver.onNext(false)

        verifyZeroInteractions(observerSleepTimer)
    }

    @Test
    fun `sleep timer event on sleep timer set`() {
        val observer: Observer<Date> = mock()
        viewModel.onSleepTimerSetEvent.observeForever(observer)

        val date = Date()
        sleepTimerEvent.onNext(TimerSet(date))

        verify(observer, times(1)).onChanged(date)
    }

    @Test
    fun `on change password tapped`() {
        val observer: Observer<Void> = mock()
        viewModel.openChangePasswordEvent.observeForever(observer)
        viewModel.onChangePasswordTapped()
        verify(observer).onChanged(null)
    }
}
