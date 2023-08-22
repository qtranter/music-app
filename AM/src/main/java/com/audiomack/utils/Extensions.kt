package com.audiomack.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.UnderlineSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFavoriteResult
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.actions.ToggleRepostResult
import com.audiomack.data.reachability.Reachability
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserRepository
import com.audiomack.fragments.DataDownloadsFragment
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventDeletedDownload
import com.audiomack.model.EventDownload
import com.audiomack.model.EventHighlightsUpdated
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.PermissionType
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.artist.ArtistFragment
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.ui.highlights.EditHighlightsActivity
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.notifications.preferences.NotificationsPreferencesActivity
import com.audiomack.usecases.LoginAlertUseCase
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.views.AMSnackbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

typealias Second = Long
fun Second.toMilliseconds(): Millisecond = times(1000L)

typealias Millisecond = Long
fun Millisecond.toSeconds(): Long = div(1000L)

typealias Url = String

val String.featArtists: List<String>
get() {
    return split(" and ").map { it.split(" y ").map { it.split(" & ").map { it.split(",") }.flatten() }.flatten() }.flatten().map { it.trim() }
}

fun JSONObject.getStringOrNull(name: String): String? = if (isNull(name)) null else optString(name)

fun JSONObject.getIntOrNull(name: String): Int? = if (isNull(name)) null else optInt(name)

fun JSONObject.getLongOrNull(name: String): Long? = if (isNull(name)) null else optLong(name)

fun JSONArray.getJSONObjectOrNull(index: Int): JSONObject? =
    if (isNull(index)) null else optJSONObject(index)

fun JSONArray.getStringOrNull(index: Int): String? = if (isNull(index)) null else optString(index)

fun JSONObject.putIfNotNull(name: String, value: Long?) {
    value?.let {
        put(name, it)
    }
}

fun JSONObject.putIfNotNull(name: String, value: String?) {
    value?.let {
        put(name, it)
    }
}

fun Context.showAlert(message: String, onPositiveButtonClicked: (() -> Unit)? = null) {
    try {
        val spannableStringMessage = SpannableString(message)
        try {
            Linkify.addLinks(spannableStringMessage, Linkify.ALL)
        } catch (e: Exception) {
            // Prevent MissingWebViewPackageException
            Timber.w(e)
        }
        val dialog = AlertDialog.Builder(this, R.style.AudiomackAlertDialog)
            .setMessage(spannableStringMessage)
            .setPositiveButton(getString(R.string.ok)) { _, _ -> onPositiveButtonClicked?.invoke() }
            .create()
        dialog.show()
    } catch (e: Exception) {
        Timber.w(e)
    }
}

fun Fragment.showOfflineAlert() {
    if (isAdded) {
        AlertDialog.Builder(activity, R.style.AudiomackAlertDialog)
            .setMessage(getString(R.string.feature_not_available_offline_alert_message))
            .setPositiveButton(getString(R.string.feature_not_available_offline_alert_button), null)
            .show()
    }
}

fun FragmentActivity.showOfflineAlert() {
    if (!isFinishing) {
        AlertDialog.Builder(this, R.style.AudiomackAlertDialog)
            .setMessage(getString(R.string.feature_not_available_offline_alert_message))
            .setPositiveButton(getString(R.string.feature_not_available_offline_alert_button), null)
            .show()
    }
}

fun Fragment.showFollowedToast(followNotify: ToggleFollowResult.Notify) {
    if (isAdded) {
        val otherString = followNotify.uploaderName
        val fullString = getString(if (followNotify.followed) R.string.player_account_followed else R.string.player_account_unfollowed, otherString)
        val openArtist = { followNotify.uploaderUrlSlug.let { if (it.isNotEmpty()) HomeActivity.instance?.homeViewModel?.onArtistScreenRequested(it) } }
        val spannableTitle = context?.let {
            it.spannableString(
                fullString = fullString,
                highlightedStrings = listOf(otherString),
                highlightedColor = it.colorCompat(R.color.orange),
                highlightedFont = R.font.opensans_bold,
                clickableSpans = listOf(AMClickableSpan(it) { openArtist() })
            )
        } ?: SpannableString(fullString)
        val builder = AMSnackbar.Builder(activity)
            .withSpannableTitle(spannableTitle)
            .withImageClickListener(View.OnClickListener { openArtist() })
        followNotify.uploaderImage.let { if (it.isNotEmpty()) builder.withImageUrl(it) else builder.withDrawable(R.drawable.ic_snackbar_user) }
        builder.show()
    }
}

fun Fragment.showDownloadUnlockedToast(musicName: String) {
    if (isAdded) {
        AMSnackbar.Builder(activity)
            .withTitle(getString(R.string.premium_limited_download_unlock_toast, musicName))
            .withDrawable(R.drawable.ic_snackbar_download_success)
            .show()
    }
}

fun Activity.showAddedToQueueToast() {
    AMSnackbar.Builder(this)
        .withTitle(getString(R.string.queue_added))
        .withDrawable(R.drawable.ic_snackbar_queue)
        .show()
}

fun Context.showFavoritedToast(notify: ToggleFavoriteResult.Notify) {
    when {
        notify.wantedToFavorite && notify.isSuccessful -> {
            AMSnackbar.Builder(HomeActivity.instance)
                .withTitle(getString(if (notify.isAlbum) R.string.toast_favorited_album else if (notify.isPlaylist) R.string.toast_favorited_playlist else R.string.toast_favorited_song))
                .withSubtitle(if (notify.title.isNotEmpty() && notify.artist.isNotEmpty()) notify.artist + " - " + notify.title else "")
                .withDrawable(R.drawable.ic_snackbar_favorite)
                .show()
        }
        notify.wantedToFavorite && !notify.isSuccessful -> {
            AMSnackbar.Builder(HomeActivity.instance)
                .withTitle(getString(if (notify.isAlbum) R.string.toast_favorited_album_error else if (notify.isPlaylist) R.string.toast_favorited_playlist_error else R.string.toast_favorited_song_error))
                .withSubtitle(getString(R.string.please_check_connection_try_again))
                .withDrawable(R.drawable.ic_snackbar_error)
                .withSecondary(R.drawable.ic_snackbar_favorite_grey)
                .show()
        }
        !notify.wantedToFavorite && notify.isSuccessful -> {
            AMSnackbar.Builder(HomeActivity.instance)
                .withTitle(getString(if (notify.isAlbum) R.string.toast_unfavorited_album else if (notify.isPlaylist) R.string.toast_unfavorited_playlist else R.string.toast_unfavorited_song))
                .withSubtitle(if (notify.title.isNotEmpty() && notify.artist.isNotEmpty()) notify.artist + " - " + notify.title else "")
                .withDrawable(R.drawable.ic_snackbar_favorite)
                .show()
        }
        !notify.wantedToFavorite && !notify.isSuccessful -> {
            AMSnackbar.Builder(HomeActivity.instance)
                .withTitle(getString(if (notify.isAlbum) R.string.toast_unfavorited_album_error else if (notify.isPlaylist) R.string.toast_unfavorited_playlist_error else R.string.toast_unfavorited_song_error))
                .withSubtitle(getString(R.string.please_check_connection_try_again))
                .withDrawable(R.drawable.ic_snackbar_error)
                .withSecondary(R.drawable.ic_snackbar_favorite_grey)
                .show()
        }
    }
}

fun Context.showRepostedToast(notify: ToggleRepostResult.Notify) {
    if (notify.isSuccessful) {
        AMSnackbar.Builder(HomeActivity.instance)
            .withTitle(getString(if (notify.isAlbum) R.string.toast_reposted_album else R.string.toast_reposted_song))
            .withSubtitle(if (notify.title.isNotEmpty() && notify.artist.isNotEmpty()) notify.artist + " - " + notify.title else "")
            .withDrawable(R.drawable.ic_snackbar_repost)
            .show()
    } else {
        AMSnackbar.Builder(HomeActivity.instance)
            .withTitle(getString(if (notify.isAlbum) R.string.toast_reposted_album_error else R.string.toast_reposted_song_error))
            .withSubtitle(getString(R.string.please_check_connection_try_again))
            .withDrawable(R.drawable.ic_snackbar_error)
            .withSecondary(R.drawable.ic_snackbar_repost_grey)
            .show()
    }
}

fun Fragment.showLoggedOutAlert(source: LoginSignupSource) {
    val activity = activity ?: return
    AMAlertFragment.show(
        activity,
        SpannableString(LoginAlertUseCase().getMessage(activity)),
        null,
        getString(R.string.login_needed_yes),
        getString(R.string.login_needed_no),
        Runnable { AuthenticationActivity.show(activity, source, null) },
        Runnable { UserRepository.getInstance().onLoginCanceled() },
        Runnable { UserRepository.getInstance().onLoginCanceled() }
    )
}

fun Fragment.showReachedLimitOfHighlightsAlert() {
    val activity = activity ?: return
    AMAlertFragment.show(
        activity,
        SpannableString(getString(R.string.highlights_reached_limit_message)),
        null,
        getString(R.string.highlights_reached_limit_edit),
        getString(R.string.highlights_reached_limit_cancel),
        Runnable { EditHighlightsActivity.show(activity) },
        null,
        null
    )
}

fun Fragment.showHighlightErrorToast() {
    AMSnackbar.Builder(activity)
        .withTitle(getString(R.string.toast_highlight_error))
        .withSubtitle(getString(R.string.please_check_connection_try_again))
        .withDrawable(R.drawable.ic_snackbar_error)
        .withSecondary(R.drawable.ic_snackbar_highlight_grey)
        .show()
}

fun Fragment.showHighlightSuccessAlert(title: String) {
    val activity = activity ?: return
    AMAlertFragment.show(
        activity,
        activity.spannableString(
            fullString = getString(R.string.highlight_confirmation_title_template, title),
            highlightedStrings = listOf(title),
            highlightedColor = activity.colorCompat(R.color.orange)
        ),
        getString(R.string.highlight_confirmation_message),
        getString(R.string.highlight_confirmation_view),
        getString(R.string.highlight_confirmation_close),
        Runnable {
            if (activity is BaseActivity) {
                activity.closeOptionsFragment()
            }
            if (parentFragment is ArtistFragment) {
                (parentFragment as ArtistFragment).scrollToUploads()
                EventBus.getDefault().post(EventHighlightsUpdated())
            } else {
                UserRepository.getInstance().getUserSlug()?.let {
                    HomeActivity.instance?.homeViewModel?.onArtistScreenRequested(it, "uploads")
                }
            }
        },
        Runnable { EventBus.getDefault().post(EventHighlightsUpdated()) }
    )
}

fun Fragment.confirmDownloadDeletion(music: AMResultItem, successListener: (() -> Unit)? = null) {
    val activity = activity ?: return
    AMAlertFragment.show(
        activity,
        activity.spannableString(
            fullString = getString(R.string.download_delete_confirmation_title, music.title),
            highlightedStrings = listOf(music.title ?: ""),
            highlightedColor = activity.colorCompat(R.color.orange),
            highlightedFont = R.font.opensans_bold
        ),
        null,
        getString(R.string.download_delete_confirmation_yes),
        getString(R.string.download_delete_confirmation_no),
        {
            music.deepDelete()
            if (this !is DataDownloadsFragment || music.isAlbumTrack || music.isPlaylistTrack) {
                EventBus.getDefault().post(EventDownload(music.itemId, false))
            }
            EventBus.getDefault().post(EventDeletedDownload(music))
            successListener?.invoke()
        },
        null
    )
}

fun Fragment.confirmPlaylistSync(tracksCount: Int, syncRunnable: Runnable) {
    val activity = activity ?: return
    val connectedToWiFi = Reachability.getInstance().networkAvailable
    AMAlertFragment.show(
        activity,
        activity.spannableString(
            fullString = getString(R.string.playlist_download_sync_title, tracksCount.toString()),
            highlightedStrings = listOf(getString(R.string.playlist_download_sync_title_highlighted, tracksCount.toString())),
            highlightedColor = activity.colorCompat(R.color.orange)
        ),
        if (connectedToWiFi) null else getString(R.string.playlist_download_sync_message),
        getString(R.string.playlist_download_sync_yes),
        getString(R.string.playlist_download_sync_cancel),
        syncRunnable,
        null
    )
}

fun Fragment.confirmPlaylistDownloadDeletion(music: AMResultItem) {
    val activity = activity ?: return
    AMAlertFragment.show(
        activity,
        activity.spannableString(
            fullString = getString(R.string.playlist_delete_download_title),
            highlightedStrings = listOf(getString(R.string.playlist_delete_download_title_highlighted)),
            highlightedColor = activity.colorCompat(R.color.orange)
        ),
        getString(R.string.playlist_delete_download_message),
        getString(R.string.playlist_delete_download_yes),
        getString(R.string.playlist_delete_download_cancel),
        Runnable {
            music.deepDelete()
            EventBus.getDefault().post(EventDownload(music.itemId, false))
            EventBus.getDefault().post(EventDeletedDownload(music))
        },
        null
    )
}

fun Fragment.showFailedPlaylistDownload() {
    AMSnackbar.Builder(activity)
        .withTitle(getString(R.string.playlist_download_error))
        .withSubtitle(getString(R.string.please_check_connection_try_download_again))
        .withDrawable(R.drawable.ic_snackbar_download_failure)
        .show()
}

fun Context.openUrlExcludingAudiomack(urlString: String) {
    try {
        startActivity(
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER).apply {
                data = Uri.parse(urlString)
            }
        )
    } catch (e: Exception) {
        Timber.w(e)
    }
}

fun Context.openUrlInAudiomack(urlString: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlString)).apply {
            `package` = packageName
        })
    } catch (e: Exception) {
        openUrlExcludingAudiomack(urlString)
    }
}

fun Fragment.showPermissionRationaleDialog(permissionType: PermissionType) {
    val activity = activity ?: return
    val message = when (permissionType) {
        PermissionType.Storage -> {
            getString(R.string.permissions_rationale_alert_storage_message)
        }
        PermissionType.Camera -> {
            getString(R.string.permissions_rationale_alert_camera_message)
        }
        else -> ""
    }
    AMAlertFragment.show(
        activity,
        SpannableString(message),
        null,
        getString(R.string.permissions_rationale_alert_settings),
        getString(R.string.permissions_rationale_alert_cancel),
        Runnable {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", activity.packageName, null)
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Timber.e(e)
            }
        },
        null
    )
}

fun Fragment.scrollListToTop(resId: Int = R.id.recyclerView, position: Int = 0) {
    val recyclerView: RecyclerView? = this.view?.findViewById(resId)
    recyclerView?.scrollToPosition(position)
}

fun ViewPager.addOnPageSelectedListener(listener: (Int) -> Unit) {
    this.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            listener(position)
        }
    })
}

fun <T> MutableList<T>.move(from: Int, to: Int) {
    this.add(to, this.removeAt(from))
}

fun ViewPager.next(smoothScroll: Boolean = true) {
    this.setCurrentItem(this.currentItem.inc(), smoothScroll)
}

fun ViewPager.prev(smoothScroll: Boolean = true) {
    this.setCurrentItem(this.currentItem.dec(), smoothScroll)
}

fun NestedScrollView.setOnScrollYListener(listener: (Int) -> Unit) {
    this.setOnScrollChangeListener(
        NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            listener(scrollY)
        }
    )
}

/**
 * Helper extension to convert a potentially null [String] to a [Uri] falling back to [Uri.EMPTY]
 */
fun String?.toUri(): Uri = this?.let { Uri.parse(it) } ?: Uri.EMPTY

fun String?.isValidUrl(): Boolean = this?.let { URLUtil.isValidUrl(it) } ?: false

fun String?.isWebUrl(): Boolean =
    this?.let { URLUtil.isHttpUrl(it) || URLUtil.isHttpsUrl(it) } ?: false

fun String?.isFileUrl(): Boolean = this?.let { URLUtil.isFileUrl(it) } ?: false

/**
 * Creates a SpannableString with an image at the end of the text. The height parameter is used to resize the drawable.
 */
fun TextView.spannableStringWithImageAtTheEnd(string: String?, drawableResId: Int, heightDp: Int): SpannableString {
    val spannableString = SpannableString("${string ?: ""}   ")
    val drawable = context.drawableCompat(drawableResId) ?: return spannableString
    val heightPx = (heightDp * resources.displayMetrics.density).toInt()
    val widthPx = if (drawable.intrinsicHeight == 0) 0 else ((drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()) * heightPx.toFloat()).toInt()
    drawable.setBounds(0, 0, widthPx, heightPx)
    val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BASELINE)
    spannableString.setSpan(
        imageSpan,
        spannableString.length - 1,
        spannableString.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return spannableString
}

/**
 * A safe way to load a font
 * @param context
 * @param fontId
 * @return the desired font or the default one in case a android.content.res.Resources$NotFoundException is thrown
 */
fun Context.getTypefaceSafely(fontId: Int): Typeface {
    return try {
        ResourcesCompat.getFont(this, fontId) ?: Typeface.DEFAULT
    } catch (e: Exception) {
        Timber.w(e)
        Typeface.DEFAULT
    }
}

/**
 * Convenient way to apply horizontal padding on an EditText
 * @param padding: in dp
 */
fun EditText.setHorizontalPadding(padding: Int) {
    val paddingPx = (context.resources.displayMetrics.density * padding.toFloat()).roundToInt()
    setPadding(paddingPx, 0, paddingPx, 0)
}

/**
 * Convenient way to apply letterspacing padding on a TextView
 * @param value: as expressed in design tools (e.g. sketch)
 */
fun TextView.applyLetterspacing(value: Float) {
    letterSpacing = value / (textSize / resources.displayMetrics.density)
}

/**
 * Convenient way to convert a size from dp to px
 * @param dp: size expressed in dp
 * @return the converted size expressed in px
 */
fun Context.convertDpToPixel(dp: Float): Int {
    return (dp * resources.displayMetrics.density).roundToInt()
}

fun Context.getStatusBarHeight(): Int {
    val resourceId = this.resources.getIdentifier(
            "status_bar_height",
            "dimen",
            "android"
    )
    return if (resourceId > 0) {
        this.resources.getDimensionPixelSize(resourceId)
    } else {
        val rectangle = Rect()
        (this as? Activity)?.window?.decorView?.getWindowVisibleDisplayFrame(rectangle)
        rectangle.top
    }
}

/**
 * Convenient way to apply some alpha to a given color
 * @param factor: alpha value in [0,1]
 * @return the converted color as Int
 */
fun Int.adjustColorAlpha(factor: Float): Int {
    val alpha = (Color.alpha(this).toFloat() * factor).roundToInt()
    return Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
}

/**
 * Creates a SpannableString properly formatted.
 * @param fullString: the complete string
 * @param highlightedStrings: a list of strings to be highlighted
 * @param fullColor: color for the whole string, if null no changes will be applied
 * @param highlightedColor: color for the [highlightedStrings], if null no changes will be applied
 * @param fullFont: font for the whole string, if null no changes will be applied
 * @param highlightedFont: font for the [highlightedStrings], if null no changes will be applied
 * @param fullUnderline: whether or not the whole string must be underlined, if null no changes will be applied
 * @param highlightedFont: whether or not the [highlightedStrings] must be underlined, if null no changes will be applied
 * @param fullSize: font size for the whole string expressed in dp, if null no changes will be applied
 * @param highlightedSize: font size for the [highlightedStrings] expressed in dp, if null no changes will be applied
 * @param clickableSpans: list of click handlers for the [highlightedStrings], if null no changes will be applied
 * @return the spannable string
 */
fun Context.spannableString(
    fullString: String,
    highlightedStrings: List<String> = emptyList(),
    fullColor: Int? = null,
    highlightedColor: Int? = null,
    fullFont: Int? = null,
    highlightedFont: Int? = null,
    fullUnderline: Boolean = false,
    highlightedUnderline: Boolean = false,
    fullSize: Int? = null,
    highlightedSize: Int? = null,
    clickableSpans: List<ClickableSpan> = emptyList()
): SpannableString {

    require(clickableSpans.isEmpty() || clickableSpans.size == highlightedStrings.size) { "Invalid number of clickableSpans" }

    val spannableString = SpannableString(fullString)

    if (fullString.isEmpty()) {
        return spannableString
    }

    fullColor?.let {
        spannableString.setSpan(ForegroundColorSpan(it), 0, spannableString.length, 0)
    }
    fullFont?.let {
        spannableString.setSpan(
            CustomTypefaceSpan("", getTypefaceSafely(it)),
            0,
            spannableString.length,
            0
        )
    }
    fullSize?.let {
        spannableString.setSpan(AbsoluteSizeSpan(it, true), 0, spannableString.length, 0)
    }
    if (fullUnderline) {
        spannableString.setSpan(UnderlineSpan(), 0, spannableString.length, 0)
    }

    var start = 0
    highlightedStrings.forEachIndexed { i, highlightedString ->
        val clickableSpan = clickableSpans.getOrNull(i)
        if (highlightedString.isNotBlank()) {
            fullString.substring(start).toLowerCase(Locale.getDefault())
                .indexOf(highlightedString.toLowerCase(Locale.getDefault())).takeIf { it != -1 }
                ?.let { index ->
                    start += index
                    val end = min(start + highlightedString.length, fullString.length)
                    highlightedColor?.let {
                        spannableString.setSpan(ForegroundColorSpan(it), start, end, 0)
                    }
                    highlightedFont?.let {
                        spannableString.setSpan(
                            CustomTypefaceSpan("", getTypefaceSafely(it)),
                            start,
                            end,
                            0
                        )
                    }
                    highlightedSize?.let {
                        spannableString.setSpan(AbsoluteSizeSpan(it, true), start, end, 0)
                    }
                    if (highlightedUnderline) {
                        spannableString.setSpan(UnderlineSpan(), start, end, 0)
                    }
                    clickableSpan?.let {
                        spannableString.setSpan(it, start, end, 0)
                    }
                    start += highlightedString.length
                }
        }
    }

    return spannableString
}

fun Context.getScreenRealSize(): Point {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val screenResolution = Point()
    windowManager.defaultDisplay.getRealSize(screenResolution)
    return screenResolution
}

fun Spinner.setOnItemSelectedListener(listener: (Pair<Int, Long>) -> Unit) {
    this.onItemSelectedListener = object : OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            listener.invoke(Pair(position, id))
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
}

fun FragmentManager.isReady() = !this.isDestroyed && !this.isStateSaved

fun FragmentManager.lastBackStackEntry() = backStackEntryCount.takeIf { it > 0 }?.let { getBackStackEntryAt(it - 1) }

fun Disposable.addTo(compositeDisposable: CompositeDisposable) =
    this.also { compositeDisposable.add(it) }

fun Context?.hideKeyboard() =
    (this?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.takeIf { it.isActive }
        ?.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)

fun Fragment.askFollowNotificationPermissions(redirect: PermissionRedirect) {
    val activity = activity ?: return
    AMAlertFragment.Builder(activity)
        .title(SpannableString(getString(R.string.follow_artist_push_notification_permission_alert_title)))
        .solidButton(SpannableString(getString(R.string.follow_artist_push_notification_permission_alert_grant))) {
            MixpanelRepository().trackFollowPushPermissionPrompt(true)
            when (redirect) {
                PermissionRedirect.NotificationsManager -> startActivity(Intent(activity, NotificationsPreferencesActivity::class.java))
                PermissionRedirect.Settings -> startActivity(activity.intentForNotificationSettings())
            }
        }
        .plain1Button(SpannableString(getString(R.string.follow_artist_push_notification_permission_alert_cancel))) {
            MixpanelRepository().trackFollowPushPermissionPrompt(false)
        }
        .dismissWithoutSelectionHandler {
            MixpanelRepository().trackFollowPushPermissionPrompt(false)
        }
        .cancelHandler {
            MixpanelRepository().trackFollowPushPermissionPrompt(false)
        }
        .show(parentFragmentManager)
}

fun Context.intentForNotificationSettings() = Intent().apply {
    action = "android.settings.APP_NOTIFICATION_SETTINGS"
    putExtra("app_package", packageName)
    putExtra("app_uid", applicationInfo.uid)
    putExtra("android.provider.extra.APP_PACKAGE", packageName)
}

/**
 * Compares two strings that represent a version (e.g. "1.0.0" is lower than "1.2.0") based on the semantic versioning.
 * @param target: version name used for comparison, e.g. "1.2.0"
 * @return true if the current string is lower than target
 */
fun String.isVersionLowerThan(target: String): Boolean {
    val thisComponents = split(".").mapNotNull { component ->
        component.filter { it.isDigit() }.toIntOrNull()
    }
    val targetComponents = target.split(".").mapNotNull { component ->
        component.filter { it.isDigit() }.toIntOrNull()
    }

    (0 until kotlin.math.max(thisComponents.size, targetComponents.size)).forEach {
        if (thisComponents.getOrElse(it) { 0 } < targetComponents.getOrElse(it) { 0 }) {
            return true
        } else if (thisComponents.getOrElse(it) { 0 } > targetComponents.getOrElse(it) { 0 }) {
            return false
        }
    }
    return false
}

fun Uri.buildString(withBuilder: Uri.Builder.() -> Uri.Builder): String =
    buildUpon().withBuilder().build().toString()

fun String.buildUrl(withBuilder: Uri.Builder.() -> Uri.Builder): String =
    Uri.parse(this).buildString(withBuilder)

fun Int?.gt(other: Int): Boolean = this != null && this > other
fun Int?.gte(other: Int): Boolean = this != null && this >= other
fun Int?.lt(other: Int): Boolean = this != null && this < other
fun Int?.lte(other: Int): Boolean = this != null && this <= other
fun Int.takeIfPositive(): Int? = takeIf { this > 0 }
fun Int.takeIfZeroOrPositive(): Int? = takeIf { this >= 0 }
fun Int.takeIfNegative(): Int? = takeIf { this < 0 }
fun Int.takeIfZeroOrNegative(): Int? = takeIf { this <= 0 }

fun RecyclerView.addOnScrollChangeListener(listener: (Int) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            listener(newState)
        }
    })
}

fun <T> Comparable<T>?.nullSafeCompareTo(other: T?): Int {
    if (this == null || other == null) {
        if (this === other) return 0
        return if (this == null) -1 else 1
    }
    return this.compareTo(other)
}

fun String?.nullSafeCompareTo(other: String?, ignoreCase: Boolean = true): Int {
    if (this == null || other == null) {
        if (this === other) return 0
        return if (this == null) -1 else 1
    }
    return this.compareTo(other, ignoreCase)
}

fun CompoundButton.onCheckChanged(listener: (Boolean) -> Unit) {
    setOnCheckedChangeListener { _, isChecked -> listener(isChecked) }
}

fun ViewGroup.inflate(
    @LayoutRes resource: Int,
    attachToRoot: Boolean = false
): View = LayoutInflater.from(context).inflate(resource, this, attachToRoot)

fun BottomSheetBehavior<*>.onHidden(listener: () -> Unit): BottomSheetCallback {
    return object : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) listener()
        }
        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }.also { addBottomSheetCallback(it) }
}

/**
 * Returns true if given Uri authority is [MediaStore.AUTHORITY]
 */
fun Uri?.isMediaStoreUri(): Boolean = this?.authority?.contains(MediaStore.AUTHORITY) ?: false

/**
 * Set a query string parameter overwriting the old one if available.
 * @param key: name of the parameter
 * @param value: value of the parameter
 * @return the updated [Uri]
 */
fun Uri.setQueryParameter(key: String, value: String): Uri =
    with(buildUpon()) {
        clearQuery()
        queryParameterNames.forEach {
            if (it != key) appendQueryParameter(it, getQueryParameter(it))
        }
        appendQueryParameter(key, value)
        build()
    }

/**
 * Sets the start-position compound drawable and clears all others
 *
 * @see TextView.setCompoundDrawablesRelativeWithIntrinsicBounds
 */
fun TextView.setStartDrawable(@DrawableRes icon: Int) =
    setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
