package com.audiomack.data.user

import androidx.annotation.VisibleForTesting
import com.audiomack.MainApplication
import com.audiomack.data.database.ArtistDAO
import com.audiomack.data.database.ArtistDAOImpl
import com.audiomack.data.database.MusicDAO
import com.audiomack.data.database.MusicDAOImpl
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.data.remotevariables.datasource.FirebaseRemoteVariablesDataSource
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.model.ArtistFollowStatusChange
import com.audiomack.model.Credentials
import com.audiomack.model.EventFollowChange
import com.audiomack.model.EventLoginState
import com.audiomack.model.LogoutReason
import com.audiomack.network.API
import com.audiomack.network.APIInterface
import com.audiomack.onesignal.OneSignalDataSource
import com.audiomack.onesignal.OneSignalRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.Date
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class UserRepository private constructor(
    private val userData: UserDataInterface,
    private val api: APIInterface.UserInterface,
    private val authAPI: APIInterface.AuthenticationInterface,
    private val apiSettings: APIInterface.SettingsInterface,
    private val artistDAO: ArtistDAO,
    private val musicDAO: MusicDAO,
    private val remoteVariablesProvider: RemoteVariablesProvider,
    eventBus: EventBus,
    private val oneSignalDataSource: OneSignalDataSource
) : UserDataSource {

    init {
        eventBus.register(this)
    }

    private val loginStatusSubject = BehaviorSubject.create<EventLoginState>()
    override val loginEvents: Observable<EventLoginState> get() = loginStatusSubject

    private val artistFollowSubject = PublishSubject.create<ArtistFollowStatusChange>()
    override val artistFollowEvents: Observable<ArtistFollowStatusChange> get() = artistFollowSubject

    override fun isLoggedIn(): Boolean {
        return Credentials.isLogged(MainApplication.context)
    }

    override fun isLoggedInAsync(): Single<Boolean> {
        return Single.create {
            it.onSuccess(isLoggedIn())
        }
    }

    override fun isAdmin(): Boolean {
        return isLoggedIn() && getUser()?.isAdmin == true
    }

    override fun isTester(): Boolean {
        return remoteVariablesProvider.tester
    }

    override fun canComment(): Boolean {
        return getUser()?.canComment == true
    }

    override val avatar: String?
        get() {
            return getUser()?.smallImage
        }

    override fun getUser(): AMArtist? = artistDAO.findSync()

    override fun getUserAsync() = artistDAO.find()

    override fun getArtistId(): String? {
        return getUser()?.artistId
    }

    override fun getEmail(): String? {
        return Credentials.load(MainApplication.context)?.email
    }

    override fun getUserSlug(): String? {
        return Credentials.load(MainApplication.context)?.userUrlSlug
    }

    override fun getUserId(): String? {
        return Credentials.load(MainApplication.context)?.userId
    }

    override fun getUserScreenName(): String? {
        return Credentials.load(MainApplication.context)?.userScreenName
    }

    override val credentials: Credentials?
        get() = Credentials.load(MainApplication.context)

    override fun logout(reason: LogoutReason): Completable =
        Completable.create { emitter ->
            Credentials.logout(MainApplication.context!!, false, reason)
            emitter.onComplete()
        }
        .andThen(authAPI.logout().onErrorComplete())
        .andThen(Completable.create { emitter ->
            apiSettings.updateEnvironment()
            emitter.onComplete()
        })

    override fun getOfflineDownloadsCount() = musicDAO.downloadsCount()

    override fun getPremiumLimitedDownloadsCount() = musicDAO.premiumLimitedDownloadCount()

    override fun getPremiumOnlyDownloadsCount() = musicDAO.premiumOnlyDownloadCount()

    override fun saveAccount(artist: AMArtist): Single<AMArtist?> {
        val urlSlug = artist.urlSlug
        return api.editUserAccountInfo(artist)
                .flatMap {
                    it.urlSlug = urlSlug ?: ""
                    api.editUserUrlSlug(it)
                }
    }

    override fun isArtistFollowed(artistId: String?) = userData.isArtistFollowed(artistId)

    override fun addArtistToFollowing(artistId: String) {
        userData.addArtistToFollowing(artistId)
    }

    override fun removeArtistFromFollowing(artistId: String) {
        userData.removeArtistFromFollowing(artistId)
    }

    override fun isMusicFavorited(music: AMResultItem): Boolean {
        return userData.isItemFavorited(music)
    }

    override fun addMusicToFavorites(music: AMResultItem) {
        userData.addItemToFavorites(music)
    }

    override fun removeMusicFromFavorites(music: AMResultItem) {
        userData.removeItemFromFavorites(music)
    }

    override val hasFavorites: Boolean
        get() = userData.favoritedItemsCount > 0

    override fun isMusicReposted(music: AMResultItem): Boolean {
        return userData.isItemReuped(music.itemId)
    }

    override val isContentCreator: Boolean
        get() = (getUser()?.uploadsCount ?: 0) > 0

    override fun refreshUserData(): Observable<AMArtist> {
        return api.userData
    }

    override val highlightsCount: Int
        get() = userData.getHighlights().size

    override fun isMusicHighlighted(music: AMResultItem): Boolean {
        return userData.isItemHighlighted(music)
    }

    override fun addToHighlights(music: AMResultItem) {
        userData.addItemToHighlights(music)
    }

    override fun removeFromHighlights(music: AMResultItem) {
        userData.removeItemFromHighlights(music)
    }

    override fun completeProfile(
        name: String,
        birthday: Date,
        gender: AMArtist.Gender
    ) = api.completeProfile(name, birthday, gender)

    override fun saveLocalArtist(artist: AMArtist) = artistDAO.save(artist)

    override val oneSignalId: String?
        get() = oneSignalDataSource.playerId

    override fun onLoggedIn() {
        loginStatusSubject.onNext(EventLoginState.LOGGED_IN)
    }

    override fun onLoggedOut() {
        loginStatusSubject.onNext(EventLoginState.LOGGED_OUT)
    }

    override fun onLoginCanceled() {
        loginStatusSubject.onNext(EventLoginState.CANCELED_LOGIN)
    }

    // Other

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(artistFollowChange: EventFollowChange) {
        val artistId = artistFollowChange.artistId
        artistFollowSubject.onNext(ArtistFollowStatusChange(
            artistId = artistId,
            followed = isArtistFollowed(artistId)
        ))
    }

    companion object {

        @Volatile
        private var INSTANCE: UserRepository? = null

        @JvmStatic
        @JvmOverloads
        fun getInstance(
            userData: UserDataInterface = UserData,
            api: APIInterface.UserInterface = API.getInstance(),
            authAPI: APIInterface.AuthenticationInterface = API.getInstance(),
            apiSettings: APIInterface.SettingsInterface = API.getInstance(),
            artistDAO: ArtistDAO = ArtistDAOImpl(),
            musicDAO: MusicDAO = MusicDAOImpl(),
            remoteVariablesProvider: RemoteVariablesProvider = RemoteVariablesProviderImpl(FirebaseRemoteVariablesDataSource),
            eventBus: EventBus = EventBus.getDefault(),
            oneSignalDataSource: OneSignalDataSource = OneSignalRepository.getInstance()
        ): UserRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: UserRepository(
                        userData,
                        api,
                        authAPI,
                        apiSettings,
                        artistDAO,
                        musicDAO,
                        remoteVariablesProvider,
                        eventBus,
                        oneSignalDataSource
                    ).also { INSTANCE = it }
            }

        @VisibleForTesting
        fun destroy() {
            INSTANCE = null
        }
    }
}
