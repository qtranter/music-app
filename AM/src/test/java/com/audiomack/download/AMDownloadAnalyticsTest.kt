package com.audiomack.download

import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.model.AMResultItem
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import java.lang.Exception
import org.junit.Before
import org.junit.Test

class AMDownloadAnalyticsTest {

    private lateinit var origin: DownloadOrigin
    private lateinit var trackingDataSource: TrackingDataSource
    private lateinit var sut: AmDownloadAnalytics

    @Before
    fun setup() {
        origin = DownloadOrigin.BASE_SINGLE
        trackingDataSource = mock()
        sut = AmDownloadAnalytics(origin, trackingDataSource)
    }

    @Test
    fun `download start`() {
        val track = mock<AMResultItem> {
            on { itemId } doReturn "123"
        }
        sut.eventDownloadStart(track)
        verify(trackingDataSource).trackGA(eq("Download Song"), eq("Single"), argWhere { it.contains("123") })
    }

    @Test
    fun `download failed`() {
        sut.eventDownloadFailure(Exception("Unknown error for tests"), "Extra infos")
        verify(trackingDataSource).trackGA(eq("Download Failed"), eq("Unknown error for tests"), anyOrNull())
    }

    @Test
    fun `download complete`() {
        val track = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { type } doReturn "song"
        }
        sut.eventDownloadComplete(track)
        verify(trackingDataSource).trackGA(eq("Download Successful"), eq("song"), argWhere { it.contains("123") })
    }
}
