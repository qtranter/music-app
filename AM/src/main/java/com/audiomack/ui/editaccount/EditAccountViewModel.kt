package com.audiomack.ui.editaccount

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.socialauth.SocialAuthManager
import com.audiomack.data.socialauth.SocialAuthManagerImpl
import com.audiomack.data.sociallink.SocialLinkDataSource
import com.audiomack.data.sociallink.SocialLinkRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.AccountImageProvider
import com.audiomack.data.user.AccountImages
import com.audiomack.data.user.AccountSaveException
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.data.user.UserSlugSaveException
import com.audiomack.model.AMArtist
import com.audiomack.model.PermissionType
import com.audiomack.model.SocialNetwork
import com.audiomack.network.LinkSocialException
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.common.Resource
import com.audiomack.ui.webviewauth.WebViewAuthResult
import com.audiomack.usecases.SaveImageUseCase
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.Utils.saveImageFileFromUri
import com.steelkiwi.cropiwa.CropIwaView
import io.reactivex.Observable
import java.io.File
import java.util.Locale
import kotlin.math.min

class EditAccountViewModel(
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val accountImages: AccountImages = AccountImageProvider(),
    val imageLoader: ImageLoader = PicassoImageLoader,
    private val socialAuthManager: SocialAuthManager = SocialAuthManagerImpl(),
    private val socialLinkDataSource: SocialLinkDataSource = SocialLinkRepository(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    enum class EditingMode {
        None, Avatar, Banner
    }

    data class TextData(var editText: EditText, var newValue: String, var originalValue: String)

    val showFilePickerTypeAlertEvent = SingleLiveEvent<Void>()
    val requestCameraEvent = SingleLiveEvent<Void>()
    val requestGalleryEvent = SingleLiveEvent<Void>()
    val showBannerEvent = SingleLiveEvent<Void>()
    val cropImageEvent = SingleLiveEvent<Void>()

    val closeEvent = SingleLiveEvent<Void>()
    val hideKeyboardEvent = SingleLiveEvent<Void>()
    val showLoaderEvent = SingleLiveEvent<Void>()
    val hideLoaderEvent = SingleLiveEvent<Void>()
    val showErrorEvent = SingleLiveEvent<AccountSaveException>()
    val showGenericErrorEvent = SingleLiveEvent<Void>()
    val refreshSaveButtonEvent = SingleLiveEvent<Boolean>()
    val showInstagramWebViewEvent = SingleLiveEvent<Void>()
    val showAlreadyLinkedEvent = SingleLiveEvent<SocialNetwork>()

    private val _artist = MutableLiveData<AMArtist>()
    val artist: LiveData<AMArtist> get() = _artist

    private val _displayName = MutableLiveData<String>()
    val displayName: LiveData<String> get() = _displayName

    private val _verifiedName = MutableLiveData<String>()
    val verifiedName: LiveData<String> get() = _verifiedName

    private val _tastemakerName = MutableLiveData<String>()
    val tastemakerName: LiveData<String> get() = _tastemakerName

    private val _authenticatedName = MutableLiveData<String>()
    val authenticatedName: LiveData<String> get() = _authenticatedName

    private val _imageUrl = MutableLiveData<String>()
    val imageUrl: LiveData<String> get() = _imageUrl

    private val _bannerUrl = MutableLiveData<String>()
    val bannerUrl: LiveData<String> get() = _bannerUrl

    private val _name = MutableLiveData<String>()
    val name: LiveData<String> get() = _name

    private val _label = MutableLiveData<String>()
    val label: LiveData<String> get() = _label

    private val _hometown = MutableLiveData<String>()
    val hometown: LiveData<String> get() = _hometown

    private val _url = MutableLiveData<String>()
    val url: LiveData<String> get() = _url

    private val _bio = MutableLiveData<String>()
    val bio: LiveData<String> get() = _bio

    private val _bioCounter = MutableLiveData<String>()
    val bioCounter: LiveData<String> get() = _bioCounter

    private val _urlSlug = MutableLiveData<Resource<String>>()
    val urlSlug: LiveData<Resource<String>> get() = _urlSlug

    private val _followersExtended = MutableLiveData<String>()
    val followersExtended: LiveData<String> get() = _followersExtended

    private val _followingExtended = MutableLiveData<String>()
    val followingExtended: LiveData<String> get() = _followingExtended

    private val _playsExtended = MutableLiveData<String>()
    val playsExtended: LiveData<String> get() = _playsExtended

    private val _playsCount = MutableLiveData<Long>()
    val playsCount: LiveData<Long> get() = _playsCount

    private val _text = MutableLiveData<TextData>()
    val text: LiveData<TextData> get() = _text

    private val _authentication = MutableLiveData<SocialNetwork>()
    val authentication: LiveData<SocialNetwork> get() = _authentication

    /** Emits the twitter slug **/
    private val _twitter = MutableLiveData<String>()
    val twitter: LiveData<String> get() = _twitter

    /** Emits a boolean representing the twitter linkage validity **/
    private val _twitterLinked = MutableLiveData<Boolean>()
    val twitterLinked: LiveData<Boolean> get() = _twitterLinked

    /** Emits the instagram username **/
    private val _instagram = MutableLiveData<String>()
    val instagram: LiveData<String> get() = _instagram

    /** Emits a boolean representing the instagram linkage validity **/
    private val _instagramLinked = MutableLiveData<Boolean>()
    val instagramLinked: LiveData<Boolean> get() = _instagramLinked

    /** Emits the facebook url **/
    private val _facebook = MutableLiveData<String>()
    val facebook: LiveData<String> get() = _facebook

    /** Emits a boolean representing the facebook linkage validity **/
    private val _facebookLinked = MutableLiveData<Boolean>()
    val facebookLinked: LiveData<Boolean> get() = _facebookLinked

    /** Emits the youtube slug **/
    private val _youtube = MutableLiveData<String>()
    val youtube: LiveData<String> get() = _youtube

    /** Emits a boolean representing the youtube linkage validity **/
    private val _youtubeLinked = MutableLiveData<Boolean>()
    val youtubeLinked: LiveData<Boolean> get() = _youtubeLinked

    lateinit var loggedUser: AMArtist

    var editingMode = EditingMode.None

    val imageFile: File
        get() = if (editingMode == EditingMode.Avatar) accountImages.avatarFile else accountImages.bannerFile

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onCreate() {
        val artist = userDataSource.getUser()

        artist?.let {
            loggedUser = it
            _artist.postValue(it)

            _imageUrl.postValue(it.smallImage)
            _bannerUrl.postValue(if (it.banner.isNullOrEmpty()) it.smallImage else it.banner)

            when {
                it.isVerified -> _verifiedName.postValue(it.name)
                it.isTastemaker -> _tastemakerName.postValue(it.name)
                it.isAuthenticated -> _authenticatedName.postValue(it.name)
                else -> _displayName.postValue(it.name)
            }

            _followersExtended.postValue(it.followersExtended)
            _followingExtended.postValue(it.followingExtended)
            _playsExtended.postValue(it.playsExtended)
            _playsCount.postValue(it.playsCount)
            _name.postValue(it.name)
            _urlSlug.postValue(Resource.Success(it.urlSlug))
            _label.postValue(it.label)
            _hometown.postValue(it.hometown)
            _url.postValue(it.url)
            _bio.postValue(it.bio)

            _twitter.postValue(it.twitter)
            _twitterLinked.postValue(!it.twitterId.isNullOrBlank())
            _instagram.postValue(it.instagram)
            _instagramLinked.postValue(!it.instagramId.isNullOrBlank())
            _facebook.postValue(it.facebook)
            _facebookLinked.postValue(!it.facebookId.isNullOrBlank())
            _youtube.postValue(it.youtube)
            _youtubeLinked.postValue(!it.youtubeId.isNullOrBlank())
        } ?: run {
            closeEvent.call()
        }
    }

    fun onLoadBannerCropView(context: Context?, imageUrl: String, cropIwaView: CropIwaView) {
        imageLoader.load(context, imageUrl, cropIwaView)
    }

    fun onLoadAvatarImageView(context: Context?, imageUrl: String, imageView: ImageView) {
        imageLoader.load(context, imageUrl, imageView)
    }

    fun onEditBannerTapped() {
        editingMode = EditingMode.Banner
        accountImages.bannerFile.delete()
        hideKeyboardEvent.call()
        showFilePickerTypeAlertEvent.call()
    }

    fun onEditAvatarTapped() {
        editingMode = EditingMode.Avatar
        accountImages.avatarFile.delete()
        hideKeyboardEvent.call()
        showFilePickerTypeAlertEvent.call()
    }

    fun onCameraRequested() {
        requestCameraEvent.call()
    }

    fun onGalleryRequested() {
        requestGalleryEvent.call()
    }

    fun onSaveTapped(name: String, urlSlug: String, label: String, hometown: String, website: String, bio: String) {
        hideKeyboardEvent.call()

        loggedUser.name = name
        loggedUser.label = label
        loggedUser.hometown = hometown
        loggedUser.url = website
        loggedUser.bio = bio
        loggedUser.twitter = twitter.value
        loggedUser.instagram = instagram.value
        loggedUser.facebook = facebook.value
        loggedUser.youtube = youtube.value
        loggedUser.urlSlug = urlSlug

        showLoaderEvent.call()

        compositeDisposable.add(
                userDataSource.saveAccount(loggedUser)
                        .subscribeOn(schedulersProvider.io)
                        .observeOn(schedulersProvider.main)
                        .subscribe({
                            hideLoaderEvent.call()
                            closeEvent.call()
                        }, { error ->
                            hideLoaderEvent.call()
                            when (error) {
                                is AccountSaveException -> {
                                    showErrorEvent.postValue(error)
                                }
                                is UserSlugSaveException -> {
                                    _urlSlug.postValue(Resource.Failure(error))
                                }
                                else -> {
                                    showGenericErrorEvent.call()
                                }
                            }
                        })
        )
    }

    fun onBioChanged(string: String) {
        if (string.length > AMArtist.BIO_MAX_LENGTH) {
            val shortBio = string.substring(0, min(AMArtist.BIO_MAX_LENGTH, string.length))
            _bio.postValue(shortBio)
        } else {
            val counterString = MainApplication.context?.getString(
                    R.string.editaccount_bio_counter_template,
                    (AMArtist.BIO_MAX_LENGTH - string.length).toString()
            ) ?: ""
            _bioCounter.postValue(counterString)
        }
    }

    fun onTextChanged(editText: EditText, newString: String, originalString: String) {
        _text.postValue(TextData(editText, newString, originalString))
    }

    fun onPermissionsEnabled(context: Context, permissions: Array<String>, grantResults: IntArray) {
        mixpanelDataSource.trackEnablePermissions(context, permissions, grantResults)
    }

    fun onPermissionRequested(type: PermissionType) {
        mixpanelDataSource.trackPromptPermissions(type)
    }

    fun onAvatarPicked(base64: String?) {
        loggedUser.imageBase64 = base64
    }

    fun onBannerPicked(base64: String?) {
        loggedUser.bannerBase64 = base64
    }

    fun onTwitterTapped() {
        _authentication.postValue(SocialNetwork.Twitter)
    }

    fun onInstagramTapped() {
        showInstagramWebViewEvent.call()
    }

    fun onFacebookTapped() {
        _authentication.postValue(SocialNetwork.Facebook)
    }

    fun onYoutubeTapped() {
        _authentication.postValue(SocialNetwork.YouTube)
    }

    fun handleInstagramResult(result: WebViewAuthResult) {

        when (result) {
            is WebViewAuthResult.Success -> {
                showLoaderEvent.call()
                compositeDisposable.add(
                    socialLinkDataSource.linkInstagram(result.token)
                        .subscribeOn(schedulersProvider.io)
                        .andThen(
                            userDataSource.refreshUserData()
                                .doOnError {
                                    // Mark instagram as linked even if we failed to get the updated data
                                    _instagramLinked.postValue(true)
                                }
                                .onErrorResumeNext(Observable.error(LinkSocialException.Ignore))
                        )
                        .observeOn(schedulersProvider.main)
                        .subscribe({
                            hideLoaderEvent.call()
                            _instagram.value = it.instagram
                            _instagramLinked.value = !it.instagramId.isNullOrBlank()
                        }, {
                            hideLoaderEvent.call()
                            if (it is LinkSocialException.SocialIDAlreadyLinked) {
                                showAlreadyLinkedEvent.postValue(SocialNetwork.Instagram)
                            } else if (it !is LinkSocialException.Ignore) {
                                showGenericErrorEvent.call()
                            }
                        })
                )
            }
            is WebViewAuthResult.Failure -> {
                showGenericErrorEvent.call()
            }
        }
    }

    fun onLinkSocial(activity: FragmentActivity, socialNetwork: SocialNetwork) {

        when (socialNetwork) {
            SocialNetwork.Twitter -> {
                compositeDisposable.add(
                    socialAuthManager.authenticateWithTwitter(activity)
                        .flatMap {
                            showLoaderEvent.call()
                            socialLinkDataSource.linkTwitter(it.token, it.secret).andThen(Observable.just(it))
                        }
                        .subscribeOn(schedulersProvider.io)
                        .flatMap {
                            userDataSource.refreshUserData()
                                .doOnError {
                                    // Mark twitter as linked even if we failed to get the updated data
                                    _twitterLinked.postValue(true)
                                }
                                .onErrorResumeNext(Observable.error(LinkSocialException.Ignore))
                        }
                        .observeOn(schedulersProvider.main)
                        .subscribe({
                            hideLoaderEvent.call()
                            _twitter.value = it.twitter
                            _twitterLinked.value = !it.twitterId.isNullOrBlank()
                        }, {
                            hideLoaderEvent.call()
                            if (it is LinkSocialException.SocialIDAlreadyLinked) {
                                showAlreadyLinkedEvent.postValue(SocialNetwork.Twitter)
                            } else if (it !is LinkSocialException.Ignore) {
                                showGenericErrorEvent.call()
                            }
                        })
                )
            }
            SocialNetwork.Instagram -> {
                // Nothing to do here, it's being handled with showInstagramWebViewEvent
            }
            SocialNetwork.Facebook -> {
                compositeDisposable.add(
                        LinkSocialAlertFragment.show(activity, socialNetwork, facebook.value)
                                .observeOn(schedulersProvider.main)
                                .subscribeOn(schedulersProvider.main)
                                .subscribe({
                                    refreshSaveButtonEvent.postValue(true)
                                    _facebook.postValue(it)
                                }, { /* Nothing to do here */ })
                )
            }
            SocialNetwork.YouTube -> {
                compositeDisposable.add(
                        LinkSocialAlertFragment.show(activity, socialNetwork, youtube.value)
                                .observeOn(schedulersProvider.main)
                                .subscribeOn(schedulersProvider.main)
                                .subscribe({
                                    refreshSaveButtonEvent.postValue(true)
                                    _youtube.postValue(it)
                                }, { /* Nothing to do here */ })
                )
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        socialAuthManager.onActivityResult(requestCode, resultCode, data)
    }

    fun onUrlSlugChanged(string: String) {
        val cleanString = cleanUrlSlug(string)
        _urlSlug.postValue(Resource.Success(cleanString))
    }

    private fun cleanUrlSlug(dirtySlug: String): String {

        val lowerCaseSlug = dirtySlug.toLowerCase(Locale.US)
        val shortenedSlug = lowerCaseSlug.take(AMArtist.URL_MAX_LENGTH)

        val regexAlphanumeric = Regex("[^-_a-z0-9 ]")
        var slug = regexAlphanumeric.replace(shortenedSlug, "")

        val hyphen = "-"
        val regexDivider = Regex("[ ]+")
        slug = regexDivider.replace(slug, hyphen)

        return slug
    }

    fun saveGalleryImage(saveImageUseCase: SaveImageUseCase, uri: Uri?) {
        saveImageFileFromUri(saveImageUseCase, uri, imageFile)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .onErrorReturnItem(false)
            .doOnSuccess {
                if (it) {
                    when (editingMode) {
                        EditingMode.Avatar -> cropAvatarImage()
                        EditingMode.Banner -> showBannerImage()
                        else -> {}
                    }
                }
            }
            .subscribe()
            .composite()
    }

    private fun cropAvatarImage() {
        cropImageEvent.call()
    }

    private fun showBannerImage() {
        showBannerEvent.call()
    }
}
