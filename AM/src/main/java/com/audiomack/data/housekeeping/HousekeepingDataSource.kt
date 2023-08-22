package com.audiomack.data.housekeeping

import android.content.Context
import com.audiomack.model.AMResultItem
import io.reactivex.Completable
import io.reactivex.Observable

interface HousekeepingDataSource {

    val syncMusic: Completable

    val houseekping: Completable

    val downloadsToRestore: Observable<List<AMResultItem>>

    fun createNoMediaFiles(context: Context): Completable

    fun clearRestoredDatabase(): Completable
}
