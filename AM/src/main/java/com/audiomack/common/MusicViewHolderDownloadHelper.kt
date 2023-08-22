package com.audiomack.common

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.audiomack.DISABLED_ALPHA
import com.audiomack.R
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadRepository
import com.audiomack.download.AMMusicDownloader
import com.audiomack.download.MusicDownloader
import com.audiomack.model.AMResultItem
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.views.AMProgressBar
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MusicViewHolderDownloadHelper(
    private val premiumDownloadDataSource: PremiumDownloadDataSource = PremiumDownloadRepository.getInstance(),
    private val premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
    private val musicDownloader: MusicDownloader = AMMusicDownloader.getInstance()
) {
    fun configureDownloadStatus(
        music: AMResultItem,
        badgeFrozen: TextView?,
        imageViewDownloaded: ImageView,
        buttonDownload: ImageButton,
        progressBarDownloading: AMProgressBar,
        buttonActions: ImageButton?,
        views: List<View>?,
        myDownloadsMode: Boolean
    ): Disposable {
        return Single.fromCallable {
            val downloadStatus = when {
                music.isDownloadCompletedIndependentlyFromType -> MusicDownloadStatus.Completed
                musicDownloader.isMusicBeingDownloaded(music) -> MusicDownloadStatus.InProgress
                musicDownloader.isMusicWaitingForDownload(music) -> MusicDownloadStatus.Queued
                myDownloadsMode && music.isDownloadedAndNotCached -> MusicDownloadStatus.Failed
                else -> MusicDownloadStatus.Idle
            }
            val downloadType = music.downloadType
            val frozenCount = premiumDownloadDataSource.getFrozenCount(music)
            val isPremium = premiumDataSource.isPremium
            val shouldShowFrozenCount = music.isDownloaded && (music.isAlbum || music.isPlaylist) && frozenCount > 0
            MusicDownloadTypeDetails(downloadStatus, downloadType, isPremium, shouldShowFrozenCount, frozenCount)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ state ->

                imageViewDownloaded.visibility = if (state.downloadStatus == MusicDownloadStatus.Completed) View.VISIBLE else View.INVISIBLE
                buttonDownload.visibility = if (state.downloadStatus == MusicDownloadStatus.Completed || state.downloadStatus == MusicDownloadStatus.InProgress || state.downloadStatus == MusicDownloadStatus.Queued) View.GONE else View.VISIBLE
                progressBarDownloading.visibility = if (state.downloadStatus == MusicDownloadStatus.InProgress || state.downloadStatus == MusicDownloadStatus.Queued) View.VISIBLE else View.GONE
                if (state.downloadStatus == MusicDownloadStatus.InProgress) {
                    progressBarDownloading.applyColor(R.color.orange)
                } else if (state.downloadStatus == MusicDownloadStatus.Queued) {
                    progressBarDownloading.applyColor(R.color.gray_text)
                }
                buttonActions?.setImageDrawable(buttonActions.context.drawableCompat(
                        if (state.downloadStatus == MusicDownloadStatus.Failed) R.drawable.ic_redownload else R.drawable.ic_list_kebab
                    )
                )
                views?.forEach { it.alpha = if (myDownloadsMode && state.downloadStatus == MusicDownloadStatus.Failed) DISABLED_ALPHA else 1.0f }

                badgeFrozen?.let { it.text = "${state.frozenCount}" }
                badgeFrozen?.let { it.visibility = if (state.shouldShowFrozenCount) View.VISIBLE else View.GONE }
                buttonDownload.setImageDrawable(buttonDownload.context.drawableCompat(if (state.downloadType == AMResultItem.MusicDownloadType.Limited || (state.isPremium && state.downloadType == AMResultItem.MusicDownloadType.Premium)) R.drawable.ic_download_premium else R.drawable.ic_list_download_off))
                imageViewDownloaded.setImageDrawable(
                    imageViewDownloaded.context.drawableCompat(
                        if (state.frozenCount > 0)
                            if (state.downloadType == AMResultItem.MusicDownloadType.Premium) R.drawable.ic_download_frozen_locked
                            else R.drawable.ic_download_frozen_unlocked
                        else if (state.downloadType == AMResultItem.MusicDownloadType.Limited || (state.isPremium && state.downloadType == AMResultItem.MusicDownloadType.Premium)) R.drawable.ic_download_downloaded_premium
                        else if (!state.isPremium && state.downloadType == AMResultItem.MusicDownloadType.Premium) R.drawable.ic_download_frozen_locked
                        else R.drawable.ic_list_download_completed
                    )
                )
                buttonDownload.alpha = if (!state.isPremium && state.downloadType == AMResultItem.MusicDownloadType.Premium) DISABLED_ALPHA else 1.0F
            }, { Timber.w(it) })
    }
}

private class MusicDownloadTypeDetails(
    val downloadStatus: MusicDownloadStatus,
    val downloadType: AMResultItem.MusicDownloadType,
    val isPremium: Boolean,
    val shouldShowFrozenCount: Boolean,
    val frozenCount: Int
)

enum class MusicDownloadStatus {
    Completed, Failed, InProgress, Queued, Idle
}
