package com.audiomack.data.user

import androidx.annotation.WorkerThread
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.ArtistFollowStatusChange
import com.audiomack.model.Credentials
import com.audiomack.model.EventFollowChange
import com.audiomack.model.EventLoginState
import com.audiomack.model.LogoutReason
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.lang.Exception
import java.util.Date

interface UserDataSource {

    @WorkerThread
    fun isLoggedIn(): Boolean

    fun isLoggedInAsync(): Single<Boolean>

    @WorkerThread
    fun isAdmin(): Boolean

    @WorkerThread
    fun isTester(): Boolean

    @WorkerThread
    fun canComment(): Boolean

    val avatar: String?

    @WorkerThread
    fun getUser(): AMArtist?

    fun getUserAsync(): Observable<AMArtist>

    @WorkerThread
    fun getArtistId(): String?

    @WorkerThread
    fun getEmail(): String?

    @WorkerThread
    fun getUserSlug(): String?

    @WorkerThread
    fun getUserId(): String?

    @WorkerThread
    fun getUserScreenName(): String?

    val credentials: Credentials?

    fun logout(reason: LogoutReason): Completable

    val loginEvents: Observable<EventLoginState>

    @WorkerThread
    fun getOfflineDownloadsCount(): Int

    @WorkerThread
    fun getPremiumLimitedDownloadsCount(): Int

    @WorkerThread
    fun getPremiumOnlyDownloadsCount(): Int

    fun saveAccount(artist: AMArtist): Single<AMArtist?>

    /**
     * Emits [ArtistFollowStatusChange] from artist follow/unfollow actions generated after the subscription.
     * It's meant to replace subscriptions to [EventFollowChange] through EventBus.
     **/
    val artistFollowEvents: Observable<ArtistFollowStatusChange>

    fun isArtistFollowed(artistId: String?): Boolean

    fun addArtistToFollowing(artistId: String)

    fun removeArtistFromFollowing(artistId: String)

    fun isMusicFavorited(music: AMResultItem): Boolean

    fun addMusicToFavorites(music: AMResultItem)

    fun removeMusicFromFavorites(music: AMResultItem)

    val hasFavorites: Boolean

    fun isMusicReposted(music: AMResultItem): Boolean

    val isContentCreator: Boolean

    fun refreshUserData(): Observable<AMArtist>

    val highlightsCount: Int

    fun isMusicHighlighted(music: AMResultItem): Boolean

    fun addToHighlights(music: AMResultItem)

    fun removeFromHighlights(music: AMResultItem)

    fun completeProfile(name: String, birthday: Date, gender: AMArtist.Gender): Completable

    fun saveLocalArtist(artist: AMArtist): Single<AMArtist>

    val oneSignalId: String?

    fun onLoggedIn()

    fun onLoggedOut()

    fun onLoginCanceled()
}

class AccountSaveException(val title: String?, message: String?) : Exception("$message")
class UserSlugSaveException(val title: String?, message: String?) : Exception("$message")
