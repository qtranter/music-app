package com.audiomack.data.music.local

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.TestApplication
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.common.Permission.Storage
import com.audiomack.ui.common.PermissionHandler
import com.audiomack.ui.home.AlertTriggers
import com.audiomack.ui.home.NavigationActions
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    application = TestApplication::class
)
class OpenLocalMediaUseCaseImplTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var activityResultRegistry: ActivityResultRegistry

    @Mock
    private lateinit var localMediaRepo: LocalMediaDataSource

    @Mock
    private lateinit var storagePermissions: PermissionHandler<Storage>

    @Mock
    private lateinit var mixPanelRepo: MixpanelDataSource

    @Mock
    private lateinit var mimeTypeHelper: MimeTypeHelper

    @Mock
    private lateinit var alerts: AlertTriggers

    @Mock
    private lateinit var navigation: NavigationActions

    @Mock
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private val schedulers: SchedulersProvider = TestSchedulersProvider()

    private lateinit var useCase: OpenLocalMediaUseCaseImpl

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        whenever(
            activityResultRegistry.register(
                any(),
                any<ActivityResultContract<String, Boolean>>(),
                any()
            )
        ).thenReturn(permissionLauncher)

        useCase = OpenLocalMediaUseCaseImpl(
            activityResultRegistry,
            localMediaRepo,
            schedulers,
            storagePermissions,
            mixPanelRepo,
            mimeTypeHelper,
            navigation,
            alerts
        )
    }

    @After
    fun tearDown() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `given a content uri with audio mime type, when open is called with storage permission, then player is launched`() {
        whenever(storagePermissions.hasPermission).thenReturn(true)

        val id = "1234"

        val item = mock<AMResultItem> {
            on { itemId } doReturn id
        }
        whenever(localMediaRepo.getTrack(id.toLong())).thenReturn(Single.just(item))

        val uri = mock<Uri> {
            on { scheme } doReturn ContentResolver.SCHEME_CONTENT
            on { authority } doReturn MediaStore.AUTHORITY
            on { lastPathSegment } doReturn id
        }
        val mimeType: MimeType = "audio/mpeg"

        useCase.open(uri, mimeType)

        verify(navigation, times(1)).launchPlayer(argWhere { it.item == item })
        verify(alerts, never()).onGenericError()
        verify(alerts, never()).onPlayUnsupportedFileAttempt(any())
    }

    @Test
    fun `given a content uri with audio mime type, when open is called without storage permission, permission is requested`() {
        whenever(storagePermissions.hasPermission).thenReturn(false)

        val id = "1234"

        val item = mock<AMResultItem> {
            on { itemId } doReturn id
        }
        whenever(localMediaRepo.getTrack(id.toLong())).thenReturn(Single.just(item))

        val uri = mock<Uri> {
            on { scheme } doReturn ContentResolver.SCHEME_CONTENT
            on { authority } doReturn MediaStore.AUTHORITY
            on { lastPathSegment } doReturn id
        }
        val mimeType: MimeType = "audio/mpeg"

        useCase.open(uri, mimeType)

        verify(permissionLauncher, times(1)).launch(Storage.key)
        verify(navigation, never()).launchPlayer(any())
        verify(alerts, never()).onPlayUnsupportedFileAttempt(any())
    }

    @Test
    fun `given a mp3 file uri with audio mime type, when open is called with storage permission and the file is part of the media store, then player is launched`() {
        val id = "1234"
        val filePath = "song.mp3"
        val uri = mock<Uri> {
            on { scheme } doReturn ContentResolver.SCHEME_FILE
            on { path } doReturn filePath
        }
        val item = mock<AMResultItem> {
            on { itemId } doReturn id
        }

        whenever(storagePermissions.hasPermission).thenReturn(true)
        whenever(localMediaRepo.findIdByPath(filePath)).thenReturn(Maybe.just(id.toLong()))
        whenever(localMediaRepo.getTrack(id.toLong())).thenReturn(Single.just(item))

        useCase.open(uri, "audio/mpeg")

        verify(alerts, never()).onGenericError()
        verify(alerts, never()).onPlayUnsupportedFileAttempt(any())
        verify(navigation, times(1)).launchPlayer(argWhere { it.item?.itemId == id })
    }

    @Test
    fun `given a mp3 file uri without mime type, when open is called with storage permission and the file is not part of the media store, then player is launched`() {
        val id = "1234"
        val filePath = "song.mp3"
        val uri = mock<Uri> {
            on { scheme } doReturn ContentResolver.SCHEME_FILE
            on { path } doReturn filePath
            on { encodedPath } doReturn filePath
        }
        val item = mock<AMResultItem> {
            on { itemId } doReturn id
        }

        whenever(mimeTypeHelper.getMimeTypeFromUrl(any())).thenReturn("audio/mpeg")
        whenever(storagePermissions.hasPermission).thenReturn(true)
        whenever(localMediaRepo.findIdByPath(any())).thenReturn(Maybe.empty())
        whenever(localMediaRepo.getTrack(any())).thenReturn(Single.just(item))

        useCase.open(uri)

        verify(alerts, never()).onGenericError()
        verify(alerts, never()).onPlayUnsupportedFileAttempt(any())
        verify(navigation, times(1)).launchPlayer(argWhere { it.item?.itemId == filePath })
    }

    @Test
    fun `given a uri with a non-audio mime type, when open is called with storage permission, error is thrown`() {
        useCase.open(mock(), "video/mpeg")

        verify(navigation, never()).launchPlayer(any())
        verify(alerts, times(1)).onPlayUnsupportedFileAttempt(any())
    }
}
