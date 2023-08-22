package com.audiomack.ui.playlist.add

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.database.MusicDAO
import com.audiomack.data.inapprating.InAppRating
import com.audiomack.data.playlist.PlayListDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataInterface
import com.audiomack.model.AMResultItem
import com.audiomack.model.AddToPlaylistModel
import com.audiomack.model.MixpanelSource
import com.audiomack.model.Music
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class AddToPlaylistViewModelTest {

    @get:Rule val rule = InstantTaskExecutorRule()

    @Mock private lateinit var userDataInterface: UserDataInterface
    @Mock private lateinit var playListDataSource: PlayListDataSource
    @Mock private lateinit var musicDAO: MusicDAO
    @Mock private lateinit var mixpanelDataSource: MixpanelDataSource
    @Mock private lateinit var trackingDataSource: TrackingDataSource
    @Mock private lateinit var eventBus: EventBus
    private lateinit var schedulersProvider: SchedulersProvider
    @Mock private lateinit var actionsDataSource: ActionsDataSource
    @Mock private lateinit var inAppRating: InAppRating

    @Mock private lateinit var progressBarVisibleObserver: Observer<Boolean>
    @Mock private lateinit var reloadAdapterPositionEventObserver: Observer<Int>
    @Mock private lateinit var addDataToAdapterEventObserver: Observer<List<AMResultItem>>
    @Mock private lateinit var hideLoadMoreEventObserver: Observer<Void>
    @Mock private lateinit var enableLoadMoreEventObserver: Observer<Void>
    @Mock private lateinit var disableLoadMoreEventObserver: Observer<Void>
    @Mock private lateinit var closeEventObserver: Observer<Void>
    @Mock private lateinit var showPlaylistsEventObserver: Observer<Void>
    @Mock private lateinit var newPlaylistEventObserver: Observer<Void>
    @Mock private lateinit var playlistCannotBeEditedEventObserver: Observer<Void>
    @Mock private lateinit var songCannotBeAddedEventObserver: Observer<Void>
    @Mock private lateinit var cannotRemoveLastTrackEventObserver: Observer<Void>
    @Mock private lateinit var addedSongEventObserver: Observer<Void>
    @Mock private lateinit var failedToAddSongEventObserver: Observer<Void>
    @Mock private lateinit var removedSongEventObserver: Observer<Void>
    @Mock private lateinit var failedToRemoveSongEventObserver: Observer<Void>
    @Mock private lateinit var failedToFetchPlaylistEventObserver: Observer<Void>

    private lateinit var viewModel: AddToPlaylistsViewModel

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        schedulersProvider = TestSchedulersProvider()

        viewModel = AddToPlaylistsViewModel(
            userDataInterface,
            playListDataSource,
            musicDAO,
            mixpanelDataSource,
            trackingDataSource,
            eventBus,
            schedulersProvider,
            actionsDataSource,
            inAppRating
        ).apply {
            progressBarVisible.observeForever(progressBarVisibleObserver)
            reloadAdapterPositionEvent.observeForever(reloadAdapterPositionEventObserver)
            addDataToAdapterEvent.observeForever(addDataToAdapterEventObserver)
            hideLoadMoreEvent.observeForever(hideLoadMoreEventObserver)
            enableLoadMoreEvent.observeForever(enableLoadMoreEventObserver)
            disableLoadMoreEvent.observeForever(disableLoadMoreEventObserver)
            closeEvent.observeForever(closeEventObserver)
            showPlaylistsEvent.observeForever(showPlaylistsEventObserver)
            newPlaylistEvent.observeForever(newPlaylistEventObserver)
            playlistCannotBeEditedEvent.observeForever(playlistCannotBeEditedEventObserver)
            songCannotBeAddedEvent.observeForever(songCannotBeAddedEventObserver)
            cannotRemoveLastTrackEvent.observeForever(cannotRemoveLastTrackEventObserver)
            addedSongEvent.observeForever(addedSongEventObserver)
            failedToAddSongEvent.observeForever(failedToAddSongEventObserver)
            removedSongEvent.observeForever(removedSongEventObserver)
            failedToRemoveSongEvent.observeForever(failedToRemoveSongEventObserver)
            failedToFetchPlaylistEvent.observeForever(failedToFetchPlaylistEventObserver)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `on create - user has at least one playlist`() {
        whenever(userDataInterface.myPlaylistsCount).thenReturn(1)
        viewModel.onCreate()
        verify(showPlaylistsEventObserver).onChanged(null)
    }

    @Test
    fun `on create - user has no playlist`() {
        whenever(userDataInterface.myPlaylistsCount).thenReturn(0)
        viewModel.onCreate()
        verify(newPlaylistEventObserver).onChanged(null)
    }

    @Test
    fun `on close click observed`() {
        viewModel.onCloseCliked()
        verify(closeEventObserver).onChanged(null)
    }

    @Test
    fun `requestPlaylists - empty songs`() {
        testDownloadEmptySongs { viewModel.requestPlaylists() }
    }

    @Test
    fun `requestPlaylists - success - got many playlists`() {
        testDownloadSuccessManyPlaylists { viewModel.requestPlaylists() }
    }

    @Test
    fun `requestPlaylists - success - got no playlists`() {
        testDownloadSuccessNoPlaylists { viewModel.requestPlaylists() }
    }

    @Test
    fun `requestPlaylists - error on first page`() {
        testDownloadErrorOnFirstPage { viewModel.requestPlaylists() }
    }

    @Test
    fun `load more - empty songs`() {
        testDownloadEmptySongs { viewModel.didStartLoadMore() }
    }

    @Test
    fun `load more - success - got many playlists`() {
        testDownloadSuccessManyPlaylists { viewModel.didStartLoadMore() }
    }

    @Test
    fun `load more - success - got no playlists`() {
        testDownloadSuccessNoPlaylists { viewModel.didStartLoadMore() }
    }

    @Test
    fun `load more - error on next pages`() {
        testDownloadErrorOnNextPages { viewModel.didStartLoadMore() }
    }

    private fun testDownloadEmptySongs(action: () -> Unit) {
        viewModel.init(AddToPlaylistModel(emptyList(), "", "", MixpanelSource.empty, ""))

        action()

        verifyNoMoreInteractions(playListDataSource)
    }

    private fun testDownloadSuccessManyPlaylists(action: () -> Unit) {
        viewModel.init(AddToPlaylistModel(listOf(Music(id = "1")), "", "", MixpanelSource.empty, ""))
        val playlistMock = mock<AMResultItem> {
            on { itemId } doReturn "123"
        }
        whenever(playListDataSource.getMyPlaylists(anyInt(), anyOrNull(), anyOrNull(), anyBoolean())).thenReturn(
            Observable.just(listOf(playlistMock, playlistMock, playlistMock)))

        action()

        verify(playListDataSource, times(2)).getMyPlaylists(anyInt(), anyOrNull(), anyOrNull(), anyBoolean())
        verify(userDataInterface, times(3)).addPlaylistToMyPlaylists(any())
        verify(addDataToAdapterEventObserver).onChanged(any())
        verifyZeroInteractions(disableLoadMoreEventObserver)
        verify(enableLoadMoreEventObserver).onChanged(null)
    }

    private fun testDownloadSuccessNoPlaylists(action: () -> Unit) {
        viewModel.init(AddToPlaylistModel(listOf(Music(id = "1")), "", "", MixpanelSource.empty, ""))
        whenever(playListDataSource.getMyPlaylists(anyInt(), anyOrNull(), anyOrNull(), anyBoolean())).thenReturn(
            Observable.just(listOf()))

        action()

        verify(playListDataSource, times(2)).getMyPlaylists(anyInt(), anyOrNull(), anyOrNull(), anyBoolean())
        verify(userDataInterface, times(0)).addPlaylistToMyPlaylists(any())
        verify(addDataToAdapterEventObserver).onChanged(any())
        verifyZeroInteractions(enableLoadMoreEventObserver)
        verify(disableLoadMoreEventObserver).onChanged(null)
    }

    private fun testDownloadErrorOnFirstPage(action: () -> Unit) {
        viewModel.init(AddToPlaylistModel(listOf(Music(id = "1")), "", "", MixpanelSource.empty, ""))
        whenever(playListDataSource.getMyPlaylists(anyInt(), anyOrNull(), anyOrNull(), anyBoolean())).thenReturn(
            Observable.error(Exception("")))

        action()

        verify(playListDataSource, times(2)).getMyPlaylists(anyInt(), anyOrNull(), anyOrNull(), anyBoolean())
        verify(userDataInterface, times(0)).addPlaylistToMyPlaylists(any())
        verifyZeroInteractions(addDataToAdapterEventObserver)
        verifyZeroInteractions(enableLoadMoreEventObserver)
        verifyZeroInteractions(disableLoadMoreEventObserver)
        verify(hideLoadMoreEventObserver).onChanged(null)
        verify(failedToFetchPlaylistEventObserver).onChanged(null)
    }

    private fun testDownloadErrorOnNextPages(action: () -> Unit) {
        viewModel.init(AddToPlaylistModel(listOf(Music(id = "1")), "", "", MixpanelSource.empty, ""))
        whenever(playListDataSource.getMyPlaylists(anyInt(), anyOrNull(), anyOrNull(), anyBoolean())).thenReturn(
            Observable.error(Exception("")))

        action()

        verify(playListDataSource, times(2)).getMyPlaylists(anyInt(), anyOrNull(), anyOrNull(), anyBoolean())
        verify(userDataInterface, times(0)).addPlaylistToMyPlaylists(any())
        verifyZeroInteractions(addDataToAdapterEventObserver)
        verifyZeroInteractions(enableLoadMoreEventObserver)
        verifyZeroInteractions(disableLoadMoreEventObserver)
        verify(hideLoadMoreEventObserver).onChanged(null)
        verifyZeroInteractions(failedToFetchPlaylistEventObserver)
    }

    @Test
    fun `toggle playlist, already loading`() {
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { addToPlaylistStatus } doReturn AMResultItem.ItemAPIStatus.Loading
        }
        viewModel.didTogglePlaylist(playlist, 0)
        verifyZeroInteractions(playListDataSource)
    }

    @Test
    fun `toggle playlist, null id`() {
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn null
        }
        viewModel.didTogglePlaylist(playlist, 0)
        verify(playlistCannotBeEditedEventObserver).onChanged(null)
        verifyZeroInteractions(playListDataSource)
    }

    @Test
    fun `toggle playlist, empty songs to be added`() {
        viewModel.init(AddToPlaylistModel(emptyList(), "", "", MixpanelSource.empty, ""))
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn "123"
        }
        viewModel.didTogglePlaylist(playlist, 0)
        verify(songCannotBeAddedEventObserver).onChanged(null)
        verifyZeroInteractions(playListDataSource)
    }

    @Test
    fun `toggle playlist, one song, add, success and download`() {
        viewModel.init(AddToPlaylistModel(listOf(Music(id = "1")), "", "", MixpanelSource.empty, ""))
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { addToPlaylistStatus } doReturn AMResultItem.ItemAPIStatus.Off
        }
        whenever(playListDataSource.addSongsToPlaylist(any(), any(), any())).thenReturn(Completable.complete())
        whenever(musicDAO.findById(any())).thenReturn(Observable.just(mock()))
        whenever(playListDataSource.getPlaylistInfo(any())).thenReturn(Observable.just(mock()))
        whenever(actionsDataSource.toggleDownload(any(), any(), any(), any(), any(), anyOrNull())).thenReturn(Observable.empty())

        viewModel.didTogglePlaylist(playlist, 0)
        verify(playListDataSource).addSongsToPlaylist("123", "1", "")
        verify(addedSongEventObserver).onChanged(null)
        verify(reloadAdapterPositionEventObserver).onChanged(0)
        verify(trackingDataSource).trackGA(any(), any(), any())
        verify(mixpanelDataSource).trackAddToPlaylist(any(), any(), any(), any())
        verify(inAppRating).request()
        verify(actionsDataSource).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `toggle playlist, many songs, add, success and don't download`() {
        viewModel.init(AddToPlaylistModel(listOf(Music(id = "1"), Music(id = "2")), "", "", MixpanelSource.empty, ""))
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { addToPlaylistStatus } doReturn AMResultItem.ItemAPIStatus.Off
        }
        whenever(playListDataSource.addSongsToPlaylist(any(), any(), any())).thenReturn(Completable.complete())
        whenever(musicDAO.findById(any())).thenReturn(Observable.error(Exception("")))
        whenever(playListDataSource.getPlaylistInfo(any())).thenReturn(Observable.just(mock()))

        viewModel.didTogglePlaylist(playlist, 0)
        verify(playListDataSource).addSongsToPlaylist("123", "1,2", "")
        verify(addedSongEventObserver).onChanged(null)
        verify(reloadAdapterPositionEventObserver).onChanged(0)
        verifyZeroInteractions(trackingDataSource)
        verifyZeroInteractions(mixpanelDataSource)
        verify(inAppRating).request()
        verify(actionsDataSource, never()).toggleDownload(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `toggle playlist, one song, add, error`() {
        viewModel.init(AddToPlaylistModel(listOf(Music(id = "1")), "", "", MixpanelSource.empty, ""))
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { addToPlaylistStatus } doReturn AMResultItem.ItemAPIStatus.Off
        }
        whenever(playListDataSource.addSongsToPlaylist(any(), any(), any())).thenReturn(Completable.error(Exception("")))

        viewModel.didTogglePlaylist(playlist, 0)
        verify(playListDataSource).addSongsToPlaylist("123", "1", "")
        verify(failedToAddSongEventObserver).onChanged(null)
    }

    @Test
    fun `click on new playlist observed`() {
        viewModel.didTapNew()
        verify(newPlaylistEventObserver).onChanged(null)
    }
}
