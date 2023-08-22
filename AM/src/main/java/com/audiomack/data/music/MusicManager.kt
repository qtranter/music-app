package com.audiomack.data.music

import com.audiomack.model.AMResultItem
import io.reactivex.Observable

interface MusicManager {

    fun isDownloadFailed(track: AMResultItem): Observable<Boolean>
}
