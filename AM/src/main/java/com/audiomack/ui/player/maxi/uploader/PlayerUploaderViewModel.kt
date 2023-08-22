package com.audiomack.ui.player.maxi.uploader

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.player.PlayerRepository
import com.audiomack.data.tracking.mixpanel.MixpanelButtonNowPlaying
import com.audiomack.data.user.UserData
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.ArtistWithBadge
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.common.Resource
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class PlayerUploaderViewModel(
    playerDataSource: PlayerDataSource = PlayerRepository.getInstance(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    private val songObserver = object : Observer<Resource<AMResultItem>> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {}

        override fun onNext(item: Resource<AMResultItem>) {
            item.data?.let { onSongChanged(it) }
        }
    }

    private val _name = MutableLiveData<ArtistWithBadge>()
    val name: LiveData<ArtistWithBadge> get() = _name

    private val _followers = MutableLiveData<String>()
    val followers: LiveData<String> get() = _followers

    private val _avatar = MutableLiveData<String>()
    val avatar: LiveData<String?> get() = _avatar

    private val _followStatus = MutableLiveData<Boolean>()
    val followStatus: LiveData<Boolean> get() = _followStatus

    private val _followVisible = MutableLiveData<Boolean>()
    val followVisible: LiveData<Boolean> get() = _followVisible

    private val _tagsWithGenre = MutableLiveData<List<String>>()
    val tagsWithGenre: LiveData<List<String>> get() = _tagsWithGenre

    val notifyFollowToast = SingleLiveEvent<ToggleFollowResult.Notify>()
    val offlineAlert = SingleLiveEvent<Void>()
    val loggedOutAlert = SingleLiveEvent<LoginSignupSource>()
    val openInternalUrlEvent = SingleLiveEvent<String>()
    val promptNotificationPermissionEvent = SingleLiveEvent<PermissionRedirect>()
    val genreEvent = SingleLiveEvent<String>()
    val tagEvent = SingleLiveEvent<String>()

    var currentSong: AMResultItem? = null

    private var pendingActionAfterLogin: PendingActionAfterLogin? = null

    init {
        playerDataSource.subscribeToSong(songObserver)
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
    }

    private fun onSongChanged(song: AMResultItem) {
        _name.postValue(ArtistWithBadge(
            song.uploaderName ?: "",
            song.isUploaderVerified,
            song.isUploaderTastemaker,
            song.isUploaderAuthenticated
        ))
        _followers.postValue(if (song.hasUploaderFollowers()) song.uploaderFollowersExtended else "")
        _avatar.postValue(song.uploaderTinyImage)
        _followStatus.postValue(UserData.isArtistFollowed(song.uploaderId))
        _followVisible.postValue(userDataSource.getUserSlug() != song.uploaderSlug)
        currentSong = song

        val tagsList = song.tags.toList().filterNot { it == song.genre }
            .toMutableList()
        song.genre?.let {
            tagsList.add(0, it)
        }
        _tagsWithGenre.postValue(tagsList)
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
            actionsDataSource.toggleFollow(currentSong, null, MixpanelButtonNowPlaying, mixpanelSource)
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

    fun onUploaderTapped() {
        currentSong?.let { song ->
            openInternalUrlEvent.postValue("audiomack://artist/${song.uploaderSlug ?: ""}")
        }
    }

    fun onTagClicked(tag: String) {
        if (tag == currentSong?.genre) {
            genreEvent.postValue(tag.trim())
        } else {
            tagEvent.postValue("tag:${tag.trim()}")
        }
    }

    sealed class PendingActionAfterLogin {
        data class Follow(val mixpanelSource: MixpanelSource) : PendingActionAfterLogin()
    }
}
