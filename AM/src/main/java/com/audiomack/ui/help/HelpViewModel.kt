package com.audiomack.ui.help

import androidx.lifecycle.MutableLiveData
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.data.support.ZendeskRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import zendesk.configurations.Configuration

class HelpViewModel(
    private val zendeskRepository: ZendeskDataSource = ZendeskRepository(),
    private val userRepository: UserDataSource = UserRepository.getInstance(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    val unreadTicketsCount = MutableLiveData<Int>()
    val close = SingleLiveEvent<Void>()
    val showKnowledgeBase = SingleLiveEvent<Void>()
    val showTickets = SingleLiveEvent<Void>()
    val showLoginAlert = SingleLiveEvent<Void>()
    val showLogin = SingleLiveEvent<LoginSignupSource>()
    val showUnreadAlert = SingleLiveEvent<Void>()

    private var pendingTickets = false

    var zendeskUIConfigs: List<Configuration> = zendeskRepository.getUIConfigs()

    init {
        userRepository.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
    }

    override fun onCleared() {
        super.onCleared()
        clearPendingActions()
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        if (state == EventLoginState.LOGGED_IN) {
            resumePendingActions()
        } else {
            clearPendingActions()
        }
    }

    fun onUnreadTicketsCountRequested() {
        compositeDisposable.add(
                zendeskRepository.getUnreadTicketsCount()
                        .subscribeOn(schedulersProvider.io)
                        .observeOn(schedulersProvider.main)
                        .subscribe({
                            unreadTicketsCount.postValue(it.unreadCount)
                            if (it.needToShowAlert == true) {
                                showUnreadAlert.call()
                            }
                        }, {
                            unreadTicketsCount.postValue(0)
                        })
        )
    }

    fun onCloseTapped() {
        close.call()
    }

    fun onKnowledgeBaseTapped() {
        showKnowledgeBase.call()
    }

    fun onTicketsTapped() {
        if (userRepository.isLoggedIn()) {
            showTickets.call()
        } else {
            pendingTickets = true
            showLoginAlert.call()
        }
    }

    fun onStartLoginTapped() {
        showLogin.postValue(LoginSignupSource.Support)
    }

    fun onCancelLoginTapped() {
        clearPendingActions()
    }

    private fun resumePendingActions() {
        if (pendingTickets) {
            onTicketsTapped()
        }
    }

    private fun clearPendingActions() {
        pendingTickets = false
    }
}
