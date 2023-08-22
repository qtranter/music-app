package com.audiomack.usecases.download

import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.download.AMMusicDownloader
import com.audiomack.download.MusicDownloader
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.model.ProgressHUDMode
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.disposables.Disposable
import timber.log.Timber

interface DownloadUseCase {
    operator fun invoke(
        item: AMResultItem,
        mixpanelSource: MixpanelSource,
        mixpanelButton: String,
        onLoginRequired: (() -> Unit)?
    ): Disposable?
}

class DownloadUseCaseImpl(
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val musicDownloader: MusicDownloader = AMMusicDownloader.getInstance(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val downloadEventsManager: DownloadEventsManager = DownloadEventsManager
) : DownloadUseCase {
    override fun invoke(
        item: AMResultItem,
        mixpanelSource: MixpanelSource,
        mixpanelButton: String,
        onLoginRequired: (() -> Unit)?
    ): Disposable? {
        return if (!musicDownloader.isMusicBeingDownloaded(item)) {
            download(item, mixpanelSource, mixpanelButton, onLoginRequired)
        } else null
    }

    private fun download(
        item: AMResultItem,
        mixpanelSource: MixpanelSource,
        mixpanelButton: String,
        onLoginRequired: (() -> Unit)?
    ): Disposable {
        return actionsDataSource.toggleDownload(
            item,
            mixpanelButton,
            mixpanelSource,
            skipFrozenCheck = !mixpanelSource.isInMyDownloads
        ).subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ result ->
                when (result) {
                    is ToggleDownloadResult.ConfirmPlaylistDeletion ->
                        downloadEventsManager.showConfirmPlaylistDownloadDeletion(item)
                    is ToggleDownloadResult.ConfirmMusicDeletion ->
                        downloadEventsManager.showConfirmDownloadDeletion(item)
                    is ToggleDownloadResult.ConfirmPlaylistDownload ->
                        downloadEventsManager.showConfirmPlaylistSync(item, result.tracksCount)
                    is ToggleDownloadResult.StartedBlockingAPICall ->
                        downloadEventsManager.showHUDE(ProgressHUDMode.Loading)
                    is ToggleDownloadResult.EndedBlockingAPICall ->
                        downloadEventsManager.showHUDE(ProgressHUDMode.Dismiss)
                    is ToggleDownloadResult.ShowUnlockedToast ->
                        downloadEventsManager.showUnlockedToast(result.musicName)
                    is ToggleDownloadResult.DownloadStarted -> Timber.i("DownloadStarted")
                }
            }, { throwable ->
                when (throwable) {
                    is ToggleDownloadException.LoggedOut -> {
                        onLoginRequired?.invoke()
                        downloadEventsManager.loggedOutAlert(throwable)
                    }
                    is ToggleDownloadException.Unsubscribed ->
                        downloadEventsManager.showPremium(throwable)
                    is ToggleDownloadException.FailedDownloadingPlaylist ->
                        downloadEventsManager.showFailedPlaylistDownload(throwable)
                    is ToggleDownloadException.ShowPremiumDownload ->
                        downloadEventsManager.showPremiumDownload(throwable)
                    else -> Timber.w(throwable)
                }
            })
    }
}
