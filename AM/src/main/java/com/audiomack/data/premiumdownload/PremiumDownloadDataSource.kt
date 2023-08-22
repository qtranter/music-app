package com.audiomack.data.premiumdownload

import com.audiomack.model.AMResultItem

interface PremiumDownloadDataSource {

    val premiumDownloadLimit: Int
    val remainingPremiumLimitedDownloadCount: Int
    val premiumLimitedUnfrozenDownloadCount: Int

    fun getToBeDownloadedPremiumLimitedCount(music: AMResultItem): Int
    fun canDownloadMusicBasedOnPremiumLimitedCount(music: AMResultItem): Boolean
    fun getFrozenCount(music: AMResultItem): Int
}
