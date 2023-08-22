package com.audiomack.data.share

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.audiomack.BuildConfig
import com.audiomack.CONTACT_US_URL
import com.audiomack.R
import com.audiomack.data.bitmap.BitmapManager
import com.audiomack.data.bitmap.BitmapManagerImpl
import com.audiomack.data.device.DeviceRepository
import com.audiomack.data.storage.DB_AUDIOMACK
import com.audiomack.data.storage.Storage
import com.audiomack.data.storage.StorageProvider
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.data.tracking.appsflyer.AppsFlyerDataSource
import com.audiomack.data.tracking.appsflyer.AppsFlyerRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.AMComment
import com.audiomack.model.AMResultItem
import com.audiomack.model.Artist
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.Credentials
import com.audiomack.model.MixpanelSource
import com.audiomack.model.Music
import com.audiomack.model.ScreenshotModel
import com.audiomack.model.ShareMethod
import com.audiomack.network.API
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.screenshot.ScreenshotActivity
import com.audiomack.ui.screenshot.ShareStoryModel
import com.audiomack.utils.Utils
import com.audiomack.views.AMProgressHUD
import com.audiomack.views.AMSnackbar
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.ShareStoryContent
import com.facebook.share.widget.ShareDialog
import com.google.android.material.snackbar.Snackbar
import com.snapchat.kit.sdk.SnapCreative
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitCompletionCallback
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitSendError
import com.snapchat.kit.sdk.creative.models.SnapPhotoContent
import com.twitter.sdk.android.core.Twitter
import com.twitter.sdk.android.core.TwitterAuthConfig
import com.twitter.sdk.android.core.TwitterConfig
import com.twitter.sdk.android.tweetcomposer.TweetComposer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.Locale
import okio.buffer
import okio.sink
import timber.log.Timber

class ShareManagerImpl(
    private val bitmapManager: BitmapManager = BitmapManagerImpl(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val appsFlyerDataSource: AppsFlyerDataSource = AppsFlyerRepository(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val trackingDataSource: TrackingDataSource = TrackingRepository(),
    private val storage: Storage = StorageProvider.getInstance()
) : ShareManager {

    override fun copyMusicLink(activity: Activity?, item: AMResultItem?, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        if (item != null) {
            copyLink(activity, item.link)
            mixpanelDataSource.trackShareContent(ShareMethod.CopyLink, null, item, null, null, mixpanelSource, mixpanelButton)
            appsFlyerDataSource.trackShareContent()
        }
    }

    override fun copyArtistink(activity: Activity?, artist: AMArtist?, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        if (artist != null) {
            copyLink(activity, artist.link)
            mixpanelDataSource.trackShareContent(ShareMethod.CopyLink, artist, null, null, null, mixpanelSource, mixpanelButton)
            appsFlyerDataSource.trackShareContent()
        }
    }

    private fun copyLink(activity: Activity?, link: String?) {
        if (activity != null && link != null) {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: run { return }
            val clip = ClipData.newPlainText("link", link)
            clipboard.setPrimaryClip(clip)
            AMSnackbar.Builder(activity)
                .withDrawable(R.drawable.ic_snackbar_link)
                .withTitle(activity.getString(R.string.share_link_copied))
                .withDuration(Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun shareText(activity: Activity?, message: String?, url: String?, method: ShareMethod) {

        if (message == null && url == null) return

        if (method === ShareMethod.SMS) {
            shareViaSms(activity, message)
            return
        }

        if (method === ShareMethod.Twitter) {
            val sharedOnTwitter =
                shareViaTwitter(activity, message)
            if (sharedOnTwitter) {
                return
            }
        }

        if (method == ShareMethod.Facebook) {
            val sharedOnFacebook =
                shareViaFacebook(activity, url)
            if (sharedOnFacebook) {
                return
            }
        }

        shareViaOther(activity, message)
    }

    private fun shareViaSms(activity: Activity?, message: String?) {
        if (activity != null) {
            try {
                val defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(activity)
                val sendIntent = Intent(Intent.ACTION_SEND)
                sendIntent.type = "text/plain"
                sendIntent.putExtra(Intent.EXTRA_TEXT, message)
                if (defaultSmsPackageName != null) {
                    sendIntent.setPackage(defaultSmsPackageName)
                }
                activity.startActivity(sendIntent)
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }

    private fun shareViaTwitter(activity: Activity?, message: String?): Boolean {
        if (activity != null && message != null && Utils.isTwitterAppInstalled) {
            val twitterConfig = TwitterConfig.Builder(activity)
                .twitterAuthConfig(
                    TwitterAuthConfig(
                        BuildConfig.AM_TWITTER_CONSUMER_KEY,
                        BuildConfig.AM_TWITTER_CONSUMER_SECRET
                    )
                )
                .build()
            Twitter.initialize(twitterConfig)

            val builder = TweetComposer.Builder(activity).text(message)
            builder.show()
            return true
        }
        return false
    }

    private fun shareViaFacebook(activity: Activity?, url: String?): Boolean {
        try {
            activity?.let {
                val content = ShareLinkContent.Builder()
                    .setContentUrl(Uri.parse(url))
                    .build()
                ShareDialog.show(it, content)
                return true
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
        return false
    }

    private fun shareViaOther(activity: Activity?, message: String?) {
        if (activity != null) {
            try {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, message)
                val chooser = Intent.createChooser(intent, "Share")
                activity.startActivity(chooser)
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }

    override fun shareArtist(activity: Activity?, artist: AMArtist?, method: ShareMethod, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        if (artist == null) {
            return
        }

        mixpanelDataSource.trackShareContent(method, artist, null, null, null, mixpanelSource, mixpanelButton)
        appsFlyerDataSource.trackShareContent()

        shareText(activity, artist.link, artist.link, method)
    }

    override fun shareScreenshot(activity: Activity?, music: AMResultItem?, artist: AMArtist?, method: ShareMethod, benchmark: BenchmarkModel?, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        ScreenshotActivity.show(
            activity,
            ScreenshotModel(
                benchmark ?: BenchmarkModel(),
                mixpanelSource,
                mixpanelButton,
                music?.let { Music(it) },
                artist?.let { Artist(it) }
            )
        )

        mixpanelDataSource.trackShareContent(method, artist, music, null, null, mixpanelSource, mixpanelButton)
        appsFlyerDataSource.trackShareContent()
    }

    override fun shareStory(activity: Activity?, music: AMResultItem?, artist: AMArtist?, method: ShareMethod, mixpanelSource: MixpanelSource, mixpanelButton: String, compositeDisposable: CompositeDisposable) {

        if (music == null && artist == null) {
            return
        }

        if (activity != null) {

            mixpanelDataSource.trackShareContent(method, artist, music, null, null, mixpanelSource, mixpanelButton)
            appsFlyerDataSource.trackShareContent()

            val message = activity.resources.getString(R.string.permissions_rationale_alert_storage_message)
            val positive = activity.resources.getString(R.string.permissions_rationale_alert_settings)

            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder(activity, R.style.AudiomackAlertDialog)
                        .setMessage(message)
                        .setNegativeButton(activity.resources.getString(R.string.permissions_rationale_alert_cancel), null)
                        .setPositiveButton(positive) { _, _ ->
                            try {
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                intent.data = Uri.fromParts("package", activity.packageName, null)
                                activity.startActivity(intent)
                            } catch (e: Exception) {
                                Timber.w(e)
                            }
                        }.show()
                } else {
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                }
            } else {

                val imageUrl = artist?.largeImage ?: music?.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetOriginal)
                val contentUrl = artist?.link ?: music?.link ?: "https://audiomack.com"

                compositeDisposable.add(
                    (bitmapManager.createStickerUri(
                        activity,
                        imageUrl,
                        music?.title,
                        artist?.name ?: music?.artist,
                        music?.featured,
                        Bitmap.CompressFormat.PNG,
                        "stickerBitmap.png",
                        method != ShareMethod.Snapchat
                    )).concatWith(
                        bitmapManager.createBackgroundUri(
                            activity,
                            imageUrl,
                            Bitmap.CompressFormat.JPEG,
                            "backgroundBitmap.png",
                            method != ShareMethod.Snapchat
                        )
                    ).toList()
                        .subscribeOn(schedulersProvider.main)
                        .observeOn(schedulersProvider.main)
                        .subscribe({ list ->

                            val stickerUri = list[0]
                            val backgroundUri = list[1]

                            val story = ShareStoryModel(
                                contentUrl,
                                method,
                                stickerUri,
                                backgroundUri
                            )
                            when (method) {
                                ShareMethod.Facebook -> shareFacebookStory(activity, story)
                                ShareMethod.Instagram -> shareInstagramStory(activity, story)
                                ShareMethod.Snapchat -> shareSnapchatStory(activity, story)
                                        else -> { }
                            }
                        }, { throwable ->
                            trackingDataSource.trackException(throwable)
                        })
                )
            }
        }
    }

    private fun shareInstagramStory(activity: Activity?, model: ShareStoryModel) {

        val packageName = "com.instagram.android"

        val stickerUri = model.stickerUri
        val backgroundUri = model.backgroundUri
        val contentUrl = model.contentUrl

        try {

            val intent = Intent("com.instagram.share.ADD_TO_STORY")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.setDataAndType(backgroundUri, "image/*")
            intent.putExtra("interactive_asset_uri", stickerUri)
            intent.putExtra("content_url", contentUrl)

            activity?.let { notNullActivity ->

                if (!isAppInstalled(notNullActivity, packageName)) {
                    return
                }

                notNullActivity.grantUriPermission(packageName, stickerUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                if (notNullActivity.packageManager.resolveActivity(intent, 0) != null) {
                    notNullActivity.startActivityForResult(intent, HomeActivity.REQ_CODE_INSTAGRAM_SHARE)
                } else {
                    val dialog = AlertDialog.Builder(notNullActivity, R.style.AudiomackAlertDialog)
                        .setMessage(notNullActivity.resources.getString(R.string.permissions_rationale_alert_instagram_message))
                        .setPositiveButton(notNullActivity.resources.getString(R.string.ok), null)
                        .create()
                    dialog.show()
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
            trackingDataSource.trackException(e)
        }
    }

    private fun shareFacebookStory(activity: Activity?, model: ShareStoryModel) {

        val stickerUri = model.stickerUri
        val backgroundUri = model.backgroundUri
        val contentUrl = model.contentUrl

        activity?.let { notNullActivity ->

            if (!isAppInstalled(notNullActivity, "com.facebook.katana")) {
                return
            }

            val backgroundAsset = SharePhoto.Builder().setImageUrl(backgroundUri).build()
            val stickerAsset = SharePhoto.Builder().setImageUrl(stickerUri).build()
            val content = ShareStoryContent.Builder().setBackgroundAsset(backgroundAsset).setStickerAsset(stickerAsset).setAttributionLink(contentUrl).build()

            ShareDialog.show(notNullActivity, content)
        }
    }

    private fun shareSnapchatStory(activity: Activity?, model: ShareStoryModel) {

        val stickerUri = model.stickerUri
        val backgroundUri = model.backgroundUri
        val contentUrl = model.contentUrl
        val stickerPath = stickerUri.path
        val photoPath = backgroundUri.path

        activity?.let { notNullActivity ->

            if (!isAppInstalled(notNullActivity, "com.snapchat.android")) {
                return
            }

            if (!stickerPath.isNullOrEmpty() && !photoPath.isNullOrEmpty()) {

                val inputStream = notNullActivity.contentResolver.openInputStream(stickerUri)

                inputStream?.let { notNullInputStream ->

                    val snapPhoto = SnapCreative.getMediaFactory(notNullActivity).getSnapPhotoFromFile(File(photoPath))

                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeStream(notNullInputStream, null, options)

                    val imageHeight = options.outHeight
                    val imageWidth = options.outWidth
                    notNullInputStream.close()

                    val snapSticker = SnapCreative.getMediaFactory(notNullActivity).getSnapStickerFromFile(File(stickerPath)).apply {
                        this.setHeight(imageHeight.toFloat())
                        this.setWidth(imageWidth.toFloat())
                    }

                    val snapContent = SnapPhotoContent(snapPhoto).apply {
                        this.snapSticker = snapSticker
                        this.attachmentUrl = contentUrl
                    }

                    SnapCreative.getApi(notNullActivity).sendWithCompletionHandler(snapContent, object : SnapCreativeKitCompletionCallback {
                        override fun onSendSuccess() {}

                        override fun onSendFailed(error: SnapCreativeKitSendError?) {
                            error?.let {
                                trackingDataSource
                                    .trackException(Exception("Snapchat share error: ${error.name}"))
                            }
                        }
                    })
                }
            }
        }
    }

    override fun shareLink(activity: Activity?, music: AMResultItem?, artist: AMArtist?, method: ShareMethod, mixpanelSource: MixpanelSource, mixpanelButton: String) {

        val contentUrl = artist?.link ?: music?.link ?: "https://audiomack.com"

        mixpanelDataSource.trackShareContent(method, artist, music, null, null, mixpanelSource, mixpanelButton)
        appsFlyerDataSource.trackShareContent()

        val packageName = when (method) {
            ShareMethod.WhatsApp -> "com.whatsapp"
            ShareMethod.Messenger -> "com.facebook.orca"
            ShareMethod.WeChat -> "com.tencent.mm"
            else -> ""
        }

        activity?.let {
            if (isAppInstalled(it, packageName)) {
                try {
                    it.startActivity(
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            setPackage(packageName)
                            putExtra(Intent.EXTRA_TEXT, contentUrl)
                            type = "text/plain"
                        }
                    )
                } catch (e: Exception) {
                    if (e is ActivityNotFoundException) {
                        if (method == ShareMethod.Messenger) {
                            AMSnackbar.Builder(activity)
                                .withTitle(it.getString(R.string.share_messenger_login_error))
                                .withDrawable(R.drawable.ic_snackbar_facebook_error)
                                .show()
                        }
                    } else {
                        Timber.tag(ShareManagerImpl::class.java.simpleName)
                            .d("Unable to start 'shareLink' via $packageName")
                    }
                }
            }
        }
    }

    override fun shareMusic(
        activity: Activity?,
        item: AMResultItem?,
        method: ShareMethod,
        mixpanelSource: MixpanelSource,
        mixpanelButton: String,
        compositeDisposable: CompositeDisposable
    ) {
        if (item == null) {
            return
        }

        mixpanelDataSource.trackShareContent(method, null, item, null, null, mixpanelSource, mixpanelButton)
        appsFlyerDataSource.trackShareContent()

        val needToFetchFromAPI =
            item.isAlbumTrack && (item.uploaderSlug == null || item.urlSlug == null) || method === ShareMethod.Twitter

        val listener = object : API.GetInfoListener {
            override fun onSuccess(item: AMResultItem) {
                AMProgressHUD.dismiss()

                var message = item.link
                if (method == ShareMethod.Twitter) {
                    item.uploaderTwitter.takeIf { !it.isNullOrEmpty() }?.let { message = "$message by @$it" } ?: item.uploaderName.takeIf { !it.isNullOrEmpty() }?.let { message = "$message by @$it" }
                }
                val url = item.link

                shareText(activity, message, url, method)
            }

            override fun onFailure(statusCode: Int) {
                AMProgressHUD.showWithError(activity, activity?.getString(R.string.song_info_failed))
            }
        }

        if (needToFetchFromAPI) {
            AMProgressHUD.showWithStatus(activity)
            compositeDisposable.add(
                item.refreshInfo()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        listener.onSuccess(it)
                    }, {
                        listener.onFailure(-1)
                    })
            )
        } else {
            listener.onSuccess(item)
        }
    }

    override fun openSupport(context: Context) {

        try {

            val currentDB = File(storage.databaseDir, DB_AUDIOMACK)
            val backupDB = File(storage.shareDir, "export.db")
            backupDB.parentFile.mkdirs()
            Utils.copy(currentDB, backupDB)

            val dump = File(storage.shareDir, "dump.txt")
            dump.delete()
            val fileSink = dump.sink()
            val bufferedSink = fileSink.buffer()
            val musicFilesDir = storage.offlineDir
            val files = musicFilesDir?.listFiles()
            if (files != null && files.isNotEmpty()) {
                for (file in files) {
                    if (file.isDirectory) {
                        bufferedSink.writeUtf8(file.absoluteFile.toString() + "\n")
                        val subFiles = file.listFiles()
                        if (subFiles != null && subFiles.isNotEmpty()) {
                            for (subFile in subFiles) {
                                bufferedSink.writeUtf8("    " + subFile.absoluteFile.toString() + " ( " + subFile.length() + " )\n")
                            }
                        }
                    } else {
                        bufferedSink.writeUtf8(file.absoluteFile.toString() + " ( " + file.length() + " )\n")
                    }
                }
            }
            bufferedSink.close()
            fileSink.close()

            var username: String? = null
            val deviceId: String?
            val appVersion = DeviceRepository.getAppVersionFull()
            val phoneModel = Build.MODEL
            val osVersion = Build.VERSION.RELEASE
            var accountString: String? = null
            val country = Locale.getDefault().displayCountry
            val credentials = Credentials.load(context)
            if (credentials != null) {
                username = credentials.userUrlSlug
                accountString = String.format(
                    "Account login email: %s\nAccount ID: %s\n",
                    if (TextUtils.isEmpty(credentials.email)) "" else credentials.email,
                    credentials.userId
                )
                deviceId = Credentials.load(context)!!.deviceId
            } else {
                deviceId = Credentials.generateDeviceId(context)
            }

            val emailBody = String.format(
                "\n\n\n\n\n\n\n\n\n\nType the issue you are having above. Do not edit below this line.\n" + "==============================\nApp Version: %s\nPhone Model: %s\nOS Version: %s\n%sCountry: %s",
                appVersion, phoneModel, osVersion, accountString, country
            )

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@audiomack.com"))
            intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "Audiomack App Support - " + if (TextUtils.isEmpty(username)) deviceId else username
            )
            intent.putExtra(Intent.EXTRA_TEXT, emailBody)
            val uriDB = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                backupDB
            )
            val uriDump = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                dump
            )
            val uris = ArrayList<Uri>()
            uris.add(uriDB)
            uris.add(uriDump)
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            context.startActivity(Intent.createChooser(intent, "Send email..."))
        } catch (anfe: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(CONTACT_US_URL)
                    )
                )
            } catch (e: Exception) {
                Timber.tag(ShareManagerImpl::class.java.simpleName)
                    .d("Unable to start 'contact us' URL intent")
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    override fun shareCommentLink(activity: Activity?, comment: AMComment, item: AMResultItem, mixpanelSource: MixpanelSource, mixpanelButton: String) {
        item.link?.let { link ->
            val builder = Uri.parse(link).buildUpon().appendQueryParameter("comment", comment.uuid)
            if (!comment.threadUuid.isNullOrEmpty()) {
                builder.appendQueryParameter("thread", comment.threadUuid)
            }
            mixpanelDataSource.trackShareContent(ShareMethod.Standard, null, item, comment, null, mixpanelSource, mixpanelButton)
            appsFlyerDataSource.trackShareContent()
            shareViaOther(activity, builder.build().toString())
        }
    }

    private fun isAppInstalled(activity: Activity, packageName: String): Boolean {
        return try {
            activity.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (exception: PackageManager.NameNotFoundException) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
            false
        }
    }
}
