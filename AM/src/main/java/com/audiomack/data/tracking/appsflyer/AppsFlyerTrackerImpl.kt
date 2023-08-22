package com.audiomack.data.tracking.appsflyer

import android.app.Application
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.ConversionDataListener
import com.appsflyer.adrevenue.AppsFlyerAdRevenue
import com.appsflyer.adrevenue.adnetworks.AppsFlyerAdRevenueWrapperType
import com.audiomack.BuildConfig
import com.audiomack.MainApplication
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.model.AdRevenueInfo
import com.audiomack.usecases.NotifyAdsEventsUseCase
import com.audiomack.usecases.NotifyAdsEventsUseCaseImpl
import org.json.JSONObject
import timber.log.Timber

class AppsFlyerTrackerImpl(
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val notifyAdsEventsUseCase: NotifyAdsEventsUseCase = NotifyAdsEventsUseCaseImpl()
) : AppsFlyerTracker {

    private val sdkEnabled = !BuildConfig.AUDIOMACK_DEBUG

    private var firstOpen: Boolean? = null

    private var lib: AppsFlyerLib = AppsFlyerLib.getInstance().init(BuildConfig.AM_APPS_FLYER_KEY, object : ConversionDataListener,
        AppsFlyerConversionListener {
        override fun onAppOpenAttribution(p0: MutableMap<String, String>?) {
        }

        override fun onConversionDataSuccess(p0: MutableMap<String, Any>?) {
        }

        override fun onConversionDataFail(p0: String?) {
        }

        override fun onAttributionFailure(p0: String?) {
        }

        override fun onConversionFailure(p0: String?) {
        }

        override fun onConversionDataLoaded(conversionData: MutableMap<String, Any>?) {
            conversionData?.let {
                mixpanelDataSource.trackAppsFlyerConversion(it, firstOpen == null)
                firstOpen = false
            }
        }
    }, MainApplication.context).also {
        it.startTracking(MainApplication.context)
        MainApplication.context?.let { application -> startRevenueTracker(application) }
    }

    private fun startRevenueTracker(application: Application) {
        AppsFlyerAdRevenue.initialize(
            AppsFlyerAdRevenue.Builder(application).addNetworks(AppsFlyerAdRevenueWrapperType.MOPUB).adEventListener { adEvent ->
                adEvent?.let { event ->
                    val impressionLevelData = event.adNetworkPayload["impressionLevelData"]
                    impressionLevelData?.let { data ->
                        if (data.toString().isNotEmpty()) {
                            val json = JSONObject(data.toString())
                            val info = AdRevenueInfo(json)
                            mixpanelDataSource.trackAdServed(info)
                            notifyAdsEventsUseCase.notify(
                                info.toString(),
                                "${info.adUnitName} shown: ${info.adGroupName}"
                            )
                        }
                    }
                }
            }.build()
        )
        AppsFlyerAdRevenue.moPubWrapper().recordImpressionData()
    }

    override fun trackEvent(name: String) {
        Timber.tag(TAG).d("Event: $name")
        if (sdkEnabled) {
            lib.trackEvent(MainApplication.context, name, emptyMap())
        }
    }

    override fun trackUserId(userId: String) {
        Timber.tag(TAG).d("User Id: $userId")
        if (sdkEnabled) {
            lib.setCustomerUserId(userId)
        }
    }

    companion object {
        private const val TAG = "AppsFlyerTrackerImpl"
        private var INSTANCE: AppsFlyerTrackerImpl? = null

        fun getInstance(
            mixpanelDataSource: MixpanelDataSource = MixpanelRepository()
        ): AppsFlyerTracker =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppsFlyerTrackerImpl(mixpanelDataSource).also { INSTANCE = it }
            }
    }
}
