package com.audiomack.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.R
import com.audiomack.data.api.MusicDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.model.MaximizePlayerData
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import com.audiomack.model.ProgressHUDMode
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class PlayMusicFromIdUseCaseTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var musicDataSource: MusicDataSource

    private lateinit var sut: PlayMusicFromIdUseCaseImpl

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = PlayMusicFromIdUseCaseImpl(musicDataSource)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `successfully loaded song`() {
        val song = mock<AMResultItem> {
            on { isGeoRestricted } doReturn false
        }
        val mixpanelSource = MixpanelSource.empty
        whenever(musicDataSource.getMusicInfo("123", "song")).thenReturn(Observable.just(song))

        sut.loadAndPlay("123", MusicType.Song, mixpanelSource)
            .test()
            .assertValues(
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Loading),
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Dismiss),
                PlayMusicFromIdResult.ReadyToPlay(
                    MaximizePlayerData(
                    item = song,
                    mixpanelSource = mixpanelSource
                ))
            )
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `successfully loaded album`() {
        val track = mock<AMResultItem>()
        val album = mock<AMResultItem> {
            on { isGeoRestricted } doReturn false
            on { tracksWithoutRestricted } doReturn listOf(track)
        }
        val mixpanelSource = MixpanelSource.empty
        whenever(musicDataSource.getMusicInfo("123", "album")).thenReturn(Observable.just(album))

        sut.loadAndPlay("123", MusicType.Album, mixpanelSource)
            .test()
            .assertValues(
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Loading),
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Dismiss),
                PlayMusicFromIdResult.ReadyToPlay(MaximizePlayerData(
                    item = track,
                    collection = album,
                    albumPlaylistIndex = 0,
                    mixpanelSource = mixpanelSource
                ))
            )
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `successfully loaded playlist`() {
        val track = mock<AMResultItem>()
        val playlist = mock<AMResultItem> {
            on { isGeoRestricted } doReturn false
            on { tracksWithoutRestricted } doReturn listOf(track)
        }
        val mixpanelSource = MixpanelSource.empty
        whenever(musicDataSource.getMusicInfo("123", "playlist")).thenReturn(Observable.just(playlist))

        sut.loadAndPlay("123", MusicType.Playlist, mixpanelSource)
            .test()
            .assertValues(
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Loading),
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Dismiss),
                PlayMusicFromIdResult.ReadyToPlay(MaximizePlayerData(
                    item = track,
                    collection = playlist,
                    albumPlaylistIndex = 0,
                    loadFullPlaylist = true,
                    mixpanelSource = mixpanelSource
                ))
            )
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `error on loading album`() {
        whenever(musicDataSource.getMusicInfo("123", "album")).thenReturn(Observable.error(Exception("")))

        sut.loadAndPlay("123", MusicType.Album, MixpanelSource.empty)
            .test()
            .assertValues(
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Loading),
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Failure("", R.string.album_info_failed))
            )
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `georestricted song`() {
        val song = mock<AMResultItem> {
            on { isGeoRestricted } doReturn true
        }
        whenever(musicDataSource.getMusicInfo("123", "song")).thenReturn(Observable.just(song))

        sut.loadAndPlay("123", MusicType.Song, MixpanelSource.empty)
            .test()
            .assertValues(
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Loading),
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Dismiss),
                PlayMusicFromIdResult.Georestricted
            )
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `album with georestricted tracks`() {
        val album = mock<AMResultItem> {
            on { isGeoRestricted } doReturn false
            on { tracksWithoutRestricted } doReturn emptyList()
        }
        whenever(musicDataSource.getMusicInfo("123", "album")).thenReturn(Observable.just(album))

        sut.loadAndPlay("123", MusicType.Album, MixpanelSource.empty)
            .test()
            .assertValues(
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Loading),
                PlayMusicFromIdResult.ToggleLoader(ProgressHUDMode.Dismiss),
                PlayMusicFromIdResult.Georestricted
            )
            .assertNoErrors()
            .assertComplete()
    }
}
