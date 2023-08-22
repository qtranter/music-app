package com.audiomack.ui.mylibrary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.data.support.ZendeskRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.home.NavigationActions
import com.audiomack.ui.home.NavigationManager
import io.reactivex.Observable

class MyLibraryViewModel(
    showBackButton: Boolean,
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    zendeskDataSource: ZendeskDataSource = ZendeskRepository(),
    private val navigation: NavigationActions = NavigationManager.getInstance(),
    schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    data class ViewState(
        val userName: String = "",
        val userVerified: Boolean = false,
        val userTastemaker: Boolean = false,
        val userAuthenticated: Boolean = false,
        val userImage: String = "",
        val notificationsCount: Int = 0,
        val ticketsBadgeVisible: Boolean = false,
        val backButtonVisible: Boolean = false
    )

    private var _viewState = MutableLiveData(ViewState(backButtonVisible = showBackButton))
    val viewState: LiveData<ViewState> get() = _viewState

    val artistSlug: String? get() = userDataSource.getUserSlug()

    init {
        Observable.mergeDelayError(userDataSource.getUserAsync(), userDataSource.refreshUserData())
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main, true)
            .subscribe({ artist ->
                _viewState.value = requireNotNull(_viewState.value).copy(
                    userName = artist.name ?: "",
                    userVerified = artist.isVerified,
                    userTastemaker = artist.isTastemaker,
                    userAuthenticated = artist.isAuthenticated,
                    userImage = artist.smallImage ?: "",
                    notificationsCount = artist.unseenNotificationsCount
                )
            }, {
                it.printStackTrace()
            })
            .composite()

        zendeskDataSource.getUnreadTicketsCount()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ data ->
                _viewState.value = requireNotNull(_viewState.value).copy(
                    ticketsBadgeVisible = data.unreadCount > 0
                )
            }, {})
            .composite()
    }

    fun onSearchClick() {
        navigation.launchMyLibrarySearchEvent()
    }

    fun onNotificationsClick() {
        navigation.launchNotificationsEvent()
    }

    fun onSettingsClick() {
        navigation.launchSettingsEvent()
    }

    fun onBackClick() {
        navigation.navigateBack()
    }
}

class MyLibraryViewModelFactory(
    private val showBackButton: Boolean
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MyLibraryViewModel(showBackButton) as T
    }
}
