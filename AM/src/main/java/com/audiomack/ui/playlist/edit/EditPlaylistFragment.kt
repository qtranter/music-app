package com.audiomack.ui.playlist.edit

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.storage.AUTHORITY
import com.audiomack.model.AMResultItem
import com.audiomack.model.AddToPlaylistModel
import com.audiomack.model.PermissionType
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.common.AMGenreProvider
import com.audiomack.usecases.SaveImageUseCaseImpl
import com.audiomack.utils.TextWatcherAdapter
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.showPermissionRationaleDialog
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressHUD
import com.audiomack.views.AMSnackbar
import com.squareup.picasso.Picasso
import com.steelkiwi.cropiwa.AspectRatio
import com.steelkiwi.cropiwa.config.CropIwaSaveConfig
import com.steelkiwi.cropiwa.config.InitialPosition
import java.io.File
import kotlinx.android.synthetic.main.fragment_editplaylist.buttonBanner
import kotlinx.android.synthetic.main.fragment_editplaylist.buttonClose
import kotlinx.android.synthetic.main.fragment_editplaylist.buttonDelete
import kotlinx.android.synthetic.main.fragment_editplaylist.buttonSave
import kotlinx.android.synthetic.main.fragment_editplaylist.editImageButton
import kotlinx.android.synthetic.main.fragment_editplaylist.etDescription
import kotlinx.android.synthetic.main.fragment_editplaylist.etName
import kotlinx.android.synthetic.main.fragment_editplaylist.imageViewAvatar
import kotlinx.android.synthetic.main.fragment_editplaylist.imageViewBanner
import kotlinx.android.synthetic.main.fragment_editplaylist.imageViewPermissions
import kotlinx.android.synthetic.main.fragment_editplaylist.layoutGenre
import kotlinx.android.synthetic.main.fragment_editplaylist.layoutPermissions
import kotlinx.android.synthetic.main.fragment_editplaylist.tvGenre
import kotlinx.android.synthetic.main.fragment_editplaylist.tvPermissions
import kotlinx.android.synthetic.main.fragment_editplaylist.tvTopTitle
import kotlinx.android.synthetic.main.fragment_editplaylist.viewOverlay
import timber.log.Timber

class EditPlaylistFragment : Fragment() {

    private val viewModel: EditPlaylistViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editplaylist, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val mode = arguments?.getSerializable(ARG_MODE) as EditPlaylistMode?
            ?: throw IllegalStateException("No mode specified in arguments")

        val data = arguments?.getParcelable(ARG_DATA) as? AddToPlaylistModel

        initViews()
        initClickListeners()
        initViewModelObservers()

        context?.let { context ->
            viewModel.init(mode, data, viewStateProvider, AMGenreProvider(context))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Timber.tag(TAG).i("onActivityResult: $requestCode, $resultCode, $data")

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQ_PICK_GALLERY_IMAGE -> {
                viewModel.saveGalleryImage(
                    SaveImageUseCaseImpl(context),
                    data?.data,
                    viewModel.imageFile
                )
            }
            REQ_PICK_GALLERY_BANNER -> {
                viewModel.saveGalleryImage(
                    SaveImageUseCaseImpl(context),
                    data?.data,
                    viewModel.bannerFile
                )
            }
            REQ_PICK_CAMERA_IMAGE -> {
                cropImage()
            }
            REQ_PICK_CAMERA_BANNER -> {
                showBannerFromFile()
            }
            REQ_CROP_IMAGE -> {
                onImageCropped()
            }
        }
    }

    private fun initViews() {
        etName.addTextChangedListener(HintTextWatcher(etName))
        etName.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable?) {
                viewModel.onTitleChange(s?.toString())
            }
        })

        etDescription.addTextChangedListener(HintTextWatcher(etDescription))
        etDescription.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable?) {
                viewModel.onDescriptionChange(s?.toString())
            }
        })

        initBannerView()
    }

    private fun initClickListeners() {
        layoutGenre.setOnClickListener { viewModel.onGenreClick() }
        layoutPermissions.setOnClickListener { viewModel.onPermissionsClick() }

        buttonClose.setOnClickListener { viewModel.onBackClick() }
        buttonDelete.setOnClickListener { viewModel.onDeleteClick() }
        buttonSave.setOnClickListener { viewModel.onSaveClick() }

        imageViewBanner.setImageClickListener { viewModel.onEditBannerClick() }
        buttonBanner.setOnClickListener { viewModel.onEditBannerClick() }
        editImageButton.setOnClickListener { viewModel.onEditImageClick() }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            mode.observe(viewLifecycleOwner, modeObserver)
            title.observe(viewLifecycleOwner, titleObserver)
            genre.observe(viewLifecycleOwner, genreObserver)
            description.observe(viewLifecycleOwner, descriptionObserver)
            banner.observe(viewLifecycleOwner, bannerObserver)
            smallImage.observe(viewLifecycleOwner, smallImageObserver)
            private.observe(viewLifecycleOwner, privacyToggleObserver)
            bannerImageBase64.observe(viewLifecycleOwner, bannerBase64Observer)

            createdEvent.observe(viewLifecycleOwner, playlistCreatedObserver)
            editedEvent.observe(viewLifecycleOwner, playlistEditedObserver)
            deletedEvent.observe(viewLifecycleOwner, playlistDeletedObserver)
            errorEvent.observe(viewLifecycleOwner, playlistErrorObserver)
            changeEvent.observe(viewLifecycleOwner, playlistChangeObserver)

            progressEvent.observe(viewLifecycleOwner, progressVisibilityObserver)
            hideKeyboardEvent.observe(viewLifecycleOwner, keyboardObserver)
            editBannerEvent.observe(viewLifecycleOwner, editBannerObserver)
            editImageEvent.observe(viewLifecycleOwner, editImageObserver)
            saveBannerEvent.observe(viewLifecycleOwner, saveBannerObserver)
            imageSavedEvent.observe(viewLifecycleOwner, imageSavedObserver)
            deletePromptEvent.observe(viewLifecycleOwner, deletePromptObserver)
            showBannerEvent.observe(viewLifecycleOwner) { showBannerFromFile() }
            cropImageEvent.observe(viewLifecycleOwner) { cropImage() }
        }
    }

    private fun initBannerView() {
        imageViewBanner
            .configureOverlay()
            .setAspectRatio(AspectRatio(3, 1))
            .setShouldDrawGrid(false)
            .setDynamicCrop(false)
            .setCropScale(1f)
            .setBorderColor(Color.TRANSPARENT)
            .apply()

        imageViewBanner
            .configureImage()
            .setImageInitialPosition(InitialPosition.CENTER_CROP)
            .setImageScaleEnabled(true)
            .setImageTranslationEnabled(true)
            .setMaxScale(4f)
            .apply()

        imageViewBanner.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        imageViewBanner.setCropSaveCompleteListener { bitmapUri ->
            activity?.contentResolver?.openInputStream(bitmapUri)?.let {
                viewModel.onBannerImageCreated(it)
            }
        }

        imageViewBanner.setErrorListener { error ->
            Timber.tag(TAG).e(error, "CropIwaView.ErrorListener")
            viewModel.onBannerSaveError(error)
        }
    }

    private val viewStateProvider = object : EditPlaylistViewStateProvider {
        override fun isViewAvailable() = isAdded
        override fun getTitle() = etName.text.toString().trim()
        override fun getGenre() = tvGenre.text.toString()
        override fun getDesc() = etDescription.text.toString()
        override fun isBannerVisible() = buttonBanner.isVisible
    }

    private val modeObserver = Observer<EditPlaylistMode> { mode ->
        val edit = mode == EditPlaylistMode.EDIT

        tvTopTitle.setText(if (edit) R.string.editplaylist_title else R.string.editplaylist_create_title)
        buttonBanner.setText(if (edit) R.string.editaccount_banner else R.string.editplaylist_create_banner)

        buttonClose.visibility = if (edit) View.VISIBLE else View.GONE
        buttonDelete.visibility = if (edit) View.VISIBLE else View.GONE
    }

    private val bannerObserver = Observer<String> { playlistBannerUrl ->
        context?.let { context ->
            PicassoImageLoader.load(context, playlistBannerUrl, imageViewBanner)
            viewOverlay.setBackgroundColor(viewOverlay.context.colorCompat(if (playlistBannerUrl.isEmpty()) R.color.profile_bg else R.color.black_alpha50))
        }
    }

    private val titleObserver = Observer<String> { playlistTitle ->
        etName.setText(playlistTitle)
        etName.setSelection(playlistTitle.length)
    }

    private val genreObserver = Observer<String> { playlistGenre ->
        tvGenre.text = playlistGenre
    }

    private val descriptionObserver = Observer<String> { description ->
        etDescription.setText(description)
    }

    private val smallImageObserver = Observer<String> { playlistImageUrl ->
        context?.let { context ->
            PicassoImageLoader.load(context, playlistImageUrl, imageViewAvatar)
        }
    }

    private val playlistCreatedObserver = Observer<AMResultItem> { playlist ->
        playlist.tracks?.let { tracks ->
            if (tracks.size == 1) {
                val title = tracks[0]?.title
                AMSnackbar.Builder(activity)
                    .withTitle(getString(R.string.add_to_playlist_success, title))
                    .withDrawable(R.drawable.ic_snackbar_playlist)
                    .show()
            }
        }
    }

    private val playlistEditedObserver = Observer<AMResultItem> { playlist ->
        MainApplication.playlist = playlist
        AMSnackbar.Builder(activity)
            .withTitle(getString(R.string.edit_playlist_success, playlist.title))
            .withDrawable(R.drawable.ic_snackbar_playlist)
            .show()
    }

    private val saveBannerObserver = Observer<File> { bannerFile ->
        context?.let { context ->
            val bannerUri = FileProvider.getUriForFile(
                context,
                AUTHORITY,
                bannerFile
            )
            imageViewBanner.crop(
                CropIwaSaveConfig.Builder(bannerUri)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setQuality(90)
                    .build()
            )
        }
    }

    private val imageSavedObserver = Observer<File> { imageFile ->
        Picasso.get().load(imageFile).into(imageViewAvatar)
    }

    private val deletePromptObserver = Observer<String> { playlistTitle ->
        activity?.let { context ->
            val title = context.spannableString(
                fullString = getString(R.string.playlist_delete_title_template, playlistTitle),
                highlightedStrings = listOf(playlistTitle),
                highlightedColor = context.colorCompat(R.color.orange)
            )
            AMAlertFragment.show(
                context,
                title,
                getString(R.string.playlist_delete_message),
                getString(R.string.playlist_delete_yes),
                getString(R.string.playlist_delete_no),
                Runnable { viewModel.onDeleteConfirmed() },
                null,
                null
            )
        }
    }

    private val playlistDeletedObserver = Observer<AMResultItem> { playlist ->
        AMSnackbar.Builder(activity)
            .withTitle(getString(R.string.playlist_delete_succeeded_template, playlist.title))
            .withDrawable(R.drawable.ic_snackbar_playlist)
            .show()
    }

    private val keyboardObserver = Observer<Void> {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(etName.windowToken, 0)
    }

    private val editBannerObserver = Observer<Void> {
        editImage(true)
    }

    private val editImageObserver = Observer<Void> {
        editImage(false)
    }

    private val playlistErrorObserver =
        Observer<EditPlaylistException> { error ->
            Timber.tag(TAG).e(error.throwable, "Edit playlist error")
            val info = when (error.type) {
                EditPlaylistException.Type.CREATE ->
                    Pair(R.string.add_to_playlist_error, true)
                EditPlaylistException.Type.EDIT ->
                    Pair(R.string.edit_playlist_error, true)
                EditPlaylistException.Type.DELETE ->
                    Pair(R.string.playlist_delete_failed, true)
                EditPlaylistException.Type.TITLE ->
                    Pair(R.string.add_to_playlist_error_no_name, false)
                EditPlaylistException.Type.BANNER ->
                    Pair(R.string.edit_playlist_banner_error, true)
            }
            val builder = AMSnackbar.Builder(activity)
                .withTitle(getString(info.first))
                .withDrawable(R.drawable.ic_snackbar_error)
                .withSecondary(R.drawable.ic_snackbar_playlist_grey)

            if (info.second) {
                builder.withSubtitle(getString(R.string.please_try_again_later))
            }

            builder.show()
        }

    private val playlistChangeObserver = Observer<Void> {
        buttonSave.isEnabled = true
    }

    private val progressVisibilityObserver = Observer<Boolean> { showProgress ->
        if (showProgress) {
            AMProgressHUD.showWithStatus(activity)
        } else {
            AMProgressHUD.dismiss()
        }
    }

    private val privacyToggleObserver = Observer<Boolean> { private ->
        tvPermissions.setText(if (private) R.string.add_to_playlist_permissions_private else R.string.add_to_playlist_permissions_public)
        imageViewPermissions.visibility = if (private) View.INVISIBLE else View.VISIBLE
    }

    private val bannerBase64Observer = Observer<String> {
        viewModel.onBannerSaved()
    }

    private fun editImage(banner: Boolean) {
        context?.let {
            AlertDialog.Builder(it, R.style.AudiomackAlertDialog)
                .setMessage(R.string.imagepicker_message)
                .setPositiveButton(R.string.imagepicker_camera) { _, _ ->
                    requestCameraPermissions(banner)
                }
                .setNegativeButton(R.string.imagepicker_gallery) { _, _ ->
                    requestGalleryPermissions(banner)
                }
                .setNeutralButton(R.string.imagepicker_gallery_cancel, null)
                .setCancelable(false)
                .show()
        }
    }

    private fun requestCameraPermissions(banner: Boolean) {
        context?.let { context ->
            val cameraPermission = Manifest.permission.CAMERA
            val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            val cameraGranted = ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED
            val storageGranted = ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
            if (!cameraGranted || !storageGranted) {

                if (!shouldShowRequestPermissionRationale(cameraPermission) && !shouldShowRequestPermissionRationale(storagePermission)) {
                    val requestCode =
                        if (banner) REQ_PERMISSION_BANNER_CAMERA_PERMISSIONS else REQ_PERMISSION_IMAGE_CAMERA_PERMISSIONS
                    requestPermissions(arrayOf(cameraPermission, storagePermission), requestCode)
                    if (!cameraGranted) {
                        viewModel.onPermissionRequested(PermissionType.Camera)
                    }
                    if (!storageGranted) {
                        viewModel.onPermissionRequested(PermissionType.Storage)
                    }
                } else {
                    if (shouldShowRequestPermissionRationale(storagePermission)) {
                        showPermissionRationaleDialog(PermissionType.Storage)
                    } else if (shouldShowRequestPermissionRationale(cameraPermission)) {
                        showPermissionRationaleDialog(PermissionType.Camera)
                    }
                }
            } else {
                launchCameraIntent(banner)
            }
        }
    }

    private fun requestGalleryPermissions(banner: Boolean) {
        context?.let { context ->
            val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            val storageGranted = ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
            if (!storageGranted) {
                if (shouldShowRequestPermissionRationale(storagePermission)) {
                    showPermissionRationaleDialog(PermissionType.Storage)
                } else {
                    val requestCode =
                        if (banner) REQ_PERMISSION_BANNER_GALLERY_PERMISSIONS else REQ_PERMISSION_IMAGE_GALLERY_PERMISSIONS
                    requestPermissions(arrayOf(storagePermission), requestCode)
                    viewModel.onPermissionRequested(PermissionType.Storage)
                }
            } else {
                launchGalleryIntent(banner)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        context?.let {
            viewModel.onPermissionsEnabled(it, permissions, grantResults)
        }

        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            when (requestCode) {
                REQ_PERMISSION_IMAGE_CAMERA_PERMISSIONS -> launchCameraIntent(false)
                REQ_PERMISSION_BANNER_CAMERA_PERMISSIONS -> launchCameraIntent(true)
                REQ_PERMISSION_IMAGE_GALLERY_PERMISSIONS -> launchGalleryIntent(false)
                REQ_PERMISSION_BANNER_GALLERY_PERMISSIONS -> launchGalleryIntent(true)
            }
        }
    }

    private fun launchGalleryIntent(banner: Boolean) {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                .setType("image/*")
            val reqCode = if (banner) REQ_PICK_GALLERY_BANNER else REQ_PICK_GALLERY_IMAGE
            startActivityForResult(intent, reqCode)
        } catch (e: ActivityNotFoundException) {
            Timber.w(e)
        }
    }

    private fun launchCameraIntent(banner: Boolean) {
        val file = (if (banner) viewModel.bannerFile else viewModel.imageFile) ?: return

        context?.let { context ->
            val uri = FileProvider.getUriForFile(
                context,
                AUTHORITY,
                file
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, uri)

            val reqCode = if (banner) REQ_PICK_CAMERA_BANNER else REQ_PICK_CAMERA_IMAGE
            startActivityForResult(intent, reqCode)
        }
    }

    private fun cropImage() {
        val imageFile = viewModel.imageFile ?: return

        context?.let { context ->
            val destination = FileProvider.getUriForFile(
                context,
                AUTHORITY,
                imageFile
            )

            val cropIntent = Intent("com.android.camera.action.CROP").apply {
                setDataAndType(destination, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                putExtra(MediaStore.EXTRA_OUTPUT, destination)
                putExtra("crop", "true")
                putExtra("aspectX", 1)
                putExtra("aspectY", 1)
                putExtra("outputX", AMResultItem.PLAYLIST_IMAGE_MAX_SIZE_PX)
                putExtra("outputY", AMResultItem.PLAYLIST_IMAGE_MAX_SIZE_PX)
            }

            try {
                startActivityForResult(cropIntent, REQ_CROP_IMAGE)
            } catch (exception: ActivityNotFoundException) {
                Timber.w(exception)
                AMSnackbar.Builder(activity)
                    .withTitle(getString(R.string.unsupported_crop_error))
                    .withDrawable(R.drawable.ic_snackbar_error)
                    .show()
            }
        }
    }

    private fun onImageCropped() {
        viewModel.apply {
            smallImage.value?.let { imageUrl ->
                Picasso.get().invalidate(imageUrl)
            }
            imageFile?.let { file ->
                Picasso.get().invalidate(file)
            }
            onPlaylistImageCreated()
        }
    }

    private fun showBannerFromFile() {
        val bannerFile = viewModel.bannerFile ?: return

        context?.let { context ->
            val contentUri = FileProvider.getUriForFile(
                context,
                AUTHORITY,
                bannerFile
            )
            imageViewBanner.setImageUri(contentUri)
            viewOverlay.setBackgroundColor(viewOverlay.context.colorCompat(R.color.black_alpha50))
            buttonBanner.visibility = View.GONE
        }
    }

    class HintTextWatcher(private val view: TextView) : TextWatcherAdapter() {
        override fun afterTextChanged(s: Editable?) {
            if (s?.isEmpty() == true) {
                view.typeface =
                    ResourcesCompat.getFont(view.context, R.font.opensans_italic)
            } else {
                view.typeface =
                    ResourcesCompat.getFont(view.context, R.font.opensans_regular)
            }
        }
    }

    companion object {
        private const val TAG = "EditPlaylistFragment"

        const val ARG_MODE = "mode"
        const val ARG_DATA = "data"

        private const val REQ_PICK_GALLERY_IMAGE = 0
        private const val REQ_PICK_CAMERA_IMAGE = 1
        private const val REQ_PICK_GALLERY_BANNER = 2
        private const val REQ_PICK_CAMERA_BANNER = 3
        private const val REQ_CROP_IMAGE = 4
        private const val REQ_PERMISSION_IMAGE_CAMERA_PERMISSIONS = 5
        private const val REQ_PERMISSION_BANNER_CAMERA_PERMISSIONS = 6
        private const val REQ_PERMISSION_IMAGE_GALLERY_PERMISSIONS = 7
        private const val REQ_PERMISSION_BANNER_GALLERY_PERMISSIONS = 8

        @JvmOverloads
        fun newInstance(mode: EditPlaylistMode = EditPlaylistMode.EDIT, data: AddToPlaylistModel? = null) =
            EditPlaylistFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_MODE, mode)
                    putParcelable(ARG_DATA, data)
                }
            }
    }
}
