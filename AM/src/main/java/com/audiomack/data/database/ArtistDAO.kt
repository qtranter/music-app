package com.audiomack.data.database

import com.audiomack.model.AMArtist
import io.reactivex.Observable
import io.reactivex.Single

interface ArtistDAO {

    fun findSync(): AMArtist?

    fun find(): Observable<AMArtist>

    fun save(artist: AMArtist): Single<AMArtist>
}

class ArtistDAOException(message: String) : Exception(message)
