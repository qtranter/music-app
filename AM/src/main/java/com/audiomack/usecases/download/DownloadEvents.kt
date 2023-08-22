package com.audiomack.usecases.download

import androidx.lifecycle.LiveData
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.model.AMResultItem
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.utils.SingleLiveEvent

interface DownloadTriggers {
    fun showConfirmPlaylistDownloadDeletion(item: AMResultItem)
    fun showConfirmDownloadDeletion(item: AMResultItem)
    fun showConfirmPlaylistSync(item: AMResultItem, tracksCount: Int)
    fun showHUDE(hudMode: ProgressHUDMode)
    fun showUnlockedToast(musicName: String)
    fun loggedOutAlert(throwable: ToggleDownloadException.LoggedOut)
    fun showPremium(throwable: ToggleDownloadException.Unsubscribed)
    fun showFailedPlaylistDownload(throwable: ToggleDownloadException.FailedDownloadingPlaylist)
    fun showPremiumDownload(throwable: ToggleDownloadException.ShowPremiumDownload)
}

interface DownloadEvents {
    val showConfirmPlaylistDownloadDeletionEvent: LiveData<AMResultItem>
    val showConfirmDownloadDeletionEvent: LiveData<AMResultItem>
    val showConfirmPlaylistSyncEvent: LiveData<Pair<AMResultItem, Int>>
    val downloadHUDEvent: LiveData<ProgressHUDMode>
    val showUnlockedToastEvent: LiveData<String>
    val downloadLoggedOutAlertEvent: LiveData<LoginSignupSource>
    val showPremiumEvent: LiveData<InAppPurchaseMode>
    val showFailedPlaylistDownloadEvent: LiveData<Void>
    val showPremiumDownloadEvent: LiveData<PremiumDownloadModel>
}

object DownloadEventsManager : DownloadTriggers, DownloadEvents {
    override val showConfirmPlaylistDownloadDeletionEvent = SingleLiveEvent<AMResultItem>()
    override val showConfirmDownloadDeletionEvent = SingleLiveEvent<AMResultItem>()
    override val showConfirmPlaylistSyncEvent = SingleLiveEvent<Pair<AMResultItem, Int>>()
    override val downloadHUDEvent = SingleLiveEvent<ProgressHUDMode>()
    override val showUnlockedToastEvent = SingleLiveEvent<String>()
    override val downloadLoggedOutAlertEvent = SingleLiveEvent<LoginSignupSource>()
    override val showPremiumEvent = SingleLiveEvent<InAppPurchaseMode>()
    override val showFailedPlaylistDownloadEvent = SingleLiveEvent<Void>()
    override val showPremiumDownloadEvent = SingleLiveEvent<PremiumDownloadModel>()

    override fun showConfirmPlaylistDownloadDeletion(item: AMResultItem) =
        showConfirmPlaylistDownloadDeletionEvent.postValue(item)

    override fun showConfirmDownloadDeletion(item: AMResultItem) =
        showConfirmDownloadDeletionEvent.postValue(item)

    override fun showConfirmPlaylistSync(item: AMResultItem, tracksCount: Int) =
        showConfirmPlaylistSyncEvent.postValue(Pair(item, tracksCount))

    override fun showHUDE(hudMode: ProgressHUDMode) =
        downloadHUDEvent.postValue(hudMode)

    override fun showUnlockedToast(musicName: String) =
        showUnlockedToastEvent.postValue(musicName)

    override fun loggedOutAlert(throwable: ToggleDownloadException.LoggedOut) =
        downloadLoggedOutAlertEvent.postValue(throwable.source)

    override fun showPremium(throwable: ToggleDownloadException.Unsubscribed) =
        showPremiumEvent.postValue(throwable.mode)

    override fun showFailedPlaylistDownload(throwable: ToggleDownloadException.FailedDownloadingPlaylist) =
        showFailedPlaylistDownloadEvent.call()

    override fun showPremiumDownload(throwable: ToggleDownloadException.ShowPremiumDownload) =
        showPremiumDownloadEvent.postValue(throwable.model)
}
