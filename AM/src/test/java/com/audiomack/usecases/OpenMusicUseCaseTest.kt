package com.audiomack.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.R
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.database.MusicDAOException
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelPageFeedSuggestedFollows
import com.audiomack.data.tracking.mixpanel.MixpanelPageMyLibraryOffline
import com.audiomack.data.tracking.mixpanel.MixpanelTabFeed
import com.audiomack.model.AMResultItem
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.MixpanelSource
import com.audiomack.model.NextPageData
import com.audiomack.model.OpenMusicData
import com.audiomack.model.ProgressHUDMode
import com.audiomack.network.APIException
import com.audiomack.ui.common.Resource
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class OpenMusicUseCaseTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val musicDataSource: MusicDataSource = mock()
    private val reachabilityDataSource: ReachabilityDataSource = mock()
    private val mixPanelSource = MixpanelSource(MixpanelTabFeed, MixpanelPageFeedSuggestedFollows)

    private lateinit var openMusicUseCase: OpenMusicUseCaseImpl

    @Before
    fun setup() {
        openMusicUseCase =
            OpenMusicUseCaseImpl(musicDataSource, reachabilityDataSource)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `geoRestricted item`() {
        val song = mock<AMResultItem> {
            on { isGeoRestricted } doReturn true
        }
        val data = OpenMusicData(song, emptyList(), mixPanelSource, false, null, 0)
        openMusicUseCase(data)
            .test()
            .assertValue(OpenMusicResult.GeoRestricted)
            .assertNoErrors()
            .assertComplete()
    }

    // Playlist
    @Test
    fun `successfully open playlist available network`() {
        val track = mock<AMResultItem>()
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { isGeoRestricted } doReturn false
            on { isPlaylist } doReturn true
            on { tracksWithoutRestricted } doReturn listOf(track)
        }
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(musicDataSource.getPlaylistInfo("123")).thenReturn(Observable.just(playlist))
        whenever(musicDataSource.getOfflineResource("123")).thenReturn(
            Observable.just(
                Resource.Success(
                    playlist
                )
            )
        )

        val data = OpenMusicData(playlist, emptyList(), mixPanelSource, false, null, 0)
        openMusicUseCase(data)
            .test()
            .assertValues(
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Loading),
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss),
                OpenMusicResult.ShowPlaylist(
                    playlist,
                    online = true,
                    deleted = false,
                    mixpanelSource = mixPanelSource,
                    openShare = false
                )
            )
            .assertNoErrors()
            .assertComplete()

        verify(playlist, atLeastOnce()).updatePlaylist(playlist)
    }

    @Test
    fun `successfully open playlist no available network`() {
        val track = mock<AMResultItem>()
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { isGeoRestricted } doReturn false
            on { isPlaylist } doReturn true
            on { tracksWithoutRestricted } doReturn listOf(track)
        }
        whenever(reachabilityDataSource.networkAvailable).thenReturn(false)
        whenever(musicDataSource.getOfflineResource("123")).thenReturn(
            Observable.just(
                Resource.Success(
                    playlist
                )
            )
        )

        val data = OpenMusicData(playlist, emptyList(), mixPanelSource, false, null, 0)
        openMusicUseCase(data)
            .test()
            .assertValues(
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Loading),
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss),
                OpenMusicResult.ShowPlaylist(
                    playlist,
                    online = false,
                    deleted = false,
                    mixpanelSource = mixPanelSource,
                    openShare = false
                )
            )
            .assertNoErrors()
            .assertComplete()

        verify(playlist, atLeastOnce()).loadTracks()
    }

    @Test
    fun `MusicDAOException error open playlist`() {
        val track = mock<AMResultItem>()
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { isGeoRestricted } doReturn false
            on { isPlaylist } doReturn true
            on { tracksWithoutRestricted } doReturn listOf(track)
        }
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(musicDataSource.getPlaylistInfo("123")).thenReturn(Observable.just(playlist))
        whenever(musicDataSource.getOfflineResource("123")).thenReturn(
            Observable.error(MusicDAOException(""))
        )

        val data = OpenMusicData(playlist, emptyList(), mixPanelSource, false, null, 0)
        openMusicUseCase(data)
            .test()
            .assertValues(
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Loading),
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss),
                OpenMusicResult.ShowPlaylist(
                    playlist,
                    online = true,
                    deleted = false,
                    mixpanelSource = mixPanelSource,
                    openShare = false
                )
            )
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `APIException error open playlist`() {
        val track = mock<AMResultItem>()
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { isGeoRestricted } doReturn false
            on { isPlaylist } doReturn true
            on { tracksWithoutRestricted } doReturn listOf(track)
        }
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(musicDataSource.getPlaylistInfo("123")).thenReturn(
            Observable.error(
                APIException(
                    404
                )
            )
        )
        whenever(musicDataSource.getOfflineResource("123")).thenReturn(
            Observable.just(
                Resource.Success(
                    playlist
                )
            )
        )

        val data = OpenMusicData(playlist, emptyList(), mixPanelSource, false, null, 0)
        openMusicUseCase(data)
            .test()
            .assertValues(
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Loading),
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss),
                OpenMusicResult.ShowPlaylist(
                    playlist,
                    online = true,
                    deleted = false,
                    mixpanelSource = mixPanelSource,
                    openShare = false
                )
            )
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `error open playlist`() {
        val track = mock<AMResultItem>()
        val playlist = mock<AMResultItem> {
            on { isGeoRestricted } doReturn false
            on { isPlaylist } doReturn true
            on { tracksWithoutRestricted } doReturn listOf(track)
        }
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(musicDataSource.getPlaylistInfo("123")).thenReturn(Observable.just(playlist))
        whenever(musicDataSource.getOfflineResource("123")).thenReturn(
            Observable.just(
                Resource.Success(
                    playlist
                )
            )
        )

        val data = OpenMusicData(playlist, emptyList(), mixPanelSource, false, null, 0)
        openMusicUseCase(data)
            .test()
            .assertValues(
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Loading),
                OpenMusicResult.ToggleLoader(
                    ProgressHUDMode.Failure(
                        "",
                        R.string.playlist_info_failed
                    )
                )
            )
            .assertNoErrors()
            .assertComplete()
    }

    // Album
    @Test
    fun `successfully open album`() {
        val track = mock<AMResultItem>()
        val album = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { isGeoRestricted } doReturn false
            on { isAlbum } doReturn true
            on { tracksWithoutRestricted } doReturn listOf(track)
        }
        whenever(musicDataSource.getAlbumInfo("123")).thenReturn(Observable.just(album))

        val data = OpenMusicData(album, emptyList(), mixPanelSource, true, null, 0)
        openMusicUseCase(data)
            .test()
            .assertValues(
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Loading),
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss),
                OpenMusicResult.ShowAlbum(
                    album = album,
                    mixpanelSource = mixPanelSource,
                    openShare = true
                )
            )
            .assertNoErrors()
            .assertComplete()

        Assert.assertEquals(false, mixPanelSource.shuffled)
    }

    @Test
    fun `successfully open album offline`() {
        val mixPanelSource = MixpanelSource(MixpanelTabFeed, MixpanelPageMyLibraryOffline)

        val track = mock<AMResultItem>()
        val album = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { isGeoRestricted } doReturn false
            on { isAlbum } doReturn true
            on { tracksWithoutRestricted } doReturn listOf(track)
        }
        whenever(musicDataSource.getAlbumInfo("123")).thenReturn(Observable.just(album))
        whenever(musicDataSource.getOfflineResource("123")).thenReturn(
            Observable.just(
                Resource.Success(
                    album
                )
            )
        )

        val data = OpenMusicData(album, emptyList(), mixPanelSource, false, null, 0)
        openMusicUseCase(data)
            .test()
            .assertValues(
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Loading),
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Dismiss),
                OpenMusicResult.ShowAlbum(
                    album = album,
                    mixpanelSource = mixPanelSource,
                    openShare = false
                )
            )
            .assertNoErrors()
            .assertComplete()

        Assert.assertEquals(false, mixPanelSource.shuffled)
        verify(album, atLeastOnce()).loadTracks()
    }

    @Test
    fun `error open album`() {
        val track = mock<AMResultItem>()
        val album = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { isGeoRestricted } doReturn false
            on { isAlbum } doReturn true
            on { tracksWithoutRestricted } doReturn listOf(track)
        }
        whenever(musicDataSource.getAlbumInfo("123")).thenReturn(Observable.error(Exception("")))

        val data = OpenMusicData(album, emptyList(), mixPanelSource, false, null, 0)
        openMusicUseCase(data)
            .test()
            .assertValues(
                OpenMusicResult.ToggleLoader(ProgressHUDMode.Loading),
                OpenMusicResult.ToggleLoader(
                    ProgressHUDMode.Failure(
                        "",
                        R.string.album_info_failed
                    )
                )
            )
            .assertNoErrors()
            .assertComplete()

        Assert.assertEquals(false, mixPanelSource.shuffled)
    }

    // Song
    @Test
    fun `successfully open song`() {
        val items = listOf(mock<AMResultItem>())
        val song = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { isGeoRestricted } doReturn false
        }

        val data = OpenMusicData(song, items, mixPanelSource, true, "api.audiomack.com", 2)
        openMusicUseCase(data)
            .test()
            .assertValue(
                OpenMusicResult.ReadyToPlay(
                    MaximizePlayerData(
                        item = song,
                        items = items,
                        mixpanelSource = mixPanelSource,
                        openShare = true,
                        nextPageData = NextPageData("api.audiomack.com", 2, mixPanelSource, false)
                    )
                )
            )
            .assertNoErrors()
            .assertComplete()
    }
}
