package com.audiomack.data.ads

import com.mopub.mobileads.MoPubInterstitial

sealed class ShowInterstitialResult {
    class Shown(val interstitial: MoPubInterstitial) : ShowInterstitialResult()
    object Dismissed : ShowInterstitialResult()
    object Loading : ShowInterstitialResult()
    class Ready(val interstitial: MoPubInterstitial) : ShowInterstitialResult()
    data class Failed(val reason: String) : ShowInterstitialResult()
    data class NotShown(val reason: String) : ShowInterstitialResult()
}
