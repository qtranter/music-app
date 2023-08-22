package com.audiomack.data.support

import com.audiomack.BuildConfig
import com.audiomack.MainApplication
import com.audiomack.data.device.DeviceDataSource
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.premium.PurchasesManager
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.Credentials
import com.audiomack.model.EventShowUnreadTicketsAlert
import com.audiomack.network.AnalyticsHelper
import com.google.firebase.messaging.FirebaseMessaging
import com.zendesk.logger.Logger
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import io.reactivex.Observable
import java.util.Locale
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import zendesk.configurations.Configuration
import zendesk.core.AnonymousIdentity
import zendesk.core.Zendesk
import zendesk.support.CreateRequest
import zendesk.support.CustomField
import zendesk.support.Request
import zendesk.support.RequestProvider
import zendesk.support.RequestUpdates
import zendesk.support.Support
import zendesk.support.guide.ArticleConfiguration
import zendesk.support.guide.HelpCenterConfiguration
import zendesk.support.request.RequestActivity

class ZendeskRepository(
    private val supportStatsDataSource: SupportStatsDataSource = ZendeskSupportStatsRepository(),
    deviceDataSource: DeviceDataSource = DeviceRepository,
    premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    purchasesManager: PurchasesManager = PurchasesManager.getInstance(),
    analytics: AnalyticsHelper = AnalyticsHelper.getInstance()
) : ZendeskDataSource {

    init {
        Zendesk.INSTANCE.init(
            MainApplication.context!!,
            BuildConfig.AM_ZENDESK_URL,
            BuildConfig.AM_ZENDESK_APP_ID,
            BuildConfig.AM_ZENDESK_CLIENT_ID
        )
        Logger.setLoggable(BuildConfig.AUDIOMACK_DEBUG)
        Support.INSTANCE.init(Zendesk.INSTANCE)

        analytics.setIdentityListener { trackIdentity() }
    }

    override val cantLoginArticleId: Long = 360039143492L

    override fun getUnreadTicketsCount(): Observable<ZendeskUnreadTicketsData> {
        return Observable.fromPublisher { subscriber ->

            trackIdentity()

            if (!userDataSource.isLoggedIn()) {
                subscriber.onNext(ZendeskUnreadTicketsData(0))
            } else {
                // Immediately return cached value
                val oldCount = supportStatsDataSource.unreadTicketReplies()
                subscriber.onNext(ZendeskUnreadTicketsData(oldCount))

                Support.INSTANCE.provider()?.requestProvider()?.getUpdatesForDevice(object : ZendeskCallback<RequestUpdates>() {
                    override fun onSuccess(requestUpdates: RequestUpdates) {

                        val newCount =
                            if (requestUpdates.hasUpdatedRequests()) {
                                requestUpdates.requestUpdates.map { it.value }.sum()
                            } else 0

                        // Save value
                        supportStatsDataSource.setUnreadTicketReplies(newCount)

                        val needToShowAlert = newCount > oldCount
                        if (needToShowAlert) {
                            EventBus.getDefault().post(EventShowUnreadTicketsAlert())
                        }

                        subscriber.onNext(ZendeskUnreadTicketsData(newCount, needToShowAlert))
                    }

                    override fun onError(errorResponse: ErrorResponse) {
                        try {
                            subscriber.onError(Exception())
                        } catch (e: Exception) {
                            Timber.w(e)
                        }
                    }
                })
            }
        }
    }

    override fun getUIConfigs(): List<Configuration> {
        return listOf(
                RequestActivity.builder()
                    .withCustomFields(requestCustomFields)
                    .config(),
                ArticleConfiguration.Builder()
                        .withContactUsButtonVisible(Credentials.isLogged(MainApplication.context))
                        .config(),
                HelpCenterConfiguration.Builder()
                        .withShowConversationsMenuButton(Credentials.isLogged(MainApplication.context))
                        .withContactUsButtonVisible(Credentials.isLogged(MainApplication.context))
                        .config()
        )
    }

    override val cachedUnreadTicketsCount: Int
        get() = supportStatsDataSource.unreadTicketReplies()

    override fun sendSupportTicket(whatText: String, howText: String, whenText: String, emailText: String, notesText: String): Observable<Boolean> {
        return Observable.fromPublisher { subscriber ->

            val tags: List<String> = listOf("cant-login")

            Zendesk.INSTANCE.setIdentity(AnonymousIdentity.Builder()
                .withEmailIdentifier(emailText)
                .build())

            val createRequest = CreateRequest()

            createRequest.subject = "Support Request: Can't Login"
            createRequest.description = "Login details\n\n> Were you trying to login or singup?\n$whatText\n\n> How were you trying to log in / sign up?\n$howText\n\n> When did you last log in?\n$whenText\n\n> Can you tell us anything else that happened when you tried to log in or sign up?\n$notesText"
            createRequest.customFields = requestCustomFields
            createRequest.tags = tags

            Support.INSTANCE.provider()?.let {
                val requestProvider: RequestProvider = it.requestProvider()

                requestProvider.createRequest(createRequest, object : ZendeskCallback<Request>() {

                    override fun onError(errorResponse: ErrorResponse) {
                        subscriber.onError(Exception())
                    }

                    override fun onSuccess(request: Request?) {
                        subscriber.onNext(true)
                    }
                })
            }
        }
    }

    override fun trackIdentity() {
        val identity = if (!userDataSource.isLoggedIn()) {
            AnonymousIdentity.Builder()
                .build()
        } else {
            AnonymousIdentity.Builder()
                .withEmailIdentifier(userDataSource.getEmail())
                .withNameIdentifier(userDataSource.getUser()?.name)
                .build()
        }
        Zendesk.INSTANCE.setIdentity(identity)
    }

    override fun registerForPush(token: String?) {
        trackIdentity()

        if (token != null) {
            registerToken(token)
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener {
                it.takeIf { it.isSuccessful }?.result?.let { registerToken(it) }
            }
    }

    private fun registerToken(token: String) {
        Zendesk.INSTANCE.provider()
            ?.pushRegistrationProvider()
            ?.registerWithDeviceIdentifier(token, null)
    }

    override fun logout() {
        trackIdentity()
        unregisterForPush()
    }

    private fun unregisterForPush() {
        Zendesk.INSTANCE.provider()?.pushRegistrationProvider()?.unregisterDevice(null)
    }

    private val requestCustomFields = listOf(
        CustomField(360012705332L, deviceDataSource.getAppVersionFull()),
        CustomField(360012745931L, deviceDataSource.getOsVersion()),
        CustomField(360012705352L, Locale.getDefault().displayCountry),
        CustomField(360012745951L, Credentials.load(MainApplication.context)?.userUrlSlug
            ?: ""),
        CustomField(360012705372L, deviceDataSource.getModel()),
        CustomField(360012730072L, userDataSource.getUser()?.artistId ?: ""),
        CustomField(360012770431L, AMResultItem.getAllItemsIds().size.toString()),
        CustomField(360013486752L, deviceDataSource.hasStoragePermissionGranted().toString()),
        CustomField(360013488352L, deviceDataSource.hasDoNotKeepActivitiesFlag().toString()),
        CustomField(360015260111L, if (deviceDataSource.isMobileDataEnabled()) "Mobile" else "WiFi"),
        CustomField(360015261151L, deviceDataSource.getCarrierName() ?: ""),
        CustomField(360019202351L, purchasesManager.revenueCatUserId),
        CustomField(360019389691L, Credentials.load(MainApplication.context)?.userId
            ?: ""),
        CustomField(360026821372L, premiumDataSource.isPremium.toString()),
        CustomField(360028817872L, deviceDataSource.getManufacturer()),
        CustomField(360028883911L, "Android")
    )
}
