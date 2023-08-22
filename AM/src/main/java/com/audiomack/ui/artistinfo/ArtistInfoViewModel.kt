package com.audiomack.ui.artistinfo

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.tracking.mixpanel.MixpanelButtonArtistInfo
import com.audiomack.data.user.UserData
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.EventContentReported
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.ReportContentModel
import com.audiomack.model.ReportContentType
import com.audiomack.model.ReportType
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ArtistInfoViewModel(
    val imageLoader: ImageLoader = PicassoImageLoader,
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val eventBus: EventBus = EventBus.getDefault()
) : BaseViewModel() {

    val closeEvent = SingleLiveEvent<Void>()
    val imageEvent = SingleLiveEvent<Void>()
    var openUrlEvent = SingleLiveEvent<String>()
    val shareEvent = SingleLiveEvent<Void>()
    val notifyFollowToast = SingleLiveEvent<ToggleFollowResult.Notify>()
    val offlineAlert = SingleLiveEvent<Void>()
    val loggedOutAlert = SingleLiveEvent<LoginSignupSource>()
    val showReportReasonEvent = SingleLiveEvent<ReportContentModel>()
    val showReportAlertEvent = SingleLiveEvent<ReportType>()
    val promptNotificationPermissionEvent = SingleLiveEvent<PermissionRedirect>()

    private val _followStatus = MutableLiveData<Boolean>()
    val followStatus: LiveData<Boolean> get() = _followStatus

    private lateinit var artist: AMArtist

    val verified by lazy { artist.isVerified }
    val tastemaker by lazy { artist.isTastemaker }
    val authenticated by lazy { artist.isAuthenticated }
    val image by lazy { artist.largeImage }
    val name by lazy { artist.name }
    val slug by lazy { artist.urlSlugDisplay }
    val twitter by lazy { artist.twitter ?: "" }
    val facebook by lazy { artist.facebook ?: "" }
    val instagram by lazy { artist.instagram ?: "" }
    val youtube by lazy { artist.youtube ?: "" }
    val bio by lazy { artist.bio }
    val hometown by lazy { artist.hometown }
    val memberSince by lazy { artist.getCreated() }
    val website by lazy { artist.url }
    val genre by lazy { artist.genrePretty }
    val label by lazy { artist.label }
    val followingExtended by lazy { artist.followingExtended }
    val followersExtended by lazy { artist.followersExtended }
    val playsExtended by lazy { artist.playsExtended }

    val playsVisible by lazy { artist.playsCount > 0L }
    val twitterVisible by lazy { twitter.isNotEmpty() }
    val facebookVisible by lazy { facebook.isNotEmpty() }
    val instagramVisible by lazy { instagram.isNotEmpty() }
    val youtubeVisible by lazy { youtube.isNotEmpty() }
    val websiteVisible by lazy { !artist.url.isNullOrEmpty() }
    val genreVisible by lazy { artist.genrePretty.isNotEmpty() }
    val labelVisible by lazy { !artist.label.isNullOrEmpty() }
    val hometownVisible by lazy { !artist.hometown.isNullOrEmpty() }
    val memberSinceVisible by lazy { artist.getCreated().isNotEmpty() }
    val bioVisible by lazy { !artist.bio.isNullOrEmpty() }
    val followVisible by lazy { !AMArtist.isMyAccount(artist) }

    private var pendingActionAfterLogin: PendingActionAfterLogin? = null

    init {
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        eventBus.register(this)
    }

    @VisibleForTesting
    public override fun onCleared() {
        super.onCleared()
        eventBus.unregister(this)
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        when (state) {
            EventLoginState.LOGGED_IN -> pendingActionAfterLogin?.let {
                when (it) {
                    is PendingActionAfterLogin.Follow -> onFollowTapped(it.mixpanelSource)
                }
                pendingActionAfterLogin = null
            }
            else -> pendingActionAfterLogin = null
        }
    }

    fun initArtist(artist: AMArtist) {
        this.artist = artist
        _followStatus.postValue(UserData.isArtistFollowed(artist.artistId))
    }

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onImageTapped() {
        imageEvent.call()
    }

    fun onTwitterTapped() {
        val urlString = if (twitter.startsWith("http")) {
            twitter
        } else {
            "http://www.twitter.com/$twitter"
        }
        openUrlEvent.postValue(urlString)
    }

    fun onFacebookTapped() {
        openUrlEvent.postValue(facebook)
    }

    fun onInstagramTapped() {
        val urlString = if (Utils.isInstagramAppInstalled) {
            val components = instagram.split("/")
            val username = components[if (components.size > 1) (components.size - 1) else 0]
            "instagram://user?username=$username"
        } else {
            if (instagram.startsWith("http")) {
                instagram
            } else {
                "http://www.instagram.com/${artist.instagram}"
            }
        }
        openUrlEvent.postValue(urlString)
    }

    fun onYoutubeTapped() {
        openUrlEvent.postValue(youtube)
    }

    fun onShareTapped() {
        shareEvent.call()
    }

    fun onWebsiteTapped() {
        openUrlEvent.postValue(website)
    }

    fun onFollowTapped(mixpanelSource: MixpanelSource) {
        compositeDisposable.add(
            actionsDataSource.toggleFollow(null, artist, MixpanelButtonArtistInfo, mixpanelSource)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    when (it) {
                        is ToggleFollowResult.Finished -> _followStatus.postValue(it.followed)
                        is ToggleFollowResult.Notify -> notifyFollowToast.postValue(it)
                        is ToggleFollowResult.AskForPermission -> promptNotificationPermissionEvent.postValue(it.redirect)
                    }
                }, {
                    when (it) {
                        is ToggleFollowException.LoggedOut -> {
                            pendingActionAfterLogin = PendingActionAfterLogin.Follow(mixpanelSource)
                            loggedOutAlert.postValue(LoginSignupSource.AccountFollow)
                        }
                        is ToggleFollowException.Offline -> offlineAlert.call()
                    }
                })
        )
    }

    fun onReportTapped() {
        val contentId = artist.artistId ?: return
        val contentType = ReportContentType.Artist
        val reportType = ReportType.Report
        showReportReasonEvent.postValue(ReportContentModel(contentId, contentType, reportType, null))
    }

    fun onBlockTapped() {
        val contentId = artist.artistId ?: return
        val contentType = ReportContentType.Artist
        val reportType = ReportType.Block
        showReportReasonEvent.postValue(ReportContentModel(contentId, contentType, reportType, null))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventContentReported) {
        showReportAlertEvent.postValue(event.reportType)
    }

    sealed class PendingActionAfterLogin {
        data class Follow(val mixpanelSource: MixpanelSource) : PendingActionAfterLogin()
    }
}
