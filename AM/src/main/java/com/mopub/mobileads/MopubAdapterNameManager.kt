package com.mopub.mobileads

import com.mopub.nativeads.NativeAd

object MopubAdapterNameManager {

    fun getAdapterNameFromBanner(moPubView: MoPubView): String? {
        return moPubView.mAdViewController?.baseAdClassName?.split(".")?.lastOrNull()
    }

    fun getAdapterNameFromInterstitial(interstitial: MoPubInterstitial): String? {
        return interstitial.mAdViewController?.baseAdClassName?.split(".")?.lastOrNull()
    }

    fun getAdapterNameFromNative(nativeAd: NativeAd): String {
        return nativeAd.moPubAdRenderer.javaClass.simpleName
    }
}
