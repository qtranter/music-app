package com.audiomack.ui.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.MainApplication
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.data.actions.ToggleFavoriteException
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.actions.ToggleHighlightException
import com.audiomack.data.actions.ToggleHighlightResult
import com.audiomack.data.actions.ToggleRepostException
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.tracking.mixpanel.MixpanelButtonKebabMenu
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.AMNotification
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventLoginState
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.model.SubscriptionNotification
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import timber.log.Timber

class DataViewModel(
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val remoteVariables: RemoteVariablesProvider = RemoteVariablesProviderImpl()
) : BaseViewModel() {

    private val premiumStateObserver: Observer<Boolean> = object : Observer<Boolean> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {}

        override fun onNext(t: Boolean) {
            premiumStateChangedEvent.call()
        }
    }

    private val _followStatus = MutableLiveData<Boolean>()
    val followStatus: LiveData<Boolean> get() = _followStatus

    val notifyFollowToastEvent = SingleLiveEvent<ToggleFollowResult.Notify>()
    val offlineAlertEvent = SingleLiveEvent<Void>()
    val loggedOutAlertEvent = SingleLiveEvent<LoginSignupSource>()
    val showHUDEvent = SingleLiveEvent<ProgressHUDMode>()
    val showPremiumEvent = SingleLiveEvent<InAppPurchaseMode>()
    val openURLEvent = SingleLiveEvent<String>()
    val removeHighlightAtPositionEvent = SingleLiveEvent<Int>()
    val showConfirmDownloadDeletionEvent = SingleLiveEvent<AMResultItem>()
    val showConfirmPlaylistDownloadDeletionEvent = SingleLiveEvent<AMResultItem>()
    val showFailedPlaylistDownloadEvent = SingleLiveEvent<Void>()
    val showConfirmPlaylistSyncEvent = SingleLiveEvent<Pair<AMResultItem, Int>>()
    val premiumStateChangedEvent = SingleLiveEvent<Void>()
    val showPremiumDownloadEvent = SingleLiveEvent<PremiumDownloadModel>()
    val showUnlockedToastEvent = SingleLiveEvent<String>()
    val promptNotificationPermissionEvent = SingleLiveEvent<PermissionRedirect>()

    private val _showFollowBtn = MutableLiveData<Boolean>()
    val showFollowBtn: LiveData<Boolean> get() = _showFollowBtn

    private var pendingActionAfterLogin: PendingActionAfterLogin? = null

    init {
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        premiumDataSource.premiumObservable.subscribe(premiumStateObserver)
        setShouldHideLogoutFollow()
    }

    fun setShouldHideLogoutFollow() {
        _showFollowBtn.postValue(remoteVariables.hideFollowOnSearchForLoggedOutUsers && !userDataSource.isLoggedIn())
    }
    // Tracking

    fun onBellNotificationClicked(type: AMNotification.NotificationType) {
        mixpanelDataSource.trackBellNotification(type.analyticsType)
    }

    // Upsell

    fun onUpsellClicked() {
        if (premiumDataSource.subscriptionNotification != SubscriptionNotification.None) {
            premiumDataSource.subscriptionStore.url?.let { url ->
                mixpanelDataSource.trackBillingIssue()
                openURLEvent.postValue(url)
            }
        } else {
            showPremiumEvent.postValue(InAppPurchaseMode.MyLibraryBar)
        }
    }

    // Follow

    fun onFollowTapped(music: AMResultItem?, artist: AMArtist?, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        compositeDisposable.add(
            actionsDataSource.toggleFollow(music, artist, mixpanelButton, mixpanelSource)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    when (it) {
                        is ToggleFollowResult.Finished -> _followStatus.postValue(it.followed)
                        is ToggleFollowResult.Notify -> notifyFollowToastEvent.postValue(it)
                        is ToggleFollowResult.AskForPermission -> promptNotificationPermissionEvent.postValue(it.redirect)
                    }
                }, {
                    when (it) {
                        is ToggleFollowException.LoggedOut -> {
                            pendingActionAfterLogin = PendingActionAfterLogin.Follow(music, artist, mixpanelSource, mixpanelButton)
                            loggedOutAlertEvent.postValue(LoginSignupSource.AccountFollow)
                        }
                        is ToggleFollowException.Offline -> offlineAlertEvent.call()
                    }
                })
        )
    }

    fun removeGeorestrictedItemFromFavorites(music: AMResultItem, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        // Unfavorite
        showHUDEvent.postValue(ProgressHUDMode.Loading)
        compositeDisposable.add(
            actionsDataSource.toggleFavorite(music, mixpanelButton, mixpanelSource)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    if (it is ToggleFavoriteResult.Notify) {
                        showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    }
                }, {
                    if (it is ToggleFavoriteException.Offline) {
                        showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                        offlineAlertEvent.call()
                    } else {
                        showHUDEvent.postValue(ProgressHUDMode.Failure(""))
                    }
                })
        )
    }

    fun removeGeorestrictedItemFromUploads(userSlug: String, music: AMResultItem, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        // Unhighlight (if needed) and unrepost
        showHUDEvent.postValue(ProgressHUDMode.Loading)
        compositeDisposable.add(
            musicDataSource.getHighlights(userSlug, true)
                .subscribeOn(schedulersProvider.io)
                .flatMap {
                    if (it.any { it.itemId == music.itemId }) {
                        musicDataSource.reorderHighlights(it.filter { it.itemId != music.itemId })
                    } else {
                        Observable.just(it)
                    }
                }
                .flatMap {
                    actionsDataSource.toggleRepost(music, mixpanelButton, mixpanelSource)
                }
                .observeOn(schedulersProvider.main)
                .subscribe({
                    if (it is ToggleRepostResult.Notify) {
                        showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    }
                }, {
                    if (it is ToggleRepostException.Offline) {
                        showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                        offlineAlertEvent.call()
                    } else {
                        showHUDEvent.postValue(ProgressHUDMode.Failure(""))
                    }
                })
        )
    }

    fun onHighlightRemoved(music: AMResultItem, highlightPosition: Int, mixpanelButton: String, mixpanelSource: MixpanelSource) {
        showHUDEvent.postValue(ProgressHUDMode.Loading)
        compositeDisposable.add(
            actionsDataSource.toggleHighlight(music, mixpanelButton, mixpanelSource)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ result ->
                    showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    when (result) {
                        ToggleHighlightResult.Removed -> {
                            removeHighlightAtPositionEvent.postValue(highlightPosition)
                        }
                        else -> {} // Nothing to do here since it's currently only used for removing highlights
                    }
                }, { throwable ->
                    showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    when (throwable) {
                        is ToggleHighlightException.Offline -> { offlineAlertEvent.call() }
                        is ToggleHighlightException.LoggedOut -> {
                            pendingActionAfterLogin = PendingActionAfterLogin.Highlight(music, highlightPosition, mixpanelSource, mixpanelButton)
                            loggedOutAlertEvent.postValue(LoginSignupSource.Highlight)
                        }
                        else -> { showHUDEvent.postValue(ProgressHUDMode.Failure("")) }
                    }
                })
        )
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        when (state) {
            EventLoginState.LOGGED_IN -> pendingActionAfterLogin?.let {
                when (it) {
                    is PendingActionAfterLogin.Follow -> onFollowTapped(it.music, it.artist, it.mixpanelSource, it.mixpanelButton)
                    is PendingActionAfterLogin.Highlight -> onHighlightRemoved(it.music, it.highlightPosition, it.mixpanelButton, it.mixpanelSource)
                    is PendingActionAfterLogin.Download -> download(it.music, it.mixpanelSource, it.mixpanelButton)
                }
                pendingActionAfterLogin = null
            }
            else -> pendingActionAfterLogin = null
        }

        _showFollowBtn.postValue(state == EventLoginState.LOGGED_IN && remoteVariables.hideFollowOnSearchForLoggedOutUsers)
    }

    // Favorite

    private fun onFavoriteTapped(music: AMResultItem, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        actionsDataSource.toggleFavorite(music, mixpanelButton, mixpanelSource)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({}, {})
            .also { compositeDisposable.add(it) }
    }

    // Download

    fun onDownloadTapped(music: AMResultItem, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        download(music, mixpanelSource, mixpanelButton)
    }

    fun onPlaylistSyncConfirmed(playlist: AMResultItem, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        download(playlist, mixpanelSource, mixpanelButton)
        if (userDataSource.isLoggedIn() && !playlist.isUploadedByMyself(MainApplication.context) && !userDataSource.isMusicFavorited(playlist)) {
            onFavoriteTapped(playlist, mixpanelSource, mixpanelButton)
        }
    }

    private fun download(music: AMResultItem, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        actionsDataSource.toggleDownload(music, MixpanelButtonKebabMenu, mixpanelSource, skipFrozenCheck = !mixpanelSource.isInMyDownloads)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                when (result) {
                    ToggleDownloadResult.ConfirmPlaylistDeletion -> showConfirmPlaylistDownloadDeletionEvent.postValue(music)
                    is ToggleDownloadResult.ConfirmMusicDeletion -> showConfirmDownloadDeletionEvent.postValue(music)
                    is ToggleDownloadResult.ConfirmPlaylistDownload -> showConfirmPlaylistSyncEvent.postValue(Pair(music, result.tracksCount))
                    ToggleDownloadResult.StartedBlockingAPICall -> showHUDEvent.postValue(ProgressHUDMode.Loading)
                    ToggleDownloadResult.EndedBlockingAPICall -> showHUDEvent.postValue(ProgressHUDMode.Dismiss)
                    is ToggleDownloadResult.ShowUnlockedToast -> showUnlockedToastEvent.postValue(result.musicName)
                }
            }, { throwable ->
                when (throwable) {
                    is ToggleDownloadException.LoggedOut -> {
                        pendingActionAfterLogin = PendingActionAfterLogin.Download(music, mixpanelSource, mixpanelButton)
                        loggedOutAlertEvent.postValue(throwable.source)
                    }
                    is ToggleDownloadException.Unsubscribed -> showPremiumEvent.postValue(throwable.mode)
                    ToggleDownloadException.FailedDownloadingPlaylist -> showFailedPlaylistDownloadEvent.call()
                    is ToggleDownloadException.ShowPremiumDownload -> showPremiumDownloadEvent.postValue(throwable.model)
                    else -> Timber.w(throwable)
                }
            })
            .also { compositeDisposable.add(it) }
    }

    // Entities

    sealed class PendingActionAfterLogin {
        data class Follow(val music: AMResultItem?, val artist: AMArtist?, val mixpanelSource: MixpanelSource, val mixpanelButton: String) : PendingActionAfterLogin()
        data class Highlight(val music: AMResultItem, val highlightPosition: Int, val mixpanelSource: MixpanelSource, val mixpanelButton: String) : PendingActionAfterLogin()
        data class Download(val music: AMResultItem, val mixpanelSource: MixpanelSource, val mixpanelButton: String) : PendingActionAfterLogin()
        data class Favorite(val music: AMResultItem, val mixpanelSource: MixpanelSource, val mixpanelButton: String) : PendingActionAfterLogin()
    }
}
