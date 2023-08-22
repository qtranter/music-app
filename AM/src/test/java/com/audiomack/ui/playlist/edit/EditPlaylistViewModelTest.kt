package com.audiomack.ui.playlist.edit

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.playlist.PlayListDataSource
import com.audiomack.data.tracking.appsflyer.AppsFlyerDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.model.AMGenre
import com.audiomack.model.AMResultItem
import com.audiomack.model.Action
import com.audiomack.model.AddToPlaylistModel
import com.audiomack.model.MixpanelSource
import com.audiomack.model.Music
import com.audiomack.model.PermissionType
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.common.GenreProvider
import com.audiomack.ui.playlist.edit.EditPlaylistException.Type
import com.audiomack.ui.playlist.edit.EditPlaylistMode.CREATE
import com.audiomack.ui.playlist.edit.EditPlaylistMode.EDIT
import com.audiomack.usecases.SaveImageUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import java.io.File
import java.io.InputStream
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class EditPlaylistViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    lateinit var playListDataSource: PlayListDataSource

    @Mock
    lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    lateinit var appsFlyerDataSource: AppsFlyerDataSource

    @Mock
    lateinit var playlistItemProvider: EditPlaylistItemProvider

    @Mock
    lateinit var playlistImageProvider: EditPlaylistImageProvider

    @Mock
    lateinit var viewStateProvider: EditPlaylistViewStateProvider

    @Mock
    lateinit var genreProvider: GenreProvider

    private val eventBus: EventBus = EventBus.getDefault()

    private lateinit var viewModel: EditPlaylistViewModel

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        viewModel = EditPlaylistViewModel(
            playListDataSource,
            mixpanelDataSource,
            appsFlyerDataSource,
            TestSchedulersProvider(),
            playlistItemProvider,
            playlistImageProvider,
            eventBus
        )

        whenever(genreProvider.getHumanValue(any())).thenReturn(GENRE_RAP_HUMAN)
        whenever(genreProvider.getApiValue(any())).thenReturn(GENRE_RAP_API)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `edit mode init success`() {
        val playlist = mock<AMResultItem> {
            on { title } doReturn PLAYLIST_TITLE
            on { genre } doReturn AMGenre.Rap.apiValue()
            on { desc } doReturn PLAYLIST_DESC
            on { banner } doReturn BANNER_FILE_PATH
            on { getImageURLWithPreset(any()) } doReturn IMAGE_FILE_PATH
            on { isPrivatePlaylist } doReturn false
        }
        whenever(playlistItemProvider.playlist).thenReturn(playlist)

        val modeObserver: Observer<EditPlaylistMode> = mock()
        val titleObserver: Observer<String> = mock()
        val genreObserver: Observer<String> = mock()
        val descObserver: Observer<String> = mock()
        val bannerObserver: Observer<String> = mock()
        val smallImageObserver: Observer<String> = mock()
        val privateObserver: Observer<Boolean> = mock()

        viewModel.mode.observeForever(modeObserver)
        viewModel.title.observeForever(titleObserver)
        viewModel.genre.observeForever(genreObserver)
        viewModel.description.observeForever(descObserver)
        viewModel.banner.observeForever(bannerObserver)
        viewModel.smallImage.observeForever(smallImageObserver)
        viewModel.private.observeForever(privateObserver)

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)

        verify(modeObserver).onChanged(EDIT)
        verify(titleObserver).onChanged(playlist.title)
        verify(genreObserver).onChanged(genreProvider.getHumanValue(playlist.genre))
        verify(descObserver).onChanged(playlist.desc)
        verify(bannerObserver).onChanged(BANNER_FILE_PATH)
        verify(smallImageObserver).onChanged(IMAGE_FILE_PATH)
        verify(privateObserver).onChanged(false)
    }

    @Test
    fun `create mode init success`() {
        val data = AddToPlaylistModel(
            listOf(),
            AMGenre.Electronic.apiValue(),
            IMAGE_FILE_PATH,
            MixpanelSource.empty,
            ""
        )

        val modeObserver: Observer<EditPlaylistMode> = mock()
        val genreObserver: Observer<String> = mock()
        val smallImageObserver: Observer<String> = mock()
        val privateObserver: Observer<Boolean> = mock()

        viewModel.mode.observeForever(modeObserver)
        viewModel.genre.observeForever(genreObserver)
        viewModel.smallImage.observeForever(smallImageObserver)
        viewModel.private.observeForever(privateObserver)

        viewModel.init(CREATE, data, viewStateProvider, genreProvider)

        verify(modeObserver).onChanged(CREATE)
        verify(genreObserver).onChanged(genreProvider.getHumanValue(AMGenre.Electronic.apiValue()))
        verify(smallImageObserver).onChanged(IMAGE_FILE_PATH)
        verify(privateObserver).onChanged(false)
    }

    @Test(expected = IllegalStateException::class)
    fun `init create mode fails because no songs`() {
        viewModel.init(CREATE, null, viewStateProvider, genreProvider)
    }

    @Test(expected = IllegalStateException::class)
    fun `init edit mode fails because no playlist`() {
        whenever(playlistItemProvider.playlist).thenReturn(null)
        viewModel.init(EDIT, null, viewStateProvider, genreProvider)
    }

    @Test
    fun `back press handled`() {
        val backObserver: Observer<Void> = mock()
        viewModel.backEvent.observeForever(backObserver)
        viewModel.onBackClick()
        verify(backObserver).onChanged(null)
    }

    @Test
    fun `content change handled`() {
        val observer: Observer<Void> = mock()
        viewModel.changeEvent.observeForever(observer)
        viewModel.onContentChange()
        verify(observer).onChanged(null)
    }

    @Test
    fun `confirmation shown on delete click`() {
        val playlist = mock<AMResultItem> {
            on { title } doReturn PLAYLIST_TITLE
        }
        whenever(playlistItemProvider.playlist).thenReturn(playlist)
        viewModel.init(EDIT, null, viewStateProvider, genreProvider)

        val observer: Observer<String> = mock()
        viewModel.deletePromptEvent.observeForever(observer)
        viewModel.onDeleteClick()
        verify(observer).onChanged(playlist.title)
    }

    @Test
    fun `delete playlist success`() {
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn PLAYLIST_ID
        }
        whenever(playlistItemProvider.playlist).thenReturn(playlist)
        whenever(playListDataSource.deletePlaylist(any())).thenReturn(Observable.just(true))

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)

        val progressObserver: Observer<Boolean> = mock()
        val deletedObserver: Observer<AMResultItem> = mock()
        val finishObserver: Observer<Void> = mock()
        val errorObserver: Observer<EditPlaylistException> = mock()

        viewModel.progressEvent.observeForever(progressObserver)
        viewModel.deletedEvent.observeForever(deletedObserver)
        viewModel.finishEvent.observeForever(finishObserver)
        viewModel.errorEvent.observeForever(errorObserver)

        viewModel.onDeleteConfirmed()

        verify(progressObserver).onChanged(true)
        verify(playListDataSource).deletePlaylist(playlist.itemId)
        verify(deletedObserver).onChanged(playlist)
        verify(finishObserver).onChanged(null)
        verify(progressObserver).onChanged(false)

        verifyZeroInteractions(errorObserver)
    }

    @Test
    fun `runtime error handled when deleting playlist`() {
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn PLAYLIST_ID
        }
        whenever(playlistItemProvider.playlist).thenReturn(playlist)
        whenever(playListDataSource.deletePlaylist(any())).thenReturn(Observable.error(Exception()))

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)

        val progressObserver: Observer<Boolean> = mock()
        val deletedObserver: Observer<AMResultItem> = mock()
        val finishObserver: Observer<Void> = mock()
        val errorObserver: Observer<EditPlaylistException> = mock()

        viewModel.progressEvent.observeForever(progressObserver)
        viewModel.deletedEvent.observeForever(deletedObserver)
        viewModel.finishEvent.observeForever(finishObserver)
        viewModel.errorEvent.observeForever(errorObserver)

        viewModel.onDeleteConfirmed()

        verify(progressObserver).onChanged(true)
        verify(playListDataSource).deletePlaylist(playlist.itemId)

        val captor = ArgumentCaptor.forClass(EditPlaylistException::class.java)
        verify(errorObserver).onChanged(captor.capture())
        assertEquals(captor.value.type, Type.DELETE)

        verify(progressObserver).onChanged(false)
        verify(finishObserver).onChanged(null)

        verifyZeroInteractions(deletedObserver)
    }

    @Test
    fun `options shown when genre clicked`() {
        val playlist: AMResultItem = mock()
        whenever(playlistItemProvider.playlist).thenReturn(playlist)

        val hideKeyboardObserver: Observer<Void> = mock()
        val showOptionsObserver: Observer<List<Action>> = mock()

        viewModel.hideKeyboardEvent.observeForever(hideKeyboardObserver)
        viewModel.showOptionsEvent.observeForever(showOptionsObserver)

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)
        viewModel.onGenreClick()

        verify(hideKeyboardObserver).onChanged(null)
        verify(showOptionsObserver).onChanged(any())
    }

    @Test
    fun `genre action selected from options`() {
        val genre = PLAYLIST_GENRE

        val genreObserver: Observer<String> = mock()
        val popBackStackObserver: Observer<Void> = mock()
        val changeObserver: Observer<Void> = mock()

        viewModel.genre.observeForever(genreObserver)
        viewModel.popBackStackEvent.observeForever(popBackStackObserver)
        viewModel.changeEvent.observeForever(changeObserver)

        viewModel.onGenreSelected(genre)

        verify(genreObserver).onChanged(genre)
        verify(changeObserver).onChanged(null)
        verify(popBackStackObserver).onChanged(null)
    }

    @Test
    fun `title change observed`() {
        val playlist = mock<AMResultItem> {
            on { title } doReturn PLAYLIST_TITLE
        }
        whenever(playlistItemProvider.playlist).thenReturn(playlist)

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)

        val changeObserver: Observer<Void> = mock()
        viewModel.changeEvent.observeForever(changeObserver)

        viewModel.onTitleChange("New Title")

        verify(changeObserver).onChanged(null)
    }

    @Test
    fun `description change observed`() {
        val playlist = mock<AMResultItem> {
            on { desc } doReturn PLAYLIST_DESC
        }
        whenever(playlistItemProvider.playlist).thenReturn(playlist)

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)

        val changeObserver: Observer<Void> = mock()
        viewModel.changeEvent.observeForever(changeObserver)

        viewModel.onDescriptionChange("New description")

        verify(changeObserver).onChanged(null)
    }

    @Test
    fun `privacy toggle observed`() {
        val playlist = mock<AMResultItem> {
            on { isPrivatePlaylist } doReturn true
        }
        whenever(playlistItemProvider.playlist).thenReturn(playlist)

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)

        val privateObserver: Observer<Boolean> = mock()
        val changeObserver: Observer<Void> = mock()

        viewModel.private.observeForever(privateObserver)
        viewModel.changeEvent.observeForever(changeObserver)

        viewModel.onPermissionsClick()

        verify(privateObserver).onChanged(false)
        verify(changeObserver).onChanged(null)
    }

    @Test
    fun `save click fails because view is unavailable`() {
        whenever(viewStateProvider.isViewAvailable()).thenReturn(false)

        val playlist: AMResultItem = mock()
        whenever(playlistItemProvider.playlist).thenReturn(playlist)

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)

        val saveBannerObserver: Observer<File> = mock()
        viewModel.saveBannerEvent.observeForever(saveBannerObserver)

        viewModel.onSaveClick()

        verifyZeroInteractions(saveBannerObserver)
        verifyZeroInteractions(playListDataSource)
    }

    @Test
    fun `banner saved on save click`() {
        whenever(viewStateProvider.isViewAvailable()).thenReturn(true)
        whenever(viewStateProvider.isBannerVisible()).thenReturn(false)
        whenever(playlistImageProvider.bannerFile).thenReturn(File(BANNER_FILE_PATH))

        val playlist: AMResultItem = mock()
        whenever(playlistItemProvider.playlist).thenReturn(playlist)

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)

        val saveBannerObserver: Observer<File> = mock()
        viewModel.saveBannerEvent.observeForever(saveBannerObserver)

        viewModel.onSaveClick()

        verify(saveBannerObserver).onChanged(File(BANNER_FILE_PATH))
        verifyZeroInteractions(playListDataSource)
    }

    @Test
    fun `banner save error handled`() {
        val errorObserver: Observer<EditPlaylistException> = mock()
        viewModel.errorEvent.observeForever(errorObserver)

        viewModel.onBannerSaveError()

        val captor = ArgumentCaptor.forClass(EditPlaylistException::class.java)
        verify(errorObserver).onChanged(captor.capture())
        assertEquals(captor.value.type, Type.BANNER)
    }

    @Test
    fun `create playlist with empty title not allowed`() {
        val data = AddToPlaylistModel(
            listOf(),
            AMGenre.Electronic.apiValue(),
            "",
            MixpanelSource.empty,
            ""
        )

        whenever(viewStateProvider.isViewAvailable()).thenReturn(true)
        whenever(viewStateProvider.isBannerVisible()).thenReturn(true)

        val emptyTitle = ""
        whenever(viewStateProvider.getTitle()).thenReturn(emptyTitle)

        val errorObserver: Observer<EditPlaylistException> = mock()
        viewModel.errorEvent.observeForever(errorObserver)

        viewModel.init(CREATE, data, viewStateProvider, genreProvider)
        viewModel.onSaveClick()

        val captor = ArgumentCaptor.forClass(EditPlaylistException::class.java)
        verify(errorObserver).onChanged(captor.capture())
        assertEquals(captor.value.type, Type.TITLE)
    }

    @Test
    fun `runtime error handled when creating playlist`() {
        whenever(viewStateProvider.isViewAvailable()).thenReturn(true)
        whenever(viewStateProvider.isBannerVisible()).thenReturn(true)
        whenever(viewStateProvider.getTitle()).thenReturn(PLAYLIST_TITLE)
        whenever(viewStateProvider.getGenre()).thenReturn(PLAYLIST_GENRE)
        whenever(viewStateProvider.getDesc()).thenReturn(PLAYLIST_DESC)

        val songItemId = TRACK_ID
        val data = AddToPlaylistModel(
            listOf(Music(id = songItemId)),
            AMGenre.Electronic.apiValue(),
            "",
            MixpanelSource.empty,
            ""
        )

        val exception = Exception()
        whenever(
            playListDataSource.createPlaylist(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                any()
            )
        ).thenReturn(Observable.error(exception))

        val errorObserver: Observer<EditPlaylistException> = mock()
        val progressObserver: Observer<Boolean> = mock()

        viewModel.errorEvent.observeForever(errorObserver)
        viewModel.progressEvent.observeForever(progressObserver)

        viewModel.init(CREATE, data, viewStateProvider, genreProvider)
        viewModel.onSaveClick()

        verify(progressObserver).onChanged(true)

        val captor = ArgumentCaptor.forClass(EditPlaylistException::class.java)
        verify(errorObserver).onChanged(captor.capture())
        assertEquals(captor.value.type, Type.CREATE)
        assertEquals(captor.value.throwable, exception)

        verify(progressObserver).onChanged(false)
    }

    @Test
    fun `playlist created successfully`() {
        val playlistTitle = PLAYLIST_TITLE
        val playlistGenre = AMGenre.Rap.apiValue()
        val playlistDesc = PLAYLIST_DESC
        val playlistPrivate = false

        val playlist = mock<AMResultItem> {
            on { title } doReturn playlistTitle
            on { genre } doReturn playlistGenre
            on { desc } doReturn playlistDesc
            on { isPrivatePlaylist } doReturn playlistPrivate
        }

        val songItemId = TRACK_ID
        val data = AddToPlaylistModel(
            listOf(Music(id = songItemId)),
            playlistGenre,
            "",
            MixpanelSource.empty,
            ""
        )

        whenever(viewStateProvider.isViewAvailable()).thenReturn(true)
        whenever(viewStateProvider.isBannerVisible()).thenReturn(true)
        whenever(viewStateProvider.getTitle()).thenReturn(playlistTitle)
        whenever(viewStateProvider.getGenre()).thenReturn(playlistGenre)
        whenever(viewStateProvider.getDesc()).thenReturn(playlistDesc)

        whenever(
            playListDataSource.createPlaylist(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                any()
            )
        ).thenReturn(Observable.just(playlist))

        viewModel.init(CREATE, data, viewStateProvider, genreProvider)

        val createdObserver: Observer<AMResultItem> = mock()
        val progressObserver: Observer<Boolean> = mock()
        val finishObserver: Observer<Void> = mock()
        val errorObserver: Observer<EditPlaylistException> = mock()
        val hideKeyboardObserver: Observer<Void> = mock()

        viewModel.createdEvent.observeForever(createdObserver)
        viewModel.progressEvent.observeForever(progressObserver)
        viewModel.finishEvent.observeForever(finishObserver)
        viewModel.errorEvent.observeForever(errorObserver)
        viewModel.hideKeyboardEvent.observeForever(hideKeyboardObserver)

        viewModel.onSaveClick()

        verify(progressObserver).onChanged(true)
        verify(hideKeyboardObserver).onChanged(null)
        verify(playListDataSource).createPlaylist(
            playlistTitle,
            genreProvider.getApiValue(playlistGenre),
            playlistDesc,
            playlistPrivate,
            songItemId,
            null,
            null,
            ""
        )
        verify(mixpanelDataSource).trackCreatePlaylist(playlist)
        verify(appsFlyerDataSource).trackCreatePlaylist()
        verify(createdObserver).onChanged(playlist)
        verify(finishObserver).onChanged(null)
        verify(progressObserver).onChanged(false)

        verifyZeroInteractions(errorObserver)
    }

    @Test
    fun `edit playlist with empty title not allowed`() {
        whenever(viewStateProvider.isViewAvailable()).thenReturn(true)
        whenever(viewStateProvider.isBannerVisible()).thenReturn(true)

        val playlist: AMResultItem = mock()
        whenever(playlistItemProvider.playlist).thenReturn(playlist)

        val emptyTitle = ""
        whenever(viewStateProvider.getTitle()).thenReturn(emptyTitle)

        val errorObserver: Observer<EditPlaylistException> = mock()
        viewModel.errorEvent.observeForever(errorObserver)

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)
        viewModel.onSaveClick()

        val captor = ArgumentCaptor.forClass(EditPlaylistException::class.java)
        verify(errorObserver).onChanged(captor.capture())
        assertEquals(captor.value.type, Type.TITLE)
    }

    @Test
    fun `runtime error handled when editing playlist`() {
        val playlistId = PLAYLIST_ID
        val playlistTitle = PLAYLIST_TITLE
        val playlistGenre = AMGenre.Rap.apiValue()
        val playlistDesc = PLAYLIST_DESC
        val playlistPrivate = true

        whenever(viewStateProvider.isViewAvailable()).thenReturn(true)
        whenever(viewStateProvider.isBannerVisible()).thenReturn(true)
        whenever(viewStateProvider.getTitle()).thenReturn(playlistTitle)
        whenever(viewStateProvider.getGenre()).thenReturn(playlistGenre)
        whenever(viewStateProvider.getDesc()).thenReturn(playlistDesc)

        val playlist = mock<AMResultItem> {
            on { itemId } doReturn playlistId
            on { title } doReturn playlistTitle
            on { genre } doReturn playlistGenre
            on { desc } doReturn playlistDesc
            on { isPrivatePlaylist } doReturn playlistPrivate
            on { trackIDs } doReturn ""
        }
        whenever(playlistItemProvider.playlist).thenReturn(playlist)

        val exception = Exception()
        whenever(
            playListDataSource.editPlaylist(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Observable.error(exception))

        val errorObserver: Observer<EditPlaylistException> = mock()
        val progressObserver: Observer<Boolean> = mock()

        viewModel.errorEvent.observeForever(errorObserver)
        viewModel.progressEvent.observeForever(progressObserver)

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)
        viewModel.onSaveClick()

        verify(progressObserver).onChanged(true)

        val captor = ArgumentCaptor.forClass(EditPlaylistException::class.java)
        verify(errorObserver).onChanged(captor.capture())
        assertEquals(captor.value.type, Type.EDIT)
        assertEquals(captor.value.throwable, exception)

        verify(progressObserver).onChanged(false)
    }

    @Test
    fun `playlist edited successfully`() {
        val playlistId = PLAYLIST_ID
        val playlistTitle = PLAYLIST_TITLE
        val playlistGenre = AMGenre.Rap.apiValue()
        val playlistDesc = PLAYLIST_DESC
        val playlistPrivate = true
        val trackId = TRACK_ID
        val playlist = mock<AMResultItem> {
            on { itemId } doReturn playlistId
            on { title } doReturn playlistTitle
            on { genre } doReturn playlistGenre
            on { desc } doReturn playlistDesc
            on { isPrivatePlaylist } doReturn playlistPrivate
            on { trackIDs } doReturn trackId
        }

        whenever(playlistItemProvider.playlist).thenReturn(playlist)
        whenever(viewStateProvider.isViewAvailable()).thenReturn(true)
        whenever(viewStateProvider.isBannerVisible()).thenReturn(true)
        whenever(viewStateProvider.getTitle()).thenReturn(playlistTitle)
        whenever(viewStateProvider.getGenre()).thenReturn(playlistGenre)
        whenever(viewStateProvider.getDesc()).thenReturn(playlistDesc)

        whenever(
            playListDataSource.editPlaylist(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Observable.just(playlist))

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)

        val editedObserver: Observer<AMResultItem> = mock()
        val progressObserver: Observer<Boolean> = mock()
        val finishObserver: Observer<Void> = mock()
        val errorObserver: Observer<EditPlaylistException> = mock()
        val hideKeyboardObserver: Observer<Void> = mock()

        viewModel.editedEvent.observeForever(editedObserver)
        viewModel.progressEvent.observeForever(progressObserver)
        viewModel.finishEvent.observeForever(finishObserver)
        viewModel.errorEvent.observeForever(errorObserver)
        viewModel.hideKeyboardEvent.observeForever(hideKeyboardObserver)

        viewModel.onSaveClick()

        verify(progressObserver).onChanged(true)
        verify(hideKeyboardObserver).onChanged(null)
        verify(playListDataSource).editPlaylist(
            playlistId,
            playlistTitle,
            genreProvider.getApiValue(playlistGenre),
            playlistDesc,
            playlistPrivate,
            trackId,
            null,
            null
        )
        verify(editedObserver).onChanged(playlist)
        verify(finishObserver).onChanged(null)
        verify(progressObserver).onChanged(false)

        verifyZeroInteractions(errorObserver)
    }

    @Test
    fun `playlist banner Base64 string when banner is created`() {
        val data = AddToPlaylistModel(
            listOf(),
            AMGenre.Electronic.apiValue(),
            "",
            MixpanelSource.empty,
            ""
        )

        val imageStream: InputStream = mock()
        val encodedString = "Base64 String"

        whenever(playlistImageProvider.inputStreamToBase64(imageStream)).thenReturn(encodedString)

        val bannerObserver: Observer<String> = mock()
        val progressObserver: Observer<Boolean> = mock()

        viewModel.bannerImageBase64.observeForever(bannerObserver)
        viewModel.progressEvent.observeForever(progressObserver)

        viewModel.init(CREATE, data, viewStateProvider, genreProvider)
        viewModel.onBannerImageCreated(imageStream)

        verify(progressObserver).onChanged(true)
        verify(bannerObserver).onChanged(encodedString)
        verify(progressObserver).onChanged(false)
    }

    @Test
    fun `playlist banner Base64 string when banner is created, failure`() {
        val data = AddToPlaylistModel(
            listOf(),
            AMGenre.Electronic.apiValue(),
            "",
            MixpanelSource.empty,
            ""
        )

        val imageStream: InputStream = mock()

        whenever(playlistImageProvider.inputStreamToBase64(imageStream)).thenThrow(RuntimeException("Unknown error for tests"))

        val errorObserver: Observer<EditPlaylistException> = mock()
        val progressObserver: Observer<Boolean> = mock()

        viewModel.errorEvent.observeForever(errorObserver)
        viewModel.progressEvent.observeForever(progressObserver)

        viewModel.init(CREATE, data, viewStateProvider, genreProvider)
        viewModel.onBannerImageCreated(imageStream)

        verify(progressObserver).onChanged(true)
        verify(errorObserver).onChanged(argWhere { it.type == Type.BANNER })
        verify(progressObserver).onChanged(false)
    }

    @Test
    fun `playlist image Base64 string saved when image is created`() {
        val data = AddToPlaylistModel(
            listOf(),
            AMGenre.Electronic.apiValue(),
            "",
            MixpanelSource.empty,
            ""
        )

        val file = File(IMAGE_FILE_PATH)
        val encodedString = "Base64 String"

        whenever(playlistImageProvider.imageFile).thenReturn(file)
        whenever(playlistImageProvider.fileToBase64(file)).thenReturn(encodedString)

        val imageSavedObserver: Observer<File> = mock()
        val imageBase64Observer: Observer<String> = mock()
        val progressObserver: Observer<Boolean> = mock()

        viewModel.imageSavedEvent.observeForever(imageSavedObserver)
        viewModel.imageBase64.observeForever(imageBase64Observer)
        viewModel.progressEvent.observeForever(progressObserver)

        viewModel.init(CREATE, data, viewStateProvider, genreProvider)
        viewModel.onPlaylistImageCreated()

        verify(progressObserver).onChanged(true)
        verify(imageSavedObserver).onChanged(file)
        verify(imageBase64Observer).onChanged(encodedString)
        verify(progressObserver).onChanged(false)
    }

    @Test
    fun `image file deleted when image clicked`() {
        val file: File = mock()
        whenever(playlistImageProvider.imageFile).thenReturn(file)
        whenever(file.delete()).thenReturn(true)

        val playlist: AMResultItem = mock()
        whenever(playlistItemProvider.playlist).thenReturn(playlist)

        val keyboardObserver: Observer<Void> = mock()
        val editImageObserver: Observer<Void> = mock()
        val changeObserver: Observer<Void> = mock()

        viewModel.hideKeyboardEvent.observeForever(keyboardObserver)
        viewModel.editImageEvent.observeForever(editImageObserver)
        viewModel.changeEvent.observeForever(changeObserver)

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)
        viewModel.onEditImageClick()

        verify(keyboardObserver).onChanged(null)
        verify(editImageObserver).onChanged(null)
        verify(changeObserver).onChanged(null)
    }

    @Test
    fun `banner file deleted when banner clicked`() {
        val file: File = mock()
        whenever(playlistImageProvider.bannerFile).thenReturn(file)
        whenever(file.delete()).thenReturn(true)

        val playlist: AMResultItem = mock()
        whenever(playlistItemProvider.playlist).thenReturn(playlist)

        val keyboardObserver: Observer<Void> = mock()
        val editBannerObserver: Observer<Void> = mock()
        val changeObserver: Observer<Void> = mock()

        viewModel.hideKeyboardEvent.observeForever(keyboardObserver)
        viewModel.editBannerEvent.observeForever(editBannerObserver)
        viewModel.changeEvent.observeForever(changeObserver)

        viewModel.init(EDIT, null, viewStateProvider, genreProvider)
        viewModel.onEditBannerClick()

        verify(keyboardObserver).onChanged(null)
        verify(editBannerObserver).onChanged(null)
        verify(changeObserver).onChanged(null)
    }

    @Test
    fun `after saving banner image to file, load banner image`() {
        val bannerFile = mock<File>()
        val saveImageUseCase = mock<SaveImageUseCase>()

        whenever(viewModel.bannerFile).thenReturn(bannerFile)
        whenever(saveImageUseCase.copyInputStreamToFile(any(), any())).thenReturn(1L)

        val observerOnShowBannerEvent: Observer<Void> = mock()
        viewModel.showBannerEvent.observeForever(observerOnShowBannerEvent)

        viewModel.saveGalleryImage(saveImageUseCase, mock(), bannerFile)

        verify(observerOnShowBannerEvent).onChanged(null)
    }

    @Test
    fun `after saving image to file, crop image`() {
        val imageFile = mock<File>()
        val saveImageUseCase = mock<SaveImageUseCase>()

        whenever(viewModel.imageFile).thenReturn(imageFile)
        whenever(saveImageUseCase.copyInputStreamToFile(any(), any())).thenReturn(1L)

        val observerOnCropImageEvent: Observer<Void> = mock()
        viewModel.cropImageEvent.observeForever(observerOnCropImageEvent)

        viewModel.saveGalleryImage(saveImageUseCase, mock(), imageFile)

        verify(observerOnCropImageEvent).onChanged(null)
    }

    @Test
    fun `on camera permission requested`() {
        viewModel.onPermissionRequested(PermissionType.Camera)
        verify(mixpanelDataSource).trackPromptPermissions(argWhere { it == PermissionType.Camera })
    }

    @Test
    fun `on storage permission requested`() {
        viewModel.onPermissionRequested(PermissionType.Storage)
        verify(mixpanelDataSource).trackPromptPermissions(argWhere { it == PermissionType.Storage })
    }

    companion object {
        const val BANNER_FILE_PATH = "banner file"
        const val IMAGE_FILE_PATH = "image file"
        const val PLAYLIST_TITLE = "Playlist Title"
        const val PLAYLIST_DESC = "Playlist description"
        const val PLAYLIST_ID = "Playlist ID"
        const val PLAYLIST_GENRE = "Playlist Genre"
        const val TRACK_ID = "Song ID"
        const val GENRE_RAP_HUMAN = "Hip-Hop/Rap"
        const val GENRE_RAP_API = "rap"
    }
}
