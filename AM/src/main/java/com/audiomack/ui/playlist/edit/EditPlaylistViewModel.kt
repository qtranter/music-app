package com.audiomack.ui.playlist.edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.playlist.PlayListDataSource
import com.audiomack.data.playlist.PlaylistRepository
import com.audiomack.data.tracking.appsflyer.AppsFlyerDataSource
import com.audiomack.data.tracking.appsflyer.AppsFlyerRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMResultItem.ItemImagePreset
import com.audiomack.model.Action
import com.audiomack.model.AddToPlaylistModel
import com.audiomack.model.EventPlaylistDeleted
import com.audiomack.model.EventPlaylistEdited
import com.audiomack.model.PermissionType
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.common.GenreProvider
import com.audiomack.ui.playlist.edit.EditPlaylistException.Type
import com.audiomack.ui.playlist.edit.EditPlaylistMode.CREATE
import com.audiomack.ui.playlist.edit.EditPlaylistMode.EDIT
import com.audiomack.usecases.SaveImageUseCase
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.Utils.saveImageFileFromUri
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File
import java.io.InputStream
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

private const val TAG = "EditPlaylistViewModel"

class EditPlaylistViewModel(
    private val playListDataSource: PlayListDataSource = PlaylistRepository(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val appsFlyerDataSource: AppsFlyerDataSource = AppsFlyerRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val playlistItemProvider: EditPlaylistItemProvider = EditPlaylistProvider.getInstance(),
    private val playlistImageProvider: EditPlaylistImageProvider = EditPlaylistProvider.getInstance(),
    private val eventBus: EventBus = EventBus.getDefault()
) : BaseViewModel() {

    private val _mode = MutableLiveData<EditPlaylistMode>()
    val mode: LiveData<EditPlaylistMode> get() = _mode

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> get() = _title

    private val _genre = MutableLiveData<String>()
    val genre: LiveData<String> get() = _genre

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> get() = _description

    private val _banner = MutableLiveData<String>()
    val banner: LiveData<String> get() = _banner

    private val _smallImage = MutableLiveData<String>()
    val smallImage: LiveData<String> get() = _smallImage

    private val _private = MutableLiveData<Boolean>()
    val private: LiveData<Boolean> get() = _private

    private val _imageBase64 = MutableLiveData<String>()
    val imageBase64: LiveData<String> get() = _imageBase64

    private val _bannerImageBase64 = MutableLiveData<String>()
    val bannerImageBase64: LiveData<String> get() = _bannerImageBase64

    val imageFile: File?
        get() = playlistImageProvider.imageFile

    val bannerFile: File?
        get() = playlistImageProvider.bannerFile

    // Activity Events

    val showOptionsEvent = SingleLiveEvent<List<Action>>()
    val finishEvent = SingleLiveEvent<Void>()
    val popBackStackEvent = SingleLiveEvent<Void>()
    val backEvent = SingleLiveEvent<Void>()

    // Fragment Events

    val createdEvent = SingleLiveEvent<AMResultItem>()
    val editedEvent = SingleLiveEvent<AMResultItem>()
    val deletedEvent = SingleLiveEvent<AMResultItem>()
    val errorEvent = SingleLiveEvent<EditPlaylistException>()
    val changeEvent = SingleLiveEvent<Void>()

    val progressEvent = SingleLiveEvent<Boolean>()
    val hideKeyboardEvent = SingleLiveEvent<Void>()
    val editImageEvent = SingleLiveEvent<Void>()
    val editBannerEvent = SingleLiveEvent<Void>()
    val saveBannerEvent = SingleLiveEvent<File>()
    val imageSavedEvent = SingleLiveEvent<File>()
    val deletePromptEvent = SingleLiveEvent<String>()
    val showBannerEvent = SingleLiveEvent<Void>()
    val cropImageEvent = SingleLiveEvent<Void>()

    // Local state

    private var playlist: AMResultItem? = null
    private var addToPlaylistData: AddToPlaylistModel? = null

    private lateinit var viewStateProvider: EditPlaylistViewStateProvider
    private lateinit var genreProvider: GenreProvider

    fun init(
        mode: EditPlaylistMode,
        data: AddToPlaylistModel?,
        viewStateProvider: EditPlaylistViewStateProvider,
        genreProvider: GenreProvider
    ) {
        _mode.postValue(mode)
        this.viewStateProvider = viewStateProvider
        this.genreProvider = genreProvider

        when (mode) {
            EDIT -> playlist = playlistItemProvider.playlist.also {
                onPlaylistLoaded(it)
            } ?: throw IllegalStateException("Mode is EDIT, but no playlist found")
            CREATE -> addToPlaylistData = data.also {
                onAddToPlaylistDataLoaded(it)
            } ?: throw IllegalStateException("Mode is CREATE, but no 'AddToPlaylistModel' was found")
        }
    }

    override fun onCleared() {
        super.onCleared()
        hideKeyboardEvent.call()
    }

    private fun onPlaylistLoaded(playlist: AMResultItem?) {
        Timber.tag(TAG).d("onPlaylistLoaded: $playlist, banner = ${playlist?.banner}")
        playlist?.let {
            _title.postValue(it.title)
            _genre.postValue(genreProvider.getHumanValue(it.genre))
            _description.postValue(it.desc)
            _banner.postValue(it.banner)
            _smallImage.postValue(it.getImageURLWithPreset(ItemImagePreset.ItemImagePresetSmall))
            _private.postValue(it.isPrivatePlaylist)
        }
    }

    private fun onAddToPlaylistDataLoaded(data: AddToPlaylistModel?) {
        Timber.tag(TAG).d("onAddToPlaylistDataLoaded: $data")
        data?.let {
            _genre.postValue(genreProvider.getHumanValue(it.genre))
            _smallImage.postValue(it.thumbnail)
            _private.postValue(false)
        }
    }

    fun onTitleChange(newTitle: String?) {
        newTitle?.let {
            if (it != title.value) {
                onContentChange()
            }
        }
    }

    fun onDescriptionChange(newDescription: String?) {
        newDescription?.let {
            if (it != description.value) {
                onContentChange()
            }
        }
    }

    fun onSaveClick() {
        if (!viewStateProvider.isViewAvailable()) return

        if (!viewStateProvider.isBannerVisible()) {
            onSaveBanner()
        } else {
            onSavePlaylist()
        }
    }

    private fun onSaveBanner() {
        progressEvent.postValue(true)
        saveBannerEvent.postValue(playlistImageProvider.bannerFile)
    }

    fun onBannerSaved() {
        onSavePlaylist()
    }

    private fun onSavePlaylist() {
        when (mode.value) {
            CREATE -> onCreatePlaylist()
            EDIT -> onEditPlaylist()
        }
    }

    private fun onCreatePlaylist() {
        val title = viewStateProvider.getTitle()
        if (!validTitle(title)) return

        addToPlaylistData?.let { data ->
            val size = data.songs.size
            val itemIds = data.songs.map { it.id }.joinToString(",") { it }

            val genre = viewStateProvider.getGenre()
            val desc = viewStateProvider.getDesc()

            createPlaylist(
                title,
                genreProvider.getApiValue(genre),
                desc,
                isPrivate(),
                itemIds,
                _imageBase64.value,
                _bannerImageBase64.value,
                size,
                data.mixpanelSource.page
            )
        } ?: onPlaylistCreateError(IllegalStateException("There are no songs to add"))
    }

    private fun onPlaylistCreateError(error: Throwable?) {
        errorEvent.postValue(
            EditPlaylistException(
                Type.CREATE,
                error ?: RuntimeException("Unable to create playlist")
            )
        )
    }

    private fun createPlaylist(
        title: String,
        genre: String?,
        desc: String?,
        privatePlaylist: Boolean,
        musicIds: String?,
        imageBase64: String?,
        bannerImageBase64: String?,
        playlistSize: Int,
        mixpanelPage: String
    ) {
        val disposable = playListDataSource.createPlaylist(
            title,
            genre,
            desc,
            privatePlaylist,
            musicIds,
            imageBase64,
            bannerImageBase64,
            mixpanelPage
        )
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .doOnSubscribe {
                progressEvent.postValue(true)
                hideKeyboardEvent.call()
            }
            .doFinally { progressEvent.postValue(false) }
            .subscribe({ playlist ->
                if (playlistSize == 1) {
                    mixpanelDataSource.trackCreatePlaylist(playlist)
                    appsFlyerDataSource.trackCreatePlaylist()
                }
                createdEvent.postValue(playlist)
                finishEvent.call()
            }, { error ->
                onPlaylistCreateError(error)
            })
        compositeDisposable.add(disposable)
    }

    private fun onEditPlaylist() {
        val title = viewStateProvider.getTitle()
        if (!validTitle(title)) return

        playlist?.let {
            val genre = viewStateProvider.getGenre()
            val desc = viewStateProvider.getDesc()

            editPlaylist(
                it.itemId,
                title,
                genreProvider.getApiValue(genre),
                desc,
                isPrivate(),
                it.trackIDs,
                _imageBase64.value,
                _bannerImageBase64.value
            )
        } ?: onPlaylistEditError(IllegalStateException("No playlist found"))
    }

    private fun onPlaylistEditError(error: Throwable?) {
        errorEvent.postValue(
            EditPlaylistException(Type.EDIT, error ?: RuntimeException("Unable to edit playlist"))
        )
    }

    private fun editPlaylist(
        id: String,
        title: String,
        genre: String?,
        desc: String?,
        privatePlaylist: Boolean,
        musicId: String,
        imageBase64: String?,
        bannerImageBase64: String?
    ) {
        val disposable = playListDataSource.editPlaylist(
            id,
            title,
            genre,
            desc,
            privatePlaylist,
            musicId,
            imageBase64,
            bannerImageBase64
        )
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .doOnSubscribe {
                progressEvent.postValue(true)
                hideKeyboardEvent.call()
            }
            .doFinally { progressEvent.postValue(false) }
            .subscribe({ playlist ->
                editedEvent.postValue(playlist)
                eventBus.post(EventPlaylistEdited(playlist))
                finishEvent.call()
            }, { error ->
                onPlaylistEditError(error)
            })
        compositeDisposable.add(disposable)
    }

    fun onDeleteClick() {
        deletePromptEvent.postValue(title.value)
    }

    fun onDeleteConfirmed() {
        playlist?.let { deletePlaylist(it) } ?: onPlaylistDeleteError()
    }

    private fun deletePlaylist(playlist: AMResultItem) {
        val disposable = playListDataSource.deletePlaylist(playlist.itemId)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .doOnSubscribe { progressEvent.postValue(true) }
            .doFinally { progressEvent.postValue(false) }
            .subscribe({
                deletedEvent.postValue(playlist)
                eventBus.post(EventPlaylistDeleted(playlist))
                finishEvent.call()
            }, { error: Throwable? ->
                onPlaylistDeleteError(error)
            })
        compositeDisposable.add(disposable)
    }

    private fun onPlaylistDeleteError(error: Throwable? = null) {
        errorEvent.postValue(
            EditPlaylistException(
                Type.DELETE,
                error ?: IllegalStateException("No playlist found")
            )
        )
        finishEvent.call()
    }

    fun onGenreClick() {
        hideKeyboardEvent.call()

        val actions = genreProvider.getHumanValueList().map { genre ->
            val listener = object : Action.ActionListener {
                override fun onActionExecuted() {
                    onGenreSelected(genre)
                }
            }
            Action(genre, this.genre.value == genre, listener)
        }
        showOptionsEvent.postValue(actions)
    }

    fun onGenreSelected(genre: String) {
        _genre.postValue(genre)
        onContentChange()
        popBackStackEvent.call()
    }

    fun onPermissionsClick() {
        val private = private.value ?: false
        _private.postValue(!private)
        onContentChange()
    }

    fun onPermissionRequested(type: PermissionType) {
        mixpanelDataSource.trackPromptPermissions(type)
    }

    fun onPermissionsEnabled(context: Context, permissions: Array<String>, grantResults: IntArray) {
        mixpanelDataSource.trackEnablePermissions(context, permissions, grantResults)
    }

    fun onEditImageClick() {
        imageFile?.let { file ->
            compositeDisposable.add(
                Observable.just(file)
                    .subscribeOn(schedulersProvider.io)
                    .map { it.delete() }
                    .observeOn(schedulersProvider.main)
                    .subscribe {
                        hideKeyboardEvent.call()
                        editImageEvent.call()
                        onContentChange()
                    }
            )
        }
    }

    fun onEditBannerClick() {
        bannerFile?.let { file ->
            compositeDisposable.add(
                Observable.just(file)
                    .subscribeOn(schedulersProvider.io)
                    .map { it.delete() }
                    .observeOn(schedulersProvider.main)
                    .subscribe {
                        hideKeyboardEvent.call()
                        editBannerEvent.call()
                        onContentChange()
                    }
            )
        }
    }

    fun onBannerImageCreated(imageStream: InputStream) {
        val disposable = Single.just(imageStream)
            .subscribeOn(schedulersProvider.computation)
            .map(playlistImageProvider::inputStreamToBase64)
            .observeOn(schedulersProvider.main)
            .doOnSubscribe { progressEvent.postValue(true) }
            .doFinally { progressEvent.postValue(false) }
            .subscribe({ encodedString ->
                _bannerImageBase64.postValue(encodedString)
            }, { error ->
                onBannerSaveError(error)
            })
        compositeDisposable.add(disposable)
    }

    fun onPlaylistImageCreated() {
        val disposable = Single.just(imageFile)
            .subscribeOn(schedulersProvider.computation)
            .map(playlistImageProvider::fileToBase64)
            .observeOn(schedulersProvider.main)
            .doOnSubscribe { progressEvent.postValue(true) }
            .doFinally { progressEvent.postValue(false) }
            .subscribe { encodedString ->
                _imageBase64.postValue(encodedString)
                imageSavedEvent.postValue(imageFile)
                onContentChange()
            }
        compositeDisposable.add(disposable)
    }

    fun saveGalleryImage(saveImageUseCase: SaveImageUseCase, uri: Uri?, file: File?) {
        saveImageFileFromUri(saveImageUseCase, uri, file)
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .onErrorReturnItem(false)
            .doOnSuccess {
                if (it) {
                    if (file?.equals(imageFile) == true) {
                        cropImage()
                    }
                    if (file?.equals(bannerFile) == true) {
                        showBannerImage()
                    }
                }
            }
            .subscribe()
            .composite()
    }

    private fun cropImage() {
        cropImageEvent.call()
    }

    private fun showBannerImage() {
        showBannerEvent.call()
    }

    @JvmOverloads
    fun onBannerSaveError(error: Throwable? = null) {
        errorEvent.postValue(EditPlaylistException(Type.BANNER, error))
    }

    fun onBackClick() {
        backEvent.call()
    }

    fun onContentChange() {
        changeEvent.call()
    }

    private fun validTitle(title: String): Boolean {
        if (title.isBlank()) {
            errorEvent.postValue(EditPlaylistException(Type.TITLE))
            return false
        }
        return true
    }

    private fun isPrivate(): Boolean {
        return private.value ?: false
    }
}
