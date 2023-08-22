package com.audiomack.data.premiumdownload

import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.model.EventDownload
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.disposables.Disposable
import org.greenrobot.eventbus.EventBus

interface IUnlockPremiumDownloadUseCase {
    fun unlockFrozenDownload(musicId: String): Disposable
}

class UnlockPremiumDownloadUseCase(
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val eventBus: EventBus = EventBus.getDefault(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : IUnlockPremiumDownloadUseCase {

    override fun unlockFrozenDownload(musicId: String) =
        musicDataSource.getOfflineItem(musicId)
            .subscribeOn(schedulersProvider.io)
            .doOnSuccess { if (it.isAlbum) it.loadTracks() }
            .flatMapCompletable {
                val ids = it.tracks?.map { track -> track.itemId } ?: listOf(it.itemId)
                musicDataSource.markFrozenDownloads(false, ids)
            }
            .observeOn(schedulersProvider.main)
            .subscribe({
                eventBus.post(EventDownload(musicId, true))
            }, {})
}
