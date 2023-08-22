package com.audiomack.usecases.download

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ToggleDownloadException
import com.audiomack.model.AMResultItem
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.PremiumDownloadModel
import com.audiomack.model.ProgressHUDMode
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test

class DownloadEventsManagerTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val downloadEventsManager = DownloadEventsManager

    @Test
    fun showConfirmPlaylistDownloadDeletionTest() {
        val item = mock<AMResultItem>()
        val observer: Observer<AMResultItem> = mock()
        downloadEventsManager.showConfirmPlaylistDownloadDeletionEvent.observeForever(observer)
        downloadEventsManager.showConfirmPlaylistDownloadDeletion(item)
        verify(observer, atLeastOnce()).onChanged(item)
    }

    @Test
    fun showConfirmDownloadDeletionTest() {
        val item = mock<AMResultItem>()
        val observer: Observer<AMResultItem> = mock()
        downloadEventsManager.showConfirmDownloadDeletionEvent.observeForever(observer)
        downloadEventsManager.showConfirmDownloadDeletion(item)
        verify(observer, atLeastOnce()).onChanged(item)
    }

    @Test
    fun showConfirmPlaylistSyncTest() {
        val item = mock<AMResultItem>()
        val observer: Observer<Pair<AMResultItem, Int>> = mock()
        downloadEventsManager.showConfirmPlaylistSyncEvent.observeForever(observer)
        downloadEventsManager.showConfirmPlaylistSync(item, 2)
        verify(observer, atLeastOnce()).onChanged(Pair(item, 2))
    }

    @Test
    fun downloadHUDEventTest() {
        val item = mock<ProgressHUDMode>()
        val observer: Observer<ProgressHUDMode> = mock()
        downloadEventsManager.downloadHUDEvent.observeForever(observer)
        downloadEventsManager.showHUDE(item)
        verify(observer, atLeastOnce()).onChanged(item)
    }

    @Test
    fun showUnlockedToastTest() {
        val musicName = "musicName"
        val observer: Observer<String> = mock()
        downloadEventsManager.showUnlockedToastEvent.observeForever(observer)
        downloadEventsManager.showUnlockedToast(musicName)
        verify(observer, atLeastOnce()).onChanged(musicName)
    }

    @Test
    fun loggedOutAlertTest() {
        val throwable = ToggleDownloadException.LoggedOut(LoginSignupSource.Download)
        val observer: Observer<LoginSignupSource> = mock()
        downloadEventsManager.downloadLoggedOutAlertEvent.observeForever(observer)
        downloadEventsManager.loggedOutAlert(throwable)
        verify(observer, atLeastOnce()).onChanged(throwable.source)
    }

    @Test
    fun showPremiumTest() {
        val throwable = ToggleDownloadException.Unsubscribed(InAppPurchaseMode.PlaylistDownload)
        val observer: Observer<InAppPurchaseMode> = mock()
        downloadEventsManager.showPremiumEvent.observeForever(observer)
        downloadEventsManager.showPremium(throwable)
        verify(observer, atLeastOnce()).onChanged(throwable.mode)
    }

    @Test
    fun showFailedPlaylistDownloadTest() {
        val throwable = ToggleDownloadException.FailedDownloadingPlaylist
        val observer: Observer<Void> = mock()
        downloadEventsManager.showFailedPlaylistDownloadEvent.observeForever(observer)
        downloadEventsManager.showFailedPlaylistDownload(throwable)
        verify(observer, atLeastOnce()).onChanged(null)
    }

    @Test
    fun showPremiumDownloadTest() {
        val premiumDownloadModel = mock<PremiumDownloadModel>()
        val throwable = ToggleDownloadException.ShowPremiumDownload(premiumDownloadModel)
        val observer: Observer<PremiumDownloadModel> = mock()
        downloadEventsManager.showPremiumDownloadEvent.observeForever(observer)
        downloadEventsManager.showPremiumDownload(throwable)
        verify(observer, atLeastOnce()).onChanged(premiumDownloadModel)
    }
}
