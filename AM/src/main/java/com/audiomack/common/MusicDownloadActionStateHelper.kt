package com.audiomack.common

import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.playback.ActionState

interface MusicDownloadActionStateHelper {
    fun downloadState(music: AMResultItem, premium: Boolean? = null): ActionState
}

class MusicDownloadActionStateHelperImpl(
    private val premiumDownloadDataSource: PremiumDownloadDataSource,
    private val premiumDataSource: PremiumDataSource
) : MusicDownloadActionStateHelper {

    override fun downloadState(music: AMResultItem, premium: Boolean?) =
        when {
            music.isDownloadInProgress -> ActionState.LOADING
            music.isDownloadQueued -> ActionState.QUEUED
            music.isDownloaded && (premiumDownloadDataSource.getFrozenCount(music) > 0 || (music.downloadType == AMResultItem.MusicDownloadType.Premium &&
                !(premium ?: premiumDataSource.isPremium))) -> ActionState.FROZEN.apply {
                frozenDownloadsCount = premiumDownloadDataSource.getFrozenCount(music)
                frozenDownloadsTotal = music.tracks?.size ?: 1
            }
            music.isDownloadCompletedIndependentlyFromType -> ActionState.ACTIVE.apply {
                downloadType = music.downloadType
                isPremium = premium ?: premiumDataSource.isPremium
            }
            else -> ActionState.DEFAULT.apply {
                downloadType = music.downloadType
                isPremium = premium ?: premiumDataSource.isPremium
            }
        }
}
