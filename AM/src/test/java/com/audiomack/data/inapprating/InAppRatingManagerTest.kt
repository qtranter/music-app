package com.audiomack.data.inapprating

import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class InAppRatingManagerTest {

    @Mock
    private lateinit var remoteVariables: RemoteVariablesProvider

    @Mock
    private lateinit var trackingDataSource: TrackingDataSource

    @Mock
    private lateinit var preferences: InAppRatingPreferences

    @Mock
    private lateinit var engine: InAppRatingEngine

    private lateinit var sut: InAppRatingManager

    @Mock
    private lateinit var subjectObserver: TestObserver<InAppRatingResult>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = InAppRatingManager.getInstance(
            remoteVariables,
            trackingDataSource,
            preferences,
            engine
        ).apply {
            subjectObserver = inAppRating.test()
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
        InAppRatingManager.destroy()
    }

    @Test
    fun `increment download count`() {
        sut.incrementDownloadCount()
        verify(preferences).incrementDownloadCount()
    }

    @Test
    fun `increment favorites count`() {
        sut.incrementFavoriteCount()
        verify(preferences).incrementFavoriteCount()
    }

    @Test
    fun `on rating prompt accepted`() {
        sut.onRatingPromptAccepted()
        verify(preferences).answer = "yes"
        verify(trackingDataSource).trackEvent(eq("RatingEnjoyingAudiomack"), anyOrNull(), argWhere { it.first() == TrackingProvider.Firebase })
        subjectObserver.assertValue(InAppRatingResult.OpenRating)
    }

    @Test
    fun `on rating prompt declined`() {
        sut.onRatingPromptDeclined()
        verify(preferences).answer = "no"
        verify(trackingDataSource).trackEvent(eq("RatingNotEnjoyingAudiomack"), anyOrNull(), argWhere { it.first() == TrackingProvider.Firebase })
        subjectObserver.assertValue(InAppRatingResult.ShowDeclinedRatingPrompt)
    }

    @Test
    fun `on declined rating prompt accepted`() {
        sut.onDeclinedRatingPromptAccepted()
        verify(trackingDataSource).trackEvent(eq("RatingEnjoyingRedirect"), anyOrNull(), argWhere { it.first() == TrackingProvider.Firebase })
        subjectObserver.assertValue(InAppRatingResult.OpenSupport)
    }

    @Test
    fun `on declined rating prompt declined`() {
        sut.onDeclinedRatingPromptDeclined()
        verify(trackingDataSource).trackEvent(eq("RatingNotEnjoyingRedirect"), anyOrNull(), argWhere { it.first() == TrackingProvider.Firebase })
    }

    @Test
    fun `request - remote variable is off`() {
        whenever(remoteVariables.inAppRatingEnabled).doReturn(false)
        sut.request()
        subjectObserver.assertNoValues()
    }

    @Test
    fun `request - already rated`() {
        whenever(remoteVariables.inAppRatingEnabled).doReturn(true)
        whenever(preferences.answer).doReturn("yes")
        sut.request()
        subjectObserver.assertNoValues()
    }

    @Test
    fun `request - not enough time passed`() {
        val interval = 7 * 24 * 60 * 60 * 1000L
        whenever(remoteVariables.inAppRatingEnabled).doReturn(true)
        whenever(remoteVariables.inAppRatingInterval).doReturn(interval)
        whenever(preferences.answer).doReturn("")
        whenever(preferences.timestamp).doReturn(System.currentTimeMillis() - interval / 2)
        sut.request()
        subjectObserver.assertNoValues()
    }

    @Test
    fun `request - not enough downloads or favorites`() {
        whenever(remoteVariables.inAppRatingEnabled).doReturn(true)
        whenever(preferences.answer).doReturn("")
        whenever(preferences.timestamp).doReturn(0L)
        whenever(preferences.downloadsCount).doReturn(4)
        whenever(remoteVariables.inAppRatingMinDownloads).doReturn(5)
        whenever(preferences.favoritesCount).doReturn(4)
        whenever(remoteVariables.inAppRatingMinFavorites).doReturn(5)
        sut.request()
        subjectObserver.assertNoValues()
    }

    @Test
    fun `request - all conditions met`() {
        whenever(remoteVariables.inAppRatingEnabled).doReturn(true)
        whenever(preferences.answer).doReturn("")
        whenever(preferences.timestamp).doReturn(0L)
        whenever(preferences.downloadsCount).doReturn(5)
        whenever(remoteVariables.inAppRatingMinDownloads).doReturn(5)
        whenever(preferences.favoritesCount).doReturn(6)
        whenever(remoteVariables.inAppRatingMinFavorites).doReturn(5)
        sut.request()
        verify(preferences).timestamp > 0L
        verify(trackingDataSource).trackEvent(eq("RatingPrompt"), anyOrNull(), argWhere { it.first() == TrackingProvider.Firebase })
        subjectObserver.assertValue(InAppRatingResult.ShowRatingPrompt)
    }

    @Test
    fun `show rating`() {
        sut.show(mock())
        verify(engine).show(any())
    }
}
