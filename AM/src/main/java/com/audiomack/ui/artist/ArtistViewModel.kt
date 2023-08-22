package com.audiomack.ui.artist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.tracking.mixpanel.MixpanelButtonProfile
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent

class ArtistViewModel(
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    private var artist: AMArtist? = null

    private val _followStatus = MutableLiveData<Boolean>()
    val followStatus: LiveData<Boolean> get() = _followStatus

    val notifyFollowToastEvent = SingleLiveEvent<ToggleFollowResult.Notify>()
    val offlineAlertEvent = SingleLiveEvent<Void>()
    val loggedOutAlertEvent = SingleLiveEvent<LoginSignupSource>()
    val promptNotificationPermissionEvent = SingleLiveEvent<PermissionRedirect>()

    private var pendingActionAfterLogin: PendingActionAfterLogin? = null

    init {
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
    }

    fun initWithArtist(artist: AMArtist) {
        this.artist = artist
        _followStatus.postValue(userDataSource.isArtistFollowed(artist.artistId))
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

    fun onFollowTapped(mixpanelSource: MixpanelSource) {
        compositeDisposable.add(
            actionsDataSource.toggleFollow(null, artist, MixpanelButtonProfile, mixpanelSource)
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
                            pendingActionAfterLogin = PendingActionAfterLogin.Follow(mixpanelSource)
                            loggedOutAlertEvent.postValue(LoginSignupSource.AccountFollow)
                        }
                        is ToggleFollowException.Offline -> offlineAlertEvent.call()
                    }
                })
        )
    }

    sealed class PendingActionAfterLogin {
        data class Follow(val mixpanelSource: MixpanelSource) : PendingActionAfterLogin()
    }
}
