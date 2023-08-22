package com.audiomack.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.R
import com.audiomack.data.api.MusicDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.model.MusicType
import com.audiomack.model.ProgressHUDMode
import com.audiomack.playback.PlayerPlayback
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

class AddMusicToQueueUseCaseTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var musicDataSource: MusicDataSource
    @Mock private lateinit var playback: PlayerPlayback

    private lateinit var sut: AddMusicToQueueUseCaseImpl

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = AddMusicToQueueUseCaseImpl(musicDataSource, playback)
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
        whenever(musicDataSource.getMusicInfo("123", "song")).thenReturn(Observable.just(song))

        sut.loadAndAdd("123", MusicType.Song, MixpanelSource.empty, AddMusicToQueuePosition.Later)
            .test()
            .assertValues(
                AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Loading),
                AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Dismiss),
                AddMusicToQueueUseCaseResult.Success
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
        whenever(musicDataSource.getMusicInfo("123", "playlist")).thenReturn(Observable.just(playlist))

        sut.loadAndAdd("123", MusicType.Playlist, MixpanelSource.empty, AddMusicToQueuePosition.Later)
            .test()
            .assertValues(
                AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Loading),
                AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Dismiss),
                AddMusicToQueueUseCaseResult.Success
            )
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `error on loading album`() {
        whenever(musicDataSource.getMusicInfo("123", "album")).thenReturn(Observable.error(Exception("")))

        sut.loadAndAdd("123", MusicType.Album, MixpanelSource.empty, AddMusicToQueuePosition.Later)
            .test()
            .assertValues(
                AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Loading),
                AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Failure("", R.string.album_info_failed))
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

        sut.loadAndAdd("123", MusicType.Song, MixpanelSource.empty, AddMusicToQueuePosition.Later)
            .test()
            .assertValues(
                AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Loading),
                AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Dismiss),
                AddMusicToQueueUseCaseResult.Georestricted
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

        sut.loadAndAdd("123", MusicType.Album, MixpanelSource.empty, AddMusicToQueuePosition.Later)
            .test()
            .assertValues(
                AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Loading),
                AddMusicToQueueUseCaseResult.ToggleLoader(ProgressHUDMode.Dismiss),
                AddMusicToQueueUseCaseResult.Georestricted
            )
            .assertNoErrors()
            .assertComplete()
    }
}
