package com.audiomack.data.actions

import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumDownloadModel
import io.reactivex.Observable
import java.lang.Exception

interface ActionsDataSource {

    /**
     * Emits [ToggleFavoriteResult], throws [ToggleFavoriteException]
     */
    fun toggleFavorite(
        music: AMResultItem,
        mixpanelButton: String,
        mixpanelSource: MixpanelSource
    ): Observable<ToggleFavoriteResult>

    /**
     * Emits [ToggleRepostResult], throws [ToggleRepostException]
     */
    fun toggleRepost(
        music: AMResultItem,
        mixpanelButton: String,
        mixpanelSource: MixpanelSource
    ): Observable<ToggleRepostResult>

    /**
     * Emits [ToggleFollowResult], throws [ToggleFollowException]
     */
    fun toggleFollow(
        music: AMResultItem? = null,
        artist: AMArtist? = null,
        mixpanelButton: String,
        mixpanelSource: MixpanelSource
    ): Observable<ToggleFollowResult>

    /**
     * Emits a meaningless boolean, throws [AddToPlaylistException]
     */
    fun addToPlaylist(
        music: AMResultItem
    ): Observable<Boolean>

    /**
     * Emits [ToggleHighlightResult], throws [ToggleHighlightException]
     */
    fun toggleHighlight(
        music: AMResultItem,
        mixpanelButton: String,
        mixpanelSource: MixpanelSource
    ): Observable<ToggleHighlightResult>

    /**
     * Emits [ToggleDownloadResult], throws [ToggleDownloadException]
     * @param music: [AMResultItem] to be downloaded, can be either a song, album or playlist
     * @param retry: this is useful when music has already been downloaded, instead of prompting for its deletion it will simply be retried
     * @param skipFrozenCheck: this is useful when music is frozen, if set to true we'll skip the ShowPremiumDownload error and go ahead in the waterfall to ask for deletion
     * @param parentAlbum: optional [AMResultItem] that represents the album which the song we are downloading belongs to
     */
    fun toggleDownload(
        music: AMResultItem,
        mixpanelButton: String,
        mixpanelSource: MixpanelSource,
        retry: Boolean = false,
        skipFrozenCheck: Boolean = true,
        parentAlbum: AMResultItem? = null
    ): Observable<ToggleDownloadResult>
}

sealed class ToggleFavoriteResult {
    data class Notify(val isSuccessful: Boolean, val wantedToFavorite: Boolean, val isPlaylist: Boolean, val isAlbum: Boolean, val isSong: Boolean, val title: String, val artist: String) : ToggleFavoriteResult()
}

sealed class ToggleFavoriteException(message: String) : Exception(message) {
    object Offline : ToggleFavoriteException("Offline")
    object LoggedOut : ToggleFavoriteException("User is not logged in")
}

sealed class ToggleRepostResult {
    data class Notify(val isSuccessful: Boolean, val isAlbum: Boolean, val title: String, val artist: String) : ToggleRepostResult()
}

sealed class ToggleRepostException(message: String) : Exception(message) {
    object Offline : ToggleRepostException("Offline")
    object LoggedOut : ToggleRepostException("User is not logged in")
}

sealed class ToggleFollowResult {
    data class Notify(val followed: Boolean, val uploaderName: String, val uploaderUrlSlug: String, val uploaderImage: String) : ToggleFollowResult()
    data class Finished(val followed: Boolean) : ToggleFollowResult()
    data class AskForPermission(val redirect: PermissionRedirect) : ToggleFollowResult()
}

enum class PermissionRedirect { Settings, NotificationsManager }

sealed class ToggleFollowException(message: String) : Exception(message) {
    object Offline : ToggleFollowException("Offline")
    object LoggedOut : ToggleFollowException("User is not logged in")
}

sealed class AddToPlaylistException(message: String) : Exception(message) {
    object LoggedOut : AddToPlaylistException("User is not logged in")
}

sealed class ToggleHighlightResult {
    data class Added(val title: String) : ToggleHighlightResult()
    object Removed : ToggleHighlightResult()
}

sealed class ToggleHighlightException(message: String) : Exception(message) {
    object Offline : ToggleHighlightException("Offline")
    object LoggedOut : ToggleHighlightException("User is not logged in")
    object ReachedLimit : ToggleHighlightException("Reached max number of highlights allowed")
    data class Failure(val highliting: Boolean) : ToggleHighlightException("Failed to add or remove from highlights")
}

sealed class ToggleDownloadResult {
    object ConfirmPlaylistDeletion : ToggleDownloadResult()
    object ConfirmMusicDeletion : ToggleDownloadResult()
    data class ConfirmPlaylistDownload(val tracksCount: Int) : ToggleDownloadResult()
    object StartedBlockingAPICall : ToggleDownloadResult()
    object EndedBlockingAPICall : ToggleDownloadResult()
    data class ShowUnlockedToast(val musicName: String) : ToggleDownloadResult()
    object DownloadStarted : ToggleDownloadResult()
}

sealed class ToggleDownloadException(message: String) : Exception(message) {
    data class LoggedOut(val source: LoginSignupSource) : ToggleDownloadException("User is not logged in")
    data class Unsubscribed(val mode: InAppPurchaseMode) : ToggleDownloadException("User is not premium")
    object FailedDownloadingPlaylist : ToggleDownloadException("Failed download playlist, API call to get the trakcs failed")
    data class ShowPremiumDownload(val model: PremiumDownloadModel) : ToggleDownloadException("Subject to premium download limit")
}
