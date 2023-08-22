package com.audiomack.data.ads

import android.content.Context
import com.mopub.mobileads.MoPubView
import io.reactivex.Observable

interface AdsDataSource {

    /** Tells wether ads are enabled for the user **/
    val adsVisible: Boolean

    /** Turns ads on or off based on the user's premium status **/
    fun toggle()

    /** Removes all ad views and timers **/
    fun destroy()

    /** Called when the lifecycle is started **/
    fun create()

    /** Called when the banner view is ready to be shown (main activity is resumed) **/
    fun onBannerAppeared()

    /** Stops all ads, to be called when the app goes into background **/
    fun stopAds()

    /** Resumes all ads, to be called when the app goes into foreground **/
    fun restartAds()

    /** To be called when the ad layout is ready **/
    fun setHomeViewLoaded()

    /** Initializes Ogury's SDK, it's safe to call this multiple times since the init is idempotent **/
    fun initOgury()

    /** Used to set the Mopub banner view grabbed from the xml layout **/
    fun postInit(homeBannerView: MoPubView?)

    /** Clears any reference to the 300x250 ad. To be called once such ad has been removed from the view **/
    fun resetMopub300x250Ad()

    /** Caches a new native ad without showing it **/
    fun preloadNativeAd()

    /** Shows (or load if not ready yet) a player ad, could be native or banner **/
    fun showPlayerAd(showPlayerAdWhenReady: Boolean)

    /** Tells wether we are in the first app session ever **/
    fun isFreshInstall(): Boolean

    /** Emits events related to interstitial ads **/
    val interstitialObservable: Observable<ShowInterstitialResult>

    /**
     * Shows an interstitial if one is loaded and conditions are met.
     *
     * Callers may subscribe to results with [interstitialObservable]
     * */
    fun showInterstitial()

    /** Fetches the advertising identifier **/
    fun getAdvertisingIdentifier(context: Context): Observable<String>
}
