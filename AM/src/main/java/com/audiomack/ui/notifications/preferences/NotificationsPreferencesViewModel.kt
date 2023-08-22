package com.audiomack.ui.notifications.preferences

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.api.NotificationSettingsDataSource
import com.audiomack.data.api.NotificationSettingsRepository
import com.audiomack.data.api.NotificationsEnabledResult
import com.audiomack.model.NotificationPreferenceType
import com.audiomack.model.NotificationPreferenceTypeValue
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo

class NotificationsPreferencesViewModel(
    private val notificationSettingsDataSource: NotificationSettingsDataSource = NotificationSettingsRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    val closeEvent = SingleLiveEvent<Void>()
    val openOSNotificationSettingsEvent = SingleLiveEvent<Void>()

    private var _notificationsDisabledVisible = MutableLiveData(false)
    val notificationsDisabledVisible: LiveData<Boolean> get() = _notificationsDisabledVisible

    private var _newSongAlbumEnabled = MutableLiveData<Boolean>()
    val newSongAlbumEnabled: LiveData<Boolean> get() = _newSongAlbumEnabled

    private var _weeklyArtistReportsEnabled = MutableLiveData<Boolean>()
    val weeklyArtistReportsEnabled: LiveData<Boolean> get() = _weeklyArtistReportsEnabled

    private var _playMilestonesEnabled = MutableLiveData<Boolean>()
    val playMilestonesEnabled: LiveData<Boolean> get() = _playMilestonesEnabled

    private var _commentRepliesEnabled = MutableLiveData<Boolean>()
    val commentRepliesEnabled: LiveData<Boolean> get() = _commentRepliesEnabled

    private var _upvoteMilestonesEnabled = MutableLiveData<Boolean>()
    val upvoteMilestonesEnabled: LiveData<Boolean> get() = _upvoteMilestonesEnabled

    private var _verifiedPlaylistAddsEnabled = MutableLiveData<Boolean>()
    val verifiedPlaylistAddsEnabled: LiveData<Boolean> get() = _verifiedPlaylistAddsEnabled

    init {
        notificationSettingsDataSource.getNotificationPreferences()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ settings ->
                _newSongAlbumEnabled.postValue(settings.firstOrNull { it.type == NotificationPreferenceType.NewSongAlbum }?.value ?: false)
                _weeklyArtistReportsEnabled.postValue(settings.firstOrNull { it.type == NotificationPreferenceType.WeeklyArtistReport }?.value ?: false)
                _playMilestonesEnabled.postValue(settings.firstOrNull { it.type == NotificationPreferenceType.PlayMilestones }?.value ?: false)
                _commentRepliesEnabled.postValue(settings.firstOrNull { it.type == NotificationPreferenceType.CommentReplies }?.value ?: false)
                _upvoteMilestonesEnabled.postValue(settings.firstOrNull { it.type == NotificationPreferenceType.UpvoteMilestones }?.value ?: false)
                _verifiedPlaylistAddsEnabled.postValue(settings.firstOrNull { it.type == NotificationPreferenceType.VerifiedPlaylistAdds }?.value ?: false)
            }, {})
            .addTo(compositeDisposable)
    }

    fun fetchNotificationsEnabledStatus() {
        notificationSettingsDataSource.areNotificationsEnabledForNewMusic()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ _notificationsDisabledVisible.postValue(it is NotificationsEnabledResult.DisabledAtOSLevel) }, {})
            .addTo(compositeDisposable)
    }

    fun onBackClicked() {
        closeEvent.call()
    }

    fun onNotificationsDisabledClicked() {
        openOSNotificationSettingsEvent.call()
    }

    fun onPreferenceChanged(typeValue: NotificationPreferenceTypeValue) {
        val liveData = when (typeValue.type) {
            NotificationPreferenceType.NewSongAlbum -> _newSongAlbumEnabled
            NotificationPreferenceType.WeeklyArtistReport -> _weeklyArtistReportsEnabled
            NotificationPreferenceType.PlayMilestones -> _playMilestonesEnabled
            NotificationPreferenceType.CommentReplies -> _commentRepliesEnabled
            NotificationPreferenceType.UpvoteMilestones -> _upvoteMilestonesEnabled
            NotificationPreferenceType.VerifiedPlaylistAdds -> _verifiedPlaylistAddsEnabled
        }
        notificationSettingsDataSource.setNotificationPreference(typeValue)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                liveData.postValue(if (it) typeValue.value else !typeValue.value)
            }, {
                liveData.postValue(!typeValue.value)
            })
            .addTo(compositeDisposable)
    }
}
