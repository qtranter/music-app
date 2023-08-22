package com.audiomack.usecases

import com.audiomack.data.accounts.AccountsDataSource
import com.audiomack.data.accounts.AccountsRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMArtist
import com.audiomack.model.APIResponseData
import io.reactivex.Observable
import io.reactivex.Single

interface FetchSuggestedAccountsUseCase {
    /**
     * Emits a list of suggested [AMArtist] to follow.
     * Under the hood it combines recsys data with editorial picks.
     */
    operator fun invoke(page: Int): Single<List<AMArtist>>
}

class FetchSuggestedAccountsUseCaseImpl(
    private val userRepository: UserDataSource = UserRepository.getInstance(),
    private val accountsRepository: AccountsDataSource = AccountsRepository()
) : FetchSuggestedAccountsUseCase {

    @Suppress("UNCHECKED_CAST")
    override fun invoke(page: Int) =
        userRepository.isLoggedInAsync()
            .onErrorReturnItem(false)
            .flatMap { isLoggedIn ->
                Single.fromObservable(
                    if (isLoggedIn) {
                        when (page) {
                            0 ->
                                Observable.zip(
                                    accountsRepository.getRecsysArtists(),
                                    accountsRepository.getEditorialPickedArtists(page),
                                    { recsys, editorial -> APIResponseData(recsys.objects + editorial.objects, null) }
                                )
                            else -> accountsRepository.getEditorialPickedArtists(page)
                        }
                    } else accountsRepository.getEditorialPickedArtists(page)
                )
            }
            .map { it.objects as List<AMArtist> }
}
