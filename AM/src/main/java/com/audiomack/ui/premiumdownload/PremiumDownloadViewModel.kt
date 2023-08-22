package com.audiomack.ui.premiumdownload

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.PREMIUM_SUPPORT_URL
import com.audiomack.model.MusicType
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.PremiumLimitedDownloadInfoViewType
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent

class PremiumDownloadViewModel : BaseViewModel() {

    private lateinit var data: PremiumDownloadModel

    val backEvent = SingleLiveEvent<Void>()
    val upgradeEvent = SingleLiveEvent<Void>()
    val goToDownloadsEvent = SingleLiveEvent<Void>()
    val openURLEvent = SingleLiveEvent<String>()

    /** Emits the [Float] percentage of limited downloads that have been used **/
    private var _progressPercentage = MutableLiveData<Float>()
    val progressPercentage: LiveData<Float> get() = _progressPercentage

    /** Emits the [PremiumDownloadProgressInfo] object with data to be used to show the info text **/
    private var _infoText = MutableLiveData<PremiumDownloadProgressInfo>()
    val infoText: LiveData<PremiumDownloadProgressInfo> get() = _infoText

    /** Emits the [Boolean] representing the visibility of the "first download" UI **/
    private var _firstDownloadLayoutVisible = MutableLiveData<Boolean>()
    val firstDownloadLayoutVisible: LiveData<Boolean> get() = _firstDownloadLayoutVisible

    fun init(
        data: PremiumDownloadModel
    ) {
        this.data = data
        _infoText.postValue(PremiumDownloadProgressInfo(
            countOfSongsBeingDownloaded = this.data.music?.countOfSongsToBeDownloaded ?: 0,
            countOfAvailablDownloads = this.data.stats.availableCount,
            maxDownloads = this.data.stats.premiumLimitCount,
            typeLimited = this.data.infoTypeLimited,
            musicType = this.data.music?.type ?: MusicType.Song
        ))
        _progressPercentage.postValue(this.data.stats.premiumLimitUnfrozenDownloadCount.toFloat() / this.data.stats.premiumLimitCount.toFloat())
        _firstDownloadLayoutVisible.postValue(this.data.infoTypeLimited == PremiumLimitedDownloadInfoViewType.FirstDownload)
    }

    fun onBackClick() {
        backEvent.call()
    }

    fun onUpgradeClick() {
        upgradeEvent.call()
    }

    fun onGoToDownloadsClick() {
        goToDownloadsEvent.call()
    }

    fun onLearnClick() {
        openURLEvent.postValue(PREMIUM_SUPPORT_URL)
    }
}

data class PremiumDownloadProgressInfo(
    val countOfSongsBeingDownloaded: Int,
    val countOfAvailablDownloads: Int,
    val maxDownloads: Int,
    val typeLimited: PremiumLimitedDownloadInfoViewType,
    val musicType: MusicType
)
