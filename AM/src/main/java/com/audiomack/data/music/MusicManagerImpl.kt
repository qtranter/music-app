package com.audiomack.data.music

import com.audiomack.model.AMResultItem
import io.reactivex.Observable

class MusicManagerImpl : MusicManager {

    override fun isDownloadFailed(track: AMResultItem): Observable<Boolean> {
        return Observable.create {
            try {
                val failed = track.isDownloadedAndNotCached && !track.isDownloadCompleted && !track.isDownloadInProgress
                it.onNext(failed)
            } catch (e: Exception) {
                it.onNext(false)
            }
        }
    }
}
