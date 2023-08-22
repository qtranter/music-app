package com.audiomack.ui.ads

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.ads.AdsWizzManager
import com.audiomack.data.ads.AudioAdManager
import com.audiomack.data.ads.AudioAdState.Playing
import com.audiomack.data.logviewer.LogType
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import io.reactivex.Observable
import java.util.concurrent.TimeUnit.SECONDS
import timber.log.Timber

typealias AudioAdPlaying = Boolean
typealias StartTrial = Boolean

class AudioAdViewModel(
    private val audioAdManager: AudioAdManager = AdsWizzManager.getInstance(),
    private val trackingRepo: TrackingDataSource = TrackingRepository(),
    private val schedulers: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    private val timeRemaining: Double
        get() = audioAdManager.currentDuration.minus(audioAdManager.currentPlaybackTime)

    private val _secondsRemaining = MutableLiveData<Long>()
    val secondsRemaining: LiveData<Long> get() = _secondsRemaining

    val audioAdEvent = SingleLiveEvent<AudioAdPlaying>()
    val upSellClickEvent = SingleLiveEvent<StartTrial>()

    val companionAdDisplayedEvent = SingleLiveEvent<Boolean>()
    val showHouseAdEvent = SingleLiveEvent<Void>()

    init {
        subscribePlayback()
        subscribeTimer()
    }

    private fun subscribePlayback() {
        audioAdManager.adStateObservable
            .distinctUntilChanged()
            .observeOn(schedulers.main)
            .subscribe {
                audioAdEvent.value = it is Playing
            }.addTo(compositeDisposable)
    }

    private fun subscribeTimer() {
        audioAdManager.adStateObservable
            .distinctUntilChanged()
            .filter { it is Playing }
            .flatMap {
                Observable.interval(1, SECONDS, schedulers.interval)
                    .takeUntil { timeRemaining == 0.0 }
                    .map { timeRemaining.toLong() }
            }
            .observeOn(schedulers.main)
            .subscribe {
                _secondsRemaining.value = it
            }.addTo(compositeDisposable)
    }

    fun onUpSellClick() {
        upSellClickEvent.postValue(false)
    }

    fun onStartTrialClick() {
        upSellClickEvent.postValue(true)
    }

    fun onCompanionAdDisplayed() {
        notifyAdmins("Audio ad companion shown")
        companionAdDisplayedEvent.postValue(true)
    }

    fun onCompanionAdEnded() {
        notifyAdmins("Audio ad companion ad closed")
        companionAdDisplayedEvent.postValue(false)
    }

    fun onViewVisible() {
        if (companionAdDisplayedEvent.value != true) {
            Observable.timer(DEFAULT_COMPANION_DELAY, SECONDS, schedulers.interval)
                .takeWhile { companionAdDisplayedEvent.value != true }
                .observeOn(schedulers.main)
                .subscribe { showHouseAdEvent.call() }
                .addTo(compositeDisposable)
        }
    }

    fun onError(throwable: Throwable?) {
        notifyAdmins("Audio ad companion ad failed")
        showHouseAdEvent.call()
        throwable?.let { trackingRepo.trackException(it) }
    }

    private fun notifyAdmins(message: String) = Timber.tag(LogType.ADS.tag).d(message)

    companion object {
        const val DEFAULT_COMPANION_DELAY = 3L
    }
}
