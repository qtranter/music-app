package com.audiomack.ui.premium

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.premium.EntitlementManager
import com.audiomack.data.premium.InAppPurchaseDataSource
import com.audiomack.data.premium.InAppPurchaseRepository
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.premium.PurchasesManager
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.appsflyer.AppsFlyerDataSource
import com.audiomack.data.tracking.appsflyer.AppsFlyerRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.EventLoginState
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.network.AnalyticsKeys
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import java.util.HashMap

internal class InAppPurchaseViewModel(
    private val inAppPurchaseDataSource: InAppPurchaseDataSource = InAppPurchaseRepository.getInstance(),
    private val entitlementManager: EntitlementManager = PurchasesManager.getInstance(),
    premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val appsFlyerDataSource: AppsFlyerDataSource = AppsFlyerRepository(),
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    enum class ActionAfterLogin {
        Purchase, Restore
    }

    lateinit var mode: InAppPurchaseMode

    private var actionAfterLogin: ActionAfterLogin? = null

    private var _subscriptionPriceString =
        MutableLiveData(inAppPurchaseDataSource.getSubscriptionPriceString())
    val subscriptionPriceString: LiveData<String> get() = _subscriptionPriceString

    val closeEvent = SingleLiveEvent<Unit>()
    val startLoginFlowEvent = SingleLiveEvent<Unit>()
    val showRestoreLoadingEvent = SingleLiveEvent<Unit>()
    val hideRestoreLoadingEvent = SingleLiveEvent<Unit>()
    val showRestoreFailureNoSubscriptionsEvent = SingleLiveEvent<Unit>()
    val showRestoreFailureErrorEvent = SingleLiveEvent<Unit>()
    val requestUpgradeEvent = SingleLiveEvent<Unit>()

    init {
        premiumDataSource.premiumObservable
            .subscribeOn(schedulersProvider.io)
            .take(1)
            .observeOn(schedulersProvider.main)
            .subscribe {
                if (it) closeEvent.call()
            }.also { compositeDisposable.add(it) }

        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
    }

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onUpgradeTapped(activity: Activity) {
        if (userDataSource.isLoggedIn()) {

            mixpanelDataSource.trackPremiumCheckoutStarted(mode)
            appsFlyerDataSource.trackPremiumStart()

            trackingDataSource.trackEventOnAllProviders(
                AnalyticsKeys.EVENT_PREMIUM_STARTED,
                object : HashMap<String, String>() {
                    init {
                        put(AnalyticsKeys.PARAM_SOURCE, mode.stringValue())
                        put(AnalyticsKeys.PARAM_ENV, AnalyticsKeys.PARAM_ENV_VALUE)
                    }
                })

            compositeDisposable.add(
                inAppPurchaseDataSource.purchase(activity)
                    .subscribeOn(schedulersProvider.main)
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        adsDataSource.toggle()
                        trackingDataSource.trackEventOnAllProviders(
                            AnalyticsKeys.EVENT_PREMIUM_SUCCEEDED,
                            object : HashMap<String, String>() {
                                init {
                                    put(AnalyticsKeys.PARAM_SOURCE, mode.stringValue())
                                    put(AnalyticsKeys.PARAM_ENV, AnalyticsKeys.PARAM_ENV_VALUE)
                                }
                            })
                        mixpanelDataSource.trackPurchasePremiumTrial(mode, inAppPurchaseDataSource)
                        appsFlyerDataSource.trackPremiumTrial()
                        closeEvent.call()
                    }, {
                        trackingDataSource.trackEventOnAllProviders(
                            AnalyticsKeys.EVENT_PREMIUM_FAILED,
                            object : HashMap<String, String>() {
                                init {
                                    put(AnalyticsKeys.PARAM_SOURCE, mode.stringValue())
                                    put(AnalyticsKeys.PARAM_ENV, AnalyticsKeys.PARAM_ENV_VALUE)
                                }
                            })
                        entitlementManager.reload()
                    })
            )
        } else {
            actionAfterLogin = ActionAfterLogin.Purchase
            startLoginFlowEvent.call()
        }
    }

    fun onRestoreTapped() {
        if (userDataSource.isLoggedIn()) {
            showRestoreLoadingEvent.call()
            compositeDisposable.add(
                entitlementManager.restore()
                    .subscribeOn(schedulersProvider.main)
                    .observeOn(schedulersProvider.main)
                    .subscribe({
                        hideRestoreLoadingEvent.call()
                        if (it) {
                            adsDataSource.toggle()
                            closeEvent.call()
                        } else {
                            showRestoreFailureNoSubscriptionsEvent.call()
                        }
                    }, {
                        hideRestoreLoadingEvent.call()
                        showRestoreFailureErrorEvent.call()
                    })
            )
        } else {
            actionAfterLogin = ActionAfterLogin.Restore
            startLoginFlowEvent.call()
        }
    }

    fun onCreate() {
        mixpanelDataSource.trackViewPremiumSubscription(mode)
        appsFlyerDataSource.trackPremiumView()
        trackingDataSource.trackEventOnAllProviders(
            AnalyticsKeys.EVENT_PREMIUM_VIEW,
            object : HashMap<String, String>() {
                init {
                    put(AnalyticsKeys.PARAM_SOURCE, mode.stringValue())
                    put(AnalyticsKeys.PARAM_ENV, AnalyticsKeys.PARAM_ENV_VALUE)
                }
            })

        inAppPurchaseDataSource.fetchOfferings()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                _subscriptionPriceString.postValue(inAppPurchaseDataSource.getSubscriptionPriceString())
                entitlementManager.reload()
            }, {})
            .also { compositeDisposable.add(it) }
    }

    fun onLoginStateChanged(state: EventLoginState) {
        if (state == EventLoginState.LOGGED_IN) {
            when (actionAfterLogin) {
                ActionAfterLogin.Purchase -> requestUpgradeEvent.call()
                ActionAfterLogin.Restore -> onRestoreTapped()
            }
        }
    }
}
