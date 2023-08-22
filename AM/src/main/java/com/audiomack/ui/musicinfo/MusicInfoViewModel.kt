package com.audiomack.ui.musicinfo

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import com.audiomack.MainApplication
import com.audiomack.data.actions.ActionsDataSource
import com.audiomack.data.actions.ActionsRepository
import com.audiomack.data.actions.PermissionRedirect
import com.audiomack.data.actions.ToggleFollowException
import com.audiomack.data.actions.ToggleFollowResult
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.tracking.mixpanel.MixpanelButtonMusicInfo
import com.audiomack.data.user.UserData
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMGenre
import com.audiomack.model.AMResultItem
import com.audiomack.model.Credentials
import com.audiomack.model.EventContentReported
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.model.ReportContentModel
import com.audiomack.model.ReportContentType
import com.audiomack.model.ReportType
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class MusicInfoViewModel(
    val imageLoader: ImageLoader = PicassoImageLoader,
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val actionsDataSource: ActionsDataSource = ActionsRepository(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val eventBus: EventBus = EventBus.getDefault()
) : BaseViewModel() {

    val closeEvent = SingleLiveEvent<Void>()
    val imageEvent = SingleLiveEvent<Void>()
    val uploaderEvent = SingleLiveEvent<String>()
    val artistEvent = SingleLiveEvent<String>()
    val updatedItemEvent = MutableLiveData<AMResultItem>()
    val followStatus = MutableLiveData<Boolean>()
    val notifyFollowToast = SingleLiveEvent<ToggleFollowResult.Notify>()
    val offlineAlert = SingleLiveEvent<Void>()
    val loggedOutAlert = SingleLiveEvent<LoginSignupSource>()
    val showReportReasonEvent = SingleLiveEvent<ReportContentModel>()
    val showReportAlertEvent = SingleLiveEvent<ReportType>()
    val promptNotificationPermissionEvent = SingleLiveEvent<PermissionRedirect>()

    private lateinit var item: AMResultItem

    private var pendingActionAfterLogin: PendingActionAfterLogin? = null

    init {
        userDataSource.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        eventBus.register(this)
    }

    @VisibleForTesting
    public override fun onCleared() {
        super.onCleared()
        eventBus.unregister(this)
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        when (state) {
            EventLoginState.LOGGED_IN -> pendingActionAfterLogin?.let {
                when (it) {
                    is PendingActionAfterLogin.Follow -> onFollowTapped(it.mixpanelSource)
                }
                pendingActionAfterLogin = null
            }
            else -> pendingActionAfterLogin = null
        }
    }

    fun initItem(item: AMResultItem) {
        this.item = item
        updatedItemEvent.postValue(item)
    }

    fun getImage(): String? {
        return item.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetOriginal)
    }

    fun getTitle(): String? {
        return item.title
    }

    fun isUploaderVerified(): Boolean {
        return item.isUploaderVerified
    }

    fun isUploaderTastemaker(): Boolean {
        return item.isUploaderTastemaker
    }

    fun isUploaderAuthenticated(): Boolean {
        return item.isUploaderAuthenticated
    }

    fun getArtist(): String? {
        return item.artist
    }

    fun isArtistVisible(): Boolean {
        return !item.isPlaylist
    }

    fun getAddedBy(): String? {
        return item.uploaderName
    }

    fun getFeat(): String? {
        return item.featured
    }

    fun isFeatVisible(): Boolean {
        return !getFeat().isNullOrEmpty()
    }

    fun isFollowButtonVisible(): Boolean {
        return !Credentials.itsMe(MainApplication.context, item.uploaderId ?: "")
    }

    fun getAlbum(): String? {
        return if (item.isAlbum) item.title else item.album
    }

    fun isAlbumVisible(): Boolean {
        return !getAlbum().isNullOrBlank() && !item.isPlaylist
    }

    fun getProducer(): String? {
        return item.producer
    }

    fun isProducerVisible(): Boolean {
        return !getProducer().isNullOrBlank() && !item.isPlaylist
    }

    fun getAddedOn(): String? {
        return item.released
    }

    fun isAddedOnVisible(): Boolean {
        return !getAddedOn().isNullOrBlank() && !item.isPlaylist
    }

    fun getGenre(context: Context): String? {
        return AMGenre.fromApiValue(item.genre).humanValue(context)
    }

    fun isGenreVisible(context: Context): Boolean {
        return !getGenre(context).isNullOrBlank()
    }

    fun getDescription(): String? {
        return item.desc
    }

    fun isDescriptionVisible(): Boolean {
        return !getDescription().isNullOrBlank()
    }

    fun getPlaylistCreator(): String? {
        return item.artist
    }

    fun isPlaylistCreatorVisible(): Boolean {
        return !getPlaylistCreator().isNullOrEmpty() && item.isPlaylist
    }

    fun getPlaylistTracksCount(): String? {
        return item.playlistTracksCount.toString()
    }

    fun isPlaylistTracksCountVisible(): Boolean {
        return item.isPlaylist
    }

    fun getLastUpdated(): String? {
        return item.lastUpdated
    }

    fun isLastUpdatedVisible(): Boolean {
        return !getLastUpdated().isNullOrEmpty() && item.isPlaylist
    }

    fun getPlays(): String {
        return item.playsShort
    }

    fun getFavorites(): String {
        return item.favoritesShort
    }

    fun getReposts(): String {
        return item.repostsShort
    }

    fun getPlaylists(): String {
        return item.playlistsShort
    }

    fun getRankDaily(): String? {
        return item.rankDaily
    }

    fun getRankWeekly(): String? {
        return item.rankWeekly
    }

    fun getRankMonthly(): String? {
        return item.rankMonthly
    }

    fun getRankAllTime(): String? {
        return item.rankAllTime
    }

    fun isReupsVisible(): Boolean {
        return !item.isPlaylist
    }

    fun isPlaylistsVisible(): Boolean {
        return !item.isPlaylist
    }

    fun isDividerRanks(): Boolean {
        return !item.isPlaylist
    }

    fun isRankingVisible(): Boolean {
        return !item.isPlaylist
    }

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onImageTapped() {
        imageEvent.call()
    }

    fun onUploaderTapped() {
        uploaderEvent.postValue(item.uploaderSlug)
    }

    fun onArtistNameTapped() {
        artistEvent.postValue(getArtist())
    }

    fun onFeatNameTapped(name: String) {
        artistEvent.postValue(name)
    }

    fun onPlaylistCreatorTapped() {
        artistEvent.postValue(getArtist())
    }

    fun onFollowTapped(mixpanelSource: MixpanelSource) {
        compositeDisposable.add(
            actionsDataSource.toggleFollow(item, null, MixpanelButtonMusicInfo, mixpanelSource)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    when (it) {
                        is ToggleFollowResult.Finished -> followStatus.postValue(it.followed)
                        is ToggleFollowResult.Notify -> notifyFollowToast.postValue(it)
                        is ToggleFollowResult.AskForPermission -> promptNotificationPermissionEvent.postValue(it.redirect)
                    }
                }, {
                    when (it) {
                        is ToggleFollowException.LoggedOut -> {
                            pendingActionAfterLogin = PendingActionAfterLogin.Follow(mixpanelSource)
                            loggedOutAlert.postValue(LoginSignupSource.AccountFollow)
                        }
                        is ToggleFollowException.Offline -> offlineAlert.call()
                    }
                })
        )
    }

    fun updateFollowButton() {
        followStatus.postValue(UserData.isArtistFollowed(item.uploaderId))
    }

    fun updateMusicInfo() {
        compositeDisposable.add(
            musicDataSource.getMusicInfo(item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ initItem(it) }, { Timber.w(it) })
        )
    }

    fun onReportTapped() {
        val contentId = item.itemId ?: return
        val contentType = when {
            item.isPlaylist -> {
                ReportContentType.Playlist
            }
            item.isAlbum -> {
                ReportContentType.Album
            }
            else -> {
                ReportContentType.Song
            }
        }
        val reportType = ReportType.Report
        showReportReasonEvent.postValue(ReportContentModel(contentId, contentType, reportType, null))
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventContentReported) {
        showReportAlertEvent.postValue(event.reportType)
    }

    sealed class PendingActionAfterLogin {
        data class Follow(val mixpanelSource: MixpanelSource) : PendingActionAfterLogin()
    }
}
