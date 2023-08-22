package com.audiomack.ui.browse.world.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.MainApplication
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.deeplink.Deeplink
import com.audiomack.data.reachability.Reachability
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelButtonWorld
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelPageWorld
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.tracking.mixpanel.MixpanelTabBrowse
import com.audiomack.data.world.WorldDataSource
import com.audiomack.data.world.WorldRepository
import com.audiomack.model.MixpanelSource
import com.audiomack.model.ShareMethod
import com.audiomack.model.WorldArticle
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo

class WorldArticleViewModel(
    private val repository: WorldDataSource = WorldRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val reachabilityDataSource: ReachabilityDataSource = Reachability.getInstance(),
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val jsMessageHandler: WorldArticleJSMessageHandler = WorldArticleJSMessageHandlerImpl()
) : BaseViewModel() {

    sealed class ViewState {
        object Loading : ViewState()
        data class Content(val html: String?, val adsVisible: Boolean) : ViewState()
        object ContentLoaded : ViewState()
        object Error : ViewState()
        object Offline : ViewState()
    }

    private var slug: String? = null
    private var article: WorldArticle? = null
    private val mixpanelSource = MixpanelSource(MixpanelTabBrowse, MixpanelPageWorld)

    val onBackPressedEvent = SingleLiveEvent<Void>()
    val sharePostEvent = SingleLiveEvent<String>()
    val openDeeplinkEvent = SingleLiveEvent<Deeplink>()

    private val _viewState = MutableLiveData<ViewState>()
    val viewState: LiveData<ViewState> get() = _viewState

    private var trackedArticle = false

    fun initWithSlug(slug: String) {
        this.slug = slug
        _viewState.postValue(ViewState.Loading)
        repository.getPost(slug)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                this.article = it
                _viewState.postValue(ViewState.Content(it.html, adsDataSource.adsVisible))
                if (!trackedArticle) {
                    trackedArticle = true
                    mixpanelDataSource.trackViewArticle(it)
                }
            }, {
                if (reachabilityDataSource.networkAvailable) _viewState.postValue(ViewState.Error)
                else _viewState.postValue(ViewState.Offline)
            })
            .addTo(compositeDisposable)
    }

    fun onBackClicked() {
        onBackPressedEvent.call()
    }

    fun onHtmlContentLoaded() {
        _viewState.postValue(ViewState.ContentLoaded)
    }

    fun onShareClicked() {
        val currentSlug = slug?.takeIf { it.isNotBlank() } ?: return
        val currentPost = article ?: return

        val url = "https://audiomack.com/world/post/$currentSlug"
        sharePostEvent.postValue(url)
        mixpanelDataSource.trackShareContent(
            method = ShareMethod.Standard,
            artist = null,
            music = null,
            comment = null,
            article = currentPost,
            source = MixpanelSource(MainApplication.currentTab, MixpanelPageWorld),
            button = MixpanelButtonWorld
        )
    }

    fun onJSMessageReceived(message: String) {
        jsMessageHandler.parseMessage(message, mixpanelSource)?.let { deeplink ->
            openDeeplinkEvent.postValue(deeplink)
        }
    }
}
