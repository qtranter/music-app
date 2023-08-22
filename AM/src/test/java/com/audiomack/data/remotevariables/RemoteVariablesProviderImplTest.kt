package com.audiomack.data.remotevariables

import com.audiomack.data.remotevariables.datasource.RemoteVariablesDataSource
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import org.junit.After
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class RemoteVariablesProviderImplTest {

    private val firebase: RemoteVariablesDataSource = mock()
    private val sut = RemoteVariablesProviderImpl(firebase)

    private val schedulersProvider = TestSchedulersProvider()

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun initialise() {
        `when`(firebase.init(any())).thenReturn(Observable.just(true))

        sut.initialise()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({}, {})

        verify(firebase).init(any())
    }

    @Test
    fun interstitialTiming() {
        sut.interstitialTiming
        verify(firebase).getLong(any())
    }

    @Test
    fun playerAdEnabled() {
        sut.playerAdEnabled
        verify(firebase).getBoolean(any())
    }

    @Test
    fun bannerAdEnabled() {
        sut.bannerAdEnabled
        verify(firebase).getBoolean(any())
    }

    @Test
    fun interstitialAdEnabled() {
        sut.interstitialAdEnabled
        verify(firebase).getBoolean(any())
    }

    @Test
    fun interstitialSoundOnAdEnabled() {
        sut.interstitialSoundOnAdEnabled
        verify(firebase).getBoolean(any())
    }

    @Test
    fun playerNativeAdsPercentage() {
        sut.playerNativeAdsPercentage
        verify(firebase).getLong(any())
    }

    @Test
    fun interstitialSoundOnAdPeriod() {
        sut.interstitialSoundOnAdPeriod
        verify(firebase).getLong(any())
    }

    @Test
    fun apsEnabled() {
        sut.apsEnabled
        verify(firebase).getBoolean(any())
    }

    @Test
    fun apsTimeout() {
        sut.apsTimeout
        verify(firebase).getLong(any())
    }

    @Test
    fun firstOpeningDeeplink() {
        sut.firstOpeningDeeplink
        verify(firebase).getString(any())
    }

    @Test
    fun tester() {
        sut.tester
        verify(firebase).getBoolean(any())
    }

    @Test
    fun downloadCheckEnabled() {
        sut.downloadCheckEnabled
        verify(firebase).getBoolean(any())
    }

    @Test
    fun syncCheckEnabled() {
        sut.syncCheckEnabled
        verify(firebase).getBoolean(any())
    }

    @Test
    fun bookmarksEnabled() {
        sut.bookmarksEnabled
        verify(firebase).getBoolean(any())
    }

    @Test
    fun bookmarksExpirationHours() {
        sut.bookmarksExpirationHours
        verify(firebase).getLong(any())
    }

    @Test
    fun inAppRatingEnabled() {
        sut.inAppRatingEnabled
        verify(firebase).getBoolean(any())
    }

    @Test
    fun inAppUpdatesMinImmediateVersion() {
        sut.inAppUpdatesMinImmediateVersion
        verify(firebase).getString(any())
    }

    @Test
    fun inAppUpdatesMinFlexibleVersion() {
        sut.inAppUpdatesMinFlexibleVersion
        verify(firebase).getString(any())
    }

    @Test
    fun audioAdsEnabled() {
        sut.audioAdsEnabled
        verify(firebase).getBoolean(any())
    }

    @Test
    fun audioAdsTiming() {
        sut.audioAdsTiming
        verify(firebase).getLong(any())
    }

    @Test
    fun adFirstPlayDelay() {
        sut.adFirstPlayDelay
    }

    @Test
    fun inAppRatingMinFavorites() {
        sut.inAppRatingMinFavorites
        verify(firebase).getLong(any())
    }

    @Test
    fun inAppRatingMinDownloads() {
        sut.inAppRatingMinDownloads
        verify(firebase).getLong(any())
    }

    @Test
    fun inAppRatingInterval() {
        sut.inAppRatingInterval
        verify(firebase).getLong(any())
    }

    @Test
    fun deeplinksPathsBlacklist() {
        whenever(firebase.getString(any())).thenReturn("a,,bb ")
        val result = sut.deeplinksPathsBlacklist
        verify(firebase).getString(any())
        assert(result == listOf("a", "bb"))
    }

    @Test
    fun trendingBannerEnabled() {
        sut.trendingBannerEnabled
        verify(firebase).getBoolean(any())
    }

    @Test
    fun trendingBannerMessage() {
        sut.trendingBannerMessage
        verify(firebase).getString(any())
    }

    @Test
    fun trendingBannerLink() {
        sut.trendingBannerLink
        verify(firebase).getString(any())
    }

    @Test
    fun hideFollowOnSearchForLoggedOutUsers() {
        sut.hideFollowOnSearchForLoggedOutUsers
        verify(firebase).getBoolean(any())
    }

    @Test
    fun slideUpMenuShareMode() {
        sut.slideUpMenuShareMode
        verify(firebase).getString(any())
    }

    @Test
    fun loginAlertMessage() {
        sut.loginAlertMessage
        verify(firebase).getString(any())
    }

    @Test
    fun adWithholdPlays() {
        sut.adWithholdPlays
        verify(firebase).getLong(any())
    }
}
