package com.audiomack.ui.feed.suggested

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelButtonList
import com.audiomack.data.tracking.mixpanel.MixpanelPageFeedSuggestedFollows
import com.audiomack.data.tracking.mixpanel.MixpanelTabFeed
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.data.DataViewModel
import com.audiomack.usecases.FetchSuggestedAccountsUseCase
import com.audiomack.usecases.FetchSuggestedAccountsUseCaseImpl
import com.audiomack.utils.SingleLiveEvent
import timber.log.Timber

class SuggestedAccountsViewModel(
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val fetchSuggestedAccountsUseCase: FetchSuggestedAccountsUseCase = FetchSuggestedAccountsUseCaseImpl(),
    adsDataSource: AdsDataSource = AdProvidersHelper
) : BaseViewModel() {

    val promptNotificationPermissionEvent = SingleLiveEvent<PermissionRedirect>()
    val offlineAlertEvent = SingleLiveEvent<Unit>()
    val loggedOutAlertEvent = SingleLiveEvent<LoginSignupSource>()
    val notifyFollowToastEvent = SingleLiveEvent<ToggleFollowResult.Notify>()

    /** Notifies of a followed [AMArtist] which should be removed from the list **/
    val accountFollowedEvent = SingleLiveEvent<AMArtist>()
    val reloadEvent = SingleLiveEvent<Unit>()

    private val _suggestedAccounts = MutableLiveData<List<AMArtist>>()
    val suggestedAccounts: LiveData<List<AMArtist>> = _suggestedAccounts

    private val mixpanelSource = MixpanelSource(MixpanelTabFeed, MixpanelPageFeedSuggestedFollows)
    private var pendingActionAfterLogin: DataViewModel.PendingActionAfterLogin? = null
    private var currentPage = 0
    var hasMoreItems = true
        private set

    val adsVisible = adsDataSource.adsVisible

    init {
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        loadMore()
    }

    private fun onSuggestedAccountsNext(artists: List<AMArtist>) {
        currentPage++
        val unFollowedArtists = artists.filterNot { userDataSource.isArtistFollowed(it.artistId) }
        if (unFollowedArtists.isEmpty() && artists.isNotEmpty()) {
            loadMore()
        } else {
            hasMoreItems = unFollowedArtists.isNotEmpty()
            _suggestedAccounts.value = unFollowedArtists
        }
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        when (state) {
            EventLoginState.LOGGED_IN -> pendingActionAfterLogin?.let {
                if (it is DataViewModel.PendingActionAfterLogin.Follow) {
                    it.artist?.let { artist ->
                        onFollowTapped(artist)
                    }
                }
                reloadItems()
                pendingActionAfterLogin = null
            }
            else -> pendingActionAfterLogin = null
        }
    }

    private fun reloadItems() {
        currentPage = 0
        reloadEvent.call()
        loadMore()
    }

    fun onFollowTapped(artist: AMArtist) {
        actionsDataSource.toggleFollow(null, artist, MixpanelButtonList, mixpanelSource)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .distinct()
            .subscribe({
                when (it) {
                    is ToggleFollowResult.Finished -> if (it.followed) {
                        accountFollowedEvent.postValue(artist)
                    }
                    is ToggleFollowResult.Notify -> notifyFollowToastEvent.postValue(it)
                    is ToggleFollowResult.AskForPermission -> promptNotificationPermissionEvent.postValue(
                        it.redirect
                    )
                }
            }, {
                when (it) {
                    is ToggleFollowException.LoggedOut -> {
                        pendingActionAfterLogin = DataViewModel.PendingActionAfterLogin.Follow(
                            null,
                            artist,
                            mixpanelSource,
                            MixpanelButtonList
                        )
                        loggedOutAlertEvent.postValue(LoginSignupSource.AccountFollow)
                    }
                    is ToggleFollowException.Offline -> offlineAlertEvent.call()
                }
            }).composite()
    }

    fun loadMore() {
        fetchSuggestedAccountsUseCase(currentPage)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ onSuggestedAccountsNext(it) }, { Timber.tag(TAG).e(it) })
            .composite()
    }

    companion object {
        const val TAG = "SuggestedAccountsVM"
    }
}
