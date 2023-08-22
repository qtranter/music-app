package com.audiomack.usecases.download

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.data.actions.ToggleDownloadResult
import com.audiomack.download.MusicDownloader
import com.audiomack.model.AMResultItem
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class DownloadUseCaseTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val actionDataSource: ActionsDataSource = mock()
    private val musicDownloader: MusicDownloader = mock()
    private val downloadEventsManager: DownloadEventsManager = mock()
    private val mixpanelSource: MixpanelSource = mock()

    private lateinit var schedulersProvider: SchedulersProvider
    private lateinit var downloadUseCaseImpl: DownloadUseCaseImpl

    @Before
    fun setup() {
        schedulersProvider = TestSchedulersProvider()

        downloadUseCaseImpl =
            DownloadUseCaseImpl(
                actionDataSource,
                musicDownloader,
                schedulersProvider,
                downloadEventsManager
            )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun isBeingDonloadedTest() {
        val item = mock<AMResultItem>()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(true)
        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(any())
        Assert.assertNull(disposable)
    }

    @Test
    fun isNotBeingDownloadedTest() {
        val item = mock<AMResultItem>()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(false)
        whenever(mixpanelSource.isInMyDownloads).thenReturn(false)
        whenever(actionDataSource.toggleDownload(item, "", mixpanelSource))
            .thenReturn(Observable.just(ToggleDownloadResult.DownloadStarted))
        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(any())
        Assert.assertNotNull(disposable)
    }

    @Test
    fun confirmPlaylistDeletionTest() {
        val item = mock<AMResultItem>()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(false)
        whenever(mixpanelSource.isInMyDownloads).thenReturn(false)
        whenever(actionDataSource.toggleDownload(item, "", mixpanelSource))
            .thenReturn(Observable.just(ToggleDownloadResult.ConfirmPlaylistDeletion))

        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(any())
        verify(downloadEventsManager, times(1)).showConfirmPlaylistDownloadDeletion(item)
        Assert.assertNotNull(disposable)
    }

    @Test
    fun confirmMusicDeletionTest() {
        val item = mock<AMResultItem>()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(false)
        whenever(mixpanelSource.isInMyDownloads).thenReturn(false)
        whenever(actionDataSource.toggleDownload(item, "", mixpanelSource))
            .thenReturn(Observable.just(ToggleDownloadResult.ConfirmMusicDeletion))

        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(any())
        verify(downloadEventsManager, times(1)).showConfirmDownloadDeletion(item)
        Assert.assertNotNull(disposable)
    }

    @Test
    fun confirmPlaylistDownloadTest() {
        val item = mock<AMResultItem>()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(false)
        whenever(mixpanelSource.isInMyDownloads).thenReturn(false)
        whenever(actionDataSource.toggleDownload(item, "", mixpanelSource))
            .thenReturn(Observable.just(ToggleDownloadResult.ConfirmPlaylistDownload(1)))

        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(any())
        verify(downloadEventsManager, times(1)).showConfirmPlaylistSync(item, 1)
        Assert.assertNotNull(disposable)
    }

    @Test
    fun startedBlockingAPICallTest() {
        val item = mock<AMResultItem>()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(false)
        whenever(mixpanelSource.isInMyDownloads).thenReturn(false)
        whenever(actionDataSource.toggleDownload(item, "", mixpanelSource))
            .thenReturn(Observable.just(ToggleDownloadResult.StartedBlockingAPICall))

        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(any())
        verify(downloadEventsManager, times(1)).showHUDE(ProgressHUDMode.Loading)
        Assert.assertNotNull(disposable)
    }

    @Test
    fun endedBlockingAPICallTest() {
        val item = mock<AMResultItem>()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(false)
        whenever(mixpanelSource.isInMyDownloads).thenReturn(false)
        whenever(actionDataSource.toggleDownload(item, "", mixpanelSource))
            .thenReturn(Observable.just(ToggleDownloadResult.EndedBlockingAPICall))

        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(any())
        verify(downloadEventsManager, times(1)).showHUDE(ProgressHUDMode.Dismiss)
        Assert.assertNotNull(disposable)
    }

    @Test
    fun showUnlockedToastTest() {
        val item = mock<AMResultItem>()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(false)
        whenever(mixpanelSource.isInMyDownloads).thenReturn(false)
        whenever(actionDataSource.toggleDownload(item, "", mixpanelSource))
            .thenReturn(Observable.just(ToggleDownloadResult.ShowUnlockedToast("musicName")))

        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(any())
        verify(downloadEventsManager, times(1)).showUnlockedToast("musicName")
        Assert.assertNotNull(disposable)
    }

    @Test
    fun loggedOutExceptionTest() {
        val item = mock<AMResultItem>()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(false)
        whenever(mixpanelSource.isInMyDownloads).thenReturn(false)
        whenever(actionDataSource.toggleDownload(item, "", mixpanelSource))
            .thenReturn(Observable.error(ToggleDownloadException.LoggedOut(LoginSignupSource.Download)))

        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(item)
        verify(downloadEventsManager, times(1))
            .loggedOutAlert(ToggleDownloadException.LoggedOut(LoginSignupSource.Download))
        Assert.assertNotNull(disposable)
    }

    @Test
    fun unsubscribedExceptionTest() {
        val item = mock<AMResultItem>()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(false)
        whenever(mixpanelSource.isInMyDownloads).thenReturn(false)
        whenever(actionDataSource.toggleDownload(item, "", mixpanelSource))
            .thenReturn(Observable.error(ToggleDownloadException.Unsubscribed(InAppPurchaseMode.AudioAd)))

        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(any())
        verify(downloadEventsManager, times(1))
            .showPremium(ToggleDownloadException.Unsubscribed(InAppPurchaseMode.AudioAd))
        Assert.assertNotNull(disposable)
    }

    @Test
    fun failedDownloadingPlaylistExceptionTest() {
        val item = mock<AMResultItem>()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(false)
        whenever(mixpanelSource.isInMyDownloads).thenReturn(false)
        whenever(actionDataSource.toggleDownload(item, "", mixpanelSource))
            .thenReturn(Observable.error(ToggleDownloadException.FailedDownloadingPlaylist))

        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(any())
        verify(downloadEventsManager, times(1)).showFailedPlaylistDownload(any())
        Assert.assertNotNull(disposable)
    }

    @Test
    fun showPremiumDownloadExceptionTest() {
        val item = mock<AMResultItem>()
        val premiumDownloadModel: PremiumDownloadModel = mock()
        whenever(musicDownloader.isMusicBeingDownloaded(item)).thenReturn(false)
        whenever(mixpanelSource.isInMyDownloads).thenReturn(false)
        whenever(
            actionDataSource.toggleDownload(
                item,
                "",
                mixpanelSource
            )
        ).thenReturn(
            Observable.error(
                ToggleDownloadException.ShowPremiumDownload(premiumDownloadModel)
            )
        )

        val disposable: Disposable? =
            downloadUseCaseImpl(item, mixpanelSource, "", null)
        verify(musicDownloader, atLeastOnce()).isMusicBeingDownloaded(any())
        verify(downloadEventsManager, times(1))
            .showPremiumDownload(ToggleDownloadException.ShowPremiumDownload(premiumDownloadModel))
        Assert.assertNotNull(disposable)
    }
}
