package com.audiomack.ui.editaccount

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
import android.text.SpannableString
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.audiomack.BuildConfig
import com.audiomack.R
import com.audiomack.data.storage.AUTHORITY
import com.audiomack.data.user.AccountSaveException
import com.audiomack.model.AMArtist
import com.audiomack.model.PermissionType
import com.audiomack.model.SocialNetwork
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.common.Resource
import com.audiomack.ui.editaccount.EditAccountViewModel.TextData
import com.audiomack.ui.webviewauth.WebViewAuthConfigurationFactory
import com.audiomack.ui.webviewauth.WebViewAuthManager
import com.audiomack.usecases.SaveImageUseCaseImpl
import com.audiomack.utils.BitmapUtils
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.showPermissionRationaleDialog
import com.audiomack.utils.spannableStringWithImageAtTheEnd
import com.audiomack.views.AMProgressHUD
import com.audiomack.views.AMSnackbar
import com.squareup.picasso.Picasso
import com.steelkiwi.cropiwa.AspectRatio
import com.steelkiwi.cropiwa.config.CropIwaSaveConfig
import com.steelkiwi.cropiwa.config.InitialPosition
import kotlinx.android.synthetic.main.fragment_editaccount.buttonBanner
import kotlinx.android.synthetic.main.fragment_editaccount.buttonClose
import kotlinx.android.synthetic.main.fragment_editaccount.buttonFacebook
import kotlinx.android.synthetic.main.fragment_editaccount.buttonInstagram
import kotlinx.android.synthetic.main.fragment_editaccount.buttonSave
import kotlinx.android.synthetic.main.fragment_editaccount.buttonTwitter
import kotlinx.android.synthetic.main.fragment_editaccount.buttonYoutube
import kotlinx.android.synthetic.main.fragment_editaccount.editImageButton
import kotlinx.android.synthetic.main.fragment_editaccount.etBio
import kotlinx.android.synthetic.main.fragment_editaccount.etHometown
import kotlinx.android.synthetic.main.fragment_editaccount.etLabel
import kotlinx.android.synthetic.main.fragment_editaccount.etName
import kotlinx.android.synthetic.main.fragment_editaccount.etSlug
import kotlinx.android.synthetic.main.fragment_editaccount.etSlugLayout
import kotlinx.android.synthetic.main.fragment_editaccount.etWebsite
import kotlinx.android.synthetic.main.fragment_editaccount.imageViewAvatar
import kotlinx.android.synthetic.main.fragment_editaccount.imageViewBanner
import kotlinx.android.synthetic.main.fragment_editaccount.tvBioCounter
import kotlinx.android.synthetic.main.fragment_editaccount.tvFacebook
import kotlinx.android.synthetic.main.fragment_editaccount.tvFollowers
import kotlinx.android.synthetic.main.fragment_editaccount.tvFollowing
import kotlinx.android.synthetic.main.fragment_editaccount.tvInstagram
import kotlinx.android.synthetic.main.fragment_editaccount.tvName
import kotlinx.android.synthetic.main.fragment_editaccount.tvPlays
import kotlinx.android.synthetic.main.fragment_editaccount.tvPlaysLabel
import kotlinx.android.synthetic.main.fragment_editaccount.tvTwitter
import kotlinx.android.synthetic.main.fragment_editaccount.tvYoutube
import timber.log.Timber

class EditAccountFragment : Fragment() {

    private var changedFields: MutableList<Any> = mutableListOf()
    private var originalArtist: AMArtist? = null
    private val viewModel: EditAccountViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_editaccount, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModelObservers()
        initClickListeners()
        initTextChangedListeners()
        configureImageViewBanner()

        viewModel.onCreate()
    }

    private fun configureImageViewBanner() {
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
    }

    private fun initTextChangedListeners() {
        etName.addTextChangedListener(textWatcher)
        etLabel.addTextChangedListener(textWatcher)
        etHometown.addTextChangedListener(textWatcher)
        etWebsite.addTextChangedListener(textWatcher)
        etBio.addTextChangedListener(textWatcher)
        etBio.addTextChangedListener(bioTextWatcher)
        etSlug.addTextChangedListener(urlSlugTextWatcher)
    }

    private fun initClickListeners() {
        buttonTwitter.setOnClickListener { viewModel.onTwitterTapped() }
        buttonInstagram.setOnClickListener { viewModel.onInstagramTapped() }
        buttonFacebook.setOnClickListener { viewModel.onFacebookTapped() }
        buttonYoutube.setOnClickListener { viewModel.onYoutubeTapped() }
        imageViewBanner.setImageClickListener { viewModel.onEditBannerTapped() }
        editImageButton.setOnClickListener { viewModel.onEditAvatarTapped() }
        buttonBanner.setOnClickListener { viewModel.onEditBannerTapped() }
        buttonClose.setOnClickListener { viewModel.onCloseTapped() }

        buttonSave.setOnClickListener {

            val saveRunnable = Runnable {

                viewModel.onSaveTapped(
                    etName.text.toString().trim(),
                    etSlug.text.toString().trim(),
                    etLabel.text.toString().trim(),
                    etHometown.text.toString().trim(),
                    etWebsite.text.toString().trim(),
                    etBio.text.toString().trim()
                )
            }

            if (buttonBanner.visibility != View.VISIBLE) {
                val motionEvent = MotionEvent.obtain(0, 1, MotionEvent.ACTION_UP, 0f, 0f, 0)
                imageViewBanner.dispatchTouchEvent(motionEvent)
                imageViewBanner.setCropSaveCompleteListener { bitmapUri ->
                    try {
                        activity?.let {
                            val imageStream = it.contentResolver.openInputStream(bitmapUri)
                            imageStream?.let {
                                viewModel.onBannerPicked(BitmapUtils.inputStreamToBase64(imageStream))
                                changedFields.add(imageViewBanner)
                                refreshSaveButton(true)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e)
                    }

                    saveRunnable.run()
                }
                imageViewBanner.setErrorListener {
                    AMSnackbar.Builder(activity)
                        .withTitle(getString(R.string.editaccount_error_banner))
                        .withDrawable(R.drawable.ic_snackbar_error)
                        .show()
                    saveRunnable.run()
                }
                context?.let {
                    val bannerUri = FileProvider.getUriForFile(
                        it,
                        AUTHORITY,
                        viewModel.imageFile
                    )
                    imageViewBanner.postDelayed({
                        imageViewBanner?.crop(
                            CropIwaSaveConfig.Builder(bannerUri)
                                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                                .setQuality(90)
                                .build()
                        )
                    }, 200)
                }
            } else {
                saveRunnable.run()
            }
        }

        imageViewBanner.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
    }

    private fun initViewModelObservers() {
        viewModel.apply {

            artist.observe(viewLifecycleOwner, artistObserver)

            displayName.observe(viewLifecycleOwner, displayNameObserver)
            verifiedName.observe(viewLifecycleOwner, verifiedNameObserver)
            tastemakerName.observe(viewLifecycleOwner, tastemakerNameObserver)
            authenticatedName.observe(viewLifecycleOwner, authenticatedNameObserver)

            imageUrl.observe(viewLifecycleOwner, imageUrlObserver)
            bannerUrl.observe(viewLifecycleOwner, bannerUrlObserver)

            name.observe(viewLifecycleOwner, nameObserver)
            label.observe(viewLifecycleOwner, labelObserver)
            hometown.observe(viewLifecycleOwner, hometownObserver)
            url.observe(viewLifecycleOwner, urlObserver)
            bio.observe(viewLifecycleOwner, bioObserver)
            bioCounter.observe(viewLifecycleOwner, bioCounterObserver)
            urlSlug.observe(viewLifecycleOwner, urlSlugObserver)
            followersExtended.observe(viewLifecycleOwner, followersExtendedObserver)
            followingExtended.observe(viewLifecycleOwner, followingExtendedObserver)
            playsExtended.observe(viewLifecycleOwner, playsExtendedObserver)
            playsCount.observe(viewLifecycleOwner, playsCountObserver)
            text.observe(viewLifecycleOwner, textObserver)
            authentication.observe(viewLifecycleOwner, authenticationObserver)

            twitter.observe(viewLifecycleOwner, twitterObserver)
            twitterLinked.observe(viewLifecycleOwner, twitterLinkedObserver)
            instagram.observe(viewLifecycleOwner, instagramObserver)
            instagramLinked.observe(viewLifecycleOwner, instagramLinkedObserver)
            facebook.observe(viewLifecycleOwner, facebookObserver)
            facebookLinked.observe(viewLifecycleOwner, facebookLinkedObserver)
            youtube.observe(viewLifecycleOwner, youtubeObserver)
            youtubeLinked.observe(viewLifecycleOwner, youtubeLinkedObserver)

            closeEvent.observe(viewLifecycleOwner, closeEventObserver)
            hideKeyboardEvent.observe(viewLifecycleOwner, hideKeyboardEventObserver)
            showLoaderEvent.observe(viewLifecycleOwner, showLoaderEventObserver)
            hideLoaderEvent.observe(viewLifecycleOwner, hideLoaderEventObserver)
            showErrorEvent.observe(viewLifecycleOwner, showErrorEventObserver)
            showGenericErrorEvent.observe(viewLifecycleOwner, showGenericErrorEventObserver)
            refreshSaveButtonEvent.observe(viewLifecycleOwner, refreshSaveButtonEventObserver)
            showInstagramWebViewEvent.observe(viewLifecycleOwner, showInstagramWebViewEventObserver)
            showAlreadyLinkedEvent.observe(viewLifecycleOwner, showAlreadyLinkedEventObserver)

            showFilePickerTypeAlertEvent.observe(viewLifecycleOwner, showFilePickerTypeAlertEventObserver)
            requestCameraEvent.observe(viewLifecycleOwner, requestCameraEventObserver)
            requestGalleryEvent.observe(viewLifecycleOwner, requestGalleryEventObserver)
            showBannerEvent.observe(viewLifecycleOwner) { showBannerFromFile() }
            cropImageEvent.observe(viewLifecycleOwner) { cropImage() }
        }
    }

    private val showFilePickerTypeAlertEventObserver = Observer<Void> {
        activity?.let {
            AlertDialog.Builder(it, R.style.AudiomackAlertDialog)
                .setMessage(getString(R.string.imagepicker_message))
                .setPositiveButton(getString(R.string.imagepicker_camera)) { _, _ -> viewModel.onCameraRequested() }
                .setNegativeButton(getString(R.string.imagepicker_gallery)) { _, _ -> viewModel.onGalleryRequested() }
                .setNeutralButton(getString(R.string.imagepicker_gallery_cancel), null)
                .setCancelable(false)
                .show()
        }
    }

    private val requestCameraEventObserver = Observer<Void> {
        context?.let { context ->
            val cameraPermission = Manifest.permission.CAMERA
            val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            val cameraGranted = ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED
            val storageGranted = ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
            if (!cameraGranted || !storageGranted) {
                if (!shouldShowRequestPermissionRationale(cameraPermission) && !shouldShowRequestPermissionRationale(storagePermission)) {
                    requestPermissions(arrayOf(cameraPermission, storagePermission), REQ_PERMISSION_CAMERA_PERMISSIONS)
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
                startCameraIntent()
            }
        }
    }

    private val requestGalleryEventObserver = Observer<Void> {
        context?.let { context ->
            val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            val storageGranted = ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
            if (!storageGranted) {
                if (shouldShowRequestPermissionRationale(storagePermission)) {
                    showPermissionRationaleDialog(PermissionType.Storage)
                } else {
                    requestPermissions(arrayOf(storagePermission), REQ_PERMISSION_STORAGE_PERMISSIONS)
                    viewModel.onPermissionRequested(PermissionType.Storage)
                }
            } else {
                startGalleryIntent()
            }
        }
    }

    private val artistObserver = Observer<AMArtist> {
        originalArtist = it
    }

    private val twitterObserver = Observer<String> {
        val twitterSet = !it.isNullOrEmpty()
        tvTwitter.text = if (twitterSet) it else getString(R.string.connect_social_twitter)
        tvTwitter.setTextColor(tvTwitter.context.colorCompat(if (twitterSet) R.color.orange else R.color.gray_text))
    }

    private val twitterLinkedObserver = Observer<Boolean> {
        buttonTwitter.setImageResource(if (it) R.drawable.ic_social_link_twitter_connected else R.drawable.ic_social_link_twitter)
        buttonTwitter.isClickable = !it
    }

    private val facebookObserver = Observer<String> {
        val facebookSet = !it.isNullOrEmpty()
        tvFacebook.text = if (facebookSet) it else getString(R.string.connect_social_facebook)
        tvFacebook.setTextColor(tvFacebook.context.colorCompat(if (facebookSet) R.color.orange else R.color.gray_text))
    }

    private val facebookLinkedObserver = Observer<Boolean> {
        buttonFacebook.setImageResource(if (it) R.drawable.ic_social_link_facebook_connected else R.drawable.ic_social_link_facebook)
        buttonFacebook.isClickable = !it
    }

    private val instagramObserver = Observer<String> {
        val instagramSet = !it.isNullOrEmpty()
        tvInstagram.text = if (instagramSet) it else getString(R.string.connect_social_instagram)
        tvInstagram.setTextColor(tvInstagram.context.colorCompat(if (instagramSet) R.color.orange else R.color.gray_text))
    }

    private val instagramLinkedObserver = Observer<Boolean> {
        buttonInstagram.setImageResource(if (it) R.drawable.ic_social_link_instagram_connected else R.drawable.ic_social_link_instagram)
        buttonInstagram.isClickable = !it
    }

    private val youtubeObserver = Observer<String> {
        val youtubeSet = !it.isNullOrEmpty()
        tvYoutube.text = if (youtubeSet) it else getString(R.string.connect_social_youtube)
        tvYoutube.setTextColor(tvYoutube.context.colorCompat(if (youtubeSet) R.color.orange else R.color.gray_text))
    }

    private val youtubeLinkedObserver = Observer<Boolean> {
        buttonYoutube.setImageResource(if (it) R.drawable.ic_social_link_youtube_connected else R.drawable.ic_social_link_youtube)
        buttonYoutube.isClickable = !it
    }

    private val imageUrlObserver = Observer<String> {
        viewModel.onLoadAvatarImageView(context, it, imageViewAvatar)
    }

    private val bannerUrlObserver = Observer<String> {
        imageViewBanner.post {
            viewModel.onLoadBannerCropView(context, it, imageViewBanner)
        }
    }

    private val displayNameObserver = Observer<String> {
        tvName.text = it
    }

    private val verifiedNameObserver = Observer<String> {
        tvName.spannableStringWithImageAtTheEnd(
            it,
            R.drawable.ic_verified,
            16
        )
    }

    private val tastemakerNameObserver = Observer<String> {
        tvName.spannableStringWithImageAtTheEnd(
            it,
            R.drawable.ic_tastemaker,
            16
        )
    }

    private val authenticatedNameObserver = Observer<String> {
        tvName.spannableStringWithImageAtTheEnd(
            it,
            R.drawable.ic_authenticated,
            16
        )
    }

    private val nameObserver = Observer<String> {
        etName.setText(it)
        etName.setSelection(it.length)
    }

    private val labelObserver = Observer<String> {
        etLabel.setText(it)
        etLabel.setSelection(it.length)
    }

    private val hometownObserver = Observer<String> {
        etHometown.setText(it)
        etHometown.setSelection(it.length)
    }

    private val urlObserver = Observer<String> {
        etWebsite.setText(it)
        etWebsite.setSelection(it.length)
    }

    private val bioObserver = Observer<String> {
        etBio.setText(it)
        etBio.setSelection(it.length)
    }

    private val bioCounterObserver = Observer<String> {
        tvBioCounter.text = it
    }

    private val urlSlugObserver = Observer<Resource<String>> { resource ->
        when (resource) {
            is Resource.Success -> {
                resource.data?.let { newValue ->
                    etSlugLayout.isErrorEnabled = false
                    etSlugLayout.error = ""
                    etSlug.removeTextChangedListener(urlSlugTextWatcher)
                    etSlug.setText(newValue)
                    etSlug.setSelection(newValue.length)
                    etSlug.addTextChangedListener(urlSlugTextWatcher)
                    originalArtist?.urlSlug?.let { viewModel.onTextChanged(etSlug, newValue, it) }
                }
            }
            is Resource.Failure -> {
                resource.error?.let { saveError ->
                    etSlugLayout.isErrorEnabled = true
                    etSlugLayout.error = saveError.message
                }
            }
        }
    }

    private val followersExtendedObserver = Observer<String> {
        tvFollowers.text = it
    }

    private val followingExtendedObserver = Observer<String> {
        tvFollowing.text = it
    }

    private val playsExtendedObserver = Observer<String> {
        tvPlays.text = it
    }

    private val playsCountObserver = Observer<Long> {
        tvPlays.visibility = if (it == 0L) View.GONE else View.VISIBLE
        tvPlaysLabel.visibility = if (it == 0L) View.GONE else View.VISIBLE
    }

    private val textObserver = Observer<TextData> {
        if (it.newValue != it.originalValue) {
            if (!changedFields.contains(it.editText)) changedFields.add(it.editText)
        } else {
            if (changedFields.contains(it.editText)) changedFields.remove(it.editText)
        }
        refreshSaveButton(changedFields.isNotEmpty())
    }

    private val authenticationObserver = Observer<SocialNetwork> { network ->
        activity?.let {
            viewModel.onLinkSocial(it, network)
        }
    }

    private val showLoaderEventObserver = Observer<Void> {
        AMProgressHUD.showWithStatus(activity)
    }

    private val hideLoaderEventObserver = Observer<Void> {
        AMProgressHUD.dismiss()
    }

    private val showErrorEventObserver = Observer<AccountSaveException> { saveError ->
        context?.let {
            AlertDialog.Builder(it, R.style.AudiomackAlertDialog)
                .setTitle(saveError.title)
                .setMessage(saveError.message)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .show()
        }
    }

    private val showGenericErrorEventObserver = Observer<Void> {
        context?.let {
            AlertDialog.Builder(it, R.style.AudiomackAlertDialog)
                .setTitle("")
                .setMessage(it.getString(R.string.generic_api_error))
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .show()
        }
    }

    private val refreshSaveButtonEventObserver = Observer<Boolean> {
        refreshSaveButton(it)
    }

    private val showInstagramWebViewEventObserver = Observer<Void> {
        WebViewAuthManager(
            childFragmentManager,
            "Instagram",
            WebViewAuthConfigurationFactory().createInstagramConfiguration(
                BuildConfig.AM_INSTAGRAM_APP_ID,
                BuildConfig.AM_INSTAGRAM_REDIRECT_URL
            )
        ) { result ->
            viewModel.handleInstagramResult(result)
        }.show()
    }

    private val showAlreadyLinkedEventObserver = Observer<SocialNetwork> {
        val activity = activity ?: return@Observer
        if (it == SocialNetwork.Twitter || it == SocialNetwork.Instagram) {
            AMAlertFragment.show(
                activity,
                SpannableString(getString(R.string.social_link_error_already_linked_title)),
                getString(if (it == SocialNetwork.Twitter) R.string.social_link_error_already_linked_message_twitter else R.string.social_link_error_already_linked_message_instagram),
                getString(R.string.ok),
                null,
                null,
                null,
                null
            )
        }
    }

    private val closeEventObserver = Observer<Void> {
        activity?.finish()
    }

    private val hideKeyboardEventObserver = Observer<Void> {
        context?.let {
            (it.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(etName.windowToken, 0)
        }
    }

    private fun startCameraIntent() {
        context?.let { context ->
            val uri = FileProvider.getUriForFile(
                context,
                AUTHORITY,
                viewModel.imageFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, uri)
            val reqCode = if (viewModel.editingMode == EditAccountViewModel.EditingMode.Avatar) {
                REQ_PICK_CAMERA_IMAGE
            } else {
                REQ_PICK_CAMERA_BANNER
            }
            startActivityForResult(intent, reqCode)
        }
    }

    private fun startGalleryIntent() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                .setType("image/*")
            val reqCode = if (viewModel.editingMode == EditAccountViewModel.EditingMode.Avatar) {
                REQ_PICK_GALLERY_IMAGE
            } else {
                REQ_PICK_GALLERY_BANNER
            }
            startActivityForResult(intent, reqCode)
        } catch (e: ActivityNotFoundException) {
            Timber.w(e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        context?.let {
            viewModel.onPermissionsEnabled(it, permissions, grantResults)
        }
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            when (requestCode) {
                REQ_PERMISSION_CAMERA_PERMISSIONS -> startCameraIntent()
                REQ_PERMISSION_STORAGE_PERMISSIONS -> startGalleryIntent()
            }
        }
    }

    private val bioTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            viewModel.onBioChanged(s.toString())
        }
    }

    private val urlSlugTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(editable: Editable?) {
            if (editable != null && editable.toString().isNotEmpty()) {
                val newValue = editable.toString()
                viewModel.onUrlSlugChanged(newValue)
            }
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(editable: Editable?) {
            if (editable != null && editable.toString().isNotEmpty()) {
                val newValue = editable.toString()
                when (editable.hashCode()) {
                    etName.text.hashCode() -> originalArtist?.name?.let {
                        viewModel.onTextChanged(etName, newValue, it)
                    }
                    etLabel.text.hashCode() -> originalArtist?.label?.let {
                        viewModel.onTextChanged(etLabel, newValue, it)
                    }
                    etHometown.text.hashCode() -> originalArtist?.hometown?.let {
                        viewModel.onTextChanged(etHometown, newValue, it)
                    }
                    etWebsite.text.hashCode() -> originalArtist?.url?.let {
                        viewModel.onTextChanged(etWebsite, newValue, it)
                    }
                    etBio.text.hashCode() -> originalArtist?.bio?.let {
                        viewModel.onTextChanged(etBio, newValue, it)
                    }
                }
            }
        }
    }

    private fun refreshSaveButton(active: Boolean) {
        if (active) {
            buttonSave.isClickable = true
            buttonSave.setTextColor(Color.WHITE)
        } else {
            buttonSave.isClickable = false
            buttonSave.setTextColor(Color.GRAY)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.onActivityResult(requestCode, resultCode, data)

        Timber.tag(TAG).i("onActivityResult: $requestCode, $resultCode, $data")

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQ_PICK_GALLERY_IMAGE, REQ_PICK_GALLERY_BANNER -> {
                viewModel.saveGalleryImage(
                    SaveImageUseCaseImpl(context),
                    data?.data
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

    private fun cropImage() {
        context?.let { context ->
            val destination = FileProvider.getUriForFile(
                context,
                AUTHORITY,
                viewModel.imageFile
            )

            val cropIntent = Intent("com.android.camera.action.CROP").apply {
                setDataAndType(destination, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                putExtra(MediaStore.EXTRA_OUTPUT, destination)
                putExtra("crop", "true")
                putExtra("aspectX", 1)
                putExtra("aspectY", 1)
                putExtra("outputX", AMArtist.AVATAR_MAX_SIZE_PX)
                putExtra("outputY", AMArtist.AVATAR_MAX_SIZE_PX)
            }
            try {
                startActivityForResult(cropIntent, REQ_CROP_IMAGE)
            } catch (anfe: ActivityNotFoundException) {
                Timber.w(anfe)
                AMSnackbar.Builder(activity)
                    .withTitle(getString(R.string.unsupported_crop_error))
                    .withDrawable(R.drawable.ic_snackbar_error)
                    .show()
            }
        }
    }

    private fun onImageCropped() {
        val avatar = BitmapUtils.fileToBase64(viewModel.imageFile, AMArtist.AVATAR_MAX_SIZE_PX)

        Picasso.get().invalidate(viewModel.imageFile)
        Picasso.get().load(viewModel.imageFile).into(imageViewAvatar)

        viewModel.onAvatarPicked(avatar)
        changedFields.add(imageViewAvatar)
        refreshSaveButton(true)
    }

    private fun showBannerFromFile() {
        context?.let { context ->
            val contentUri = FileProvider.getUriForFile(
                context,
                AUTHORITY,
                viewModel.imageFile
            )
            imageViewBanner.setImageUri(contentUri)
            buttonBanner.visibility = View.GONE
            changedFields.add(imageViewBanner)
            refreshSaveButton(true)
        }
    }

    companion object {
        private const val TAG = "EditAccountFragment"

        private const val REQ_PICK_GALLERY_IMAGE = 0
        private const val REQ_PICK_CAMERA_IMAGE = 1
        private const val REQ_PICK_GALLERY_BANNER = 2
        private const val REQ_PICK_CAMERA_BANNER = 3
        private const val REQ_CROP_IMAGE = 4
        private const val REQ_PERMISSION_CAMERA_PERMISSIONS = 5
        private const val REQ_PERMISSION_STORAGE_PERMISSIONS = 6

        fun newInstance(): EditAccountFragment {
            return EditAccountFragment()
        }
    }
}
