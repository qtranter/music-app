package com.audiomack.data.ads

import com.ad.core.adBaseManager.AdData
import com.audiomack.data.ads.AudioAdState.None
import io.reactivex.Observable

sealed class AudioAdState(val ad: AdData? = null) {
    /**
     * No ad has been requested
     */
    object None : AudioAdState()

    /**
     * Loading an ad
     */
    class Loading(ad: AdData? = null) : AudioAdState()

    /**
     * An ad is loaded and ready for playback
     */
    class Ready(ad: AdData?) : AudioAdState(ad)

    /**
     * An ad is playing
     */
    class Playing(ad: AdData?) : AudioAdState(ad)

    /**
     * All ads are done playing
     */
    object Done : AudioAdState()

    /**
     * Error while loading or playing an ad
     */
    class Error(val throwable: Throwable?) : AudioAdState()
}

interface AudioAdManager {
    /**
     * True when an ad is ready for playback
     *
     * @see AudioAdState
     */
    val hasAd: Boolean

    /**
     * The latest emission of [adStateObservable]
     */
    val adState: AudioAdState

    /**
     * An observable that emits the latest audio ad state
     */
    val adStateObservable: Observable<AudioAdState>

    /**
     * Returns the duration of the loaded audio ad, if there is one
     */
    val currentDuration: Double

    /**
     * Returns the current playback time of an audio ad, if one is playing
     */
    val currentPlaybackTime: Double

    /**
     * Starts playing an ad if ready, or loads a new one and plays when finished loading.
     *
     * @return An observable that emits the latest audio ad state after calling play and
     * completes when [AudioAdState.DONE] is emitted
     * @see [adStateObservable]
     */
    fun play(): Observable<AudioAdState>
}

object NoOpAudioAdManager : AudioAdManager {
    override val hasAd: Boolean = false
    override val adState: AudioAdState = None
    override val adStateObservable: Observable<AudioAdState> = Observable.never()
    override val currentPlaybackTime = 0.0
    override val currentDuration = 0.0
    override fun play(): Observable<AudioAdState> = Observable.never()
}
