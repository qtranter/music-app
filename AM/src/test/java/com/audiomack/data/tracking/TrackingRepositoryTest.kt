package com.audiomack.data.tracking

import com.audiomack.network.AnalyticsHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TrackingRepositoryTest {

    private lateinit var analytics: AnalyticsHelper
    private lateinit var trackingRepository: TrackingRepository

    @Before
    fun setup() {
        analytics = mock(AnalyticsHelper::class.java)
        trackingRepository = TrackingRepository(analytics)
    }

    @Test
    fun trackEvent_shouldPreserveEventName() {
        val data = HashMap<String, String>()
        val providers = emptyList<TrackingProvider>()
        val event = "testEvent"

        val trackedData = trackingRepository.trackEvent(event, data, providers)

        assertEquals(trackedData, event)
    }

    @Test
    fun trackScreen_shouldPreserveEventName() {
        val event = "testEvent"

        val trackedData = trackingRepository.trackScreen(event)

        assertEquals(trackedData, event)
    }

    @Test
    fun trackGA_shouldPreserveEventName() {
        val event = "testEvent"
        val action = "asd"
        val label = "123"

        val trackedData = trackingRepository.trackGA(event, action, label)

        assertEquals(trackedData, event)
    }
}
