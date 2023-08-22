package com.audiomack.data.accounts

import com.audiomack.model.APIResponseData
import com.audiomack.network.API
import com.audiomack.network.APIInterface
import io.reactivex.Observable

class AccountsRepository(
    private val api: APIInterface.AccountsInterface = API.getInstance()
) : AccountsDataSource {

    override fun getEditorialPickedArtists(page: Int): Observable<APIResponseData> {
        return api.getSuggestedFollows(page)
    }

    override fun getRecsysArtists(): Observable<APIResponseData> {
        return api.getArtistsRecommendations()
    }
}
