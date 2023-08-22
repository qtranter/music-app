package com.audiomack.data.api

import com.audiomack.data.database.ArtistDAO
import com.audiomack.data.database.ArtistDAOImpl
import com.audiomack.model.AMArtist
import com.audiomack.network.API
import io.reactivex.Observable
import io.reactivex.Single

class ArtistsRepository(
    private val artistDAO: ArtistDAO = ArtistDAOImpl()
) : ArtistsDataSource {

    override fun updateUserData(): Observable<AMArtist> {
        return API.getInstance().userData
    }

    override fun updateUserNotifications(): Observable<Boolean> {
        return API.getInstance().getUserNotifications(null, false).observable.map { true }
    }

    override fun follow(artistSlug: String): Observable<Boolean> {
        return Observable.create { emitter ->
            API.getInstance().followArtist(artistSlug, object : API.FollowListener {
                override fun onSuccess() {
                    emitter.onNext(true)
                    emitter.onComplete()
                }
                override fun onFailure() {
                    emitter.onNext(false)
                    emitter.onComplete()
                }
            })
        }
    }

    override fun unfollow(artistSlug: String): Observable<Boolean> {
        return Observable.create { emitter ->
            API.getInstance().unfollowArtist(artistSlug, object : API.FollowListener {
                override fun onSuccess() {
                    emitter.onNext(false)
                    emitter.onComplete()
                }
                override fun onFailure() {
                    emitter.onNext(true)
                    emitter.onComplete()
                }
            })
        }
    }

    override fun artistData(urlSlug: String): Observable<AMArtist> {
        return API.getInstance().getArtistInfo(urlSlug)
    }

    override fun findLoggedArtist(): Observable<AMArtist> {
        return artistDAO.find()
    }

    override fun save(amArtist: AMArtist): Single<AMArtist> {
        return artistDAO.save(amArtist)
    }
}
