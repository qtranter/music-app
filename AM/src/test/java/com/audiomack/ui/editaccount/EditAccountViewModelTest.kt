package com.audiomack.ui.editaccount

import android.content.Context
import android.widget.EditText
import android.widget.ImageView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.socialauth.SocialAuthManager
import com.audiomack.data.socialauth.TwitterAuthData
import com.audiomack.data.sociallink.SocialLinkDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.AccountImages
import com.audiomack.data.user.AccountSaveException
import com.audiomack.data.user.UserDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.PermissionType
import com.audiomack.model.SocialNetwork
import com.audiomack.network.LinkSocialException
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.audiomack.ui.common.Resource
import com.audiomack.ui.webviewauth.WebViewAuthResult
import com.audiomack.usecases.SaveImageUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.steelkiwi.cropiwa.CropIwaView
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.MockitoAnnotations

class EditAccountViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var accountImages: AccountImages

    @Mock
    private lateinit var imageLoader: ImageLoader

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var socialAuthManager: SocialAuthManager

    @Mock
    private lateinit var socialLinkDataSource: SocialLinkDataSource

    private lateinit var schedulersProvider: SchedulersProvider

    private lateinit var viewModel: EditAccountViewModel

    // Observers
    @Mock
    private lateinit var observerShowLoaderEvent: Observer<Void>
    @Mock
    private lateinit var observerHideLoaderEvent: Observer<Void>
    @Mock
    private lateinit var observerShowGenericErrorEvent: Observer<Void>
    @Mock
    private lateinit var observerShowInstagramWebViewEvent: Observer<Void>
    @Mock
    private lateinit var observerShowAlreadyLinkedEvent: Observer<SocialNetwork>
    @Mock
    private lateinit var observerText: Observer<EditAccountViewModel.TextData>
    @Mock
    private lateinit var observerHideKeyboardEvent: Observer<Void>
    @Mock
    private lateinit var observerShowFilePickerTypeAlertEvent: Observer<Void>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        whenever(accountImages.avatarFile).thenReturn(mock())
        whenever(accountImages.bannerFile).thenReturn(mock())
        viewModel = EditAccountViewModel(
            userDataSource,
            accountImages,
            imageLoader,
            socialAuthManager,
            socialLinkDataSource,
            mixpanelDataSource,
            schedulersProvider
        ).apply {
            showLoaderEvent.observeForever(observerShowLoaderEvent)
            hideLoaderEvent.observeForever(observerHideLoaderEvent)
            showGenericErrorEvent.observeForever(observerShowGenericErrorEvent)
            showInstagramWebViewEvent.observeForever(observerShowInstagramWebViewEvent)
            showAlreadyLinkedEvent.observeForever(observerShowAlreadyLinkedEvent)
            text.observeForever(observerText)
            hideKeyboardEvent.observeForever(observerHideKeyboardEvent)
            showFilePickerTypeAlertEvent.observeForever(observerShowFilePickerTypeAlertEvent)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun close() {
        val observerClose: Observer<Void> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onCloseTapped()
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `logged out`() {
        `when`(userDataSource.getUser()).thenReturn(null)
        val observerClose: Observer<Void> = mock()
        val observerUpdateUserData: Observer<AMArtist> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.artist.observeForever(observerUpdateUserData)
        viewModel.onCreate()
        verify(observerClose).onChanged(null)
        verifyZeroInteractions(observerUpdateUserData)
    }

    @Test
    fun `logged in`() {
        val artist = mock<AMArtist> {
            on { name } doReturn "Matteo"
        }
        `when`(userDataSource.getUser()).thenReturn(artist)
        val observerClose: Observer<Void> = mock()
        val observerUpdateUserData: Observer<AMArtist> = mock()
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.artist.observeForever(observerUpdateUserData)
        viewModel.onCreate()
        verifyZeroInteractions(observerClose)
        verify(observerUpdateUserData).onChanged(artist)
    }

    @Test
    fun `onLoadBannerCropView calls image loader`() {
        val context = mock<Context>()
        val imageUrl = "image"
        val imageView = mock<CropIwaView>()
        viewModel.onLoadBannerCropView(context, imageUrl, imageView)
        verify(imageLoader).load(context, imageUrl, imageView)
    }

    @Test
    fun `onLoadAvatarImageView calls image loader`() {
        val context = mock<Context>()
        val imageUrl = "image"
        val imageView = mock<ImageView>()
        viewModel.onLoadAvatarImageView(context, imageUrl, imageView)
        verify(imageLoader).load(context, imageUrl, imageView)
    }

    @Test
    fun `onEditBannerTapped shows alert`() {
        viewModel.onEditBannerTapped()
        verify(observerHideKeyboardEvent).onChanged(null)
        verify(observerShowFilePickerTypeAlertEvent).onChanged(null)
    }

    @Test
    fun `onEditAvatarTapped shows alert`() {
        viewModel.onEditAvatarTapped()
        verify(observerHideKeyboardEvent).onChanged(null)
        verify(observerShowFilePickerTypeAlertEvent).onChanged(null)
    }

    @Test
    fun `on camera requested`() {
        val observerRequestCamera: Observer<Void> = mock()
        viewModel.requestCameraEvent.observeForever(observerRequestCamera)
        viewModel.onCameraRequested()
        verify(observerRequestCamera).onChanged(null)
    }

    @Test
    fun `on gallery requested`() {
        val observerRequestGallery: Observer<Void> = mock()
        viewModel.requestGalleryEvent.observeForever(observerRequestGallery)
        viewModel.onGalleryRequested()
        verify(observerRequestGallery).onChanged(null)
    }

    @Test
    fun `save account success`() {
        val artist = mock<AMArtist>()
        viewModel.loggedUser = artist
        `when`(userDataSource.saveAccount(artist)).thenReturn(Single.just(artist))
        val observerHideKeyboard: Observer<Void> = mock()
        val observerShowLoader: Observer<Void> = mock()
        val observerHideLoader: Observer<Void> = mock()
        val observerShowError: Observer<AccountSaveException> = mock()
        val observerClose: Observer<Void> = mock()
        viewModel.hideKeyboardEvent.observeForever(observerHideKeyboard)
        viewModel.showLoaderEvent.observeForever(observerShowLoader)
        viewModel.hideLoaderEvent.observeForever(observerHideLoader)
        viewModel.showErrorEvent.observeForever(observerShowError)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.onSaveTapped("", "", "", "", "", "")
        verify(observerShowLoader).onChanged(null)
        verify(observerHideLoader).onChanged(null)
        verifyZeroInteractions(observerShowError)
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `save account failure`() {
        val artist = mock<AMArtist>()
        viewModel.loggedUser = artist
        val error = AccountSaveException("title", "message")
        `when`(userDataSource.saveAccount(artist)).thenReturn(Single.error(error))
        val observerHideKeyboard: Observer<Void> = mock()
        val observerShowLoader: Observer<Void> = mock()
        val observerHideLoader: Observer<Void> = mock()
        val observerClose: Observer<Void> = mock()
        val observerShowError: Observer<AccountSaveException> = mock()
        viewModel.hideKeyboardEvent.observeForever(observerHideKeyboard)
        viewModel.showLoaderEvent.observeForever(observerShowLoader)
        viewModel.hideLoaderEvent.observeForever(observerHideLoader)
        viewModel.closeEvent.observeForever(observerClose)
        viewModel.showErrorEvent.observeForever(observerShowError)
        viewModel.onSaveTapped("", "", "", "", "", "")
        verify(observerShowLoader).onChanged(null)
        verify(observerHideLoader).onChanged(null)
        verifyZeroInteractions(observerClose)
        verify(observerShowError).onChanged(eq(error))
    }

    @Test
    fun `bio too long`() {
        val bio: String = (0 until 1000).map { "a" }.joinToString()
        val observerUpdateBio: Observer<String> = mock()
        val observerUpdateBioCounter: Observer<String> = mock()
        viewModel.bio.observeForever(observerUpdateBio)
        viewModel.bioCounter.observeForever(observerUpdateBioCounter)
        viewModel.onBioChanged(bio)
        verify(observerUpdateBio).onChanged(argWhere { it.length == AMArtist.BIO_MAX_LENGTH })
        verifyZeroInteractions(observerUpdateBioCounter)
    }

    @Test
    fun `bio short enough`() {
        val bio: String = (0 until 50).joinToString { "a" }
        val observerUpdateBio: Observer<String> = mock()
        val observerUpdateBioCounter: Observer<String> = mock()
        viewModel.bio.observeForever(observerUpdateBio)
        viewModel.bioCounter.observeForever(observerUpdateBioCounter)
        viewModel.onBioChanged(bio)
        verifyZeroInteractions(observerUpdateBio)
        verify(observerUpdateBioCounter).onChanged(anyString())
    }

    @Test
    fun `onTextChanged observed`() {
        val editText = mock<EditText>()
        val newString = "Hello"
        val originalString = "Hell"
        viewModel.onTextChanged(editText, newString, originalString)
        verify(observerText).onChanged(
            EditAccountViewModel.TextData(editText, newString, originalString)
        )
    }

    @Test
    fun `onPermissionsEnabled calls mixpanel`() {
        val context = mock<Context>()
        val permissions = arrayOf<String>()
        val grantResults = intArrayOf()
        viewModel.onPermissionsEnabled(context, permissions, grantResults)
        verify(mixpanelDataSource).trackEnablePermissions(context, permissions, grantResults)
    }

    @Test
    fun `onActivityResult calls socialAuthManager`() {
        val requestCode = 0
        val resultCode = 1
        val data = null
        viewModel.onActivityResult(requestCode, resultCode, data)
        verify(socialAuthManager).onActivityResult(requestCode, resultCode, data)
    }

    @Test
    fun `dirty user slug`() {
        val dirtySlug = "-%ab#cd&-"
        val cleanSlug = "-abcd-"
        val observerUrlSlug: Observer<Resource<*>> = mock()
        viewModel.urlSlug.observeForever(observerUrlSlug)
        viewModel.onUrlSlugChanged(dirtySlug)
        ArgumentCaptor.forClass(Resource::class.java).run {
            verify(observerUrlSlug).onChanged(capture())
            assertTrue(value is Resource.Success)
            assertEquals(cleanSlug, value.data)
        }
    }

    @Test
    fun `clean user slug`() {
        val urlSlug = "abcd"
        val observerUrlSlug: Observer<Resource<*>> = mock()
        viewModel.urlSlug.observeForever(observerUrlSlug)
        viewModel.onUrlSlugChanged(urlSlug)
        ArgumentCaptor.forClass(Resource::class.java).run {
            verify(observerUrlSlug).onChanged(capture())
            assertTrue(value is Resource.Success)
            assertEquals(urlSlug, value.data)
        }
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

    @Test
    fun `after saving banner image to file, load banner image`() {
        val saveImageUseCase = mock<SaveImageUseCase>()

        viewModel.editingMode = EditAccountViewModel.EditingMode.Banner
        whenever(saveImageUseCase.copyInputStreamToFile(any(), any())).thenReturn(1L)

        val observerOnShowBannerEvent: Observer<Void> = mock()
        viewModel.showBannerEvent.observeForever(observerOnShowBannerEvent)

        viewModel.saveGalleryImage(saveImageUseCase, mock())

        verify(observerOnShowBannerEvent).onChanged(null)
    }

    @Test
    fun `after saving avatar image to file, crop image`() {
        val saveImageUseCase = mock<SaveImageUseCase>()

        viewModel.editingMode = EditAccountViewModel.EditingMode.Avatar
        whenever(saveImageUseCase.copyInputStreamToFile(any(), any())).thenReturn(1L)

        val observerOnCropImageEvent: Observer<Void> = mock()
        viewModel.cropImageEvent.observeForever(observerOnCropImageEvent)

        viewModel.saveGalleryImage(saveImageUseCase, mock())

        verify(observerOnCropImageEvent).onChanged(null)
    }

    @Test
    fun `link twitter click observed`() {
        val observerStartAuthentication: Observer<SocialNetwork> = mock()
        viewModel.authentication.observeForever(observerStartAuthentication)

        viewModel.onTwitterTapped()

        verify(observerStartAuthentication).onChanged(eq(SocialNetwork.Twitter))
    }

    @Test
    fun `link twitter, auth fails`() {
        whenever(socialAuthManager.authenticateWithTwitter(any())).thenReturn(Observable.error(Exception("")))
        whenever(socialLinkDataSource.linkTwitter(anyString(), anyString())).thenReturn(Completable.error(Exception("")))
        whenever(userDataSource.refreshUserData()).thenReturn(Observable.error(Exception("")))

        viewModel.onLinkSocial(mock(), SocialNetwork.Twitter)

        verifyZeroInteractions(socialLinkDataSource)
        verifyZeroInteractions(observerShowLoaderEvent)
        verify(observerShowGenericErrorEvent).onChanged(null)
    }

    @Test
    fun `link twitter, auth succeeds, link fails (generic)`() {
        val token = "token"
        val secret = "secret"
        whenever(socialAuthManager.authenticateWithTwitter(any())).thenReturn(Observable.just(
            TwitterAuthData(token, secret, false)
        ))
        whenever(socialLinkDataSource.linkTwitter(anyString(), anyString())).thenReturn(Completable.error(Exception("")))
        whenever(userDataSource.refreshUserData()).thenReturn(Observable.error(Exception("")))

        viewModel.onLinkSocial(mock(), SocialNetwork.Twitter)

        verify(socialLinkDataSource).linkTwitter(eq(token), eq(secret))
        verify(observerShowLoaderEvent).onChanged(null)
        verify(observerShowGenericErrorEvent).onChanged(null)
    }

    @Test
    fun `link twitter, auth succeeds, link fails (already linked)`() {
        val token = "token"
        val secret = "secret"
        whenever(socialAuthManager.authenticateWithTwitter(any())).thenReturn(Observable.just(
            TwitterAuthData(token, secret, false)
        ))
        whenever(socialLinkDataSource.linkTwitter(anyString(), anyString())).thenReturn(Completable.error(LinkSocialException.SocialIDAlreadyLinked))
        whenever(userDataSource.refreshUserData()).thenReturn(Observable.error(Exception("")))

        viewModel.onLinkSocial(mock(), SocialNetwork.Twitter)

        verify(socialLinkDataSource).linkTwitter(eq(token), eq(secret))
        verify(observerShowLoaderEvent).onChanged(null)
        verifyZeroInteractions(observerShowGenericErrorEvent)
        verify(observerShowAlreadyLinkedEvent).onChanged(SocialNetwork.Twitter)
    }

    @Test
    fun `link twitter, auth succeeds, link succeeds, refresh user fails`() {
        val token = "token"
        val secret = "secret"
        whenever(socialAuthManager.authenticateWithTwitter(any())).thenReturn(Observable.just(
            TwitterAuthData(token, secret, false)
        ))
        whenever(socialLinkDataSource.linkTwitter(anyString(), anyString())).thenReturn(Completable.complete())
        whenever(userDataSource.refreshUserData()).thenReturn(Observable.error(Exception("")))

        val twitterObserver: Observer<String> = mock()
        val twitterLinkedObserver: Observer<Boolean> = mock()
        viewModel.twitter.observeForever(twitterObserver)
        viewModel.twitterLinked.observeForever(twitterLinkedObserver)

        viewModel.onLinkSocial(mock(), SocialNetwork.Twitter)

        verify(socialLinkDataSource).linkTwitter(eq(token), eq(secret))
        verify(observerShowLoaderEvent).onChanged(null)
        verifyZeroInteractions(observerShowGenericErrorEvent)
        verifyZeroInteractions(twitterObserver)
        verify(twitterLinkedObserver).onChanged(true)
    }

    @Test
    fun `link twitter, auth succeeds, link succeeds, refresh user succeeds`() {
        val mockedArtist = mock<AMArtist> {
            on { twitterId } doReturn "id"
            on { twitter } doReturn "user"
        }

        val token = "token"
        val secret = "secret"
        whenever(socialAuthManager.authenticateWithTwitter(any())).thenReturn(Observable.just(
            TwitterAuthData(token, secret, false)
        ))
        whenever(socialLinkDataSource.linkTwitter(anyString(), anyString())).thenReturn(Completable.complete())
        whenever(userDataSource.refreshUserData()).thenReturn(Observable.just(mockedArtist))

        val twitterObserver: Observer<String> = mock()
        val twitterLinkedObserver: Observer<Boolean> = mock()
        viewModel.twitter.observeForever(twitterObserver)
        viewModel.twitterLinked.observeForever(twitterLinkedObserver)

        viewModel.onLinkSocial(mock(), SocialNetwork.Twitter)

        verify(socialLinkDataSource).linkTwitter(eq(token), eq(secret))
        verify(observerShowLoaderEvent).onChanged(null)
        verifyZeroInteractions(observerShowGenericErrorEvent)
        verify(twitterObserver).onChanged(eq("user"))
        verify(twitterLinkedObserver).onChanged(true)
    }

    @Test
    fun `link instagram click observed`() {
        viewModel.onInstagramTapped()

        verify(observerShowInstagramWebViewEvent).onChanged(null)
    }

    @Test
    fun `link instagram, auth cancelled`() {
        viewModel.handleInstagramResult(WebViewAuthResult.Cancel)

        verifyZeroInteractions(socialLinkDataSource)
        verifyZeroInteractions(observerShowGenericErrorEvent)
    }

    @Test
    fun `link instagram, auth fails`() {
        viewModel.handleInstagramResult(WebViewAuthResult.Failure(Exception("Test")))

        verifyZeroInteractions(socialLinkDataSource)
        verify(observerShowGenericErrorEvent).onChanged(null)
    }

    @Test
    fun `link instagram, auth succeeds, link fails (generic)`() {
        whenever(socialLinkDataSource.linkInstagram(anyString())).thenReturn(Completable.error(Exception("")))
        whenever(userDataSource.refreshUserData()).thenReturn(Observable.error(Exception("")))

        val token = "token"
        viewModel.handleInstagramResult(WebViewAuthResult.Success(token))

        verify(socialLinkDataSource).linkInstagram(eq(token))
        verify(observerShowLoaderEvent).onChanged(null)
        verify(observerShowGenericErrorEvent).onChanged(null)
    }

    @Test
    fun `link instagram, auth succeeds, link fails (already linked)`() {
        whenever(socialLinkDataSource.linkInstagram(anyString())).thenReturn(Completable.error(LinkSocialException.SocialIDAlreadyLinked))
        whenever(userDataSource.refreshUserData()).thenReturn(Observable.error(Exception("")))

        val token = "token"
        viewModel.handleInstagramResult(WebViewAuthResult.Success(token))

        verify(socialLinkDataSource).linkInstagram(eq(token))
        verify(observerShowLoaderEvent).onChanged(null)
        verifyZeroInteractions(observerShowGenericErrorEvent)
        verify(observerShowAlreadyLinkedEvent).onChanged(SocialNetwork.Instagram)
    }

    @Test
    fun `link instagram, auth succeeds, link succeeds, refresh user fails`() {
        whenever(socialLinkDataSource.linkInstagram(anyString())).thenReturn(Completable.complete())
        whenever(userDataSource.refreshUserData()).thenReturn(Observable.error(Exception("")))

        val instagramObserver: Observer<String> = mock()
        val instagramLinkedObserver: Observer<Boolean> = mock()
        viewModel.instagram.observeForever(instagramObserver)
        viewModel.instagramLinked.observeForever(instagramLinkedObserver)

        val token = "token"
        viewModel.handleInstagramResult(WebViewAuthResult.Success(token))

        verify(socialLinkDataSource).linkInstagram(eq(token))
        verify(observerShowLoaderEvent).onChanged(null)
        verifyZeroInteractions(observerShowGenericErrorEvent)
        verifyZeroInteractions(instagramObserver)
        verify(instagramLinkedObserver).onChanged(true)
    }

    @Test
    fun `link instagram, auth succeeds, link succeeds, refresh user succeeds`() {
        val mockedArtist = mock<AMArtist> {
            on { instagramId } doReturn "id"
            on { instagram } doReturn "user"
        }

        whenever(socialLinkDataSource.linkInstagram(anyString())).thenReturn(Completable.complete())
        whenever(userDataSource.refreshUserData()).thenReturn(Observable.just(mockedArtist))

        val instagramObserver: Observer<String> = mock()
        val instagramLinkedObserver: Observer<Boolean> = mock()
        viewModel.instagram.observeForever(instagramObserver)
        viewModel.instagramLinked.observeForever(instagramLinkedObserver)

        val token = "token"
        viewModel.handleInstagramResult(WebViewAuthResult.Success(token))

        verify(socialLinkDataSource).linkInstagram(eq(token))
        verify(observerShowLoaderEvent).onChanged(null)
        verify(observerHideLoaderEvent).onChanged(null)
        verifyZeroInteractions(observerShowGenericErrorEvent)
        verify(instagramObserver).onChanged(eq("user"))
        verify(instagramLinkedObserver).onChanged(true)
    }

    @Test
    fun `facebook click observed`() {
        val observerStartAuthentication: Observer<SocialNetwork> = mock()
        viewModel.authentication.observeForever(observerStartAuthentication)

        viewModel.onFacebookTapped()

        verify(observerStartAuthentication).onChanged(eq(SocialNetwork.Facebook))
    }

    @Test
    fun `youtube click observed`() {
        val observerStartAuthentication: Observer<SocialNetwork> = mock()
        viewModel.authentication.observeForever(observerStartAuthentication)

        viewModel.onYoutubeTapped()

        verify(observerStartAuthentication).onChanged(eq(SocialNetwork.YouTube))
    }
}
