package com.audiomack.ui.authentication.contact

import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.support.ZendeskDataSource
import com.audiomack.data.support.ZendeskRepository
import com.audiomack.data.support.ZendeskSupportStatsRepository
import com.audiomack.model.Action
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.common.ContactProvider
import com.audiomack.utils.SingleLiveEvent

class ContactSupportViewModel(
    private val zendeskDataSource: ZendeskDataSource = ZendeskRepository(
        ZendeskSupportStatsRepository(),
        DeviceRepository,
        PremiumRepository.getInstance()
    ),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val preferencesDataSource: PreferencesDataSource = PreferencesRepository()
) : BaseViewModel() {

    private lateinit var contactProvider: ContactProvider

    val showLoadingEvent = SingleLiveEvent<Void>()
    val hideLoadingEvent = SingleLiveEvent<Void>()
    val showErrorMessageEvent = SingleLiveEvent<Void>()
    val showSuccessMessageEvent = SingleLiveEvent<Void>()
    val showOptionsEvent = SingleLiveEvent<List<Action>>()
    val closeOptionsEvent = SingleLiveEvent<Void>()
    val closeEvent = SingleLiveEvent<Void>()
    val sendEvent = SingleLiveEvent<Void>()
    val errorEvent = SingleLiveEvent<Void>()
    val tooltipEvent = SingleLiveEvent<Void>()

    val whatSelectEvent = SingleLiveEvent<String>()
    val howSelectEvent = SingleLiveEvent<String>()
    val whenSelectEvent = SingleLiveEvent<String>()
    val emailEvent = SingleLiveEvent<String>()
    val notesEvent = SingleLiveEvent<String>()

    fun init(contactProvider: ContactProvider) {
        this.contactProvider = contactProvider
    }

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onSendTapped(whatText: String, howText: String, whenText: String, emailText: String, notesText: String) {
        if (whatText.isEmpty() || howText.isEmpty() || whenText.isEmpty() || emailText.isEmpty() || notesText.isEmpty()) {
            errorEvent.call()
        } else {
            if (!preferencesDataSource.needToShowContactTooltip) {
                sendEvent.call()
            } else {
                tooltipEvent.call()
            }
        }
    }

    fun onSendTicket(whatText: String, howText: String, whenText: String, emailText: String, notesText: String) {

        showLoadingEvent.call()

        compositeDisposable.add(
            zendeskDataSource.sendSupportTicket(whatText, howText, whenText, emailText, notesText)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    preferencesDataSource.needToShowContactTooltip = true
                    hideLoadingEvent.call()
                    closeEvent.call()
                    showSuccessMessageEvent.call()
                }, {
                    hideLoadingEvent.call()
                    closeEvent.call()
                    showErrorMessageEvent.call()
                })
        )
    }

    fun onWhatTapped() {
        val actions = contactProvider.getWhatTitleList().map { contact ->
            val listener = object : Action.ActionListener {
                override fun onActionExecuted() {
                    whatSelectEvent.postValue(contact)
                    closeOptionsEvent.call()
                }
            }
            Action(contact, false, listener)
        }
        showOptionsEvent.postValue(actions)
    }

    fun onHowTapped() {
        val actions = contactProvider.getHowTitleList().map { contact ->
            val listener = object : Action.ActionListener {
                override fun onActionExecuted() {
                    howSelectEvent.postValue(contact)
                    closeOptionsEvent.call()
                }
            }
            Action(contact, false, listener)
        }
        showOptionsEvent.postValue(actions)
    }

    fun onWhenTapped() {
        val actions = contactProvider.getWhenTitleList().map { contact ->
            val listener = object : Action.ActionListener {
                override fun onActionExecuted() {
                    whenSelectEvent.postValue(contact)
                    closeOptionsEvent.call()
                }
            }
            Action(contact, false, listener)
        }
        showOptionsEvent.postValue(actions)
    }

    fun onEmailChanged(string: String) {
        emailEvent.postValue(string)
    }

    fun onNotesChanged(string: String) {
        notesEvent.postValue(string)
    }
}
