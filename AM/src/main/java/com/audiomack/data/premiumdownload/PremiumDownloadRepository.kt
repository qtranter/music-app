package com.audiomack.data.premiumdownload

import androidx.annotation.VisibleForTesting
import com.audiomack.data.database.MusicDAO
import com.audiomack.data.database.MusicDAOImpl
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premium.PremiumRepository
import com.audiomack.download.AMMusicDownloader
import com.audiomack.download.MusicDownloader
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.utils.addTo
import io.reactivex.disposables.CompositeDisposable
import kotlin.math.max
import org.greenrobot.eventbus.EventBus

class PremiumDownloadRepository private constructor(
    private val premiumDataSource: PremiumDataSource,
    private val musicDAO: MusicDAO,
    private val schedulersProvider: SchedulersProvider,
    private val musicDownloader: MusicDownloader,
    private val eventBus: EventBus
) : PremiumDownloadDataSource {

    private val disposables = CompositeDisposable()

    init {
        premiumDataSource.premiumObservable
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ onPremiumStatusChanged(it) }, {})
            .addTo(disposables)
    }

    private fun onPremiumStatusChanged(isPremium: Boolean) {
        if (isPremium) {
            // Unfreeze all frozen premium-limited items
            musicDAO.getPremiumLimitedSongs()
                .subscribeOn(schedulersProvider.io)
                .flatMapCompletable { ids -> musicDAO.markFrozen(false, ids) }
                .observeOn(schedulersProvider.main)
                .subscribe({ eventBus.post(EventDownloadsEdited()) }, {})
                .addTo(disposables)
        } else {
            // Freeze the oldest (premiumDownloadCount-premiumDownloadLimit) tracks if there are more than 20 unfrozen tracks
            musicDAO.premiumLimitedUnfrozenDownloadCountAsync()
                .subscribeOn(schedulersProvider.io)
                .doOnSuccess { if (it <= premiumDownloadLimit) throw Exception("No freeze needed") }
                .flatMap { musicDAO.getPremiumLimitedSongs() }
                .map { it.dropLast(premiumDownloadLimit) }
                .flatMapCompletable { ids -> musicDAO.markFrozen(true, ids) }
                .observeOn(schedulersProvider.main)
                .subscribe({ eventBus.post(EventDownloadsEdited()) }, {})
                .addTo(disposables)
        }
    }

    override val premiumDownloadLimit: Int = 20

    override val remainingPremiumLimitedDownloadCount: Int
        get() = max(premiumDownloadLimit - premiumLimitedUnfrozenDownloadCount, 0)

    override val premiumLimitedUnfrozenDownloadCount: Int
        get() = musicDAO.premiumLimitedUnfrozenDownloadCount()

    override fun getFrozenCount(music: AMResultItem): Int {
        return when {
            music.isSong || music.isAlbumTrack || music.isPlaylistTrack -> {
                if (music.isDownloadFrozen) 1 else 0
            }
            music.isAlbum || music.isPlaylist -> {
                if (music.tracks.isNullOrEmpty()) music.loadTracks()
                music.tracks?.filter { it.isDownloadFrozen }?.size ?: 0
            }
            else -> 0
        }
    }

    override fun getToBeDownloadedPremiumLimitedCount(music: AMResultItem): Int {
        return when {
            music.isSong || music.isAlbumTrack || music.isPlaylistTrack -> {
                if (music.downloadType == AMResultItem.MusicDownloadType.Limited && !music.isGeoRestricted) 1 else 0
            }
            music.isAlbum || music.isPlaylist -> {
                music.tracks?.filter { it.downloadType == AMResultItem.MusicDownloadType.Limited && !music.isGeoRestricted && (!it.isDownloadCompleted || it.isDownloadFrozen) }?.size ?: 0
            }
            else -> 0
        }
    }

    override fun canDownloadMusicBasedOnPremiumLimitedCount(music: AMResultItem): Boolean {
        if (music.downloadType == AMResultItem.MusicDownloadType.Free) return true
        if (premiumDataSource.isPremium) return true
        if (music.downloadType == AMResultItem.MusicDownloadType.Premium) return false
        if (music.isDownloadFrozen) return false
        val toBeDownloadedCount = getToBeDownloadedPremiumLimitedCount(music)
        if (toBeDownloadedCount == 0) return true
        if (premiumLimitedUnfrozenDownloadCount + musicDownloader.countOfPremiumLimitedDownloadsInProgressOrQueued + toBeDownloadedCount > premiumDownloadLimit) return false
        return true
    }

    companion object {
        private var INSTANCE: PremiumDownloadRepository? = null

        @JvmOverloads
        @JvmStatic
        fun getInstance(
            premiumDataSource: PremiumDataSource = PremiumRepository.getInstance(),
            musicDAO: MusicDAO = MusicDAOImpl(),
            schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
            musicDownloader: MusicDownloader = AMMusicDownloader.getInstance(),
            eventBus: EventBus = EventBus.getDefault()
        ): PremiumDownloadRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PremiumDownloadRepository(
                    premiumDataSource,
                    musicDAO,
                    schedulersProvider,
                    musicDownloader,
                    eventBus
                ).also { INSTANCE = it }
            }

        @VisibleForTesting
        internal fun destroy() {
            INSTANCE?.disposables?.clear()
            INSTANCE = null
        }
    }
}
