package com.audiomack.data.sociallink

import com.audiomack.network.API
import com.audiomack.network.APIInterface
import com.audiomack.network.LoginProviderData
import io.reactivex.Completable

interface SocialLinkDataSource {
    fun linkTwitter(token: String, secret: String): Completable
    fun linkInstagram(token: String): Completable
}

class SocialLinkRepository(
    private val api: APIInterface.SocialLinkInterface = API.getInstance()
) : SocialLinkDataSource {
    override fun linkTwitter(token: String, secret: String): Completable {
        return api.linkSocial(LoginProviderData.Twitter(token, secret))
    }
    override fun linkInstagram(token: String): Completable {
        return api.linkSocial(LoginProviderData.Instagram(token))
    }
}
