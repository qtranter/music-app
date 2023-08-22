package com.audiomack.common

import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.playback.ActionState
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class MusicDownloadActionStateHelperTest {

    private lateinit var sut: MusicDownloadActionStateHelperImpl

    @Mock
    private lateinit var premiumDownloadDataSource: PremiumDownloadDataSource

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = MusicDownloadActionStateHelperImpl(premiumDownloadDataSource, premiumDataSource)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `in progress state`() {
        val music = mock<AMResultItem> {
            on { isDownloadInProgress } doReturn true
        }
        assert(sut.downloadState(music) == ActionState.LOADING)
    }

    @Test
    fun `queued state`() {
        val music = mock<AMResultItem> {
            on { isDownloadQueued } doReturn true
        }
        assert(sut.downloadState(music) == ActionState.QUEUED)
    }

    @Test
    fun `frozen state`() {
        val music = mock<AMResultItem> {
            on { isDownloaded } doReturn true
            on { tracks } doReturn emptyList()
        }
        val frozenCount = 3
        whenever(premiumDownloadDataSource.getFrozenCount(any())).thenReturn(frozenCount)
        val result = sut.downloadState(music)
        assert(result == ActionState.FROZEN)
        assert(result.frozenDownloadsCount == frozenCount)
        assert(result.frozenDownloadsTotal == 0)
    }

    @Test
    fun `active state`() {
        val type = AMResultItem.MusicDownloadType.Premium
        val premium = true
        val music = mock<AMResultItem> {
            on { isDownloadCompletedIndependentlyFromType } doReturn true
            on { downloadType } doReturn type
        }
        whenever(premiumDataSource.isPremium).thenReturn(premium)
        val result = sut.downloadState(music)
        assert(result == ActionState.ACTIVE)
        assert(result.downloadType == type)
        assert(result.isPremium == premium)
        verify(premiumDataSource).isPremium
    }

    @Test
    fun `active state, premium value passed directly`() {
        val type = AMResultItem.MusicDownloadType.Premium
        val premium = false
        val music = mock<AMResultItem> {
            on { isDownloadCompletedIndependentlyFromType } doReturn true
            on { downloadType } doReturn type
        }
        val result = sut.downloadState(music, premium)
        assert(result == ActionState.ACTIVE)
        assert(result.downloadType == type)
        assert(result.isPremium == premium)
        verify(premiumDataSource, never()).isPremium
    }

    @Test
    fun `default state`() {
        val type = AMResultItem.MusicDownloadType.Premium
        val premium = false
        val music = mock<AMResultItem> {
            on { downloadType } doReturn type
        }
        whenever(premiumDataSource.isPremium).thenReturn(premium)
        val result = sut.downloadState(music)
        assert(result == ActionState.DEFAULT)
        assert(result.downloadType == type)
        assert(result.isPremium == premium)
        verify(premiumDataSource).isPremium
    }

    @Test
    fun `default state, premium value passed directly`() {
        val type = AMResultItem.MusicDownloadType.Premium
        val premium = true
        val music = mock<AMResultItem> {
            on { downloadType } doReturn type
        }
        val result = sut.downloadState(music, premium)
        assert(result == ActionState.DEFAULT)
        assert(result.downloadType == type)
        assert(result.isPremium == premium)
        verify(premiumDataSource, never()).isPremium
    }
}
