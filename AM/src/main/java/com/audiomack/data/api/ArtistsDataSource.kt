package com.audiomack.data.api

import com.audiomack.model.AMArtist
import io.reactivex.Observable
import io.reactivex.Single

interface ArtistsDataSource {
    fun updateUserData(): Observable<AMArtist>
    fun updateUserNotifications(): Observable<Boolean>
    fun follow(artistSlug: String): Observable<Boolean>
    fun unfollow(artistSlug: String): Observable<Boolean>
    fun artistData(urlSlug: String): Observable<AMArtist>
    fun findLoggedArtist(): Observable<AMArtist>
    fun save(amArtist: AMArtist): Single<AMArtist>
}
