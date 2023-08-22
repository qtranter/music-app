package com.audiomack.data.database

import com.activeandroid.query.Select
import com.audiomack.model.AMArtist
import io.reactivex.Observable
import io.reactivex.Single
import java.lang.IllegalStateException

class ArtistDAOImpl : ArtistDAO {

    override fun findSync(): AMArtist? =
        Select().from(AMArtist::class.java).executeSingle()

    override fun find() = Observable.create<AMArtist> { emitter ->
        val result = Select().from(AMArtist::class.java).executeSingle<AMArtist>()
        if (result != null) {
            emitter.onNext(result)
            emitter.onComplete()
        } else {
            emitter.onError(ArtistDAOException("Artist not found"))
        }
    }

    override fun save(artist: AMArtist) = Single.create<AMArtist> { emitter ->
        val saved = artist.save()
        if (saved < 0L) {
            emitter.onError(IllegalStateException("Database is null"))
        } else {
            emitter.onSuccess(artist)
        }
    }
}
