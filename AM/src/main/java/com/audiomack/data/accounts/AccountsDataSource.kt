package com.audiomack.data.accounts

import com.audiomack.model.APIResponseData
import io.reactivex.Observable

interface AccountsDataSource {
    fun getEditorialPickedArtists(page: Int): Observable<APIResponseData>
    fun getRecsysArtists(): Observable<APIResponseData>
}
